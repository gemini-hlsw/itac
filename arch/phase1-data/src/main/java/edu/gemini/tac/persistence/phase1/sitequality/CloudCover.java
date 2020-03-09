package edu.gemini.tac.persistence.phase1.sitequality;

import org.apache.commons.lang.Validate;

public enum CloudCover {
    CC_50("50%/Clear","cc50","50%", 50),
    CC_70("70%/Cirrus","cc70","70%", 70),
    CC_80("80%/Cloudy","cc80","80%", 80),
    CC_100("Any","cc100", "-", 100);

    private String key;
    private String displayName;
    private byte percentage;
    private String abbreviation;

    public static final String qualityDisplayName = "Cloud Cover";

    CloudCover() {}

    CloudCover(String key, final String abbreviation, final String displayName, final int percentage) {
        Validate.isTrue(percentage >= 0 && percentage <= 100, SiteQuality.PERCENTAGE_CONSTRAINT_MESSAGE);
		this.displayName = displayName;
        this.percentage = (byte) percentage; // Java and it's numeric integer constants.
        this.key = key;
        this.abbreviation = abbreviation;
    }

    public String value() {
        return key;
    }

    public static CloudCover fromValue(String v) {
        for (CloudCover c: CloudCover.values()) {
            if (c.key.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

    public static CloudCover fromValue(byte percentage) {
        for (CloudCover c: CloudCover.values()) {
            if (c.getPercentage() == percentage) {
                return c;
            }
        }
        throw new IllegalArgumentException("" + percentage);
    }

    public String getKey() {
        return key;
    }

    public String getDisplayName() {
        return displayName;
    }

    public byte getPercentage() {
        return percentage;
    }

    public CloudCover[] getValues() {
        return values();
    }

    public String getAbbreviation() {
        return abbreviation;
    }

    public String getQualityDisplayName() {
        return qualityDisplayName;
    }
}
