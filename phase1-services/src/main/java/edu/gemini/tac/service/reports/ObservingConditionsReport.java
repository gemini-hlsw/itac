package edu.gemini.tac.service.reports;

import edu.gemini.tac.persistence.Proposal;
import edu.gemini.tac.persistence.phase1.Condition;
import edu.gemini.tac.persistence.phase1.ConditionsBin;
import edu.gemini.tac.persistence.phase1.Observation;
import edu.gemini.tac.persistence.queues.ScienceBand;
import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.lang.Validate;
import org.apache.log4j.Logger;
import org.hibernate.Session;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Report for observing conditions.
 * Prepares a set of data beans that can be displayed in different ways using jasper reports.
 */
public abstract class ObservingConditionsReport extends Report {

    private static final Logger LOGGER = Logger.getLogger(ObservingConditionsReport.class.getName());

    public static class ForAllBands extends ObservingConditionsReport {
        public static final String NAME = "observingConditions";
        public ForAllBands() {
            super(NAME, null);
        }
    }

    public static class ForBand1 extends ObservingConditionsReport {
        public static final String NAME = "observingConditionsForBand1";
        public ForBand1() {
            super("observingConditionsForBand", ScienceBand.BAND_ONE);
        }
    }

    public static class ForBand2 extends ObservingConditionsReport {
        public static final String NAME = "observingConditionsForBand2";
        public ForBand2() {
            super("observingConditionsForBand", ScienceBand.BAND_TWO);
        }
    }

    public static class ForBand3 extends ObservingConditionsReport {
        public static final String NAME = "observingConditionsForBand3";
        public ForBand3() {
            super("observingConditionsForBand", ScienceBand.BAND_THREE);
        }
    }

    public static class ForBand4 extends ObservingConditionsReport {
        public static final String NAME = "observingConditionsForBand4";
        public ForBand4() {
            super("observingConditionsForBand", ScienceBand.POOR_WEATHER);
        }
    }

    private static final Set bandsToShow = new HashSet<ScienceBand>();
    static {
        bandsToShow.add(ScienceBand.CLASSICAL);
        bandsToShow.add(ScienceBand.BAND_ONE);
        bandsToShow.add(ScienceBand.BAND_TWO);
        bandsToShow.add(ScienceBand.BAND_THREE);
        bandsToShow.add(ScienceBand.POOR_WEATHER);
    }
    
    private ScienceBand bandFilter = null;
    private double[] bandTotalTime;

    /**
     * Constructs a new observing conditions report.
     */
    private ObservingConditionsReport(String name, ScienceBand bandFilter) {
        super(name);

        // set filtering
        this.bandFilter = bandFilter;

        // general init
        bandTotalTime = new double[ScienceBand.byProgramIdOrder().size()];
        for (int i = 0; i < bandTotalTime.length; i++) {
            // init total hours per band
            bandTotalTime[i] = 0.0;
        }

        // Init one dummy record for each condition group; this will make sure that order and colors
        // for all groups will be the same in all reports and makes them easier to read; the downside of this
        // is that the reports will contain entries for 0 hours (0%).

        ObservingConditionsReportBean nullBean;
        List<ScienceBand> bands = ScienceBand.byProgramIdOrder();
        for (int i = 0; i < bands.size(); i++) {
            ScienceBand sb = bands.get(i);
            if (!bandsToShow.contains(sb)) {
                continue;    
            }
            // for each band add a "null" bean to make sure band is shown in report
            // even if there is no data for this band in the report we're going to render
            for (ConditionsBin bin : ConditionsBin.values()) {
                nullBean = new ObservingConditionsReportBean();
                nullBean.setBandName(sb.getDescription());
                if (sb.equals(ScienceBand.POOR_WEATHER)) nullBean.setBandName("Poor Weather");
                nullBean.setBandIndex(i);
                nullBean.setConditionName(bin.getName());
                nullBean.setConditionRank(bin.ordinal());
                nullBean.setHours(0.0);
                addResultBean(nullBean);
            }
        }
    }

    @Override
    public List<ReportBean> collectDataForCommittee(Session session, Long committeeId) {
        // this report relies on banding information in queues, it can therefore not run on a committee!
        throw new NotImplementedException();
    }

    @Override
    protected void collectDataForObservation(Proposal proposal, Observation observation) {

        ScienceBand proposalBand = getBand(proposal);

        // filter for band if necessary
        if (bandFilter != null && !bandFilter.equals(proposalBand)) {
            return;
        }

        // band name
        String bandName = getBandName(proposal);
        if (proposalBand.equals(ScienceBand.POOR_WEATHER)) {
            bandName = "Poor Weather"; // use shorter name for graph
        }

        Validate.notNull(observation);
        Condition siteQuality = observation.getCondition();
        Validate.notNull(siteQuality, "Missing observation conditions");
        int bandIndex = getBandIndex(proposalBand);
        if (bandIndex >= 7) {
            throw new IllegalArgumentException();
        }

        // normalize time
        double hours = getContextNormalizedExposureHrs(proposal, observation.getTime());

        bandTotalTime[bandIndex] = bandTotalTime[bandIndex] + hours;
        ConditionsBin conditionsGroup = ConditionsBin.forCondition(siteQuality);

        ObservingConditionsReportBean bean = new ObservingConditionsReportBean();
        bean.setBandName(bandName);
        bean.setBandIndex(bandIndex);
        bean.setConditionRank(conditionsGroup.ordinal());
        bean.setObservingMode(getObservingMode(proposal));
        bean.setConditionName(conditionsGroup.getName());
        bean.setHours(hours);

        addResultBean(bean);
    }
    
    private int getBandIndex(ScienceBand sb) {
        int i = 0;
        for (ScienceBand orderedSb : ScienceBand.byProgramIdOrder()) {
            if (sb.equals(orderedSb)) {
                return i;
            }
            i++;
        }
        throw new IllegalArgumentException("science band not found in byProgramIdOrder");
    }
    
    private String getObservingMode(Proposal p) {
        if (p.isToo()) {
            return "ToO";
        } else if (p.isClassical()) {
            return "Classical";
        } else {
            return "Queue";
        }
    }


    @Override
    public List<ReportBean> getData() {
        calculatePercentages();
        return super.getData();
    }

    private void calculatePercentages() {
        List<ReportBean> beans = super.getData();
        for (ReportBean b : beans) {
            ObservingConditionsReportBean bean = (ObservingConditionsReportBean) b;
            bean.setHoursPercent(bean.getHours() / bandTotalTime[bean.getBandIndex()] * 100.0);
        }
    }

    public class ObservingConditionsReportBean extends ReportBean {

        private String bandName;
        private int bandIndex;
        private int conditionRank;
        private String observingMode;
        private String conditionName;
        private double hours;
        private double hoursPercent;

        public String getBandName() {
            return bandName;
        }

        public void setBandName(String bandName) {
            this.bandName = bandName;
        }

        public int getBandIndex() {
            return bandIndex;
        }

        public void setBandIndex(int bandIndex) {
            this.bandIndex = bandIndex;
        }

        public int getConditionRank() {
            return conditionRank;
        }

        public void setConditionRank(int rank) {
            this.conditionRank = rank;
        }

        public String getObservingMode() {
            return observingMode;
        }

        public void setObservingMode(String observingMode) {
            this.observingMode = observingMode;
        }

        public String getConditionName() {
            return conditionName;
        }

        public void setConditionName(String conditionName) {
            this.conditionName = conditionName;
        }

        public double getHours() {
            return hours;
        }

        public void setHours(double hours) {
            this.hours = hours;
        }

        public double getHoursPercent() {
            return hoursPercent;
        }

        public void setHoursPercent(double hoursPercent) {
            this.hoursPercent = hoursPercent;
        }


    }
}
