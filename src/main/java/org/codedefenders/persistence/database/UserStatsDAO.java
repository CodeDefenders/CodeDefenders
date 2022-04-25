package org.codedefenders.persistence.database;


import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.codedefenders.database.ConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class UserStatsDAO {
    private static final Logger logger = LoggerFactory.getLogger(UserStatsDAO.class);

    private final ConnectionFactory connectionFactory;

    @Inject
    private UserStatsDAO(ConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }
}
