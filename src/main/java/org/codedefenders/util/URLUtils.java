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
 * A helper class for constructing relative and absolute URLs for paths in our application
 * for usage from Java and JSP code.
 */
@Named("url")
@ApplicationScoped
public class URLUtils {

    private Optional<URI> appURI = Optional.empty();

    @Inject
    public URLUtils(@SuppressWarnings("CdiInjectionPointsInspection") Configuration config) {
        config.getApplicationURL().ifPresent(this::setAppURI);
    }

    /**
     * Retrieves the protocol, host, port and path components of the passed {@link URL}
     * to construct the application url.
     */
    public synchronized void setAppURI(@Nonnull URL url) {
        if (appURI.isEmpty()) {
            try {
                // Ensure path always ends with a leading "/"
                String path = url.getPath() + (url.getPath().endsWith("/") ? "" : "/");
                appURI = Optional.of(new URI(url.getProtocol(), null, url.getHost(), url.getPort(), path,
                        null, null).normalize());
            } catch (URISyntaxException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * Constructs a root-relative URL from the given context-relative URL.
     *
     * <p>This should be used for all links in JSP pages and redirects.
     *
     * <p>A context-relative URL is relative to the path we consider the root of our application (the landing page)
     * and might contain a leading slash.<br>
     * A root-relative URL is the full path component (with any necessary parents) and will always have a leading
     * slash.
     *
     * @param path A context-relative URL with or without leading slash(es)
     * @return A root-relative URL
     */
    @Nonnull
    public String forPath(@Nonnull String path) {
        return getURIForPath(path).getPath();
    }

    /**
     * Constructs an absolute URL (with protocol, host, and port if necessary) for the given path.
     *
     * <p>This should only be used if the full URL is really necessary (e.g. when generating links that can/will be
     * distributed outside our application, e.g. via e-mail)
     */
    @Nonnull
    public String getAbsoluteURLForPath(@Nonnull String path) {
        return getURIForPath(path).toString();
    }

    @Nonnull
    private URI getURIForPath(@Nonnull String path) {
        if (path.startsWith("/")) {
            return getURIForPath(path.substring(1));
        } else {
            return appURI.orElseThrow(RuntimeException::new).resolve(path);
        }
    }
}
