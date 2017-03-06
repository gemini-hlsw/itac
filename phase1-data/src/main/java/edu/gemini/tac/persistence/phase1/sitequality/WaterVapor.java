package edu.gemini.tac.persistence.phase1.sitequality;


import org.apache.commons.lang.Validate;

public enum WaterVapor {

    WV_20("20%/Low","wv20", "<=20%", 20),
    WV_50("50%/Median","wv50", "<=50%", 50),
    WV_80("80%/High","wv80", "<=80%", 80),
    WV_100("Any","wv100", "Any", 100);

    private String key;
    private byte percentage;
    private String displayName;
    private String abbreviation;

    public static final String qualityDisplayName = "Water Vapor";

    WaterVapor() {}

    WaterVapor(final String key, final String abbreviation, final String displayName, final int percentage) {
        Validate.isTrue(percentage >= 0 && percentage <= 100, SiteQuality.PERCENTAGE_CONSTRAINT_MESSAGE);
		this.displayName = displayName;
        this.percentage = (byte) percentage; // Java and it's numeric integer constants.
        this.key = key;
        this.abbreviation = abbreviation;
    }

    public String value() {
        return key;
    }

    public static WaterVapor fromValue(String v) {
        for (WaterVapor c: WaterVapor.values()) {
            if (c.key.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

    public static WaterVapor fromValue(byte percentage){
        for(WaterVapor c : WaterVapor.values()){
            if(c.getPercentage() == percentage){
                return c;
            }
        }
        throw new IllegalArgumentException(""+percentage);
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

    public WaterVapor[] getValues() {
        return values();
    }

    public String getAbbreviation() {
        return abbreviation;
    }

    public String getQualityDisplayName() {
        return qualityDisplayName;
    }
}
