package com.catadmirer.infuseSMP.util.trust;

import com.booksaw.betterTeams.Team;
import com.booksaw.betterTeams.TeamPlayer;
import com.catadmirer.infuseSMP.Infuse;
import org.jspecify.annotations.NullMarked;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@NullMarked
public class BetterTeamsTrustManager implements TrustManager {
    private final Infuse plugin = Infuse.getInstance();

    /**
     * Gets the set of unique ids of all players in the player's team.
     *
     * @param player The player to get the trusted players of.
     */
    @Override
    public Set<UUID> getTrusted(UUID player) {
        Team team = Team.getTeam(player);
        if (team == null) return Set.of();

        return team.getMembers().get().stream().map(TeamPlayer::getPlayerUUID).collect(Collectors.toSet());
    }

    /** @throws UnsupportedOperationException Is not intended to be used.  Use regular BetterTeams methods for this functionality. */
    @Override
    public void setTrusted(UUID player, Set<UUID> trusted) {
        throw new UnsupportedOperationException("Modify a player's team to set trusted players");
    }

    /** @throws UnsupportedOperationException Is not intended to be used.  Use regular BetterTeams methods for this functionality. */
    @Override
    public void addTrust(UUID player, UUID trusted) {
        throw new UnsupportedOperationException("Modify a player's team to add trusted players");
    }

    /** @throws UnsupportedOperationException Is not intended to be used.  Use regular BetterTeams methods for this functionality. */
    @Override
    public void removeTrust(UUID player, UUID trusted) {
        throw new UnsupportedOperationException("Modify a player's team to remove trusted players");
    }

    /**
     * Checks if one player trusts another.
     * If the config allows, it allows allied teams to be trusted.
     *
     * @param player The player whose trust list will be checked.
     * @param trusted The player to check for in the trust list.
     * @return True if 'player' is on the same team as 'trusted', or if they are allies.  False otherwise.
     */
    @Override
    public boolean doesTrust(UUID player, UUID trusted) {
        Team team = Team.getTeam(player);
        if (team == null) return false;

        if (getTrusted(player).contains(trusted)) return true;

        if (!plugin.getMainConfig().betterTeamsTrustAllies()) return false;

        Team otherTeam = Team.getTeam(trusted);
        if (otherTeam == null) return false;

        return team.isAlly(trusted);
    }
}
