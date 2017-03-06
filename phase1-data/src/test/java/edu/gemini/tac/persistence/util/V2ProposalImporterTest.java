package edu.gemini.tac.persistence.util;

import edu.gemini.model.p1.mutable.*;
import edu.gemini.shared.skycalc.Angle;
import edu.gemini.tac.persistence.Committee;
import edu.gemini.tac.persistence.fixtures.HibernateFixture;
import edu.gemini.tac.persistence.phase1.CoInvestigator;
import edu.gemini.tac.persistence.phase1.Condition;
import edu.gemini.tac.persistence.phase1.Coordinates;
import edu.gemini.tac.persistence.phase1.DegDegCoordinates;
import edu.gemini.tac.persistence.phase1.EphemerisElement;
import edu.gemini.tac.persistence.phase1.GuideStar;
import edu.gemini.tac.persistence.phase1.HmsDmsCoordinates;
import edu.gemini.tac.persistence.phase1.Itac;
import edu.gemini.tac.persistence.phase1.ItacAccept;
import edu.gemini.tac.persistence.phase1.Magnitude;
import edu.gemini.tac.persistence.phase1.*;
import edu.gemini.tac.persistence.phase1.Observation;
import edu.gemini.tac.persistence.phase1.ObservationMetaData;
import edu.gemini.tac.persistence.phase1.PrincipalInvestigator;
import edu.gemini.tac.persistence.phase1.ProperMotion;
import edu.gemini.tac.persistence.phase1.SiderealTarget;
import edu.gemini.tac.persistence.phase1.Target;
import edu.gemini.tac.persistence.phase1.TimeAmount;
import edu.gemini.tac.persistence.phase1.TimeUnit;
import edu.gemini.tac.persistence.phase1.blueprint.BlueprintBase;
import edu.gemini.tac.persistence.phase1.blueprint.altair.AltairConfiguration;
import edu.gemini.tac.persistence.phase1.blueprint.gmosn.GmosNBlueprintImaging;
import edu.gemini.tac.persistence.phase1.blueprint.gnirs.GnirsBlueprintImaging;
import edu.gemini.tac.persistence.phase1.blueprint.nifs.NifsBlueprintAo;
import edu.gemini.tac.persistence.phase1.proposal.ClassicalProposal;
import edu.gemini.tac.persistence.phase1.proposal.PhaseIProposal;
import edu.gemini.tac.persistence.phase1.sitequality.CloudCover;
import edu.gemini.tac.persistence.phase1.sitequality.ImageQuality;
import edu.gemini.tac.persistence.phase1.sitequality.SkyBackground;
import edu.gemini.tac.persistence.phase1.sitequality.WaterVapor;
import edu.gemini.tac.util.ProposalImporter;
import org.apache.log4j.Logger;
import org.hibernate.Query;
import org.hibernate.classic.Session;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;

public class V2ProposalImporterTest extends HibernateFixture {
    private static final Logger LOGGER = Logger.getLogger(V2ProposalImporterTest.class);

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    @Before
    public void before() {
        super.teardown();
        super.before();
        createCommitteeAndProposalsFixture();
    }

    @Test
    public void testQueueDemo() {
        final ProposalImporter importer = new ProposalImporter(tmp.newFolder("pdf"));
        Session session = getSessionFactory().openSession();
        try {
            Committee committee = (Committee) session.createQuery("from Committee where id = "+committees.get(0).getId()).uniqueResult();
            
            final String fileName = "/queueDemo.xml";
            final InputStream inputStream = getClass().getResourceAsStream(fileName);
            importer.importDocuments(session, fileName, inputStream, committee);
            final Long phaseIProposalId = importer.getResults().get(0).getPhaseIProposalId();

            session.close();

            session = getSessionFactory().openSession();
            session.getTransaction().begin();
            final Query query = session.createQuery("from PhaseIProposal p " +
                    "join fetch p.targets t " +
                    "left join fetch t.coordinates " +
                    "where p.id = :id").setLong("id", phaseIProposalId);
            final PhaseIProposal proposal = (PhaseIProposal) query.uniqueResult();
            session.createQuery("from PhaseIProposal p " +
                    "join fetch p.blueprints " +
                    "where p.id = :id").setLong("id", phaseIProposalId).uniqueResult();

            assertEquals("933b9bcb-db23-42dc-b178-3e37dfc8507f", proposal.getSubmissionsKey());
            assertEquals(TacCategory.SOLAR_SYSTEM, proposal.getTacCategory());
            assertEquals("2013.2.1", proposal.getSchemaVersion());

            assertNotNull(proposal.getMeta());
            assertEquals("figure1.png", proposal.getMeta().getAttachment());

            assertEquals("Molecular Hydrogen Excitation in Actively Star-forming Dwarf Galaxies", proposal.getTitle());
            assertEquals("We propose to observe a small sample of weak-continuum , dwarf galaxies to investigate the excitation of molecular hydrogen in massivestar-forming complexes.  In the usable fraction of our previous allocation we were able to observe one of our targets, NGC5461.  This dataset unambiguously shows that the gas is excited in low density photo-dissociation regions, contrary to the widespread assumption in   the literature that the H2 in galaxies is predominantly shock excited. The weakness of the dwarf galaxy continua permits detection of thehigher level H2 transitions which are essential to determine the gas excitation and relative contributions of thermal and UV-excited gas.", proposal.getProposalAbstract());

            assertEquals(2, proposal.getKeywords().size());
            assertTrue(proposal.getKeywords().contains(Keyword.COOLING_FLOWS));
            assertTrue(proposal.getKeywords().contains(Keyword.BLUE_STRAGGLERS));

            assertNotNull(proposal.getInvestigators());
            final PrincipalInvestigator pi = proposal.getInvestigators().getPi();
            assertNotNull(pi);

            assertEquals("Samuel", pi.getFirstName());
            assertEquals("Nasweis", pi.getLastName());
            assertEquals(InvestigatorStatus.PH_D, pi.getStatus());
            assertEquals("flo@gna.cl", pi.getEmail());
            assertTrue(pi.getPhoneNumbers().contains("99393939"));
            assertEquals("Gemini Observatory - North",pi.getInstitutionAddress().getInstitution());
            assertTrue(pi.getInstitutionAddress().getAddress().contains("Gemini Observatory"));
            assertTrue(pi.getInstitutionAddress().getAddress().contains("670 N. Aohoku Place"));
            assertTrue(pi.getInstitutionAddress().getAddress().contains("Hilo, HI 96720"));
            assertEquals("USA", pi.getInstitutionAddress().getCountry());

            assertEquals(6, proposal.getTargets().size());

            boolean jupiterFound = false;
            boolean vegaFound = false;
            boolean otherFound = false;

            for (Target t: proposal.getTargets()) {
                if (t.getName().equals("Jupiter")) {
                    NonsiderealTarget nst = (NonsiderealTarget) t;
                    final List<EphemerisElement> ephemeris = nst.getEphemeris();
                    assertEquals(3, ephemeris.size());
                    jupiterFound = true;
                } else if (t.getName().equals("Vega")) {
                    SiderealTarget st = (SiderealTarget) t;
                    final Coordinates coordinates = st.getCoordinates();
                    assertNotNull(coordinates);
                    assertTrue(coordinates instanceof DegDegCoordinates);
                    final DegDegCoordinates degCoordinates = (DegDegCoordinates) coordinates;

                    assertEquals(new Angle(279.23473479d, Angle.Unit.DEGREES), degCoordinates.getRa());
                    assertEquals(new Angle(38.78368896d, Angle.Unit.DEGREES), degCoordinates.getDec());

                    final ProperMotion properMotion = st.getProperMotion();
                    assertEquals(new BigDecimal("286.23"), properMotion.getDeltaDec());
                    assertEquals(new BigDecimal("200.94"), properMotion.getDeltaRA());

                    assertEquals(6, st.getMagnitudes().size());

                    vegaFound = true;
                } else if (t.getName().equals("GSC0726600038")) {
                    SiderealTarget st = (SiderealTarget) t;

                    assertTrue(st.getCoordinates() instanceof HmsDmsCoordinates);
                    final HmsDmsCoordinates hmsDmsCoordinates = (HmsDmsCoordinates) st.getCoordinates();

                    assertEquals(1, st.getMagnitudes().size());

                    otherFound = true;
                }
            }
            assertTrue(jupiterFound);
            assertTrue(vegaFound);
            assertTrue(otherFound);

            boolean cc50Found = false;
            boolean cc70Found = false;
            boolean iq20Found = false;
            boolean iq70Found = false;

            assertEquals(2, proposal.getConditions().size());
            for (Condition c: proposal.getConditions()) {
                if (c.getCloudCover().equals(CloudCover.CC_50))
                    cc50Found = true;
                if (c.getCloudCover().equals(CloudCover.CC_70))
                    cc70Found = true;

                if (c.getImageQuality().equals(ImageQuality.IQ_20))
                    iq20Found = true;
                if (c.getImageQuality().equals(ImageQuality.IQ_70))
                    iq70Found = true;
            }

            assertTrue(iq20Found);
            assertTrue(iq70Found);
            assertTrue(cc50Found);
            assertTrue(cc70Found);

            boolean gnirsFound = false;
            boolean gmosNFound = false;
            boolean nifsFound = false;
            assertEquals(3, proposal.getBlueprints().size());
            for (BlueprintBase b : proposal.getBlueprints()) {
                if (b instanceof GnirsBlueprintImaging) {
                    gnirsFound = true;
                    final GnirsBlueprintImaging blueprintImaging = (GnirsBlueprintImaging) b;
                    blueprintImaging.getFilter().equals(GnirsFilter.H2);
                    blueprintImaging.getPixelScale().equals(GnirsPixelScale.PS_005);
                } else if (b instanceof NifsBlueprintAo) {
                    final NifsBlueprintAo nifsAo = (NifsBlueprintAo) b;
                    assertTrue(nifsAo.getDisperser().equals(NifsDisperser.H));
                    assertTrue(nifsAo.getAltairConfiguration().equals(AltairConfiguration.LGS_WITHOUT_PWFS1));
                    assertEquals(NifsOccultingDisk.OD_2,nifsAo.getOccultingDisk());

                    nifsFound = true;
                } else if (b instanceof GmosNBlueprintImaging) {
                    final GmosNBlueprintImaging blueprintImaging = (GmosNBlueprintImaging) b;
                    assertTrue(blueprintImaging.getFilters().contains(GmosNFilter.fromValue("i (780 nm)")));
                    gmosNFound = true;
                }
            }
            assertTrue(gnirsFound);
            assertTrue(gmosNFound);
            assertTrue(nifsFound);

            //TODO: Lots more to do here.
        } finally {
            session.close();
        }
    }

    @Test
    public void testDemo() throws JAXBException {
        final ProposalImporter importer = new ProposalImporter(tmp.newFolder("pdf"));
        Session session = getSessionFactory().openSession();
        try {
            Committee committee = (Committee) session.createQuery("from Committee where id = "+committees.get(0).getId()).uniqueResult();

            final String fileName = "/Demo.xml";
            final InputStream inputStream = getClass().getResourceAsStream(fileName);
            importer.importDocuments(session, fileName, inputStream, committee);
            final Long phaseIProposalId = importer.getResults().get(0).getPhaseIProposalId();

            session.close();

            session = getSessionFactory().openSession();
            session.getTransaction().begin();
            final Query query = session.createQuery("from PhaseIProposal p " +
                    "join fetch p.targets t " +
                    "join fetch t.coordinates " +
                    "where p.id = :id").setLong("id", phaseIProposalId);
            final PhaseIProposal proposal = (PhaseIProposal) query.uniqueResult();
            session.createQuery("from PhaseIProposal p " +
                    "join fetch p.blueprints " +
                    "where p.id = :id").setLong("id", phaseIProposalId).uniqueResult();

            assertEquals("933b9bcb-db23-42dc-b178-3e37dfc8507e",proposal.getSubmissionsKey());
            assertEquals(TacCategory.SOLAR_SYSTEM, proposal.getTacCategory());
            assertEquals("2013.2.1", proposal.getSchemaVersion());

            assertNotNull(proposal.getMeta());
            assertEquals("figure1.png", proposal.getMeta().getAttachment());

            assertEquals("Molecular Hydrogen Excitation in Actively Star-forming Dwarf Galaxies", proposal.getTitle());
            assertTrue(proposal.getProposalAbstract().contains("We propose to observe a small sample of weak-continuum , dwarf galaxies"));

            assertEquals(2, proposal.getKeywords().size());
            assertTrue(proposal.getKeywords().contains(Keyword.SUPERGIANTS));
            assertTrue(proposal.getKeywords().contains(Keyword.WOLF_RAYET_STARS));

            assertNotNull(proposal.getInvestigators());
            assertNotNull(proposal.getInvestigators().getPi());

            final List<Target> targets = proposal.getTargets();
            assertNotNull(targets);
            assertEquals(5, targets.size());

            for (Target t : targets) {
                if (t.getTargetId().equals("target-1")) {
                    assertTrue(t instanceof SiderealTarget);
                    final SiderealTarget st = (SiderealTarget) t;
                    final Coordinates coordinates = st.getCoordinates();
                    final HmsDmsCoordinates c = (HmsDmsCoordinates) coordinates;
                    assertEquals(204.980417, c.getRa().convertTo(Angle.Unit.DEGREES).getMagnitude(), 0.00001);
                    assertEquals(-31.640278, c.getDec().convertTo(Angle.Unit.DEGREES).getMagnitude(), 0.00001);

                    final Set<Magnitude> magnitudes = st.getMagnitudes();
                    assertEquals(3, magnitudes.size());

                    for (Magnitude m : magnitudes) {
                        if (m.getBand() == MagnitudeBand.B) {
                            assertEquals(new BigDecimal("10.87"), m.getValue());
                        } else if (m.getBand() == MagnitudeBand.V) {
                            assertEquals(new BigDecimal("10.55"), m.getValue());
                        } else if (m.getBand() == MagnitudeBand.K) {
                            assertEquals(new BigDecimal("14.70"), m.getValue());
                        }
                    }
                }
            }

            final List<Condition> conditions = proposal.getConditions();
            assertNotNull(conditions);
            assertEquals(3, conditions.size());
            Condition condition3 = null;
            for (Condition c: conditions) {
                if (c.getConditionId().equals("condition-1")) {
                    assertEquals("Default", c.getName());
                    assertEquals(new BigDecimal("1.75"), c.getMaxAirmass());
                    assertEquals(CloudCover.CC_50, c.getCloudCover());
                    assertEquals(ImageQuality.IQ_70, c.getImageQuality());
                    assertEquals(SkyBackground.SB_100, c.getSkyBackground());
                    assertEquals(WaterVapor.WV_100, c.getWaterVapor());
                } else if (c.getConditionId().equals("condition-2")) {

                } else if (c.getConditionId().equals("condition-3")) {
                    condition3 = c;
                    assertEquals("Band 3 Observing Conditions", c.getName());
                    assertNull(c.getMaxAirmass());
                    assertEquals(CloudCover.CC_70, c.getCloudCover());
                    assertEquals(ImageQuality.IQ_85, c.getImageQuality());
                    assertEquals(SkyBackground.SB_100, c.getSkyBackground());
                    assertEquals(WaterVapor.WV_100, c.getWaterVapor());
                }
            }

            assertNotNull(condition3);

            assertEquals("933b9bcb-db23-42dc-b178-3e37dfc8507e", proposal.getSubmissionsKey());

            assertTrue(proposal instanceof ClassicalProposal);
            final ClassicalProposal classicalProposal = (ClassicalProposal) proposal;
            assertNull(classicalProposal.getExchange());

            final Itac itac = classicalProposal.getItac();
            assertNotNull(itac);
            assertFalse(itac.getRejected());
            assertEquals("Excellent proposal, let's use it as a demo.", itac.getComment());

            final ItacAccept itacAccept = itac.getAccept();
            assertNotNull(itacAccept);
            assertEquals("GS-2003A-Q-1", itacAccept.getProgramId());
            assertEquals(1, itacAccept.getBand());
            assertEquals(new TimeAmount(new BigDecimal("18.0"), TimeUnit.HR), itacAccept.getAward());
            assertFalse(itacAccept.isRollover());

            final Set<edu.gemini.tac.persistence.phase1.submission.NgoSubmission> ngo = classicalProposal.getNgos();
            assertNotNull(ngo);
            assertEquals(4, ngo.size());
            boolean auFound = false;
            boolean usFound = false;
            for (edu.gemini.tac.persistence.phase1.submission.NgoSubmission n : ngo) {
                if (n.getPartner().getPartnerCountryKey().toUpperCase().equals(NgoPartner.AU.name().toUpperCase())) {
                    auFound = true;
                    final edu.gemini.tac.persistence.phase1.submission.SubmissionRequest request = n.getRequest();
//                    final PrincipalInvestigator test1 = proposal.getInvestigators().getPi();
//                    final Investigator test2 = request.getPrincipalInvestigator();
//                    assertTrue(test1.equals(test2));
                    assertEquals(new TimeAmount(new BigDecimal("6.0"), TimeUnit.HR), request.getTime());
                    assertEquals(new TimeAmount(new BigDecimal("6.0"), TimeUnit.HR), request.getMinTime());

                    final edu.gemini.tac.persistence.phase1.submission.SubmissionReceipt receipt = n.getReceipt();
                    assertNotNull(receipt);
                    assertEquals("sub-1", receipt.getReceiptId());
                    assertNotNull(n.getAccept());
                    assertEquals("joshuaBloch@partner.edu", n.getAccept().getEmail());
                    assertEquals(new BigDecimal("8"), n.getAccept().getRanking());
                    assertEquals(new TimeAmount(10.0d, TimeUnit.HR), n.getAccept().getRecommend());
                    assertEquals(new TimeAmount(10.0d, TimeUnit.HR), n.getAccept().getMinRecommend());
                    assertFalse(n.getAccept().isPoorWeather());
                    assertEquals("We think this proposal should be scheduled.", n.getComment());
                } else if (n.getPartner().getPartnerCountryKey().toUpperCase().equals(NgoPartner.US.name().toUpperCase())) {
                    usFound = true;
                    final edu.gemini.tac.persistence.phase1.submission.SubmissionRequest request = n.getRequest();
                    assertEquals(new TimeAmount(new BigDecimal("2"), TimeUnit.HR), request.getTime());
                    assertEquals(new TimeAmount(new BigDecimal("2"), TimeUnit.HR), request.getMinTime());

                    final edu.gemini.tac.persistence.phase1.submission.SubmissionReceipt receipt = n.getReceipt();
                    assertNotNull(receipt);
                    assertEquals("sub-2", receipt.getReceiptId());

                    assertEquals("Yo, accept this or die.", n.getComment());
                }
            }
            assertTrue(auFound);
            assertTrue(usFound);

            final List<BlueprintBase> blueprints = proposal.getBlueprints();
            assertNotNull(blueprints);
            assertEquals(1, blueprints.size());

            final BlueprintBase blueprintBase = blueprints.get(0);
            assertEquals("blueprint-1", blueprintBase.getBlueprintId());
            assertEquals("Imaging", blueprintBase.getName());
            assertTrue(blueprintBase instanceof edu.gemini.tac.persistence.phase1.blueprint.gmoss.GmosSBlueprintImaging);
            final edu.gemini.tac.persistence.phase1.blueprint.gmoss.GmosSBlueprintImaging imagingBlueprint = (edu.gemini.tac.persistence.phase1.blueprint.gmoss.GmosSBlueprintImaging) blueprintBase;
            final List<GmosSFilter> filters = imagingBlueprint.getFilters();
            assertNotNull(filters);
            assertEquals(2, filters.size());
            assertTrue(filters.contains(GmosSFilter.fromValue("u (350 nm)")));
            assertTrue(filters.contains(GmosSFilter.fromValue("g (475 nm)")));
            assertEquals(GmosSWavelengthRegime.OPTICAL, imagingBlueprint.getWavelengthRegime());

            final Set<Observation> observations = proposal.getObservations();
            boolean observationOneFound = false;
            boolean observationTwoFound = false;
            for (Observation o: observations) {
                if ((o.getCondition().getConditionId().equals("condition-2")) &&
                    (o.getTarget().getTargetId().equals("target-1"))) {
                    observationOneFound = true;
                    assertEquals(new BigDecimal("3.0"),o.getTime().getValue());
                    assertEquals(TimeUnit.HR, o.getTime().getUnits());
                    final Set<GuideStar> guideStars = o.getGuideStars();
                    assertNotNull(guideStars);
                    assertEquals(1, guideStars.size());
                    final Iterator<GuideStar> iterator = guideStars.iterator();
                    final GuideStar guideStar = iterator.next();
                    assertNotNull(guideStar);
                    assertNotNull(guideStar.getTarget());
                    assertEquals("target-5", guideStar.getTarget().getTargetId());
                    assertEquals(Guider.GMOS_OIWFS, guideStar.getGuider());
                } else if ((o.getCondition().getConditionId().equals("condition-1")) &&
                    (o.getTarget().getTargetId().equals("target-2"))) {
                    observationTwoFound = true;
                    assertTrue(o.getGuideStars() == null || o.getGuideStars().size() == 0);
                    assertEquals(new BigDecimal("2.0"),o.getTime().getValue());
                    assertEquals(TimeUnit.HR, o.getTime().getUnits());
                    final ObservationMetaData metaData = o.getMetaData();
                    assertNotNull(metaData);
                    assertEquals("0123456789abcdef", metaData.getCk());
                    assertNotNull(metaData.getGsa());
                    assertEquals(new BigInteger("50"), metaData.getGsa());
                    assertNotNull(metaData.getGuiding());
                    assertEquals(GuidingEvaluation.SUCCESS,metaData.getGuiding().getEvaluation());
                    assertEquals(70, metaData.getGuiding().getPercentage());
                    assertNotNull(metaData.getVisibility());
                    assertEquals(TargetVisibility.GOOD, metaData.getVisibility());

                    // TODO: update with observation meta data
//                    assertEquals(new BigDecimal(89), o.getSuccess().getValue());
//                    assertEquals("873e0cfb14bde732d187bd5e4da7399d", o.getSuccess().getChecksum());
                }

                assertEquals(blueprintBase, o.getBlueprint());
            }
            assertTrue(observationOneFound);
            assertTrue(observationTwoFound);
        } finally {
            session.close();
        }
    }

    @Test
    public void testInvestigatorNormalization() throws JAXBException, IOException {
        final ProposalImporter importer = new ProposalImporter(tmp.newFolder("pdf"));
        Session session = getSessionFactory().openSession();
        try {

            Committee committee = (Committee) session.createQuery("from Committee where id = "+committees.get(0).getId()).uniqueResult();

            // -- read first
            final String fileName1 = "/edu/gemini/tac/exchange/testImport1.xml";
            final InputStream inputStream1 = getClass().getResourceAsStream(fileName1);
            importer.importDocuments(session, fileName1, inputStream1, committee);
            final Long firstPhaseIProposalId = importer.getResults().get(0).getPhaseIProposalId();
            inputStream1.close();
            session.close();

            // -- read second
            final String fileName2 = "/edu/gemini/tac/exchange/testImport2.xml";
            session = getSessionFactory().openSession();
            final InputStream inputStream2 = getClass().getResourceAsStream(fileName2);
            importer.importDocuments(session, fileName2, inputStream2, committee);
            final Long secondPhaseIProposalId = importer.getResults().get(0).getPhaseIProposalId();
            inputStream2.close();
            session.close();

            session = getSessionFactory().openSession();
            session.getTransaction().begin();

            Query query = session.createQuery("from PhaseIProposal pi join fetch pi.investigators pii join fetch pii.pi join fetch pii.coi where pi.id = :id").setLong("id", firstPhaseIProposalId);
            final PhaseIProposal firstProposal = (PhaseIProposal) query.uniqueResult();

            query = session.createQuery("from PhaseIProposal pi join fetch pi.investigators pii join fetch pii.pi join fetch pii.coi where pi.id = :id").setLong("id", secondPhaseIProposalId);
            final PhaseIProposal secondProposal = (PhaseIProposal) query.uniqueResult();

            final CoInvestigator fpCoi = firstProposal.getInvestigators().getCoi().iterator().next();
            final CoInvestigator spCoi = secondProposal.getInvestigators().getCoi().iterator().next();

            assertEquals(firstProposal.getInvestigators().getPi(), secondProposal.getInvestigators().getPi());
            assertEquals(firstProposal.getInvestigators().getPi().getId(), secondProposal.getInvestigators().getPi().getId());
            assertEquals(firstProposal.getInvestigators().getCoi().size(), secondProposal.getInvestigators().getCoi().size());
            assertEquals(fpCoi, spCoi);
            assertEquals(fpCoi.getId(), spCoi.getId());
        } finally {
            session.close();
        }
    }
    
}
