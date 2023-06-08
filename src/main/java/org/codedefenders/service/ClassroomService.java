package org.codedefenders.service;

import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.regex.Pattern;
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
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

// TODO: Add a second Classroom class to abstract from the database class, like User <-> UserEntity?
// TODO: Add caching?
@Transactional
@ApplicationScoped
public class ClassroomService {

    private final ClassroomRepository classroomRepository;
    private final ClassroomMemberRepository memberRepository;

    private static final char[] ROOM_CODE_RANDOM_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789".toCharArray();
    private static final Pattern VALID_ROOM_CODE_REGEX = Pattern.compile("[a-zA-Z0-9\\-]*");

    @Inject
    public ClassroomService(ClassroomRepository classroomRepository,
                            ClassroomMemberRepository memberRepository) {
        this.classroomRepository = classroomRepository;
        this.memberRepository = memberRepository;
    }

    public int addClassroom(Classroom classroom, int ownerId) throws ValidationException {
        validateName(classroom.getName());
        validatePassword(classroom.getPassword().orElse(null));

        if (classroom.getRoomCode() != null) {
            validateRoomCode(classroom.getRoomCode());
            validateRoomCodeNotPresent(classroom.getId(), classroom.getRoomCode());

        } else {
            String roomCode = tryGenerateUniqueRoomCode()
                    .orElseThrow(() -> new IllegalStateException("Couldn't generate a unique room code."));
            classroom = Classroom.builderFrom(classroom)
                    .setRoomCode(roomCode)
                    .build();
        }

        int id = classroomRepository.storeClassroom(classroom)
                .orElseThrow(() -> new UncheckedSQLException("Could not store classroom."));

        ClassroomMember owner = new ClassroomMember(ownerId, id, ClassroomRole.OWNER);
        memberRepository.storeMember(owner);

        return id;
    }

    private Optional<String> tryGenerateUniqueRoomCode() {
        // Try to generate a unique random code 50 times for lengths of 4 - 20.
        for (int length = 4; length <= 20; length++) {
            for (int i = 0; i < 50; i++) {
                String roomCode = generateRoomCode(length);
                Optional<Classroom> existingClassroom = getClassroomByRoomCode(roomCode);
                if (!existingClassroom.isPresent()) {
                    return Optional.of(roomCode);
                }
            }
        }
        return Optional.empty();
    }

    private String generateRoomCode(int length) {
        Random random = new Random();
        StringBuilder roomCode = new StringBuilder();
        for (int i = 0; i < length; i++) {
            int choice = random.nextInt(ROOM_CODE_RANDOM_CHARS.length);
            roomCode.append(ROOM_CODE_RANDOM_CHARS[choice]);
        }
        return roomCode.toString();
    }

    public void changeName(int classroomId, String newName) throws ValidationException {
        validateName(newName);

        Classroom classroom = getClassroomById(classroomId)
                .orElseThrow(() -> new IllegalArgumentException("Classroom doesn't exist."));

        Classroom newClassroom = Classroom.builderFrom(classroom)
                .setName(newName)
                .build();
        classroomRepository.updateClassroom(newClassroom);
    }

    public void setPassword(int classroomId, String newPassword) throws ValidationException {
        validatePassword(newPassword);

        Classroom classroom = getClassroomById(classroomId)
                .orElseThrow(() -> new IllegalArgumentException("Classroom doesn't exist."));

        String encryptedPassword = new BCryptPasswordEncoder().encode(newPassword);

        Classroom newClassroom = Classroom.builderFrom(classroom)
                .setPassword(encryptedPassword)
                .build();
        classroomRepository.updateClassroom(newClassroom);
    }

    public void removePassword(int classroomId) {
        Classroom classroom = getClassroomById(classroomId)
                .orElseThrow(() -> new IllegalArgumentException("Classroom doesn't exist."));

        Classroom newClassroom = Classroom.builderFrom(classroom)
                .setPassword(null)
                .build();
        classroomRepository.updateClassroom(newClassroom);
    }

    public void changeRoomCode(int classroomId, String newRoomCode) throws ValidationException {
        validateRoomCode(newRoomCode);
        validateRoomCodeNotPresent(classroomId, newRoomCode);

        Classroom classroom = getClassroomById(classroomId)
                .orElseThrow(() -> new IllegalArgumentException("Classroom doesn't exist."));

        Classroom newClassroom = Classroom.builderFrom(classroom)
                .setRoomCode(newRoomCode)
                .build();
        classroomRepository.updateClassroom(newClassroom);
    }

    public void setOpen(int classroomId, boolean open) {
        Classroom classroom = getClassroomById(classroomId)
                .orElseThrow(() -> new IllegalArgumentException("Classroom doesn't exist."));

        Classroom newClassroom = Classroom.builderFrom(classroom)
                .setOpen(open)
                .build();
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

    public void validateRoomCode(String roomCode) throws ValidationException {
        if (roomCode == null) {
            throw new ValidationException("Room code can't be null.");
        }
        if (roomCode.isEmpty()) {
            throw new ValidationException("Room code can't be empty.");
        }
        if (roomCode.length() < 4) {
            throw new ValidationException("Room code can't be shorter than 4 characters.");
        }
        if (roomCode.length() > 20) {
            throw new ValidationException("Room code can't be longer than 20 characters.");
        }
        if (!VALID_ROOM_CODE_REGEX.matcher(roomCode).matches()) {
            throw new ValidationException("Room code contains illegal characters");
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

    public void validateRoomCodeNotPresent(int classroomId, String roomCode) {
        Optional<Classroom> existingClassroom = getClassroomByRoomCode(roomCode);
        if (existingClassroom.isPresent() && existingClassroom.get().getId() != classroomId) {
            throw new ValidationException("Classroom with given room code already exists.");
        }
    }

    public Optional<Classroom> getClassroomById(int id) {
        return classroomRepository.getClassroomById(id);
    }

    public Optional<Classroom> getClassroomByRoomCode(String roomCode) {
        return classroomRepository.getClassroomByRoomCode(roomCode);
    }

    public List<Classroom> getClassroomsByMemberAndRole(int userId, ClassroomRole role) {
        return classroomRepository.getClassroomsByMemberAndRole(userId, role);
    }

    public List<Classroom> getClassroomsByMember(int userId) {
        return classroomRepository.getClassroomsByMember(userId);
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
        if (!existingMember.isPresent()) {
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

    public boolean changeRole(int classroomId, int userId, ClassroomRole role) {
        ClassroomMember existingMember = getMemberForClassroomAndUser(classroomId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Classroom member does not exist."));

        if (existingMember.getRole() == ClassroomRole.OWNER || role == ClassroomRole.OWNER) {
            throw new IllegalArgumentException("Can't update role of owner. Use changeOwner() instead.");
        }

        return memberRepository.updateMember(new ClassroomMember(userId, classroomId, role));
    }

    public void changeOwner(int classroomId, int userId) {
        ClassroomMember member = getMemberForClassroomAndUser(classroomId, userId)
                .orElseThrow(() -> new IllegalArgumentException("New owner must be a member of the classroom."));

        List<ClassroomMember> owners = getMembersForClassroom(classroomId).stream()
                .filter(m -> m.getRole() == ClassroomRole.OWNER)
                .collect(Collectors.toList());

        // Should only ever be one owner, but best make it work with multiple
        for (ClassroomMember owner : owners) {
            ClassroomMember updatedMember = new ClassroomMember(
                    owner.getUserId(), classroomId, ClassroomRole.MODERATOR);
            if (!memberRepository.updateMember(updatedMember)) {
                throw new UncheckedSQLException("Couldn't change role of old owner.");
            }
        }

        ClassroomMember newOwner = new ClassroomMember(userId, classroomId, ClassroomRole.OWNER);
        if (!memberRepository.updateMember(newOwner)) {
            throw new UncheckedSQLException("Couldn't change role of new owner.");
        }
    }
}
