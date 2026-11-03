package ua.lpnu.kzp.direi.tomash;

import java.time.LocalDate;

/**
 * Поїздка вантажного транспортного засобу.
 *
 * <p>Для навчального розрахунку використовується норматив 25 літрів на
 * 100 кілометрів; він не змінює фактичні показники поїздки.</p>
 */
public final class CargoTrip extends Trip {
    /** Нормативна витрата вантажного транспорту на 100 км. */
    private static final double NORMATIVE_LITERS_PER_100_KM = 25.0;

    /**
     * Створює вантажну поїздку зі спільними перевіреними показниками.
     *
     * @param vehicle назва транспортного засобу
     * @param driver ім'я водія
     * @param km кілометраж поїздки
     * @param fuelLiters фактично витрачене пальне в літрах
     * @param date дата поїздки
     */
    public CargoTrip(String vehicle, String driver, double km, double fuelLiters, LocalDate date) {
        super(Trip.validatedState(vehicle, driver, km, fuelLiters, date));
    }

    /**
     * Повертає категорію вантажної поїздки.
     *
     * @return {@link TripKind#CARGO}
     */
    @Override
    public TripKind getKind() {
        return TripKind.CARGO;
    }

    /**
     * Обчислює нормативну кількість пального для вантажної поїздки.
     *
     * @return 25 літрів на кожні 100 кілометрів поїздки
     */
    @Override
    public double calculateNormativeFuelLiters() {
        return getKm() * NORMATIVE_LITERS_PER_100_KM / 100.0;
    }
}
