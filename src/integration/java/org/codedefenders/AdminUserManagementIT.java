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

import java.lang.reflect.Field;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.codedefenders.beans.message.MessagesBean;
import org.codedefenders.database.AdminDAO;
import org.codedefenders.instrumentation.MetricsRegistry;
import org.codedefenders.persistence.database.UserRepository;
import org.codedefenders.persistence.database.util.QueryRunner;
import org.codedefenders.servlets.admin.AdminSystemSettings;
import org.codedefenders.servlets.admin.AdminUserManagement;
import org.codedefenders.util.DatabaseExtension;
import org.codedefenders.util.EmailUtils;
import org.codedefenders.util.Paths;
import org.codedefenders.util.URLUtils;
import org.codedefenders.util.tags.DatabaseTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.stubbing.Answer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

@DatabaseTest
@ExtendWith(DatabaseExtension.class)
public class AdminUserManagementIT {

    @Test
    public void testCorrectURLinEmail(QueryRunner queryRunner) throws Exception {
        String userNameList = "user1,12345678,user1@email.email";

        try (var mockedAdminDAO = mockStatic(AdminDAO.class);
             var mockedEmailUtils = mockStatic(EmailUtils.class)) {
            mockedAdminDAO.when(() -> AdminDAO.getSystemSetting(AdminSystemSettings.SETTING_NAME.EMAILS_ENABLED))
                    .thenReturn(new AdminSystemSettings.SettingsDTO(AdminSystemSettings.SETTING_NAME.EMAILS_ENABLED, true));
            mockedAdminDAO.when(() -> AdminDAO.getSystemSetting(AdminSystemSettings.SETTING_NAME.EMAIL_ADDRESS))
                    .thenReturn(new AdminSystemSettings.SettingsDTO(AdminSystemSettings.SETTING_NAME.EMAIL_ADDRESS, "test@fake.test"));
            mockedAdminDAO.when(() -> AdminDAO.getSystemSetting(AdminSystemSettings.SETTING_NAME.MIN_PASSWORD_LENGTH))
                    .thenReturn(new AdminSystemSettings.SettingsDTO(AdminSystemSettings.SETTING_NAME.MIN_PASSWORD_LENGTH, 8));
            mockedEmailUtils.when(() -> EmailUtils.sendEmail(anyString(), anyString(), anyString()))
                    .thenAnswer((Answer<Boolean>) invocation -> true);

            HttpServletRequest request = mock(HttpServletRequest.class);
            HttpServletResponse response = mock(HttpServletResponse.class);
            HttpSession mockedHttpSession = mock(HttpSession.class);

            when(request.getSession()).thenReturn(mockedHttpSession);
            when(request.getParameter("formType")).thenReturn("createUsers");
            when(request.getScheme()).thenReturn("http");
            when(request.getServerName()).thenReturn("localhost");
            when(request.getServerPort()).thenReturn(8080);
            when(request.getContextPath()).thenReturn("/");
            when(request.getServletPath()).thenReturn(Paths.ADMIN_USERS);
            when(request.getParameter("user_name_list")).thenReturn(userNameList);

            AdminUserManagement adminUserManagement = new AdminUserManagement();

            Field fieldUserRepo = AdminUserManagement.class.getDeclaredField("userRepo");
            fieldUserRepo.setAccessible(true);
            UserRepository userRepo = new UserRepository(queryRunner, mock(MetricsRegistry.class));
            fieldUserRepo.set(adminUserManagement, userRepo);

            Field fieldMessages = AdminUserManagement.class.getDeclaredField("messages");
            fieldMessages.setAccessible(true);
            MessagesBean messagesBean = mock(MessagesBean.class);
            fieldMessages.set(adminUserManagement, messagesBean);

            Field fieldPasswordEncoder = AdminUserManagement.class.getDeclaredField("passwordEncoder");
            fieldPasswordEncoder.setAccessible(true);
            PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
            fieldPasswordEncoder.set(adminUserManagement, passwordEncoder);

            Field fieldURLUtils = AdminUserManagement.class.getDeclaredField("url");
            fieldURLUtils.setAccessible(true);
            URLUtils url = mock(URLUtils.class);
            fieldURLUtils.set(adminUserManagement, url);
            when(url.getAbsoluteURLForPath("/")).thenReturn("http://localhost:8080/");

            adminUserManagement.doPost(request, response);


            ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
            mockedEmailUtils.verify(() -> EmailUtils.sendEmail(anyString(), anyString(), captor.capture()));
            String email = captor.getValue();
            assertFalse(email.contains(Paths.ADMIN_USERS));
        }
    }
}
