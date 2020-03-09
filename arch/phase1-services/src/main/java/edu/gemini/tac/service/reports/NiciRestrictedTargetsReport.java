package edu.gemini.tac.service.reports;

import edu.gemini.shared.skycalc.Angle;
import edu.gemini.tac.persistence.Proposal;
import edu.gemini.tac.persistence.phase1.*;
import org.apache.commons.lang.Validate;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import java.io.*;
import java.util.LinkedList;
import java.util.List;

/**
 * Restricted targets report for NICI
*/
public class NiciRestrictedTargetsReport extends Report {

    public static final String NAME = "niciRestrictedTargets";

    private static final Logger LOGGER = Logger.getLogger(NiciRestrictedTargetsReport.class.getName());

    private List<SiderealTarget> restrictedTargets;

    /**
     * Constructs a new restricted targets reports object.
     */
    public NiciRestrictedTargetsReport(InputStream inputStream) {
        super(NAME);
        restrictedTargets = new LinkedList<SiderealTarget>();

        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            String line = null;
            while ((line = reader.readLine()) != null) {
                String[] tokens = line.split("\\t");

                if (tokens.length == 1 && tokens[0].isEmpty()) {
                    // ignore empty lines
                    continue;
                }
                if (tokens.length < 3) {
                    // throw exception if there are not at least three  tokens (name, c1, c2)
                    throw new RuntimeException("nici restricted targets input file is invalid");
                }

                HmsDmsCoordinates c = new HmsDmsCoordinates();
                c.setRa(tokens[1]);
                c.setDec(tokens[2]);

                SiderealTarget t = new SiderealTarget();
                t.setName(tokens[0]);
                t.setCoordinates(c);
                restrictedTargets.add(t);
            }
            inputStream.close();
        } catch (IOException e) {
            LOGGER.log(Level.ERROR, e);
            throw new RuntimeException("could not read nici restricted targets input file: " + e);
        }
    }

    @Override
    protected void collectDataForObservation(Proposal proposal, Observation observation) {

        // get the instrument for the observation
        String instrument = observation.getBlueprint().getInstrument().getDisplayName();
        Validate.notNull(instrument, "no instrument for observation");

        // we are only interested in observations that use NICI as the instrument
        if (!instrument.contains("NICI")) {
            return;
        }

        Target scienceTarget = observation.getTarget();
        if (scienceTarget instanceof SiderealTarget) {
            // NOTE: according to Sandy Leggett the restricted targets refer to stars therefore we do not
            // need to check non-sidereal coordinates against them (they usually refer to planets, comets etc.)
            for (SiderealTarget restrictedTarget : restrictedTargets) {
                double diffRa = raArcsecsDiff((SiderealTarget)scienceTarget, restrictedTarget);
                double diffDec = decArcsecsDiff((SiderealTarget)scienceTarget, restrictedTarget);
                if (diffRa < 60.0 && diffDec < 60.0) {
                    // too close to restricted target if it is closer than one arcmin to it
                    addRestrictedTargetMatch(proposal, (SiderealTarget) scienceTarget, restrictedTarget);
                }
            }
        }
    }
    
    private void addRestrictedTargetMatch(Proposal proposal, SiderealTarget scienceTarget, SiderealTarget restrictedTarget) {
        NICIRestrictedTargetsReportBean bean = new NICIRestrictedTargetsReportBean();
        bean.setC1Distance(raArcsecsDiff(scienceTarget, restrictedTarget));  // show distance in arcseconds
        bean.setC2Distance(decArcsecsDiff(scienceTarget, restrictedTarget)); // show distance in arcseconds
        bean.setPartner(proposal.getPartner().getAbbreviation());
        bean.setProposalId(proposal.getSubmissionsPartnerEntry().getReceipt().getReceiptId());
        bean.setProposalUrl(getUrl() + "/tac/committees/" + proposal.getCommittee().getId() + "/proposals/" + proposal.getId());
        bean.setRequestedTargetName(scienceTarget.getName());
        bean.setRequestedTargetCoordinates(scienceTarget.getCoordinates().toDisplayString());
        bean.setRestrictedTargetName(restrictedTarget.getName());
        bean.setRestrictedTargetCoordinates(restrictedTarget.getCoordinates().toDisplayString());
        addResultBean(bean);
    }

    private double raArcsecsDiff(SiderealTarget a, SiderealTarget b) {
        return Math.abs(
                a.getCoordinates().getRa().toArcsecs().getMagnitude() -
                b.getCoordinates().getRa().toArcsecs().getMagnitude());
    }
    private double decArcsecsDiff(SiderealTarget a, SiderealTarget b) {
        return Math.abs(
                a.getCoordinates().getDec().toArcsecs().getMagnitude() -
                b.getCoordinates().getDec().toArcsecs().getMagnitude());
    }


    public class NICIRestrictedTargetsReportBean extends ReportBean {

        private String partner;
        private String proposalId;
        private String requestedTargetName;
        private String requestedTargetCoordinates;
        private String restrictedTargetName;
        private String restrictedTargetCoordinates;
        private double c1Distance;
        private double c2Distance;
        private String url;

        public String getPartner() {
            return partner;
        }

        public void setPartner(String partner) {
            this.partner = partner;
        }

        public String getProposalId() {
            return proposalId;
        }

        public void setProposalId(String proposalId) {
            this.proposalId = proposalId;
        }

        public String getRequestedTargetName() {
            return requestedTargetName;
        }

        public void setRequestedTargetName(String requestedTargetName) {
            this.requestedTargetName = requestedTargetName;
        }

        public String getRequestedTargetCoordinates() {
            return requestedTargetCoordinates;
        }

        public void setRequestedTargetCoordinates(String requestedTargetCoordinates) {
            this.requestedTargetCoordinates = requestedTargetCoordinates;
        }

        public String getRestrictedTargetName() {
            return restrictedTargetName;
        }

        public void setRestrictedTargetName(String restrictedTargetName) {
            this.restrictedTargetName = restrictedTargetName;
        }

        public String getRestrictedTargetCoordinates() {
            return restrictedTargetCoordinates;
        }

        public void setRestrictedTargetCoordinates(String restrictedTargetCoordinates) {
            this.restrictedTargetCoordinates = restrictedTargetCoordinates;
        }

        public double getC1Distance() {
            return this.c1Distance;
        }

        public void setC1Distance(double c1Distance) {
            this.c1Distance = c1Distance;
        }

        public double getC2Distance() {
            return this.c2Distance;
        }

        public void setC2Distance(double c2Distance) {
            this.c2Distance = c2Distance;
        }

        public String getProposalUrl() {
            return this.url;
        }

        public void setProposalUrl(String url) {
            this.url = url;
        }
    }
}
