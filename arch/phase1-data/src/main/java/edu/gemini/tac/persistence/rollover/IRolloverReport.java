package edu.gemini.tac.persistence.rollover;

import edu.gemini.tac.persistence.Site;

import java.util.Set;

/**
 * TODO: Refactor / restructure system so that this interface is used by phase2-services
 *
 * A clone of phase2-services edu.gemini.tac.rollover.api.RolloverReport
 *
 * Cloned because we do not want to make phase1-data dependent on phase2-services, but I do not want to refactor phase2-services w/o consultation with Shane.
 *
 * So this is just a temporary holder (but I have to make it public since it's an interface)
 *
 * Author: lobrien
 * Date: 3/8/11
 */

public interface IRolloverReport {
    Site site();
    Set<IRolloverObservation> observations();

}
