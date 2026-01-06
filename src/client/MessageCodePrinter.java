package client;

public interface MessageCodePrinter {
    static void printMessageFromCode(int code) {
        switch (code) {
            case 5000 -> System.out.println("User has already logged in."); // User probeert in te loggen op een user die al ingelogd is
            case 5001 -> System.out.println("Username has an invalid length or format.");
            case 5002 -> System.out.println("User cannot login twice"); // User that is already logged in is trying to log in
        }
    }
}
