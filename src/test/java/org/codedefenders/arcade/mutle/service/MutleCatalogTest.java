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

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.codedefenders.arcade.mutle.fixture.MutleClass;
import org.codedefenders.arcade.mutle.fixture.MutleEntry;
import org.codedefenders.arcade.mutle.fixture.MutleFixtureLoader;
import org.codedefenders.arcade.mutle.fixture.MutleMutant;
import org.codedefenders.arcade.mutle.model.MutleFixtureBundle;
import org.codedefenders.arcade.mutle.model.MutlePublication;
import org.codedefenders.arcade.mutle.persistence.MutleRepository;
import org.codedefenders.configuration.Configuration;
import org.junit.jupiter.api.Test;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MutleCatalogTest {
    private static final Instant FIRST_RELEASE = Instant.parse("2099-01-05T09:00:00Z");

    @Test
    void schedulesExactlyOneMutantPerFifteenDistinctClasses() {
        FixtureSet fixtureSet = fixtureSet(15);
        MutleCatalog catalog = fixtureSet.catalog();

        List<MutlePublication> publications = catalog.scheduleInitial(FIRST_RELEASE);

        assertThat(publications).hasSize(15);
        assertThat(publications.stream().map(MutlePublication::fixtureKey).distinct().count()).isEqualTo(15);
        assertThat(publications.get(14).releaseAt()).isEqualTo(FIRST_RELEASE.plusSeconds(14 * 7L * 24 * 60 * 60));
    }

    @Test
    void refusesInitialScheduleWithoutFifteenClasses() {
        FixtureSet fixtureSet = fixtureSet(14);

        assertThrows(IllegalStateException.class, () -> fixtureSet.catalog().scheduleInitial(FIRST_RELEASE));
    }

    @Test
    void holdsLastReleasedCaseAfterScheduleEnds() {
        FixtureSet fixtureSet = fixtureSet(2);
        Instant first = Instant.parse("2020-01-05T09:00:00Z");
        List<MutlePublication> publications = fixtureSet.publications;
        publications.add(new MutlePublication(1, "class-1/mutant-1", first));
        publications.add(new MutlePublication(2, "class-2/mutant-2", first.plusSeconds(7 * 24 * 60 * 60)));

        assertThat(fixtureSet.catalog().currentEntry(Instant.parse("2021-01-01T00:00:00Z")).get().series())
                .isEqualTo(2);
    }

    @Test
    void onlyAllowsFutureChronologicalPublicationChanges() {
        FixtureSet fixtureSet = fixtureSet(15);
        fixtureSet.publications.add(new MutlePublication(1, "class-1/mutant-1", FIRST_RELEASE));
        Instant next = FIRST_RELEASE.plusSeconds(7 * 24 * 60 * 60);

        fixtureSet.catalog().savePublication(2, "class-2/mutant-2", next);

        assertThat(fixtureSet.publications).hasSize(2);
        assertThrows(IllegalArgumentException.class,
                () -> fixtureSet.catalog().savePublication(3, "class-3/mutant-3", FIRST_RELEASE));
    }

    @Test
    void rejectsAssigningFixtureToMoreThanOnePublication() {
        FixtureSet fixtureSet = fixtureSet(15);
        fixtureSet.publications.add(new MutlePublication(1, "class-1/mutant-1", FIRST_RELEASE));

        assertThrows(IllegalArgumentException.class,
                () -> fixtureSet.catalog().savePublication(2, "class-1/mutant-1", FIRST_RELEASE.plusSeconds(60)));
    }

    @Test
    void rejectsNonPositivePeriodWhenBackfillingLegacySchedule() {
        FixtureSet fixtureSet = fixtureSet(2, true);
        when(fixtureSet.configuration.getMutleRotationPeriodDays()).thenReturn(0);

        assertThrows(IllegalStateException.class, fixtureSet.catalog::ensureSeeded);
    }

    @Test
    void removesOnlyTheLastUnpublishedCase() {
        FixtureSet fixtureSet = fixtureSet(15);
        fixtureSet.publications.add(new MutlePublication(1, "class-1/mutant-1",
                Instant.parse("2020-01-05T09:00:00Z")));
        fixtureSet.publications.add(new MutlePublication(2, "class-2/mutant-2", FIRST_RELEASE));
        fixtureSet.publications.add(new MutlePublication(3, "class-3/mutant-3", FIRST_RELEASE.plusSeconds(604800)));

        assertThrows(IllegalStateException.class, () -> fixtureSet.catalog().removeLastFuturePublication(2));
        fixtureSet.catalog().removeLastFuturePublication(3);
        assertThat(fixtureSet.publications).hasSize(2);
        fixtureSet.catalog().removeLastFuturePublication(2);
        assertThat(fixtureSet.publications).hasSize(1);
        assertThrows(IllegalStateException.class, () -> fixtureSet.catalog().removeLastFuturePublication(1));
    }

    @Test
    void clearingFutureSchedulePreservesPublishedCases() {
        FixtureSet fixtureSet = fixtureSet(15);
        fixtureSet.publications.add(new MutlePublication(1, "class-1/mutant-1",
                Instant.parse("2020-01-05T09:00:00Z")));
        fixtureSet.publications.add(new MutlePublication(2, "class-2/mutant-2", FIRST_RELEASE));
        fixtureSet.publications.add(new MutlePublication(3, "class-3/mutant-3", FIRST_RELEASE.plusSeconds(604800)));

        fixtureSet.catalog().clearFuturePublications();

        assertThat(fixtureSet.publications).hasSize(1);
        assertThat(fixtureSet.publications.get(0).series()).isEqualTo(1);
        assertThrows(IllegalStateException.class, fixtureSet.catalog()::clearFuturePublications);
    }

    private static FixtureSet fixtureSet(int classCount) {
        return fixtureSet(classCount, false);
    }

    private static FixtureSet fixtureSet(int classCount, boolean hasGames) {
        MutleFixtureLoader loader = mock(MutleFixtureLoader.class);
        MutleRepository repository = mock(MutleRepository.class);
        Configuration configuration = mock(Configuration.class);
        List<MutleEntry> entries = new ArrayList<>();
        List<MutleFixtureBundle> bundles = new ArrayList<>();
        for (int i = 1; i <= classCount; i++) {
            String classKey = "class-" + i;
            String mutantKey = "mutant-" + i;
            MutleMutant mutant = new MutleMutant(mutantKey, "mutants/" + mutantKey + ".java", "method",
                    List.of(1), "operator", "test", "explanation");
            MutleClass fixtureClass = new MutleClass(classKey, "Class " + i, "specification", List.of("state"),
                    "Class.java", List.of(new org.codedefenders.arcade.mutle.fixture.MutleTest(
                            "test", "tests/Test.java")),
                    List.of(mutant));
            entries.add(new MutleEntry(i, classKey, mutantKey));
            bundles.add(new MutleFixtureBundle(classKey, new com.google.gson.Gson().toJson(fixtureClass),
                    Map.of("Class.java", "class", "tests/Test.java", "test",
                            "mutants/" + mutantKey + ".java", "mutant"),
                    "hash-" + i, FIRST_RELEASE));
        }
        when(loader.pack()).thenReturn(new org.codedefenders.arcade.mutle.fixture.MutlePack(entries));
        when(loader.bundle(any())).thenAnswer(invocation -> bundles.stream()
                .filter(bundle -> bundle.classKey().equals(invocation.getArgument(0))).findFirst().orElseThrow());
        when(repository.findBundle(any())).thenAnswer(invocation -> bundles.stream()
                .filter(bundle -> bundle.classKey().equals(invocation.getArgument(0))).findFirst());
        when(repository.findBundles()).thenReturn(bundles);
        List<MutlePublication> publications = new ArrayList<>();
        when(repository.findPublications()).thenAnswer(invocation -> List.copyOf(publications));
        when(repository.findCurrentPublication(any())).thenAnswer(invocation -> publications.stream()
                .filter(publication -> !publication.releaseAt().isAfter(invocation.getArgument(0, Instant.class)))
                .max(java.util.Comparator.comparing(MutlePublication::releaseAt)));
        when(repository.hasGames()).thenReturn(hasGames);
        doAnswer(invocation -> {
            publications.add(invocation.getArgument(0));
            return null;
        }).when(repository).insertPublication(any());
        when(repository.deleteFuturePublication(anyLong(), any())).thenAnswer(invocation -> {
            long series = invocation.getArgument(0);
            Instant now = invocation.getArgument(1);
            return publications.removeIf(publication -> publication.series() == series
                    && publication.releaseAt().isAfter(now)) ? 1 : 0;
        });
        when(repository.deleteFuturePublications(any())).thenAnswer(invocation -> {
            Instant now = invocation.getArgument(0);
            int before = publications.size();
            publications.removeIf(publication -> publication.releaseAt().isAfter(now));
            return before - publications.size();
        });
        when(configuration.getMutleRotationStart()).thenReturn("2099-01-05T00:00:00Z");
        when(configuration.getMutleRotationPeriodDays()).thenReturn(7);
        return new FixtureSet(new MutleCatalog(loader, repository, configuration), publications, configuration);
    }

    private record FixtureSet(MutleCatalog catalog, List<MutlePublication> publications,
                              Configuration configuration) {
    }
}
