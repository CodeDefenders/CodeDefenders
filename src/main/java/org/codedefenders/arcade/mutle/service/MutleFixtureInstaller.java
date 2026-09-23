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
package org.codedefenders.arcade.mutle.service;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.codedefenders.arcade.mutle.fixture.MutleClass;
import org.codedefenders.arcade.mutle.fixture.MutleEntry;
import org.codedefenders.arcade.mutle.fixture.MutleMutant;
import org.codedefenders.arcade.mutle.model.MutleFixtureInstallation;
import org.codedefenders.arcade.mutle.persistence.MutleRepository;
import org.codedefenders.configuration.Configuration;
import org.codedefenders.database.UncheckedSQLException;
import org.codedefenders.execution.CompileException;
import org.codedefenders.execution.Compiler;
import org.codedefenders.game.AssertionLibrary;
import org.codedefenders.game.GameClass;
import org.codedefenders.game.GameLevel;
import org.codedefenders.game.Mutant;
import org.codedefenders.game.TestingFramework;
import org.codedefenders.game.puzzle.Puzzle;
import org.codedefenders.game.puzzle.PuzzleType;
import org.codedefenders.persistence.database.GameClassRepository;
import org.codedefenders.persistence.database.MutantRepository;
import org.codedefenders.persistence.database.PuzzleRepository;
import org.codedefenders.transaction.TransactionManager;
import org.codedefenders.util.FileUtils;
import org.codedefenders.validation.code.DefaultRuleSets;

import static org.codedefenders.util.Constants.CUTS_CLASSES_DIR;
import static org.codedefenders.util.Constants.CUTS_MUTANTS_DIR;
import static org.codedefenders.util.Constants.DEFAULT_NB_ASSERTIONS;

@ApplicationScoped
public class MutleFixtureInstaller {
    private final MutleCatalog catalog;
    private final MutleRepository mutleRepository;
    private final GameClassRepository classRepository;
    private final MutantRepository mutantRepository;
    private final PuzzleRepository puzzleRepository;
    private final Configuration configuration;
    private final TransactionManager transactionManager;
    private final Set<String> installedKeys = ConcurrentHashMap.newKeySet();

    @Inject
    public MutleFixtureInstaller(MutleCatalog catalog, MutleRepository mutleRepository,
                                 GameClassRepository classRepository, MutantRepository mutantRepository,
                                 PuzzleRepository puzzleRepository, Configuration configuration,
                                 TransactionManager transactionManager) {
        this.catalog = catalog;
        this.mutleRepository = mutleRepository;
        this.classRepository = classRepository;
        this.mutantRepository = mutantRepository;
        this.puzzleRepository = puzzleRepository;
        this.configuration = configuration;
        this.transactionManager = transactionManager;
    }

    public synchronized void ensureInstalled(MutleEntry entry) {
        String key = MutleService.fixtureKey(entry);
        if (installedKeys.contains(key)) {
            return;
        }
        RuntimeException failure = null;
        for (int attempt = 0; attempt < 2; attempt++) {
            try {
                transactionManager.executeInTransaction(transaction -> {
                    install(entry);
                    transaction.commit();
                    return null;
                });
                installedKeys.add(key);
                return;
            } catch (UncheckedSQLException exception) {
                failure = exception;
                if (exception.getErrorCode() != 1062) {
                    throw exception;
                }
            } catch (Exception exception) {
                throw exception instanceof RuntimeException runtime
                        ? runtime : new IllegalStateException("Could not install Mutle fixture " + key, exception);
            }
        }
        throw new IllegalStateException("Could not install Mutle fixture after a concurrent installation: " + key,
                failure);
    }

    public static String contentHash(MutleClass fixtureClass, MutleMutant mutant, String cutSource,
                                     String mutantSource, String proofSource) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            hashField(digest, fixtureClass.id());
            hashField(digest, fixtureClass.title());
            hashField(digest, fixtureClass.specification());
            hashField(digest, Integer.toString(fixtureClass.concepts().size()));
            fixtureClass.concepts().forEach(concept -> hashField(digest, concept));
            hashField(digest, fixtureClass.sourcePath());
            hashField(digest, mutant.key());
            hashField(digest, mutant.sourcePath());
            hashField(digest, mutant.method());
            hashField(digest, Integer.toString(mutant.mutatedLines().size()));
            mutant.mutatedLines().forEach(line -> hashField(digest, Integer.toString(line)));
            hashField(digest, mutant.operator());
            hashField(digest, mutant.proofTestKey());
            hashField(digest, mutant.revealExplanation());
            hashField(digest, cutSource);
            hashField(digest, mutantSource);
            hashField(digest, proofSource);
            return java.util.HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void hashField(MessageDigest digest, String value) {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        digest.update(ByteBuffer.allocate(Integer.BYTES).putInt(bytes.length).array());
        digest.update(bytes);
    }

    private void install(MutleEntry entry) {
        MutleClass fixtureClass = catalog.fixtureClass(entry.classKey());
        String cutSource = catalog.readClassResource(entry.classKey(), fixtureClass.sourcePath());
        MutleMutant mutant = fixtureClass.mutants().stream().filter(value -> value.key().equals(entry.mutantKey()))
                .findFirst().orElseThrow(() -> new IllegalStateException("Mutle entry has no mutant"));
        String mutantSource = catalog.readClassResource(entry.classKey(), mutant.sourcePath());
        String proofSource = fixtureClass.tests().stream()
                .filter(test -> test.key().equals(mutant.proofTestKey()))
                .findFirst()
                .map(test -> catalog.readClassResource(entry.classKey(), test.sourcePath()))
                .orElseThrow(() -> new IllegalStateException("Mutle mutant proof test is missing"));
        String key = MutleService.fixtureKey(entry);
        String hash = contentHash(fixtureClass, mutant, cutSource, mutantSource, proofSource);
        var existing = mutleRepository.findFixture(key);
        if (existing.isPresent()) {
            if (!existing.get().contentHash().equals(hash)) {
                throw new IllegalStateException("Installed Mutle fixture content differs: " + key);
            }
            return;
        }
        InstalledClass installed = installedClass(entry.classKey(), fixtureClass, cutSource);
        Mutant template = storeMutant(entry.classKey(), mutant, mutantSource, installed.cut());
        mutleRepository.insertFixture(new MutleFixtureInstallation(key, hash, installed.puzzleId(), template.getId(),
                Clock.systemUTC().instant()));
    }

    private InstalledClass installedClass(String classKey, MutleClass fixtureClass, String source) {
        List<MutleFixtureInstallation> existing = mutleRepository.findFixtures().stream()
                .filter(fixture -> fixture.fixtureKey().startsWith(classKey + "/")).toList();
        for (MutleFixtureInstallation installation : existing) {
            Puzzle puzzle = puzzleRepository.getPuzzleForId(installation.hiddenPuzzleId());
            if (puzzle == null) {
                throw new IllegalStateException("Installed Mutle class has no hidden puzzle");
            }
            GameClass cut = classRepository.getClassForId(puzzle.getClassId()).orElseThrow();
            if (cut.getSourceCode().equals(source)) {
                return new InstalledClass(cut, puzzle.getPuzzleId());
            }
        }
        try {
            String alias = "mutle-" + classKey;
            String storageKey = classKey;
            GameClass existingClass = classRepository.getClassForAlias(alias).orElse(null);
            if (existingClass != null && existingClass.getSourceCode().equals(source)) {
                Puzzle existingPuzzle = puzzleRepository.getPuzzleForClassId(existingClass.getId());
                if (existingPuzzle != null) {
                    return new InstalledClass(existingClass, existingPuzzle.getPuzzleId());
                }
                return new InstalledClass(existingClass, createPuzzle(existingClass));
            }
            if (existingClass != null) {
                throw new IllegalStateException("Installed Mutle class version differs: " + classKey);
            }
            Path directory = cutDirectory(configuration.getSourcesDir().toPath(), storageKey, source);
            Path sourcePath = storeFixtureFile(directory, fixtureClass.sourcePath(), source);
            String classPath = Compiler.compileJavaFile(sourcePath.toString());
            GameClass cut = GameClass.build().name(FileUtils.getFullyQualifiedName(classPath))
                    .alias(alias).javaFile(sourcePath.toString())
                    .classFile(classPath).mockingEnabled(false).testingFramework(TestingFramework.JUNIT4)
                    .assertionLibrary(AssertionLibrary.JUNIT4_HAMCREST).puzzleClass(true).active(false).create();
            classRepository.storeClass(cut);
            return new InstalledClass(cut, createPuzzle(cut));
        } catch (IOException | CompileException e) {
            throw new IllegalStateException("Could not install Mutle class " + classKey, e);
        }
    }

    private int createPuzzle(GameClass cut) {
        Puzzle puzzle = new Puzzle(-1, cut.getId(), PuzzleType.DEFENDER, false, GameLevel.EASY,
                DEFAULT_NB_ASSERTIONS, DefaultRuleSets.MODERATE, null, null, null, null);
        int puzzleId = puzzleRepository.storePuzzle(puzzle);
        puzzle.setPuzzleId(puzzleId);
        return puzzleId;
    }

    private Mutant storeMutant(String classKey, MutleMutant metadata, String source, GameClass cut) {
        try {
            Path directory = mutantDirectory(configuration.getSourcesDir().toPath(), classKey, metadata.key(), source);
            Path sourcePath = storeFixtureFile(directory, cut.getBaseName() + ".java", source);
            String classPath = Compiler.compileJavaFile(sourcePath.toString());
            Mutant mutant = new Mutant(sourcePath.toString(), classPath, FileUtils.getMD5FromText(source), cut.getId());
            mutant.setLines(metadata.mutatedLines());
            mutant.setId(mutantRepository.storeMutant(mutant));
            mutantRepository.mapMutantToClass(mutant.getId(), cut.getId());
            return mutant;
        } catch (IOException | CompileException e) {
            throw new IllegalStateException("Could not install Mutle mutant " + metadata.key(), e);
        }
    }

    static Path storeFixtureFile(Path directory, String fileName, String source) throws IOException {
        Path filePath = directory.resolve(fileName);
        try {
            Files.createDirectories(filePath.getParent());
            return FileUtils.storeFile(directory, fileName, source);
        } catch (FileAlreadyExistsException e) {
            if (Files.isRegularFile(filePath, LinkOption.NOFOLLOW_LINKS)
                    && Files.readString(filePath, StandardCharsets.UTF_8).equals(source)) {
                return filePath;
            }
            throw e;
        }
    }

    static Path cutDirectory(Path sourcesDirectory, String classKey, String source) {
        return fixtureDirectory(sourcesDirectory, classKey).resolve(CUTS_CLASSES_DIR)
                .resolve(FileUtils.getPackagePathFromJavaFile(source));
    }

    static Path mutantDirectory(Path sourcesDirectory, String classKey, String mutantKey, String source) {
        return fixtureDirectory(sourcesDirectory, classKey).resolve(CUTS_MUTANTS_DIR).resolve(mutantKey)
                .resolve(FileUtils.getPackagePathFromJavaFile(source));
    }

    private static Path fixtureDirectory(Path sourcesDirectory, String classKey) {
        return sourcesDirectory.resolve("mutle-" + classKey);
    }

    private record InstalledClass(GameClass cut, int puzzleId) {
    }
}
