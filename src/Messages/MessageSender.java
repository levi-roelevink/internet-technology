package Messages;

import java.io.PrintWriter;

public interface MessageSender {
    static void sendLine(String message, PrintWriter writer) {
        writer.println(message);
        writer.flush();
    }
}
