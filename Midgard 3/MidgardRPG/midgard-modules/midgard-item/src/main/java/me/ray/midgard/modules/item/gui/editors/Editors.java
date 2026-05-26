package me.ray.midgard.modules.item.gui.editors;

import me.ray.midgard.modules.item.model.MidgardItem;
import me.ray.midgard.modules.item.gui.editors.impl.*;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

public class Editors {

    public static StatEditor string(BiConsumer<MidgardItem, String> setter, Function<MidgardItem, String> getter, String prompt) {
        return new StringEditor(setter, getter, prompt);
    }

    public static StatEditor integer(BiConsumer<MidgardItem, Integer> setter, Function<MidgardItem, Integer> getter, String prompt) {
        return new IntegerEditor(setter, getter, prompt);
    }

    public static StatEditor doubleVal(BiConsumer<MidgardItem, Double> setter, Function<MidgardItem, Double> getter, String prompt) {
        return new DoubleEditor(setter, getter, prompt);
    }

    public static StatEditor bool(BiConsumer<MidgardItem, Boolean> setter, Function<MidgardItem, Boolean> getter) {
        return new BooleanEditor(setter, getter);
    }

    public static StatEditor list(BiConsumer<MidgardItem, List<String>> setter, Function<MidgardItem, List<String>> getter, String title) {
        return new ListEditor(setter, getter, title);
    }
    
    public static StatEditor material(BiConsumer<MidgardItem, org.bukkit.Material> setter, String prompt) {
        return new MaterialEditor(setter, prompt);
    }
    
    public static StatEditor rpgStat(me.ray.midgard.modules.item.model.ItemStat stat) {
        return new RpgStatEditor(stat);
    }
}
