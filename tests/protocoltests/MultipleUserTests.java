package protocoltests;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.*;
import protocoltests.protocol.messages.*;
import protocoltests.protocol.utils.Utils;

import java.io.*;
import java.net.Socket;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import static java.time.Duration.ofMillis;
import static org.junit.jupiter.api.Assertions.*;

class MultipleUserTests {

    private static Properties props = new Properties();
    private Socket socketUser1, socketUser2;
    private BufferedReader inUser1, inUser2;
    private PrintWriter outUser1, outUser2;

    private final static int max_delta_allowed_ms = 100;

    @BeforeAll
    static void setupAll() throws IOException {
        InputStream in = MultipleUserTests.class.getResourceAsStream("testconfig.properties");
        props.load(in);
        in.close();
    }

    @BeforeEach
    void setup() throws IOException {
        socketUser1 = new Socket(props.getProperty("host"), Integer.parseInt(props.getProperty("port")));
        inUser1 = new BufferedReader(new InputStreamReader(socketUser1.getInputStream()));
        outUser1 = new PrintWriter(socketUser1.getOutputStream(), true);

        socketUser2 = new Socket(props.getProperty("host"), Integer.parseInt(props.getProperty("port")));
        inUser2 = new BufferedReader(new InputStreamReader(socketUser2.getInputStream()));
        outUser2 = new PrintWriter(socketUser2.getOutputStream(), true);
    }

    @AfterEach
    void cleanup() throws IOException {
        socketUser1.close();
        socketUser2.close();
    }

    @Test
    void TC3_1_joinedIsReceivedByOtherUserWhenUserConnects() throws JsonProcessingException {
        receiveLineWithTimeout(inUser1); //WELCOME
        receiveLineWithTimeout(inUser2); //WELCOME

        // Connect user1
        outUser1.println(Utils.objectToMessage(new Login("user1")));
        outUser1.flush();
        receiveLineWithTimeout(inUser1); //OK

        // Connect user2
        outUser2.println(Utils.objectToMessage(new Login("user2")));
        outUser2.flush();
        receiveLineWithTimeout(inUser2); //OK

        //JOINED is received by user1 when user2 connects
        String resIdent = receiveLineWithTimeout(inUser1);
        Joined joined = Utils.messageToObject(resIdent);

        assertEquals(new Joined("user2"),joined);
    }

    @Test
    void TC3_2_broadcastMessageIsReceivedByOtherConnectedClients() throws JsonProcessingException {
        receiveLineWithTimeout(inUser1); //WELCOME
        receiveLineWithTimeout(inUser2); //WELCOME

        // Connect user1
        outUser1.println(Utils.objectToMessage(new Login("user1")));
        outUser1.flush();
        receiveLineWithTimeout(inUser1); //OK

        // Connect user2
        outUser2.println(Utils.objectToMessage(new Login("user2")));
        outUser2.flush();
        receiveLineWithTimeout(inUser2); //OK
        receiveLineWithTimeout(inUser1); //JOINED

        //send BROADCAST from user 1
        outUser1.println(Utils.objectToMessage(new BroadcastReq("messagefromuser1")));

        outUser1.flush();
        String fromUser1 = receiveLineWithTimeout(inUser1);
        BroadcastResp broadcastResp1 = Utils.messageToObject(fromUser1);

        assertEquals("OK", broadcastResp1.status());

        String fromUser2 = receiveLineWithTimeout(inUser2);
        Broadcast broadcast2 = Utils.messageToObject(fromUser2);

        assertEquals(new Broadcast("user1","messagefromuser1"), broadcast2);

        //send BROADCAST from user 2
        outUser2.println(Utils.objectToMessage(new BroadcastReq("messagefromuser2")));
        outUser2.flush();
        fromUser2 = receiveLineWithTimeout(inUser2);
        BroadcastResp broadcastResp2 = Utils.messageToObject(fromUser2);
        assertEquals("OK", broadcastResp2.status());

        fromUser1 = receiveLineWithTimeout(inUser1);
        Broadcast broadcast1 = Utils.messageToObject(fromUser1);

        assertEquals(new Broadcast("user2","messagefromuser2"), broadcast1);
    }

    @Test
    void TC3_3_loginMessageWithAlreadyConnectedUsernameReturnsError() throws JsonProcessingException {
        receiveLineWithTimeout(inUser1); //welcome message
        receiveLineWithTimeout(inUser2); //welcome message

        // Connect user 1
        outUser1.println(Utils.objectToMessage(new Login("user1")));
        outUser1.flush();
        receiveLineWithTimeout(inUser1); //OK

        // Connect using same username
        outUser2.println(Utils.objectToMessage(new Login("user1")));
        outUser2.flush();
        String resUser2 = receiveLineWithTimeout(inUser2);
        LoginResp loginResp = Utils.messageToObject(resUser2);
        assertEquals(new LoginResp("ERROR", 1000), loginResp);
    }

    @Test
    void TC3_4_listUsersRequestReturnsListOfCurrentlyConnectedUsers() throws JsonProcessingException {
        receiveLineWithTimeout(inUser1); //WELCOME
        receiveLineWithTimeout(inUser2); //WELCOME

        // Connect user1
        outUser1.println(Utils.objectToMessage(new Login("user1")));
        outUser1.flush();
        receiveLineWithTimeout(inUser1); //OK

        // Connect user2
        outUser2.println(Utils.objectToMessage(new Login("user2")));
        outUser2.flush();
        receiveLineWithTimeout(inUser2); //OK
        receiveLineWithTimeout(inUser1); //JOINED

        //Request for list of users by user1
        outUser1.println(Utils.objectToMessage(new ListUsersReq()));
        outUser1.flush();

        String listForUser1 = receiveLineWithTimeout(inUser1);
        ListUsersResp listUsersResp = Utils.messageToObject(listForUser1);

        Set<String> expected = new HashSet<>();
        expected.add("user1");
        expected.add("user2");
        assertEquals(expected, listUsersResp.userList());
    }

    @Test
    void TC3_5_privateMessageIsReceivedByIntendedRecipient() throws JsonProcessingException {
        receiveLineWithTimeout(inUser1); //WELCOME
        receiveLineWithTimeout(inUser2); //WELCOME

        //Connect user1
        outUser1.println(Utils.objectToMessage(new Login("user1")));
        outUser1.flush();
        receiveLineWithTimeout(inUser1); //OK

        //Connect user2
        outUser2.println(Utils.objectToMessage(new Login("user2")));
        outUser2.flush();
        receiveLineWithTimeout(inUser2); //OK
        receiveLineWithTimeout(inUser1); //JOINED

        //Send private_message from user1 to user2
        outUser1.println(Utils.objectToMessage(new PrivateMessageReq("messagefromuser1", "user2")));
        outUser1.flush();

        String fromUser1 = receiveLineWithTimeout(inUser1);
        PrivateMessageResp privateMessageResp = Utils.messageToObject(fromUser1);

        assertEquals("OK", privateMessageResp.status());

        String fromUser2 = receiveLineWithTimeout(inUser2);
        PrivateMessage privateMessage = Utils.messageToObject(fromUser2);

        assertEquals(new PrivateMessage("user1", "messagefromuser1"), privateMessage);
    }

    @Test
    void TC3_6_privateMessageIsNotReceivedByNotIntendedRecipient() throws JsonProcessingException {
        receiveLineWithTimeout(inUser1); //WELCOME
        receiveLineWithTimeout(inUser2); //WELCOME

        //Connect user1
        outUser1.println(Utils.objectToMessage(new Login("user1")));
        outUser1.flush();
        receiveLineWithTimeout(inUser1); //OK

        //Connect user2
        outUser2.println(Utils.objectToMessage(new Login("user2")));
        outUser2.flush();
        receiveLineWithTimeout(inUser2); //OK
        receiveLineWithTimeout(inUser1); //JOINED

        //Send private_message from user1 to user999
        outUser1.println(Utils.objectToMessage(new PrivateMessageReq("messagefromuser1", "user999")));
        outUser1.flush();

        String fromUser1 = receiveLineWithTimeout(inUser1);
        PrivateMessageResp privateMessageResp = Utils.messageToObject(fromUser1);

        assertEquals("ERROR", privateMessageResp.status());

        //Send private_message from user1 to user2
        outUser1.println(Utils.objectToMessage(new PrivateMessageReq("secondmessagefromuser1", "user2")));
        outUser1.flush();

        String secondFromUser1 = receiveLineWithTimeout(inUser1);
        PrivateMessageResp privateMessageResp2 = Utils.messageToObject(secondFromUser1);

        assertEquals("OK", privateMessageResp2.status());

        String fromUser2 = receiveLineWithTimeout(inUser2);
        PrivateMessage privateMessage = Utils.messageToObject(fromUser2);

        assertEquals(new PrivateMessage("user1", "secondmessagefromuser1"), privateMessage);
    }

    //In order for this test to consistently work, the boolean "testing" in the NumberGame class has to be set to true
    @Test
    void TC3_7_numberGameProceedsAsExpected() throws JsonProcessingException, InterruptedException {
        receiveLineWithTimeout(inUser1); //WELCOME
        receiveLineWithTimeout(inUser2); //WELCOME

        // Connect user1
        outUser1.println(Utils.objectToMessage(new Login("user1")));
        outUser1.flush();
        receiveLineWithTimeout(inUser1); //OK

        // Connect user2
        outUser2.println(Utils.objectToMessage(new Login("user2")));
        outUser2.flush();
        receiveLineWithTimeout(inUser2); //OK
        receiveLineWithTimeout(inUser1); //JOINED

        outUser1.println(Utils.objectToMessage(new NumberSetupReq()));
        outUser1.flush();

        receiveLineWithTimeout(inUser1); //OK
        String resUser2 = receiveLineWithTimeout(inUser2);
        assertEquals(new NumberSetup("user1"), Utils.messageToObject(resUser2));

        //Join number game
        outUser2.println(Utils.objectToMessage(new NumberJoinReq()));
        outUser2.flush();
        resUser2 = receiveLineWithTimeout(inUser2);
        NumberJoinResp numberJoinResp = Utils.messageToObject(resUser2);
        assertEquals("OK", numberJoinResp.status());

        Thread.sleep(10000);

        String resUser1 = receiveLineWithTimeout(inUser1); //PING or START
        catchPingUser1(resUser1);

        resUser2 = receiveLineWithTimeout(inUser2);
        resUser2 = catchPingUser2(resUser2);
        NumberStart numberStart = Utils.messageToObject(resUser2);
        assertNotNull(numberStart);

        //Guess too low
        outUser2.println(Utils.objectToMessage(new NumberGuessReq(19)));
        outUser2.flush();
        resUser2 = receiveLineWithTimeout(inUser2);
        resUser2 = catchPingUser2(resUser2);
        NumberGuessResp numberGuessResp = Utils.messageToObject(resUser2);
        assertEquals(-1, numberGuessResp.code());

        //Guess too high
        outUser2.println(Utils.objectToMessage(new NumberGuessReq(21)));
        outUser2.flush();
        resUser2 = receiveLineWithTimeout(inUser2);
        resUser2 = catchPingUser2(resUser2);
        numberGuessResp = Utils.messageToObject(resUser2);
        assertEquals(1, numberGuessResp.code());

        //Guess correct
        outUser2.println(Utils.objectToMessage(new NumberGuessReq(20)));
        outUser2.flush();
        resUser2 = receiveLineWithTimeout(inUser2);
        resUser2 = catchPingUser2(resUser2);
        numberGuessResp = Utils.messageToObject(resUser2);
        assertEquals(0, numberGuessResp.code());

        //Check if results are sent
        outUser1.println(Utils.objectToMessage(new NumberGuessReq(20)));
        outUser1.flush();
        receiveLineWithTimeout(inUser1);
        catchPingUser1(resUser1);
        resUser2 = receiveLineWithTimeout(inUser2);
        resUser2 = catchPingUser2(resUser2);
        NumberResult numberResult = Utils.messageToObject(resUser2);
        assertNotNull(numberResult);
    }

    private void catchPingUser1(String message) {
        if (message.equals("PING")) {
            outUser1.println("PONG");
            outUser1.flush();
            receiveLineWithTimeout(inUser1);
        }
    }

    private String catchPingUser2(String message) {
        if (message.equals("PING")) {
            outUser2.println("PONG");
            outUser2.flush();
            return receiveLineWithTimeout(inUser2);
        } else {
            return message;
        }
    }

    private String receiveLineWithTimeout(BufferedReader reader) {
        return assertTimeoutPreemptively(ofMillis(max_delta_allowed_ms), reader::readLine);
    }
}