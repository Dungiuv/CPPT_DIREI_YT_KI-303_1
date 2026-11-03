package ua.lpnu.kzp.direi.tomash;

/**
 * Фіксовані категорії поїздок автопарку.
 */
public enum TripKind {
    /** Поїздка пасажирського транспортного засобу. */
    PASSENGER("пасажирська"),

    /** Поїздка вантажного транспортного засобу. */
    CARGO("вантажна");

    /** Людинозрозумілий підпис категорії. */
    private final String label;

    /**
     * Створює категорію з підписом для відображення.
     *
     * @param label текстовий підпис категорії
     */
    TripKind(String label) {
        this.label = label;
    }

    /**
     * Повертає текстовий підпис категорії.
     *
     * @return підпис категорії
     */
    public String label() {
        return label;
    }
}
