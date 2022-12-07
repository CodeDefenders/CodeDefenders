package org.codedefenders.dto.api;

import java.util.ArrayList;
import java.util.List;

public class MeleeScoreboard {
    List<MeleeScore> players = new ArrayList<>();
    public void addPlayer(MeleeScore player) {
        players.add(player);
    }
}
