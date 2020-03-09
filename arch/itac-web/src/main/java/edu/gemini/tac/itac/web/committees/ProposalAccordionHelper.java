package edu.gemini.tac.itac.web.committees;

import edu.gemini.model.p1.mutable.ExchangePartner;
import edu.gemini.tac.persistence.Committee;
import edu.gemini.tac.persistence.Proposal;
import edu.gemini.tac.persistence.ProposalIssue;
import edu.gemini.tac.persistence.joints.JointProposal;
import edu.gemini.tac.service.ICommitteeService;
import edu.gemini.tac.service.IProposalService;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.springframework.web.servlet.ModelAndView;

import java.util.*;

/**
 * Proposal views with accordion rely on a standard set of model keys.  As well, the class string necessary for filtering
 * comes with a set of assumptions that this contains.  It is also possible to validate via this utility class.  Validation
 * consists of checking issues and submissions.
 */
public class ProposalAccordionHelper {
    private static final Logger LOGGER = Logger.getLogger(ProposalAccordionHelper.class);

    protected ProposalAccordionHelper() {}

    /**
     * Processes a fully-populated list of proposals and injects mappings from proposal id to a variety of information that
     * will be useful to the view.  Does not contain services for fetching unavailable and will not create additional trips
     * to the database (unless it's used in the context of a session, not current usage.)
     *
     * @param modelAndView model will have committee proposals, and mappings from proposal id to class string, instruments, and ranks
     * @param committeeProposals *** MUST BE FULLY POPULATED OR ELSE LIES WILL ENSUE. ***
     */
    static void populateModelForProposalAccordionNoServiceNoValidation(final ModelAndView modelAndView, final List<Proposal> committeeProposals) {
        modelAndView.addObject("committeeProposals", committeeProposals);
        modelAndView.addObject("instruments_by_proposal_id", getInstrumentsByProposal(null, committeeProposals));
        modelAndView.addObject("class_string_by_proposal_id", getClassStringByProposal(committeeProposals));
        modelAndView.addObject("sortable_rank_by_proposal_id", getSortableRankByProposal(committeeProposals));
    }

    /**
     * Processes a populated list of proposals, validates that they are complete to some degree and injects mappings
     * from proposal id to a variety of information that will be useful to the view.
     *
     * @param proposalService that will be used to retrieve instrument names
     * @param modelAndView model will have committee proposals, and mappings from proposal id to class string, instruments, and ranks
     * @param committeeProposals *** MUST BE FULLY POPULATED OR ELSE LIES WILL ENSUE. ***
     */
    static void populateModelForProposalAccordion(final IProposalService proposalService,
                                                  final ModelAndView modelAndView,
                                                  final List<Proposal> committeeProposals) {
        validateNoLIE(committeeProposals);
        modelAndView.addObject("committeeProposals", committeeProposals);
        modelAndView.addObject("instruments_by_proposal_id", getInstrumentsByProposal(proposalService, committeeProposals));
        modelAndView.addObject("class_string_by_proposal_id", getClassStringByProposal(committeeProposals));
        modelAndView.addObject("sortable_rank_by_proposal_id", getSortableRankByProposal(committeeProposals));
        validateNoLIE(committeeProposals);
    }

    static private void validateNoLIE(Collection<Proposal> ps){
        for(Proposal p : ps){
            validateNoLIE(p);
        }
    }

    static private void validateNoLIE(Proposal p){
        try{
            p.getPhaseIProposal().getSubmissions().toString();
            for(ProposalIssue i : p.getIssues()){
                i.toString();
            }
        }catch(Exception x){
            LOGGER.error("LIE with proposal " + p);
        }
    }


    static private Map<Long, String> getSortableRankByProposal(List<Proposal> proposals) {
        Map<Long, String> rankByProposalId = new HashMap<Long, String>();
        for (Proposal p : proposals) {
            final Long id = p.getId();
            try {
                double rank;
                String partnerAbbreviation = p.getPartnerAbbreviation();
                if (p.isJoint()) {
                    JointProposal jp = (JointProposal) p;
                    rank = Double.parseDouble(jp.getPrimaryProposal().getPartnerRanking());
                    partnerAbbreviation = jp.getPrimaryProposal().getPartnerAbbreviation();
                } else {
                    rank = Double.parseDouble(p.getPartnerRanking());
                }
                final String paddedRankString = String.format("%s %06.2f", partnerAbbreviation, rank);
                rankByProposalId.put(id, paddedRankString);
            } catch (NumberFormatException pe) {
                LOGGER.log(Level.WARN, "Could not parse ranking for proposal[" + p.getEntityId() + "] '" + p.getPartnerRanking() + "'");
                rankByProposalId.put(id, p.getPartner().getAbbreviation() + " UNRANKED");
            }
        }

        return rankByProposalId;
    }

    static private Map<Long, String> getClassStringByProposal(final List<Proposal> proposals) {
        final Map<Long, String> classStringByProposal = new HashMap<Long, String>();

        for (Proposal p : proposals) {
            classStringByProposal.put(p.getId(), p.getClassString());
        }

        return classStringByProposal;
    }

    /**
     * Retrieve the display names for all instruments in the list of proposals.
     *
     * @param proposalService Can be null in which case, data must be present.
     * @param proposals List of proposals that contain the data needed.
     * @return proposalId -> instrumentDisplayNames, e.g. 3 -> ["GMOS North", "GMOS South"]
     */
    static private Map<Long, Set<String>> getInstrumentsByProposal(final IProposalService proposalService, final List<Proposal> proposals) {
        Map<Long, Set<String>> ibp = new HashMap<Long, Set<String>>();
        for (Proposal p : proposals) {
            if (proposalService == null)
                ibp.put(p.getId(), p.collectInstrumentDisplayNames());
            else
                ibp.put(p.getId(), proposalService.getInstrumentNamesForProposal(p));
        }
        return ibp;
    }
}
