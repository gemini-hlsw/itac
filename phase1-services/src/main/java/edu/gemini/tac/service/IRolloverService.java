package edu.gemini.tac.service;

import edu.gemini.tac.persistence.Site;
import edu.gemini.tac.persistence.rollover.IRolloverReport;
import edu.gemini.tac.persistence.rollover.RolloverReport;
import edu.gemini.tac.persistence.rollover.RolloverSet;

import java.rmi.RemoteException;
import java.util.Set;

/**
 * TODO: Refactor / Restructure from phase2-services
 *
 * Once again, I'm beginning just by cloning Shane's work since I don't want to restructure without his assistance...
 *
 * Author: lobrien
 * Date: 3/8/11
 */
public interface IRolloverService {

    /**
     * Retrieves the rollover information from the observing database
     * associated with the given site.
     *
     * @return RolloverReport describing the current unobserved rollover
     * observations
     *
     * @throws RemoteException if there is a problem finding or communicating
     * with the remote Observing Database
     */
    IRolloverReport report(Site site, IPartnerService partnerService) throws RemoteException;

    RolloverSet createRolloverSet(Site site, String setName, String[] observationIds, String[] timesAllocated);

    Set<RolloverSet> rolloverSetsFor(String siteName);

    RolloverSet getRolloverSet(Long rolloverSetId);

    //Converts a Scala report into a Java report.
    RolloverReport convert(IRolloverReport odbReport);
}
