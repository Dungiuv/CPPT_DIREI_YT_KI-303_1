package ua.lpnu.kzp.direi.tomash;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Reads and validates fleet trip records and produces a summary report.
 *
 * <p>Each non-blank UTF-8 input line must contain five semicolon-separated
 * fields in this order: vehicle, driver, distance in kilometres, fuel in
 * litres, and an ISO-8601 date ({@code YYYY-MM-DD}). Invalid records are
 * excluded from the calculated statistics and reported with their line
 * numbers.</p>
 *
 * <p>The report is printed to the console and written to a file. The default
 * paths are {@code data/input.csv} and {@code out/report.txt}; both can be
 * overridden through command-line options.</p>
 */
public final class Main {
    /** Default path of the input CSV file. */
    private static final Path DEFAULT_INPUT = Path.of("data", "input.csv");

    /** Default path of the generated report. */
    private static final Path DEFAULT_OUTPUT = Path.of("out", "report.txt");

    /** Prevents instantiation of this command-line utility class. */
    private Main() {
    }

    /**
     * Runs the command-line application.
     *
     * <p>Help and version options are handled without reading the input file.
     * Otherwise, the selected CSV file is read as UTF-8, summarized, printed
     * to standard output, and saved to the selected output path. File and
     * argument errors are reported to standard error.</p>
     *
     * @param args аргументи командного рядка
     */
    public static void main(String[] args) {
        try {
            Options options = Options.parse(args);
            if (options.help()) {
                System.out.print(helpText());
                return;
            }
            if (options.version()) {
                System.out.println(VersionInfo.projectVersion());
                return;
            }

            List<String> lines;
            try {
                lines = Files.readAllLines(options.input(), StandardCharsets.UTF_8);
            } catch (IOException exception) {
                System.err.printf(Locale.ROOT, "Помилка читання файлу %s: %s%n",
                        options.input(), exception.getMessage());
                return;
            }

            String reportText = buildReport(lines);
            System.out.print(reportText);

            try {
                Path parent = options.output().getParent();
                if (parent != null) {
                    Files.createDirectories(parent);
                }
                Files.writeString(options.output(), reportText, StandardCharsets.UTF_8);
            } catch (IOException exception) {
                System.err.printf(Locale.ROOT, "Помилка запису файлу звіту %s: %s%n",
                        options.output(), exception.getMessage());
            }
        } catch (IllegalArgumentException exception) {
            System.err.println("Помилка аргументів: %s".formatted(exception.getMessage()));
            System.err.print(helpText());
        }
    }

    /**
     * Builds the usage text for the supported command-line options.
     *
     * @return usage instructions, terminated by the platform line separator
     */
    private static String helpText() {
        return String.join(System.lineSeparator(),
            "Використання: java -jar target/labs-1.0.0.jar [OPTIONS]",
            "  -h, --help             Показати цю довідку",
            "      --version          Показати версію проєкту",
            "  -i, --input PATH       Вказати CSV-файл (типово: data/input.csv)",
            "  -o, --output PATH      Вказати файл звіту (типово: out/report.txt)",
            "      --input=PATH       Альтернативний формат параметра input",
            "      --output=PATH      Альтернативний формат параметра output",
            "");
    }

    /**
     * Holds the input, output, and action options selected for one invocation.
     *
     * @param input path to the CSV input
     * @param output path to the generated report
     * @param help whether usage instructions should be printed
     * @param version whether the project version should be printed
     */
    private record Options(Path input, Path output, boolean help, boolean version) {
        /**
         * Parses command-line arguments and fills unspecified paths with defaults.
         *
         * @param args command-line arguments
         * @return parsed options
         * @throws IllegalArgumentException if an option is unknown or requires
         *         a missing or blank path
         */
        private static Options parse(String[] args) {
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
    }

    /**
     * Validates input rows and formats statistics for the valid trip records.
     *
     * <p>Blank lines are ignored. A non-blank row must have five fields, a
     * non-blank vehicle and driver, a valid ISO date, and finite non-negative
     * distance and fuel values. Invalid rows are omitted from the statistics
     * and included in the report's error list.</p>
     *
     * <p>The average fuel consumption is total fuel divided by total distance,
     * multiplied by 100. It is reported as zero when the total distance is
     * zero. If there are no valid records, the maximum trip distance is zero.</p>
     *
     * @param lines input file lines in their original order
     * @return formatted report with statistics and validation errors
     */
    static String buildReport(List<String> lines) {
        List<String> errors = new ArrayList<>();
        int validCount = 0;
        double totalKm = 0.0;
        double totalFuelLiters = 0.0;
        double maxKm = Double.NEGATIVE_INFINITY;

        for (int index = 0; index < lines.size(); index++) {
            String line = lines.get(index).trim();
            
            // Пропускаємо порожні рядки
            if (line.isEmpty()) {
                continue;
            }

            // Розділяємо рядок на поля за допомогою крапки з комою
            String[] fields = line.split(";", -1);
            if (fields.length != 5) {
                errors.add("Рядок %d: очікується 5 полів, знайдено %d".formatted(index + 1, fields.length));
                continue;
            }

            for (int fieldIndex = 0; fieldIndex < fields.length; fieldIndex++) {
                fields[fieldIndex] = fields[fieldIndex].trim();
            }

            // Перевіряємо текстові поля на пустоту (vehicle та driver)
            if (fields[0].isBlank() || fields[1].isBlank()) {
                errors.add("Рядок %d: порожня назва транспортного засобу або водія".formatted(index + 1));
                continue;
            }

            try {
                LocalDate.parse(fields[4]);
            } catch (DateTimeParseException exception) {
                errors.add("Рядок %d: дата має бути у форматі YYYY-MM-DD і бути коректною".formatted(index + 1));
                continue;
            }

            try {
                // Перетворення числових полів (km та fuelLiters)
                double km = Double.parseDouble(fields[2]);
                double fuelLiters = Double.parseDouble(fields[3]);

                // Відхиляємо від'ємні, нескінченні та нечислові значення.
                if (!Double.isFinite(km) || !Double.isFinite(fuelLiters) || km < 0 || fuelLiters < 0) {
                    errors.add("Рядок %d: кілометраж і паливо мають бути скінченними невід'ємними числами".formatted(index + 1));
                    continue;
                }

                // Збираємо статистику для валідних записів
                validCount++;
                totalKm += km;
                totalFuelLiters += fuelLiters;
                maxKm = Math.max(maxKm, km);

            } catch (NumberFormatException exception) {
                errors.add("Рядок %d: числове поле має помилковий формат".formatted(index + 1));
            }
        }

        // Обчислення середньої витрати палива на 100 км (захист від ділення на нуль)
        double avgFuelPer100Km = (totalKm == 0.0) ? 0.0 : (totalFuelLiters / totalKm) * 100.0;
        if (validCount == 0) {
            maxKm = 0.0; // Якщо немає валідних записів
        }

        // Формуємо текст звіту
        StringBuilder reportBuilder = new StringBuilder();
        reportBuilder.append(String.format(Locale.ROOT, "Коректних записів: %d%n", validCount));
        reportBuilder.append(String.format(Locale.ROOT, "Сумарний кілометраж: %.2f км%n", totalKm));
        reportBuilder.append(String.format(Locale.ROOT, "Середня витрата на 100 км: %.2f л/100км%n",
                avgFuelPer100Km));
        reportBuilder.append(String.format(Locale.ROOT, "Найбільша поїздка: %.2f км%n", maxKm));
        reportBuilder.append(String.format(Locale.ROOT, "Кількість помилок: %d%n", errors.size()));
        
        if (!errors.isEmpty()) {
            reportBuilder.append("Список помилок:%n".formatted());
            for (String err : errors) {
                reportBuilder.append("%s%n".formatted(err));
            }
        }

        return reportBuilder.toString();
    }
}