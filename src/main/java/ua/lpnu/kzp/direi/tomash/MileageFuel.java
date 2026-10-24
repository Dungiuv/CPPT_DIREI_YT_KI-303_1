package ua.lpnu.kzp.direi.tomash;

import java.util.Objects;

/**
 * Зберігає незмінні підсумкові значення кілометражу та витрати пального.
 *
 * @param km сумарний кілометраж
 * @param fuelLiters сумарна кількість витраченого пального
 */
public record MileageFuel(double km, double fuelLiters) {
    /**
     * Повертає нову пару підсумків, доповнену показниками поїздки.
     *
     * @param trip поїздка для додавання до підсумків
     * @return нові підсумкові значення
     * @throws NullPointerException якщо поїздка дорівнює {@code null}
     */
    public MileageFuel plus(Trip trip) {
        Objects.requireNonNull(trip, "Поїздка не може бути null");
        return new MileageFuel(km + trip.getKm(), fuelLiters + trip.getFuelLiters());
    }
}
