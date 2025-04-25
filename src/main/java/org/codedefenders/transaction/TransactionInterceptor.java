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
package org.codedefenders.transaction;

import java.io.Serializable;
import java.util.Optional;

import jakarta.inject.Inject;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

@Transactional
@Interceptor
public class TransactionInterceptor implements Serializable { // TODO(Alex): Check if Serializable is really necessary?!

    private final TransactionManager txManager;

    @Inject
    public TransactionInterceptor(TransactionManager txManager) {
        this.txManager = txManager;
    }

    @SuppressWarnings("unused")
    @AroundInvoke
    public Object manageTransaction(InvocationContext ctx) throws Exception {
        Optional<Transactional> annotation = ctx.getInterceptorBindings(Transactional.class).stream().findFirst();
        Integer transactionIsolation = null;
        if (annotation.isPresent()) {
            int value = annotation.get().value();
            if (value != -1) {
                transactionIsolation = value;
            }
        }

        TransactionalSupplier<Object> txExec = transaction -> {
            Object result = ctx.proceed();
            transaction.commit();
            return result;
        };
        return txManager.executeInTransaction(txExec, transactionIsolation);
    }
}
