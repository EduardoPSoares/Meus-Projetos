# Midgard NMS Module

Este módulo contém a abstração e implementação para acesso ao NMS (Net Minecraft Server).

## Estrutura

- **api**: Contém as interfaces que definem os métodos NMS. O `midgard-core` depende deste módulo.
- **v1_21**: Contém a implementação para a versão 1.21 do Minecraft. O `midgard-loader` depende deste módulo para incluir a implementação no plugin final.

## Como adicionar dependências NMS

Para que o código no módulo `v1_21` compile com acesso às classes `net.minecraft.server` e `org.bukkit.craftbukkit`, você precisa adicionar a dependência correta no `build.gradle` do módulo `v1_21`.

Existem várias formas de fazer isso:

1. **PaperWeight (Recomendado para desenvolvimento moderno)**: Configurar o plugin `io.papermc.paperweight.userdev` no projeto.
2. **Local Jar**: Adicionar o jar do servidor (spigot ou paper) como dependência local.
3. **Maven Local**: Se você rodou o BuildTools, pode depender do `org.spigotmc:spigot:1.21-R0.1-SNAPSHOT`.

Exemplo no `midgard-nms/v1_21/build.gradle`:

```gradle
dependencies {
    implementation project(':midgard-nms:api')
    compileOnly "io.papermc.paper:paper-api:1.21-R0.1-SNAPSHOT"
    // compileOnly 'org.spigotmc:spigot:1.21-R0.1-SNAPSHOT' // Se tiver no maven local
}
```

Após configurar a dependência, você pode descomentar o código em `NMSHandlerImpl.java`.
