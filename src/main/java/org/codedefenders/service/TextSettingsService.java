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

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.codedefenders.dto.TextSetting;
import org.codedefenders.persistence.database.TextSettingsRepository;

@ApplicationScoped
public class TextSettingsService {

    private final TextSettingsRepository textSettingsRepository;

    @Inject
    public TextSettingsService(TextSettingsRepository textSettingsRepository) {
        this.textSettingsRepository = textSettingsRepository;
    }

    public List<TextSetting> getAllTextSettings(String language) {
        // get all with values for the current language
        var settings = textSettingsRepository.getTextSettings(language);

        // add any missing settings with empty value
        for (var settingName : TextSetting.SETTING_NAME.values()) {
            if (settings.stream().noneMatch(s -> s.name() == settingName)) {
                settings.add(new TextSetting(settingName, language, ""));
            }
        }

        return settings;
    }

    public boolean updateTextSetting(TextSetting setting) {
        return textSettingsRepository.updateTextSetting(setting);
    }
}
