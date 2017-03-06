package edu.gemini.tac.persistence.phase1;

import edu.gemini.tac.persistence.phase1.sitequality.CloudCover;
import edu.gemini.tac.persistence.phase1.sitequality.ImageQuality;
import edu.gemini.tac.persistence.phase1.sitequality.SkyBackground;

/**
 * A set of condition bins which can be used to group conditions and establishes an order from "best" to "worst" conditions.
 * The condition bins are used for reporting.
 */
public enum ConditionsBin {

    IQ20_CC50_SB50("IQ20%, CC50%, <=SB50%"),
    IQ20_CC50_SB80("IQ20%, CC50%, >=SB80%"),
    IQ20_CC70_SBANY("IQ20%, >=CC70%, --"),
    IQ70_CC50_SB50("IQ70%, CC50%, <=SB50%"),
    IQ70_CC50_SB80("IQ70%, CC50%, >=SB80%"),
    IQ70_CC70_SBANY("IQ70%, >=CC70%, --"),
    IQ85_CC50_SBANY("IQ85%, CC50%, --"),
    IQ85_CC70_SBANY("IQ85%, >=CC70%, --"),
    IQANY_CCANY_SBANY("IQAny, --, --");


    private String name;

    ConditionsBin(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public static ConditionsBin forCondition(Condition quality) {
        if (quality.getImageQuality().equals(ImageQuality.IQ_20)) {
            if (quality.getCloudCover().equals(CloudCover.CC_50)) {
                if (quality.getSkyBackground().getPercentage() <= SkyBackground.SB_50.getPercentage()) {
                    return IQ20_CC50_SB50;
                } else {
                    return IQ20_CC50_SB80;
                }
            } else {
                return IQ20_CC70_SBANY;
            }
        } else if (quality.getImageQuality().equals(ImageQuality.IQ_70)) {
            if (quality.getCloudCover().equals(CloudCover.CC_50)) {
                if (quality.getSkyBackground().getPercentage() <= SkyBackground.SB_50.getPercentage()) {
                    return IQ70_CC50_SB50;
                } else {
                    return IQ70_CC50_SB80;
                }
            } else {
                return IQ70_CC70_SBANY;
            }
        } else if (quality.getImageQuality().equals(ImageQuality.IQ_85)) {
            if (quality.getCloudCover().equals(CloudCover.CC_50)) {
                return IQ85_CC50_SBANY;
            } else {
                return IQ85_CC70_SBANY;
            }
        } else {
            return IQANY_CCANY_SBANY;
        }
    }
}
