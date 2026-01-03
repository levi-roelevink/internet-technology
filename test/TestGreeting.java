import client.Client;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

public class TestGreeting {
    @Test
    public void givenClient1_whenServerResponds_thenCorrect() throws IOException {
        Client client = new Client();
        client.startConnection("127.0.0.1", 3000);

        String resp1 = client.sendMessage("Hello server");
        String resp2 = client.sendMessage("I am Bob");
        String resp3 = client.sendMessage(".");

        assertEquals("Hello server", resp1);
        assertEquals("I am Bob", resp2);
        assertEquals("Goodbye", resp3);
    }

    @Test
    public void givenClient2_whenServerResponds_thenCorrect() throws IOException {
        Client client = new Client();
        client.startConnection("127.0.0.1", 3000);

        String resp1 = client.sendMessage("Hello server");
        String resp2 = client.sendMessage("I am Hank");
        String resp3 = client.sendMessage(".");

        assertEquals("Hello server", resp1);
        assertEquals("I am Hank", resp2);
        assertEquals("Goodbye", resp3);
    }
}
