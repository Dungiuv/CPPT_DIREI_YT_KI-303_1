package ua.lpnu.kzp.direi.tomash;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Verifies command-line handling, input validation, and report generation. */
class MainTest {
    /** Temporary directory used for isolated input and output files. */
    @TempDir
    Path tempDir;

    /** Verifies that the version option prints metadata without reading a CSV. */
    @Test
    void versionOptionPrintsProjectVersionWithoutReadingInput() {
        CommandResult result = runMain("--version");

        assertEquals("%s%n".formatted(VersionInfo.projectVersion()), result.stdout());
        assertEquals("", result.stderr());
    }

    /** Verifies that long and short help options print the same usage text. */
    @Test
    void helpOptionsPrintUsageWithoutReadingInput() {
        CommandResult longOption = runMain("--help");
        CommandResult shortOption = runMain("-h");

        assertTrue(longOption.stdout().contains("Використання:"));
        assertTrue(longOption.stdout().contains(
                "target/labs-%s.jar".formatted(VersionInfo.projectVersion())));
        assertTrue(longOption.stdout().contains("--version"));
        assertTrue(longOption.stdout().contains("--input PATH"));
        assertTrue(longOption.stdout().contains("--output PATH"));
        assertEquals(longOption.stdout(), shortOption.stdout());
        assertEquals("", longOption.stderr());
    }

    /** Verifies custom input and nested output paths are honored. */
    @Test
    void inputAndOutputOptionsUseRequestedFiles() throws IOException {
        Path input = tempDir.resolve("trips.csv");
        Path output = tempDir.resolve("reports/daily/report.txt");
        Files.writeString(input, "Car;Driver;100;8;2026-09-28%n".formatted(), StandardCharsets.UTF_8);

        CommandResult result = runMain("--input", input.toString(), "--output", output.toString());

        assertTrue(result.stdout().contains("Коректних записів: 1"));
        assertEquals(result.stdout(), Files.readString(output, StandardCharsets.UTF_8));
        assertEquals("", result.stderr());
    }

    /** Verifies short options and equals-sign path forms are accepted. */
    @Test
    void shortAndEqualsFormsAreAccepted() throws IOException {
        Path input = tempDir.resolve("trips.csv");
        Path shortOutput = tempDir.resolve("short.txt");
        Path equalsOutput = tempDir.resolve("equals.txt");
        Files.writeString(input, "Car;Driver;50;5;2026-09-28%n".formatted(), StandardCharsets.UTF_8);

        CommandResult shortResult = runMain("-i", input.toString(), "-o", shortOutput.toString());
        CommandResult equalsResult = runMain(
                "--input=%s".formatted(input),
                "--output=%s".formatted(equalsOutput));

        assertEquals(shortResult.stdout(), Files.readString(shortOutput, StandardCharsets.UTF_8));
        assertEquals(equalsResult.stdout(), Files.readString(equalsOutput, StandardCharsets.UTF_8));
        assertTrue(equalsResult.stdout().contains("Сумарний кілометраж: 50.00 км"));
        assertEquals("", shortResult.stderr());
        assertEquals("", equalsResult.stderr());
    }

    /** Verifies invalid or incomplete options produce useful error messages. */
    @Test
    void invalidOptionsPrintClearErrorsAndUsage() {
        CommandResult missingInputPath = runMain("--input");
        CommandResult emptyOutputPath = runMain("--output=");
        CommandResult unknownOption = runMain("--unknown");

        assertTrue(missingInputPath.stderr().contains("опція --input потребує шлях"));
        assertTrue(emptyOutputPath.stderr().contains("опція --output потребує шлях"));
        assertTrue(unknownOption.stderr().contains("невідома опція: --unknown"));
        assertTrue(unknownOption.stderr().contains("Використання:"));
    }

    /**
     * Runs the application while capturing its standard output and error streams.
     *
     * @param args command-line arguments passed to the application
     * @return captured output and error text
     */
    private static CommandResult runMain(String... args) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ByteArrayOutputStream error = new ByteArrayOutputStream();
        PrintStream originalOutput = System.out;
        PrintStream originalError = System.err;

        try (PrintStream capturedOutput = new PrintStream(output, true, StandardCharsets.UTF_8);
             PrintStream capturedError = new PrintStream(error, true, StandardCharsets.UTF_8)) {
            System.setOut(capturedOutput);
            System.setErr(capturedError);
            Main.main(args);
        } finally {
            System.setOut(originalOutput);
            System.setErr(originalError);
        }

        return new CommandResult(output.toString(StandardCharsets.UTF_8), error.toString(StandardCharsets.UTF_8));
    }

    /**
     * Captured standard output and error text from an application invocation.
     *
     * @param stdout text written to standard output
     * @param stderr text written to standard error
     */
    private record CommandResult(String stdout, String stderr) {
    }

    /** Verifies statistics use valid rows and ignore surrounding field whitespace. */
    @Test
    void buildsStatisticsFromValidRowsAndTrimsFields() {
        String report = TripController.buildReport(List.of(
                " Toyota ; Driver A ; 100 ; 8 ; 2026-09-28 ",
                "Van;Driver B;50;7;2026-09-27"));

        assertTrue(report.contains("Коректних записів: 2"));
        assertTrue(report.contains("Сумарний кілометраж: 150.00 км"));
        assertTrue(report.contains("Середня витрата на 100 км: 10.00 л/100км"));
        assertTrue(report.contains("Найбільша поїздка: 100.00 км"));
        assertTrue(report.contains("Кількість помилок: 0"));
    }

    /** Verifies rows with too few or too many fields are rejected precisely. */
    @Test
    void reportsExactErrorForTooFewOrTooManyFields() {
        assertErrors(List.of(
                        "Car;Driver;1;1",
                        "Car;Driver;1;1;2026-09-28;extra"),
                "Рядок 1: очікується 5 полів, знайдено 4",
                "Рядок 2: очікується 5 полів, знайдено 6");
    }

    /** Verifies a blank vehicle name is rejected. */
    @Test
    void reportsExactErrorForBlankVehicle() {
        assertErrors(List.of(" ;Driver;1;1;2026-09-28"),
                "Рядок 1: порожня назва транспортного засобу або водія");
    }

    /** Verifies a blank driver name is rejected. */
    @Test
    void reportsExactErrorForBlankDriver() {
        assertErrors(List.of("Car; ;1;1;2026-09-28"),
                "Рядок 1: порожня назва транспортного засобу або водія");
    }

    /** Verifies malformed distance and fuel values are rejected. */
    @Test
    void reportsExactErrorForNonNumericKilometersAndFuel() {
        assertErrors(List.of(
                        "Car;Driver;many;1;2026-09-28",
                        "Car;Driver;1;many;2026-09-28"),
                "Рядок 1: числове поле має помилковий формат",
                "Рядок 2: числове поле має помилковий формат");
    }

    /** Verifies negative distance and fuel values are rejected. */
    @Test
    void reportsExactErrorForNegativeKilometersAndFuel() {
        assertErrors(List.of(
                        "Car;Driver;-1;1;2026-09-28",
                        "Car;Driver;1;-1;2026-09-28"),
                "Рядок 1: кілометраж і паливо мають бути скінченними невід'ємними числами",
                "Рядок 2: кілометраж і паливо мають бути скінченними невід'ємними числами");
    }

    /** Verifies NaN and infinite distance or fuel values are rejected. */
    @Test
    void reportsExactErrorForNonFiniteKilometersAndFuel() {
        assertErrors(List.of(
                        "Car;Driver;NaN;1;2026-09-28",
                        "Car;Driver;Infinity;1;2026-09-28",
                        "Car;Driver;1;NaN;2026-09-28",
                        "Car;Driver;1;Infinity;2026-09-28"),
                "Рядок 1: кілометраж і паливо мають бути скінченними невід'ємними числами",
                "Рядок 2: кілометраж і паливо мають бути скінченними невід'ємними числами",
                "Рядок 3: кілометраж і паливо мають бути скінченними невід'ємними числами",
                "Рядок 4: кілометраж і паливо мають бути скінченними невід'ємними числами");
    }

    /** Verifies malformed and impossible ISO dates are rejected. */
    @Test
    void reportsExactErrorForMalformedAndImpossibleDates() {
        assertErrors(List.of(
                        "Car;Driver;1;1;28-09-2026",
                        "Car;Driver;1;1;2026-02-30"),
                "Рядок 1: дата має бути у форматі YYYY-MM-DD і бути коректною",
                "Рядок 2: дата має бути у форматі YYYY-MM-DD і бути коректною");
    }

    /** Verifies blank rows are skipped without changing reported source line numbers. */
    @Test
    void skipsBlankLinesAndKeepsOriginalLineNumbersInErrors() {
        String report = TripController.buildReport(List.of(
                "",
                "Car;Driver;10;2;2026-09-28",
                "not-a-record"));

        assertTrue(report.contains("Коректних записів: 1"));
        assertTrue(report.contains("Кількість помилок: 1"));
        assertTrue(report.contains("Рядок 3: очікується 5 полів, знайдено 1"));
    }

    /** Verifies invalid rows are excluded from the list used for statistics. */
    @Test
    void invalidRowsDoNotAffectStatisticsForValidTrips() {
        String report = TripController.buildReport(List.of(
                "Car;Driver A;100;8;2026-09-28",
                "Car;Driver B;-500;900;2026-09-27",
                "Van;Driver C;50;7;2026-09-26",
                "Truck;Driver D;many;30;2026-09-25"));

        assertTrue(report.contains("Коректних записів: 2"));
        assertTrue(report.contains("Сумарний кілометраж: 150.00 км"));
        assertTrue(report.contains("Середня витрата на 100 км: 10.00 л/100км"));
        assertTrue(report.contains("Найбільша поїздка: 100.00 км"));
        assertTrue(report.contains("Кількість помилок: 2"));
        assertTrue(report.contains("Рядок 2: кілометраж і паливо мають бути скінченними невід'ємними числами"));
        assertTrue(report.contains("Рядок 4: числове поле має помилковий формат"));
    }

    /**
     * Asserts that the report contains exactly the expected validation errors.
     *
     * @param rows input rows passed to the report builder
     * @param expectedErrors expected error messages, in any order
     */
    private static void assertErrors(List<String> rows, String... expectedErrors) {
        String report = TripController.buildReport(rows);

        assertTrue(report.contains("Коректних записів: 0"));
        assertTrue(report.contains("Кількість помилок: %d".formatted(expectedErrors.length)));
        List<String> reportLines = report.lines().toList();
        for (String expectedError : expectedErrors) {
            assertEquals(1, reportLines.stream().filter(expectedError::equals).count(),
                    "Очікувалося точне повідомлення: %s".formatted(expectedError));
        }
    }
}