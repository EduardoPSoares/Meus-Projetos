package midgardvanish.nms;

import net.minecraft.ChatFormatting;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundSetPlayerTeamPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.Team;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;

import java.util.List;

public class NMSHandler {

    private static final String GLOW_TEAM_NAME = "mv_glow";
    private static final String HIDDEN_TEAM_NAME = "mv_hidden";

    private ServerGamePacketListenerImpl getConnection(Player player) {
        return ((CraftPlayer) player).getHandle().connection;
    }

    public void sendGlowing(Player viewer, Player target) {
        var nmsTarget = ((CraftPlayer) target).getHandle();
        int entityId = nmsTarget.getId();

        byte flags = buildEntityFlags(nmsTarget, true);

        List<SynchedEntityData.DataValue<?>> values = List.of(
                SynchedEntityData.DataValue.create(
                        new EntityDataAccessor<>(0, EntityDataSerializers.BYTE), flags
                )
        );

        getConnection(viewer).send(new ClientboundSetEntityDataPacket(entityId, values));
    }

    public void removeGlowing(Player viewer, Player target) {
        var nmsTarget = ((CraftPlayer) target).getHandle();
        int entityId = nmsTarget.getId();

        byte flags = buildEntityFlags(nmsTarget, false);

        List<SynchedEntityData.DataValue<?>> values = List.of(
                SynchedEntityData.DataValue.create(
                        new EntityDataAccessor<>(0, EntityDataSerializers.BYTE), flags
                )
        );

        getConnection(viewer).send(new ClientboundSetEntityDataPacket(entityId, values));
    }

    private byte buildEntityFlags(net.minecraft.world.entity.Entity entity, boolean glowing) {
        byte flags = 0;
        if (entity.isOnFire()) flags |= 0x01;
        if (entity.isShiftKeyDown()) flags |= 0x02;
        if (entity.isSprinting()) flags |= 0x08;
        if (entity.isSwimming()) flags |= 0x10;
        if (entity.isInvisible()) flags |= 0x20;
        if (glowing) flags |= 0x40;
        if (entity instanceof LivingEntity le && le.isFallFlying()) flags |= (byte) 0x80;
        return flags;
    }

    public void createGlowTeam(Player viewer) {
        PlayerTeam team = new PlayerTeam(new Scoreboard(), GLOW_TEAM_NAME);
        team.setColor(ChatFormatting.RED);
        team.setNameTagVisibility(Team.Visibility.ALWAYS);

        getConnection(viewer).send(ClientboundSetPlayerTeamPacket.createAddOrModifyPacket(team, true));
    }

    public void addToGlowTeam(Player viewer, String playerName) {
        PlayerTeam team = new PlayerTeam(new Scoreboard(), GLOW_TEAM_NAME);
        getConnection(viewer).send(
                ClientboundSetPlayerTeamPacket.createPlayerPacket(team, playerName, ClientboundSetPlayerTeamPacket.Action.ADD)
        );
    }

    public void removeFromGlowTeam(Player viewer, String playerName) {
        PlayerTeam team = new PlayerTeam(new Scoreboard(), GLOW_TEAM_NAME);
        getConnection(viewer).send(
                ClientboundSetPlayerTeamPacket.createPlayerPacket(team, playerName, ClientboundSetPlayerTeamPacket.Action.REMOVE)
        );
    }

    public void removeGlowTeam(Player viewer) {
        PlayerTeam team = new PlayerTeam(new Scoreboard(), GLOW_TEAM_NAME);
        getConnection(viewer).send(ClientboundSetPlayerTeamPacket.createRemovePacket(team));
    }

    public void createHiddenTeam(Player viewer) {
        PlayerTeam team = new PlayerTeam(new Scoreboard(), HIDDEN_TEAM_NAME);
        team.setNameTagVisibility(Team.Visibility.NEVER);
        getConnection(viewer).send(ClientboundSetPlayerTeamPacket.createAddOrModifyPacket(team, true));
    }

    public void addToHiddenTeam(Player viewer, String playerName) {
        PlayerTeam team = new PlayerTeam(new Scoreboard(), HIDDEN_TEAM_NAME);
        getConnection(viewer).send(
                ClientboundSetPlayerTeamPacket.createPlayerPacket(team, playerName, ClientboundSetPlayerTeamPacket.Action.ADD)
        );
    }

    public void removeHiddenTeam(Player viewer) {
        PlayerTeam team = new PlayerTeam(new Scoreboard(), HIDDEN_TEAM_NAME);
        getConnection(viewer).send(ClientboundSetPlayerTeamPacket.createRemovePacket(team));
    }
}
