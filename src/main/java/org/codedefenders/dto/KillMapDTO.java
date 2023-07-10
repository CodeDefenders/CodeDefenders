package org.codedefenders.dto;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.codedefenders.database.KillmapDAO;
import org.codedefenders.execution.KillMap;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class KillMapDTO {
    private final Map<Integer, Map<Integer, KillMap.KillMapEntry.Status>> killMapForMutants;

    public KillMapDTO(int gameId) {
        killMapForMutants = getKillMapForMutants(KillmapDAO.getKillMapEntriesForGame(gameId));
    }

    private Map<Integer, Map<Integer, KillMap.KillMapEntry.Status>> getKillMapForMutants(
            List<KillMap.KillMapEntry> killMap) {
        Map<Integer, Map<Integer, KillMap.KillMapEntry.Status>> killMapForMutants = new HashMap<>();
        for (KillMap.KillMapEntry entry : killMap) {
            if (!killMapForMutants.containsKey(entry.mutant.getId())) {
                killMapForMutants.put(entry.mutant.getId(), new HashMap<>());
            }
            killMapForMutants.get(entry.mutant.getId()).put(entry.test.getId(), entry.status);
        }
        return killMapForMutants;
    }

    public Optional<Map<Integer, KillMap.KillMapEntry.Status>> getKillMapForMutant(int mutantId) {
        return Optional.ofNullable(killMapForMutants.get(mutantId));
    }

    public String getKillMapForMutantsAsJSON() {
        Gson gson = new GsonBuilder()
                .excludeFieldsWithoutExposeAnnotation()
                .create();
        return gson.toJson(killMapForMutants);
    }
}
