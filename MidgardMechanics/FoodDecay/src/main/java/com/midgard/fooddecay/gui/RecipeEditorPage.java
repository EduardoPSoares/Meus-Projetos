package com.midgard.fooddecay.gui;

public enum RecipeEditorPage {
    RECIPE("Receita", "&7Configura entrada, saida, tempo e traco."),
    APPEARANCE("Aparencia", "&7Ajusta nome, lore e modelos da saida."),
    REQUIREMENTS("Requisitos", "&7Define condicoes opcionais da receita."),
    MMOCORE("MMOCore", "&7Integra profissao e experiencia do MMOCore.");

    private final String title;
    private final String description;

    RecipeEditorPage(String title, String description) {
        this.title = title;
        this.description = description;
    }

    public String title() {
        return title;
    }

    public String description() {
        return description;
    }

    public int index() {
        return ordinal();
    }

    public static int count() {
        return values().length;
    }

    public static RecipeEditorPage fromIndex(int index) {
        RecipeEditorPage[] pages = values();
        if (index < 0) return pages[0];
        if (index >= pages.length) return pages[pages.length - 1];
        return pages[index];
    }
}
