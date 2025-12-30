import client.GreetClient;
import client.GreetServer;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

public class TestGreeting {
    @Test
    public void givenGreetingClient_whenServerRespondsWhenStarted_thenCorrect() throws IOException {
        GreetClient client = new GreetClient();
        client.startConnection("127.0.0.1", 3000);
        String response = client.sendMessage("Hello server");
        assertEquals("Hello client", response);
    }
}
