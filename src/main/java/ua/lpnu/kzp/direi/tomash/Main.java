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

public final class Main {
    private static final Path DEFAULT_INPUT = Path.of("data", "input.csv");
    private static final Path DEFAULT_OUTPUT = Path.of("out", "report.txt");

    /* Забороняє створення екземплярів службового класу. */
    private Main() {
    }

    /**
     * Головна точка входу до програми.
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
            System.err.println("Помилка аргументів: " + exception.getMessage());
            System.err.print(helpText());
        }
    }

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

    private record Options(Path input, Path output, boolean help, boolean version) {
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
                            throw new IllegalArgumentException("невідома опція: " + argument);
                        }
                    }
                }
            }

            return new Options(input, output, help, version);
        }

        private static Path optionPath(String[] args, int valueIndex, String option) {
            if (valueIndex >= args.length) {
                throw new IllegalArgumentException("опція " + option + " потребує шлях");
            }
            return pathValue(args[valueIndex], option);
        }

        private static Path pathValue(String value, String option) {
            if (value.isBlank()) {
                throw new IllegalArgumentException("опція " + option + " потребує шлях");
            }
            return Path.of(value);
        }
    }

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
        reportBuilder.append(String.format(Locale.ROOT, "Середня витрата на 100 км: %.2f л/100км%n", avgFuelPer100Km));
        reportBuilder.append(String.format(Locale.ROOT, "Найбільша поїздка: %.2f км%n", maxKm));
        reportBuilder.append(String.format(Locale.ROOT, "Кількість помилок: %d%n", errors.size()));
        
        if (!errors.isEmpty()) {
            reportBuilder.append("Список помилок:\n");
            for (String err : errors) {
                reportBuilder.append(err).append("\n");
            }
        }

        return reportBuilder.toString();
    }
}