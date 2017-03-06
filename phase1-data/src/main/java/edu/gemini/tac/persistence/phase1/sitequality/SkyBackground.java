package edu.gemini.tac.persistence.phase1.sitequality;


import org.apache.commons.lang.Validate;

public enum SkyBackground {

    SB_20("20%/Darkest","sb20", "<=20%", 20),
    SB_50("50%/Dark","sb50", "<=50%", 50),
    SB_80("80%/Grey","sb80", "<=80%", 80),
    SB_100("Any/Bright","sb100", "Any", 100);

    private String key;
    private byte percentage;
    private String displayName;
    private String abbreviation;

    public static final String qualityDisplayName = "Sky Background";

    SkyBackground() {}

    SkyBackground(final String key, final String abbreviation, final String displayName, final int percentage) {
        Validate.isTrue(percentage >= 0 && percentage <= 100, SiteQuality.PERCENTAGE_CONSTRAINT_MESSAGE);
		this.displayName = displayName;
        this.percentage = (byte) percentage; // Java and it's numeric integer constants.
        this.key = key;
        this.abbreviation = abbreviation;
    }

    public String value() {
        return key;
    }

    public static SkyBackground fromValue(String v) {
        for (SkyBackground c: SkyBackground.values()) {
            if (c.key.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

    public static SkyBackground fromValue(byte v){
        for(SkyBackground c : SkyBackground.values()){
            if(c.getPercentage() == v){
                return c;
            }
        }
        throw new IllegalArgumentException("" + v);
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

    public SkyBackground[] getValues() {
        return values();
    }

    public String getAbbreviation() {
        return abbreviation;
    }

    public String getQualityDisplayName() {
        return qualityDisplayName;
    }
}
