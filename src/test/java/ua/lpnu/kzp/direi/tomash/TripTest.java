package ua.lpnu.kzp.direi.tomash;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

/** Verifies CSV conversion and value behavior for trip domain objects. */
class TripTest {
    /** Verifies a valid constructor call retains each supplied component. */
    @Test
    void constructorStoresValidValues() {
        LocalDate date = LocalDate.parse("2026-09-28");
        Trip trip = new Trip("Toyota", "Driver A", 100.5, 8.25, date);

        assertEquals("Toyota", trip.getVehicle());
        assertEquals("Driver A", trip.getDriver());
        assertEquals(100.5, trip.getKm());
        assertEquals(8.25, trip.getFuelLiters());
        assertEquals(date, trip.getDate());
    }

    /** Verifies null reference fields are rejected by the constructor. */
    @Test
    void constructorRejectsNullValues() {
        LocalDate date = LocalDate.parse("2026-09-28");

        assertThrows(NullPointerException.class, () -> new Trip(null, "Driver A", 1, 1, date));
        assertThrows(NullPointerException.class, () -> new Trip("Car", null, 1, 1, date));
        assertThrows(NullPointerException.class, () -> new Trip("Car", "Driver A", 1, 1, null));
    }

    /** Verifies blank vehicle and driver names are rejected by the constructor. */
    @Test
    void constructorRejectsBlankText() {
        LocalDate date = LocalDate.parse("2026-09-28");

        assertThrows(IllegalArgumentException.class, () -> new Trip(" ", "Driver A", 1, 1, date));
        assertThrows(IllegalArgumentException.class, () -> new Trip("Car", "\t", 1, 1, date));
    }

    /** Verifies zero-valued trip metrics are accepted at the boundary. */
    @Test
    void constructorAcceptsZeroMetrics() {
        Trip trip = new Trip("Car", "Driver A", 0.0, 0.0, LocalDate.parse("2026-09-28"));

        assertEquals(0.0, trip.getKm());
        assertEquals(0.0, trip.getFuelLiters());
    }

    /** Verifies negative and non-finite trip metrics are rejected. */
    @Test
    void constructorRejectsNegativeAndNonFiniteMetrics() {
        LocalDate date = LocalDate.parse("2026-09-28");

        assertThrows(IllegalArgumentException.class, () -> new Trip("Car", "Driver A", -1, 1, date));
        assertThrows(IllegalArgumentException.class, () -> new Trip("Car", "Driver A", 1, -1, date));
        assertThrows(IllegalArgumentException.class,
                () -> new Trip("Car", "Driver A", Double.NaN, 1, date));
        assertThrows(IllegalArgumentException.class,
                () -> new Trip("Car", "Driver A", 1, Double.POSITIVE_INFINITY, date));
    }

    /** Verifies CSV fields are trimmed and converted to their domain types. */
    @Test
    void fromCsvCreatesTripFromValidRow() {
        Trip trip = Trip.fromCsv(" Toyota ; Driver A ; 100 ; 8 ; 2026-09-28 ");

        assertEquals("Toyota", trip.getVehicle());
        assertEquals("Driver A", trip.getDriver());
        assertEquals(100.0, trip.getKm());
        assertEquals(8.0, trip.getFuelLiters());
        assertEquals(LocalDate.parse("2026-09-28"), trip.getDate());
    }

    /** Verifies malformed CSV structure and numeric values are rejected. */
    @Test
    void fromCsvRejectsMalformedFields() {
        IllegalArgumentException structureError = assertThrows(
                IllegalArgumentException.class, () -> Trip.fromCsv("Car;Driver;10;2"));
        IllegalArgumentException extraFieldError = assertThrows(
                IllegalArgumentException.class, () -> Trip.fromCsv("Car;Driver;10;2;2026-09-28;extra"));
        IllegalArgumentException numberError = assertThrows(
                IllegalArgumentException.class, () -> Trip.fromCsv("Car;Driver;many;2;2026-09-28"));

        assertEquals("очікується 5 полів, знайдено 4", structureError.getMessage());
        assertEquals("очікується 5 полів, знайдено 6", extraFieldError.getMessage());
        assertEquals("числове поле має помилковий формат", numberError.getMessage());
    }

    /** Verifies CSV factory rejects records that violate entity invariants. */
    @Test
    void fromCsvRejectsInvariantViolations() {
        IllegalArgumentException blankNameError = assertThrows(
                IllegalArgumentException.class, () -> Trip.fromCsv(";Driver;10;2;2026-09-28"));
        IllegalArgumentException negativeMetricError = assertThrows(
                IllegalArgumentException.class, () -> Trip.fromCsv("Car;Driver;10;-2;2026-09-28"));

        assertEquals("порожня назва транспортного засобу або водія", blankNameError.getMessage());
        assertEquals("кілометраж і паливо мають бути скінченними невід'ємними числами",
                negativeMetricError.getMessage());
    }

    /** Verifies record equality and generated component accessors. */
    @Test
    void mileageFuelUsesValueEqualityAndExposesComponents() {
        MileageFuel totals = new MileageFuel(150.0, 15.0);

        assertEquals(new MileageFuel(150.0, 15.0), totals);
        assertNotEquals(new MileageFuel(150.0, 14.0), totals);
        assertEquals(150.0, totals.km());
        assertEquals(15.0, totals.fuelLiters());
    }

    /** Verifies record adds trip metrics without mutating its original value. */
    @Test
    void mileageFuelAccumulatesTripMetrics() {
        MileageFuel initial = new MileageFuel(100.0, 8.0);
        MileageFuel totals = initial.plus(
                new Trip("Van", "Driver B", 50.0, 7.0, LocalDate.parse("2026-09-27")));

        assertEquals(new MileageFuel(100.0, 8.0), initial);
        assertEquals(new MileageFuel(150.0, 15.0), totals);
    }

    /** Verifies the trip's diagnostic text uses locale-independent decimals. */
    @Test
    void toStringFormatsNumericFieldsWithRootLocale() {
        Trip trip = new Trip("Toyota", "Driver A", 100.0, 8.5, LocalDate.parse("2026-09-28"));

        assertEquals("Toyota: водій Driver A, 100.00 км, 8.50 л, дата 2026-09-28", trip.toString());
    }
}
