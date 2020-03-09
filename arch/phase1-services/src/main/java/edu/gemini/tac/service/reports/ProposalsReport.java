package edu.gemini.tac.service.reports;

import edu.gemini.model.p1.mutable.TooOption;
import edu.gemini.tac.persistence.Proposal;
import edu.gemini.tac.persistence.phase1.Condition;
import edu.gemini.tac.persistence.phase1.ConditionsBin;
import edu.gemini.tac.persistence.phase1.Observation;
import edu.gemini.tac.service.IQueueService;
import org.apache.commons.lang.Validate;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * Report for proposals.
 */
public class ProposalsReport extends Report {

    private static final Logger LOGGER = Logger.getLogger(ProposalsReport.class.getName());

    public static final String NAME = "proposals";

    /**
     * Constructs a new proposals report.
     */
    public ProposalsReport() {
        super(NAME);
    }

    @Override
    protected void collectDataForProposal(Proposal proposal) {

        // band name
        String bandName;
        Integer mergeIndex;
        String geminiId = "-"; //Set to default for unfinalized queue
        String partnerAbbreviation;
        String partnerAllocatedTime;
        String awardedTime;
        if (getContextBanding() == null) {  // this proposal is not banded but classical
            bandName = "C";
            mergeIndex = -1;
            //If queue's finalized, get the real ID
            if (proposal.getItac().getAccept() != null && proposal.getItac().getAccept().getProgramId() != null) {
                geminiId = proposal.getItac().getAccept().getProgramId();
            }
            partnerAbbreviation = proposal.getPartnerAbbreviation();
            partnerAllocatedTime = proposal.getPartnerRecommendedTime();
            awardedTime = proposal.getTotalAwardedTime().toPrettyString();
        } else {
            // this works only for banded proposals
            Validate.notNull(getContextBanding());
            // get information...
            bandName = Long.toString(getContextBanding().getBand().getRank());
            mergeIndex = getContextBanding().getMergeIndex();
            // note that some information must be taken from the banding
            // in order to reflect a) split joints and b) partially accepted joints
            if (getContextBanding().getProgramId() != null) {
                geminiId = getContextBanding().getProgramId();
            }
            partnerAbbreviation = getContextBanding().getPartnerAbbreviation() + " " + getContextBanding().getRejectedPartnerAbbreviation();
            partnerAllocatedTime = getContextBanding().getPartnerRecommendedTime();
            awardedTime = getContextBanding().getAwardedTime().toPrettyString();
        }

        // -- this is a quick hack to save some space.. should be done somewhere else...
        partnerAbbreviation = partnerAbbreviation.replaceAll("GeminiStaff", "Ge");
        partnerAllocatedTime = partnerAllocatedTime.replaceAll("GeminiStaff", "Ge");
        // -- end quick hack

        ProposalsReportBean bean = new ProposalsReportBean();

        if (mergeIndex == null) {     // UX-1507 queue report proposal list broken.  While we need to fix the problem at it's source, we also shouldn't refuse to report.
            LOGGER.error(String.format("Merge index for banding %d and proposal %d in queue %d is missing.  Something has gone wrong elsewhere, reporting merge index as -1.", getContextBanding().getId(), proposal.getId(), getContextBanding().getQueue().getId()));
            mergeIndex = -1;
        }
        bean.setMergeIndex(mergeIndex);
        bean.setGeminiId(geminiId);
        bean.setPiName(proposal.getPhaseIProposal().getInvestigators().getPi().getLastName());
        bean.setAllocatedTime(awardedTime);
        bean.setBand(bandName);
        bean.setInstruments(proposal.getPhaseIProposal().getInstrumentsDisplay());
        bean.setToO(tooOptionToDisplay(proposal.getPhaseIProposal().getTooOption()));
        bean.setLgs(Boolean.toString(proposal.isLgs()));
        bean.setPartners(partnerAbbreviation);
        bean.setPartnerAllocations(partnerAllocatedTime);
        bean.setNgo(proposal.getPartner().getAbbreviation().replaceAll("GeminiStaff", "GS")); // another quick hack to cut down length of strings
        bean.setNgoEmail(proposal.getPartnerTacExtension().getAccept().getEmail());
        if (proposal.getItac() != null && proposal.getItac().getAccept() != null) {
            bean.setStaffEmail(proposal.getItac().getAccept().getContact());
        } else {
            bean.setStaffEmail("-");
        }
        bean.setTitle(proposal.getPhaseIProposal().getTitle());
        List<Condition> conditions = proposal.getPhaseIProposal().getConditions();
        Condition best = getBestCondition(conditions);
        if (bandName.equals("3")) {
            // if this proposal is in band 3 and we have active band 3 observations than show best of those conditions
            // (note that it can happen that there are no active band 3 observations even if the proposal is in
            // band 3, the question is if this makes sense but we did have data like that in 2012B)
            List<Condition> band3Conditions = proposal.getPhaseIProposal().getBand3Conditions();
            if (band3Conditions.size() > 0) {
                best = getBestCondition(band3Conditions);
            }
        }
        bean.setConditions(best.getName());
        bean.setPartnerReference(proposal.getPartnerReferenceNumber());

        addResultBean(bean);
    }

    /**
     * Picks one of the "best" observing conditions from the list.
     * There is no real order defined on all the different observing conditions but there is an order defined
     * for {@link ConditionsBin}s which we are using here by returning the first condition for the best bin
     * we find. Strictly speaking, this might not be the absolutely best condition but it is a good enough
     * indicator for the best observing conditions needed by the proposal for this report.
     * @param conditions
     * @return
     */
    private Condition getBestCondition(List<Condition> conditions) {
        Validate.isTrue(conditions.size() > 0);
        Condition bestCondition = conditions.get(0);
        ConditionsBin bestBin = ConditionsBin.forCondition(bestCondition);
        for (Condition c : conditions) {
            ConditionsBin otherBin = ConditionsBin.forCondition(c);
            if (otherBin.compareTo(bestBin) < 0) {
                bestCondition = c;
            }
        }
        return bestCondition;
    }

    private String tooOptionToDisplay(final TooOption tooOption) {
        switch(tooOption) {
            case NONE:
                return "";
            case RAPID:
                return "RT";
            case STANDARD:
                return "ST";
            default:
                return "?";
        }
    }

    public class ProposalsReportBean extends ReportBean {

        private int mergeIndex;
        private String geminiId;
        private String piName;
        private String allocatedTime;
        private String band;
        private String instruments;
        private String toO;
        private String lgs;
        private String partners;
        private String partnerAllocations;
        private String ngo;
        private String ngoEmail;
        private String staffEmail;
        private String title;
        private String conditions;
        private String partnerReference;

        public int getMergeIndex() {
            return mergeIndex;
        }

        public void setMergeIndex(int mergeIndex) {
            this.mergeIndex = mergeIndex;
        }

        public String getGeminiId() {
            return geminiId;
        }

        public void setGeminiId(String geminiId) {
            this.geminiId = geminiId;
        }

        public String getPiName() {
            return piName;
        }

        public void setPiName(String piName) {
            this.piName = piName;
        }

        public String getAllocatedTime() {
            return allocatedTime;
        }

        public void setAllocatedTime(String allocatedTime) {
            this.allocatedTime = allocatedTime;
        }

        public String getBand() {
            return band;
        }

        public void setBand(String band) {
            this.band = band;
        }

        public String getInstruments() {
            return instruments;
        }

        public void setInstruments(String instruments) {
            this.instruments = instruments;
        }

        public String getToO() {
            return toO;
        }

        public void setToO(String toO) {
            this.toO = toO;
        }

        public String getLgs() {
            return lgs;
        }

        public void setLgs(String lgs) {
            this.lgs = lgs;
        }

        public String getPartners() {
            return partners;
        }

        public void setPartners(String partners) {
            this.partners = partners;
        }

        public String getPartnerAllocations() {
            return partnerAllocations;
        }

        public void setPartnerAllocations(String partnerAllocations) {
            this.partnerAllocations = partnerAllocations;
        }

        public String getNgo() {
            return ngo;
        }

        public void setNgo(String ngo) {
            this.ngo = ngo;
        }

        public String getNgoEmail() {
            return ngoEmail;
        }

        public void setNgoEmail(String ngoEmail) {
            this.ngoEmail = ngoEmail;
        }

        public String getStaffEmail() {
            return staffEmail;
        }

        public void setStaffEmail(String staffEmail) {
            this.staffEmail = staffEmail;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getConditions() {
            return conditions;
        }

        public void setConditions(String conditions) {
            this.conditions = conditions;
        }

        public String getPartnerReference() {
            return partnerReference;
        }

        public void setPartnerReference(String partnerReference) {
            this.partnerReference = partnerReference;
        }
    }
}
