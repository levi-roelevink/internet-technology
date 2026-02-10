package Server;

import Messages.NumberResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.PrintWriter;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

import static Messages.MessageSender.sendLine;

public class NumberGame {
    private final Map<String, PrintWriter> participants = new HashMap<>();
    private final Random random = new Random();
    private final List<NumberGameResult> results = new ArrayList<>();
    private final ObjectMapper mapper = new ObjectMapper();
    private final boolean testing = true;
    private NumberGameState state = NumberGameState.IDLE;
    private int number;
    private Timer timer = new Timer();
    private Instant start;

    public boolean isIdle() {
        return state.equals(NumberGameState.IDLE);
    }

    public boolean isRequested() {
        return state.equals(NumberGameState.REQUESTED);
    }

    public boolean isRunning() {
        return state.equals(NumberGameState.RUNNING);
    }

    public int getNumber() {
        return number;
    }

    public void setupGame(String username, PrintWriter writer) {
        state = NumberGameState.REQUESTED;
        participants.put(username, writer);
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                startGame();
            }
        }, 10000);
    }

    public boolean userHasJoined(String username) {
        return participants.containsKey(username);
    }

    public void joinGame(String username, PrintWriter writer) {
        assert !participants.containsKey(username);
        participants.put(username, writer);
    }

    public void startGame() {
        if (participants.size() > 1) {
            state = NumberGameState.RUNNING;
            if (testing) {
                //When testing, the number should be set to a known value, so the guessing can be tested.
                number = 20;
            } else {
                //This method will generate a random number from 0 to 49. Afterward 1 is added, so we end up with a random number between 1 and 50
                number = random.nextInt(50) + 1;
            }

            for (PrintWriter writer : participants.values()) {
                sendLine("NUMBER_START", writer);
            }
            start = Instant.now();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    endGame();
                }
            }, 2 * 60 * 1000);
        } else {
            state = NumberGameState.IDLE;
            for (PrintWriter writer : participants.values()) {
                sendLine("NUMBER_CANCEL", writer);
            }
        }
    }

    public boolean userHasGuessedNumber(String username) {
        for (NumberGameResult result : results) {
            if (result.username().equals(username)) {
                return true;
            }
        }
        return false;
    }

    public void addResult(String username) {
        Instant stop = Instant.now();
        NumberGameResult result = new NumberGameResult(username, (int) Duration.between(start, stop).toMillis());
        results.add(result);
        if (results.size() == participants.size()) {
            endGame();
        }
    }

    //This method can be called when the timer runs out or when every participant has guessed the number
    //
    //In order to prevent this method being called again by the timer after the latter condition triggers,
    //this method cancels the current timer and creates a new one
    public void endGame() {
        try {
            NumberResult numberResult = new NumberResult(results);
            for (PrintWriter writer : participants.values()) {
                sendLine("NUMBER_RESULT " + mapper.writeValueAsString(numberResult), writer);
            }
            participants.clear();
            results.clear();
            timer.cancel();
            timer = new Timer();
            state = NumberGameState.IDLE;
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private enum NumberGameState {
        IDLE,
        REQUESTED,
        RUNNING
    }
}
