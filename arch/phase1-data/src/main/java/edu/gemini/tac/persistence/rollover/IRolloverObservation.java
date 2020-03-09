package edu.gemini.tac.persistence.rollover;

import edu.gemini.tac.persistence.Partner;
import edu.gemini.tac.persistence.phase1.Condition;
import edu.gemini.tac.persistence.phase1.Target;
import edu.gemini.tac.persistence.phase1.TimeAmount;

/**
 * A RolloverObservation identifies an existing phase 2 pending observation
 * that will be available for execution during the semester.
 *
 * Author: lobrien
 * Date: 3/8/11
 */
public interface IRolloverObservation {
    Partner partner();
    String observationId();
    Target target();
    Condition siteQuality(); // Alias for condition
    Condition condition();
    TimeAmount observationTime();
}
