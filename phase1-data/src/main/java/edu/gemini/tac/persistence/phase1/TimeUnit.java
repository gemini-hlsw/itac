package edu.gemini.tac.persistence.phase1;

public enum TimeUnit {
    MIN("min"),
    HR("hr"),
    NIGHT("night");

    private final String value;

    TimeUnit(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static TimeUnit fromValue(String v) {
        for (TimeUnit c: TimeUnit.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }

        throw new IllegalArgumentException(v);
    }

    public String getDisplay() {
        return value();
    }

    public static double conversionFactor(TimeUnit from, TimeUnit to) {
        switch(from) {
        case MIN:
            switch(to) {
            case MIN:
                return 1.0d;
            case HR:
                return 1.0d / 60.0d;
            case NIGHT:
                return 1.0d / (60.0d * 10.0d);
            }
        case HR:
            switch(to) {
            case MIN:
                return 60.0d;
            case HR:
                return 1.0d;
            case NIGHT:
                return 1.0d / (10.0d);
            }
        case NIGHT:
            switch(to) {
            case MIN:
                return 60.0d * 10.0d * 1.0d;
            case HR:
                return 10.0d;
            case NIGHT:
                return 1.0d;
            }
        }

        throw new IllegalArgumentException();
    }
}

