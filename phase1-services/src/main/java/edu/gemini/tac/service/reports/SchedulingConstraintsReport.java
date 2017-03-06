package edu.gemini.tac.service.reports;

import edu.gemini.tac.persistence.Proposal;
import org.apache.log4j.Logger;

/**
 * Additional report for scheduling constraints.
 * REL-1161
*/
public class SchedulingConstraintsReport extends Report {
   private static final Logger LOGGER = Logger.getLogger(SchedulingConstraintsReport.class.getName());

   public static final String NAME = "schedulingConstraints";

   /**
    * Constructs a new proposals report.
    */
   public SchedulingConstraintsReport() {
      super(NAME);
   }

    @Override
    protected void collectDataForProposal(Proposal proposal) {

        // filter unwanted proposals
        if (!matchesProposalFilter(proposal)) return;
        // NOTE: in theory this should be part of the proposalFilter above, but I don't dare to add
        // this to the filter above right now in order not to take the risk of breaking existing reports
        // NOTE2: for exchange proposals site will be null.
        if (proposal.getSite() == null || !matchesSiteFilter(proposal.getSite().getDisplayName())) return;

        // check if we have a gemini id assigned
        String geminiId;
        if (proposal.isAssignedGeminiId()) {
            geminiId = proposal.getItac().getAccept().getProgramId();
        } else {
            geminiId = "-";
        }


        if (proposal.isJoint()) {
            // scheduling information exists on all individual proposals of a joint proposal,
            // handle all of them separately
            for (Proposal p : proposal.getProposals()) {
                doCollectData(geminiId, p);
            }
        } else {
            // for all other proposals deal with proposal directly
            doCollectData(geminiId, proposal);
        }
    }

    private void doCollectData(String geminiId, Proposal proposal) {
        // only collect data for proposals that actually have a scheduling note
        String scheduling = proposal.getPhaseIProposal().getScheduling();
        if (scheduling == null || scheduling.isEmpty()) {
            return;
        }

        // -- this is a quick hack to save some space.. should be done somewhere else...
        String partnerAbbreviation = proposal.getPartnerAbbreviation().replaceAll("GeminiStaff", "Ge");
        // -- end quick hack

        // put everything together
        SchedulingConstraintsReportBean bean = new SchedulingConstraintsReportBean();
        bean.partner = partnerAbbreviation;
        bean.geminiId = geminiId;
        bean.ntacRef = proposal.getPhaseIProposal().getPrimary().getReceipt().getReceiptId();
        bean.scheduling = scheduling;

        // store bean for usage in jasper report
        addResultBean(bean);
    }

    public class SchedulingConstraintsReportBean extends ReportBean {
        String partner;
        String geminiId;
        String ntacRef;
        String scheduling;

        public String getPartner() { return partner; }
        public String getGeminiId() { return geminiId; }
        public String getNtacRef() { return ntacRef; }
        public String getScheduling() { return scheduling; }
    }

}
