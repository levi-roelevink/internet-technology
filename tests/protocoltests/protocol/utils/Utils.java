package protocoltests.protocol.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import protocoltests.protocol.messages.*;

import java.util.HashMap;
import java.util.Map;

public class Utils {

    private final static ObjectMapper mapper = new ObjectMapper();
    private final static Map<Class<?>, String> objToNameMapping = new HashMap<>();
    static {
        objToNameMapping.put(Login.class, "LOGIN");
        objToNameMapping.put(LoginResp.class, "LOGIN_RESP");
        objToNameMapping.put(BroadcastReq.class, "BROADCAST_REQ");
        objToNameMapping.put(BroadcastResp.class, "BROADCAST_RESP");
        objToNameMapping.put(Broadcast.class, "BROADCAST");
        objToNameMapping.put(Joined.class, "JOINED");
        objToNameMapping.put(ParseError.class, "PARSE_ERROR");
        objToNameMapping.put(Pong.class, "PONG");
        objToNameMapping.put(PongError.class, "PONG_ERROR");
        objToNameMapping.put(Welcome.class, "WELCOME");
        objToNameMapping.put(Ping.class, "PING");
        objToNameMapping.put(ListUsersReq.class, "LIST_USERS_REQ");
        objToNameMapping.put(ListUsersResp.class, "LIST_USERS_RESP");
        objToNameMapping.put(PrivateMessageReq.class, "PRIVATE_MESSAGE_REQ");
        objToNameMapping.put(PrivateMessageResp.class, "PRIVATE_MESSAGE_RESP");
        objToNameMapping.put(PrivateMessage.class, "PRIVATE_MESSAGE");
        objToNameMapping.put(NumberSetupReq.class, "NUMBER_SETUP_REQ");
        objToNameMapping.put(NumberSetupResp.class, "NUMBER_SETUP_RESP");
        objToNameMapping.put(NumberSetup.class, "NUMBER_SETUP");
        objToNameMapping.put(NumberJoinReq.class, "NUMBER_JOIN_REQ");
        objToNameMapping.put(NumberJoinResp.class, "NUMBER_JOIN_RESP");
        objToNameMapping.put(NumberStart.class, "NUMBER_START");
        objToNameMapping.put(NumberCancel.class, "NUMBER_CANCEL");
        objToNameMapping.put(NumberGuessReq.class, "NUMBER_GUESS_REQ");
        objToNameMapping.put(NumberGuessResp.class, "NUMBER_GUESS_RESP");
        objToNameMapping.put(NumberResult.class, "NUMBER_RESULT");
    }

    public static String objectToMessage(Object object) throws JsonProcessingException {
        Class<?> clazz = object.getClass();
        String header = objToNameMapping.get(clazz);
        if (header == null) {
            throw new RuntimeException("Cannot convert this class to a message");
        }
        String body = mapper.writeValueAsString(object);
        return header + " " + body;
    }

    public static <T> T messageToObject(String message) throws JsonProcessingException {
        String[] parts = message.split(" ", 2);
        if (parts.length > 2 || parts.length == 0) {
            throw new RuntimeException("Invalid message");
        }
        String header = parts[0];
        String body = "{}";
        if (parts.length == 2) {
            body = parts[1];
        }
        Class<?> clazz = getClass(header);
        Object obj = mapper.readValue(body, clazz);
        return (T) clazz.cast(obj);
    }

    private static Class<?> getClass(String header) {
        return objToNameMapping.entrySet().stream()
                .filter(e -> e.getValue().equals(header))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Cannot find class belonging to header " + header));
    }
}
