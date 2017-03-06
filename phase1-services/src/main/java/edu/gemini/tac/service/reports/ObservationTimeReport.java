package edu.gemini.tac.service.reports;

import edu.gemini.tac.persistence.Proposal;
import edu.gemini.tac.persistence.phase1.Observation;
import org.apache.log4j.Logger;

public abstract class ObservationTimeReport extends Report {

    public static class ByBand extends ObservationTimeReport {
        public static final String NAME = "observationTimeByBand";

        public ByBand() {
            super(NAME);
        }
    }

    public static class ByInstrument extends ObservationTimeReport {
        public static final String NAME = "observationTimeByInstrument";

        public ByInstrument() {
            super(NAME);
        }
    }

    public static class ByPartner extends ObservationTimeReport {
        public static final String NAME = "observationTimeByPartner";

        public ByPartner() {
            super(NAME);
        }
    }

    private static final Logger LOGGER = Logger.getLogger(ObservationTimeReport.class.getName());

    /**
     * Constructs a new duplicate target reports object.
     */
    private ObservationTimeReport(String name) {
        super(name);
    }

    @Override
    protected void collectDataForObservation(final Proposal proposal, final Observation observation) {
        // band name
        String bandName = getBandName(proposal);

        // instrument name (including LGS yes / no)
        String instrument = observation.getBlueprint().getInstrument().getDisplayName();
        if (proposal.isLgs()) {
            instrument = instrument + "+LGS";
        }

        // observation time normalized by the amount of hours that were actually assigned to the proposal for banded proposals
        double hours = getContextNormalizedExposureHrs(proposal, observation.getTime());

        // create and fill bean
        ObservationTimeReportBean bean = new ObservationTimeReportBean();
        bean.setRank(getBand(proposal).getRank());
        bean.setPartner(proposal.getPartner().getAbbreviation());
        bean.setBand(bandName);
        bean.setInstrument(instrument);
        bean.setHours(hours);
        addResultBean(bean);
    }


    public class ObservationTimeReportBean extends ReportBean {
        private long rank;
        private String partner;
        private String band;
        private String instrument;
        private double hours;

        public long getRank() {
            return rank;
        }

        public void setRank(long rank) {
            this.rank = rank;
        }

        public String getPartner() {
            return partner;
        }

        public void setPartner(String partner) {
            this.partner = partner;
        }

        public String getBand() {
            return band;
        }

        public void setBand(String band) {
            this.band = band;
        }

        public String getInstrument() {
            return instrument;
        }

        public void setInstrument(String instrument) {
            this.instrument = instrument;
        }

        public double getHours() {
            return hours;
        }

        public void setHours(double hours) {
            this.hours = hours;
        }
    }

}
