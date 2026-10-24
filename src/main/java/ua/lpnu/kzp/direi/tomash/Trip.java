package ua.lpnu.kzp.direi.tomash;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.Objects;

/**
 * Описує незмінний запис про поїздку автопарку.
 *
 * <p>Коректна поїздка має назву транспортного засобу й водія, дату,
 * а також скінченні невід'ємні значення кілометражу та витрати пального.</p>
 */
public final class Trip {
    /** Назва транспортного засобу. */
    private final String vehicle;

    /** Ім'я водія. */
    private final String driver;

    /** Кілометраж поїздки. */
    private final double km;

    /** Кількість витраченого пального в літрах. */
    private final double fuelLiters;

    /** Дата поїздки. */
    private final LocalDate date;

    /**
     * Створює запис про поїздку після перевірки всіх його полів.
     *
     * @param vehicle назва транспортного засобу
     * @param driver ім'я водія
     * @param km кілометраж поїздки
     * @param fuelLiters кількість витраченого пального в літрах
     * @param date дата поїздки
     * @throws NullPointerException якщо назва, ім'я водія або дата дорівнюють {@code null}
     * @throws IllegalArgumentException якщо текстове поле порожнє, або числове
     *         значення від'ємне чи нескінченне
     */
    public Trip(String vehicle, String driver, double km, double fuelLiters, LocalDate date) {
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

        this.vehicle = vehicle;
        this.driver = driver;
        this.km = km;
        this.fuelLiters = fuelLiters;
        this.date = date;
    }

    /**
     * Створює поїздку з CSV-рядка формату
     * {@code vehicle;driver;km;fuelLiters;date}.
     *
     * @param line рядок із даними поїздки
     * @return створена поїздка
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

        return new Trip(fields[0], fields[1], km, fuelLiters, date);
    }

    /**
     * Повертає назву транспортного засобу.
     *
     * @return назва транспортного засобу
     */
    public String getVehicle() {
        return vehicle;
    }

    /**
     * Повертає ім'я водія.
     *
     * @return ім'я водія
     */
    public String getDriver() {
        return driver;
    }

    /**
     * Повертає кілометраж поїздки.
     *
     * @return кілометраж у кілометрах
     */
    public double getKm() {
        return km;
    }

    /**
     * Повертає кількість витраченого пального.
     *
     * @return витрачене пальне в літрах
     */
    public double getFuelLiters() {
        return fuelLiters;
    }

    /**
     * Повертає дату поїздки.
     *
     * @return дата поїздки
     */
    public LocalDate getDate() {
        return date;
    }

    /**
     * Повертає форматоване текстове подання поїздки.
     *
     * @return транспортний засіб, водій, показники та дата поїздки
     */
    @Override
    public String toString() {
        return String.format(Locale.ROOT, "%s: водій %s, %.2f км, %.2f л, дата %s",
                vehicle, driver, km, fuelLiters, date);
    }
}
