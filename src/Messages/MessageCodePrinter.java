package Messages;

public interface MessageCodePrinter {
    static void printMessageFromCode(int code) {
        switch (code) {
            case 1000 -> System.out.println("This username has already been taken");
            case 1001 -> System.out.println("Invalid username");
            case 1002 -> System.out.println("You are already logged in");
            case 2000 -> System.out.println("You are not logged in");
            case 3000 -> System.out.println("Connection timed out");
            case 3001 -> System.out.println("Message is too long to parse");
            case 4000 -> System.out.println("Pong sent without ping");
            case 5000 -> System.out.println("Invalid recipient provided");
            case 6000 -> System.out.println("Game has already been set up");
            case 6001 -> System.out.println("Game has already started");
            case 6002 -> System.out.println("No game is going on");
            case 6003 -> System.out.println("You are already a participant");
            case 6004 -> System.out.println("You are not a participant");
            case 6005 -> System.out.println("Not a number");
            case 6006 -> System.out.println("You have already guessed the number");
            case 7000 -> System.out.println("Sender has disconnected");
            default -> System.out.println("Unknown error");
        }
    }

    static void printHelpMessage() {
        System.out.println("help: show list of commands");
        System.out.println("logout: log out and exit");
        System.out.println("broadcast <message>: broadcast message to other users connected to the server");
        System.out.println("list_users: displays a list of currently connected users");
        System.out.println("private_message <user> <message>: send a private message to a user");
        System.out.println("number_setup: set up a number guessing game other users can join");
        System.out.println("number_join: join a number guessing game set up by another user");
        System.out.println("number_guess <number>: guess a number for the number guessing game");
        System.out.println("file_transfer <username> <filepath>: send a request to transfer a file to another user");
        System.out.println("file_accept <username>: accept a file transfer from a user");
        System.out.println("file_decline <username>: decline a file transfer from a user");
        System.out.println("encrypted_private_message <user> <message>: send an encrypted private message to a user");
    }
}
