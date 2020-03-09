package edu.gemini.tac.service;

import edu.gemini.tac.persistence.Partner;
import edu.gemini.tac.persistence.Proposal;
import edu.gemini.tac.persistence.Site;
import edu.gemini.tac.persistence.fixtures.FastHibernateFixture;
import edu.gemini.tac.persistence.fixtures.builders.ProposalBuilder;
import edu.gemini.tac.persistence.fixtures.builders.QueueProposalBuilder;
import edu.gemini.tac.persistence.phase1.TimeAmount;
import edu.gemini.tac.persistence.phase1.blueprint.BlueprintBase;
import edu.gemini.tac.persistence.phase1.blueprint.flamingos2.Flamingos2BlueprintImaging;
import edu.gemini.tac.persistence.phase1.blueprint.gmoss.GmosSBlueprintImaging;
import edu.gemini.tac.persistence.phase1.proposal.PhaseIProposal;
import edu.gemini.tac.persistence.phase1.submission.Submission;
import edu.gemini.tac.persistence.phase1.submission.SubmissionAccept;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.FileCopyUtils;

import javax.annotation.Resource;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Tests the Proposal service.
 * <p/>
 * <p/>
 * User: lobrien
 * Date: Dec 2, 2010
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"/services-applicationContext.xml"})
public class ProposalServiceTest extends FastHibernateFixture.WithProposals {
    private static final Logger LOGGER = Logger.getLogger(ProposalServiceTest.class.getName());

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    @Resource(name = "proposalService")
    private IProposalService proposalService;

    @Test
    public void getProposal() {
        assertNotNull(proposalService);
        final Long committeeId = getCommittee(0).getId();
        final Long proposalId = getNgoProposalId();
        final Proposal proposal = proposalService.getProposal(committeeId, proposalId);
        assertNotNull(proposal);
        final PhaseIProposal p1p = proposal.getPhaseIProposal();
        assertNotNull(p1p);
        //Validate data required at Web-layer "proposal details"
        for (Submission s : p1p.getSubmissions()) {
            final Partner p = s.getPartner();
            assertNotNull(p);
        }
        String semesterDisplayName = proposal.getCommittee().getSemester().getDisplayName();
        assertNotNull(semesterDisplayName);
    }


    @Test
    public void saveEditedProposal() {
        LOGGER.log(Level.DEBUG, "saveEditedProposal()");
        Proposal proposal = proposalService.getProposal(getCommittee(0).getId(), getNgoProposalId());
        assertNotNull(proposal);

        PhaseIProposal document = proposal.getPhaseIProposal();
        assertNotNull(document);

        List<Submission> submissions = document.getSubmissions();
        assertNotNull(submissions);

        for (Submission ngo : submissions) {
            assertNotNull(ngo);
            final SubmissionAccept accept = ngo.getAccept();
            assertNotNull(accept);
            accept.setRanking(new BigDecimal("213"));
            accept.setEmail("neat@here.com");
        }
        proposalService.saveEditedProposal(proposal);
        LOGGER.log(Level.DEBUG, "test passed()");
    }

    @Test
    public void canRetrieveProposalsForQueueAnalysis(){
        LOGGER.log(Level.DEBUG, "canRetrieveProposalsForQueueAnalysis");
        List<Proposal> ps = proposalService.getProposalsForQueueAnalysis("Test committee 0");
        Assert.assertTrue(ps.size()  > 0);
    }

    //
    @Test
    /**
     * FR-10
     */
    public void switchSites() {
        LOGGER.log(Level.DEBUG, "switchSites()");

        ProposalBuilder.setDefaultBlueprint(ProposalBuilder.GMOS_N_IMAGING);
        LOGGER.debug("Partner:" + getPartner(0));
        Proposal proposal = new QueueProposalBuilder().
                setPartner(getPartner(0)).
                setCommittee(getCommittee(0)).
                addObservation(ProposalBuilder.GMOS_N_IMAGING).   // one observation with GMOS N
                addObservation(ProposalBuilder.NIRI_STANDARD).    // one observation with NIRI
                create(sessionFactory);

        // check that proposal is at North (all observations are at North)
        assertEquals(proposal.getSite(), Site.NORTH);

        proposalService.switchSites(proposal);
        final Long switchedId = proposal.getId();
        assertTrue(proposal.getId().equals(switchedId));

        // check that now everything is in the south
        assertEquals(proposal.getSite(), Site.SOUTH);
        assertEquals(2, proposal.getBlueprints().size());
        int f2Imaging = 0;
        int gmomsSImaging = 0;
        for (BlueprintBase b : proposal.getBlueprints()) {
            if (b instanceof Flamingos2BlueprintImaging) {
                f2Imaging++;
            }
            if (b instanceof GmosSBlueprintImaging) {
                gmomsSImaging++;
            }
        }
        // expect f2 imaging to replace niri
        assertEquals(1, f2Imaging);
        // expect Gmos S imaging to replace Gmos N imaging
        assertEquals(1, gmomsSImaging);
    }
//
//    @Test
//    public void canFilterProposals() {
//        LOGGER.log(Level.DEBUG, "canFilterProposals()");
//        List<Proposal> ps = proposalService.getProposals(10L, new IMatch<Proposal>() {
//            public boolean match(Proposal arg) {
//                return true;
//            }
//        });
//        Assert.assertEquals(5, ps.size());
//        List<Proposal> ps1 = proposalService.getProposals(10L, new IMatch<Proposal>() {
//            @Override
//            public boolean match(Proposal arg) {
//                return false;
//            }
//        });
//        Assert.assertEquals(0, ps1.size());
//    }
//
//    @Test
//    public void canWorkWithJointsAndComponents() {
//        LOGGER.log(Level.DEBUG, "canWorkWithJointsAndComponents()");
//
//    }
//
    @Test
    @Ignore
    public void canEditTacExtension() {
        LOGGER.log(Level.DEBUG, "canEditTacExtensions()");
        
        final Long committeeId = getCommittee(0).getId();
        final Long proposalId = getNgoProposalId();
        
        final Proposal proposal = proposalService.getProposal(committeeId, proposalId);
        final List<Submission> submissions1 = proposal.getPhaseIProposal().getSubmissions();
        Submission ngo = submissions1.iterator().next();
        Assert.assertNotNull(ngo);

        final String partnerKey = ngo.getPartner().getPartnerCountryKey();
        final BigDecimal oldRanking = ngo.getAccept().getRanking();
        final String oldComment = proposal.getPhaseIProposal().getComment();
        final TimeAmount oldMinReco = ngo.getAccept().getMinRecommend();
        final TimeAmount oldReco = ngo.getAccept().getRecommend();
        final StringBuilder fromValueBuffer = new StringBuilder();

        proposalService.editProposalWithAlreadyValidatedData(committeeId, proposalId, "TacExtension", "GeminiStaff", "partnerRanking", "4", fromValueBuffer);
        proposalService.editProposalWithAlreadyValidatedData(committeeId, proposalId, "TacExtension", "GeminiStaff", "partnerComment", "foo", fromValueBuffer);
        proposalService.editProposalWithAlreadyValidatedData(committeeId, proposalId, "TacExtension", "GeminiStaff", "partnerMinTime", "4", fromValueBuffer);
        proposalService.editProposalWithAlreadyValidatedData(committeeId, proposalId, "TacExtension", "GeminiStaff", "partnerTime", "4", fromValueBuffer);

        Proposal pEdited = proposalService.getProposal(committeeId, proposalId);
        final List<Submission> submissionsNew = pEdited.getPhaseIProposal().getSubmissions();
        Submission ngoNew = submissionsNew.iterator().next();

        Assert.assertEquals("4", ngoNew.getAccept().getRanking().toPlainString());
        Assert.assertEquals("foo",proposal.getPhaseIProposal().getComment());
        Assert.assertEquals("4", ngoNew.getAccept().getMinRecommend().getValue().toString());
        Assert.assertEquals("4", ngoNew.getAccept().getRecommend());
    }
//
//    @Test
//    public void canEditGeminiPart() {
//        LOGGER.log(Level.DEBUG, "canEditGeminiPart()");
//        Proposal p = proposalService.getProposal(10L, 100L);
//        SiteQuality defaultSq = p.getDocument().getObservatory().getGeminiPart().getDefaultSiteQuality();
//        Assert.assertEquals(80, defaultSq.getCloudCover().getPercentage());
//        Assert.assertEquals(100, defaultSq.getImageQuality().getPercentage());
//        Assert.assertEquals(100, defaultSq.getSkyBackground().getPercentage());
//        Assert.assertEquals(100, defaultSq.getWaterVapor().getPercentage());
//
//        proposalService.editProposalWithAlreadyValidatedData(10L, 100L, "GeminiPart", "DefaultSiteQuality", "cloudCover", "70");
//        proposalService.editProposalWithAlreadyValidatedData(10L, 100L, "GeminiPart", "DefaultSiteQuality", "imageQuality", "70");
//        proposalService.editProposalWithAlreadyValidatedData(10L, 100L, "GeminiPart", "DefaultSiteQuality", "skyBackground", "80");
//        proposalService.editProposalWithAlreadyValidatedData(10L, 100L, "GeminiPart", "DefaultSiteQuality", "waterVapor", "80");
//
//        Proposal pEdited = proposalService.getProposal(10L, 100L);
//        SiteQuality sqNew = pEdited.getDocument().getObservatory().getGeminiPart().getDefaultSiteQuality();
//        Assert.assertEquals(70, sqNew.getCloudCover().getPercentage());
//        Assert.assertEquals(70, sqNew.getImageQuality().getPercentage());
//        Assert.assertEquals(80, sqNew.getSkyBackground().getPercentage());
//        Assert.assertEquals(80, sqNew.getWaterVapor().getPercentage());
//    }
//
//    @Test
//    public void canEditBand3Conditions() {
//        LOGGER.log(Level.DEBUG, "canEditBand3Conditions()");
//        Proposal p = proposalService.getProposal(10L, 100L);
//        SiteQuality defaultSq = p.getDocument().getObservatory().getGeminiPart().getBand3ConditionsSiteQuality();
//        Assert.assertEquals(80, defaultSq.getCloudCover().getPercentage());
//        Assert.assertEquals(100, defaultSq.getImageQuality().getPercentage());
//        Assert.assertEquals(100, defaultSq.getSkyBackground().getPercentage());
//        Assert.assertEquals(100, defaultSq.getWaterVapor().getPercentage());
//
//        proposalService.editProposalWithAlreadyValidatedData(10L, 100L, "GeminiPart", "Band3Conditions", "cloudCover", "70");
//        proposalService.editProposalWithAlreadyValidatedData(10L, 100L, "GeminiPart", "Band3Conditions", "imageQuality", "70");
//        proposalService.editProposalWithAlreadyValidatedData(10L, 100L, "GeminiPart", "Band3Conditions", "skyBackground", "80");
//        proposalService.editProposalWithAlreadyValidatedData(10L, 100L, "GeminiPart", "Band3Conditions", "waterVapor", "80");
//
//        Proposal pEdited = proposalService.getProposal(10L, 100L);
//        SiteQuality sqNew = pEdited.getDocument().getObservatory().getGeminiPart().getBand3ConditionsSiteQuality();
//        Assert.assertEquals(70, sqNew.getCloudCover().getPercentage());
//        Assert.assertEquals(70, sqNew.getImageQuality().getPercentage());
//        Assert.assertEquals(80, sqNew.getSkyBackground().getPercentage());
//        Assert.assertEquals(80, sqNew.getWaterVapor().getPercentage());
//    }
//
//    @Test
//    public void canEditItacExtension() {
//        LOGGER.log(Level.DEBUG, "canEditITacExtension()");
//        final Long committeeId = getCommitteeId();
//        final Long proposalId = getNgoProposalId();
//        proposalService.editProposalWithAlreadyValidatedData(committeeId, proposalId, "ITacExtension", "Default", "contactScientistEmail", "foo@bar");
//        proposalService.editProposalWithAlreadyValidatedData(committeeId, proposalId, "ITacExtension", "Default", "itacComment", "awesome!");
//        //TODO: proposalService.editProposalWithAlreadyValidatedData(committeeId, proposalId, "ITacExtension", "Default", "geminiComment", "w00t!");
//
//        Proposal pEdited = proposalService.getProposal(committeeId, proposalId);
//        final Itac itac = pEdited.getPhaseIProposal().getSubmissions().getItac();
//        Assert.assertEquals("foo@bar", itac.getAccept().getEmail());
//        Assert.assertEquals("awesome!", itac.getComment());
//        //TODO: Assert.assertEquals("w00t!", itac.);
//    }

    @Test
    @Transactional
    public void canDuplicate() throws IOException {
        LOGGER.log(Level.DEBUG, "canDuplicate()");
        // -- in preparation of the test create a dummy pdf file that corresponds with the proposal to be copied
        Proposal p = (Proposal) sessionFactory.getCurrentSession().merge(getProposal(0));
        File pdfFolder = tmp.newFolder("pdfs");
        File dummy = p.getPdfLocation(pdfFolder);
        dummy.getParentFile().mkdirs(); // create parent directory for committee
        FileCopyUtils.copy(getClass().getResourceAsStream("/edu/gemini/tac/util/empty.pdf"), new FileOutputStream(dummy));

        // -- duplicate and check if it worked including duplication of pdf file
        Proposal dup = proposalService.duplicate(p, pdfFolder);
        Assert.assertNotSame(p, dup);
        Assert.assertEquals(p.getPhaseIProposal().getTitle(), dup.getPhaseIProposal().getTitle());
        Assert.assertTrue(dup.getPdfLocation(pdfFolder).exists());

        //Confirm dupe has different receipt ids
        Collection<Submission> submissions = p.getSubmissionsPartnerEntries().values();
        Assert.assertTrue(submissions.size() > 0);
        List<String> receipts = new ArrayList<String>();
        for(Submission submission : submissions){
            receipts.add(submission.getReceipt().getReceiptId());
        }
        Collection<Submission> dupSubmissions = dup.getSubmissionsPartnerEntries().values();
        Assert.assertEquals(submissions.size(), dupSubmissions.size());
        for(Submission dupSubmission : dupSubmissions){
            String receipt = dupSubmission.getReceipt().getReceiptId();
            Assert.assertTrue(receipts.contains(receipt) == false);
        }
    }

//    @Test
//    public void ITAC14() {
//        LOGGER.log(Level.DEBUG, "ITAC-14()");
//        List<Proposal> origProps = proposalService.getProposals(10L, new IMatch<Proposal>() {
//            public boolean match(Proposal arg) {
//                return true;
//            }
//        });
//        int origSize = origProps.size();
//
//        Proposal dup = proposalService.duplicate(proposalService.getProposal(10L, 100L));
//        List<Proposal> dupProps = proposalService.getProposals(10L, new IMatch<Proposal>() {
//            public boolean match(Proposal arg) {
//                return true;
//            }
//        });
//        Assert.assertEquals(origSize + 1, dupProps.size());
//        LOGGER.log(Level.DEBUG, "There were " + origSize + " now there are " + dupProps.size());
//
//    }
//
//    @Test
//    public void ITAC127() {
//        LOGGER.log(Level.DEBUG, "ITAC-127()");
//        Proposal orig = proposalService.getProposal(10L, 100L);
//        int originalTargetCatalogCount = orig.getDocument().getCommon().getTargetCatalog().getTargets().size();
//        //Make sure it has some targets
//        Assert.assertTrue(originalTargetCatalogCount > 0);
//        Proposal dup = proposalService.duplicate(orig);
//
//        int dupTargetCatalogCount = dup.getDocument().getCommon().getTargetCatalog().getTargets().size();
//        //Make sure that the original and dup target catalog's targets are the same
//        Assert.assertEquals(dupTargetCatalogCount, originalTargetCatalogCount);
//        Assert.assertEquals(originalTargetCatalogCount, orig.getDocument().getCommon().getTargetCatalog().getTargets().size());
//
//        //Go over the TargetCatalogs targets, make sure that they have been duplicated (i.e., dup contains pointers to new Targets)
//        List<Long> origTargetIds = new ArrayList<Long>();
//        List<Long> dupTargetIds = new ArrayList<Long>();
//        for (Target t : orig.getDocument().getCommon().getTargetCatalog().getTargets()) {
//            origTargetIds.add(t.getId());
//        }
//        for (Target t : dup.getDocument().getCommon().getTargetCatalog().getTargets()) {
//            dupTargetIds.add(t.getId());
//
//            for (Long originalId : origTargetIds) {
//                if (originalId.longValue() == t.getId().longValue()) {
//                    Assert.fail("Found a duplicate ID" + originalId);
//                }
//            }
//        }
//
//        //String origDot = orig.toDot();
//        //String dupDot = dup.toDot();
//        //LOGGER.log(Level.DEBUG, dupDot);
//
//        //Now make sure that targets via TargetCatalog are same as targets via ObservationList (i.e., they are references to TargetCatalog's recently created Targets)
//        confirmTargetFixup(dup);
//    }
//
//    private void confirmTargetFixup(Proposal p) {
//        List<Target> targetCatalogTargets = p.getDocument().getCommon().getTargetCatalog().getTargets();
//        List<Target> observationListTargets = p.getDocument().getObservatory().getObservationList().getTargets();
//        Assert.assertEquals(targetCatalogTargets.size(), observationListTargets.size());
//
//        List<Long> targetCatalogTargetIds = new ArrayList<Long>();
//        List<Long> observationListTargetIds = new ArrayList<Long>();
//        for (Target t : targetCatalogTargets) {
//            targetCatalogTargetIds.add(t.getId());
//        }
//        for (Target t : observationListTargets) {
//            observationListTargetIds.add(t.getId());
//        }
//        //Now compare 'em
//        for (Long tcId : targetCatalogTargetIds) {
//            boolean hasMatch = false;
//            for (Long olId : observationListTargetIds) {
//                if (tcId.longValue() == olId.longValue()) {
//                    hasMatch = true;
//                    break;
//                }
//            }
//            Assert.assertTrue("Could not find match for TargetCatalog.target[" + tcId + "]", hasMatch);
//        }
//    }
//    /*
//    @Test
//    public void canCheckProposal(){
//        LOGGER.log(Level.DEBUG, "canCheckProposal()");
//        Proposal p = proposalService.getProposal(10L, 100L);
//        Set<Proposal> ps = new HashSet<Proposal>();
//        ps.add(p);
//        Set<ProposalIssue> pis = proposalService.checkProposals(ps);
//        Assert.assertNotNull(pis);
//    }
//    */
//
//    @Test
//    public void canRetrieveToItacExtenionsPromptly() {
//        LOGGER.log(Level.DEBUG, "canRetrieveItacExtensionsPromptly()");
//        List<Proposal> ps = proposalService.getProposalswithItacExtension(10L);
//        Assert.assertEquals(4, ps.size());
//        for (Proposal p : ps) {
//            Assert.assertTrue(p.getDocument().getObservatory().getGeminiPart().getItacExtension().getId() > 0);
//        }
//    }
//
//
//    @Test
//    public void itac274() {
//        Proposal p = proposalService.getProposal(10L, 100L);
//        Proposal d = proposalService.duplicate(p);
//        String pXml = new String(proposalService.getProposalAsXml(p));
//        String dXml = new String(proposalService.getProposalAsXml(d));
//        LOGGER.log(Level.DEBUG, dXml);
//        Assert.assertEquals(14, instanceCount(pXml, "targetRef"));
//        Assert.assertEquals(instanceCount(pXml, "targetRef"), instanceCount(dXml, "targetRef"));
//    }
//
//
//    int instanceCount(String container, String search){
//        if(container.indexOf(search) > -1){
//            final String trimmedContainer = container.substring(container.indexOf(search) + 1, container.length());
//            return 1 + instanceCount(trimmedContainer, search);
//        }
//        return 0;
//    }
//
//    @Test
//    public void canFindBySearch(){
//        Set<Proposal> ps = proposalService.search(10L, "101");
//        Assert.assertEquals(1, ps.size());
//        Set<Proposal> ps2 = proposalService.search(10L, "Geb");
//        Assert.assertEquals(3, ps2.size());
//        Set<Proposal> ps3 = proposalService.search(10L, "000");
//        Assert.assertEquals(5, ps3.size());
//        Set<Proposal> ps4 = proposalService.search(10L, "000-20077-9a1c-1db000000000");
//        Assert.assertEquals(1, ps4.size());
//    }
//

}
