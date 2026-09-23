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
package org.codedefenders.arcade.mutle.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeParseException;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.codedefenders.configuration.Configuration;
import org.codedefenders.database.AdminDAO;
import org.codedefenders.servlets.admin.AdminSystemSettings;

/** Runtime configuration for Mutle availability and fixture rotation. */
@Named("mutleSettings")
@ApplicationScoped
public class MutleSettings {
    private final Configuration configuration;

    @Inject
    public MutleSettings(Configuration configuration) {
        this.configuration = configuration;
    }

    public boolean enabled() {
        return AdminDAO.getSystemSetting(AdminSystemSettings.SETTING_NAME.ALLOW_MUTLE_SECTION).getBoolValue();
    }

    public MutleRotation rotation(Clock clock) {
        try {
            Instant start = Instant.parse(configuration.getMutleRotationStart());
            Duration period = Duration.ofDays(configuration.getMutleRotationPeriodDays());
            return new MutleRotation(start, period, clock);
        } catch (DateTimeParseException | ArithmeticException e) {
            throw new IllegalStateException("Invalid Mutle rotation configuration.", e);
        }
    }
}
