package edu.gemini.tac.persistence.fixtures;

import edu.gemini.model.p1.mutable.*;
import edu.gemini.model.p1.mutable.Band;
import edu.gemini.model.p1.mutable.CoordinatesEpoch;
import edu.gemini.shared.skycalc.Angle;
import edu.gemini.shared.util.DateRange;
import edu.gemini.tac.exchange.ProposalExporterImpl;
import edu.gemini.tac.persistence.*;
import edu.gemini.tac.persistence.Proposal;
import edu.gemini.tac.persistence.Semester;
import edu.gemini.tac.persistence.Site;
import edu.gemini.tac.persistence.bin.BinConfiguration;
import edu.gemini.tac.persistence.bin.DecBinSize;
import edu.gemini.tac.persistence.bin.RABinSize;
import edu.gemini.tac.persistence.condition.ConditionBucket;
import edu.gemini.tac.persistence.condition.ConditionSet;
import edu.gemini.tac.persistence.daterange.DateRangePersister;
import edu.gemini.tac.persistence.joints.JointProposal;
import edu.gemini.tac.persistence.phase1.CoInvestigator;
import edu.gemini.tac.persistence.phase1.Condition;
import edu.gemini.tac.persistence.phase1.Coordinates;
import edu.gemini.tac.persistence.phase1.DegDegCoordinates;
import edu.gemini.tac.persistence.phase1.EphemerisElement;
import edu.gemini.tac.persistence.phase1.GuideStar;
import edu.gemini.tac.persistence.phase1.HmsDmsCoordinates;
import edu.gemini.tac.persistence.phase1.InstitutionAddress;
import edu.gemini.tac.persistence.phase1.*;
import edu.gemini.tac.persistence.phase1.Investigator;
import edu.gemini.tac.persistence.phase1.Investigators;
import edu.gemini.tac.persistence.phase1.Itac;
import edu.gemini.tac.persistence.phase1.ItacAccept;
import edu.gemini.tac.persistence.phase1.Magnitude;
import edu.gemini.tac.persistence.phase1.Meta;
import edu.gemini.tac.persistence.phase1.Observation;
import edu.gemini.tac.persistence.phase1.ObservationMetaData;
import edu.gemini.tac.persistence.phase1.PrincipalInvestigator;
import edu.gemini.tac.persistence.phase1.ProperMotion;
import edu.gemini.tac.persistence.phase1.SiderealTarget;
import edu.gemini.tac.persistence.phase1.Target;
import edu.gemini.tac.persistence.phase1.TimeAmount;
import edu.gemini.tac.persistence.phase1.TimeUnit;
import edu.gemini.tac.persistence.phase1.TooTarget;
import edu.gemini.tac.persistence.phase1.blueprint.BlueprintBase;
import edu.gemini.tac.persistence.phase1.blueprint.altair.AltairConfiguration;
import edu.gemini.tac.persistence.phase1.blueprint.gnirs.GnirsBlueprintImaging;
import edu.gemini.tac.persistence.phase1.blueprint.gnirs.GnirsBlueprintSpectroscopy;
import edu.gemini.tac.persistence.phase1.blueprint.niri.NiriBlueprint;
import edu.gemini.tac.persistence.phase1.proposal.*;
import edu.gemini.tac.persistence.phase1.queues.PartnerSequence;
import edu.gemini.tac.persistence.phase1.sitequality.CloudCover;
import edu.gemini.tac.persistence.phase1.sitequality.ImageQuality;
import edu.gemini.tac.persistence.phase1.sitequality.SkyBackground;
import edu.gemini.tac.persistence.phase1.sitequality.WaterVapor;
import edu.gemini.tac.persistence.phase1.submission.ExchangeSubmission;
import edu.gemini.tac.persistence.phase1.submission.LargeProgramSubmission;
import edu.gemini.tac.persistence.phase1.submission.NgoSubmission;
import edu.gemini.tac.persistence.phase1.submission.Submission;
import edu.gemini.tac.persistence.daterange.Shutdown;
import edu.gemini.tac.persistence.queues.Banding;
import edu.gemini.tac.persistence.queues.Queue;
import edu.gemini.tac.persistence.queues.ScienceBand;
import edu.gemini.tac.persistence.rollover.AbstractRolloverObservation;
import edu.gemini.tac.persistence.rollover.RolloverObservation;
import edu.gemini.tac.persistence.rollover.RolloverReport;
import edu.gemini.tac.persistence.rollover.RolloverSet;
import edu.gemini.tac.persistence.security.AuthorityRole;
import edu.gemini.tac.util.ProposalImporter;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.Validate;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.hibernate.*;
import org.joda.time.DateTime;
import org.joda.time.Period;
import org.junit.*;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

/**
 * Taking a different tack from the nightmare of XML maintenance we had last time.  Florian's idea seems
 * sound.  I'm going to give it a try.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"/data-applicationContext.xml"})
public class HibernateFixture extends Fixture {
    private static final Logger LOGGER = Logger.getLogger(HibernateFixture.class);

    @Resource(name = "sessionFactory")
    protected SessionFactory sessionFactory;

    /* Generally, sorted by increasing dependencies (less-dependent objects near top */

    /*
    The general pattern is:
        There is a List<T> ts for every Entity

        It is populated in an initT() method. Generally, there should be at least 1 T for every structural
        variant available in T's components (e.g., there should be at least 1 TimeAmount for every TimeUnit value).
        However, the combinations of the complex objects (PhaseIProposal, etc.) make this impractical. Such objects
        can have an initT(int n) method that generates n objects. ?: Some kind of Dijkstra-algorithm 'random' structure?

        There should be a function <T> Tcopy(int i) or <T> T(int i) that retrieves ts[i % ts.size]. Tcopy() should be
        implemented for *:n relationships, t() for *:1

        The elements of ts MAY be stored in the database by explicit tests but are not initially stored. Remember that
        since Tcopy() exists, the entities in ts do not necessarily have a reference correspondence to similarly-
        valued entities that are components of higher-level entities.

        Every entity should be cleared from the database in after()
     */

    protected List<Site> sites;
    protected List<TimeAmount> times;
    protected List<Investigator> investigators;
    protected List<DegDegCoordinates> degDegCoordinates;
    protected List<HmsDmsCoordinates> hmsDmsCoordinates;
    protected List<Magnitude> magnitudes;
    protected List<ProperMotion> properMotions;
    protected List<EphemerisElement> ephemerisElements;
    protected List<SuccessEstimation> successEstimations;

    protected List<GuideStar> guideStars;
    protected List<BlueprintBase> blueprintBases;
    protected List<NonsiderealTarget> nonSiderealTargets;
    protected List<SiderealTarget> siderealTargets;
    protected List<ItacAccept> itacAccepts;
    protected List<edu.gemini.tac.persistence.phase1.submission.SubmissionAccept> submissionAccepts;
    protected List<edu.gemini.tac.persistence.phase1.submission.SubmissionReceipt> submissionReceipts;
    protected List<edu.gemini.tac.persistence.phase1.submission.SubmissionRequest> submissionRequests;
    protected List<CoInvestigator> coInvestigators;
    protected List<InstitutionAddress> institutionAddresses;
    protected List<PrincipalInvestigator> principalInvestigators;
    protected List<Condition> conditions;
    protected List<Observation> observations;
    protected List<ExchangeSubmission> exchangeSubmissions;
    protected List<LargeProgramSubmission> largeProgramSubmissions;
    protected List<NgoSubmission> ngoSubmissions;
    protected List<Itac> itacs;
    protected List<Investigators> investigatorTeams;
    protected List<Meta> metas;
    protected List<PhaseIProposal> phaseIProposals;
    protected List<ExchangeProposal> exchangeProposals = new ArrayList<>();
    protected List<ClassicalProposal> classicalProposals = new ArrayList<>();
    protected List<QueueProposal> queueProposals = new ArrayList<>();
    protected List<LargeProgram> largePrograms = new ArrayList<>();

    protected List<Partner> partners;
    protected List<Committee> committees;
    protected List<PartnerSequence> partnerSequences;
    protected List<Proposal> proposals;

    protected List<Banding> bandings;
    protected List<Semester> semesters;
    protected List<Queue> queues;
    protected List<RABinSize> raBinSizes;
    protected List<DecBinSize> decBinSizes;
    protected List<ConditionBucket> conditionBuckets;
    protected List<ConditionSet> conditionSets;
    protected List<BinConfiguration> binConfigurations;
    protected List<Shutdown> shutdowns;

    protected List<RolloverObservation> rolloverObservations;
    protected List<RolloverSet> rolloverSets;
    protected List<RolloverReport> rolloverReports;

    protected List<AuthorityRole> authorityRoles = new ArrayList<>();
    protected List<Person> people = new ArrayList<>();
    private List<ObservationMetaData> observationMetaDatas;
    private List<ObservationMetaData.GuidingEstimation> guidingEstimations;

    protected HibernateFixture() {
    }

    public HibernateFixture(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    private void initTimes() {
        times = new ArrayList<>();
        times.add(new TimeAmount(1.0, TimeUnit.HR));
        times.add(new TimeAmount(1.0, TimeUnit.NIGHT));
    }

    protected TimeAmount timeAmountCopy(int i) {
        TimeAmount ta = times.get(i % times.size());
        return new TimeAmount(ta);
    }

    private void initDegDegCoordinates() {
        degDegCoordinates = new ArrayList<>();
        final DegDegCoordinates coordinates = new DegDegCoordinates();
        coordinates.setDec(BigDecimal.valueOf(1.23));
        coordinates.setRa(BigDecimal.valueOf(4.56));
        degDegCoordinates.add(coordinates);
    }

    protected DegDegCoordinates degDegCoordinatesCopy(int i) {
        DegDegCoordinates dc = degDegCoordinates.get(i % degDegCoordinates.size());
        final DegDegCoordinates dc2 = new DegDegCoordinates();
        dc2.setDec(dc.getDec());
        dc2.setRa(dc.getRa());
        return dc2;
    }

    private void initHmsDmsCoordinates() {
        hmsDmsCoordinates = new ArrayList<>();
        HmsDmsCoordinates coordinates = new HmsDmsCoordinates();
        coordinates.setDec("67:23:45");
        coordinates.setRa("12:12:34");
        hmsDmsCoordinates.add(coordinates);
    }

    protected HmsDmsCoordinates hmsDmsCoordinatesCopy(int i) {
        HmsDmsCoordinates c = hmsDmsCoordinates.get(i % hmsDmsCoordinates.size());
        HmsDmsCoordinates c2 = new HmsDmsCoordinates();
        c2.setDec(c.getDec());
        c2.setRa(c.getRa());
        return c2;
    }

    private void initMagnitudes() {
        magnitudes = new ArrayList<>();
        BigDecimal value = BigDecimal.valueOf(10.0);
        for (MagnitudeBand band : MagnitudeBand.values()) {
            for (MagnitudeSystem magnitudeSystem : MagnitudeSystem.values()) {
                Magnitude magnitude = new Magnitude();
                magnitude.setValue(value);
                magnitude.setBand(band);
                magnitude.setSystem(magnitudeSystem);
                magnitudes.add(magnitude);
            }
        }
        //Note that Magnitudes are incomplete, as Magnitude.Target is not set
    }

    protected Magnitude magnitudeCopy(int i) {
        Magnitude m = magnitudes.get(i % magnitudes.size());
        return new Magnitude(m, m.getTarget());
    }

    private void initProperMotions() {
        properMotions = new ArrayList<>();
        ProperMotion pm = new ProperMotion();
        pm.setDeltaDec(BigDecimal.valueOf(1.0));
        pm.setDeltaRA(BigDecimal.valueOf(1.0));
        properMotions.add(pm);
    }

    protected ProperMotion properMotionCopy(int i) {
        ProperMotion pm = properMotions.get(i % properMotions.size());
        final ProperMotion pm2 = new ProperMotion();
        pm2.setDeltaDec(pm.getDeltaDec());
        pm2.setDeltaRA(pm.getDeltaRA());
        return pm2;
    }

    private void initEphemerisElements() {
        ephemerisElements = new ArrayList<>();
        Calendar cal = Calendar.getInstance();
        cal.set(2012, 10, 01);
        for (int d = 0; d < degDegCoordinates.size(); d++) {
            DegDegCoordinates degDegCoordinates = degDegCoordinatesCopy(d);
            EphemerisElement e = new EphemerisElement();
            e.setCoordinates(degDegCoordinates);
            e.setValidAt(cal.getTime());
            e.setMagnitude(BigDecimal.valueOf(10.0));

            ephemerisElements.add(e);
        }
        for (int h = 0; h < hmsDmsCoordinates.size(); h++) {
            HmsDmsCoordinates hmsDmsCoordinates = hmsDmsCoordinatesCopy(h);
            EphemerisElement e = new EphemerisElement();
            e.setCoordinates(hmsDmsCoordinates);
            e.setValidAt(cal.getTime());
            e.setMagnitude(BigDecimal.valueOf(10.0));
            ephemerisElements.add(e);
        }
        //Note that ephemerisElements remains incomplete, since elements need Target
    }

    protected EphemerisElement ephemerisElementCopy(int i) {
        EphemerisElement e = ephemerisElements.get(i % ephemerisElements.size());
        final EphemerisElement ephemerisElement = new EphemerisElement();
        ephemerisElement.setCoordinates(e.getCoordinates());
        ephemerisElement.setMagnitude(e.getMagnitude());
        ephemerisElement.setValidAt(e.getValidAt());
        ephemerisElement.setTarget(e.getTarget());
        return ephemerisElement;
    }

    protected ObservationMetaData observationMetaDataCopy(int i) {
        return new ObservationMetaData(observationMetaDatas.get(i % observationMetaDatas.size()));
    }

    private void initObservationMetaData() {
        observationMetaDatas = new ArrayList<>();
        guidingEstimations = new ArrayList<>();

        int i = 1;
        for (GuidingEvaluation ge : GuidingEvaluation.values()) {
            guidingEstimations.add(new ObservationMetaData.GuidingEstimation(i++ * 20, ge));
        }

        for (TargetVisibility tv : TargetVisibility.values()) {
            for (ObservationMetaData.GuidingEstimation ge : guidingEstimations) {
                observationMetaDatas.add(new ObservationMetaData(ge, tv, new BigInteger("5"), "0123456789"));
            }
        }
    }

    private void initNonsiderealTargets() {
        nonSiderealTargets = new ArrayList<>();
        int count = 0;

        final Calendar calendar = Calendar.getInstance();
        final BigDecimal brightness = new BigDecimal(10.0);
        final Coordinates coordinates = new DegDegCoordinates(new Angle(1.23, Angle.Unit.DEGREES), new Angle(4.56, Angle.Unit.DEGREES));
        calendar.set(2012, 1, 1);
        EphemerisElement startOfSemester = makeEphemerisElement(calendar.getTime(), coordinates, brightness);

        calendar.set(2013, 1, 1);
        EphemerisElement endOfSemester = makeEphemerisElement(calendar.getTime(), coordinates, brightness);

        List<EphemerisElement> ees = new ArrayList<>();
        ees.add(startOfSemester);
        ees.add(endOfSemester);

        NonsiderealTarget nt = new NonsiderealTarget();
        nt.setEpoch(CoordinatesEpoch.J_2000);
        nt.setEphemeris(ees);
        nt.setTargetId("target-1" + count);
        nt.setName("TargetName_" + count);

        startOfSemester.setTarget(nt);
        endOfSemester.setTarget(nt);
        nonSiderealTargets.add(nt);
    }

    private EphemerisElement makeEphemerisElement(Date validAt, Coordinates c, BigDecimal brightness) {
        EphemerisElement ee = new EphemerisElement();
        ee.setMagnitude(brightness);
        ee.setValidAt(validAt);
        ee.setCoordinates(c);
        return ee;
    }

    protected NonsiderealTarget nonSiderealTargetCopy(int i) {
        NonsiderealTarget nt = nonSiderealTargets.get(i % nonSiderealTargets.size());
        final NonsiderealTarget nt2 = new NonsiderealTarget();
        List<EphemerisElement> newEes = new ArrayList<>();
        for (EphemerisElement ee : nt.getEphemeris()) {
            EphemerisElement newEE = new EphemerisElement(ee, nt2);
            newEes.add(newEE);
        }
        nt2.setEphemeris(newEes);

        nt2.setEpoch(nt.getEpoch());
        nt2.setTargetId(nt.getTargetId() + i);
        nt2.setName(nt.getName() + "_" + i);
        nt2.setPhaseIProposal(nt.getPhaseIProposal());
        return nt2;
    }

    private void initSiderealTargets() {
        siderealTargets = new ArrayList<>();
        int count = 0;
        for (CoordinatesEpoch ce : CoordinatesEpoch.values()) {
            for (int i = 0; i < magnitudes.size(); i++) {
                Magnitude m = magnitudeCopy(i);
                Set<Magnitude> ms = new HashSet<>();
                ms.add(m);
                for (int p = 0; p < properMotions.size(); p++) {
                    ProperMotion pm = properMotionCopy(p);
                    SiderealTarget st = new SiderealTarget();
                    st.setName("SiderealTargetName_" + count);
                    st.setTargetId("target-" + count);
                    st.setEpoch(ce);
                    st.setMagnitudes(ms);
                    st.setProperMotion(pm);
                    final DegDegCoordinates coordinates = new DegDegCoordinates();
                    coordinates.setRa(BigDecimal.valueOf(1.2));
                    coordinates.setDec(BigDecimal.valueOf(3.4));
                    st.setCoordinates(coordinates);
                    siderealTargets.add(st);
                    count++;
                }
            }
        }
        //Not complete, as Target is not set
    }

    protected SiderealTarget siderealTargetCopy(int i) {
        SiderealTarget st = siderealTargets.get(i % siderealTargets.size());
        return new SiderealTarget(st, null, st.getTargetId());
    }

    private void initGuidestars() {
        guideStars = new ArrayList<>();
        for (Guider guider : Guider.values()) {
            GuideStar gs = new GuideStar();
            gs.setGuider(guider);
            guideStars.add(gs);
        }
    }

    protected GuideStar guideStarCopy(int i) {
        GuideStar gs = guideStars.get(i % guideStars.size());
        GuideStar gs2 = new GuideStar();
        gs2.setGuider(gs.getGuider());
        gs2.setObservation(gs.getObservation());
        gs2.setTarget(gs.getTarget());
        return gs2;
    }

    private BlueprintBase blueprintCopy(int i) {
        BlueprintBase bp = blueprintBases.get(i % blueprintBases.size());
        BlueprintBase bp2;
        if (bp instanceof NiriBlueprint) {
            bp2 = new NiriBlueprint((edu.gemini.model.p1.mutable.NiriBlueprint) bp.toMutable().getBlueprintBase());
        } else if (bp instanceof GnirsBlueprintImaging) {
            bp2 = new GnirsBlueprintImaging((edu.gemini.model.p1.mutable.GnirsBlueprintImaging) bp.toMutable().getBlueprintBase());
        } else if (bp instanceof GnirsBlueprintSpectroscopy) {
            bp2 = new GnirsBlueprintSpectroscopy((edu.gemini.model.p1.mutable.GnirsBlueprintSpectroscopy) bp.toMutable().getBlueprintBase());
        } else {
            throw new RuntimeException("unknown blueprint base");
        }
        return bp2;
    }

    private void initBlueprints() {
        blueprintBases = new ArrayList<>();

        int count = 0;
        for (NiriCamera r : NiriCamera.values()) {
            List<NiriFilter> filters = new LinkedList<>();
            filters.add(NiriFilter.BBF_H);
            NiriBlueprint niriBlueprintImaging = new NiriBlueprint();
            niriBlueprintImaging.setCamera(r);
            niriBlueprintImaging.setFilters(filters);
            niriBlueprintImaging.setBlueprintId("blueprint-1" + count);
            niriBlueprintImaging.setName("NiriBlueprintImagingName_" + count);
            niriBlueprintImaging.setInstrument(Instrument.NIRI);
            blueprintBases.add(niriBlueprintImaging);
            count++;
        }

        count = 0;
        for (GnirsPixelScale ps : GnirsPixelScale.values()) {
            for (AltairConfiguration ac : AltairConfiguration.values()) {
                for (GnirsFilter f : GnirsFilter.values()) {
                    final GnirsBlueprintImaging blueprint = new GnirsBlueprintImaging();
                    blueprint.setPixelScale(ps);
                    blueprint.setAltairConfiguration(ac);
                    blueprint.setFilter(f);
                    blueprint.setBlueprintId("blueprint-2" + count);
                    blueprint.setName("GnirsBlueprintImaging_" + count);
                    blueprint.setInstrument(Instrument.GNIRS);
                    blueprintBases.add(blueprint);
                    count++;
                }

                // Not taking all iterations of this... for better or worse and for the time being.
                final GnirsBlueprintSpectroscopy blueprint = new GnirsBlueprintSpectroscopy();
                blueprint.setPixelScale(ps);
                blueprint.setAltairConfiguration(ac);
                blueprint.setCrossDisperser(GnirsCrossDisperser.values()[0]);
                blueprint.setDisperser(GnirsDisperser.values()[0]);
                blueprint.setFpu(GnirsFpu.values()[0]);
                blueprint.setBlueprintId("blueprint-3" + count);
                blueprint.setName("GnirsBlueprintSpectroscopy_" + count);
                blueprint.setInstrument(Instrument.GNIRS);
                blueprintBases.add(blueprint);
                count++;
            }
        }
    }

    private void initItacAccepts() {
        Validate.isTrue(times != null);
        itacAccepts = new ArrayList<>();
        int count = 0;
        int subVariants = times.size();
        for (int i = 0; i < subVariants; i++) {
            TimeAmount timeAmount = timeAmountCopy(i);
            for (int band = 1; band <= 4; band++) {
                String contact = "Contact_" + count;
                String email = "Contact_" + count + "@null.com";
                String programId = "GN-2000A-Q-" + count;
                ItacAccept ia = new ItacAccept(timeAmount, band, true, contact, email, programId);
                itacAccepts.add(ia);
                count++;
                contact = "Contact_" + count;
                email = "Contact_" + count + "@null.com";
                programId = "GN-2000A-Q-" + count;
                itacAccepts.add(new ItacAccept(timeAmount, band, false, contact, email, programId));
                count++;
            }
        }
    }

    protected ItacAccept itacAcceptCopy(int i) {
        ItacAccept ia = itacAccepts.get(i % itacAccepts.size());
        return new ItacAccept(ia.getAward(), ia.getBand(), ia.isRollover(), ia.getContact() + "_" + i, i + "_" + ia.getEmail(), ia.getProgramId());
    }

    private void initSubmissionAccepts() {
        Validate.isTrue(times != null);
        submissionAccepts = new ArrayList<>();
        int count = 0;
        int subVariants = times.size();
        for (int t = 0; t < subVariants; t++) {
            TimeAmount minRecommend = timeAmountCopy(t);
            for (int r = 0; r < subVariants; r++) {
                TimeAmount recommend = timeAmountCopy(r);
                String email = "SubmissionAccept_" + count + "@null.com";
                submissionAccepts.add(new edu.gemini.tac.persistence.phase1.submission.SubmissionAccept(recommend, minRecommend, email, true, new BigDecimal(1)));
                count++;
                email = "SubmissionAccept_" + count + "@null.com";
                submissionAccepts.add(new edu.gemini.tac.persistence.phase1.submission.SubmissionAccept(recommend, minRecommend, email, false, new BigDecimal(1)));
                count++;
            }
        }
    }

    protected edu.gemini.tac.persistence.phase1.submission.SubmissionAccept submissionAcceptCopy(int i) {
        edu.gemini.tac.persistence.phase1.submission.SubmissionAccept sa = submissionAccepts.get(i % submissionAccepts.size());
        return new edu.gemini.tac.persistence.phase1.submission.SubmissionAccept(sa);
    }

    private static int receiptId = 0;

    private void initSubmissionReceipts() {
        submissionReceipts = new ArrayList<>();
        edu.gemini.tac.persistence.phase1.submission.SubmissionReceipt sr = new edu.gemini.tac.persistence.phase1.submission.SubmissionReceipt();
        sr.setReceiptId("Receipt_" + receiptId++);
        final Calendar calendar = Calendar.getInstance();
        calendar.set(2012, 1, 1);
        sr.setTimestamp(calendar.getTime());
        submissionReceipts.add(sr);
    }

    protected edu.gemini.tac.persistence.phase1.submission.SubmissionReceipt submissionReceiptCopy(int i) {
        edu.gemini.tac.persistence.phase1.submission.SubmissionReceipt sr = submissionReceipts.get(i % submissionReceipts.size());
        edu.gemini.tac.persistence.phase1.submission.SubmissionReceipt dup = new edu.gemini.tac.persistence.phase1.submission.SubmissionReceipt(sr);
        dup.setReceiptId("Receipt_" + receiptId++);
        return dup;
    }

    private void initSubmissionRequests() {
        submissionRequests = new ArrayList<>();
        for (int t = 0; t < times.size(); t++) {
            TimeAmount time = timeAmountCopy(t);
            for (int mt = 0; mt < times.size(); mt++) {
                TimeAmount minTime = timeAmountCopy(mt);
                edu.gemini.tac.persistence.phase1.submission.SubmissionRequest sr = new edu.gemini.tac.persistence.phase1.submission.SubmissionRequest(time, minTime);
                submissionRequests.add(sr);
            }
        }
    }

    protected edu.gemini.tac.persistence.phase1.submission.SubmissionRequest submissioNRequestCopy(int i) {
        edu.gemini.tac.persistence.phase1.submission.SubmissionRequest sr = submissionRequests.get(i % submissionRequests.size());
        return new edu.gemini.tac.persistence.phase1.submission.SubmissionRequest(sr);
    }

    private void initCoInvestigators() {
        for (int i = 0; i < 4; i++) {
            coInvestigators = new ArrayList<>();
            Set<String> pNumbers = new HashSet<>();
            pNumbers.add("808-555-1212");
            pNumbers.add("808-555-2121");
            for (InvestigatorStatus is : InvestigatorStatus.values()) {
                coInvestigators.add(new CoInvestigator("First_" + i, "Last_" + i, InvestigatorGender.NONE_SELECTED, is, i + "-" + is.value() + "@null.com", pNumbers, "Institution"));
            }
        }
    }

    protected CoInvestigator coInvestigator(int i) {
        return coInvestigators.get(i % coInvestigators.size());
//        return new CoInvestigator(ci.getFirstName() + "_" + i, ci.getLastName() + "_" + i, ci.getStatus(), ci.getEmail(), ci.getPhoneNumbers(), ci.getInstitution());
    }


    private void initInstitutionAddresses() {
        institutionAddresses = new ArrayList<>();
        final InstitutionAddress ia = new InstitutionAddress("Prestigious", "Address", "Country");
        institutionAddresses.add(ia);
    }

    protected InstitutionAddress institutionAddressCopy(int i) {
        InstitutionAddress ia = institutionAddresses.get(i % institutionAddresses.size());
        return new InstitutionAddress(ia.getInstitution() + "_" + i, ia.getAddress(), ia.getCountry());
    }

    private void initPrincipalInvestigators() {
        principalInvestigators = new ArrayList<>();
        int count = 0;
        Set<String> pNumbers = new HashSet<>();
        pNumbers.add("808-555-1212");
        pNumbers.add("808-555-2121");

        for (int j = 0; j < institutionAddresses.size(); j++) {
            InstitutionAddress ia = institutionAddressCopy(j);
            for (InvestigatorStatus is : InvestigatorStatus.values()) {
                final PrincipalInvestigator principalInvestigator = new PrincipalInvestigator("First_" + count, "Last_" + count, InvestigatorGender.NONE_SELECTED, is, count + "@null.com", pNumbers, ia);
                principalInvestigators.add(principalInvestigator);
                count++;
            }
        }
    }

    protected PrincipalInvestigator principalInvestigator(int i) {
        PrincipalInvestigator pi = principalInvestigators.get(i % principalInvestigators.size());
        return pi;
    }

    private void initConditions() {
        conditions = new ArrayList<>();
        CloudCover[] cloudCovers = CloudCover.values();
        ImageQuality[] imageQualities = ImageQuality.values();
        SkyBackground[] skyBackgrounds = SkyBackground.values();
        WaterVapor[] waterVapors = WaterVapor.values();
        int count = 1;
        for (CloudCover cc : CloudCover.values()) {
            for (ImageQuality iq : ImageQuality.values()) {
                for (SkyBackground sb : SkyBackground.values()) {
                    for (WaterVapor wv : WaterVapor.values()) {
                        String name = "Condition_" + count;
                        BigDecimal maxAirmass = new BigDecimal(1.0);
                        Condition condition = new Condition();
                        condition.setMaxAirmass(maxAirmass);
                        condition.setName(name);
                        condition.setConditionId("condition-" + count);
                        condition.setWaterVapor(wv);
                        condition.setSkyBackground(sb);
                        condition.setImageQuality(iq);
                        condition.setCloudCover(cc);
                        //Note that this is still incomplete, as Condition.phaseIProposal == null
                        conditions.add(condition);
                        count++;
                    }
                }
            }
        }
    }

    protected Condition conditionCopy(int i) {
        Condition c = conditions.get(i % conditions.size());
        return new Condition(c);
    }

    private void initExchangeSubmissions() {
        exchangeSubmissions = new ArrayList<>();
        int count = 0;
        for (ExchangePartner exchangePartner : ExchangePartner.values()) {
            Partner partner = null;
            for (Partner p : partners) {
                if (p.getPartnerCountryKey().toUpperCase().equals(exchangePartner.name().toUpperCase()))
                    partner = p;
            }
            Validate.notNull(partner);
            Validate.isTrue(partner.isExchange(), "Partner " + partner.getName() + " is not an Exchange partner");
            for (int s = 0; s < submissionRequests.size(); s++) {
                edu.gemini.tac.persistence.phase1.submission.SubmissionRequest sr = submissioNRequestCopy(s);
                for (int r = 0; r < submissionReceipts.size(); r++) {
                    edu.gemini.tac.persistence.phase1.submission.SubmissionReceipt submissionReceipt = submissionReceiptCopy(r);
                    for (int a = 0; a < submissionAccepts.size(); a++) {
                        edu.gemini.tac.persistence.phase1.submission.SubmissionAccept sa = submissionAcceptCopy(a);
                        ExchangeSubmission es = new ExchangeSubmission(partner, sr, submissionReceipt, sa, true);
                        exchangeSubmissions.add(es);
                        count++;
                        ExchangeSubmission es2 = new ExchangeSubmission(partner, submissioNRequestCopy(count), submissionReceiptCopy(count), submissionAcceptCopy(count), false);
                        exchangeSubmissions.add(es2);
                        count++;
                    }
                }
            }
        }
    }

    private Partner getLargeProgramPartner() {
        Partner largeProgramPartner = null;
        for (Partner p:partners) {
            if (p.isLargeProgram()) {
                largeProgramPartner = p;
            }
        }
        return largeProgramPartner;
    }

    private void initLargeProgramSubmissions() {
        largeProgramSubmissions = new ArrayList<>();

        edu.gemini.tac.persistence.phase1.submission.SubmissionReceipt submissionReceipt = submissionReceiptCopy(0);
        edu.gemini.tac.persistence.phase1.submission.SubmissionRequest sr = submissioNRequestCopy(0);
        edu.gemini.tac.persistence.phase1.submission.SubmissionAccept sa = submissionAcceptCopy(0);
        LargeProgramSubmission lps = new edu.gemini.tac.persistence.phase1.submission.LargeProgramSubmission(getLargeProgramPartner(), sr, submissionReceipt, sa, false);

        lps.setReceipt(submissionReceipt);
        lps.setRequest(sr);
        lps.setAccept(sa);

        largeProgramSubmissions.add(lps);
    }

    protected ExchangeSubmission exchangeSubmissionCopy(int i) {
        ExchangeSubmission es = exchangeSubmissions.get(i % exchangeSubmissions.size());
        final ExchangeSubmission e2 = new ExchangeSubmission();
        e2.setAccept(es.getAccept());
        e2.setPartner(es.getPartner());
        e2.setReceipt(es.getReceipt());
        e2.setRequest(es.getRequest());
        return e2;
    }

    private void initNgoSubmissions() {
        List<NgoSubmission> tempSubmissions = new ArrayList<>();
        ngoSubmissions = new ArrayList<>();
        int count = 0;
        for (Partner partner : partners) {
            if (partner.isNgo()) {
                for (int submissionRequest = 0; submissionRequest < submissionRequests.size(); submissionRequest++) {
                    edu.gemini.tac.persistence.phase1.submission.SubmissionRequest sr = submissioNRequestCopy(submissionRequest);
                    for (int r = 0; r < submissionReceipts.size(); r++) {
                        edu.gemini.tac.persistence.phase1.submission.SubmissionReceipt submissionReceipt = submissionReceiptCopy(r);
                        for (int a = 0; a < submissionAccepts.size(); a++) {
                            edu.gemini.tac.persistence.phase1.submission.SubmissionAccept sa = submissionAcceptCopy(a);
                            NgoSubmission ns = new NgoSubmission(partner, sr, submissionReceipt, sa, false);
                            tempSubmissions.add(ns);
                            count++;
                            NgoSubmission ns2 = new NgoSubmission(partner, submissioNRequestCopy(count), submissionReceiptCopy(count), submissionAcceptCopy(count), false);
                            tempSubmissions.add(ns2);
                            count++;
                        }
                    }
                }
            }
        }

        final int partnersSize = partners.size();
        final int submissionsPerPartner = tempSubmissions.size() / partnersSize;
        for (int i = 0; i < submissionsPerPartner; i++) {
            for (int j = 0; j < partnersSize; j++) {
                final NgoSubmission ngoSubmission = tempSubmissions.get((j * submissionsPerPartner) + i);
                ngoSubmissions.add(ngoSubmission);
            }
        }
    }

    protected NgoSubmission ngoSubmissionCopy(int i) {
        NgoSubmission ns = ngoSubmissions.get(i % ngoSubmissions.size());
        return new NgoSubmission(ns);
    }

    private void initItacs() {
        itacs = new ArrayList<>();
        int count = 0;
        for (int i = 0; i < itacAccepts.size(); i++) {
            Itac itac = new Itac(null, true, "Comment_" + count, null);
            itacs.add(itac);
            count++;
            itacs.add(new Itac(itacAcceptCopy(i), false, "Comment_" + count, null));
            count++;
        }
    }

    protected Itac itacCopy(int i) {
        Itac itac = itacs.get(i % itacs.size());
        ItacAccept ia = itacAcceptCopy(i);
        return new Itac(ia, itac.getRejected(), itac.getComment(), itac.getNgoAuthority());
    }

    private void initMetas() {
        metas = new ArrayList<>();
        final Meta meta = new Meta();
        meta.setAttachment("Attachment_0");
        metas.add(meta);
    }

    protected Meta metaCopy(int i) {
        Meta m = metas.get(i % metas.size());
        return new Meta(m);
    }

    private void initInvestigatorTeams() {
        investigatorTeams = new ArrayList<>();
        for (int p = 0; p < principalInvestigators.size(); p++) {
            PrincipalInvestigator pi = principalInvestigator(p);
            for (int c = 0; c < coInvestigators.size(); c++) {
                CoInvestigator coi = coInvestigator(c);
                Investigators is = new Investigators();
                is.setPi(pi);
                is.getCoi().add(coi);
                investigatorTeams.add(is);
            }
        }
    }

    protected Investigators investigatorTeamCopy(int i) {
        Investigators is = investigatorTeams.get(i % investigatorTeams.size());
        return new Investigators(is);
    }

    private void initObservations(int n) {
        observations = new ArrayList<>();

        for (int i = 0; i < n; i++) {
            Observation observation = new Observation();

            Condition condition = conditionCopy(i);
            TimeAmount timeAmount = timeAmountCopy(i);
            ObservationMetaData observationMetaData = observationMetaDataCopy(i);
            GuideStar guideStar = guideStarCopy(i);
            //Guidestar MUST have reference back to Observation
            guideStar.setObservation(observation);

            Set<GuideStar> gss = new HashSet<>();
            BlueprintBase bb = blueprintCopy(i);
            gss.add(guideStar);
            Target t = siderealTargetCopy(i);
            if (i % 2 == 0) {
                t = nonSiderealTargetCopy(i);
            }
            observation.setCondition(condition);
            observation.setBlueprint(bb);
            observation.setGuideStars(gss);
            observation.setMetaData(observationMetaData);
            observation.setProgTime(timeAmount);
            observation.setPartTime(timeAmount);
            observation.setTime(timeAmount);
            observation.setTarget(t);
            if (i % 4 == 0)
                observation.setBand(Band.BAND_3);
            else
                observation.setBand(Band.BAND_1_2);

            observations.add(observation);
        }

        /* Combinatorial explosion creates >300,000 Observations

        int count = 0;
        for(int c = 0; c < conditions.size(); c++){
            Condition condition = conditionCopy(c);
            for(BlueprintBase bb : blueprintBases){
                for(int t = 0; t < times.size(); t++){
                    TimeAmount timeAmount =timeAmountCopy(t);
                    for(int s = 0; s < successEstimations.size(); s++){
                        SuccessEstimation successEstimation = successEstimationCopy(s);
                        for(int g = 0; g < guideStars.size(); g++){
                            GuideStar guideStar = guideStarCopy(g);
                            Set<GuideStar> gss = new HashSet<GuideStar>();
                            gss.add(guideStar);
                            for(int nst = 0; nst < nonSiderealTargets.size(); nst++){
                                NonsiderealTarget nonsiderealTarget = nonSiderealTargetCopy(nst);
                                Observation observation = new Observation();
                                observation.setCondition(condition);
                                observation.setBlueprint(bb);
                                observation.setGuideStars(gss);
                                observation.setSuccess(successEstimation);
                                observation.setTime(timeAmount);
                                observation.setTarget(nonsiderealTarget);
                                observations.add(observation);
                                count++;
                            }
                            if(count % 100 == 0){
                                System.out.println(count);
                            }
                            for(int st = 0; st < siderealTargets.size(); st++){
                                SiderealTarget siderealTarget = siderealTargetCopy(st);
                                Observation observation = new Observation();
                                observation.setCondition(condition);
                                observation.setBlueprint(bb);
                                observation.setGuideStars(gss);
                                observation.setSuccess(successEstimation);
                                observation.setTime(timeAmount);
                                observation.setTarget(siderealTarget);
                                observations.add(observation);
                                count++;
                            }
                            if(count % 100 == 0){
                                System.out.println(count);
                            }
                        }
                    }
                }
            }
        }
        */
    }

    private static int tooTargetCnt = 1;

    protected Observation observationCopy(int i, PhaseIProposal p) {
        Observation o = observations.get(i % observations.size());
        Observation o2 = new Observation();
        o2.setTarget(o.getTarget());
        o2.setProgTime(o.getProgTime());
        o2.setPartTime(o.getPartTime());
        o2.setTime(o.getTime());
        o2.setBlueprint(o.getBlueprint());
        o2.setCondition(o.getCondition());
        o2.setGuideStars(o.getGuideStars());
        o2.setProposal(o.getProposal());
        o2.setMetaData(o.getMetaData());
        o2.setBand(o.getBand());
        if (p != null && p.isToo()) {
            // too proposals should have TooTargets
            // replace the target that was assigned previously with a too target
            TooTarget tooTarget = new TooTarget();
            tooTarget.setName("Too Target");
            tooTarget.setTargetId("target-0" + tooTargetCnt++);
            tooTarget.setEpoch(CoordinatesEpoch.J_2000);
            o2.setMetaData(null);
            o2.setTarget(tooTarget);
        }
        return o2;
    }

    private static final int fClassical = 3;
    private static final int fExchange = 3;
    private static final int fLP = 3;
    private static final int fQueue = 91;

    private void initPhaseIProposals(int count) {
        phaseIProposals = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            final int percentile = i % 100;

            Meta meta = metaCopy(i);
            Set<Keyword> ks = new HashSet<>();
            ks.add(Keyword.values()[i % Keyword.values().length]);
            Investigators investigators = investigatorTeamCopy(i);
            Condition c = conditionCopy(i);
            List<Condition> conditions = new ArrayList<>();
            conditions.add(c);
            BlueprintBase bb = blueprintCopy(i);
            List<BlueprintBase> bbs = new ArrayList<>();
            bbs.add(bb);

            PhaseIProposal p = null;

            if (percentile < fClassical) {
                final ClassicalProposal classicalProposal = new ClassicalProposal();
                p = classicalProposal;
                if (i % 2 == 0)
                    classicalProposal.getClassicalVisitors().add(investigators.getPi());
                else {
                    if (investigators.getCoi().size() > 0) {
                        final Object[] cois = investigators.getCoi().toArray();
                        classicalProposal.getClassicalVisitors().add((Investigator) cois[i % cois.length]);
                    }
                }
                classicalProposal.getNgos().add(ngoSubmissionCopy(i));
                classicalProposals.add(classicalProposal);
            } else if (percentile < fClassical + fExchange) {
                final ExchangeProposal exchangeProposal = new ExchangeProposal();
                p = exchangeProposal;
                exchangeProposal.setPartner(ExchangePartner.values()[i % ExchangePartner.values().length]);
                exchangeProposal.getNgos().add(ngoSubmissionCopy(i));
                exchangeProposals.add(exchangeProposal);
            } else if (percentile < fClassical + fExchange + fQueue) {
                final QueueProposal queueProposal = new QueueProposal();
                p = queueProposal;

                queueProposal.setBand3Request(submissioNRequestCopy(i));
                queueProposal.setTooOption(TooOption.values()[i % TooOption.values().length]);
                if (i % 10 == 0) {
                    queueProposal.setExchange(exchangeSubmissionCopy(i));
                } else {
                    queueProposal.getNgos().add(ngoSubmissionCopy(i));
                }
                queueProposals.add(queueProposal);
            } else if (percentile < fClassical + fExchange + fQueue + fLP) {
                final LargeProgram largeProposal = new LargeProgram(TooOption.values()[i % TooOption.values().length]);
                largeProposal.setPrimarySubmission(largeProgramSubmissions.get(0));
                p = largeProposal;

                largePrograms.add(largeProposal);
            }

            Observation o = observationCopy(i, p);
            Set<Observation> os = new HashSet<>();
            os.add(o);
            List<Target> targets = new ArrayList<>();
            targets.add(o.getTarget());

            p.setInvestigators(investigators);
            for (Submission sub : p.getSubmissions()) {
                if (sub instanceof NgoSubmission) {
                    ((NgoSubmission) sub).setPartnerLead(p.getInvestigators().getPi());
                } else if (sub instanceof edu.gemini.tac.persistence.phase1.submission.PartnerSubmission) {
                    ((edu.gemini.tac.persistence.phase1.submission.PartnerSubmission) sub).setPartnerLead(p.getInvestigators().getPi());
                }
            }
            if (p instanceof GeminiNormalProposal && ((GeminiNormalProposal) p).getExchange() != null) {
                ((GeminiNormalProposal) p).getExchange().setPartnerLead(p.getInvestigators().getPi());
            }

            p.setSubmissionsKey(PhaseIProposal.generateNewSubmissionsKey());
            p.setMeta(meta);
            p.setKeywords(ks);
            p.setTargets(targets);
            for (Target t : targets) {
                t.setPhaseIProposal(p);
            }
            p.setConditions(conditions);
            for (Condition aCondition : conditions) {
                aCondition.setPhaseIProposal(p);
            }
            p.setBlueprints(bbs);
            for (BlueprintBase blueprintBase : bbs) {
                bb.setPhaseIProposal(p);
            }
            p.setObservations(os);
            p.setTacCategory(TacCategory.values()[i % TacCategory.values().length]);
            p.setTitle("PhaseIProposal_" + i);
            p.setProposalAbstract("Abstract_" + i);
            p.setSchemaVersion("1.0.0");

            phaseIProposals.add(p.memberClone());
        }
        /* Combinatorial explosion
        int count = 0;
        for (int m = 0; m < metas.size(); m++) {
            Meta meta = metaCopy(m);
            for (Keyword keyword : Keyword.values()) {
                Set<Keyword> ks = new HashSet<Keyword>();
                ks.add(keyword);
                for (int s = 0; s < schedulings.size(); s++) {
                    Scheduling scheduling = schedulingCopy(s);
                    for (int i = 0; i < investigatorTeams.size(); i++) {
                        Investigators investigators = investigatorTeamCopy(i);
                        List<Target> targets = new ArrayList<Target>(nonSiderealTargets.size() + siderealTargets.size());
                        targets.addAll(nonSiderealTargets);
                        targets.addAll(siderealTargets);
                        for (int t = 0; t < targets.size(); t++) {
                            Target target = siderealTargetCopy(t);
                            if (targets.get(t) instanceof NonsiderealTarget) {
                                target = nonSiderealTargetCopy(t);
                            }
                            List<Target> ts = new ArrayList<Target>();
                            ts.add(target);
                            for (int c = 0; c < conditions.size(); c++) {
                                Condition condition = conditionCopy(c);
                                List<Condition> cs = new ArrayList<Condition>();
                                cs.add(condition);
                                for (BlueprintBase bb : blueprintBases) {
                                    List<BlueprintBase> bbs = new ArrayList<BlueprintBase>();
                                    bbs.add(bb);
                                    for (int o = 0; o < observations.size(); o++) {
                                        Observation observation = observationCopy(o);
                                        Set<Observation> os = new HashSet<Observation>();
                                        os.add(observation);
                                        for (int b3 = 0; b3 < band3s.size(); b3++) {
                                            Band3 band3 = band3Copy(b3);
                                            for (int su = 0; su < submissions.size(); su++) {
                                                Submissions submissions = submissionsCopy(su);
                                                for (ProposalType proposalType : ProposalType.values()) {
                                                    String title = "PhaseIProposal_" + count;
                                                    PhaseIProposal p = new PhaseIProposal();
                                                    p.setMeta(meta);
                                                    p.setKeywords(ks);
                                                    p.setScheduling(scheduling);
                                                    p.setInvestigators(investigators);
                                                    p.setTargets(ts);
                                                    p.setConditions(cs);
                                                    for (Condition aCondition : cs) {
                                                        aCondition.setPhaseIProposal(p);
                                                    }
                                                    p.setBlueprints(bbs);
                                                    p.setObservations(os);
                                                    p.setBand3(band3);
                                                    p.setSubmissions(submissions);
                                                    p.setType(proposalType);
                                                    p.setTitle(title);
                                                    p.setProposalAbstract("Abstract_" + count);

                                                    phaseIProposals.add(p);
                                                    count++;
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        */
    }

    private void initCommittees() {
        committees = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            Committee c = new Committee();
            c.setName("Test committee " + i);
            c.setActive(Boolean.TRUE);
            c.setSemester(getSemester(i));
            committees.add(c);
        }
    }

    protected List listFromDatabase(String query) {
        Session session = sessionFactory.openSession();
        try {
            return session.createQuery(query).list();
        } finally {
            session.close();
        }
    }

    private void initPartners() {
        partners = listFromDatabase("from Partner");
    }

    private Partner partner(int i) {
        Partner p = partners.get(i % partners.size());
        return p;
    }

    private void initAuthorityRoles() {
        authorityRoles = listFromDatabase("from AuthorityRole");
    }

    private AuthorityRole authorityRole(int i) {
        return authorityRoles.get(i % authorityRoles.size());
    }

    protected void initConditionBuckets() {
        conditionBuckets = new ArrayList<>();
        ConditionBucket bucket = new ConditionBucket();
        bucket.setName("Bucket-0");
        bucket.setAvailablePercentage(100);
        bucket.setSkyBackground(SkyBackground.SB_100);
        bucket.setWaterVapor(WaterVapor.WV_100);
        bucket.setCloudCover(CloudCover.CC_100);
        bucket.setImageQuality(ImageQuality.IQ_100);
        conditionBuckets.add(bucket);
    }

    private ConditionBucket getConditionBucket(int i) {
        return conditionBuckets.get(i % conditionBuckets.size());
    }

    protected void initConditionSets() {
        conditionSets = new ArrayList<>();
        ConditionSet cs = new ConditionSet();
        cs.setName("CS-0");
        Set<ConditionBucket> constraints = new HashSet<>();
        getConditionBucket(0);
        cs.setConditions(constraints);
    }

    protected void initPartnerSequences() {
        partnerSequences = new ArrayList<>();
        PartnerSequence ps = new PartnerSequence();
        ps.setName("name");

        ps.setCsv("US,CA,UH,US,AU,US,BR,US,US,CA,US,GS,US,AR,US,CA,US,UH,US,US,CA,US,AU,US,BR,US,CA,US,UH,US,US,CA,US,US,GS,US,UH,US,CA,US,AU,US,BR,US,CA,US,AR,US,US,UH,US,CA,US,US,AU,US,CA,US,BR,US,UH,US,CA,US,US,GS,US,CA,US,UH,US,AR,US,CA,US,US,AU,US,BR,US,CA,US,UH,US,US,CA,US,AU,US,US,CA,US,UH,US,BR,US,CA,US,GS,US");
        ps.setCommittee(getCommittee(0));
        partnerSequences.add(ps);
    }

    protected void initBinConfigurations() {
        binConfigurations = new ArrayList<>();
        BinConfiguration b = new BinConfiguration();
        b.setName("Bin Configuration 1");
        b.setDecBinSize(decBin(0));
        b.setRaBinSize(raBin(0));
        b.setEditable(true);
        b.setSmoothingLength(1);
        binConfigurations.add(b);
    }

    private BinConfiguration getBinConfiguration(int i) {
        return binConfigurations.get(i % binConfigurations.size());
    }

    protected void initShutdowns() {
        shutdowns = new ArrayList<>();
        DateTime start = new DateTime(2012, 3, 26, 12, 0, 0, 0);
        DateTime stop = start.plus(Period.days(1));
        DateRange dr = new DateRange(start.toDate(), stop.toDate());
        DateRangePersister drp = new DateRangePersister(dr);
        Shutdown s = new Shutdown(drp, sites.get(0), getCommittee(0));
        shutdowns.add(s);
    }

    private Shutdown getShutdown(int i) {
        return shutdowns.get(i % shutdowns.size());
    }

    private AuthorityRole roleForName(String name) {
        for (AuthorityRole role : authorityRoles) {
            if (role.getRolename().equalsIgnoreCase(name)) {
                return role;
            }
        }
        throw new IllegalArgumentException();
    }

    private void initPeople() {
        people = new ArrayList<>();

        final AuthorityRole userRole = roleForName("ROLE_USER_ADMIN");
        final AuthorityRole adminRole = roleForName("ROLE_ADMIN");
        final AuthorityRole memberRole = roleForName("ROLE_COMMITTEE_MEMBER");
        final AuthorityRole writerRole = roleForName("ROLE_QUEUE_WRITER");

        int count = 0;

        Person p = new Person();
        p.setEnabled(true);
        p.setName("Sandy");
        p.setPassword("password");
        p.setPartner(partners.get(0));
        p.getAuthorities().add(userRole);
        p.getCommittees().add(committees.get(0));
        p.getCommittees().add(committees.get(1));
        people.add(p);

        p = new Person();
        p.setEnabled(true);
        p.setName("Rosemary");
        p.setPassword("password");
        p.setPartner(partners.get(0));
        p.getAuthorities().add(userRole);
        p.getCommittees().add(committees.get(0));
        p.getCommittees().add(committees.get(2));
        people.add(p);

        p = new Person();
        p.setEnabled(true);
        p.setName("Bryan");
        p.setPassword("password");
        p.setPartner(partners.get(0));
        p.getAuthorities().add(userRole);
        p.getCommittees().add(committees.get(0));
        p.getCommittees().add(committees.get(3));
        people.add(p);

        p = new Person();
        p.setEnabled(true);
        p.setName("Andy");
        p.setPassword("password");
        p.setPartner(partners.get(0));
        p.getAuthorities().add(userRole);
        p.getCommittees().add(committees.get(0));
        people.add(p);

        p = new Person();
        p.setEnabled(true);
        p.setName("Shane");
        p.setPassword("password");
        p.setPartner(partners.get(0));
        p.getAuthorities().add(userRole);
        p.getCommittees().add(committees.get(0));
        people.add(p);

        p = new Person();
        p.setEnabled(true);
        p.setName("Devin");
        p.setPassword("password");
        p.setPartner(partners.get(1)); // Change from default fixture
        p.getAuthorities().add(userRole);
        people.add(p);

        p = new Person();
        p.setEnabled(true);
        p.setName("Larry");
        p.setPassword("password");
        p.setPartner(partners.get(0));
        p.getAuthorities().add(userRole);
        p.getCommittees().add(committees.get(0));
        people.add(p);

        p = new Person();
        p.setEnabled(true);
        p.setName("admin");
        p.setPassword("password");
        p.setPartner(partners.get(0));
        p.getAuthorities().add(userRole);
        p.getAuthorities().add(adminRole);
        p.getCommittees().add(committees.get(0));
        p.getCommittees().add(committees.get(1));
        p.getCommittees().add(committees.get(2));
        p.getCommittees().add(committees.get(3));
        people.add(p);

        p = new Person();
        p.setEnabled(true);
        p.setName("user");
        p.setPassword("password");
        p.setPartner(partners.get(0));
        p.getAuthorities().add(userRole);
        people.add(p);

        p = new Person();
        p.setEnabled(true);
        p.setName("committee_member");
        p.setPassword("password");
        for (Partner partner : partners) {
            if (partner.getAbbreviation().equals("CA"))
                p.setPartner(partner);
        }
        p.getAuthorities().add(userRole);
        p.getAuthorities().add(memberRole);
        p.getCommittees().add(committees.get(2));
        p.getCommittees().add(committees.get(3));
        people.add(p);

        p = new Person();
        p.setEnabled(true);
        p.setName("committee_secretary");
        p.setPassword("password");
        p.setPartner(partners.get(0));
        p.getAuthorities().add(userRole);
        p.getAuthorities().add(writerRole);
        p.getAuthorities().add(memberRole);
        p.getCommittees().add(committees.get(2));
        p.getCommittees().add(committees.get(3));
        people.add(p);
    }

    protected PhaseIProposal phaseIProposalCopy(int i) {
        return phaseIProposals.get(i % phaseIProposals.size());
    }

    private void initProposals(int count) {
        proposals = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            Proposal p = new Proposal();
            p.setCommittee(getCommittee(i));
            p.setPartner(partner(i));
            final PhaseIProposal p1p = phaseIProposalCopy(i);
            p.setPhaseIProposal(p1p);
            p1p.setParent(p);
            //p.setClassical(i % 2 == 0);
            Set<ProposalIssue> iss = new HashSet<>();
            p.setIssues(iss);
            proposals.add(p);

            if (!p.getPartner().getAbbreviation().equals(p.getPhaseIProposal().getPrimary().getPartner().getAbbreviation()))
                p.setPartner(p.getPhaseIProposal().getPrimary().getPartner());
        }
    }

    private void initSemesters() {
        semesters = listFromDatabase("from Semester");
    }

    protected Semester getSemester(int i) {
        return semesters.get(i % semesters.size());
    }


    protected Banding bandingCopy(int i) {
        Banding banding = bandings.get(i % bandings.size());
        Banding copy = new Banding(banding.getQueue(), banding.getProposal(), banding.getBand());
        copy.setMergeIndex(banding.getMergeIndex());
        copy.setProgramId(banding.getProgramId());
        return copy;
    }

    private void initBandings() {
        bandings = new ArrayList<>();
        int count = 0;
        for (Proposal p : proposals) {
            // half of the propsals don't go into the queue
            count++;
            if (count % 2 == 0) {
                continue;
            }
            // the other half is considered successful and banded (unless it's a classical one)
            if (p.isClassical()) {
                // classical proposals must not be banded!
                continue;
            }
            // make sure it's got an ITAC accept
            if (p.getItac().getAccept() == null) {
                p.getItac().setAccept(itacAcceptCopy(count));
                //Override the timeamount
                TimeAmount psar = new TimeAmount(0, TimeUnit.HR);
                for (Submission s : p.getSubmissionsPartnerEntries().values()) {
                    if (s.getAccept() != null) {
                        psar = psar.sum(s.getAccept().getRecommend());
                    }
                }
                p.getItac().getAccept().setAward(psar);
            }

            // queue proposals are added through a banding
            ScienceBand band = ScienceBand.lookupFromRank((count / 2 % 4) + 1); // get Band1, 2, 3, or Poor Weather only!
            Banding b = new Banding(p, band);

            final Itac itac1 = p.getItac();
            final ItacAccept accept = itac1.getAccept();
            final TimeAmount award = accept.getAward();
            b.setMergeIndex(count);
            bandings.add(b);
            // proposals that are used in a queue should have itac part and itac accept part set!
            Itac itac = itacCopy(0);
            itac.setAccept(itacAcceptCopy(0));
            p.getPhaseIProposal().setItac(itac);
        }
    }

    protected Queue queueCopy(int i) {
        Queue q = queues.get(i % queues.size());
        return q;
    }

    private void initQueues() {
        queues = new ArrayList<>();
        Queue q = new Queue("Test Queue 0", committees.get(0));  // Queue on its own is useless, bind it to committee 0
        //Following values from QueueServiceTest -- if you change here, change there
        q.setTotalTimeAvailable(200);
        q.setBand1Cutoff(30);
        q.setBand2Cutoff(60);
        q.setBand3Cutoff(100);
        q.setBand3ConditionsThreshold(60);
        q.setUseBand3AfterThresholdCrossed(false);
        q.setSite(getSiteForName("North"));
        q.setBinConfiguration(getBinConfiguration(0));

        for (Proposal p : proposals) {
            if (p.isClassical() && p.getExchangeFor() == null) {
                Validate.isTrue(p.getPhaseIProposal() instanceof ClassicalProposal);
                q.getClassicalProposals().add(p);
            }
            if (p.getExchangeFor() != null) {
                if (p.getPhaseIProposal() instanceof ExchangeProposal) ;
                q.getExchangeProposals().add(p);
            }
        }
        for (Banding b : bandings) {
            Validate.isTrue(!b.getProposal().isClassical());
            q.addBanding(b);
        }
        queues.add(q);
    }

    private void initRolloverObservations() {
        // this is a bit dirty: use a target from one of the proposals to make sure it (and the proposal it
        // depends on) have been stored, otherwise the rollover will not work as expected
        rolloverObservations = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            Proposal p = getProposal(i);
            RolloverObservation o = new RolloverObservation();
            Observation template = observationCopy(i, null);                    // create new observation
            o.setObservationId("Ref-" + i);
            o.setCondition(new Condition(template.getCondition()));             // create new condition
            o.setObservationTime(template.getTime());
            o.setPartner(partner(i));
            o.setTarget(p.getObservations().iterator().next().getTarget());     // re-use from a stored proposal
            o.setCreatedTimestamp(new Date());
            rolloverObservations.add(o);
        }
    }

    protected RolloverObservation rolloverObservationCopy(int i) {
        final RolloverObservation observation = new RolloverObservation(rolloverObservations.get(i % rolloverObservations.size()));
        rolloverObservations.add(observation);
        return observation;
    }

    private Site getSiteForName(String name) {
        for (Site s : sites) {
            if (s.getDisplayName().equals(name)) {
                return s;
            }
        }
        fail("Could not find site for name " + name);
        throw new RuntimeException();
    }

    private void initRolloverSets() {
        rolloverSets = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            RolloverSet r = new RolloverSet();
            Site s = i % 2 == 0 ? getSiteForName("North") : getSiteForName("South");
            r.setSite(s);
            Set<AbstractRolloverObservation> os = new HashSet<>();
            int j = 0;
            while (os.size() < 10 && j < 100) {
                RolloverObservation o = rolloverObservationCopy(j);
                o.setParent(r);
                os.add(o);
                j++;
            }
            r.setObservations(os);
            rolloverSets.add(r);
        }
    }

    protected RolloverSet rolloverSet(int i) {
        return rolloverSets.get(i % rolloverSets.size());
    }

    private void initRolloverReports() {
        rolloverReports = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            RolloverReport r = new RolloverReport();
            Site s = i % 2 == 0 ? getSiteForName("North") : getSiteForName("South");
            r.setSite(s);
            Set<AbstractRolloverObservation> os = new HashSet<>();
            int j = 0;
            while (os.size() < 10 && j < 100) {
                RolloverObservation o = rolloverObservationCopy(j);
                o.setParent(r);
                os.add(o);
                j++;
            }
            r.setObservations(os);
            rolloverReports.add(r);
        }
    }


    protected void initAll() {
        initPartners();

        initSites();
        initTimes();
        initDegDegCoordinates();
        initHmsDmsCoordinates();
        initMagnitudes();
        initProperMotions();
        initEphemerisElements();
        initObservationMetaData();
        initGuidestars();
        initBlueprints();
        initNonsiderealTargets();
        initSiderealTargets();
        initItacAccepts();
        initSubmissionAccepts();
        initSubmissionReceipts();
        initInstitutionAddresses();
        initPrincipalInvestigators();
        initSubmissionRequests();
        initCoInvestigators();
        initInvestigatorTeams();
        initConditions();
        initObservations(100);
        initNgoSubmissions();
        initLargeProgramSubmissions();
        initExchangeSubmissions();
        initItacs();
        initMetas();
        initPhaseIProposals(100);
        initSemesters();
        initCommittees();
        initPartnerSequences();
        initProposals(100);
        initBandings();
        initConditionBuckets();
        initConditionSets();
        initRaBinSizes();
        initDecBinSizes();
        initBinConfigurations();
        initShutdowns();
        initQueues();
        initAuthorityRoles();
        initPeople();
        initRolloverObservations();
        initRolloverSets();
        initRolloverReports();

        fixupUpwardDependencies();
        for (Proposal p : proposals) {
            tieTogetherObservationsAndTargetsAndBlueprints(p);
        }
    }

    protected void tieTogetherObservationsAndTargetsAndBlueprints(Proposal proposal) {
        int fixedUp = 0;
        int threw = 0;
        for (Observation o : proposal.getAllObservations()) {
            Target t = o.getTarget();
            BlueprintBase bp = o.getBlueprint();
            Condition c = o.getCondition();
            Validate.notNull(t);
            Validate.notNull(c);
            Validate.notNull(bp);
            PhaseIProposal p = o.getProposal();
            if (p != null && bp != null && t != null) {
                try {
                    //First, make sure that proposal points at the obs
//                    if(p.getObservations().contains(o) == false){
//                        p.getAllObservations().add(o);
//                    }
//                    //Make sure proposal points at target
//                    if(p.getTargets().contains(t) == false){
//                        p.getTargets().add(t);
//                    }
//                    //Make sure proposal points at bp
//                    if(p.getBlueprints().contains(bp) == false){
//                        p.getBlueprints().add(bp);
//                    }
//                    //Make sure proposal points at bp
//                    if(p.getConditions().contains(c) == false){
//                        p.getConditions().add(c);
//                    }
                    //Make sure target points up
                    t.setPhaseIProposal(p);
                    //Make sure bp points up
                    bp.setPhaseIProposal(p);
                    // Make sure condition points up
                    c.setPhaseIProposal(p);
                    //Make sure bp points at observation
//                    if(bp.getObservations().contains(o) == false){
                    bp.getObservations().add(o);
//                    }
                    c.getObservations().add(o);
                    fixedUp++;
                } catch (Exception x) {
                    //swallow it, as long as some are good
                    threw++;
                }
            }
        }
        //LOGGER.log(Level.DEBUG, "Fixed up " + fixedUp + " observations and their stuff. Threw " + threw + " times");
    }

    protected JointProposal initJointProposals(int i) {
        //Note that this requires partners and committees to be saved (to have their IDs set, so that proposal.hashCode() does not throw
        Set<Proposal> ps = new HashSet<>();
        Proposal primary = proposals.get(i % proposals.size());
        Proposal secondary = primary.duplicate();
        Session session = sessionFactory.openSession();
        //Kill the 2ndary "Accept" if its an NGO submission
//        final Set<NgoSubmission> ngo = secondary.getPhaseIProposal().getSubmissions().getNgo();
        final List<Submission> submissions = secondary.getPhaseIProposal().getSubmissions();
        boolean isFirstAccept = true;
        for (Submission s : submissions) {
            if (s.getAccept() != null && isFirstAccept) {
                isFirstAccept = false;
            } else {
                s.setAccept(null);
            }
        }
        ps.add(primary);
        ps.add(secondary);
        JointProposal joint = new JointProposal();
        joint.setProposals(ps);
        joint.setPrimaryProposal(primary);
        try {
            //Since the ITAC is created anew in joint, save it
            session.saveOrUpdate(joint.getItac());
            session.saveOrUpdate(secondary.getItac());
            session.saveOrUpdate(secondary);
            session.saveOrUpdate(joint);
            session.flush();
        } finally {
            session.close();
        }

        final List<Submission> submissions1 = joint.getPhaseIProposal().getSubmissions();
        Assert.assertTrue(submissions1 != null);
        proposals.add(joint);
        return joint;
    }


    private void fixupUpwardDependencies() {
        Committee c = committees.get(0);
        TargetVisitor tv = new TargetVisitor() {
            @Override
            public void visit(SiderealTarget st) {
                if (siderealTargets.contains(st) == false) {
                    siderealTargets.add(st);
                }
            }

            @Override
            public void visit(NonsiderealTarget nst) {
                if (nonSiderealTargets.contains(nst) == false) {
                    nonSiderealTargets.add(nst);
                }
            }

            @Override
            public void visit(TooTarget tt) {
            }
        };

        for (Proposal pr : proposals) {
            pr.setCommittee(c);
            PhaseIProposal p = pr.getPhaseIProposal();
            for (Observation o : p.getObservations()) {
                observations.add(o);
                final BlueprintBase blueprint = o.getBlueprint();
                blueprint.setPhaseIProposal(p);
                o.setProposal(p);
                Target t = o.getTarget();
                t.accept(tv);
                t.setPhaseIProposal(p);
                for (GuideStar g : o.getGuideStars()) {
                    g.setObservation(o);
                    g.setTarget(t);
                }
            }
        }

        int i = 0;
        for (GuideStar g : guideStars) {
            Target t = siderealTargets.get(i % siderealTargets.size());
            g.setTarget(t);
            i++;
        }
    }

    /**
     * `
     * One time setup before each derived test fixture is run.  The plan is to build up a set of known
     * data that tests will have in common.  During ITAC phase 1, maintaining this in XML files using
     * DBUnit proved to be very cumbersome.  We're going to try another way.
     */
    @BeforeClass
    public static void beforeClass() {
    }

    /**
     * One time teardown after each derived test fixture is run.
     */
    @AfterClass
    public static void afterClass() {

    }

    /**
     * Setup before each individual test.
     */
    @Before
    public void before() {
        initAll();

        if (System.getenv("DUMP_FIXTURES") != null) {
            final ProposalExporterImpl proposalExporter = new ProposalExporterImpl(sessionFactory.openSession());
            for (Proposal p : proposals) {
                try {
                    if (p == null)
                        System.out.println("WHAT?");
                    else {
                        System.out.println(p.toString());
                        final byte[] asXml = proposalExporter.getAsXml(p);
                        FileUtils.writeByteArrayToFile(new File("/tmp/proposal-" + p.getPhaseIProposal().getTitle() + ".xml"), asXml);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            sessionFactory.getCurrentSession().close();
        }
    }

    /**
     * Teardown after each individual test.
     */
    @After
    public void after() {

    }

    /**
     * Returns true if this is one of the tables that contains "static" / stable data (semesters, partners, etc.)
     *
     * @param table
     * @return
     */
    boolean leaveDataInTable(String table) {
        String[] persistents = {
                "semesters",
                "restricted_bins",
                "partners",
                "email_templates",
                "bin_configurations",
                "ra_bin_sizes",
                "dec_bin_sizes",
                "band_restriction_rules",
                "conditions",
                "authority_roles",
                "condition_sets"
        };
        for (String persistent : persistents) {
            if (persistent.equalsIgnoreCase(table)) {
                return true;
            }
        }
        return false;
    }


    public void teardown() {
        final Session session = sessionFactory.openSession();

        session.beginTransaction();
        final SQLQuery tablelistQuery = session.createSQLQuery("select tablename from pg_tables where schemaname = 'public';");
        final List<String> tableNames = tablelistQuery.list();
        for (String table : tableNames) {
            if (!leaveDataInTable(table)) {
                final SQLQuery truncateQuery = session.createSQLQuery("truncate " + table + " cascade;");
                truncateQuery.executeUpdate();
            }
        }
        session.getTransaction().commit();
        session.close();
    }

    public void deleteList(final Session session, final String hql) {
        Query query = session.createQuery(hql);
        for (Object o : query.list()) {
            session.delete(o);
        }
    }

    protected Long importDemoFile() {
        final Session session = sessionFactory.openSession();
        try {
            final ProposalImporter importer = new ProposalImporter(new File("/tmp"));
            final String fileName = "/Demo.xml";
            final InputStream inputStream = getClass().getResourceAsStream(fileName);
            session.merge(committees.get(0));
            importer.importDocuments(session, fileName, inputStream, committees.get(0));
            return importer.getResults().get(0).getPhaseIProposalId();
        } finally {
            session.close();
        }
    }

    protected RABinSize raBin(int i) {
        return raBinSizes.get(i % raBinSizes.size());
    }

    private void initRaBinSizes() {
        raBinSizes = listFromDatabase("From RABinSize");
    }

    protected DecBinSize decBin(int i) {
        return decBinSizes.get(i % decBinSizes.size());
    }

    private void initDecBinSizes() {
        decBinSizes = listFromDatabase("From DecBinSize");
    }

    protected Site site(int i) {
        return sites.get(i % sites.size());
    }

    private void initSites() {
        sites = new ArrayList<>();
        Site s = new Site("North");
        sites.add(s);

        s = new Site("South");
        sites.add(s);
    }

    public Object doHibernateQueryWithUniqueResult(String s) {
        LOGGER.log(Level.DEBUG, "Query: " + s);
        final Session session = sessionFactory.openSession();

        try {
            final Query query = session.createQuery(s);
            Object o = query.uniqueResult();
            return o;
        } finally {
            LOGGER.log(Level.DEBUG, "Query completed");
            session.close();
        }
    }

    @Autowired(required = true)
    public void setSessionFactory(final SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    public SessionFactory getSessionFactory() {
        return sessionFactory;
    }

    protected Object doHibernateQueryTakingId(final String s, final Long id) {
        final Session session = sessionFactory.openSession();

        try {
            final Query query = session.createQuery(s).setLong("id", id);
            return query.uniqueResult();
        } finally {
            session.close();
        }
    }

    protected Object doHibernateNamedQueryTakingId(String s, Long id) {
        final Session session = sessionFactory.openSession();

        try {
            final Query query = session.getNamedQuery(s).setLong("id", id);
            return query.uniqueResult();
        } finally {
            session.close();
        }
    }

    protected void saveOrUpdateAll(Collection es) {
        Session s = sessionFactory.openSession();
        try {
            saveOrUpdateAll(es, s);
            s.flush();
        } finally {
            s.close();
        }
    }

    protected void saveOrUpdateAll(Collection<Object> es, Session s) {
        for (Object e : es) {
            try {
                s.saveOrUpdate(e);
            } catch (Throwable t) {
                throw new RuntimeException(t);
            }
        }
    }

    protected void saveSemesterCommitteesProposalsPeople() {
        //Remove empty partners
        List<Partner> badPs = new ArrayList<>();
        for (Partner p : partners) {
            if (p.getAbbreviation() == null) {
                badPs.add(p);
            }
        }
        for (Partner p : badPs) {
            partners.remove(p);
        }
        saveOrUpdateAll(partners);
        saveOrUpdateAll(semesters);

        saveOrUpdateAll(sites);

        saveOrUpdateAll(committees);
        saveOrUpdateAll(partnerSequences);
        saveOrUpdateAll(shutdowns);

        saveOrUpdateAll(people);
        saveOrUpdateAll(proposals);
    }

    /**
     * Creates a fixture with base data and empty committees.
     */
    public void createEmptyCommitteesFixture() {
        saveOrUpdateAll(sites);
        saveOrUpdateAll(partners);
        saveOrUpdateAll(semesters);
        saveOrUpdateAll(committees);
        saveOrUpdateAll(partnerSequences);
        saveOrUpdateAll(people);
        saveOrUpdateAll(raBinSizes);
        saveOrUpdateAll(decBinSizes);
        saveOrUpdateAll(binConfigurations);
    }

    /**
     * Creates a fixture with all proposals and committees.
     */
    public void createCommitteeAndProposalsFixture() {
        createEmptyCommitteesFixture();
        saveOrUpdateAll(proposals);
    }

    /**
     * Creates a fixture with all proposals, committees and queues.
     */
    public void createCommitteeAndQueueFixture() {
        //Eliminate all proposals without a site
        for (Proposal p : proposals) {
            if (p.getPhaseIProposal().getBlueprints().size() == 0) {
                proposals.remove(p);
            }
        }
        createCommitteeAndProposalsFixture();
        for (Proposal p : proposals) {
            if (p.getPhaseIProposal().getBlueprints().size() == 0) {
                throw new RuntimeException();

            }
        }
        // save first queue; queues are connected to committee(0) on creation (see initQueues)
        saveOrUpdateAll(sites);
        saveOrUpdateAll(decBinSizes);
        saveOrUpdateAll(raBinSizes);
        saveOrUpdateAll(binConfigurations);
        saveOrUpdateAll(bandings);
        saveOrUpdateAll(queues);
        for (RolloverReport rr : rolloverReports) {
            for (AbstractRolloverObservation ro : rr.getObservations()) {
                ro.setPartner(partners.get(0));
            }
        }
        saveOrUpdateAll(rolloverReports);
    }

    public List<Committee> getCommittees() {
        if (committees == null) {
            committees = getEntities("from Committee c left join fetch c.semester");
        }
        return committees;
    }

    protected List<CoInvestigator> getCoInvestigators() {
        if (coInvestigators == null) {
            coInvestigators = getEntities("from CoInvestigator");
        }
        return coInvestigators;
    }

    protected Partner getPartner(int i) {
        List<Partner> par = getPartners();
        Collections.sort(par, new Comparator<Partner>() {
            @Override
            public int compare(Partner partner, Partner partner2) {
                return partner.getAbbreviation().compareTo(partner2.getAbbreviation());
            }
        });
        return par.get(i % getPartners().size());
    }

    private HashMap<String, Partner> partnersMap = new HashMap<>();

    protected Partner getPartner(String key) {
        if (partnersMap.size() == 0) {
            for (Partner p : getPartners()) {
                partnersMap.put(p.getPartnerCountryKey(), p);
            }
        }
        return partnersMap.get(key);
    }

    protected List<Partner> getPartners() {
        if (partners == null) {
            partners = getEntities("from Partner");
        }
        return partners;
    }

    /**
     * Gets the i-th committee.
     * Note: i is the index in the list of created committees and *not* the database id!
     *
     * @param i
     * @return
     */
    protected Committee getCommittee(int i) {
        return getCommittees().get(i % getCommittees().size());
    }

    /**
     * Gets the i-th committee and (re-) attaches it to the session in case we need to load lazy data.
     * Note: i is the index in the list of created committees and *not* the database id!
     *
     * @param i
     * @return
     */
    protected Committee getCommittee(Session session, int i) {
        return (Committee) session.merge(getCommittees().get(i));
    }

    protected List<Proposal> getProposals() {
        if (proposals == null) {
            proposals = getEntities("from Proposal p left join fetch p.partner");
        }
        return proposals;
    }

    /**
     * Gets the i-th proposal.
     * Note: i is the index in the list of created proposals and *not* the database id!
     *
     * @param i
     * @return
     */
    protected Proposal getProposal(int i) {
        return getProposals().get(i % getProposals().size());
    }

    protected Proposal getProposal(Session session, int i) {
        return (Proposal) session.merge(getProposals().get(i));
    }

    protected Proposal getProposalFromPartner(Session session, final String partnerAbbreviation, int i) {
        if (partnerProposals.size() == 0)
            groupProposalsByPartner();

        final List<Proposal> thisPartnerProposals = partnerProposals.get(partnerAbbreviation);

        return (Proposal) session.merge(thisPartnerProposals.get(i % thisPartnerProposals.size()));
    }

    protected Proposal getProposalFromPartner(final String partnerAbbreviation, int i) {
        if (partnerProposals.size() == 0)
            groupProposalsByPartner();

        final List<Proposal> thisPartnerProposals = partnerProposals.get(partnerAbbreviation);

        return thisPartnerProposals.get(i % thisPartnerProposals.size());
    }


    protected Map<String, List<Proposal>> partnerProposals = new HashMap<>();

    protected void groupProposalsByPartner() {
        for (Partner p : getPartners()) {
            partnerProposals.put(p.getAbbreviation(), new ArrayList<>());
        }
        for (Proposal p : getProposals()) {
            partnerProposals.get(p.getPartner().getAbbreviation()).add(p);
        }
    }


    protected List<PhaseIProposal> getPhaseIProposals() {
        if (phaseIProposals == null) {
            phaseIProposals = getEntities("from PhaseIProposal");
        }
        return phaseIProposals;
    }

    /**
     * Gets the i-th proposal.
     * Note: i is the index in the list of created proposals and *not* the database id!
     *
     * @param i
     * @return
     */
    protected PhaseIProposal getPhaseIProposal(int i) {
        return getPhaseIProposals().get(i % getPhaseIProposals().size());
    }

    protected PhaseIProposal getPhaseIProposal(Session session, int i) {
        return (PhaseIProposal) session.merge(getPhaseIProposals().get(i));
    }


    protected List<Queue> getQueues() {
        if (queues == null) {
            queues = getEntities("from Queue");
        }
        return queues;
    }

    /**
     * Gets the i-th queue.
     * Note: i is the index in the list of created queues and *not* the database id!
     *
     * @param i
     * @return
     */
    protected Queue getQueue(int i) {
        return getQueues().get(i % getQueues().size());
    }

    protected Queue getQueue(Session session, int i) {
        return (Queue) session.merge(getQueues().get(i));
    }


    private List getEntities(String query) {
        Session session = sessionFactory.openSession();
        try {
            List list = getEntities(session, query);
            return list;
        } finally {
            session.close();
        }
    }

    private List getEntities(Session session, String query) {
        return session.createQuery(query).list();
    }

    // temporary helper, replace with something more generally useful
    private Long ngoProposalId = null;

    protected Long getNgoProposalId() {
        if (ngoProposalId == null) {
            Session session = sessionFactory.openSession();
            Transaction tx = session.beginTransaction();
            try {
                List<Proposal> proposals = session.createQuery("from Proposal").list();
                for (Proposal p : proposals) {
                    if (!p.isExchange()) {
                        Long proposalId = p.getId();
                        assertNotNull(proposalId);
                        ngoProposalId = proposalId;
                        break;
                    }
                }
                tx.commit();
            } catch (Exception e) {
                tx.rollback();
                throw new RuntimeException(e); // rethrow after rolling transaction back -> make sure test fails
            } finally {
                session.close();
            }
        }
        return ngoProposalId;
    }
}
