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
package org.codedefenders.cron;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.List;

import org.codedefenders.arcade.mutle.persistence.MutleRepository;
import org.codedefenders.configuration.Configuration;
import org.codedefenders.transaction.Transaction;
import org.codedefenders.transaction.TransactionManager;
import org.codedefenders.transaction.TransactionalRunnable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MutleGuestCleanupJobTest {
    @TempDir
    Path root;

    @Test
    void cleansOnlyExpiredGuestArtifactsAndKeepsAccountAndFixtureFiles() throws Exception {
        Path guest = Files.createDirectories(root.resolve("mutle/101/8/original/1"));
        Files.writeString(guest.resolve("Test.java"), "guest");
        Path account = Files.createDirectories(root.resolve("mutle/102/104/original/1"));
        Files.writeString(account.resolve("Test.java"), "account");
        Path fixture = Files.writeString(root.resolve("Fixture.java"), "fixture");
        MutleRepository repository = mock(MutleRepository.class);
        when(repository.expiredGuestGames(any())).thenReturn(List.of(101));
        when(repository.deleteExpiredGuestGame(eq(101), any())).thenReturn(true);
        Configuration config = mock(Configuration.class);
        when(config.getTestsDir()).thenReturn(root.toFile());
        TransactionManager transactions = mock(TransactionManager.class);
        Transaction transaction = mock(Transaction.class);
        doAnswer(invocation -> {
            ((TransactionalRunnable) invocation.getArgument(0)).executeInTransaction(transaction);
            return null;
        }).when(transactions).executeInTransaction(any(TransactionalRunnable.class));

        new MutleGuestCleanupJob(repository, transactions, config).run();

        assertFalse(Files.exists(guest));
        assertTrue(Files.exists(account.resolve("Test.java")));
        assertTrue(Files.exists(fixture));
        verify(transaction).commit();
    }

    @Test
    void retriesFailedGamesWithoutBlockingLaterGamesInTheBatch() throws Exception {
        Path first = Files.createDirectories(root.resolve("mutle/101/8/original/1"));
        Path second = Files.createDirectories(root.resolve("mutle/102/8/original/1"));
        MutleRepository repository = mock(MutleRepository.class);
        when(repository.expiredGuestGames(any())).thenReturn(List.of(101, 102), List.of(101));
        when(repository.deleteExpiredGuestGame(eq(101), any())).thenReturn(true);
        when(repository.deleteExpiredGuestGame(eq(102), any())).thenReturn(true);
        Configuration config = mock(Configuration.class);
        when(config.getTestsDir()).thenReturn(root.toFile());
        TransactionManager transactions = mock(TransactionManager.class);
        Transaction transaction = mock(Transaction.class);
        doThrow(new SQLException("first game cannot be deleted"))
                .doAnswer(invocation -> {
                    ((TransactionalRunnable) invocation.getArgument(0)).executeInTransaction(transaction);
                    return null;
                }).when(transactions).executeInTransaction(any(TransactionalRunnable.class));

        MutleGuestCleanupJob job = new MutleGuestCleanupJob(repository, transactions, config);
        job.run();
        assertTrue(Files.exists(first));
        assertFalse(Files.exists(second));
        verify(transaction).commit();

        job.run();
        assertFalse(Files.exists(first));
        verify(transaction, times(2)).commit();
    }
}
