import client.Client;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

public class TestGreeting {
    public static Client client;

    @BeforeAll
    public static void setup() throws IOException {
        client = new Client();
        client.startConnection("127.0.0.1", 3000);
    }

    @AfterAll
    public static void tearDown() throws IOException {
        client.stopConnection();
    }

    @Test
    public void givenGreetingClient_whenServerRespondsWhenStarted_thenCorrect() throws IOException {
        String resp1 = client.sendMessage("Hello server");
        String resp2 = client.sendMessage("I am Bob");
        String resp3 = client.sendMessage(".");

        assertEquals("Hello server", resp1);
        assertEquals("I am Bob", resp2);
        assertEquals("Goodbye", resp3);
    }
}
