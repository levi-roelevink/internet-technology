package shared.utils;

import java.util.Scanner;

public class PromptUser {
    private static final Scanner scanner = new Scanner(System.in);

    public static int getIntBetweenBounds(int lowerBound, int upperBound) {
        int result = scanner.nextInt();

        while (result < lowerBound || result > upperBound) {
            System.out.printf("Please enter a value between %d and %d: ", lowerBound, upperBound);
            result = scanner.nextInt();
        }

        // Consume leftover newline character
        scanner.nextLine();

        return result;
    }

    public static String promptForStringInput(String prompt) {
        System.out.print(prompt);
        String input = scanner.nextLine();

        while (input == null || input.isBlank()) {
            System.out.print(prompt);
            input = scanner.nextLine();
        }

        return input;
    }
}
