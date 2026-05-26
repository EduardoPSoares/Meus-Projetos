package com.midgard.fooddecay.gui;

import com.midgard.fooddecay.FoodTrait;

import java.util.ArrayList;
import java.util.List;

public final class RecipeEditorSaveValidator {

    private RecipeEditorSaveValidator() {
    }

    public static ValidationResult validate(
            boolean hasInput,
            boolean hasOutput,
            int timeMinutes,
            FoodTrait trait,
            String profession,
            int professionLevel,
            String experienceProfession,
            double experienceReward
    ) {
        List<String> missing = new ArrayList<>();

        if (!hasInput) {
            missing.add("Entrada");
        }
        if (!hasOutput) {
            missing.add("Saida");
        }
        if (timeMinutes <= 0) {
            missing.add("Tempo de processamento");
        }
        if (trait == null) {
            missing.add("Traco aplicado");
        }
        if (profession != null && professionLevel <= 0) {
            missing.add("Nivel minimo da profissao");
        }
        if (experienceReward > 0D && experienceProfession == null) {
            missing.add("Profissao da experiencia");
        }

        return new ValidationResult(missing, firstMissingPage(missing));
    }

    private static RecipeEditorPage firstMissingPage(List<String> missing) {
        for (String field : missing) {
            if ("Entrada".equals(field)
                    || "Saida".equals(field)
                    || "Tempo de processamento".equals(field)
                    || "Traco aplicado".equals(field)) {
                return RecipeEditorPage.RECIPE;
            }
            if ("Profissao da experiencia".equals(field)
                    || "Nivel minimo da profissao".equals(field)) {
                return RecipeEditorPage.MMOCORE;
            }
        }
        return RecipeEditorPage.RECIPE;
    }

    public record ValidationResult(List<String> missingRequirements, RecipeEditorPage firstMissingPage) {
        public boolean hasMissingRequirements() {
            return !missingRequirements.isEmpty();
        }
    }
}
