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

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Optional;

import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.servlet.ServletContext;

import org.codedefenders.configuration.Configuration;
import org.xnap.commons.i18n.I18n;

/**
 * A helper class for constructing relative and absolute URLs for paths in our application
 * for usage from Java and JSP code.
 */
@Named("url")
@ApplicationScoped
public class URLUtils {

    private final ServletContext servletContext;
    private Optional<URI> appURI = Optional.empty();

    @Inject
    public URLUtils(@SuppressWarnings("CdiInjectionPointsInspection") Configuration config,
                    ServletContext servletContext) {
        config.getApplicationURL().ifPresent(this::setAppURI);
        this.servletContext = servletContext;
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
     * Constructs a root-relative URL from the given context-relative URL.
     * If a localized version (name_languageCode.ext) exists, the URL to this file is given instead.
     *
     * @param path A context-relative URL with or without leading slash(es). Will be used as fallback if no localized version of the file exists
     * @param i18n The requests i18n instance. Used to determine the language.
     * @return A root-relative URL.
     */
    @Nonnull
    public String forPathLocalized(@Nonnull String path, I18n i18n) {
        var ext = path.substring(path.lastIndexOf("."));
        var base = path.substring(0, path.lastIndexOf("."));
        var lang = i18n.getLocale().getLanguage();
        var localizedPath = base + '_' + lang + ext;
        var realPathStr = servletContext.getRealPath(localizedPath);
        var existingPath = Files.exists(Paths.get(realPathStr)) ? localizedPath : path;
        return forPath(existingPath);
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
