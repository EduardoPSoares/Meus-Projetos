package me.ray.midgard.modules.combat;

import me.ray.midgard.core.debug.MidgardLogger;
import me.ray.midgard.core.utils.Task;
import me.ray.midgard.core.utils.TeleportUtils;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.TextDisplay;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Gerenciador de Indicadores de Dano (Hologramas).
 * <p>
 * Responsável por criar e animar displays de texto flutuantes quando uma entidade recebe dano.
 * Utiliza TextDisplay (entidade do Minecraft 1.19.4+) para performance otimizada.
 */
public class DamageIndicatorManager {

    private final CombatConfig config;
    private final Map<String, DamageStyle> styles = new HashMap<>();

    /**
     * Construtor do DamageIndicatorManager.
     *
     * @param plugin Instância do plugin principal.
     * @param config Configuração do módulo de combate.
     */
    public DamageIndicatorManager(JavaPlugin plugin, CombatConfig config) {
        this.config = config;
        loadStyles();
    }

    private void loadStyles() {
        // Carrega estilos padrão
        registerStyle("Normal", "", config.indicatorFormatNormal);
        registerStyle("Weapon", config.indicatorIconWeapon, config.indicatorFormatNormal);
        registerStyle("Physical", config.indicatorIconPhysical, config.indicatorFormatPhysical);
        registerStyle("Projectile", config.indicatorIconProjectile, config.indicatorFormatProjectile);
        registerStyle("Magical", config.indicatorIconMagical, config.indicatorFormatMagical);
        // Aliases for magic damage
        registerStyle("Magic", config.indicatorIconMagical, config.indicatorFormatMagical);
        registerStyle("Magic_Damage", config.indicatorIconMagical, config.indicatorFormatMagical);
        registerStyle("Magical_Damage", config.indicatorIconMagical, config.indicatorFormatMagical);
        
        registerStyle("Environment", config.indicatorIconEnvironment, config.indicatorFormatEnvironment);
        registerStyle("True", config.indicatorIconTrue, config.indicatorFormatTrue);
        
        // Novos estilos: DOT, Skill, Minion
        registerStyle("DOT", config.indicatorIconDot, config.indicatorFormatDot);
        registerStyle("Skill", config.indicatorIconSkill, config.indicatorFormatSkill);
        registerStyle("Minion", config.indicatorIconMinion, config.indicatorFormatMinion);
        
        // Carrega estilos elementais
        for (Map.Entry<String, String> entry : config.elementalIcons.entrySet()) {
            String key = entry.getKey();
            String icon = entry.getValue();
            String color = config.elementalFormats.getOrDefault(key, "<white>");
            
            registerStyle(key, icon, color);
            registerStyle(capitalize(key), icon, color);
            registerStyle(key + "_damage", icon, color);
        }
    }

    /**
     * Recarrega todos os estilos de indicador a partir da config atual.
     */
    public void reload() {
        styles.clear();
        loadStyles();
    }

    private void registerStyle(String key, String icon, String color) {
        if (icon == null) {
            icon = "";
        }
        if (color == null) {
            color = "<white>";
        }
        styles.put(key.toLowerCase(), new DamageStyle(icon.trim(), color));
    }

    private String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    public void spawnCustomIndicator(LivingEntity victim, String text, String color) {
        Location location = victim.getLocation();
        double eyeHeight = victim.getEyeHeight();
        // Fallback for dead entities or special cases
        if (eyeHeight < 0.1) {
             eyeHeight = 1.5; 
        }
        
        Location spawnLoc = location.clone().add(0, eyeHeight + 0.5, 0);

        // Usando Task.sync para compatibilidade Folia
        Task.sync(spawnLoc, () -> {
            try {
                spawnLoc.getWorld().spawn(spawnLoc, TextDisplay.class, display -> {
                    display.text(me.ray.midgard.core.text.MessageUtils.parse(color + text));
                    display.setBillboard(Display.Billboard.CENTER);
                    display.setSeeThrough(true);
                    display.setShadowed(true);
                    display.setBackgroundColor(Color.fromARGB(0, 0, 0, 0));

                    Transformation transformation = display.getTransformation();
                    transformation.getScale().set(1.2f, 1.2f, 1.2f);
                    display.setTransformation(transformation);

                    // Animação simples de subir
                    final org.bukkit.scheduler.BukkitTask[] timerTask = {null};
                    timerTask[0] = Task.syncTimer(display, new Runnable() {
                        int ticks = 0;
                        @Override
                        public void run() {
                            if (ticks > 20 || !display.isValid()) {
                                display.remove();
                                if (timerTask[0] != null) {
                                    timerTask[0].cancel();
                                }
                                return;
                            }
                            // Move up slightly
                            TeleportUtils.teleport(display, display.getLocation().add(0, 0.05, 0));
                            ticks++;
                        }
                    }, 1L, 1L);
                    
                    // Kill task after 20 ticks to be safe
                    Task.syncLater(display, () -> {
                        display.remove();
                        if (timerTask[0] != null) {
                            timerTask[0].cancel();
                        }
                    }, 21L);
                });
            } catch (Exception e) {
                MidgardLogger.error("Erro ao criar indicador de dano", e);
            }
        });
    }

    /**
     * Spawna um indicador de dano próximo à entidade vítima.
     * O indicador exibe o valor do dano e ícones correspondentes aos tipos de dano.
     *
     * @param victim A entidade que recebeu o dano.
     * @param damageMap Mapa contendo os tipos de dano e seus valores.
     * @param isCritical Se o ataque foi crítico (altera a formatação).
     */
    public void spawnIndicator(LivingEntity victim, Map<String, Double> damageMap, boolean isCritical) {
        Location location = victim.getLocation();
        
        // 1. Calcula o Deslocamento Lateral (Esquerda ou Direita)
        Vector direction = location.getDirection().setY(0).normalize();
        if (direction.lengthSquared() < 0.001) {
            direction = new Vector(1, 0, 0); 
        }
        
        Vector right = direction.clone().crossProduct(new Vector(0, 1, 0)).normalize();
        boolean isRight = ThreadLocalRandom.current().nextBoolean();
        double sideDistance = 0.6 + (ThreadLocalRandom.current().nextDouble() * 0.3); 
        double sideMultiplier = isRight ? 1 : -1;
        Vector offset = right.multiply(sideDistance * sideMultiplier);
        Vector forwardOffset = direction.clone().multiply((ThreadLocalRandom.current().nextDouble() - 0.5) * 0.4);
        offset.add(forwardOffset);
        
        double eyeHeight = victim.getEyeHeight();
        double offsetY = eyeHeight + (ThreadLocalRandom.current().nextDouble() * 0.3); 

        Location spawnLoc = location.clone().add(offset).add(0, offsetY, 0);

        // Usando Task.sync para spawnar a entidade na thread correta (Region Thread)
        Task.sync(spawnLoc, () -> {
            spawnLoc.getWorld().spawn(spawnLoc, TextDisplay.class, display -> {
                // 2. Visuais
                double totalDamage = 0;
                List<String> iconParts = new ArrayList<>();
                final String[] lastColorWrapper = {config.indicatorFormatNormal};
                
                for (Map.Entry<String, Double> entry : damageMap.entrySet()) {
                    String fullType = entry.getKey();
                    double value = entry.getValue();
                    if (value <= 0) {
                        continue;
                    }
                    totalDamage += value;
                    String[] types = fullType.split("\\+");
                    for (String rawType : types) {
                        String type = rawType.trim();
                        DamageStyle style = getStyle(type);
                        if (style != null) {
                            lastColorWrapper[0] = style.color;
                            if (!style.icon.isEmpty()) {
                                iconParts.add(style.color + style.icon);
                            }
                        }
                    }
                }
                
                String icons = String.join(" ", iconParts);
                String numberColor;
                if (isCritical) {
                    numberColor = config.indicatorFormatCritical != null ? config.indicatorFormatCritical : "<red>";
                } else {
                    numberColor = config.indicatorFormatNormal != null ? config.indicatorFormatNormal : "<gray>";
                }
                
                String damageStr = String.format(java.util.Locale.US, "%." + config.indicatorDecimals + "f", totalDamage);
                String text = config.indicatorTemplate
                    .replace("%icons%", icons)
                    .replace("%color%", numberColor)
                    .replace("%damage%", damageStr)
                    .trim();

                display.text(me.ray.midgard.core.text.MessageUtils.parse(text));
                display.setBillboard(Display.Billboard.CENTER);
                display.setSeeThrough(true);
                display.setShadowed(true);
                display.setBackgroundColor(parseColor(config.indicatorBackgroundColor));

                // 3. Configuração de Animação (Keyframed)
                // Estado Inicial: Invisível (Escala 0)
                Transformation initialTransform = new Transformation(
                    new Vector3f(0, 0, 0),       
                    new AxisAngle4f(),           
                    new Vector3f(0.0f, 0.0f, 0.0f), 
                    new AxisAngle4f()            
                );
                display.setTransformation(initialTransform);
                display.setInterpolationDelay(0);
                // Define a duração da próxima interpolação (para o Pop Up)
                display.setInterpolationDuration(3); 

                // Fase 1: Pop Up (Rápido) - Acionado no próximo tick
                Task.syncLater(display, () -> {
                    if (!display.isValid()) {
                        return;
                    }
                    display.setTransformation(new Transformation(
                        new Vector3f(0, 0.6f, 0), // Sobe 0.6
                        new AxisAngle4f(), 
                        new Vector3f(1.2f, 1.2f, 1.2f), // Aumenta Escala
                        new AxisAngle4f()
                    ));
                }, 1L);

                // Fase 2: Cair (Lento) - Acionado após Pop Up
                Task.syncLater(display, () -> {
                    if (!display.isValid()) {
                        return;
                    }
                    
                    int remainingTicks = config.indicatorDuration - 4;
                    if (remainingTicks < 1) {
                        remainingTicks = 1;
                    }
                    
                    display.setInterpolationDelay(0);
                    display.setInterpolationDuration(remainingTicks);
                    display.setTransformation(new Transformation(
                        new Vector3f(0, -0.4f, 0), // Cai para -0.4
                        new AxisAngle4f(), 
                        new Vector3f(1.0f, 1.0f, 1.0f), // Escala Normal
                        new AxisAngle4f()
                    ));
                }, 4L); // 1 (delay inicial) + 3 (duração pop up) = 4 ticks

                // Tarefa de Fade Out (Atualiza apenas opacidade)
                final org.bukkit.scheduler.BukkitTask[] fadeTask = {null};
                fadeTask[0] = Task.syncTimer(display, new Runnable() {
                    int tick = 0;
                    final int duration = config.indicatorDuration;

                    @Override
                    public void run() {
                        if (!display.isValid() || tick >= duration) {
                            display.remove();
                            if (fadeTask[0] != null) {
                                fadeTask[0].cancel();
                            }
                            return;
                        }

                        // Fade Out (Últimos 5 ticks)
                        if (tick > duration - 5) {
                            int alpha = (int) (255 * ((double)(duration - tick) / 5.0));
                            if (alpha < 0) {
                                alpha = 0;
                            }
                            display.setTextOpacity((byte) alpha);
                            
                            Color bgColor = display.getBackgroundColor();
                            if (bgColor != null) {
                                int bgAlpha = (int) (bgColor.getAlpha() * ((double)(duration - tick) / 5.0));
                                display.setBackgroundColor(Color.fromARGB(bgAlpha, bgColor.getRed(), bgColor.getGreen(), bgColor.getBlue()));
                            }
                        }
                        tick++;
                    }
                }, 1L, 1L);
            });
        });
    }

    private static final DamageStyle FALLBACK_STYLE = new DamageStyle("", "<white>");

    private DamageStyle getStyle(String type) {
        return styles.getOrDefault(type.toLowerCase(), FALLBACK_STYLE);
    }

    private static class DamageStyle {
        final String icon;
        final String color;

        DamageStyle(String icon, String color) {
            this.icon = icon;
            this.color = color;
        }
    }

    private Color parseColor(String hex) {
        if (hex == null || !hex.startsWith("#")) {
            return Color.fromARGB(0, 0, 0, 0);
        }
        try {
            String clean = hex.substring(1);
            if (clean.length() == 8) {
                int a = Integer.parseInt(clean.substring(0, 2), 16);
                int r = Integer.parseInt(clean.substring(2, 4), 16);
                int g = Integer.parseInt(clean.substring(4, 6), 16);
                int b = Integer.parseInt(clean.substring(6, 8), 16);
                return Color.fromARGB(a, r, g, b);
            }
        } catch (Exception e) {
            MidgardLogger.warn("Erro ao analisar cor hex '%s': %s", hex, e.getMessage());
        }
        return Color.fromARGB(0, 0, 0, 0);
    }
}
