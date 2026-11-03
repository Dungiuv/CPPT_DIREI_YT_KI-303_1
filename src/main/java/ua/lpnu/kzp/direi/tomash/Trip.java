package ua.lpnu.kzp.direi.tomash;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Спільний незмінний тип поїздок автопарку.
 *
 * <p>Усі поїздки мають транспортний засіб, водія, кілометраж, фактичну
 * витрату пального та дату. Базовий клас перевіряє спільні інваріанти,
 * а підтип визначає нормативну кількість пального для кілометражу поїздки.</p>
 */
public abstract class Trip {
    /** Маркери транспортних засобів, що визначають вантажну поїздку. */
    private static final List<String> CARGO_MARKERS =
            List.of("truck", "man", "volvo", "daf", "scania", "вантаж");

    /** Назва транспортного засобу. */
    private final String vehicle;

    /** Ім'я водія. */
    private final String driver;

    /** Кілометраж поїздки. */
    private final double km;

    /** Кількість фактично витраченого пального в літрах. */
    private final double fuelLiters;

    /** Дата поїздки. */
    private final LocalDate date;

    /**
     * Ініціалізує спільний стан із попередньо перевірених значень.
     *
     * @param state перевірені значення полів поїздки
     */
    Trip(ValidatedState state) {
        this.vehicle = state.vehicle;
        this.driver = state.driver;
        this.km = state.km;
        this.fuelLiters = state.fuelLiters;
        this.date = state.date;
    }

    /**
     * Перевіряє та готує спільний стан до виклику конструктора базового класу.
     * Підтипи викликають цей метод в аргументі {@code super(...)} до ініціалізації
     * власного стану.
     *
     * @param vehicle назва транспортного засобу
     * @param driver ім'я водія
     * @param km кілометраж поїздки
     * @param fuelLiters фактично витрачене пальне в літрах
     * @param date дата поїздки
     * @return перевірений стан для створення підтипу
     * @throws NullPointerException якщо текстове поле або дата дорівнює {@code null}
     * @throws IllegalArgumentException якщо текстове поле порожнє або показник
     *         є від'ємним чи нескінченним
     */
    static ValidatedState validatedState(
            String vehicle, String driver, double km, double fuelLiters, LocalDate date) {
        Objects.requireNonNull(vehicle, "Назва транспортного засобу не може бути null");
        Objects.requireNonNull(driver, "Ім'я водія не може бути null");
        Objects.requireNonNull(date, "Дата поїздки не може бути null");

        if (vehicle.isBlank() || driver.isBlank()) {
            throw new IllegalArgumentException("порожня назва транспортного засобу або водія");
        }
        if (!Double.isFinite(km) || km < 0 || !Double.isFinite(fuelLiters) || fuelLiters < 0) {
            throw new IllegalArgumentException(
                    "кілометраж і паливо мають бути скінченними невід'ємними числами");
        }

        return new ValidatedState(vehicle, driver, km, fuelLiters, date);
    }

    /** Перевірений спільний стан, доступний лише всередині пакета моделі. */
    static final class ValidatedState {
        /** Назва транспортного засобу. */
        private final String vehicle;

        /** Ім'я водія. */
        private final String driver;

        /** Кілометраж поїздки. */
        private final double km;

        /** Фактично витрачене пальне в літрах. */
        private final double fuelLiters;

        /** Дата поїздки. */
        private final LocalDate date;

        /**
         * Зберігає вже перевірені спільні значення поїздки.
         *
         * @param vehicle назва транспортного засобу
         * @param driver ім'я водія
         * @param km кілометраж
         * @param fuelLiters фактично витрачене пальне
         * @param date дата поїздки
         */
        private ValidatedState(String vehicle, String driver, double km, double fuelLiters, LocalDate date) {
            this.vehicle = vehicle;
            this.driver = driver;
            this.km = km;
            this.fuelLiters = fuelLiters;
            this.date = date;
        }
    }

    /**
     * Створює підтип поїздки з CSV-рядка формату
     * {@code vehicle;driver;km;fuelLiters;date}.
     *
     * <p>Транспортні засоби, назва яких містить {@code Truck}, {@code MAN},
     * {@code Volvo}, {@code DAF}, {@code Scania} або {@code Вантаж}
     * (без урахування регістру), створюються як вантажні. Інші записи
     * створюються як пасажирські.</p>
     *
     * @param line рядок із даними поїздки
     * @return створена пасажирська або вантажна поїздка
     * @throws NullPointerException якщо рядок дорівнює {@code null}
     * @throws IllegalArgumentException якщо кількість або значення полів некоректні
     */
    public static Trip fromCsv(String line) {
        Objects.requireNonNull(line, "Рядок CSV не може бути null");
        String[] fields = line.split(";", -1);
        if (fields.length != 5) {
            throw new IllegalArgumentException(
                    "очікується 5 полів, знайдено %d".formatted(fields.length));
        }

        for (int index = 0; index < fields.length; index++) {
            fields[index] = fields[index].trim();
        }
        if (fields[0].isBlank() || fields[1].isBlank()) {
            throw new IllegalArgumentException("порожня назва транспортного засобу або водія");
        }

        LocalDate date;
        try {
            date = LocalDate.parse(fields[4]);
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException(
                    "дата має бути у форматі YYYY-MM-DD і бути коректною", exception);
        }

        double km;
        double fuelLiters;
        try {
            km = Double.parseDouble(fields[2]);
            fuelLiters = Double.parseDouble(fields[3]);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("числове поле має помилковий формат", exception);
        }

        if (isCargoVehicle(fields[0])) {
            return new CargoTrip(fields[0], fields[1], km, fuelLiters, date);
        }
        return new PassengerTrip(fields[0], fields[1], km, fuelLiters, date);
    }

    /**
     * Повертає назву транспортного засобу.
     *
     * @return назва транспортного засобу
     */
    public final String getVehicle() {
        return vehicle;
    }

    /**
     * Повертає ім'я водія.
     *
     * @return ім'я водія
     */
    public final String getDriver() {
        return driver;
    }

    /**
     * Повертає кілометраж поїздки.
     *
     * @return кілометраж у кілометрах
     */
    public final double getKm() {
        return km;
    }

    /**
     * Повертає фактичну кількість витраченого пального.
     *
     * @return фактична витрата пального в літрах
     */
    public final double getFuelLiters() {
        return fuelLiters;
    }

    /**
     * Повертає дату поїздки.
     *
     * @return дата поїздки
     */
    public final LocalDate getDate() {
        return date;
    }

    /**
     * Повертає категорію поїздки.
     *
     * @return категорія, визначена фактичним підтипом
     */
    public abstract TripKind getKind();

    /**
     * Обчислює нормативну кількість пального для кілометражу поїздки.
     *
     * @return нормативна кількість пального в літрах
     */
    public abstract double calculateNormativeFuelLiters();

    /**
     * Порівнює поїздки за конкретним підтипом і всіма спільними полями.
     *
     * @param other об'єкт для порівняння
     * @return {@code true}, якщо тип і всі поля поїздок збігаються
     */
    @Override
    public final boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || getClass() != other.getClass()) {
            return false;
        }

        Trip trip = (Trip) other;
        return Double.compare(km, trip.km) == 0
                && Double.compare(fuelLiters, trip.fuelLiters) == 0
                && vehicle.equals(trip.vehicle)
                && driver.equals(trip.driver)
                && date.equals(trip.date);
    }

    /**
     * Повертає хеш-код, узгоджений із рівністю за типом і всіма полями.
     *
     * @return хеш-код поїздки
     */
    @Override
    public final int hashCode() {
        return Objects.hash(getClass(), vehicle, driver, km, fuelLiters, date);
    }

    /**
     * Повертає форматоване текстове подання поїздки.
     *
     * @return транспортний засіб, водій, фактичні показники та дата
     */
    @Override
    public final String toString() {
        return String.format(Locale.ROOT, "%s: водій %s, %.2f км, %.2f л, дата %s",
                vehicle, driver, km, fuelLiters, date);
    }

    /**
     * Перевіряє, чи містить назва транспортного засобу один із вантажних маркерів.
     *
     * @param vehicle назва транспортного засобу
     * @return {@code true}, якщо назва відповідає правилу вантажної поїздки
     */
    private static boolean isCargoVehicle(String vehicle) {
        String normalizedVehicle = vehicle.toLowerCase(Locale.ROOT);
        return CARGO_MARKERS.stream().anyMatch(normalizedVehicle::contains);
    }
}
