package org.codedefenders.service;

import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.codedefenders.analysis.gameclass.ClassCodeAnalyser;
import org.codedefenders.analysis.gameclass.ClassCodeAnalyser.ClassAnalysisResult;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

@ApplicationScoped
public class ClassAnalysisService {
    private final LoadingCache<Integer, ClassAnalysisResult> cache;

    private final ClassCodeAnalyser classCodeAnalyser;

    @Inject
    public ClassAnalysisService(ClassCodeAnalyser classCodeAnalyser) {
        this.classCodeAnalyser = classCodeAnalyser;

        cache = CacheBuilder.newBuilder()
                .expireAfterWrite(30, TimeUnit.MINUTES)
                .maximumSize(200)
                .recordStats()
                .build(
                        new CacheLoader<Integer, ClassAnalysisResult>() {
                            @Nonnull
                            @Override
                            public ClassAnalysisResult load(@Nonnull Integer classId)
                                    throws Exception {
                                return classCodeAnalyser.analyze(classId)
                                        .orElseThrow(AnalysisException::new);
                            }
                        }
                );
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

    public static class AnalysisException extends Exception {

    }
}
