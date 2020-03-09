package edu.gemini.tac.service.reports;

import edu.gemini.tac.persistence.Proposal;
import edu.gemini.tac.persistence.phase1.*;
import org.apache.commons.lang.Validate;
import org.hibernate.Session;

import java.util.List;

/**
 * RA distribution reports.
 */
public abstract class RaDistributionReport extends Report {

    public static class ByBand extends RaDistributionReport {

        public static final String NAME = "raDistributionByBand";

        public ByBand() {
            super(NAME);
        }

        @Override
        public List<ReportBean> collectDataForCommittee(Session session, Long committeeId) {
            throw new RuntimeException("reports for banding information can only be generated for queues");
        }
    }

    public static class ByInstrument extends RaDistributionReport {

        public static final String NAME = "raDistributionByInstrument";

        public ByInstrument() {
            super(NAME);
        }
    }


    /**
     * Constructs a new ra distribution reports object.
     */
    private RaDistributionReport(String reportName) {
        super(reportName);
    }

    @Override
    protected void collectDataForObservation(final Proposal proposal, final Observation observation) {
        String instrumentName = observation.getBlueprint().getInstrument().getDisplayName();

        // data validation
        Validate.notNull(instrumentName, "No instrument for observation");
        Validate.notNull(observation.getTarget(), "No target for observation");
        Validate.notNull(observation.getTime(), "No exposure time");

        // band name and rank
        String bandName = getBandName(proposal);

        // normalize time
        double hours = getContextNormalizedExposureHrs(proposal, observation.getTime());

        // collect all relevant information
        RaDistributionReportBean bean = new RaDistributionReportBean();
        bean.setInstrumentName(instrumentName);
        bean.setSiteName(proposal.getSite()!=null ? proposal.getSite().getDisplayName() : "Exchange");
        bean.setRaBin(observation.getTarget().getRaBin());
        bean.setLaser(proposal.isLgs());
        bean.setHours(hours);
        bean.setBandName(bandName);
        bean.setRank(getBand(proposal).getRank());

        addResultBean(bean);
    }

    public class RaDistributionReportBean extends ReportBean {
        private long rank;
        private String siteName;
        private String instrumentName;
        private int raBin;
        private double hours;
        private boolean isLaser;
        private String bandName;

        public long getRank() {
            return rank;
        }

        public void setRank(long rank) {
            this.rank = rank;
        }

        public String getSiteName() {
            return siteName;
        }

        public void setSiteName(String site) {
            this.siteName = site;
        }

        public String getInstrumentName() {
            return instrumentName;
        }

        public void setInstrumentName(String instrumentName) {
            this.instrumentName = instrumentName;
        }

        public int getRaBin() {
            return raBin;
        }

        public void setRaBin(int raBin) {
            this.raBin = raBin;
        }

        public double getHours() {
            return hours;
        }

        public void setHours(double hours) {
            this.hours = hours;
        }

        public boolean isLaser() {
            return isLaser;
        }

        public boolean getLaser() {
            return isLaser;
        }

        public void setLaser(boolean laser) {
            isLaser = laser;
        }

        public String getBandName() {
            return bandName;
        }

        public void setBandName(String bandName) {
            this.bandName = bandName;
        }

    }


}
