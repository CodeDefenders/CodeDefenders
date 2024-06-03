package org.codedefenders.beans.contact;

import org.codedefenders.auth.CodeDefendersAuth;
import org.codedefenders.database.AdminDAO;
import org.codedefenders.servlets.admin.AdminSystemSettings;
import org.codedefenders.util.LinkUtils;
import org.codedefenders.util.URLUtils;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

@RequestScoped
@Named("teacherApplicationTemplate")
public class TeacherApplicationTemplate {
    private final CodeDefendersAuth login;
    private final URLUtils url;

    @Inject
    public TeacherApplicationTemplate(CodeDefendersAuth login, URLUtils url) {
        this.login = login;
        this.url = url;
    }

    public String getMailtoLink() {
        String urlEncodedLink = "mailto:%s?subject=%s&body=%s".formatted(
                LinkUtils.urlEncode(getAddress()),
                LinkUtils.urlEncode(getSubject()),
                LinkUtils.urlEncode(getBody())
        );
        return urlEncodedLink.replaceAll("\\+", "%20");
    }

    public String getAddress() {
        return AdminDAO.getSystemSetting(AdminSystemSettings.SETTING_NAME.TEACHER_APPLICATIONS_EMAIL).getStringValue();
    }

    public String getSubject() {
        return "Teacher Account Application for " + login.getSimpleUser().getName();
    }

    public String getBody() {
        return """
                Greetings,

                I would like to request a teacher account for %s on %s.

                Regards,
                %s
                """.stripIndent().formatted(
                        login.getSimpleUser().getName(),
                        getShortAbsoluteUri(),
                        login.getSimpleUser().getName());
    }

    private String getShortAbsoluteUri() {
        return url.getAbsoluteURLForPath("/")
                .replaceFirst("^https?://", "")
                .replaceFirst("/$", "");
    }
}
