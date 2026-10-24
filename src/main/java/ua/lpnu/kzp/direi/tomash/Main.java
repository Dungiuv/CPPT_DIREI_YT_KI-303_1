package ua.lpnu.kzp.direi.tomash;

import java.util.Locale;

/**
 * Starts the application by parsing command-line arguments and invoking
 * the trip-processing controller.
 */
public final class Main {
    /** Prevents instantiation of this command-line entry point. */
    private Main() {
    }

    /**
     * Parses command-line arguments and starts the application.
     *
     * @param args command-line arguments
     */
    public static void main(String[] args) {
        try {
            ArgumentsParser.Options options = ArgumentsParser.parse(args);
            new TripController().run(options);
        } catch (IllegalArgumentException exception) {
            System.err.printf(Locale.ROOT, "Помилка аргументів: %s%n", exception.getMessage());
            System.err.print(ArgumentsParser.helpText());
        }
    }
}
