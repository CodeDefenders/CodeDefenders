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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.codedefenders.arcade.mutle.fixture.MutleClass;
import org.codedefenders.arcade.mutle.fixture.MutleEntry;
import org.codedefenders.arcade.mutle.fixture.MutleFixtureLoader;
import org.codedefenders.arcade.mutle.fixture.MutleMutant;
import org.codedefenders.arcade.mutle.model.MutleCatalogFixture;
import org.codedefenders.arcade.mutle.model.MutleFixtureBundle;
import org.codedefenders.arcade.mutle.model.MutlePublication;
import org.codedefenders.arcade.mutle.persistence.MutleRepository;
import org.codedefenders.configuration.Configuration;
import org.codedefenders.transaction.TransactionManager;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;

/** Persistent catalog and dated publication schedule for Mutle. */
@ApplicationScoped
public class MutleCatalog {
    private static final Gson GSON = new Gson();

    private final MutleFixtureLoader loader;
    private final MutleRepository repository;
    private final Configuration configuration;
    private final TransactionManager transactionManager;
    private volatile boolean seeded;

    @Inject
    public MutleCatalog(MutleFixtureLoader loader, MutleRepository repository, Configuration configuration,
                        TransactionManager transactionManager) {
        this.loader = loader;
        this.repository = repository;
        this.configuration = configuration;
        this.transactionManager = transactionManager;
    }

    MutleCatalog(MutleFixtureLoader loader, MutleRepository repository, Configuration configuration) {
        this(loader, repository, configuration, null);
    }

    /** Imports the bundled source once and reconstructs the legacy schedule only on an upgrade. */
    public synchronized void ensureSeeded() {
        if (seeded) {
            return;
        }
        List<MutleEntry> entries = loader.pack().entries();
        entries.stream().map(MutleEntry::classKey).distinct().forEach(this::storeBundledClass);
        if (repository.hasGames() && repository.findPublications().isEmpty()) {
            Instant start = Instant.parse(configuration.getMutleRotationStart());
            int periodDays = configuration.getMutleRotationPeriodDays();
            if (periodDays <= 0) {
                throw new IllegalStateException("Mutle rotation period must be positive");
            }
            Duration period = Duration.ofDays(periodDays);
            List<MutlePublication> publications = new ArrayList<>();
            for (MutleEntry entry : entries) {
                publications.add(new MutlePublication(entry.series(), fixtureKey(entry),
                        start.plus(period.multipliedBy(entry.series() - 1L))));
            }
            try {
                insertPublicationsAtomically(publications);
            } catch (RuntimeException exception) {
                if (!repository.findPublications().equals(publications)) {
                    throw exception;
                }
            }
        }
        seeded = true;
    }

    /** All scheduled entries plus imported entries not yet assigned to a publication (series zero). */
    public List<MutleEntry> allEntries() {
        ensureSeeded();
        Map<String, MutleEntry> result = new LinkedHashMap<>();
        for (MutlePublication publication : repository.findPublications()) {
            findBundledEntry(publication.fixtureKey()).ifPresent(entry -> result.put(publication.fixtureKey(),
                    new MutleEntry((int) publication.series(), entry.classKey(), entry.mutantKey())));
        }
        for (MutleFixtureBundle bundle : repository.findBundles()) {
            MutleClass fixtureClass = parse(bundle.classJson());
            for (MutleMutant mutant : fixtureClass.mutants()) {
                String key = fixtureClass.id() + "/" + mutant.key();
                result.putIfAbsent(key, new MutleEntry(0, fixtureClass.id(), mutant.key()));
            }
        }
        return List.copyOf(result.values());
    }

    public Optional<MutleEntry> currentEntry(Instant now) {
        return repository.findCurrentPublication(now).flatMap(publication -> findBundledEntry(publication.fixtureKey())
                .map(entry -> new MutleEntry((int) publication.series(), entry.classKey(), entry.mutantKey())));
    }

    public Optional<MutleEntry> findEntry(String fixtureKey) {
        Optional<MutlePublication> publication = repository.findPublications().stream()
                .filter(value -> value.fixtureKey().equals(fixtureKey)).findFirst();
        Optional<MutleEntry> entry = findBundledEntry(fixtureKey);
        if (entry.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new MutleEntry(publication.map(value -> (int) value.series()).orElse(0),
                entry.get().classKey(), entry.get().mutantKey()));
    }

    public MutleClass fixtureClass(String classKey) {
        Optional<MutleFixtureBundle> bundle = repository.findBundle(classKey);
        if (bundle.isPresent()) {
            return parse(bundle.get().classJson());
        }
        if (!isBundledClass(classKey)) {
            throw new IllegalStateException("Mutle fixture class is not in the catalog: " + classKey);
        }
        return loader.fixtureClass(classKey);
    }

    public String readClassResource(String classKey, String relativePath) {
        Optional<MutleFixtureBundle> bundle = repository.findBundle(classKey);
        if (bundle.isEmpty()) {
            if (!isBundledClass(classKey)) {
                throw new IllegalStateException("Mutle fixture class is not in the catalog: " + classKey);
            }
            return loader.readClassResource(classKey, relativePath);
        }
        if (relativePath.equals("class.json")) {
            return bundle.get().classJson();
        }
        String source = bundle.get().resources().get(relativePath);
        if (source == null) {
            throw new IllegalStateException("Mutle fixture resource is missing: " + classKey + "/" + relativePath);
        }
        return source;
    }

    public List<MutlePublication> publications() {
        ensureSeeded();
        return repository.findPublications();
    }

    public List<MutleCatalogFixture> inventory() {
        ensureSeeded();
        Map<String, Integer> published = new LinkedHashMap<>();
        repository.findPublications().forEach(value -> {
            String classKey = value.fixtureKey().substring(0, value.fixtureKey().indexOf('/'));
            published.merge(classKey, 1, Integer::sum);
        });
        return repository.findBundles().stream().map(bundle -> {
            MutleClass fixtureClass = parse(bundle.classJson());
            return new MutleCatalogFixture(fixtureClass.id(), fixtureClass.title(), fixtureClass.mutants().size(),
                    published.getOrDefault(fixtureClass.id(), 0));
        }).toList();
    }

    /** Persists a structurally validated uploaded fixture; its mutants remain unscheduled until published. */
    public MutleFixtureBundle importFixture(byte[] archive) throws IOException {
        MutleFixtureBundle bundle = MutleFixtureLoader.loadArchive(new ByteArrayInputStream(archive));
        Optional<MutleFixtureBundle> existing = repository.findBundle(bundle.classKey());
        if (existing.isPresent()) {
            if (!existing.get().contentHash().equals(bundle.contentHash())) {
                throw new IOException("A different version of fixture class " + bundle.classKey()
                        + " is already installed");
            }
            return existing.get();
        }
        repository.insertBundle(bundle);
        return bundle;
    }

    /** Schedules the first mutant from each distinct bundled class at weekly intervals. */
    public synchronized List<MutlePublication> scheduleInitial(Instant firstReleaseAt) {
        ensureSeeded();
        if (firstReleaseAt == null || !firstReleaseAt.isAfter(Instant.now())) {
            throw new IllegalArgumentException("The first Mutle release must be in the future");
        }
        if (!repository.findPublications().isEmpty()) {
            throw new IllegalStateException("Mutle publications have already been scheduled");
        }
        Duration period = Duration.ofDays(7);
        List<MutlePublication> created = new ArrayList<>();
        long series = 1;
        Set<String> classes = new LinkedHashSet<>();
        List<MutleEntry> selected = Stream.concat(loader.pack().entries().stream(), allEntries().stream())
                .filter(entry -> classes.add(entry.classKey()))
                .limit(15)
                .toList();
        if (selected.size() != 15) {
            throw new IllegalStateException("Mutle needs at least 15 distinct fixture classes");
        }
        for (MutleEntry entry : selected) {
            MutlePublication publication = new MutlePublication(series, fixtureKey(entry),
                    firstReleaseAt.plus(period.multipliedBy(series - 1)));
            created.add(publication);
            series++;
        }
        insertPublicationsAtomically(created);
        return List.copyOf(created);
    }

    public synchronized void savePublication(long series, String fixtureKey, Instant releaseAt) {
        if (series < 1 || releaseAt == null || !releaseAt.isAfter(Instant.now())
                || findBundledEntry(fixtureKey).isEmpty()) {
            throw new IllegalArgumentException("Publication refers to an invalid Mutle fixture");
        }
        Optional<MutlePublication> assignment = repository.findPublications().stream()
                .filter(publication -> publication.fixtureKey().equals(fixtureKey))
                .filter(publication -> publication.series() != series)
                .findFirst();
        if (assignment.isPresent()) {
            throw new IllegalArgumentException("Mutle fixture is already assigned to publication "
                    + assignment.get().series());
        }
        Optional<MutlePublication> existing = repository.findPublication(series);
        if (existing.isPresent()) {
            if (!existing.get().releaseAt().isAfter(Instant.now())) {
                throw new IllegalStateException("Published Mutle cases are immutable");
            }
            validateReleaseOrder(series, releaseAt);
            repository.updatePublication(new MutlePublication(series, fixtureKey, releaseAt));
        } else {
            long nextSeries = repository.findPublications().stream().mapToLong(MutlePublication::series).max()
                    .orElse(0) + 1;
            if (series != nextSeries) {
                throw new IllegalArgumentException("Mutle publications must be added in order");
            }
            validateReleaseOrder(series, releaseAt);
            repository.insertPublication(new MutlePublication(series, fixtureKey, releaseAt));
        }
    }

    public synchronized void removeLastFuturePublication(long series) {
        ensureSeeded();
        List<MutlePublication> publications = repository.findPublications();
        Instant now = Instant.now();
        if (publications.isEmpty() || publications.get(publications.size() - 1).series() != series
                || !publications.get(publications.size() - 1).releaseAt().isAfter(now)
                || repository.deleteFuturePublication(series, now) != 1) {
            throw new IllegalStateException("Only the last unpublished Mutle case can be removed");
        }
    }

    public synchronized void clearFuturePublications() {
        ensureSeeded();
        if (repository.deleteFuturePublications(Instant.now()) == 0) {
            throw new IllegalStateException("There are no unpublished Mutle cases to clear");
        }
    }


    private void insertPublicationsAtomically(List<MutlePublication> publications) {
        if (transactionManager == null) {
            publications.forEach(repository::insertPublication);
            return;
        }
        try {
            transactionManager.executeInTransaction(transaction -> {
                if (!repository.findPublications().isEmpty()) {
                    throw new IllegalStateException("Mutle publications have already been scheduled");
                }
                publications.forEach(repository::insertPublication);
                transaction.commit();
                return null;
            });
        } catch (Exception exception) {
            throw exception instanceof RuntimeException runtime
                    ? runtime : new IllegalStateException("Could not schedule Mutle publications", exception);
        }
    }

    private void storeBundledClass(String classKey) {
        MutleFixtureBundle bundle = loader.bundle(classKey);
        Optional<MutleFixtureBundle> existing = repository.findBundle(classKey);
        if (existing.isEmpty()) {
            repository.insertBundleIfAbsent(bundle);
            existing = repository.findBundle(classKey);
        }
        if (existing.isEmpty()) {
            throw new IllegalStateException("Mutle fixture bundle was not installed: " + classKey);
        }
        if (!existing.get().contentHash().equals(bundle.contentHash())) {
            throw new IllegalStateException("Installed Mutle fixture content differs: " + classKey);
        }
    }

    private void validateReleaseOrder(long series, Instant releaseAt) {
        List<MutlePublication> publications = repository.findPublications();
        publications.stream().filter(value -> value.series() < series).map(MutlePublication::releaseAt)
                .max(Instant::compareTo).ifPresent(previous -> {
                    if (!previous.isBefore(releaseAt)) {
                        throw new IllegalArgumentException("Mutle release dates must be strictly increasing");
                    }
                });
        publications.stream().filter(value -> value.series() > series).map(MutlePublication::releaseAt)
                .min(Instant::compareTo).ifPresent(next -> {
                    if (!releaseAt.isBefore(next)) {
                        throw new IllegalArgumentException("Mutle release dates must be strictly increasing");
                    }
                });
    }

    private Optional<MutleEntry> findBundledEntry(String fixtureKey) {
        if (fixtureKey == null) {
            return Optional.empty();
        }
        int separator = fixtureKey.indexOf('/');
        if (separator < 1 || separator == fixtureKey.length() - 1) {
            return Optional.empty();
        }
        String classKey = fixtureKey.substring(0, separator);
        String mutantKey = fixtureKey.substring(separator + 1);
        Optional<MutleFixtureBundle> bundle = repository.findBundle(classKey);
        if (bundle.isEmpty() && !isBundledClass(classKey)) {
            return Optional.empty();
        }
        MutleClass fixtureClass = bundle.map(value -> parse(value.classJson())).orElseGet(() -> loader.fixtureClass(classKey));
        return fixtureClass.mutants().stream().filter(value -> value.key().equals(mutantKey))
                .findFirst().map(value -> new MutleEntry(0, classKey, mutantKey));
    }

    private static MutleClass parse(String json) {
        try {
            MutleClass value = GSON.fromJson(json, MutleClass.class);
            if (value == null) {
                throw new IllegalStateException("Mutle class metadata is empty");
            }
            return value;
        } catch (JsonParseException exception) {
            throw new IllegalStateException("Malformed persisted Mutle class metadata", exception);
        }
    }

    private boolean isBundledClass(String classKey) {
        return loader.pack().entries().stream().anyMatch(entry -> entry.classKey().equals(classKey));
    }

    private static String fixtureKey(MutleEntry entry) {
        return entry.classKey() + "/" + entry.mutantKey();
    }
}
