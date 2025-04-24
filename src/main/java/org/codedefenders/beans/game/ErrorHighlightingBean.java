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
package org.codedefenders.beans.game;

import java.util.List;

import jakarta.enterprise.context.RequestScoped;

import com.google.gson.Gson;

/**
 * <p>Provides data for the error highlighting game component.</p>
 * <p>Bean Name: {@code errorHighlighting}</p>
 */
@RequestScoped
public class ErrorHighlightingBean {
    /**
     * The line numbers of the errors reported by the compiler.
     */
    private List<Integer> errorLines;

    public ErrorHighlightingBean() {
        errorLines = null;
    }

    public void setErrorLines(List<Integer> errorLines) {
        this.errorLines = errorLines;
    }

    // --------------------------------------------------------------------------------

    public boolean hasErrorLines() {
        return errorLines != null;
    }

    public String getErrorLinesJSON() {
        if (errorLines == null) {
            return "[]";
        } else {
            return new Gson().toJson(errorLines);
        }
    }
}
