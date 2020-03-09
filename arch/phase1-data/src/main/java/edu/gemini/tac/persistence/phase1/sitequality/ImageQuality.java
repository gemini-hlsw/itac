package edu.gemini.tac.persistence.phase1.sitequality;


import org.apache.commons.lang.Validate;

public enum ImageQuality {

    IQ_20("20%/Best","iq20", "20%/Best", 20),
    IQ_70("70%/Good","iq70", "70%/Good", 70),
    IQ_85("85%/Poor","iq85", "85%/Poor", 85),
    IQ_100("Any","iq100", "Any", 100);

    private String key;
    private byte percentage;
    private String displayName;
    private String abbreviation;

    public static final String qualityDisplayName = "Image Quality";

    ImageQuality() {}

    ImageQuality(String key, final String abbreviation, final String displayName, final int percentage) {
        Validate.isTrue(percentage >= 0 && percentage <= 100, SiteQuality.PERCENTAGE_CONSTRAINT_MESSAGE);
		this.displayName = displayName;
        this.percentage = (byte) percentage; // Java and it's numeric integer constants.
        this.key = key;
        this.abbreviation = abbreviation;
    }

    public String value() {
        return key;
    }

    public static ImageQuality fromValue(String v) {
        for (ImageQuality c: ImageQuality.values()) {
            if (c.key.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

    public static ImageQuality fromValue(byte v){
        for(ImageQuality c : ImageQuality.values()){
            if(c.getPercentage() == v){
                return c;
            }
        }
        throw new IllegalArgumentException(""+v);
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

    public ImageQuality[] getValues() {
        return values();
    }

    public String getAbbreviation() {
        return abbreviation;
    }

    public String getQualityDisplayName() {
        return qualityDisplayName;
    }
}
