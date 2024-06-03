/*
 * Copyright (C) 2021 Code Defenders contributors
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

package org.codedefenders.auth.annotation;

import java.io.Serializable;
import java.util.Optional;

import org.apache.shiro.SecurityUtils;
import org.jboss.weld.interceptor.WeldInvocationContext;

import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

@RequiresPermission("")
@Interceptor
public class RequiresPermissionInterceptor implements Serializable {

    public RequiresPermissionInterceptor() {
    }

    @AroundInvoke
    public Object checkPermission(InvocationContext ctx) throws Exception {
        WeldInvocationContext weldContext = (WeldInvocationContext) ctx;

        Optional<RequiresPermission> annotation = weldContext.getInterceptorBindingsByType(RequiresPermission.class)
                .stream().findFirst();

        if (annotation.isPresent()) {
            SecurityUtils.getSubject().checkPermission(annotation.get().value());
        }

        return ctx.proceed();
    }
}
