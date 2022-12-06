package org.codedefenders.dto.api;

import java.util.ArrayList;
import java.util.List;

public class MultiplayerScoreboard {
    List<AttackerScore> attackers = new ArrayList<>();
    AttackerScore attackersTotal;
    List<DefenderScore> defenders = new ArrayList<>();
    DefenderScore defendersTotal;

    public void addAttacker(AttackerScore attacker) {
        attackers.add(attacker);
    }

    public void addDefender(DefenderScore defender) {
        defenders.add(defender);
    }

    public void setAttackersTotal(AttackerScore attackersTotal) {
        this.attackersTotal = attackersTotal;
    }

    public void setDefendersTotal(DefenderScore defendersTotal) {
        this.defendersTotal = defendersTotal;
    }
}
