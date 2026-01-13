package shared.utils;

import java.util.Scanner;

public class PromptUser {
    private static final Scanner scanner = new Scanner(System.in);

    public static int getIntBetweenBounds(int lowerBound, int upperBound) {
        int result = scanner.nextInt();

        while (result < lowerBound || result > upperBound) {
            System.out.printf("Input must be between %d and %d: ", lowerBound, upperBound);
            result = scanner.nextInt();
        }

        return result;
    }
}
