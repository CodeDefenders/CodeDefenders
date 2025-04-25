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
package org.codedefenders.beans.game;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import jakarta.enterprise.context.RequestScoped;

import org.codedefenders.auth.CodeDefendersAuth;
import org.codedefenders.database.EventDAO;
import org.codedefenders.dto.SimpleUser;
import org.codedefenders.model.Event;
import org.codedefenders.model.EventStatus;
import org.codedefenders.model.EventType;
import org.codedefenders.model.Player;
import org.codedefenders.service.UserService;
import org.codedefenders.util.CDIUtil;

/**
 * <p>Provides data for the history game component.</p>
 * <p>Bean Name: {@code history}</p>
 */
@RequestScoped
public class HistoryBean {

    // TODO: Replace this with proper @Inject if this is completly managed by CDI (not jsp:useBean pseudo CDI …)
    EventDAO eventDAO = CDIUtil.getBeanFromCDI(EventDAO.class);
    UserService userService = CDIUtil.getBeanFromCDI(UserService.class);

    private Integer gameId;
    private CodeDefendersAuth login;

    private List<HistoryBeanEventDTO> events;

    private List<Player> attackers;
    private List<Player> defenders;

    public HistoryBean() {
        gameId = null;
    }

    public void setLogin(CodeDefendersAuth login) {
        this.login = login;
    }

    public List<HistoryBeanEventDTO> getEvents() {
        if (events == null) {
            events = eventDAO.getEventsForGame(gameId).stream()
                    .map(this::createHistoryBeanEvent).filter(Objects::nonNull)
                    .collect(Collectors.toList());
        }
        return events;
    }

    private HistoryBeanEventDTO createHistoryBeanEvent(Event e) {
        if (e.getUserId() < 100) {
            return null;
        }
        String userName = userService.getSimpleUserById(e.getUserId())
                .map(SimpleUser::getName)
                .orElse("Unknown user");
        String userMessage = userName + " ";
        String colour = "gray";
        switch (e.getEventType()) {
            case GAME_CREATED:
                userMessage += "created game";
                break;
            case GAME_STARTED:
                userMessage += "started game";
                break;
            case GAME_FINISHED:
                userMessage += "finished game";
                break;
            case PLAYER_JOINED:
                if (e.getEventStatus() == EventStatus.NEW) {
                    return null;
                }
                userMessage += "joined";
                colour = "blue";
                break;
            case ATTACKER_JOINED:
                if (e.getEventStatus() == EventStatus.NEW) {
                    return null;
                }
                userMessage += "joined as attacker";
                colour = "blue";
                break;
            case DEFENDER_JOINED:
                if (e.getEventStatus() == EventStatus.NEW) {
                    return null;
                }
                userMessage += "joined as defender";
                colour = "blue";
                break;
            case GAME_PLAYER_LEFT:
                userMessage += "left the game";
                break;
            case PLAYER_TEST_ERROR:
            case DEFENDER_TEST_READY:
                userMessage += "created a test that errored";
                colour = "red";
                break;
            case PLAYER_TEST_READY:
            case DEFENDER_TEST_ERROR:
                userMessage = "Test by " + userMessage + "is ready";
                colour = "green";
                break;
            case PLAYER_MUTANT_ERROR:
            case ATTACKER_MUTANT_ERROR:
                userMessage += "created a mutant that errored";
                colour = "red";
                break;
            case PLAYER_TEST_CREATED:
            case DEFENDER_TEST_CREATED:
                userMessage += "created a test";
                colour = "green";
                break;
            case PLAYER_KILLED_MUTANT:
            case DEFENDER_KILLED_MUTANT:
                userMessage += "killed a mutant";
                colour = "red";
                break;
            case PLAYER_MUTANT_CREATED:
            case ATTACKER_MUTANT_CREATED:
                userMessage += "created a mutant";
                colour = "green";
                break;
                /*
            case PLAYER_MUTANT_SURVIVED:
            case ATTACKER_MUTANT_SURVIVED:


                userMessage += "created a mutant that survived";
                break;

                 */
            case DEFENDER_MUTANT_EQUIVALENT:
            case PLAYER_MUTANT_EQUIVALENT:
                if (e.getEventStatus() == EventStatus.NEW) {
                    return null;
                }
                userMessage += "caught an equivalence";
                colour = "yellow";
                break;
            case PLAYER_WON_EQUIVALENT_DUEL:
                if (e.getEventStatus() == EventStatus.NEW) {
                    return null;
                }
                userMessage += "won an equivalence duel";
                colour = "green";
                break;
            case PLAYER_LOST_EQUIVALENT_DUEL:
                userMessage += "lost an equivalence duel";
                colour = "red";
                break;
            case PLAYER_MUTANT_CLAIMED_EQUIVALENT:
            case DEFENDER_MUTANT_CLAIMED_EQUIVALENT:
                userMessage += "claimed a mutant equivalent";
                colour = "yellow";
                break;
            case ATTACKER_MUTANT_KILLED_EQUIVALENT:
                userMessage += "proved a mutant non-equivalent";
                colour = "yellow";
                break;
            case GAME_MESSAGE:
            case GAME_GRACE_ONE:
            case GAME_GRACE_TWO:
            case ATTACKER_MESSAGE:
            case DEFENDER_MESSAGE:
            case GAME_MESSAGE_PLAYER:
            case GAME_MESSAGE_ATTACKER:
            case GAME_MESSAGE_DEFENDER:
            default:
                return null;
        }
        String alignment;
        if (e.getEventType().toString().matches("DEFENDER")) {
            alignment = "right";
        } else if (e.getEventType().toString().matches("ATTACKER")) {
            alignment = "left";
        } else if (e.getEventType().toString().matches("GAME")) {
            alignment = "right";
        } else {
            alignment = login.getUserId() == e.getUserId() ? "left" : "right";
        }
        return new HistoryBeanEventDTO(
                userName,
                new Timestamp(e.getTimestamp()),
                userMessage,
                e.getEventType(),
                alignment,
                colour
        );
    }

    public void setPlayers(List<Player> attackers, List<Player> defenders) {
        this.attackers = Collections.unmodifiableList(attackers);
        this.defenders = Collections.unmodifiableList(defenders);
    }

    public int getGameId() {
        return gameId;
    }

    // --------------------------------------------------------------------------------

    public void setGameId(int gameId) {
        this.gameId = gameId;
    }

    public List<Player> getAttackers() {
        return attackers;
    }

    public List<Player> getDefenders() {
        return defenders;
    }

    public static class HistoryBeanEventDTO {
        private final String userName;
        private final LocalDateTime time;
        private final LocalDateTime today;
        private final String userMessage;
        private final EventType type;
        private final String alignment;
        private final String colour;

        public HistoryBeanEventDTO(String userName, Timestamp time, String message, EventType type, String alignment,
                String colour) {
            this.userName = userName;
            this.time = time.toLocalDateTime();
            LocalDateTime now = LocalDateTime.now();
            // Today at midnight
            today = LocalDateTime.of(now.getYear(), now.getMonth(), now.getDayOfMonth(), 0, 0);
            this.userMessage = message;
            this.type = type;
            this.alignment = alignment;
            this.colour = colour;
        }

        public String getAlignment() {
            return alignment;
        }

        public String getColour() {
            return colour;
        }

        public String getUserName() {
            return userName;
        }

        public String getDate() {
            if (time.isAfter(today)) {
                return "Today";
            } else if (time.isAfter(today.minusDays(1))) {
                return "Yesterday";
            } else {
                return time.getDayOfMonth() + "." + time.getMonth().ordinal() + "." + time.getYear();
            }
        }

        public String getTime() {
            return String.format("%02d", time.getHour()) + ":" + String.format("%02d", time.getMinute());
        }

        public String getFormat() {
            // For the game_history.jsp in the format "2014-01-10T03:45"
            return time.getYear()
                    + "-" + String.format("%02d", time.getMonth().ordinal())
                    + "-" + String.format("%02d", time.getDayOfMonth()) + "T"
                    + getTime();
        }

        public String getUserMessage() {
            return userMessage;
        }

        public EventType getType() {
            return type;
        }
    }
}
