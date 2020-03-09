package edu.gemini.tac.util;

import edu.gemini.tac.persistence.*;
import edu.gemini.tac.persistence.joints.JointProposal;
import edu.gemini.tac.persistence.phase1.*;
import edu.gemini.tac.persistence.phase1.blueprint.BlueprintBase;
import edu.gemini.tac.persistence.phase1.proposal.PhaseIProposal;
import edu.gemini.tac.persistence.phase1.proposal.QueueProposal;
import edu.gemini.model.p1.mutable.Band;
import edu.gemini.tac.persistence.phase1.submission.Submission;
import edu.gemini.tac.persistence.phase1.submission.SubmissionAccept;
import edu.gemini.tac.persistence.phase1.submission.SubmissionRequest;
import org.apache.commons.lang.Validate;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.hibernate.Query;
import org.hibernate.Session;
import org.springframework.util.FileCopyUtils;
import org.xml.sax.SAXParseException;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.*;
import java.util.*;

/**
 * Importer to read P1 information from a bunch of file formats that might be thrown at us. The importer
 * also deals with pdf files that belong to the proposal xml files and imports them into a separate folder
 * in the file system.
 *
 * User: fnussber
 * @author fnussber
 * @author ddawson
 * @since .18
 */
public class ProposalImporter {
    private static final Logger LOGGER = Logger.getLogger(ProposalImporter.class.getName());

    final private ResultCollection results = new ResultCollection();
    final private Set<String> deletedProposals = new HashSet<String>();
    private boolean replaceProposals;
    private File pdfFolder;

    public enum State {
        IMPORTED    ("Successfully imported."),
        MERGED      ("Merged into new joint proposal."),
        ADDED2JOINT ("Added to existing joint proposal."),
        FAILED      ("Import failed!"),
        INVALID     ("Invalid xml!");

        private String description;
        private State(final String description) {
            this.description = description;
        }
        public String getDescription() {    // needed in jsp file
            return description;
        }
    };

    /**
     * Creates a proposal importer that keeps track of the state of import operations.
     */
    public ProposalImporter(File pdfFolder) {
        this(false, pdfFolder);
    }

    public ProposalImporter(boolean replaceProposals, File pdfFolder) {
        this.replaceProposals = replaceProposals;
        this.pdfFolder = pdfFolder;
    }
    

    /**
     * Gets detailed information about the results of the import operation.
     * @return
     */
    public ResultCollection getResults() {
        return this.results;
    }

    public String getStatistics() {
        StringBuilder sb = new StringBuilder();
        sb.append(getSuccessfulCount());
        sb.append(getSuccessfulCount() == 1 ? " document" : " documents");
        sb.append(" successful, ");
        sb.append(getFailedCount());
        sb.append(getFailedCount() == 1 ? " document" : " documents");
        sb.append(" failed");
        return sb.toString();
    }

    public int getSuccessfulCount() {
        return this.results.getSuccessfulCount();
    }

    public int getFailedCount() {
        return this.results.getFailedCount();
    }

    public List<Result> importDocuments(final Session session, final String fileName, final InputStream inputStream, final Committee committee) {
        final ProposalUnwrapper unwrapper = new ProposalUnwrapper(inputStream, fileName);
        final List<ProposalUnwrapper.Entry> unwrappedFiles = unwrapper.getEntries();
        for (ProposalUnwrapper.Entry unwrappedFile : unwrappedFiles) {
            LOGGER.log(Level.DEBUG, "importing document " + unwrappedFile.getFileName());
            LOGGER.debug("Importing " + unwrappedFile.toString());
            try {
                importSingleDocument(session, unwrappedFile, committee);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        unwrapper.cleanup();
        return results;
    }

    public List<Result> importSingleDocument(final Session session, final ProposalUnwrapper.Entry unwrappedFile, final Committee committee) {
        try {
            doImportSingleDocument(session, unwrappedFile, committee);
        } catch (Exception e) {
            LOGGER.debug("Problem importing file " + unwrappedFile.getFileName(), e);
            // throw runtime exception to make sure that the transaction is rolled back
            // (transaction handling is the responsibility of the caller!)
            if (e.getCause() instanceof SAXParseException) {
                // this error was thrown during schema validation, this was most probably caused by manual editing...
                results.add(new Result(unwrappedFile, State.INVALID, "File is invalid, was it edited manually? Cause: " + e.getCause().getMessage()));
                throw new RuntimeException("File " + unwrappedFile.getFileName() + " is invalid.", e);
            } else {
                results.add(new Result(unwrappedFile, State.FAILED, e.getMessage()));
                throw new RuntimeException("Import of proposal " + unwrappedFile.getFileName() + " failed.", e);
            }
        }
        return results;
    }

    /**
     * Imports a single proposal document from an input stream, this is the main worker method that actually does the import
     * including creation of the proposal object.
     * @param session
     * @param unwrappedFile
     * @param committee
     * @return
     * @throws JAXBException
     */
    private void doImportSingleDocument(final Session session, final ProposalUnwrapper.Entry unwrappedFile, final Committee committee) throws JAXBException, IOException {
        // some preconditions; make sure that xml and pdfs come in pairs
        Validate.notNull(unwrappedFile);
        Validate.notNull(unwrappedFile.getFileName());
        Validate.notNull(unwrappedFile.getPdf());
        Validate.notNull(unwrappedFile.getProposal());
        
        // now do something useful..
        PhaseIProposal phaseIProposal = unmarshal(session, unwrappedFile);
        String key = phaseIProposal.getSubmissionsKey();
        List<Proposal> proposals = getProposalsForKey(session, committee, key);
        
        Set<Observation> northObs = phaseIProposal.getObservationsForSite(Site.NORTH);
        Set<Observation> southObs = phaseIProposal.getObservationsForSite(Site.SOUTH);
        if ((northObs.size()) > 0 &&  (southObs.size() > 0)) {
            // this is a mixed proposal with observations for instruments in the north and the south -> results in two separate proposals in itac
            PhaseIProposal northProposal = cloneProposalForSite(phaseIProposal, Site.NORTH);
            PhaseIProposal southProposal = cloneProposalForSite(phaseIProposal, Site.SOUTH);

            doImport(session, filterProposalsForSite(proposals, Site.NORTH), northProposal, unwrappedFile, committee);
            doImport(session, filterProposalsForSite(proposals, Site.SOUTH), southProposal, unwrappedFile, committee);

        } else  {

            doImport(session, filterProposalsForSite(proposals, phaseIProposal.getSite()), phaseIProposal, unwrappedFile, committee);
        }

    }

    private PhaseIProposal unmarshal(Session session, ProposalUnwrapper.Entry unwrappedFile) throws JAXBException, IOException {
        PhaseIProposal phaseIProposal = null;
        InputStream inputStream = null;
        try {
            LOGGER.debug("Unmarshalling...");
            inputStream = new FileInputStream(unwrappedFile.getProposal());
            JAXBContext jaxbContext = JAXBContext.newInstance("edu.gemini.model.p1.mutable");
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
            // use the schema provided by the schema validator for validation during import
            unmarshaller.setSchema(ProposalSchemaValidator.getSchema());
            edu.gemini.model.p1.mutable.Proposal mutableProposal = (edu.gemini.model.p1.mutable.Proposal) unmarshaller.unmarshal(inputStream);
            LOGGER.debug("Unmarshalled " + mutableProposal.toString());
            List<Partner> partners = session.getNamedQuery("partner.findAllPartners").list();
            phaseIProposal = PhaseIProposal.fromMutable(mutableProposal, partners);
            LOGGER.debug("Converted " + phaseIProposal);
        } finally {
            if (inputStream != null) {
                inputStream.close();
            }
        }
        return phaseIProposal;
    }


    /**
     * Checks a set of conditions that are not / can not be validated using the xml schema because they're depending
     * on the actual state of the submission process. This could be done using separate schemas for the different
     * stages, but we choose to not go down that route (at least not for now).
     * @param phaseIProposal
     */
    private void preImportValidation(final PhaseIProposal phaseIProposal) {
        Validate.notEmpty(phaseIProposal.getSubmissionsKey(), "Proposals must have a valid submission key.");
        Validate.notEmpty(phaseIProposal.getSubmissions(), "Proposals must have at least on submission.");
        boolean accepted = false;
        boolean hasRanking = false;
        boolean hasPositiveRanking = false;
        for (Submission s: phaseIProposal.getSubmissions()) {
            accepted = s.getAccept() != null;
            if (accepted) {
                hasRanking = s.getAccept().getRanking() != null;
                if (hasRanking) {
                    hasPositiveRanking = s.getAccept().getRanking().doubleValue() > 0;
                }
                break;
            }
        }
        Validate.isTrue(accepted, "Proposal submission must be accepted");
        Validate.isTrue(hasRanking, "Proposal submission must contain a ranking");
        Validate.isTrue(hasPositiveRanking, "Proposal ranking must be positive");
    }

    /**
     * Gets all proposals for a given key.
     * Depending on current replacement mode for already existing proposals any existing proposals with the given
     * key will be deleted at the start of an import run.
     * @param session
     * @param committee
     * @param key
     * @return
     */
    private List<Proposal> getProposalsForKey(Session session, Committee committee, String key) {
        List<Proposal> proposals = session.getNamedQuery("proposal.findProposalBySubmissionsKey").
                setString("submissionsKey", key).
                setLong("committeeId", committee.getId()).
                list();

        // --- delete all already existing proposals/joints with this key if REPLACE mode is activated and
        // --- proposals with this key have not yet been deleted in this importer run
        if (replaceProposals) {
            if (!deletedProposals.contains(key)) {
                //Hibernate gets confused if we delete JPs in-line, so gather them up for delayed delete
                Set<Proposal> jointToBeDeleted = new HashSet<Proposal>();
                // delete all proposals and their pdf attachments. Gather parent JPs for delayed delete
                for (Proposal p : proposals) {
                    p.getPdfLocation(pdfFolder).delete();
                    final JointProposal maybeJointProposal = p.getJointProposal();
                    if(maybeJointProposal != null){
                        jointToBeDeleted.add(maybeJointProposal);
                    }
                    p.delete(session);
                }
                //Delete the JointProposals that were gathered in the previous loop
                for(Proposal jp : jointToBeDeleted){
                    jp.delete(session);
                }
                // mark key as deleted, don't try to delete it again in the same import run
                deletedProposals.add(key);
                // empty the list of proposals (restart import)
                proposals.clear();
            }
        }

        // return all propsals with the same key (for building joints if necessary)
        return proposals;
    }

    /**
     * Returns all proposals for a given site from a list of proposals.
     * @param proposals
     * @param targetSite
     * @return
     */
    private List<Proposal> filterProposalsForSite(Collection<Proposal> proposals, Site targetSite) {
        List<Proposal> sameSiteProposals = new ArrayList<Proposal>();
        for (Proposal p : proposals) {
            if (p.getSite() == targetSite) {
                sameSiteProposals.add(p);
            }
        }
        return sameSiteProposals;
    }

    /**
     * Does the actual import for a new phase1proposal with a given key and for a given site.
     * @param session
     * @param proposalsWithSameKeyAndSite
     * @param phaseIProposal
     * @param file
     * @param committee
     * @return
     * @throws JAXBException
     */
    private Result doImport(final Session session, final List<Proposal> proposalsWithSameKeyAndSite, final PhaseIProposal phaseIProposal, ProposalUnwrapper.Entry file, final Committee committee) throws IOException {
        // do additional pre-import validation
        preImportValidation(phaseIProposal);        

        // start actual import: normalize phase1proposal and create proposal
        normalize(session, phaseIProposal);
        session.saveOrUpdate(phaseIProposal);
        session.flush(); // flush to provoke errors early
        // create the new proposal
        Proposal newProposal = createProposal(session, phaseIProposal, committee);
        LOGGER.debug("Created new proposal " + newProposal);

        // prepare result
        String msg;
        if (phaseIProposal.isLargeProgram()) {
            msg = String.format("%d observations for Large Proposal on Gemini %s (ID=%d)", newProposal.getObservations().size(), newProposal.getSite().getDisplayName(), newProposal.getId());
        } else if (phaseIProposal.isExchange()) {
            msg = String.format("%d observations for Exchange %s (ID=%d)", newProposal.getObservations().size(), newProposal.getExchangeFor(), newProposal.getId());
        } else {
            msg = String.format("%d observations for Gemini %s (ID=%d)", newProposal.getObservations().size(), newProposal.getSite().getDisplayName(), newProposal.getId());
        }
        final Result result = new Result(file, State.IMPORTED, msg, newProposal);

        // keep track of joint proposals
        // - more than one with this submission key already exist: add to existing joint proposal
        if (proposalsWithSameKeyAndSite.size() > 1) {
            // JointProposal jp = p1proposals.get(0).getParent().getJointProposal(); // unfortunately this does not work in multi-file import (?)
            JointProposal jp = getJointProposal(proposalsWithSameKeyAndSite, session); // therefore we have to actively look for the joint proposal...
            jp.add(newProposal, session);
            session.flush();
            // make sure the changed joint proposal is still valid
            jp.validate();
            result.setState(State.ADDED2JOINT);

        // - exactly one proposal with this submission key already exists: create new joint proposal
        } else if (proposalsWithSameKeyAndSite.size() == 1) {
            JointProposal jp = new JointProposal();
            Proposal primary = proposalsWithSameKeyAndSite.get(0);
            jp.add(primary, session);       // proposal added first will be the primary
            jp.add(newProposal, session);
            session.save(jp);
            session.flush();
            // make sure the new joint proposal is valid
            jp.validate();
            result.setState(State.MERGED);
        }

        // make sure committee folder for pdf exists ..
        File committeeFolder = new File(pdfFolder.getAbsolutePath()+File.separator+committee.getId());
        committeeFolder.mkdirs();
        // .. and copy the pdf that belongs to the proposal as ../<committeeId>/<proposalId>.pdf
        File pdf = new File(committeeFolder+File.separator+result.getProposalId()+".pdf");
        FileCopyUtils.copy(file.getPdf(), pdf);

        results.add(result);

        return result;
    }
    
    private PhaseIProposal cloneProposalForSite(PhaseIProposal source, Site site) {
        PhaseIProposal clone = source.memberClone();
        Set<Observation>   thisSiteObs = clone.getObservationsForSite(site);
        Set<BlueprintBase> thisSiteBps = clone.getBlueprintsForSite(site);
        Set<Target>        thisSiteTgt = clone.getTargetsForSite(site);
        Set<Condition>     thisSiteCnd = clone.getConditionsForSite(site);
        clone.setObservations(thisSiteObs);
        clone.setBlueprints(new ArrayList<BlueprintBase>(thisSiteBps));
        clone.setTargets(new ArrayList<Target>(thisSiteTgt));
        clone.setConditions(new ArrayList<Condition>(thisSiteCnd));

        // do some time accounting: split requested times between sites

        // -- step one: sum site specific band times and total band times
        TimeAmount siteBand12Time = new TimeAmount(0, TimeUnit.HR);
        TimeAmount siteBand3Time = new TimeAmount(0, TimeUnit.HR);
        TimeAmount totalBand12Time = new TimeAmount(0, TimeUnit.HR);
        TimeAmount totalBand3Time = new TimeAmount(0, TimeUnit.HR);
        for (Observation o : source.getObservations()) {
            if (o.getBand() == Band.BAND_1_2) {
                totalBand12Time = totalBand12Time.sum(o.getTime());
                if (o.getBlueprint().getSite() == site) {
                    siteBand12Time = siteBand12Time.sum(o.getTime());
                }
            } else {
                totalBand3Time = totalBand3Time.sum(o.getTime());
                if (o.getBlueprint().getSite() == site) {
                    siteBand3Time = siteBand3Time.sum(o.getTime());
                }
            }
        }

        // -- step two: calculate ratio between site specific and total band12 time,
        // set request and recommended time to scaled total band12 time and
        // adapt min times in case scaled time is smaller than min time
        Validate.isTrue(totalBand12Time.getDoubleValue() > 0, "mixed proposal with band12 time > 0");
        SubmissionRequest request = source.getPrimary().getRequest();
        double band12ratio = siteBand12Time.getDoubleValueInHours() / totalBand12Time.getDoubleValueInHours();
        // scale requested times
        TimeAmount scaledTime = new TimeAmount(
                request.getTime().getDoubleValue() * band12ratio,
                request.getTime().getUnits());
        clone.getPrimary().getRequest().setTime(scaledTime);
        if (scaledTime.compareTo(request.getMinTime()) < 0) {
            clone.getPrimary().getRequest().setMinTime(scaledTime);
        }
        // scale recommended times if applicable
        if (source.getPrimary().getAccept() != null) {
            Validate.notNull(clone.getPrimary().getAccept());
            SubmissionAccept accept = source.getPrimary().getAccept();
            TimeAmount scaledRecTime = new TimeAmount(
                    accept.getRecommend().getDoubleValue() * band12ratio,
                    accept.getRecommend().getUnits());
            clone.getPrimary().getAccept().setRecommend(scaledRecTime);
            if (scaledRecTime.compareTo(accept.getMinRecommend()) < 0) {
                clone.getPrimary().getAccept().setMinRecommend(scaledRecTime);
            }
        }

        // -- step three: do the same for band 3
        if (source.isBand3()) {
            Validate.isTrue(totalBand3Time.getDoubleValue() > 0, "mixed propsal with band3 time == 0");
            Validate.isTrue(clone instanceof QueueProposal); // only queue proposals have band3request part
            SubmissionRequest band3Request = source.getBand3Request();
            double band3ratio = siteBand3Time.getDoubleValueInHours() / totalBand3Time.getDoubleValueInHours();
            TimeAmount scaledBand3Time = new TimeAmount(
                    band3Request.getTime().getDoubleValue() * band3ratio,
                    band3Request.getTime().getUnits());
            // set times
            clone.getBand3Request().setTime(scaledBand3Time);
            if (scaledBand3Time.compareTo(band3Request.getMinTime()) < 0) {
                clone.getBand3Request().setMinTime(scaledBand3Time);
            }
        }
        // end time magic
        
        // make it visible that this has been split by marking title and receiptId with South/North
        String addOn = " ("+site.getDisplayName()+")";
        clone.setTitle(clone.getTitle() + addOn);
        for (Submission s : clone.getSubmissions()) {
            if (s.getReceipt() != null) {
                s.getReceipt().setReceiptId(s.getReceipt().getReceiptId() + addOn);
            }
        }

        return clone;

    }

    /**
     * Finds the joint proposal for this set of proposals by checking its primary proposal id against the ids of
     * the proposals passed in as argument.
     * @param proposals
     * @param session
     * @return
     */
    private JointProposal getJointProposal(List<Proposal> proposals, Session session) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < proposals.size(); i++) {
            sb.append(proposals.get(i).getId());
            if (i < proposals.size()-1) sb.append(",");
        }
        JointProposal jp = (JointProposal) session.createQuery("from JointProposal where primaryProposal.id in ("+sb.toString()+")").uniqueResult();
        Validate.notNull(jp); // there must be a joint proposal!
        return jp;
    }

    private Proposal createProposal(final Session session, final PhaseIProposal phaseIProposal, final Committee committee) {
        final Proposal proposal = new Proposal();
        proposal.setPhaseIProposal(phaseIProposal);
        proposal.setCommittee(committee);
        proposal.setPartner(phaseIProposal.getPrimary().getPartner());
        phaseIProposal.setParent(proposal);
        session.saveOrUpdate(proposal);
        session.saveOrUpdate(phaseIProposal);
        session.flush(); // flush to provoke errors early

        // make sure the new proposal is valid
        proposal.validate();

        // return it
        return proposal;
    }

    /**
     * In lieu of using natural keys, we need to fix up links between entities manually.
     *
     * @param session
     * @param proposal
     */
    private void normalize(final Session session, final PhaseIProposal proposal) {
        // ---------- replace new principal investigators with already existing ones from database if applicable
        Query query = session.getNamedQuery("principalInvestigator.findByEmail").
                setParameter("email", proposal.getInvestigators().getPi().getEmail());
        PrincipalInvestigator nPi = (PrincipalInvestigator) query.uniqueResult();
        if (nPi != null) {
            normalizeInvestigatorRefs(proposal, proposal.getInvestigators().getPi(), nPi);
        }

        // ---------- replace new co-investigators with already existing ones from database if applicable
        // ---------- (note: work on a copy of the set to avoid concurrent manipulation exceptions!)
        Set<CoInvestigator> cois = new HashSet<CoInvestigator>(proposal.getInvestigators().getCoi());
        for (CoInvestigator coi : cois) {
            query = session.getNamedQuery("coInvestigator.findByEmail").
                    setParameter("email", coi.getEmail());
            CoInvestigator nCoi = (CoInvestigator) query.uniqueResult();
            if (nCoi != null) {
                normalizeInvestigatorRefs(proposal, coi, nCoi);
            }
        }
    }

    /**
     * Replace all new references to an investigator that refer to an already existing investigator with the
     * existing one. Equality of an investigator is defined by its type and its email address.
     * NOTE: This method will have to be adapted every time the model is changed... sorry 'bout that.
     *
     * @param proposal
     * @param nnew
     * @param existing
     */
    private void normalizeInvestigatorRefs(final PhaseIProposal proposal, final Investigator nnew, final Investigator existing) {
        proposal.normalizeInvestigator(nnew, existing);
    }
    
    public static class ResultCollection  extends ArrayList<Result> {

        public Integer getSuccessfulCount() {
            int i = 0;
            for (Result r : this) { if (r.isSuccessful()) i++; };
            return i;
        }

        public Integer getFailedCount() {
            int i = 0;
            for (Result r : this) { if (!r.isSuccessful()) i++; };
            return i;
        }

        public List<Result> getSuccessful() {
            List<Result> successful = new ArrayList<Result>();
            for (Result r : this) {
                if (r.isSuccessful()) {
                    successful.add(r);
                }
            }
            return successful;
        }
        
        public List<Result> getFailed() {
            List<Result> failed = new ArrayList<Result>();
            for (Result r : this) {
                if (!r.isSuccessful()) {
                    failed.add(r);
                }
            }
            return failed;
        }
    }

    public static class Result {
        private ProposalUnwrapper.Entry file;
        private String message;
        private State resultState;
        private Long proposalId;
        private Long phaseIProposalId;

        public Result(ProposalUnwrapper.Entry file, State state, String message) {
            this(file, state, message, null);
            proposalId = -1L;
            phaseIProposalId = -1L;
        }

        public Result(ProposalUnwrapper.Entry file, State state, String message, Proposal proposal) {
            this.file = file;
            this.resultState = state;
            this.message = message;
            if (proposal != null) {
                this.proposalId = proposal.getId();
                this.phaseIProposalId = proposal.getPhaseIProposal().getId();
            }
        }

        public boolean isSuccessful() {
            return (resultState != State.FAILED && resultState != State.INVALID);
        }

        public void setState(State state) {
            this.resultState = state;
        }

        public String getFileName() {   // needed in jsp file
            return this.file.getFileName();
        }

        public State getState() {
            return this.resultState;
        }

        public String getMessage() {
            return this.message;
        }
        
        public Long getProposalId() {
            return proposalId;
        }

        public Long getPhaseIProposalId() {
            return phaseIProposalId;
        }
    }
}
