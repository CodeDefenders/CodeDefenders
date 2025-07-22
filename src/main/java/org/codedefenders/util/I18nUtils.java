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
package org.codedefenders.util;

import java.util.Locale;

import org.xnap.commons.i18n.I18n;
import org.xnap.commons.i18n.I18nFactory;

import static org.xnap.commons.i18n.I18nFactory.FALLBACK;
import static org.xnap.commons.i18n.I18nFactory.READ_PROPERTIES;

public class I18nUtils {

    public static I18n getI18n(Locale pLocale) {
        return I18nFactory.getI18n(
                I18nUtils.class,
                pLocale == null ? Locale.getDefault() : pLocale,
                FALLBACK | READ_PROPERTIES
        );
    }
}
