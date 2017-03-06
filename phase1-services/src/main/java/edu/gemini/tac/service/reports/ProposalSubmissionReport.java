package edu.gemini.tac.service.reports;

import edu.gemini.tac.persistence.Proposal;
import edu.gemini.tac.persistence.phase1.Observation;
import edu.gemini.tac.persistence.phase1.submission.Submission;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import java.util.HashSet;
import java.util.Set;

/**
* ITAC-475
*/
public class ProposalSubmissionReport extends Report {
   private static final Logger LOGGER = Logger.getLogger(ProposalSubmissionReport.class.getName());

   public static final String NAME = "proposalSubmissions";

   /**
    * Constructs a new proposals report.
    */
   public ProposalSubmissionReport() {
      super(NAME);
   }

    @Override
    protected void collectDataForProposal(Proposal proposal) {
        if (matchesProposalFilter(proposal)) {
            // -- this is a quick hack to save some space.. should be done somewhere else...
            String partnerAbbreviation = proposal.getPartnerAbbreviation().replaceAll("GeminiStaff", "Ge");
            // -- end quick hack
            
            // collect all instruments
            Set<String> instruments = new HashSet<String>();
            for (Observation o : proposal.getPhaseIProposal().getObservations()) {
                instruments.add(o.getBlueprint().getInstrument().getDisplayName());
            }
            
            ProposalsReportBean bean = new ProposalsReportBean();
            bean.setPartnerReference(proposal.getPartnerReferenceNumber());
            bean.setKeywordsCategory(proposal.getPhaseIProposal().getTacCategory().name());
            bean.setPiName(proposal.getPhaseIProposal().getInvestigators().getPi().getLastName());
            bean.setPartnerScientist(proposal.getPhaseIProposal().getPrimary().getAccept().getEmail());
            bean.setTitle(proposal.getPhaseIProposal().getTitle());
            bean.setInstruments(StringUtils.join(instruments, ", "));
            bean.setBand3(Boolean.toString(proposal.isBand3()));
            
            Submission submission = proposal.getPhaseIProposal().getPrimary(); 
            bean.setSubmissionRequestedTime(submission.getRequest().getTime().getValueInHours().toString());
            bean.setMinimumPartnerRequest(submission.getRequest().getMinTime().getValueInHours().toString());
            bean.setPartnerRequestedTime(proposal.getPhaseIProposal().getTotalAwardedTime().getValueInHours().toString());
            bean.setTimeUnits("Hours");
            bean.setQueueOrClassical(proposal.isClassical() ? "C" : "Q");
            bean.setPW(Boolean.toString(proposal.isPw()));
            bean.setToO(Boolean.toString(proposal.isToo()));
            bean.setLgs(Boolean.toString(proposal.isLgs()));
            bean.setPartners(partnerAbbreviation);
            bean.setSite(proposal.getSite() == null ? "Exchange" : proposal.getSite().getDisplayName());
            
            /*
            bean.setBand(bandName);
            bean.setPartnerAllocations(partnerAllocatedTime);
            */
            
            addResultBean(bean);
      }
   }

   public class ProposalsReportBean extends ReportBean {
      private String partnerReference;
      private String keywordsCategory;
      private String piName;
      private String partnerScientist;
      private String title;
      private String instruments;
      private String isBand3;
      private String submissionRequestedTime;
      private String partnerRequestedTime;
      private String minimumPartnerRequest;
      private String timeUnits;
      private String jointPartners;
      private String site;
      private String queueOrClassical;
      private String isPW;
      private String ToO;
      private String lgs;


      public String getPartnerReference() {
         return partnerReference;
      }

      public void setPartnerReference(String partnerReference) {
         this.partnerReference = partnerReference;
      }

      public String getKeywordsCategory() {
         return keywordsCategory;
      }

      public void setKeywordsCategory(String keywordsCategory) {
         this.keywordsCategory = keywordsCategory;
      }

      public String getPiName() {
         return piName;
      }

      public void setPiName(String piName) {
         this.piName = piName;
      }

      public String getPartnerScientist() {
         return partnerScientist;
      }

      public void setPartnerScientist(String partnerScientist) {
         this.partnerScientist = partnerScientist;
      }

      public String getTitle() {
         return title;
      }

      public void setTitle(String title) {
         this.title = title;
      }

      public String getInstruments() {
         return instruments;
      }

      public void setInstruments(String instruments) {
         this.instruments = instruments;
      }

      public String getBand3() {
         return isBand3;
      }

      public void setBand3(String band3) {
         isBand3 = band3;
      }

      public String getSubmissionRequestedTime() {
         return submissionRequestedTime;
      }

      public void setSubmissionRequestedTime(String submissionRequestedTime) {
         this.submissionRequestedTime = submissionRequestedTime;
      }

      public String getPartnerRequestedTime() {
         return partnerRequestedTime;
      }

      public void setPartnerRequestedTime(String partnerRequestedTime) {
         this.partnerRequestedTime = partnerRequestedTime;
      }

      public String getMinimumPartnerRequest() {
         return minimumPartnerRequest;
      }

      public void setMinimumPartnerRequest(String minimumPartnerRequest) {
         this.minimumPartnerRequest = minimumPartnerRequest;
      }

      public String getTimeUnits() {
         return timeUnits;
      }

      public void setTimeUnits(String timeUnits) {
         this.timeUnits = timeUnits;
      }

      public String getPartners() {
         return jointPartners;
      }

      public void setPartners(String jointPartners) {
         this.jointPartners = jointPartners;
      }

      public String getSite() {
         return site;
      }

      public void setSite(String site) {
         this.site = site;
      }

      public String getQueueOrClassical() {
         return queueOrClassical;
      }

      public void setQueueOrClassical(String queueOrClassical) {
         this.queueOrClassical = queueOrClassical;
      }

      public String getPW() {
         return isPW;
      }

      public void setPW(String PW) {
         isPW = PW;
      }

      public String getToO() {
         return ToO;
      }

      public void setToO(String toO) {
         ToO = toO;
      }

      public String getLgs() {
         return lgs;
      }

      public void setLgs(String LGS) {
         lgs = LGS;
      }
   }
}
