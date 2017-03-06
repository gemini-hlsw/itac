package edu.gemini.tac.persistence.fixtures.builders;

import edu.gemini.model.p1.mutable.*;
import edu.gemini.model.p1.mutable.Band;
import edu.gemini.tac.persistence.*;
import edu.gemini.tac.persistence.Proposal;
import edu.gemini.tac.persistence.Semester;
import edu.gemini.tac.persistence.phase1.Meta;
import edu.gemini.tac.persistence.phase1.Condition;
import edu.gemini.tac.persistence.phase1.HmsDmsCoordinates;
import edu.gemini.tac.persistence.phase1.InstitutionAddress;
import edu.gemini.tac.persistence.phase1.Investigators;
import edu.gemini.tac.persistence.phase1.Itac;
import edu.gemini.tac.persistence.phase1.ItacAccept;
import edu.gemini.tac.persistence.phase1.Magnitude;
import edu.gemini.tac.persistence.phase1.Observation;
import edu.gemini.tac.persistence.phase1.ObservationMetaData;
import edu.gemini.tac.persistence.phase1.ObservationMetaData.GuidingEstimation;
import edu.gemini.tac.persistence.phase1.PrincipalInvestigator;
import edu.gemini.tac.persistence.phase1.SiderealTarget;
import edu.gemini.tac.persistence.phase1.NonsiderealTarget;
import edu.gemini.tac.persistence.phase1.TooTarget;
import edu.gemini.tac.persistence.phase1.EphemerisElement;
import edu.gemini.tac.persistence.phase1.Target;
import edu.gemini.tac.persistence.phase1.TimeAmount;
import edu.gemini.tac.persistence.phase1.TimeUnit;
import edu.gemini.tac.persistence.phase1.blueprint.BlueprintBase;
import edu.gemini.tac.persistence.phase1.blueprint.altair.AltairConfiguration;
import edu.gemini.tac.persistence.phase1.blueprint.exchange.SubaruBlueprint;
import edu.gemini.tac.persistence.phase1.blueprint.exchange.KeckBlueprint;
import edu.gemini.tac.persistence.phase1.blueprint.gnirs.GnirsBlueprintImaging;
import edu.gemini.tac.persistence.phase1.blueprint.gmosn.GmosNBlueprintImaging;
import edu.gemini.tac.persistence.phase1.blueprint.gmoss.GmosSBlueprintLongSlit;
import edu.gemini.tac.persistence.phase1.blueprint.gsaoi.GsaoiBlueprint;
import edu.gemini.tac.persistence.phase1.blueprint.nici.NiciBlueprintStandard;
import edu.gemini.tac.persistence.phase1.blueprint.michelle.MichelleBlueprintImaging;
import edu.gemini.tac.persistence.phase1.blueprint.trecs.TrecsBlueprintImaging;
import edu.gemini.tac.persistence.phase1.blueprint.niri.NiriBlueprint;
import edu.gemini.tac.persistence.phase1.proposal.GeminiNormalProposal;
import edu.gemini.tac.persistence.phase1.proposal.PhaseIProposal;
import edu.gemini.tac.persistence.phase1.proposal.QueueProposal;
import edu.gemini.tac.persistence.phase1.sitequality.CloudCover;
import edu.gemini.tac.persistence.phase1.sitequality.ImageQuality;
import edu.gemini.tac.persistence.phase1.sitequality.SkyBackground;
import edu.gemini.tac.persistence.phase1.sitequality.WaterVapor;
import edu.gemini.tac.persistence.phase1.submission.SubmissionAccept;
import edu.gemini.tac.persistence.phase1.submission.SubmissionReceipt;
import edu.gemini.tac.persistence.phase1.submission.SubmissionRequest;
import edu.gemini.tac.persistence.phase1.submission.NgoSubmission;
import edu.gemini.tac.persistence.phase1.submission.ExchangeSubmission;
import org.apache.commons.lang.Validate;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;

/**
 * A simple proposal builder to be used for creating test data.
 * See ProposalBuilderTest for some usage examples.
 */
public abstract class ProposalBuilder {

    // dummy partner and committee for in-memory only testing (so that we don't have to read anything from the database)
    public static final Partner dummyPartnerAR      = createPartner("AR", "Argentina", true);
    public static final Partner dummyPartnerAU      = createPartner("AU", "Australia", true);
    public static final Partner dummyPartnerBR      = createPartner("BR", "Brazil", true);
    public static final Partner dummyPartnerCA      = createPartner("CA", "Canada", true);
    public static final Partner dummyPartnerCL      = createPartner("CL", "Chile", true);
    public static final Partner dummyPartnerGS      = createPartner("GS", "GeminiStaff", false);
    public static final Partner dummyPartnerUH      = createPartner("UH", "UH", false);
    public static final Partner dummyPartnerUS      = createPartner("US", "USA", true);
    public static final Partner dummyPartnerSubaru  = createPartner("SUBARU", "Subaru", false);
    public static final Partner dummyPartnerKeck    = createPartner("KECK", "Keck", false);
    public static final Committee dummyCommittee    = createCommittee("2012A", true);

    public static final Partner[] DUMMY_PARTNERS = new Partner[] {
            dummyPartnerAR,
            dummyPartnerAU,
            dummyPartnerBR,
            dummyPartnerCA,
            dummyPartnerCL,
            dummyPartnerGS,
            dummyPartnerKeck,
            dummyPartnerSubaru,
            dummyPartnerUH,
            dummyPartnerUS
    };

    static {
        dummyPartnerCL.setSiteShare(Partner.SiteShare.SOUTH);
        dummyPartnerUH.setSiteShare(Partner.SiteShare.NORTH);
    }

    // create some default conditions for easy re-use
    public static final Condition BEST_CONDITIONS = createCondition(CloudCover.CC_50, ImageQuality.IQ_20, SkyBackground.SB_20, WaterVapor.WV_20);
    public static final Condition WORST_CONDITIONS = createCondition(CloudCover.CC_100, ImageQuality.IQ_100, SkyBackground.SB_100, WaterVapor.WV_100);
    public static final Condition CC50_IQ20_SB50_WV50 = createCondition(CloudCover.CC_50, ImageQuality.IQ_20, SkyBackground.SB_50, WaterVapor.WV_50);
    public static final Condition CC70_IQ85_SB100_WV100 = createCondition(CloudCover.CC_70, ImageQuality.IQ_85, SkyBackground.SB_100, WaterVapor.WV_100);

    // create some default blueprints for easy re-use
    public static final BlueprintBase GMOS_N_IMAGING = createGmosNImaging();
    public static final BlueprintBase GMOS_S_LONGSLIT = createGmosSBlueprintLongsslit();
    public static final BlueprintBase NICI_STANDARD = createNici();
    public static final BlueprintBase NIRI_STANDARD = createNiri();
    public static final BlueprintBase GNIRS_IMAGING_H2_PS05 = createGnirsImaging(GnirsFilter.H2, GnirsPixelScale.PS_005, AltairConfiguration.NONE);
    public static final BlueprintBase GNIRS_IMAGING_O6_PS05 = createGnirsImaging(GnirsFilter.ORDER_6, GnirsPixelScale.PS_005, AltairConfiguration.NONE);
    public static final BlueprintBase GNIRS_IMAGING_O6_PS15 = createGnirsImaging(GnirsFilter.ORDER_6, GnirsPixelScale.PS_015, AltairConfiguration.NONE);
    public static final BlueprintBase GNIRS_IMAGING_H2_PS05_LGS = createGnirsImaging(GnirsFilter.H2, GnirsPixelScale.PS_005, AltairConfiguration.LGS_WITHOUT_PWFS1);
    public static final BlueprintBase GNIRS_IMAGING_H2_PS05_NGS = createGnirsImaging(GnirsFilter.H2, GnirsPixelScale.PS_005, AltairConfiguration.NGS_WITHOUT_FIELD_LENS);
    public static final BlueprintBase TRECS_IMAGING = createTrecs();
    public static final BlueprintBase GSAOI = createGsaoi();
    public static final BlueprintBase MICHELLE_IMAGING = createMichelle();
    public static final BlueprintBase SUBARU = createSubaru();
    public static final BlueprintBase KECK = createKeck();

    // define some defaults
    // (committee and partner default to in memory versions, for proposals that are going to be
    // persisted valid entities must be loaded from the database and provided to the builder)
    private static Committee defaultCommittee = dummyCommittee;
    private static Partner defaultPartner = dummyPartnerUS;
    private static BlueprintBase defaultBlueprint = GNIRS_IMAGING_H2_PS05;
    private static Condition defaultCondition = BEST_CONDITIONS;
    private static Band defaultBand = Band.BAND_1_2;
    private static long proposalCnt = 0;

    // these are the values used in a builder instance
    private BlueprintBase currentBlueprint;
    private Condition currentCondition;
    private Band currentBand;
    private long currentProposalCnt;

    // keep track of blueprints, conditions and targets in this builder
    private Map<BlueprintBase, BlueprintBase> blueprints;
    private Map<Condition, Condition> conditions;
    private Map<Target, Target> targets;

    // make those accessible for derived builders
    protected Proposal proposal;
    protected PhaseIProposal phaseIProposal;

    // additional bits and pieces to form a complete and valid proposal
    protected SubmissionReceipt receipt = null;
    protected SubmissionAccept accept = null;
    protected SubmissionRequest request = null;
    protected SubmissionRequest band3Request = null;
    protected Itac itac = null;
    protected String programId = null;
    protected Map<Partner, SubmissionRequest> requests = new HashMap<Partner, SubmissionRequest>();
    protected Set<Keyword> keywords = new HashSet<Keyword>();

    protected ProposalBuilder(PhaseIProposal phaseIProposal) {
        // set some defaults for observation creation
        this.currentBlueprint = defaultBlueprint;
        this.currentCondition = defaultCondition;
        this.currentBand = defaultBand;
        this.currentProposalCnt = proposalCnt++;

        this.blueprints = new HashMap<BlueprintBase, BlueprintBase>();
        this.conditions = new HashMap<Condition, Condition>();
        this.targets = new HashMap<Target, Target>();

        // -- setup phase 1 proposal
        this.phaseIProposal = phaseIProposal;
        this.phaseIProposal.setInvestigators(createInvestigators());
        this.phaseIProposal.setTitle("Test Proposal " + currentProposalCnt);
        this.phaseIProposal.setSubmissionsKey(UUID.randomUUID().toString());
        this.phaseIProposal.setProposalAbstract("abstract");
        this.phaseIProposal.setSchemaVersion("1.0.0");
        this.phaseIProposal.setTacCategory(TacCategory.EXTRAGALACTIC);
        this.phaseIProposal.setMeta(createMeta());

        // -- setup proposal
        this.proposal = new Proposal();
        this.proposal.setPhaseIProposal(phaseIProposal);
        this.proposal.setCommittee(defaultCommittee);
        this.proposal.setPartner(defaultPartner);
    }

    // ========================

    /**
     * Sets a default committee that will be used for all proposals built after this method was called.
     * @param committee
     */
    public static void setDefaultCommittee(Committee committee) {
        defaultCommittee = committee;
    }

    /**
     * Sets a default partner that will be used for all proposals built after this method was called.
     * @param partner
     */
    public static void setDefaultPartner(Partner partner) {
        defaultPartner = partner;
    }

    /**
     * Sets a default band that will be used for all proposals built after this method was called.
     * @param band
     */
    public static void setDefaultBand(Band band) {
        defaultBand = band;
    }

    /**
     * Sets a default blueprint that will be used for all proposals built after this method was called.
     * @param blueprint
     */
    public static void setDefaultBlueprint(BlueprintBase blueprint) {
        defaultBlueprint = blueprint;
    }

    /**
     * Sets a default condition that will be used for all proposals built after this method was called.
     * @param condition
     */
    public static void setDefaultCondition(Condition condition) {
        defaultCondition = condition;
    }

    /**
     * Creates a GNIRS imaging blueprint with the given configuration that can be used as a blueprint template.
     * @param filter
     * @param pixelScale
     * @param altairConfiguration
     * @return
     */
    public static BlueprintBase createGnirsImaging(GnirsFilter filter, GnirsPixelScale pixelScale, AltairConfiguration altairConfiguration) {
        GnirsBlueprintImaging bp = new GnirsBlueprintImaging();
        bp.setName("GNIRS Imaging " + filter.name() + " " + pixelScale.name() + " " + altairConfiguration.getDisplayValue());
        bp.setFilter(filter);
        bp.setPixelScale(pixelScale);
        bp.setAltairConfiguration(altairConfiguration);
        return bp;
    }

    /**
     * Creates a generic GMOS-N imaging blueprint that can be used as a blueprint template.
     */
    // add parameters and make this method public if needed
    private static BlueprintBase createGmosNImaging() {
        List<GmosNFilter> filters = new ArrayList<GmosNFilter>();
        filters.add(GmosNFilter.g_G0301);
        GmosNBlueprintImaging bp = new GmosNBlueprintImaging();
        bp.setName("GMOS-N Imaging");
        bp.setFilters(filters);
        bp.setWavelengthRegime(GmosNWavelengthRegime.OPTICAL);
        return bp;
    }

    /**
     * Creates a generic GMOS-S spectroscopy blueprint that can be used as a blueprint template.
     */
    // add parameters and make this method public if needed
    private static BlueprintBase createGmosSBlueprintLongsslit() {
        GmosSBlueprintLongSlit bp = new GmosSBlueprintLongSlit();
        bp.setName("GMOS-S Spectroscopy Longslit");
        bp.setFpu(GmosSFpu.LONGSLIT_1);
        bp.setDisperser(GmosSDisperser.B1200_G5321);
        bp.setFilter(GmosSFilter.g_G0325);
        bp.setWavelengthRegime(GmosSWavelengthRegime.OPTICAL); // does that make sense?
        return bp;
    }

    /**
     * Creates a generic NICI blueprint that can be used as a blueprint template.
     */
    // add parameters and make this method public if needed
    private static BlueprintBase createNici() {
        NiciBlueprintStandard bp = new NiciBlueprintStandard();
        bp.setName("NICI Standard");
        bp.setDichroic(NiciDichroic.CH4_H_DICHROIC);
        return bp;
    }

    /**
     * Creates a generic NIRI blueprint that can be used as a blueprint template.
     */
    // add parameters and make this method public if needed
    private static BlueprintBase createNiri() {
        NiriBlueprint bp = new NiriBlueprint();
        bp.setName("NIRI Standard");
        bp.setCamera(NiriCamera.F6);
        bp.setAltairConfiguration(AltairConfiguration.NONE);
        return bp;
    }

    private static BlueprintBase createGsaoi() {
        final GsaoiBlueprint bp = new GsaoiBlueprint();
        bp.setName("GSAOI");

        return bp;
    }

    /**
     * Creates a generic TRecs blueprint that can be used as a blueprint template.
     * @return
     */
    // add parameters and make this method public if needed
    private static BlueprintBase createTrecs() {
        TrecsBlueprintImaging bp = new TrecsBlueprintImaging();
        bp.setName("TReCS Imaging");
        return bp;
    }

    /**
     * Creates a generic Michelle blueprint.
     * @return
     */
    // add parameters and make this method public if needed
    private static BlueprintBase createMichelle() {
        MichelleBlueprintImaging bp = new MichelleBlueprintImaging();
        bp.setName("Michelle Imaging");
        return bp;
    }

    private static BlueprintBase createSubaru() {
        SubaruBlueprint bp = new SubaruBlueprint();
        bp.setName("Subaru instrument");
        return bp;
    }
    private static BlueprintBase createKeck() {
        KeckBlueprint bp = new KeckBlueprint();
        bp.setName("Keck instrument");
        return bp;
    }

    /**
     * Adds a condition with given cloud cover, image quality, sky background and water vapor.
     * This condition can be used in the addObservation calls that take a condition as an argument
     * or it can be made the default for all observations using setCondition().
     * Airmass is currently not set.
     * @param cc
     * @param iq
     * @param sb
     * @param wv
     * @return
     */
    public static Condition createCondition(CloudCover cc, ImageQuality iq, SkyBackground sb, WaterVapor wv) {
        Condition c = new Condition();
        c.setCloudCover(cc);
        c.setImageQuality(iq);
        c.setSkyBackground(sb);
        c.setWaterVapor(wv);
        c.setName(c.toString());
        return c;
    }

    /**
     * Adds a sidereal target with given name and given RA (HMS) and DEC (DMS) coordinates.
     * This target can be used in the addObservation calls that take a target as an argument.
     * @param name
     * @param ra
     * @param dec
     * @return
     */
    public static SiderealTarget createSiderealTarget(String name, String ra, String dec) {
        HmsDmsCoordinates c = new HmsDmsCoordinates();
        c.setRa(ra);
        c.setDec(dec);
        SiderealTarget t = new SiderealTarget();
        t.setName(name);
        t.setCoordinates(c);
        t.setEpoch(CoordinatesEpoch.J_2000);
        return t;
    }

    /**
     * Adds a sidereal target with given name and given RA (HMS) and DEC (DMS) coordinates.
     * This target can be used in the addObservation calls that take a target as an argument.
     * @param name
     * @param ra
     * @param dec
     * @return
     */
    public static NonsiderealTarget createNonSiderealTarget(String name, String ra, String dec, Date date) {
        HmsDmsCoordinates c = new HmsDmsCoordinates();
        c.setRa(ra);
        c.setDec(dec);
        EphemerisElement e1 = new EphemerisElement();
        e1.setCoordinates(c);
        e1.setValidAt(date);
        EphemerisElement e2 = new EphemerisElement();
        e2.setCoordinates(c);
        e2.setValidAt(date);
        List<EphemerisElement> ephemerisElements = new ArrayList<EphemerisElement>();
        ephemerisElements.add(e1);
        ephemerisElements.add(e2);
        NonsiderealTarget t = new NonsiderealTarget();
        t.setName(name);
        t.setEphemeris(ephemerisElements);
        t.setEpoch(CoordinatesEpoch.J_2000);
        return t;
    }

    /**
     * Adds a too target with given name.
     * This target can be used in the addObservation calls that take a target as an argument.
     * @param name
     * @return
     */
    public static TooTarget createTooTarget(String name) {
        TooTarget t = new TooTarget();
        t.setName(name);
        t.setEpoch(CoordinatesEpoch.J_2000);
        return t;
    }

    // ======


    /**
     * Sets the committee for this proposal.
     * @param committee
     * @return
     */
    public ProposalBuilder setCommittee(Committee committee) {
        proposal.setCommittee(committee);
        return this;
    }

    /**
     * Sets the submitting partner for this proposal.
     * @param partner
     * @return
     */
    public ProposalBuilder setPartner(Partner partner) {
        proposal.setPartner(partner);
        return this;
    }

    /**
     * Sets the default band for all observations created after calling this setter.
     * @param band
     * @return
     */
    public ProposalBuilder setBand(Band band) {
        currentBand = band;
        return this;
    }

    /**
     * Sets the default blueprint used for all observations created after calling this setter.
     * @param blueprint
     * @return
     */
    public ProposalBuilder setBlueprint(BlueprintBase blueprint) {
        currentBlueprint = blueprint;
        return this;
    }

    /**
     * Sets the default condition used for all observations created after calling this setter.
     * @param condition
     * @return
     */
    public ProposalBuilder setCondition(Condition condition) {
        currentCondition = condition;
        return this;
    }

    /**
     * Sets the submission key.
     * @param key
     * @return
     */
    public ProposalBuilder setSubmissionKey(String key) {
        this.phaseIProposal.setSubmissionsKey(key);
        return this;
    }

    /**
     * Sets the proposal title.
     * @param title
     * @return
     */
    public ProposalBuilder setTitle(String title) {
        this.phaseIProposal.setTitle(title);
        return this;
    }

    /**
     * Sets the pi name, not really needed for anything but makes the test data look nicer.
     * @param first
     * @param last
     * @return
     */
    public ProposalBuilder setPiName(String first,  String last) {
        this.phaseIProposal.getInvestigators().getPi().setFirstName(first);
        this.phaseIProposal.getInvestigators().getPi().setLastName(last);
        this.phaseIProposal.getInvestigators().getPi().setEmail(first+"."+last+"@"+proposal.getPartnerAbbreviation()+".edu");
        return this;
    }

    /**
     * Sets the program ID.
      * @param programId
     */
    public ProposalBuilder setProgramId(String programId) {
        this.programId = programId;
        return this;
    }

    public ProposalBuilder setKeywords(final Set<Keyword> keywords) {
        this.keywords = keywords;
        return this;
    }

    /**
     * Adds an observation.
     * @param t
     * @param time
     * @return
     */
    public ProposalBuilder addObservation(Target t, TimeAmount time) {
        return addObservation(currentBand, currentBlueprint, currentCondition, t, time);
    }

    /**
     * Adds an observation.
     * @param ra
     * @param dec
     * @param time
     * @return
     */
    public ProposalBuilder addObservation(String ra, String dec, TimeAmount time) {
        String name = "Target-"+ra+"-"+dec;
        return addObservation(currentBand, currentBlueprint, currentCondition, createSiderealTarget(name, ra, dec), time);
    }

    /**
     * Adds an observation.
     * @param ra
     * @param dec
     * @param timeAmount
     * @param timeUnit
     * @return
     */
    public ProposalBuilder addObservation(String ra, String dec, double timeAmount, TimeUnit timeUnit) {
        String name = "Target-"+ra+"-"+dec;
        return addObservation(currentBand, currentBlueprint, currentCondition, createSiderealTarget(name, ra, dec), new TimeAmount(timeAmount, timeUnit));
    }

    /**
     * Adds an observation.
     * @param name
     * @param ra
     * @param dec
     * @param timeAmount
     * @param timeUnit
     * @return
     */
    public ProposalBuilder addObservation(String name, String ra, String dec, double timeAmount, TimeUnit timeUnit) {
        return addObservation(currentBand, currentBlueprint, currentCondition, createSiderealTarget(name, ra, dec), new TimeAmount(timeAmount, timeUnit));
    }

    /**
     * Adds an observation.
     * @param c
     * @return
     */
    public ProposalBuilder addObservation(Condition c) {
        return addObservation(currentBand, currentBlueprint, c, createSiderealTarget("", "1:0:0.0", "1:0:0.0"), new TimeAmount(1, TimeUnit.HR));
    }

    /**
     * Adds an observation.
     * @param b
     * @return
     */
    public ProposalBuilder addObservation(BlueprintBase b) {
        return addObservation(currentBand, b, currentCondition, createSiderealTarget("", "1:0:0.0", "1:0:0.0"), new TimeAmount(1, TimeUnit.HR));
    }

    /**
     * Adds an observation.
     * @param b
     * @param bp
     * @param c
     * @param t
     * @param time
     * @return
     */
    public ProposalBuilder addObservation(Band b, BlueprintBase bp, Condition c, Target t, TimeAmount time) {
        // too proposals should only contain too targets!
        Validate.isTrue((this.phaseIProposal.isToo() && (t instanceof TooTarget)) || (!this.phaseIProposal.isToo() && !(t instanceof TooTarget)));

        // create dummy observation meta data
        ObservationMetaData meta = new ObservationMetaData(
                new GuidingEstimation(100, GuidingEvaluation.SUCCESS),
                TargetVisibility.GOOD,
                new BigInteger("0"),
                "012345"
        );

        // put together the observation
        Observation o = new Observation();
        o.setBand(b);
        o.setTime(time);
        o.setProposal(phaseIProposal);
        // blueprint, condition and target have to be copied from "templates" (or re-used if they have already
        // been created for this proposal) since they must not be shared between proposals!
        o.setBlueprint(createBlueprintFromTemplate(bp));
        o.setCondition(createConditionFromTemplate(c));
        o.setTarget(createTargetFromTemplate(t));
        o.setMetaData(meta);
        phaseIProposal.getAllObservations().add(o);

        return this;
    }

    /**
     * Adds a request part for a non-submitting partner.
     * @param partner
     * @param minTime
     * @param time
     * @return
     */
    public ProposalBuilder addRequest(Partner partner, TimeAmount minTime, TimeAmount time) {
        Validate.isTrue(partner != proposal.getPartner()); // current assumption for testing: proposal partner will always be used as accept partner
        SubmissionRequest request = new SubmissionRequest();
        request.setMinTime(minTime);
        request.setTime(time);
        requests.put(partner, request);
        return this;
    }

    /**
     * Sets specific request part for submitting partner.
     * @param minTime
     * @param time
     * @return
     */
    public ProposalBuilder setRequest(TimeAmount minTime,  TimeAmount time) {
        request = new SubmissionRequest();
        request.setMinTime(minTime);
        request.setTime(time);
        return this;

    }

    /**
     * Sets specific request part for submitting partner.
     * @param minTime
     * @param time
     * @return
     */
    public ProposalBuilder setBand3Request(TimeAmount minTime,  TimeAmount time) {
        band3Request = new SubmissionRequest();
        band3Request.setMinTime(minTime);
        band3Request.setTime(time);
        return this;

    }

    /**
     * Sets specific accept part for submitting partner.
     * @param minRecommend
     * @param recommend
     * @param rank
     * @return
     */
    public ProposalBuilder setAccept(TimeAmount minRecommend, TimeAmount recommend, int rank) {
        return setAccept(minRecommend, recommend, rank, false);
    }

    /**
     * Sets specific accept part for submitting partner.
     * @param minRecommend
     * @param recommend
     * @param rank
     * @param poorWeather
     * @return
     */
    public ProposalBuilder setAccept(TimeAmount minRecommend, TimeAmount recommend, int rank, boolean poorWeather) {
        accept = new edu.gemini.tac.persistence.phase1.submission.SubmissionAccept();
        accept.setEmail("accept@accept.or");
        accept.setRanking(new BigDecimal(rank));
        accept.setMinRecommend(minRecommend);
        accept.setRecommend(recommend);
        accept.setPoorWeather(poorWeather);
        return this;
    }

    /**
     * Sets specific receipt part for submitting partner.
     * @param id
     * @param timestamp
     * @return
     */
    public ProposalBuilder setReceipt(String id, Date timestamp) {
        receipt = new SubmissionReceipt();
        receipt.setReceiptId(id);
        receipt.setTimestamp(timestamp);
        return this;
    }

    /**
     * Sets specific itac accept part.
     * @param awarded
     * @return
     */
    public ProposalBuilder setItacAccept(TimeAmount awarded) {
        ItacAccept itacAccept = new ItacAccept();
        itacAccept.setAward(awarded);
        itacAccept.setProgramId(proposal.createProgramId((int) currentProposalCnt));
        itacAccept.setBand(1);
        itac = new Itac();
        itac.setAccept(itacAccept);
        itac.setComment("comment");
        itac.setRejected(false);
        phaseIProposal.setItac(itac);
        return this;
    }

    /**
     * Builds the proposal by adding defaults for bits and pieces that have not been defined manually.
     */
    protected void build() {

        // --- create defaults...

        // create at least one observation if none has been defined manually
        if (proposal.getObservations().size() == 0) {
            if (phaseIProposal.isToo()) {
                // too proposals must have too targets!
                addObservation(createTooTarget("Too Target"), new TimeAmount(1, TimeUnit.HR));
            } else {
                addObservation("1:0:0.0", "1:0:0.0", 1, TimeUnit.HR);
            }
        }
        // create a default accept part for proposal partner (if needed)
        if (accept == null) {
            setAccept(new TimeAmount(1, TimeUnit.HR), new TimeAmount(1, TimeUnit.HR), 1);
        }
        // create a default request part for proposal partner (if needed)
        if (request == null) {
            setRequest(new TimeAmount(1, TimeUnit.HR), new TimeAmount(1, TimeUnit.HR));
        }
        // create a default receipt
        if (receipt == null) {
            Date now = new Date();
            int id =  (int) (System.currentTimeMillis()/1000%1000000);
            setReceipt(this.proposal.getPartner().getAbbreviation() + "-" + id, now);
        }
        // create a default itac part (needed for reports)
        if (itac == null) {
            setItacAccept(accept.getRecommend());
        }
        // set band3 request if applicable
        if (band3Request != null) {
            Validate.isTrue(phaseIProposal instanceof QueueProposal);
            ((QueueProposal) phaseIProposal).setBand3Request(band3Request);
        }

        // --- now replace default values with explicitly set values where applicable

        if (programId != null && phaseIProposal.getItac() != null && phaseIProposal.getItac().getAccept() != null) {
            phaseIProposal.getItac().getAccept().setProgramId(programId);
        }

        if (keywords != null) {
            phaseIProposal.setKeywords(keywords);
        }
    }

    protected void buildSubmissionElements(GeminiNormalProposal normalProposal) {
        if (proposal.getPartner().isExchange()) {
            buildExchangeSubmissionElement(normalProposal);
        } else {
            buildNgoSubmissionElements(normalProposal);
        }
    }

    private void buildExchangeSubmissionElement(GeminiNormalProposal normalProposal) {
        ExchangeSubmission xSubmission = new ExchangeSubmission();
        xSubmission.setPartner(proposal.getPartner());
        xSubmission.setAccept(accept);
        xSubmission.setReceipt(receipt);
        xSubmission.setRequest(request);
        xSubmission.setPartnerLead(normalProposal.getInvestigators().getPi());
        xSubmission.setRejected(false);
        xSubmission.setComment("");
        normalProposal.setExchange(xSubmission);
    }

    private void buildNgoSubmissionElements(GeminiNormalProposal normalProposal) {
        // assuming there have been no ngo parts added already
        // (this is a pre-condition for the current state of the code only, if you change
        // the builder logic, this pre-condition might become invalid, change as needed)
        Validate.isTrue(normalProposal.getNgos().size() == 0);

        // add all requests (if there are any)
        for (Partner partner : requests.keySet()) {
            NgoSubmission submission = new NgoSubmission();
            submission.setPartner(partner);
            submission.setAccept(null);
            submission.setReceipt(null);
            submission.setRequest(requests.get(partner));
            submission.setPartnerLead(normalProposal.getInvestigators().getPi());
            submission.setRejected(false);
            submission.setComment("");
            normalProposal.getNgos().add(submission);
        }

        // now add submission part for proposal partner with accept and request part
        NgoSubmission submission = new NgoSubmission();
        submission.setPartner(proposal.getPartner());
        submission.setAccept(accept);
        submission.setReceipt(receipt);
        submission.setRequest(request);
        submission.setPartnerLead(normalProposal.getInvestigators().getPi());
        submission.setRejected(false);
        submission.setComment("");
        normalProposal.getNgos().add(submission);

    }

    /**
     * Finalizes the proposal by adding defaults for bits and pieces that have not been defined manually.
     * Creates the proposal in memory only, no database necessary; proposal is valid and can be stored at
     * a later stage if needed. This method is also called by create(Session) and create(SessionFactory).
     * @return
     */
    public Proposal create() {
        build();
        proposal.validate();
        return proposal;
    }

    /**
     * Creates and saves the proposal using an already opened session and transaction.
     * This method is also called by create(sessinFactory)
     * @param session
     * @return
     */
    public Proposal create(Session session) {
        // final touch up...
        create();
        // ... and finally: save the proposal
        session.save(proposal);
        return proposal;
    }

    /**
     * Creates and saves the proposal using an on-the-fly session and transaction from the session factory.
     * @param sessionFactory
     * @return
     */
    public Proposal create(SessionFactory sessionFactory) {
        Session s = sessionFactory.openSession();
        s.beginTransaction();
        // final touch up and store in database
        create(s);
        // commit this
        s.getTransaction().commit();
        s.close();
        return proposal;
    }

    // helpers ========================

    /**
     * Creates valid blueprints for the current proposal using a skeleton blueprint as template.
     * @param template
     * @return
     */
    private BlueprintBase createBlueprintFromTemplate(BlueprintBase template) {
        if (blueprints.containsKey(template)) {
            return blueprints.get(template);
        }

        BlueprintBase instance;
        if (template instanceof GnirsBlueprintImaging) {
            GnirsBlueprintImaging orig = (GnirsBlueprintImaging) template;
            GnirsBlueprintImaging copy = new GnirsBlueprintImaging();
            copy.setFilter(orig.getFilter());
            copy.setPixelScale(orig.getPixelScale());
            copy.setAltairConfiguration(orig.getAltairConfiguration());
            instance = copy;
        } else if (template instanceof GmosNBlueprintImaging) {
            GmosNBlueprintImaging orig = (GmosNBlueprintImaging) template;
            GmosNBlueprintImaging copy = new GmosNBlueprintImaging();
            copy.setFilters(orig.getFilters());
            copy.setWavelengthRegime(orig.getWavelengthRegime());
            instance = copy;
        } else if (template instanceof GmosSBlueprintLongSlit) {
            GmosSBlueprintLongSlit orig = (GmosSBlueprintLongSlit) template;
            GmosSBlueprintLongSlit copy = new GmosSBlueprintLongSlit();
            copy.setFpu(orig.getFpu());
            copy.setDisperser(orig.getDisperser());
            copy.setFilter(orig.getFilter());
            copy.setWavelengthRegime(orig.getWavelengthRegime());
            instance = copy;
        } else if (template instanceof NiciBlueprintStandard) {
            NiciBlueprintStandard orig = (NiciBlueprintStandard) template;
            NiciBlueprintStandard copy = new NiciBlueprintStandard();
            copy.setDichroic(orig.getDichroic());
            instance = copy;
        } else if (template instanceof NiriBlueprint) {
            NiriBlueprint orig = (NiriBlueprint) template;
            NiriBlueprint copy = new NiriBlueprint();
            copy.setCamera(orig.getCamera());
            copy.setAltairConfiguration(orig.getAltairConfiguration());
            copy.setFilters(orig.getFilters());
            instance = copy;
        } else if (template instanceof TrecsBlueprintImaging) {
            instance = new TrecsBlueprintImaging();
        } else if (template instanceof MichelleBlueprintImaging) {
            instance = new MichelleBlueprintImaging();
        } else if (template instanceof SubaruBlueprint) {
            instance = new SubaruBlueprint();
        } else if (template instanceof KeckBlueprint) {
            instance = new KeckBlueprint();
        } else if (template instanceof GsaoiBlueprint) {
            instance = new GsaoiBlueprint();
        }else {
            // not yet supported
            throw new NotImplementedException();
        }
        instance.setBlueprintId("blueprint-" + (blueprints.size()+1));
        instance.setName(template.getName());
        phaseIProposal.getBlueprints().add(instance);

        blueprints.put(template, instance);
        return instance;
    }

    private Condition createConditionFromTemplate(Condition template) {
        if (conditions.containsKey(template)) {
            return conditions.get(template);
        }

        Condition c = new Condition();
        c.setConditionId("condition-" + (conditions.size()+1));
        c.setMaxAirmass(template.getMaxAirmass());
        c.setCloudCover(template.getCloudCover());
        c.setImageQuality(template.getImageQuality());
        c.setSkyBackground(template.getSkyBackground());
        c.setWaterVapor(template.getWaterVapor());
        c.setName(template.getName());
        phaseIProposal.getConditions().add(c);

        conditions.put(template, c);
        return c;
    }

    private Target createTargetFromTemplate(Target template) {
        if (targets.containsKey(template)) {
            return targets.get(template);
        }

        Target instance;
        if (template instanceof SiderealTarget) {
            SiderealTarget orig = (SiderealTarget) template;
            SiderealTarget copy = new SiderealTarget();
            HmsDmsCoordinates c = new HmsDmsCoordinates();
            c.setRa(orig.getCoordinates().getRa());
            c.setDec(orig.getCoordinates().getDec());
            copy.setCoordinates(c);
            // currently we are not interested in magnitudes
            // just create one and live with it
            Magnitude m = new Magnitude();
            m.setBand(MagnitudeBand.R);
            m.setSystem(MagnitudeSystem.VEGA);
            m.setValue(new BigDecimal(10));
            m.setTarget(copy);
            Set<Magnitude> magnitudeSet = new HashSet<Magnitude>();
            magnitudeSet.add(m);

            instance = copy;

        } else if (template instanceof NonsiderealTarget) {

            NonsiderealTarget orig = (NonsiderealTarget) template;
            NonsiderealTarget copy = new NonsiderealTarget();
            HmsDmsCoordinates c1 = new HmsDmsCoordinates();
            c1.setRa(orig.getEphemeris().get(0).getCoordinates().getRa());
            c1.setDec(orig.getEphemeris().get(0).getCoordinates().getDec());
            HmsDmsCoordinates c2 = new HmsDmsCoordinates();
            EphemerisElement e1 = new EphemerisElement();
            e1.setCoordinates(c1);
            e1.setValidAt(orig.getEphemeris().get(0).getValidAt());
            c2.setRa(orig.getEphemeris().get(1).getCoordinates().getRa());
            c2.setDec(orig.getEphemeris().get(1).getCoordinates().getDec());
            EphemerisElement e2 = new EphemerisElement();
            e2.setCoordinates(c2);
            e2.setValidAt(orig.getEphemeris().get(1).getValidAt());
            List<EphemerisElement> ephemerisElements = new ArrayList<EphemerisElement>();
            ephemerisElements.add(e1);
            ephemerisElements.add(e2);
            copy.setEphemeris(ephemerisElements);

            instance = copy;

        } else if (template instanceof TooTarget) {
            instance = new TooTarget();

        } else {
            throw new NotImplementedException();
        }

        instance.setTargetId("target-" + (targets.size()+1));
        instance.setName(template.getName());
        instance.setEpoch(template.getEpoch());


        instance.setPhaseIProposal(phaseIProposal);
        phaseIProposal.getTargets().add(instance);

        targets.put(template, instance);
        return instance;
    }

    /**
     * Creates a very simple set of investigators with just one fake pi and no co-is.
     * Change this in case you need more complex data.
     * @return
     */
    private Investigators createInvestigators() {
        Set<String> phoneNumbers = new HashSet<String>();
        phoneNumbers.add("888 999-7565");
        InstitutionAddress address = new InstitutionAddress();
        address.setInstitution("Gemini Observatory");
        address.setAddress("670 N. Aohoku Pl");
        address.setCountry("USA");
        PrincipalInvestigator pi = new PrincipalInvestigator();
        pi.setEmail(Long.toString(currentProposalCnt)+"@i.com");  // email must be unique for pis
        pi.setFirstName("Phil");
        pi.setLastName("Walters");
        pi.setPhoneNumbers(phoneNumbers);
        pi.setInstitutionAddress(address);
        pi.setStatus(InvestigatorStatus.OTHER);
        pi.setStatusDisplayString(InvestigatorStatus.OTHER.toString());
        Investigators investigators = new Investigators();
        investigators.setPi(pi);
        return investigators;
    }

    private Meta createMeta() {
        Meta meta = new Meta();
        meta.setAttachment("attachment");
        return meta;
    }

    /**
     * Creates a dummy committee for "in-memory" only testing (works as long as objects are not persisted).
     * @param name
     * @param isActive
     * @return
     */
    private static Committee createCommittee(String name, boolean isActive) {
        Semester s = new Semester();
        s.setName("A");
        s.setYear(2012);
        Committee c = new Committee();
        c.setName(name);
        c.setActive(isActive);
        c.setSemester(s);
        return c;
    }

    /**
     * Creates a dummy partner for "in-memory" only testing (works as long as objects are not persisted).
     * @param abbreviation
     * @param name
     * @param isCountry
     * @return
     */
    private static Partner createPartner(String abbreviation, String name, boolean isCountry) {
        Partner p = new Partner();
        p.setName(name);
        p.setAbbreviation(abbreviation);
        p.setIsCountry(isCountry);
        p.setPartnerCountryKey(abbreviation);
        p.setNgoFeedbackEmail(abbreviation.toLowerCase() + "@gemini.edu");
        p.setSiteShare(Partner.SiteShare.BOTH);
        return p;
    }

}
