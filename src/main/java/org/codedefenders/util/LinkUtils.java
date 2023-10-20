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
