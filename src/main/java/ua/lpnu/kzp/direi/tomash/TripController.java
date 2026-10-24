package ua.lpnu.kzp.direi.tomash;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Coordinates trip input, domain processing, report generation, and output.
 */
final class TripController {
    /**
     * Runs the selected application mode.
     *
     * @param options validated command-line options
     */
    void run(ArgumentsParser.Options options) {
        if (options.help()) {
            System.out.print(ArgumentsParser.helpText());
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
    }

    /**
     * Converts input lines to trips and formats statistics and validation errors.
     *
     * <p>Blank lines are ignored. Invalid records are excluded from statistics
     * and reported with their original line numbers.</p>
     *
     * @param lines input file lines in their original order
     * @return formatted report with statistics and validation errors
     */
    static String buildReport(List<String> lines) {
        List<String> errors = new ArrayList<>();
        List<Trip> trips = new ArrayList<>();

        for (int index = 0; index < lines.size(); index++) {
            String line = lines.get(index);
            if (line.isBlank()) {
                continue;
            }

            try {
                trips.add(Trip.fromCsv(line));
            } catch (IllegalArgumentException exception) {
                errors.add("Рядок %d: %s".formatted(index + 1, exception.getMessage()));
            }
        }

        MileageFuel totals = new MileageFuel(0.0, 0.0);
        double maxKm = Double.NEGATIVE_INFINITY;
        for (Trip trip : trips) {
            totals = totals.plus(trip);
            maxKm = Math.max(maxKm, trip.getKm());
        }

        double avgFuelPer100Km = totals.km() == 0.0
                ? 0.0 : (totals.fuelLiters() / totals.km()) * 100.0;
        if (trips.isEmpty()) {
            maxKm = 0.0;
        }

        StringBuilder reportBuilder = new StringBuilder();
        reportBuilder.append(String.format(Locale.ROOT, "Коректних записів: %d%n", trips.size()));
        reportBuilder.append(String.format(Locale.ROOT, "Сумарний кілометраж: %.2f км%n", totals.km()));
        reportBuilder.append(String.format(Locale.ROOT, "Середня витрата на 100 км: %.2f л/100км%n",
                avgFuelPer100Km));
        reportBuilder.append(String.format(Locale.ROOT, "Найбільша поїздка: %.2f км%n", maxKm));
        reportBuilder.append(String.format(Locale.ROOT, "Кількість помилок: %d%n", errors.size()));

        if (!errors.isEmpty()) {
            reportBuilder.append(String.format(Locale.ROOT, "Список помилок:%n"));
            for (String error : errors) {
                reportBuilder.append("%s%n".formatted(error));
            }
        }

        return reportBuilder.toString();
    }
}
