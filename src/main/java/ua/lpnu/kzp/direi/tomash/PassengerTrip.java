package ua.lpnu.kzp.direi.tomash;

import java.time.LocalDate;

/**
 * Поїздка пасажирського транспортного засобу.
 *
 * <p>Для навчального розрахунку використовується норматив 8 літрів на
 * 100 кілометрів; він не змінює фактичні показники поїздки.</p>
 */
public final class PassengerTrip extends Trip {
    /** Нормативна витрата пасажирського транспорту на 100 км. */
    private static final double NORMATIVE_LITERS_PER_100_KM = 8.0;

    /**
     * Створює пасажирську поїздку зі спільними перевіреними показниками.
     *
     * @param vehicle назва транспортного засобу
     * @param driver ім'я водія
     * @param km кілометраж поїздки
     * @param fuelLiters фактично витрачене пальне в літрах
     * @param date дата поїздки
     */
    public PassengerTrip(String vehicle, String driver, double km, double fuelLiters, LocalDate date) {
        super(Trip.validatedState(vehicle, driver, km, fuelLiters, date));
    }

    /**
     * Повертає категорію пасажирської поїздки.
     *
     * @return {@link TripKind#PASSENGER}
     */
    @Override
    public TripKind getKind() {
        return TripKind.PASSENGER;
    }

    /**
     * Обчислює нормативну кількість пального для пасажирської поїздки.
     *
     * @return 8 літрів на кожні 100 кілометрів поїздки
     */
    @Override
    public double calculateNormativeFuelLiters() {
        return getKm() * NORMATIVE_LITERS_PER_100_KM / 100.0;
    }
}
