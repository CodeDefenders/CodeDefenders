package org.codedefenders.service;

import javax.enterprise.inject.Specializes;
import javax.inject.Inject;
import javax.validation.ValidationException;

import org.codedefenders.auth.annotation.RequiresPermission;
import org.codedefenders.auth.permissions.CreateClassroomPermission;
import org.codedefenders.persistence.database.ClassroomMemberRepository;
import org.codedefenders.persistence.database.ClassroomRepository;
import org.codedefenders.util.URLUtils;
import org.springframework.security.crypto.password.PasswordEncoder;

@Specializes
public class SecureClassroomService extends ClassroomService {

    @Inject
    public SecureClassroomService(ClassroomRepository classroomRepository,
                                  ClassroomMemberRepository memberRepository,
                                  URLUtils urlUtils,
                                  AuthService login,
                                  PasswordEncoder passwordEncoder) {
        super(classroomRepository, memberRepository, urlUtils, login, passwordEncoder);
    }

    @Override
    @RequiresPermission(CreateClassroomPermission.name)
    public int addClassroom(String name) throws ValidationException {
        return super.addClassroom(name);
    }
}
