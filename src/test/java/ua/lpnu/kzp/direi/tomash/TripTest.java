package ua.lpnu.kzp.direi.tomash;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Verifies CSV conversion and value behavior for trip domain objects. */
class TripTest {
    /** Verifies a valid constructor call retains each supplied component. */
    @Test
    void constructorStoresValidValues() {
        LocalDate date = LocalDate.parse("2026-09-28");
        Trip trip = new PassengerTrip("Toyota", "Driver A", 100.5, 8.25, date);

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

        assertThrows(NullPointerException.class,
                () -> new PassengerTrip(null, "Driver A", 1, 1, date));
        assertThrows(NullPointerException.class,
                () -> new PassengerTrip("Car", null, 1, 1, date));
        assertThrows(NullPointerException.class,
                () -> new PassengerTrip("Car", "Driver A", 1, 1, null));
    }

    /** Verifies blank vehicle and driver names are rejected by the constructor. */
    @Test
    void constructorRejectsBlankText() {
        LocalDate date = LocalDate.parse("2026-09-28");

        assertThrows(IllegalArgumentException.class,
                () -> new PassengerTrip(" ", "Driver A", 1, 1, date));
        assertThrows(IllegalArgumentException.class,
                () -> new PassengerTrip("Car", "\t", 1, 1, date));
        assertThrows(IllegalArgumentException.class,
                () -> new CargoTrip(" ", "Driver A", 1, 1, date));
    }

    /** Verifies zero-valued trip metrics are accepted at the boundary. */
    @Test
    void constructorAcceptsZeroMetrics() {
        Trip trip = new PassengerTrip("Car", "Driver A", 0.0, 0.0, LocalDate.parse("2026-09-28"));

        assertEquals(0.0, trip.getKm());
        assertEquals(0.0, trip.getFuelLiters());
    }

    /** Verifies negative and non-finite trip metrics are rejected. */
    @Test
    void constructorRejectsNegativeAndNonFiniteMetrics() {
        LocalDate date = LocalDate.parse("2026-09-28");

        assertThrows(IllegalArgumentException.class,
                () -> new PassengerTrip("Car", "Driver A", -1, 1, date));
        assertThrows(IllegalArgumentException.class,
                () -> new PassengerTrip("Car", "Driver A", 1, -1, date));
        assertThrows(IllegalArgumentException.class,
                () -> new PassengerTrip("Car", "Driver A", Double.NaN, 1, date));
        assertThrows(IllegalArgumentException.class,
                () -> new PassengerTrip("Car", "Driver A", 1, Double.POSITIVE_INFINITY, date));
    }

    /** Verifies CSV fields are trimmed and converted to their domain types. */
    @Test
    void fromCsvCreatesTripFromValidRow() {
        Trip trip = Trip.fromCsv(" Toyota ; Driver A ; 100 ; 8 ; 2026-09-28 ");

        assertEquals(PassengerTrip.class, trip.getClass());
        assertEquals("Toyota", trip.getVehicle());
        assertEquals("Driver A", trip.getDriver());
        assertEquals(100.0, trip.getKm());
        assertEquals(8.0, trip.getFuelLiters());
        assertEquals(LocalDate.parse("2026-09-28"), trip.getDate());
    }

    /** Verifies vehicle markers select the cargo subtype without changing the CSV schema. */
    @Test
    void fromCsvSelectsCargoSubtypeFromVehicleName() {
        for (String marker : List.of("Truck", "MAN", "Volvo", "DAF", "Scania", "Вантаж")) {
            Trip trip = Trip.fromCsv(
                    "%s;Driver A;100;8;2026-09-28".formatted(marker.toLowerCase(Locale.ROOT)));

            assertEquals(CargoTrip.class, trip.getClass(), marker);
            assertEquals(TripKind.CARGO, trip.getKind());
        }
        Trip passenger = Trip.fromCsv("Honda;Driver A;100;8;2026-09-28");
        assertEquals(PassengerTrip.class, passenger.getClass());
        assertEquals(TripKind.PASSENGER, passenger.getKind());
    }

    /** Verifies enum categories have stable labels for display. */
    @Test
    void tripKindExposesCategoryLabels() {
        assertEquals("пасажирська", TripKind.PASSENGER.label());
        assertEquals("вантажна", TripKind.CARGO.label());
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
                new PassengerTrip("Van", "Driver B", 50.0, 7.0, LocalDate.parse("2026-09-27")));

        assertEquals(new MileageFuel(100.0, 8.0), initial);
        assertEquals(new MileageFuel(150.0, 15.0), totals);
    }

    /** Verifies the trip's diagnostic text uses locale-independent decimals. */
    @Test
    void toStringFormatsNumericFieldsWithRootLocale() {
        Trip trip = new PassengerTrip(
                "Toyota", "Driver A", 100.0, 8.5, LocalDate.parse("2026-09-28"));

        assertEquals("Toyota: водій Driver A, 100.00 км, 8.50 л, дата 2026-09-28", trip.toString());
    }

    /** Verifies the subtypes implement different normative fuel calculations polymorphically. */
    @Test
    void subtypesCalculateDifferentNormativeFuelAmounts() {
        Trip passenger = new PassengerTrip(
                "Toyota", "Driver A", 200.0, 18.0, LocalDate.parse("2026-09-28"));
        Trip cargo = new CargoTrip(
                "MAN", "Driver B", 200.0, 45.0, LocalDate.parse("2026-09-28"));

        assertEquals(16.0, passenger.calculateNormativeFuelLiters());
        assertEquals(50.0, cargo.calculateNormativeFuelLiters());
        assertEquals(TripKind.PASSENGER, passenger.getKind());
        assertEquals(TripKind.CARGO, cargo.getKind());
        assertEquals(18.0, passenger.getFuelLiters());
        assertEquals(45.0, cargo.getFuelLiters());
    }

    /** Verifies one base-type collection can process heterogeneous trips. */
    @Test
    void mixedTripsUseOverriddenBehaviorThroughBaseType() {
        List<Trip> trips = List.of(
                new PassengerTrip("Toyota", "Driver A", 100.0, 9.0,
                        LocalDate.parse("2026-09-28")),
                new CargoTrip("MAN", "Driver B", 100.0, 24.0,
                        LocalDate.parse("2026-09-28")));

        assertEquals(List.of(8.0, 25.0),
                trips.stream().map(Trip::calculateNormativeFuelLiters).toList());
        assertEquals(List.of(TripKind.PASSENGER, TripKind.CARGO),
                trips.stream().map(Trip::getKind).toList());
    }

    /** Verifies equal trips have matching hashes and produce one set entry. */
    @Test
    void equalTripsHaveSameHashAndOneHashSetEntry() {
        Trip first = new PassengerTrip(
                "Toyota", "Driver A", 100.0, 8.0, LocalDate.parse("2026-09-28"));
        Trip second = new PassengerTrip(
                "Toyota", "Driver A", 100.0, 8.0, LocalDate.parse("2026-09-28"));
        Set<Trip> trips = new HashSet<>();

        assertEquals(first, second);
        assertEquals(first.hashCode(), second.hashCode());
        trips.add(first);
        trips.add(second);
        assertEquals(1, trips.size());
    }

    /** Verifies equality includes every common field and the concrete subtype. */
    @Test
    void tripEqualityIncludesAllFieldsAndConcreteType() {
        Trip original = new PassengerTrip(
                "Toyota", "Driver A", 100.0, 8.0, LocalDate.parse("2026-09-28"));
        List<Trip> differentTrips = List.of(
                new PassengerTrip("Honda", "Driver A", 100.0, 8.0, LocalDate.parse("2026-09-28")),
                new PassengerTrip("Toyota", "Driver B", 100.0, 8.0, LocalDate.parse("2026-09-28")),
                new PassengerTrip("Toyota", "Driver A", 101.0, 8.0, LocalDate.parse("2026-09-28")),
                new PassengerTrip("Toyota", "Driver A", 100.0, 9.0, LocalDate.parse("2026-09-28")),
                new PassengerTrip("Toyota", "Driver A", 100.0, 8.0, LocalDate.parse("2026-09-29")),
                new CargoTrip("Toyota", "Driver A", 100.0, 8.0, LocalDate.parse("2026-09-28")));

        for (Trip differentTrip : differentTrips) {
            assertNotEquals(original, differentTrip);
        }
        assertNotEquals(original, null);
        assertNotEquals(original, "not a trip");
    }
}
