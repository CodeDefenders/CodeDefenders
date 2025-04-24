/*
 * Copyright (C) 2016-2025 Code Defenders contributors
 *
 * This file is part of Code Defenders.
 *
 * Code Defenders is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * Code Defenders is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Code Defenders. If not, see <http://www.gnu.org/licenses/>.
 */
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
