package org.codedefenders.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.validation.ValidationException;

import org.codedefenders.database.UncheckedSQLException;
import org.codedefenders.model.Classroom;
import org.codedefenders.model.ClassroomMember;
import org.codedefenders.model.ClassroomRole;
import org.codedefenders.persistence.database.ClassroomMemberRepository;
import org.codedefenders.persistence.database.ClassroomRepository;
import org.codedefenders.transaction.Transactional;
import org.codedefenders.util.Paths;
import org.codedefenders.util.URLUtils;
import org.springframework.security.crypto.password.PasswordEncoder;

@Transactional
@ApplicationScoped
public class ClassroomService {

    private final ClassroomRepository classroomRepository;
    private final ClassroomMemberRepository memberRepository;
    private final URLUtils url;

    private final AuthService login;
    private final PasswordEncoder passwordEncoder;

    @Inject
    public ClassroomService(ClassroomRepository classroomRepository,
                            ClassroomMemberRepository memberRepository,
                            URLUtils urlUtils,
                            AuthService login,
                            PasswordEncoder passwordEncoder) {
        this.classroomRepository = classroomRepository;
        this.memberRepository = memberRepository;
        this.url = urlUtils;
        this.login = login;
        this.passwordEncoder = passwordEncoder;
    }

    public int addClassroom(String name) throws ValidationException {
        validateName(name);

        Classroom classroom = new Classroom(
                -1,
                UUID.randomUUID(),
                login.getUserId(),
                name,
                null,
                false,
                false,
                false
        );

        int id = classroomRepository.storeClassroom(classroom)
                .orElseThrow(() -> new UncheckedSQLException("Could not store classroom."));

        ClassroomMember owner = new ClassroomMember(login.getUserId(), id, ClassroomRole.OWNER);
        memberRepository.storeMember(owner);

        return id;
    }

    public void changeName(int classroomId, String newName) throws ValidationException {
        validateName(newName);
        updateClassroom(classroomId, builder -> builder.setName(newName));
    }

    public void setPassword(int classroomId, String newPassword) throws ValidationException {
        validatePassword(newPassword);
        String encryptedPassword = newPassword != null
                ? passwordEncoder.encode(newPassword)
                : null;
        updateClassroom(classroomId, builder -> builder.setPassword(encryptedPassword));
    }

    public void removePassword(int classroomId) {
        setPassword(classroomId, null);
    }

    public void setOpen(int classroomId, boolean open) {
        updateClassroom(classroomId, builder -> builder.setOpen(open));
    }

    public void setVisible(int classroomId, boolean visible) {
        updateClassroom(classroomId, builder -> builder.setVisible(visible));
    }

    public void setArchived(int classroomId, boolean archived) {
        updateClassroom(classroomId, builder -> builder.setArchived(archived));
    }

    private void updateClassroom(int classroomId, Function<Classroom.Builder, Classroom.Builder> updater) {
        Classroom classroom = getClassroomById(classroomId)
                .orElseThrow(() -> new IllegalArgumentException("Classroom doesn't exist."));

        Classroom newClassroom = updater.apply(Classroom.builderFrom(classroom)).build();

        classroomRepository.updateClassroom(newClassroom);
    }

    public void validateName(String name) throws ValidationException {
        if (name == null) {
            throw new ValidationException("Name can't be null.");
        }
        if (name.isEmpty()) {
            throw new ValidationException("Name can't be empty.");
        }
        if (name.length() > 100) {
            throw new ValidationException("Name can't be longer than 100 characters.");
        }
        // not sure if this is needed
        for (char c : name.toCharArray()) {
            switch (Character.getType(c)) {
                case Character.CONTROL:
                case Character.LINE_SEPARATOR:
                case Character.PARAGRAPH_SEPARATOR:
                case Character.PRIVATE_USE:
                    throw new ValidationException("Name contains illegal characters");
            }
        }
    }

    public void validatePassword(String password) throws ValidationException {
        // password is allowed to be null
        if (password == null) {
            return;
        }
        if (password.isEmpty()) {
            throw new ValidationException("Password can't be empty.");
        }
        if (password.length() > 100) {
            throw new ValidationException("Password can't be longer than 100 characters.");
        }
    }

    public Optional<Classroom> getClassroomById(int id) {
        return classroomRepository.getClassroomById(id);
    }

    public Optional<Classroom> getClassroomByUUID(UUID uuid) {
        return classroomRepository.getClassroomByUUID(uuid);
    }

    public List<Classroom> getAllClassrooms() {
        return classroomRepository.getAllClassrooms();
    }

    public List<Classroom> getAllClassroomsByMember(int userId) {
        return classroomRepository.getAllClassroomsByMember(userId);
    }

    public List<Classroom> getActiveClassrooms() {
        return classroomRepository.getActiveClassrooms();
    }

    public List<Classroom> getActiveClassroomsByMember(int userId) {
        return classroomRepository.getActiveClassroomsByMember(userId);
    }

    public List<Classroom> getActiveClassroomsByMemberAndRole(int userId, ClassroomRole role) {
        return classroomRepository.getActiveClassroomsByMemberAndRole(userId, role);
    }

    public List<Classroom> getVisibleClassrooms() {
        return classroomRepository.getVisibleClassrooms();
    }

    public List<Classroom> getArchivedClassrooms() {
        return classroomRepository.getArchivedClassrooms();
    }

    public List<Classroom> getArchivedClassroomsByMember(int userId) {
        return classroomRepository.getArchivedClassroomsByMember(userId);
    }

    public List<Classroom> getArchivedClassroomsByMemberAndRole(int userId, ClassroomRole role) {
        return classroomRepository.getArchivedClassroomsByMemberAndRole(userId, role);
    }

    public List<ClassroomMember> getMembersForClassroom(int id) {
        return memberRepository.getMembersForClassroom(id);
    }

    public Optional<ClassroomMember> getMemberForClassroomAndUser(int classroomId, int userId) {
        return memberRepository.getMemberForClassroomAndUser(classroomId, userId);
    }

    public void addMember(ClassroomMember member) {
        if (member.getRole() == ClassroomRole.OWNER) {
            throw new IllegalArgumentException("Can't add classroom member as owner.");
        }

        Optional<ClassroomMember> existingMember = getMemberForClassroomAndUser(
                member.getClassroomId(), member.getUserId());
        if (existingMember.isEmpty()) {
            memberRepository.storeMember(member);
        }
    }

    public void removeMember(int classroomId, int userId) {
        ClassroomMember existingMember = getMemberForClassroomAndUser(classroomId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Classroom member does not exist."));

        if (existingMember.getRole() == ClassroomRole.OWNER) {
            throw new IllegalArgumentException("Can't remove owner from classroom.");
        }

        memberRepository.deleteMember(classroomId, userId);
    }

    public void changeRole(int classroomId, int userId, ClassroomRole role) {
        ClassroomMember existingMember = getMemberForClassroomAndUser(classroomId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Classroom member does not exist."));

        if (existingMember.getRole() == ClassroomRole.OWNER || role == ClassroomRole.OWNER) {
            throw new IllegalArgumentException("Can't update role of owner. Use changeOwner() instead.");
        }

        memberRepository.updateMember(new ClassroomMember(userId, classroomId, role));
    }

    public void changeOwner(int classroomId, int userId) {
        ClassroomMember member = getMemberForClassroomAndUser(classroomId, userId)
                .orElseThrow(() -> new IllegalArgumentException("New owner must be a member of the classroom."));

        // Should only ever be one owner, but best make it work with multiple owners
        List<ClassroomMember> owners = getMembersForClassroom(classroomId).stream()
                .filter(m -> m.getRole() == ClassroomRole.OWNER)
                .collect(Collectors.toList());
        for (ClassroomMember owner : owners) {
            ClassroomMember updatedMember = new ClassroomMember(
                    owner.getUserId(), classroomId, ClassroomRole.MODERATOR);
            memberRepository.updateMember(updatedMember);
        }

        ClassroomMember newOwner = new ClassroomMember(userId, classroomId, ClassroomRole.OWNER);
        memberRepository.updateMember(newOwner);
    }

    public String getInviteLinkForClassroom(UUID uuid) {
        return String.format("%s?classroomUid=%s",
                url.getAbsoluteURLForPath(Paths.CLASSROOM),
                uuid);
    }

    public boolean canEditClassroom(ClassroomMember member) {
        if (login.isAdmin()) {
            return true;
        }
        if (member == null) {
            return false;
        }
        return member.getRole() == ClassroomRole.OWNER || member.getRole() == ClassroomRole.MODERATOR;
    }

    public boolean canChangeRoles(ClassroomMember member) {
        if (login.isAdmin()) {
            return true;
        }
        if (member == null) {
            return false;
        }
        return member.getRole() == ClassroomRole.OWNER;
    }

    public boolean canChangeOwner(ClassroomMember member) {
        if (login.isAdmin()) {
            return true;
        }
        if (member == null) {
            return false;
        }
        return member.getRole() == ClassroomRole.OWNER;
    }

    public boolean canKickStudents(ClassroomMember member) {
        if (login.isAdmin()) {
            return true;
        }
        if (member == null) {
            return false;
        }
        return member.getRole() == ClassroomRole.OWNER || member.getRole() == ClassroomRole.MODERATOR;
    }

    public boolean canKickModerators(ClassroomMember member) {
        if (login.isAdmin()) {
            return true;
        }
        if (member == null) {
            return false;
        }
        return member.getRole() == ClassroomRole.OWNER;
    }

    public boolean canLeave(ClassroomMember member) {
        return member != null && member.getRole() != ClassroomRole.OWNER;
    }

    public boolean canJoin(Classroom classroom, ClassroomMember member) {
        return classroom.isOpen() && !classroom.isArchived() && member == null;
    }

    public boolean canCreateGames(ClassroomMember member) {
        if (login.isAdmin()) {
            return true;
        }
        if (member == null) {
            return false;
        }
        return member.getRole() == ClassroomRole.OWNER || member.getRole() == ClassroomRole.MODERATOR;
    }
}
