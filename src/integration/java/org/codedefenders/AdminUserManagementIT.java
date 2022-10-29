/*
 * Copyright (C) 2016-2019 Code Defenders contributors
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
package org.codedefenders;

import java.io.IOException;
import java.lang.reflect.Field;
import java.sql.Connection;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.codedefenders.beans.message.MessagesBean;
import org.codedefenders.database.AdminDAO;
import org.codedefenders.database.DatabaseConnection;
import org.codedefenders.persistence.database.UserRepository;
import org.codedefenders.service.MetricsService;
import org.codedefenders.servlets.admin.AdminSystemSettings;
import org.codedefenders.servlets.admin.AdminUserManagement;
import org.codedefenders.util.EmailUtils;
import org.codedefenders.util.Paths;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.powermock.api.mockito.PowerMockito.mock;

/*
`@PowerMockIgnore` is required to fix some strange exception caused by PowerMock:
java.security.NoSuchAlgorithmException: class configured for TrustManagerFactory: sun.security.ssl.TrustManagerFactoryImpl$PKIXFactory not a TrustManagerFactory

This should be removed if we no longer depend on PowerMockS

See also:
https://stackoverflow.com/questions/14654639/when-a-trustmanagerfactory-is-not-a-trustmanagerfactory-java
 */
@PowerMockIgnore("javax.net.ssl.*")
@Category(DatabaseTest.class)
@RunWith(PowerMockRunner.class)
@PrepareForTest({AdminDAO.class, EmailUtils.class, DatabaseConnection.class})
public class AdminUserManagementIT {

    /**
     * I tried to mock away stuff but at the moment is not possible since basically
     * every class writes to the database So I use a fake databased instead
     *
     * @throws Exception
     */
    // @PrepareForTest({ AdminDAO.class, EmailUtils.class, DatabaseAccess.class,
    // LoginManager.class, Data })
    // @Test
    // public void testCorrentURLinEmail() throws Exception {
    // PowerMockito.mockStatic(DatabaseAccess.class);
    // PowerMockito.when(DatabaseAccess.class, "getUserForName",
    // Mockito.anyString()).thenReturn( null );
    // PowerMockito.when(DatabaseAccess.class, "getUserForEmail",
    // Mockito.anyString()).thenReturn( new User("test", "test",
    // "test@test.test"));
    //
    // PowerMockito.mockStatic(LoginManager.class);
    // PowerMockito.when(LoginManager.class, "validUsername",
    // Mockito.anyString()).thenReturn( true );
    // PowerMockito.when(LoginManager.class, "validPassword",
    // Mockito.anyString()).thenReturn( true );
    //
    // // Mock all the interactions with AdminDao
    // PowerMockito.mockStatic(AdminDAO.class);
    // PowerMockito.when(AdminDAO.class, "getSystemSetting",
    // AdminSystemSettings.SETTING_NAME.EMAILS_ENABLED)
    // .thenReturn(new
    // AdminSystemSettings.SettingsDTO(AdminSystemSettings.SETTING_NAME.EMAILS_ENABLED,
    // true));
    //
    // ArgumentCaptor<String> propertiesCaptor =
    // ArgumentCaptor.forClass(String.class);
    //
    // PowerMockito.mockStatic(EmailUtils.class);
    // PowerMockito.when(EmailUtils.sendEmail( Mockito.anyString(),
    // Mockito.anyString(), propertiesCaptor.capture())).thenReturn( true );
    //
    // HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
    // HttpServletResponse response = Mockito.mock(HttpServletResponse.class);
    // HttpSession mockedHttpSession = Mockito.mock(HttpSession.class);
    //
    // Mockito.when(request.getSession()).thenReturn(mockedHttpSession);
    // Mockito.when(request.getParameter("formType")).thenReturn("createUsers");
    // String userNameList = "user1,12345678\nuser2,12345678";
    // Mockito.when(request.getRequestURL()).thenReturn(new
    // StringBuffer("http://localhost:8080" + Constants.ADMIN_USERS));
    // Mockito.when(request.getParameter("user_name_list")).thenReturn(userNameList);
    //
    // // Capture Response
    // // StringWriter stringWriter = new StringWriter();
    // // PrintWriter writer = new PrintWriter(stringWriter);
    // // when(response.getWriter()).thenReturn(writer);
    //
    // try {
    // new AdminUserManagement().doPost(request, response);
    // } catch (IOException | ServletException e) {
    // e.printStackTrace();
    // Assert.fail("Exception raised");
    // }
    //
    // PowerMockito.verifyStatic();
    // String email = propertiesCaptor.getValue();
    // }
    @Rule
    public DatabaseRule db = new DatabaseRule();

    @Before
    public void mockDBConnections() throws Exception {
        PowerMockito.mockStatic(DatabaseConnection.class);
        PowerMockito.when(DatabaseConnection.getConnection()).thenAnswer((Answer<Connection>) invocation -> {
            // Return a new connection from the rule instead
            return db.getConnection();
        });
    }

    @Test
    public void testCorrentURLinEmail() throws Exception {

//        Validation is done using instances of CodeDefendersValidator now.
//        PowerMockito.mockStatic(LoginManager.class);
//        PowerMockito.when(LoginManager.class, "validUsername", Mockito.anyString()).thenReturn(true);
//        PowerMockito.when(LoginManager.class, "validPassword", Mockito.anyString()).thenReturn(true);

        // Mock all the interactions with AdminDao

        PowerMockito.spy(AdminDAO.class);
        PowerMockito.when(AdminDAO.class, "getSystemSetting", AdminSystemSettings.SETTING_NAME.EMAILS_ENABLED)
                .thenReturn(new AdminSystemSettings.SettingsDTO(AdminSystemSettings.SETTING_NAME.EMAILS_ENABLED, true));

        PowerMockito.when(AdminDAO.class, "getSystemSetting", AdminSystemSettings.SETTING_NAME.EMAIL_ADDRESS)
                .thenReturn(new AdminSystemSettings.SettingsDTO(AdminSystemSettings.SETTING_NAME.EMAIL_ADDRESS,
                        "test@fake.test"));

        PowerMockito.mockStatic(EmailUtils.class);
        PowerMockito.when(EmailUtils.class, "sendEmail", Mockito.anyString(), Mockito.anyString(), Mockito.anyString())
                .thenAnswer((Answer<Boolean>) invocation -> true);

        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        HttpServletResponse response = Mockito.mock(HttpServletResponse.class);
        HttpSession mockedHttpSession = Mockito.mock(HttpSession.class);

        Mockito.when(request.getSession()).thenReturn(mockedHttpSession);
        Mockito.when(request.getParameter("formType")).thenReturn("createUsers");
        String userNameList = "user1,12345678,user1@email.email";
        // \nuser2,12345678,user2@email.email";

        Mockito.when(request.getScheme()).thenReturn("http");
        Mockito.when(request.getServerName()).thenReturn("localhost");
        Mockito.when(request.getServerPort()).thenReturn(8080);
        Mockito.when(request.getContextPath()).thenReturn("/");
        Mockito.when(request.getServletPath()).thenReturn(Paths.ADMIN_USERS);

        Mockito.when(request.getParameter("user_name_list")).thenReturn(userNameList);

        try {
            AdminUserManagement adminUserManagement = new AdminUserManagement();

            Field fieldUserRepo = AdminUserManagement.class.getDeclaredField("userRepo");
            fieldUserRepo.setAccessible(true);
            UserRepository userRepo = new UserRepository(db.getQueryRunner(), mock(MetricsService.class));
            fieldUserRepo.set(adminUserManagement, userRepo);

            Field fieldMessages = AdminUserManagement.class.getDeclaredField("messages");
            fieldMessages.setAccessible(true);
            MessagesBean messagesBean = mock(MessagesBean.class);
            fieldMessages.set(adminUserManagement, messagesBean);

            adminUserManagement.doPost(request, response);
        } catch (IOException | ServletException e) {
            e.printStackTrace();
            Assert.fail("Exception raised");
        }

        //PowerMockito.verifyStatic();
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        PowerMockito.verifyStatic(EmailUtils.class);
        EmailUtils.sendEmail(Mockito.anyString(), Mockito.anyString(), captor.capture());
        String email = captor.getValue();
        Assert.assertFalse(email.contains(Paths.ADMIN_USERS));
    }
}
