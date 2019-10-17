/*
 * Copyright (C) 2016-2019 Code Defenders contributors
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
package org.codedefenders.configuration;

import java.util.Properties;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NameClassPair;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;

/**
 * Taken from:
 * https://dzone.com/articles/how-to-inject-property-file-properties-with-cdi
 *
 * TODO For testing we might need to include "a mocked" set of configuration values:
 * https://dzone.com/articles/field-injection-when-mocking-frameworks-fail
 */
@ApplicationScoped
public class ConfigurationPropertyProducer {

    private Properties properties;

    @Property
    @Produces
    public String produceString(final InjectionPoint ip) {
        return this.properties.getProperty(getKey(ip));
    }

    @Property
    @Produces
    public int produceInt(final InjectionPoint ip) {
        return Integer.valueOf(this.properties.getProperty(getKey(ip)));
    }

    @Property
    @Produces
    public boolean produceBoolean(final InjectionPoint ip) {
        String key = getKey(ip);
        if( this.properties.containsKey(key) ){
            String value = this.properties.getProperty(key);
            return "enabled".equals( value );
        }else {
            return false;
        }
    }

    // TODO This can be improved to automatically try to resolve property names like: anotherProperty-> another.property
    private String getKey(final InjectionPoint ip) {
        return (ip.getAnnotated().isAnnotationPresent(Property.class)
                && !ip.getAnnotated().getAnnotation(Property.class).value().isEmpty())
                        ? ip.getAnnotated().getAnnotation(Property.class).value() : ip.getMember().getName();
    }

    @PostConstruct
    public void init() {
        /**
         * Under the assumption that those properties are NOT dynamically
         * changed after the startup of the execution we fill them here only
         * once... Otherwise, we need to update this implementation to include also DB provided configuration properties.
         */
        this.properties = new Properties();
        // Read all the properties from the context !
        InitialContext initialContext;
        try {
            initialContext = new InitialContext();
            /*
             * Lookup codedefenders namespace
             */
            NamingEnumeration<NameClassPair> list = initialContext.list("java:comp/env/codedefenders");
            Context environmentContext = (Context) initialContext.lookup("java:comp/env/codedefenders");

            while (list.hasMore()) {
                String name = list.next().getName();
                properties.put(name, (String) environmentContext.lookup(name));
//                System.out.println("ConfigurationPropertyProducer.init() Registering " + name + " with value " + environmentContext.lookup(name) );
            }

        } catch (NamingException e) {
            e.printStackTrace();
        }
    }
}
