package ua.lpnu.kzp.direi.tomash;

import java.nio.file.Path;

/**
 * Parses and validates command-line options for the trip-processing
 * application.
 */
final class ArgumentsParser {
    /** Default path of the input CSV file. */
    private static final Path DEFAULT_INPUT = Path.of("data", "input.csv");

    /** Default path of the generated report. */
    private static final Path DEFAULT_OUTPUT = Path.of("out", "report.txt");

    /** Prevents instantiation of this command-line utility class. */
    private ArgumentsParser() {
    }

    /**
     * Parses command-line arguments and fills unspecified paths with defaults.
     *
     * @param args command-line arguments
     * @return options selected for this invocation
     * @throws IllegalArgumentException if an option is unknown or requires
     *         a missing or blank path
     */
    static Options parse(String[] args) {
        Path input = DEFAULT_INPUT;
        Path output = DEFAULT_OUTPUT;
        boolean help = false;
        boolean version = false;

        for (int index = 0; index < args.length; index++) {
            String argument = args[index];
            switch (argument) {
                case "--help", "-h" -> help = true;
                case "--version" -> version = true;
                case "--input", "-i" -> input = optionPath(args, ++index, argument);
                case "--output", "-o" -> output = optionPath(args, ++index, argument);
                default -> {
                    if (argument.startsWith("--input=")) {
                        input = pathValue(argument.substring("--input=".length()), "--input");
                    } else if (argument.startsWith("--output=")) {
                        output = pathValue(argument.substring("--output=".length()), "--output");
                    } else {
                        throw new IllegalArgumentException("невідома опція: %s".formatted(argument));
                    }
                }
            }
        }

        return new Options(input, output, help, version);
    }

    /**
     * Builds the usage text for the supported command-line options.
     *
     * @return usage instructions, terminated by the platform line separator
     */
    static String helpText() {
        return String.join(System.lineSeparator(),
                "Використання: java -jar target/labs-%s.jar [OPTIONS]"
                        .formatted(VersionInfo.projectVersion()),
                "  -h, --help             Показати цю довідку",
                "      --version          Показати версію проєкту",
                "  -i, --input PATH       Вказати CSV-файл (типово: data/input.csv)",
                "  -o, --output PATH      Вказати файл звіту (типово: out/report.txt)",
                "      --input=PATH       Альтернативний формат параметра input",
                "      --output=PATH      Альтернативний формат параметра output",
                "");
    }

    /**
     * Reads the path value following a separate-form option.
     *
     * @param args all command-line arguments
     * @param valueIndex index of the expected path value
     * @param option option name used in any error message
     * @return parsed path
     * @throws IllegalArgumentException if the path argument is missing or blank
     */
    private static Path optionPath(String[] args, int valueIndex, String option) {
        if (valueIndex >= args.length) {
            throw new IllegalArgumentException("опція %s потребує шлях".formatted(option));
        }
        return pathValue(args[valueIndex], option);
    }

    /**
     * Converts and validates a path value supplied for a command-line option.
     *
     * @param value path text
     * @param option option name used in any error message
     * @return the path represented by {@code value}
     * @throws IllegalArgumentException if the value is blank
     */
    private static Path pathValue(String value, String option) {
        if (value.isBlank()) {
            throw new IllegalArgumentException("опція %s потребує шлях".formatted(option));
        }
        return Path.of(value);
    }

    /**
     * Holds the input, output, and action options selected for one invocation.
     *
     * @param input path to the CSV input
     * @param output path to the generated report
     * @param help whether usage instructions should be printed
     * @param version whether the project version should be printed
     */
    record Options(Path input, Path output, boolean help, boolean version) {
    }
}
