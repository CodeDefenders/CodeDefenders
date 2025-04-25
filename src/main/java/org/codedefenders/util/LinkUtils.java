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

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.codedefenders.servlets.UserProfileManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class offers utility methods to create hyperlinks/ HTML anchor-tags.
 */
public final class LinkUtils {
    private static final Logger logger = LoggerFactory.getLogger(LinkUtils.class);

    /**
     * Translates a string into application/x-www-form-urlencoded format.
     *
     * @param stringToEncode the string that should get encoded
     * @return the encoded string
     */
    public static String urlEncode(String stringToEncode) {
        try {
            return URLEncoder.encode(
                    stringToEncode,
                    StandardCharsets.UTF_8.toString() // use Charset instead of String for Java version >= 10
            );
        } catch (UnsupportedEncodingException e) {
            logger.error("Error while URL-encoding the string \"" + stringToEncode + "\"", e);
            return null;
        }
    }

    /**
     * Returns the URL to the user profile of the given username.
     *
     * @param username The username for which the link will be created
     * @return the URL to the user profile
     */
    public static String getUserProfileHyperlink(String username) {
        return CDIUtil.getBeanFromCDI(URLUtils.class).forPath(Paths.USER_PROFILE) + "?user=" + urlEncode(username);
    }

    /**
     * Returns an HTML anchor tag with the username as content and the hyperlink to the corresponding user profile as
     * href-attribute if public user profiles are enabled in the admin settings. Returns only the username if disabled.
     *
     * @param username The username for which the link will be created
     * @return HTML code containing an anchor-tag or plain text
     */
    public static String getUserProfileAnchorOrText(String username) {
        if (UserProfileManager.isProfilePublic()) {
            return "<a href=\"" + getUserProfileHyperlink(username) + "\">" + username + "</a>";
        } else {
            return username;
        }
    }
}
