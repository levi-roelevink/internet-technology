package protocoltests;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.*;
import protocoltests.protocol.messages.*;
import protocoltests.protocol.utils.Utils;

import java.io.*;
import java.net.Socket;
import java.util.Properties;

import static java.time.Duration.ofMillis;
import static org.junit.jupiter.api.Assertions.*;

class LineEndings {

    private static Properties props = new Properties();

    private Socket s;
    private BufferedReader in;
    private PrintWriter out;

    private final static int max_delta_allowed_ms = 100;

    @BeforeAll
    static void setupAll() throws IOException {
        InputStream in = LineEndings.class.getResourceAsStream("testconfig.properties");
        props.load(in);
        in.close();
    }

    @BeforeEach
    void setup() throws IOException {
        s = new Socket(props.getProperty("host"), Integer.parseInt(props.getProperty("port")));
        in = new BufferedReader(new InputStreamReader(s.getInputStream()));
        out = new PrintWriter(s.getOutputStream(), true);
    }

    @AfterEach
    void cleanup() throws IOException {
        s.close();
    }

    @Test
    void TC2_1_loginFollowedByBROADCASTWithWindowsLineEndingsReturnsOk() throws JsonProcessingException {
        receiveLineWithTimeout(in); //welcome message
        String message = Utils.objectToMessage(new Login("myname")) + "\r\n" +
                Utils.objectToMessage(new BroadcastReq("a")) + "\r\n";
        out.print(message);
        out.flush();
        String serverResponse = receiveLineWithTimeout(in);
        LoginResp loginResp = Utils.messageToObject(serverResponse);
        assertEquals("OK", loginResp.status());

        serverResponse = receiveLineWithTimeout(in);
        BroadcastResp broadcastResp = Utils.messageToObject(serverResponse);
        assertEquals("OK", broadcastResp.status());
    }

    @Test
    void TC2_2_loginFollowedByBROADCASTWithLinuxLineEndingsReturnsOk() throws JsonProcessingException {
        receiveLineWithTimeout(in); //welcome message
        String message = Utils.objectToMessage(new Login("myname")) + "\n" +
                Utils.objectToMessage(new BroadcastReq("a")) + "\n";
        out.print(message);
        out.flush();
        String serverResponse = receiveLineWithTimeout(in);
        LoginResp loginResp = Utils.messageToObject(serverResponse);
        assertEquals("OK", loginResp.status());

        serverResponse = receiveLineWithTimeout(in);
        BroadcastResp broadcastResp = Utils.messageToObject(serverResponse);
        assertEquals("OK", broadcastResp.status());
    }

    private String receiveLineWithTimeout(BufferedReader reader) {
        return assertTimeoutPreemptively(ofMillis(max_delta_allowed_ms), reader::readLine);
    }

}