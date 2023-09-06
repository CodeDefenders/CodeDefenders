/*
 * Copyright (C) 2023 Code Defenders contributors
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

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Optional;

import org.codedefenders.configuration.Configuration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class URLUtilsTest {

    private URLUtils urlUtilsEmpty;

    @BeforeEach
    public void setup() {
        urlUtilsEmpty = new URLUtils((new Configuration(){
            @Override
            public Optional<URL> getApplicationURL() {
                return Optional.empty();
            }
        }));
    }

    @Test
    public void throwsExceptionWithoutUrl() {
        assertThrows(RuntimeException.class, () -> urlUtilsEmpty.forPath("/"));
    }

    @Test
    public void canNotSetAppUrlMultipleTimes() throws MalformedURLException {
        String url = "https://example.org/";

        urlUtilsEmpty.setAppURI(new URL(url));
        assertEquals(url, urlUtilsEmpty.getAbsoluteURLForPath("/"));

        urlUtilsEmpty.setAppURI(new URL("https://example.org/alternative/"));
        assertEquals(url, urlUtilsEmpty.getAbsoluteURLForPath("/"));
    }

    @Test
    public void loadsAppUrlFromConfig() throws MalformedURLException {
        String url = "http://example.org/";
        URL applicationURL = new URL(url);

        URLUtils urlUtils = new URLUtils(new Configuration(){
            @Override
            public Optional<URL> getApplicationURL() {
                return Optional.of(applicationURL);
            }
        });

        assertEquals(url, urlUtils.getAbsoluteURLForPath("/"));
    }

    @Test
    public void appUrlAlwaysEndsWithSlash() throws MalformedURLException {
        String url = "https://example.org/subpath";
        urlUtilsEmpty.setAppURI(new URL(url));

        assertEquals(url + "/", urlUtilsEmpty.getAbsoluteURLForPath("/"));
    }

    @Test
    public void stripsLeadingSlashes() throws MalformedURLException {
        urlUtilsEmpty.setAppURI(new URL("https://example.org/subpath"));

        assertEquals("/subpath/path", urlUtilsEmpty.forPath("///path"));
    }
}
