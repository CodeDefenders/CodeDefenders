/*
 * Copyright (C) 2021,2022 Code Defenders contributors
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

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Optional;

import javax.annotation.Nonnull;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.codedefenders.configuration.Configuration;

/**
 *
 */
@Named("url")
@ApplicationScoped
public class URLUtils {

    private Optional<URI> appURI = Optional.empty();

    @Inject
    public URLUtils(@SuppressWarnings("CdiInjectionPointsInspection") Configuration config) {
        // TODO(Alex): How to fallback to parsing the request?
        config.getApplicationURL().ifPresent(this::setAppURI);
    }

    public synchronized void setAppURI(@Nonnull URL url) {
        if (!appURI.isPresent()) {
            try {
                // Ensure path always ends with a leading "/"
                String path = url.getPath() + (url.getPath().endsWith("/") ? "" : "/");
                appURI = Optional.of(new URI(url.getProtocol(), null, url.getHost(), url.getPort(), path, null, null).normalize());
            } catch (URISyntaxException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * @param path A context-relative URL
     * @return A root-relative URL
     */
    @Nonnull
    public String forPath(@Nonnull String path) {
        return getURIForPath(path).getPath();
    }

    public String getAbsoluteURLForPath(@Nonnull String path) {
        return getURIForPath(path).toString();
    }

    private URI getURIForPath(@Nonnull String path) {
        if (path.startsWith("/")) {
            return getURIForPath(path.substring(1));
        } else {
            return appURI.orElseThrow(RuntimeException::new).resolve(path);
        }
    }
}
