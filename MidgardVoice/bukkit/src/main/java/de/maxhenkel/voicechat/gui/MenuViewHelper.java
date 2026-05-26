package de.maxhenkel.voicechat.gui;

import de.maxhenkel.voicechat.range.gui.RangeDistanceMenu;
import de.maxhenkel.voicechat.range.gui.RangeGlobalAddMenu;
import de.maxhenkel.voicechat.range.gui.RangeGlobalMenu;
import de.maxhenkel.voicechat.range.gui.RangeListMenu;
import de.maxhenkel.voicechat.range.gui.RangePlayerSelectMenu;
import de.maxhenkel.voicechat.zone.gui.GlobalPlayerListMenu;
import de.maxhenkel.voicechat.zone.gui.GlobalSettingsMenu;
import de.maxhenkel.voicechat.zone.gui.ZoneAddPlayerMenu;
import de.maxhenkel.voicechat.zone.gui.ZoneAllowedPlayersMenu;
import de.maxhenkel.voicechat.zone.gui.ZoneListMenu;
import de.maxhenkel.voicechat.zone.gui.ZoneMutedPlayersMenu;
import de.maxhenkel.voicechat.zone.gui.ZoneSettingsMenu;

public final class MenuViewHelper {

    private static final String[] DISPLAY_PREFIXES = {
            "* ",
            "\u2726 ",
            "\u25CF ",
            "\u00BB ",
            "âœ¦ "
    };

    private MenuViewHelper() {
    }

    public static String stripDecorativePrefix(String value) {
        if (value == null) {
            return null;
        }

        String result = value.trim();
        boolean changed;
        do {
            changed = false;
            for (String prefix : DISPLAY_PREFIXES) {
                if (result.startsWith(prefix)) {
                    result = result.substring(prefix.length()).trim();
                    changed = true;
                }
            }
        } while (changed);

        return result;
    }

    public static boolean isPluginInventoryTitle(String title) {
        if (title == null) {
            return false;
        }

        return title.equals(AdminHubMenu.getTitle())
                || title.equals(CooldownMenu.getTitle())
                || title.equals(RecordingMenu.getTitle())
                || title.equals(RecordingMenu.getSavedTitle())
                || title.equals(RecordingMenu.getStartTitle())
                || title.equals(VolumePriorityMenu.getTitle())
                || title.equals(VolumePriorityMenu.getSelectVolumeTitle())
                || title.equals(VolumePriorityMenu.getSelectPriorityTitle())
                || title.startsWith(VolumePriorityMenu.getVolumeSelectTitlePrefix())
                || title.startsWith(VolumePriorityMenu.getPrioritySelectTitlePrefix())
                || title.startsWith(StageSpeakersMenu.getTitlePrefix())
                || title.startsWith(StageSpeakersMenu.getAddTitlePrefix())
                || title.equals(ZoneListMenu.getTitle())
                || title.equals(GlobalSettingsMenu.getTitle())
                || title.startsWith(ZoneSettingsMenu.getTitlePrefix())
                || title.startsWith(ZoneAllowedPlayersMenu.getTitlePrefix())
                || title.startsWith(ZoneMutedPlayersMenu.getTitlePrefix())
                || title.startsWith(ZoneAddPlayerMenu.getTitleAllowedPrefix())
                || title.startsWith(ZoneAddPlayerMenu.getTitleMutePrefix())
                || title.equals(GlobalPlayerListMenu.getAllowedTitle())
                || title.equals(GlobalPlayerListMenu.getMutedTitle())
                || title.equals(GlobalPlayerListMenu.getSpeakersTitle())
                || title.equals(GlobalPlayerListMenu.getAddAllowedTitle())
                || title.equals(GlobalPlayerListMenu.getAddMutedTitle())
                || title.equals(GlobalPlayerListMenu.getAddSpeakerTitle())
                || title.equals(RangeListMenu.getTitle())
                || title.equals(RangePlayerSelectMenu.getTitlePrefix())
                || title.startsWith(RangeDistanceMenu.getTitlePrefix())
                || title.equals(RangeGlobalMenu.getTitle())
                || title.equals(RangeGlobalAddMenu.getTitle());
    }
}
