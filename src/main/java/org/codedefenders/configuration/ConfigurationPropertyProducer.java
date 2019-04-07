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
        if( this.properties.contains(getKey(ip)) ){
            return Boolean.valueOf(this.properties.getProperty(getKey(ip)));
        } else {
            return false;
        }
            
    }

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
            }

        } catch (NamingException e) {
            e.printStackTrace();
        }
    }
}