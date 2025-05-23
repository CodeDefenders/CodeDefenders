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
package org.codedefenders.service;

import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.codedefenders.analysis.gameclass.ClassCodeAnalyser;
import org.codedefenders.analysis.gameclass.ClassCodeAnalyser.ClassAnalysisResult;
import org.codedefenders.game.GameClass;
import org.codedefenders.instrumentation.MetricsRegistry;
import org.codedefenders.persistence.database.GameClassRepository;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

@ApplicationScoped
public class ClassAnalysisService {
    private final LoadingCache<Integer, ClassAnalysisResult> cache;

    private final ClassCodeAnalyser classCodeAnalyser;
    private final GameClassRepository gameClassRepo;

    @Inject
    public ClassAnalysisService(ClassCodeAnalyser classCodeAnalyser, MetricsRegistry metricsRegistry,
                                GameClassRepository gameClassRepo) {
        this.classCodeAnalyser = classCodeAnalyser;
        this.gameClassRepo = gameClassRepo;

        cache = CacheBuilder.newBuilder()
                .expireAfterWrite(30, TimeUnit.MINUTES)
                .maximumSize(200)
                .recordStats()
                .build(
                        new CacheLoader<>() {
                            @Nonnull
                            @Override
                            public ClassAnalysisResult load(@Nonnull Integer classId) throws Exception {
                                GameClass clazz = gameClassRepo.getClassForId(classId)
                                        .orElseThrow();
                                return classCodeAnalyser.analyze(clazz.getSourceCode())
                                        .orElseThrow(AnalysisException::new);
                            }
                        }
                );

        metricsRegistry.registerGuavaCache("classAnalysisForClassId", cache);
    }

    /**
     * Performs the analysis on the given CUT, or returns a cached analysis.
     *
     * @param classId The ID of the CUT.
     * @return The result of the analysis.
     */
    public Optional<ClassAnalysisResult> analyze(int classId) {
        try {
            return Optional.of(cache.get(classId));
        } catch (ExecutionException e) {
            return Optional.empty();
        }
    }

    /**
     * Performs the analysis on the given source code.
     * The result is not cached.
     *
     * @param sourceCode The source code of the class.
     * @return The result of the analysis.
     */
    public Optional<ClassAnalysisResult> analyze(String sourceCode) {
        return classCodeAnalyser.analyze(sourceCode);
    }

    public static class AnalysisException extends Exception {

    }
}
