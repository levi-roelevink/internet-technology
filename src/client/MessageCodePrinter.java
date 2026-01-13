package client;

public interface MessageCodePrinter {
    static void printMessageFromCode(int code) {
        switch (code) {
            case 1000 -> System.out.println("User has already logged in."); // User probeert in te loggen op een user die al ingelogd is
            case 1001 -> System.out.println("Username has an invalid length or format.");
            case 1002 -> System.out.println("User cannot login twice."); // User that is already logged in is trying to log in
            case 2000 -> System.out.println("User is not logged in.");
            case 5000 -> System.out.println("Invalid recipient provided.");
            case 7000 -> System.out.println("Pong timeout.");
            case 7001 -> System.out.println("Unterminated message.");
            case 8000 -> System.out.println("Pong without ping.");
            case 9000 -> System.out.println("Message couldn't be parsed.");
            case 9001 -> System.out.println("Unknown command requested.");
            default -> System.out.println("Unknown error.");
        }
    }
}
