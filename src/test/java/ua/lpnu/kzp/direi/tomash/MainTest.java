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

class MainTest {
    @TempDir
    Path tempDir;

    @Test
    void versionOptionPrintsProjectVersionWithoutReadingInput() {
        CommandResult result = runMain("--version");

        assertEquals("1.0.0" + System.lineSeparator(), result.stdout());
        assertEquals("", result.stderr());
    }

    @Test
    void helpOptionsPrintUsageWithoutReadingInput() {
        CommandResult longOption = runMain("--help");
        CommandResult shortOption = runMain("-h");

        assertTrue(longOption.stdout().contains("Використання:"));
        assertTrue(longOption.stdout().contains("--version"));
        assertTrue(longOption.stdout().contains("--input PATH"));
        assertTrue(longOption.stdout().contains("--output PATH"));
        assertEquals(longOption.stdout(), shortOption.stdout());
        assertEquals("", longOption.stderr());
    }

    @Test
    void inputAndOutputOptionsUseRequestedFiles() throws IOException {
        Path input = tempDir.resolve("trips.csv");
        Path output = tempDir.resolve("reports/daily/report.txt");
        Files.writeString(input, "Car;Driver;100;8;2026-09-28\n", StandardCharsets.UTF_8);

        CommandResult result = runMain("--input", input.toString(), "--output", output.toString());

        assertTrue(result.stdout().contains("Коректних записів: 1"));
        assertEquals(result.stdout(), Files.readString(output, StandardCharsets.UTF_8));
        assertEquals("", result.stderr());
    }

    @Test
    void shortAndEqualsFormsAreAccepted() throws IOException {
        Path input = tempDir.resolve("trips.csv");
        Path shortOutput = tempDir.resolve("short.txt");
        Path equalsOutput = tempDir.resolve("equals.txt");
        Files.writeString(input, "Car;Driver;50;5;2026-09-28\n", StandardCharsets.UTF_8);

        CommandResult shortResult = runMain("-i", input.toString(), "-o", shortOutput.toString());
        CommandResult equalsResult = runMain(
                "--input=" + input,
                "--output=" + equalsOutput);

        assertEquals(shortResult.stdout(), Files.readString(shortOutput, StandardCharsets.UTF_8));
        assertEquals(equalsResult.stdout(), Files.readString(equalsOutput, StandardCharsets.UTF_8));
        assertTrue(equalsResult.stdout().contains("Сумарний кілометраж: 50.00 км"));
        assertEquals("", shortResult.stderr());
        assertEquals("", equalsResult.stderr());
    }

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

    private record CommandResult(String stdout, String stderr) {
    }

    @Test
    void buildsStatisticsFromValidRowsAndTrimsFields() {
        String report = Main.buildReport(List.of(
                " Toyota ; Driver A ; 100 ; 8 ; 2026-09-28 ",
                "Van;Driver B;50;7;2026-09-27"));

        assertTrue(report.contains("Коректних записів: 2"));
        assertTrue(report.contains("Сумарний кілометраж: 150.00 км"));
        assertTrue(report.contains("Середня витрата на 100 км: 10.00 л/100км"));
        assertTrue(report.contains("Найбільша поїздка: 100.00 км"));
        assertTrue(report.contains("Кількість помилок: 0"));
    }

    @Test
    void reportsExactErrorForTooFewOrTooManyFields() {
        assertErrors(List.of(
                        "Car;Driver;1;1",
                        "Car;Driver;1;1;2026-09-28;extra"),
                "Рядок 1: очікується 5 полів, знайдено 4",
                "Рядок 2: очікується 5 полів, знайдено 6");
    }

    @Test
    void reportsExactErrorForBlankVehicle() {
        assertErrors(List.of(" ;Driver;1;1;2026-09-28"),
                "Рядок 1: порожня назва транспортного засобу або водія");
    }

    @Test
    void reportsExactErrorForBlankDriver() {
        assertErrors(List.of("Car; ;1;1;2026-09-28"),
                "Рядок 1: порожня назва транспортного засобу або водія");
    }

    @Test
    void reportsExactErrorForNonNumericKilometersAndFuel() {
        assertErrors(List.of(
                        "Car;Driver;many;1;2026-09-28",
                        "Car;Driver;1;many;2026-09-28"),
                "Рядок 1: числове поле має помилковий формат",
                "Рядок 2: числове поле має помилковий формат");
    }

    @Test
    void reportsExactErrorForNegativeKilometersAndFuel() {
        assertErrors(List.of(
                        "Car;Driver;-1;1;2026-09-28",
                        "Car;Driver;1;-1;2026-09-28"),
                "Рядок 1: кілометраж і паливо мають бути скінченними невід'ємними числами",
                "Рядок 2: кілометраж і паливо мають бути скінченними невід'ємними числами");
    }

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

    @Test
    void reportsExactErrorForMalformedAndImpossibleDates() {
        assertErrors(List.of(
                        "Car;Driver;1;1;28-09-2026",
                        "Car;Driver;1;1;2026-02-30"),
                "Рядок 1: дата має бути у форматі YYYY-MM-DD і бути коректною",
                "Рядок 2: дата має бути у форматі YYYY-MM-DD і бути коректною");
    }

    @Test
    void skipsBlankLinesAndKeepsOriginalLineNumbersInErrors() {
        String report = Main.buildReport(List.of(
                "",
                "Car;Driver;10;2;2026-09-28",
                "not-a-record"));

        assertTrue(report.contains("Коректних записів: 1"));
        assertTrue(report.contains("Кількість помилок: 1"));
        assertTrue(report.contains("Рядок 3: очікується 5 полів, знайдено 1"));
    }

    private static void assertErrors(List<String> rows, String... expectedErrors) {
        String report = Main.buildReport(rows);

        assertTrue(report.contains("Коректних записів: 0"));
        assertTrue(report.contains("Кількість помилок: " + expectedErrors.length));
        List<String> reportLines = report.lines().toList();
        for (String expectedError : expectedErrors) {
            assertEquals(1, reportLines.stream().filter(expectedError::equals).count(),
                    "Очікувалося точне повідомлення: " + expectedError);
        }
    }
}