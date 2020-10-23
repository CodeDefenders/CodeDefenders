package org.codedefenders.beans.game;

import edu.emory.mathcs.backport.java.util.Collections;
import org.codedefenders.game.multiplayer.PlayerScore;
import org.codedefenders.model.Event;
import org.codedefenders.model.EventStatus;
import org.codedefenders.model.EventType;
import org.codedefenders.model.Player;

import javax.annotation.ManagedBean;
import javax.enterprise.context.RequestScoped;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * <p>Provides data for the history game component.</p>
 * <p>Bean Name: {@code history}</p>
 */
@ManagedBean
@RequestScoped
public class HistoryBean {
    private Integer gameId;

    private Map<Integer, PlayerScore> mutantsScores;
    private Map<Integer, PlayerScore> testScores;
    private List<HistoryBeanEventDTO> events;

    private List<Player> attackers;
    private List<Player> defenders;

    public HistoryBean() {
        gameId = null;
        mutantsScores = null;
        testScores = null;
    }

    public List<HistoryBeanEventDTO> getEvents() {
        return events;
    }

    public void setEvents(List<Event> events) {
        this.events = events.stream()
                .map(this::createUserMessage).filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private HistoryBeanEventDTO createUserMessage(Event e) {
        if (e.getUser().getId() < 100) {
            return null;
        }
        String userMessage = e.getUser().getUsername() + " ";
        switch (e.getEventType()) {
            case GAME_CREATED:
                userMessage += "created game";
                break;
            case GAME_STARTED:
                userMessage += "startet game";
                break;
            case GAME_FINISHED:
                userMessage += "finished game";
                break;
            case PLAYER_JOINED:
                userMessage += "joined";
                break;
            case ATTACKER_JOINED:
                if (e.getEventStatus() == EventStatus.NEW) {
                    return null;
                }
                userMessage += "joined as attacker";
                break;
            case DEFENDER_JOINED:
                if (e.getEventStatus() == EventStatus.NEW) {
                    return null;
                }
                userMessage += "joined as defender";
                break;
            case GAME_PLAYER_LEFT:
                userMessage += "left the game";
                break;
            case PLAYER_TEST_ERROR:
            case DEFENDER_TEST_READY:
                userMessage += "created a test that errored";
                break;
            case PLAYER_TEST_READY:
            case DEFENDER_TEST_ERROR:
                userMessage = "Test by " + userMessage + "is ready";
                break;
            case PLAYER_MUTANT_ERROR:
            case ATTACKER_MUTANT_ERROR:
                userMessage += "created a mutant that errored";
                break;
            case PLAYER_TEST_CREATED:
            case DEFENDER_TEST_CREATED:
                userMessage += "created a test";
                break;
            case PLAYER_KILLED_MUTANT:
            case DEFENDER_KILLED_MUTANT:
                userMessage += "killed a mutant";
                break;
            case PLAYER_MUTANT_CREATED:
            case ATTACKER_MUTANT_CREATED:
                userMessage += "created a mutant";
                break;
            case PLAYER_MUTANT_SURVIVED:
            case ATTACKER_MUTANT_SURVIVED:
                userMessage += "created a mutant that survived";
                break;
            //    case PLAYER_MUTANT_EQUIVALENT:
            case DEFENDER_MUTANT_EQUIVALENT:
                userMessage += "caught an equivalence";
                break;
            case PLAYER_WON_EQUIVALENT_DUEL:
                userMessage += "won an equivalence duel";
                break;
            case PLAYER_LOST_EQUIVALENT_DUEL:
                userMessage += "lost an equivalence duel";
                break;
            case PLAYER_MUTANT_CLAIMED_EQUIVALENT:
            case DEFENDER_MUTANT_CLAIMED_EQUIVALENT:
                userMessage += "claimed a mutant equivalent";
                break;
            case ATTACKER_MUTANT_KILLED_EQUIVALENT:
                userMessage += "proved a mutant non-equivalent";
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
        return new HistoryBeanEventDTO(
                e.getUser().getUsername(),
                new Timestamp(e.getTimestamp()),
                userMessage,
                e.getEventType());
    }

    public void setScores(Map<Integer, PlayerScore> mutantsScores, Map<Integer, PlayerScore> testScores) {
        this.mutantsScores = Collections.unmodifiableMap(mutantsScores);
        this.testScores = Collections.unmodifiableMap(testScores);
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

    public Map<Integer, PlayerScore> getMutantsScores() {
        return mutantsScores;
    }

    public Map<Integer, PlayerScore> getTestScores() {
        return testScores;
    }

    public List<Player> getAttackers() {
        return attackers;
    }

    public List<Player> getDefenders() {
        return defenders;
    }

    public static class HistoryBeanEventDTO {
        private final String userName;
        private final Timestamp time;
        private final String userMessage;
        private final EventType type;

        public HistoryBeanEventDTO(String userName, Timestamp time, String message, EventType type) {
            this.userName = userName;
            this.time = time;
            this.userMessage = message;
            this.type = type;
        }

        public String getUserName() {
            return userName;
        }

        public Timestamp getTime() {
            return time;
        }

        public String getUserMessage() {
            return userMessage;
        }

        public EventType getType() {
            return type;
        }
    }
}
