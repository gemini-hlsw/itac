package edu.gemini.tac.service.reports;

import edu.gemini.tac.persistence.Proposal;
import edu.gemini.tac.persistence.phase1.Instrument;
import edu.gemini.tac.persistence.phase1.Itac;
import edu.gemini.tac.persistence.phase1.Observation;
import edu.gemini.tac.persistence.phase1.TimeUnit;
import edu.gemini.tac.persistence.phase1.blueprint.BlueprintBase;
import org.apache.commons.lang.Validate;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.log4j.Logger;

import java.util.HashMap;
import java.util.Map;

public class InstrumentConfigurationReport extends Report {

    public static final String NAME = "instrumentConfiguration";

    private static final Logger LOGGER = Logger.getLogger(InstrumentConfigurationReport.class.getName());

    private Map<InstrumentConfigReportBean, InstrumentConfigReportBean> beans;

    /**
     * Constructs a new duplicate target reports object.
     */
    public InstrumentConfigurationReport() {
        super(NAME);
        beans = new HashMap<>();
    }

    @Override
    protected void collectDataForObservation(final Proposal proposal, final Observation observation /*, Resource defaultInstrument*/) {
        Validate.notNull(proposal);
        Validate.notNull(observation);
        Validate.notNull(proposal.getPhaseIProposal());
        Validate.notNull(proposal.getItac());

        final Itac itac = proposal.getItac();

        final BlueprintBase blueprint = observation.getBlueprint();
        final Instrument instrument = blueprint.getInstrument();

        String geminiReference = null;
        if (proposal.isAssignedGeminiId())
            geminiReference = itac.getAccept().getProgramId();
        else
            geminiReference = "-"; // replace with something nice and short for reporting

        final InstrumentConfigReportBean bean = new InstrumentConfigReportBean();
        bean.setGeminiId(geminiReference);
        bean.setPartner(proposal.getPartner().getAbbreviation());
        bean.setBand(getContextBanding() == null ? "-" : Long.toString(getContextBanding().getBand().getRank()));
        bean.setNtacRef(proposal.getSubmissionsPartnerEntry().getReceipt().getReceiptId());
        bean.setAllocTime(observation.getTime().convertTo(TimeUnit.MIN).getValue().doubleValue());
        bean.setInstrument(instrument.getDisplayName());
        bean.setConfigAdaptiveOptics(blueprint.getDisplayAdaptiveOptics());
        bean.setConfigCamera(blueprint.getDisplayCamera());
        bean.setConfigDisperser(blueprint.getDisplayDisperser());
        bean.setConfigFilter(blueprint.getDisplayFilter());
        bean.setConfigFocalPlaneUnit(blueprint.getDisplayFocalPlaneUnit());
        bean.setConfigOther(blueprint.getDisplayOther());
        bean.setMultiObjectSpectrograph(blueprint.isMOS() ? "Yes": "No");

        // check for beans with identical configurations and sum times if necessary
        final InstrumentConfigReportBean oldBean = beans.get(bean);
        if (oldBean != null) {
            bean.setAllocTime(bean.getAllocTime() + oldBean.getAllocTime());
        }

        // in case there was an old bean with this configuration it will be replaced with the new one (equal keys)
        beans.put(bean, bean);
    }

    @Override
    protected void boilBeans() {
        for (InstrumentConfigReportBean bean : beans.values()) {
            addResultBean(bean);
        }
    }

    public class InstrumentConfigReportBean extends ReportBean {
        private String geminiId;
        private String partner;
        private String band;
        private String ntacRef;
        private double allocTime;
        private String instrument;
        private String configCamera;
        private String configFocalPlaneUnit;
        private String configDisperser;
        private String configFilter;
        private String configAdaptiveOptics;
        private String multiObjectSpectrograph;
        private String configOther;

        public InstrumentConfigReportBean() {
            configCamera = "";
            configFocalPlaneUnit = "";
            configDisperser = "";
            configFilter = "";
            configAdaptiveOptics = "";
            multiObjectSpectrograph = "";
            configOther = "";
        }

        public String getGeminiId() {
            return geminiId;
        }

        public void setGeminiId(String geminiId) {
            this.geminiId = geminiId;
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

        public String getNtacRef() {
            return ntacRef;
        }

        public void setNtacRef(String ntacRef) {
            this.ntacRef = ntacRef;
        }

        public double getAllocTime() {
            return allocTime;
        }

        public void setAllocTime(double allocTime) {
            this.allocTime = allocTime;
        }

        public String getInstrument() {
            return instrument;
        }

        public void setInstrument(String instrument) {
            this.instrument = instrument;
        }

        public String getConfigCamera() {
            return configCamera;
        }

        public void setConfigCamera(String configCamera) {
            this.configCamera = configCamera;
        }

        public String getConfigFocalPlaneUnit() {
            return configFocalPlaneUnit;
        }

        public void setConfigFocalPlaneUnit(String configFocalPlaneUnit) {
            this.configFocalPlaneUnit = configFocalPlaneUnit;
        }

        public String getConfigDisperser() {
            return configDisperser;
        }

        public void setConfigDisperser(String configDisperser) {
            this.configDisperser = configDisperser;
        }

        public String getConfigFilter() {
            return configFilter;
        }

        public void setConfigFilter(String configFilter) {
            this.configFilter = configFilter;
        }

        public String getConfigAdaptiveOptics() {
            return configAdaptiveOptics;
        }

        public void setConfigAdaptiveOptics(String configOther) {
            this.configAdaptiveOptics = configOther;
        }

        public String getMultiObjectSpectrograph() {
            return multiObjectSpectrograph;
        }

        public void setMultiObjectSpectrograph(String multiObjectSpectrograph) {
            this.multiObjectSpectrograph = multiObjectSpectrograph;
        }

        public String getConfigOther() {
            return configOther;
        }

        public void setConfigOther(String configOther) {
            this.configOther = configOther;
        }


        // override hashCode and equals in order to have set get rid of duplicate entries...

        @Override
        public int hashCode() {
            return new HashCodeBuilder(117236111, 2141713121).
                    append(instrument).
                    append(configCamera).
                    append(configDisperser).
                    append(configFilter).
                    toHashCode();
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof InstrumentConfigReportBean))
                return false;

            final InstrumentConfigReportBean that = (InstrumentConfigReportBean) o;

            return new EqualsBuilder().
                    append(this.geminiId, that.geminiId).
                    append(this.partner, that.partner).
                    append(this.band, that.band).
                    append(this.ntacRef, that.ntacRef).
                    // two beans are considered equal if configuration is identical, time does not matter!
                    //append(this.allocTime, that.allocTime).
                    append(this.instrument, that.instrument).
                    append(this.configCamera, that.configCamera).
                    append(this.configDisperser, that.configDisperser).
                    append(this.configFilter, that.configFilter).
                    append(this.configOther, that.configOther).
                    append(this.configAdaptiveOptics, that.configAdaptiveOptics).
                    append(this.configFocalPlaneUnit, that.configFocalPlaneUnit).
                    append(this.multiObjectSpectrograph, that.multiObjectSpectrograph).
                    isEquals();
        }
    }
}
