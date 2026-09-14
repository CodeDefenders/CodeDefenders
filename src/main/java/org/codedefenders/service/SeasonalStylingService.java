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
package org.codedefenders.service;

import java.util.HashMap;
import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.codedefenders.util.URLUtils;

@ApplicationScoped
@Named("seasonStyle")
public class SeasonalStylingService {
    private final Map<String, Map<String, String>> mappings;

    @Inject
    URLUtils url;

    public SeasonalStylingService() {
        mappings = new HashMap<>();

        HashMap<String, String> christmas = new HashMap<>();
        christmas.put("/images/ingameicons/equivalence.png", "/images/seasonal/christmasEquivalence.png");

        mappings.put("christmas-season", christmas);
    }

    /**
     * Returns the path of a season-specific version of an image, or the original image if no such path exists.
     * @param originalPath The path to the normal version of the image. Must start with a "/".
     * @param season The style string for a season. This is also the name of the associated CSS class.
     */
    public String getPath(String originalPath, String season) {
        if (season.isBlank()) {
            return url.forPath(originalPath);
        }
        Map<String, String> seasonMap = mappings.get(season);
        if (seasonMap != null) {
            return url.forPath(seasonMap.getOrDefault(originalPath, originalPath));
        } else {
            return originalPath;
        }
    }
}
