package edu.gemini.tac.service;

import edu.gemini.model.p1.mutable.Band;
import edu.gemini.tac.exchange.ProposalExporterImpl;
import edu.gemini.tac.persistence.*;
import edu.gemini.tac.persistence.joints.JointProposal;
import edu.gemini.tac.persistence.phase1.*;
import edu.gemini.tac.persistence.phase1.blueprint.BlueprintBase;
import edu.gemini.tac.persistence.phase1.proposal.ClassicalProposal;
import edu.gemini.tac.persistence.phase1.proposal.PhaseIProposal;
import edu.gemini.tac.persistence.phase1.proposal.QueueProposal;
import edu.gemini.tac.persistence.phase1.sitequality.CloudCover;
import edu.gemini.tac.persistence.phase1.sitequality.ImageQuality;
import edu.gemini.tac.persistence.phase1.sitequality.SkyBackground;
import edu.gemini.tac.persistence.phase1.sitequality.WaterVapor;
import edu.gemini.tac.persistence.phase1.submission.Submission;
import edu.gemini.tac.persistence.phase1.submission.SubmissionAccept;
import edu.gemini.tac.persistence.phase1.submission.SubmissionReceipt;
import edu.gemini.tac.persistence.phase1.submission.SubmissionRequest;
import edu.gemini.tac.persistence.queues.Banding;
import edu.gemini.tac.persistence.queues.Queue;
import edu.gemini.tac.service.joints.JointProposalChecker;
import edu.gemini.tac.util.ProposalExporter;
import edu.gemini.tac.util.ProposalImporter;
import edu.gemini.tac.util.ProposalUnwrapper;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.Validate;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.hibernate.*;
import org.hibernate.criterion.Restrictions;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.FileCopyUtils;

import javax.annotation.Resource;
import java.io.*;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.text.DateFormat;
import java.util.*;

/**
 * Concrete implementation of i'face
 * <p/>
 * <p/>
 * User: lobrien
 * Date: Dec 2, 2010
 */
@Service("proposalService")
@Transactional
public class ProposalHibernateService implements IProposalService {
    private static final Logger LOGGER = Logger.getLogger(ProposalHibernateService.class.getName());

    // pdf storage and access locations
    private File pdfFolder;

    @Resource(name = "sessionFactory")
    private SessionFactory sessionFactory;

    @Resource(name = "committeeService")
    private ICommitteeService committeeService;

    /* Setter methods for Spring configuration */
    public void setPdfFolder(String pdfFolder) {
        this.pdfFolder = new File(pdfFolder);
        this.pdfFolder.mkdirs();
        // check state of pdf folder on web server startup (this is when Spring will call this setter)
        // I assume it's preferable to not start the web application with an invalid configuration instead of producing errors at some random time.
        Validate.isTrue(this.pdfFolder.exists(), "Pdf folder " + this.pdfFolder.getAbsolutePath() + " could not be created! Check configuration.");
        Validate.isTrue(this.pdfFolder.isDirectory(), "Pdf folder " + this.pdfFolder.getAbsolutePath() + " is not a directory! Check configuration.");
        Validate.isTrue(this.pdfFolder.canRead(), "Pdf folder " + this.pdfFolder.getAbsolutePath() + " is not a readable!");
        Validate.isTrue(this.pdfFolder.canWrite(), "Pdf folder " + this.pdfFolder.getAbsolutePath() + " is not a writable!");
    }

    private Proposal findProposalForEditing(final Session session, final Long committeeId, final Long proposalId) {
        ProposalHibernateService.LOGGER.log(Level.DEBUG, "----Seeking ----");
        final Query query =
                session.getNamedQuery("proposal.findProposalForEditingById").
                        setLong("committeeId", committeeId).
                        setLong("proposalId", proposalId);

        if (query.list().size() > 1) {
            ProposalHibernateService.LOGGER.log(Level.DEBUG, "Found " + query.list().size() + " proposals ");
        } else {
            if (query.list().size() == 0) {
                ProposalHibernateService.LOGGER.log(Level.WARN, "Could not getProposal(" + committeeId + ", " + proposalId + ")");
                final Query q = session.getNamedQuery("jointproposal.findForEditingById").
                        setLong("committeeId", committeeId).
                        setLong("proposalId", proposalId);
                Proposal jp = getJointProposal(session, q);
                return jp;
            }
        }
        final Proposal proposal = (Proposal) query.uniqueResult();
        return proposal;
    }

    /**
     * This method is designed to populate proposals efficiently with all data that is needed for the
     * queue engine. Handle with care.
     * @param p
     * @return
     */
    @Transactional
    @Override
    public Proposal populateProposalForQueueCreation(Proposal p) {
        Hibernate.initialize(p);
        Hibernate.initialize(p.getItac());

        // initialize all proxy objects that will be accessed by queue engine
        final PhaseIProposal phaseIProposal = p.getPhaseIProposal();
        Hibernate.initialize(p.getItac());
        if (p.getJointProposal() != null) {
            Hibernate.initialize(p.getJointProposal());
            Hibernate.initialize(p.getItac());
        }
        Hibernate.initialize(phaseIProposal);
        Hibernate.initialize(phaseIProposal.getBand3Request());
        Hibernate.initialize(phaseIProposal.getConditions());
        Hibernate.initialize(phaseIProposal.getTargets());
        Hibernate.initialize(phaseIProposal.getBlueprints());
        Hibernate.initialize(phaseIProposal.getItac());
        Hibernate.initialize(phaseIProposal.getInvestigators());
        Hibernate.initialize(phaseIProposal.getInvestigators().getPi());
        Hibernate.initialize(phaseIProposal.getObservations());
        Hibernate.initialize(p.getPartner());

        phaseIProposal.hibernateInitializeSubmissions();

        // load targets and depending objects like coordinates etc. in one go
        Session session = sessionFactory.getCurrentSession();
        final List siderealTargets = session.
                getNamedQuery("target.siderealTargetsForEditingByEntityList").
                setParameterList("targets", phaseIProposal.getTargets()).
                list();
        final List nonSiderealTargets = session.
                getNamedQuery("target.nonsiderealTargetsForEditingByEntityList").
                setParameterList("targets", phaseIProposal.getTargets()).
                list();
        Hibernate.initialize(siderealTargets);
        Hibernate.initialize(nonSiderealTargets);

        return p;
    }

    @Transactional
    @Override
    public Proposal populateProposalForProposalChecking(final Proposal proposal) {
        final Session currentSession = sessionFactory.getCurrentSession();
        final Proposal mergedProposal = (Proposal) currentSession.merge(proposal);

        Hibernate.initialize(mergedProposal);

        // initialize all proxy objects that will be accessed by proposal checker
        Hibernate.initialize(mergedProposal.getItac());
        if (mergedProposal.getJointProposal() != null) {
            Hibernate.initialize(mergedProposal.getJointProposal());
        }

        for (Proposal p : mergedProposal.getProposals()) {
            final PhaseIProposal phaseIProposal = p.getPhaseIProposal();
            Hibernate.initialize(phaseIProposal);
            Hibernate.initialize(phaseIProposal.getBand3Request());
            Hibernate.initialize(phaseIProposal.getConditions());
            Hibernate.initialize(phaseIProposal.getTargets());
            Hibernate.initialize(phaseIProposal.getBlueprints());
            Hibernate.initialize(phaseIProposal.getItac());
            Hibernate.initialize(phaseIProposal.getInvestigators());
            Hibernate.initialize(phaseIProposal.getInvestigators().getPi());
            Hibernate.initialize(phaseIProposal.getObservations());

            phaseIProposal.hibernateInitializeSubmissions();

            Hibernate.initialize(phaseIProposal.getTargets());

            for (Target t: phaseIProposal.getTargets()) {
                if (t.isSidereal()) {
                    final SiderealTarget siderealTarget = (SiderealTarget) t;
                    Hibernate.initialize(siderealTarget.getCoordinates());
                }
            }

            for (BlueprintBase bb : phaseIProposal.getBlueprints()) {
                bb.getDisplay();
            }
        }

        return mergedProposal;
    }

    /**
     * This method is very heavily relied upon.  It is intended to efficiently pull in everything in this
     * proposal.  Everything.  No lazy proxy collections, no lazy proxy objects.
     *
     * @param committeeId
     * @param proposalId
     * @return
     */
    @Transactional
    @Override
    public Proposal getProposal(final Long committeeId, final Long proposalId) {
        final Session session = sessionFactory.openSession();
        try {
            Proposal proposal = findProposalForEditing(session, committeeId, proposalId);
            if (proposal == null) {
                LOGGER.warn("Could not retrieve proposal " + proposalId + " in committee " + committeeId);
                return null;
            }

            Hibernate.initialize(proposal.getItac());
            Hibernate.initialize(proposal.getIssues());
            for (Proposal p : proposal.getProposals()) {
                Hibernate.initialize(p);

                final PhaseIProposal phaseIProposal = p.getPhaseIProposal();
                Hibernate.initialize(p.getItac());
                if (p.getJointProposal() != null) {
                    Hibernate.initialize(p.getJointProposal());
                    Hibernate.initialize(p.getItac());
                    Hibernate.initialize(p.getJointProposal().getIssues());
                }
                Hibernate.initialize(phaseIProposal);
                Hibernate.initialize(phaseIProposal.getBand3Request());
                Hibernate.initialize(phaseIProposal.getConditions());
                Hibernate.initialize(phaseIProposal.getInvestigators());
                Hibernate.initialize(phaseIProposal.getInvestigators().getPi());
                Hibernate.initialize(phaseIProposal.getInvestigators().getPi().getPhoneNumbers());
                Hibernate.initialize(phaseIProposal.getKeywords());
                Hibernate.initialize(phaseIProposal.getItac());
                Hibernate.initialize(phaseIProposal.getMeta());

                phaseIProposal.hibernateInitializeSubmissions();

                final List<BlueprintBase> blueprints = phaseIProposal.getBlueprints();
                final Set<CoInvestigator> cois = phaseIProposal.getInvestigators().getCoi();
                final Set<Observation> observations = phaseIProposal.getObservations();
                final List<Submission> submissions = phaseIProposal.getSubmissions();
                final List<Target> targets = phaseIProposal.getTargets();
                Hibernate.initialize(blueprints);
                Hibernate.initialize(observations);
                Hibernate.initialize(submissions);
                Hibernate.initialize(targets);
                Hibernate.initialize(cois);

                for (CoInvestigator coi : cois) {
                    Hibernate.initialize(coi);
                    Hibernate.initialize(coi.getPhoneNumbers());
                }

                for (Observation o : observations) {
                    Hibernate.initialize(o.getMetaData());
                    Hibernate.initialize(o.getBand());
                    Hibernate.initialize(o.getGuideStars());
                }

                for (Submission s: submissions) {
                    Hibernate.initialize(s.getRequest());
                    Hibernate.initialize(s.getAccept());
                    Hibernate.initialize(s.getReceipt());
                    Hibernate.initialize(s.getPartner());
                }

                for (Target t: targets) {
                    Hibernate.initialize(t.getEpoch());
                }

                for (BlueprintBase bp : blueprints) {
                    bp.getInstrument();
                    bp.getDisplayAdaptiveOptics();
                    bp.getDisplayCamera();
                    bp.getDisplayDisperser();
                    bp.getDisplayFilter();
                    bp.getDisplayFocalPlaneUnit();
                    bp.getDisplayOther();
                }

                final List<SiderealTarget> siderealTargets = session.getNamedQuery("target.siderealTargetsForEditingByEntityList").setParameterList("targets", targets).list();
                final List nonSiderealTargets = session.getNamedQuery("target.nonsiderealTargetsForEditingByEntityList").setParameterList("targets", targets).list();
                Hibernate.initialize(siderealTargets);
                Hibernate.initialize(nonSiderealTargets);

                if (proposal.isClassical())
                    Hibernate.initialize(((ClassicalProposal) phaseIProposal).getClassicalVisitors());
            }

            return proposal;

        } catch (RuntimeException re) {
            ProposalHibernateService.LOGGER.log(Level.WARN, "Could not getProposal(" + committeeId + ", " + proposalId + ")");
            try {
                ProposalHibernateService.LOGGER.log(Level.DEBUG, "Connection is to " + session.connection().getMetaData().getURL());
            } catch (SQLException se) {
            }
            throw re;
        } finally {
            session.close();
        }
    }

    private JointProposal getJointProposal(Session s, Query q) {
        List l = q.list();

        return (JointProposal) q.uniqueResult();
    }

    private Set<Long> collectIds(Set<Observation> observations) {
        Set<Long> ids = new HashSet<Long>();
        for (Observation o : observations) {
            ids.add(o.getId());
        }
        return ids;
    }

    @Transactional(readOnly = true)
    @Override
    public byte[] getProposalAsXml(Proposal proposal) {
        final Proposal mergedProposal = (Proposal) sessionFactory.getCurrentSession().merge(proposal);
        ProposalExporter exporter = new ProposalExporterImpl(sessionFactory.getCurrentSession());
        LOGGER.log(Level.DEBUG, "reading p1 document with id " + mergedProposal.getId());
        byte[] bytes = exporter.getAsXml(mergedProposal);
        return bytes;
    }

    @Override
    public byte[] getProposalPdf(Proposal proposal, File pdfFolder) {
        try {
            File pdf = proposal.getPdfLocation(pdfFolder);
            LOGGER.log(Level.DEBUG, "reading pdf document from " + pdf.getAbsolutePath());
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            InputStream inputStream;
            if (pdf.exists()) {
                inputStream = new FileInputStream(pdf);
            } else {
                inputStream = getClass().getResourceAsStream("/edu/gemini/tac/util/empty.pdf");
            }
            FileCopyUtils.copy(inputStream, outputStream);
            return outputStream.toByteArray();
        } catch (Exception e) {
            LOGGER.error("reading pdf file failed", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void saveProposal(final Proposal proposal) {
        final Session session = sessionFactory.getCurrentSession();
        if (proposal.getId() != null) {
            session.update(proposal);
        }
        session.save(proposal);
    }

    @Override
    public void saveEditedProposal(final Proposal proposal) {
        final Session session = sessionFactory.getCurrentSession();
        session.saveOrUpdate(proposal);
    }

    /**
     * Reverses the Proposal's sites.
     *
     * @param proposal
     */
    @Override
    @Transactional
    public void switchSites(Proposal proposal) {
        ProposalHibernateService.LOGGER.log(Level.DEBUG, String.format("ProposalHibernateService.switchSites(%d)", proposal.getId()));
        sessionFactory.getCurrentSession().update(proposal);
        Site current = proposal.getPhaseIProposal().getSite();
        Site other = current.otherSite();
        proposal.switchTo(other);
        saveEditedProposal(proposal);
        ProposalHibernateService.LOGGER.log(Level.DEBUG, "Saved proposal ok");
    }

    @Override
    @Transactional
    public void importDocuments(ProposalImporter importer, String fileName, InputStream inputStream, Long committeeId) {
        Committee committee = committeeService.getCommittee(committeeId);
        Validate.notNull(committee);
        importer.importDocuments(sessionFactory.getCurrentSession(), fileName, inputStream, committee);
    }

    @Override
    @Transactional
    public void importSingleDocument(ProposalImporter importer, ProposalUnwrapper.Entry importFile, Long committeeId) {
        Committee committee = committeeService.getCommittee(committeeId);
        Validate.notNull(committee);
        importer.importSingleDocument(sessionFactory.getCurrentSession(), importFile, committee);
    }

    @Override
    @Transactional
    public List<Proposal> getProposals(Long committeeId, IMatch<Proposal> where) {
        LOGGER.log(Level.DEBUG, String.format("getProposals()"));
        Session session = sessionFactory.getCurrentSession();
        final Query query =
                session.getNamedQuery("proposal.findProposalsByCommittee").
                        setLong("committeeId", committeeId);

        String s = query.getQueryString();
        LOGGER.log(Level.DEBUG, "Found " + query.list().size() + " proposals ");
        List<Proposal> filtered = new ArrayList<Proposal>();
        for (Object p : query.list()) {
            if (where.match(((Proposal) p))) {
                filtered.add(((Proposal) p));
            }
        }
        LOGGER.log(Level.DEBUG, "Filtered to " + filtered.size() + " proposals");
        return filtered;
    }

    /**
     * Checks a single proposal and saves all pending issues in the database.
     *
     * @param proposal
     */
    @Override
    @Transactional
    public void checkProposal(Proposal proposal) {
        List<Proposal> list = new LinkedList<Proposal>();
        list.add(proposal);
        checkProposals(list);
    }

    /**
     * Checks a collection of propsals and saves all pending issues in the database.
     *
     * @param proposals
     */
    @Override
    @Transactional
    public void checkProposals(Collection<Proposal> proposals) {
        final Set<edu.gemini.tac.service.check.ProposalIssue> issues = issuesFor(proposals);

        // collect all issues that belong to the same proposal
        final Map<Proposal, Set<ProposalIssue>> issueMap = new HashMap<Proposal, Set<ProposalIssue>>();
        for (edu.gemini.tac.service.check.ProposalIssue scalaIssue : issues) {
            Set<ProposalIssue> issueSet = issueMap.get(scalaIssue.proposal());
            if (issueSet == null) {
                issueSet = new HashSet<ProposalIssue>();
                issueMap.put(scalaIssue.proposal(), issueSet);
            }
            // translate from Scala to Java, java class is Hibernate annotated to make it persistent.
            // Scala classes are not aware of Hibernate...
            ProposalIssue issue =
                    new ProposalIssue(
                            scalaIssue.proposal(),
                            scalaIssue.severity().ordinal(),
                            scalaIssue.message(),
                            scalaIssue.category());
            // add translated issue
            issueSet.add(issue);
        }

        // assign collected issue sets to proposals and store them
        for (Proposal proposal : proposals) {
            // delete all currently existing issues
            proposal.getIssues().clear();
            // collect new ones (if there are any)
            Set<ProposalIssue> issueSet = issueMap.get(proposal);
            if (issueSet != null) {
                // set issues
                for (ProposalIssue issue : issueSet) {
                    proposal.getIssues().add(issue);
                }
            }

            // make issues persistent
            sessionFactory.getCurrentSession().update(proposal);
        }
    }

    @Transactional
    private Set<edu.gemini.tac.service.check.ProposalIssue> issuesFor(Collection<Proposal> proposals) {
        // check all proposals in one go...
        final Set<edu.gemini.tac.service.check.ProposalIssue> issues = new HashSet<edu.gemini.tac.service.check.ProposalIssue>();
        final JointProposalChecker jointProposalChecker = new JointProposalChecker();

        issues.addAll(edu.gemini.tac.service.check.impl.ProposalChecker.exec(proposals));
        issues.addAll(jointProposalChecker.exec(proposals));

        return issues;
    }

    @Override
    @Transactional
    public Set<Proposal> search(Long committeeId, String searchString) {
        Map<Long, Proposal> ps = new HashMap<Long, Proposal>();
        addIfEntityNotPresent(ps, searchByEntityId(committeeId, searchString));
        addIfEntityNotPresent(ps, partialSearchByPILastName(committeeId, searchString));
        addIfEntityNotPresent(ps, searchByProposalKey(committeeId, searchString));
        return new HashSet<Proposal>(ps.values());
    }

    void addIfEntityNotPresent(Map<Long, Proposal> ps, List proposals) {
        for (Object o : proposals) {
            LOGGER.log(Level.DEBUG, o);
            Proposal p = (Proposal) o;
            if (ps.containsKey(p.getEntityId()) == false) {
                try {
                    ps.put(p.getEntityId(), p);
                } catch (Exception x) {
                    LOGGER.log(Level.ERROR, "Data incomplete in proposal " + p.getId());
                }
            }
        }

    }

    List searchByEntityId(Long committeeId, String searchString) {
        try {
            Integer.parseInt(searchString);
            Session session = sessionFactory.getCurrentSession();
            final Query query = 
                    session.createQuery("from Proposal p " +
                            "join fetch p.committee c " +
                            "where c.id = :committeeId " +
                            "and p.id = :searchString").setParameter("committeeId", committeeId).setParameter("searchString", Long.valueOf(searchString));

            final List proposals = query.list();
            populateForAddProposal(session, proposals);

            return proposals;
        } catch (NumberFormatException nfe) {
            //Not a number
        }
        return new ArrayList();
    }

    List partialSearchByPILastName(Long committeeId, String searchString) {
        final Session session = sessionFactory.getCurrentSession();

        // Removing efficient locate search as we can't join through a collection (phaseIProposal _set_)
        final Query query = session.createQuery("from Proposal p " +
                "join fetch p.committee c " +
                "join fetch p.phaseIProposal p1p " +
                "join fetch p1p.investigators i " +
                "join fetch i.pi " +
                "where c.id = :committeeId").setParameter("committeeId", committeeId);

        final List<Proposal> list = query.list();
        final List<Proposal> contains = new ArrayList<Proposal>();
        for (Proposal p : list) {
            if (p.getPhaseIProposal().getInvestigators().getPi().getLastName().contains(searchString))
                contains.add(p);
        }

        populateForAddProposal(session, contains);

        return contains;
    }

    private void populateForAddProposal(Session session, List<Proposal> list) {
        Hibernate.initialize(session.createQuery("from Partner").list());
        Hibernate.initialize(session.createQuery("from PrincipalInvestigator").list());
        final List<Submission> submissions = new ArrayList<Submission>();
        for (Proposal p : list) {
            submissions.addAll(p.getPhaseIProposal().getSubmissions());
        }
        if (submissions.size() > 0)
            session.createQuery("from Submission s where s in (:submissions)").setParameterList("submissions",submissions).list();
    }

    List searchByProposalKey(Long committeeId, String searchString) {
        Session session = sessionFactory.getCurrentSession();
        final Query query = session.createQuery("from Proposal p " +
                "join fetch p.committee c " +
                "where c.id = :committeeId)"
        ).setParameter("committeeId", committeeId);
        final List<Proposal> proposals = query.list();
        final List<Proposal> contains = new ArrayList<Proposal>();
        for (Proposal p : proposals) {
            if (p.getPhaseIProposal().getSubmissionsKey().contains(searchString))
                contains.add(p);
        }

        populateForAddProposal(session, proposals);

        return proposals;
    }

    /**
     * The target types are remnants of the p1Model and gemini-p1Model version 1 schema that is now gone.
     * They do however have direct analogs.
     * 
     * targetType ::= ['TacExtension', 'GeminiPart', "ITacExtension]
     * TacExtension.field ::= ['partnerRanking', 'partnerComment', 'partnerMinTime', 'partnerTime']
     * GeminiPart.field ::= ['cloudCover', 'imageQuality', 'skyBackground', 'waterVapor']
     * ITacExtension.field ::= ['contactScientistEmail', 'itacComment', 'geminiComment' ]
     * <p/>
     * <p/>
     * This method assumes value is valid! May throw, e.g., parse exception if passed invalid time value.
     * <p/>
     * Note also that this sometimes mutates values (e.g., changing a partner ranking, which is just a number) and
     * other times creates a new
     * <p/>
     * TODO: The mapping could be moved into a bean and the assignment done using reflection. OTOH, is that really clearer or more maintainable than this easy-to-edit and test pattern?
     *
     *
     * @param committeeId
     * @param proposalId
     * @param targetType
     * @param naturalId
     * @param field
     * @param value
     * @param fromValueBuffer An output value containing a string representation of whatever the modified value was previously.
     * @return
     */
    @Override
    @Transactional
    public Proposal editProposalWithAlreadyValidatedData(Long committeeId, Long proposalId, String targetType, String naturalId, String field, String value, StringBuilder fromValueBuffer) {
        Session session = sessionFactory.getCurrentSession();
        Proposal proposal = getProposal(committeeId, proposalId);
        boolean invalidateQueues = false;
        // ITAC-424: make sure that html line breaks (e.g. from copy-pasting text) is replaced with "normal" linebreaks for emails
        value = value.replace("<br>", "\n");
        value = value.replace("<br/>", "\n");
        value = StringEscapeUtils.unescapeHtml(value); // get rid of &nbsp; and the like
        // ITAC-411: get rid of non breaking space (trim() does NOT remove those!)
        // Double.parse() will fail on encountering \u00A0 (&nbsp;) characters
        value = value.replace("\u00A0", " ");
        
        if (targetType.equals("TacExtension")) {
            invalidateQueues = editTacExtension(naturalId, field, value, session, proposal, fromValueBuffer);
        }
        if (targetType.equals("GeminiPart")) {
            invalidateQueues = editGeminiPart(naturalId, field, value, proposal, fromValueBuffer);
        }
        if (targetType.equals("ITacExtension")) {
            invalidateQueues = editITacExtension(naturalId, field, value, proposal, fromValueBuffer);
        }
        if (targetType.equals("GeminiBand3Extension")) {
            invalidateQueues = editGeminiBand3Extension(naturalId, field, value, proposal, fromValueBuffer);
        }
        if (targetType.equals("Observation")) {
            invalidateQueues = editObservations(naturalId, field, value, proposal, fromValueBuffer);
        }
        if (targetType.equals("Proposal")) {
            invalidateQueues = editProposal(naturalId, field, value, proposal, fromValueBuffer);
        }
        if (fromValueBuffer.toString().isEmpty())
            LOGGER.warn("Call to editProposalWithAlreadyValidatedData with parameters committeeId:" + committeeId + "proposalId:" + proposalId + "targetType:" + targetType + " naturalId:" + naturalId + " value:" + value + " generated no \"from\" value to log.");
        session.saveOrUpdate(proposal);

        if (invalidateQueues) {
            List<Banding> bandings = session.createCriteria(Banding.class).add(Restrictions.eq("proposal", proposal)).setFetchMode("queue", FetchMode.EAGER).list();
            for (Banding b : bandings) {
                Queue queue = b.getQueue();
                if (queue != null) {
                    queue.setDirty(true);
                    session.update(queue);
                } else {
                    LOGGER.warn("Banding " + b.getId() + ":" + b + " for proposal " + proposalId + " is missing it's queue.  Data corruption elsewhere?");
                }
            }
        }

        return proposal;
    }

    /**
     * @param naturalId - refers to an "element" id that has sense in the domain, like "DefaultSiteQuality" or "Band3Conditions"
     * @param field     - human reable designation of exactly what kind of thing is being edited.
     * @param value     - new value of field
     * @param proposal  - proposal under edit
     * @return true if the edit invalidates all queues containing a banding referencing the proposal, false if it's harmless.
     */
    private boolean editProposal(final String naturalId,
                                 final String field,
                                 final String value,
                                 final Proposal proposal,
                                 final StringBuilder fromValueBuffer) {
        if (field.equals("checksBypassed")) {
            fromValueBuffer.append(proposal.getChecksBypassed());
            proposal.setChecksBypassed(Boolean.valueOf(value));
        }

        return false;
    }

    /**
     * @param naturalId - refers to an "element" id that has sense in the domain, like "DefaultSiteQuality" or "Band3Conditions"
     * @param field     - human reable designation of exactly what kind of thing is being edited.
     * @param value     - new value of field
     * @param proposal  - proposal under edit
     * @return true if the edit invalidates all queues containing a banding referencing the proposal, false if it's harmless.
     */
    private boolean editObservations(final String naturalId,
                                     final String field,
                                     final String value,
                                     final Proposal proposal,
                                     final StringBuilder fromValueBuffer) {
        Validate.notEmpty(value);
        Validate.notEmpty(naturalId);
        Validate.notNull(proposal);
        Validate.notNull(fromValueBuffer);

        boolean invalidateQueues = true; // All of these edits affect queue filling.
        proposal.setChecksBypassed(Boolean.FALSE); // All of these edits restore the need the eligibility for proposal checks

        final Set<Observation> allObservations = proposal.getAllObservations();
        Observation observation = null;
        for (Observation o : allObservations) {
            if (o.getId().equals(Long.valueOf(naturalId))) {
                observation = o;
                break;
            }
        }
        Validate.notNull(observation, "Non-existent observation passed into editObservations.");

        if (field.equals("observationActive")) {
            fromValueBuffer.append(observation.getActive());
            final boolean newActive = Boolean.parseBoolean(value);
            observation.setActive(newActive);
        }
        if (field.equals("condition")) {
            Condition condition = null;
            for (Condition c : proposal.getConditions()) {
                if (c.getId().equals(Long.decode(value))) {
                    condition = c;
                }
            }
            Validate.notNull(condition);
            fromValueBuffer.append(observation.getCondition().getActiveDisplay());
            observation.setCondition(condition);
        }
        if (field.equals("blueprint")) {
            BlueprintBase blueprintBase = null;
            for (BlueprintBase bb : proposal.getBlueprints()) {
                if (bb.getId().equals(Long.decode(value))) {
                    blueprintBase = bb;
                }
            }
            fromValueBuffer.append(observation.getBlueprint().getDisplay());
            Validate.notNull(blueprintBase);
            observation.setBlueprint(blueprintBase);
        }
        if (field.equals("band")) {
            final Band band = Band.valueOf(value);
            fromValueBuffer.append(observation.getBand());
            Validate.notNull(band);
            observation.setBand(band);
        }

        return invalidateQueues;
    }

    /**
     * @param naturalId - refers to an "element" id that has sense in the domain, like "DefaultSiteQuality" or "Band3Conditions"
     * @param field     - human reable designation of exactly what kind of thing is being edited.
     * @param value     - new value of field
     * @param proposal  - proposal under edit
     * @return true if the edit invalidates all queues containing a banding referencing the proposal, false if it's harmless.
     */
    private boolean editGeminiBand3Extension(String naturalId, String field, String value, Proposal proposal, StringBuilder fromValueBuffer) {
        boolean invalidateQueues = true; // All of these edits affect queue filling.
        proposal.setChecksBypassed(Boolean.FALSE); // All of these edits restore the need the eligibility for proposal checks

        final boolean classical = proposal.isClassical();
        final boolean forExchangePartner = proposal.isForExchangePartner();
        Validate.isTrue(proposal.getPhaseIProposal() instanceof QueueProposal);
        final QueueProposal queueProposal = (QueueProposal) proposal.getPhaseIProposal();

        if (classical) {
            fromValueBuffer.append("Unable to toggle band 3 on a classical proposal.");
            return false;
        }
        if (forExchangePartner) {
            fromValueBuffer.append("Unable to toggle band 3 on an exchange proposal.");
            return false;
        }

        final SubmissionRequest band3Request = queueProposal.getBand3Request();
        if (field.equals("band3eligible")) {
            boolean containsBand12 = false;
            boolean containsBand3 = false;
            for (Observation o : proposal.getObservations()) {
                if (o.getBand().equals(Band.BAND_1_2))
                    containsBand12 = true;
                else if (o.getBand().equals(Band.BAND_3))
                    containsBand3 = true;
            }

            final boolean containsOnlyOneBandOfObservations = containsBand12 ^ containsBand3;
            // Do not operate on mixed sets of observations.
            if (!containsOnlyOneBandOfObservations) {
                fromValueBuffer.append("Proposal contains mixed observations between bandings.  Cannot toggle status.");
                return false;
            }

            if (containsBand3) {
                fromValueBuffer.append("Toggled all observations from band 3 to band1/2.");
            }
            else  if (containsBand12) {
                fromValueBuffer.append("Toggled all observations from band 1/2 to band 3.");
                if (band3Request == null) {
                    final SubmissionRequest newBand3Request = new SubmissionRequest();
                    TimeAmount timeAccumulator = new TimeAmount(BigDecimal.ZERO, TimeUnit.HR);
                    for (Observation o : proposal.getObservations()) {
                        timeAccumulator = timeAccumulator.sum(o.getTime());
                    }
                    newBand3Request.setMinTime(new TimeAmount(timeAccumulator));
                    newBand3Request.setTime(new TimeAmount(timeAccumulator));
                    queueProposal.setBand3Request(newBand3Request);
                }

            } else {
                LOGGER.error("Something has slipped through, this only operates on single band state groups of observations");
                throw new IllegalStateException("Something is broken in the logic above.  See editGeminiBand3Extension.");
            }

            for (Observation o : proposal.getObservations()) {
                if (containsBand3)
                    o.setBand(Band.BAND_1_2);
                else if (containsBand12)
                    o.setBand(Band.BAND_3);
            }

        }

        LOGGER.debug("invalidateQueues:" + invalidateQueues);
        return invalidateQueues;
    }


    /**
     *
     * @param naturalId - refers to an "element" id that has sense in the domain, like "DefaultSiteQuality" or "Band3Conditions"
     * @param field     - human reable designation of exactly what kind of thing is being edited.
     * @param value     - new value of field
     * @param proposal  - proposal under edit
     * @param fromValueBuffer
     * @return true if the edit invalidates all queues containing a banding referencing the proposal, false if it's harmless.
     */
    private boolean editITacExtension(String naturalId, String field, String value, Proposal proposal, StringBuilder fromValueBuffer) {
        //assert(naturalId == "Default")
        boolean invalidateQueues = false; // None of these edits affect queue filling.
        Itac itac = proposal.getItac();
        if (field.equals("contactScientistEmail")) {
            fromValueBuffer.append(itac.getAccept().getContact());
            itac.getAccept().setContact(value);
        }
        if (field.equals("partnerScientistEmail")) {
            fromValueBuffer.append(itac.getAccept().getEmail());
            itac.getAccept().setEmail(value);
        }
        if (field.equals("itacComment")) {
            fromValueBuffer.append(itac.getComment());
            itac.setComment(value);
        }
        if (field.equals("geminiComment")) {
            fromValueBuffer.append(itac.getGeminiComment());
            itac.setGeminiComment(value);
        }

        return invalidateQueues;
    }

    /**
     *
     * @param naturalId - refers to an "element" id that has sense in the domain, like "DefaultSiteQuality" or "Band3Conditions"
     * @param field     - human reable designation of exactly what kind of thing is being edited.
     * @param value     - new value of field
     * @param proposal  - proposal under edit
     * @param fromValueBuffer
     * @return true if the edit invalidates all queues containing a banding referencing the proposal, false if it's harmless.
     */
    private boolean editGeminiPart(String naturalId, String field, String value, Proposal proposal, StringBuilder fromValueBuffer) {
        boolean invalidateQueues = true; // All of the edits in this method modify site qualities which will invalidate the queue
        proposal.setChecksBypassed(Boolean.FALSE); // All of these edits restore the need the eligibility for proposal checks
        Condition condition = null;
        Long observationId = Long.parseLong(naturalId);
        final Set<Observation> observations = proposal.getObservations();
        for (Observation o : observations) {
            if (o.getCondition().getId().equals(observationId)) {
                condition = o.getCondition();
                break;
            }
        }
        if (condition == null) { // Condition is not associated with an observation (legal by schema).
            for (Condition c: proposal.getConditions()) {
                if (c.getId().equals(Long.parseLong(naturalId))) {
                    condition = c;
                    break;
                }
            }
        }

        Validate.isTrue(condition != null, "Could not find condition for editing");

        if (field.equals("CloudCover")) {
            LOGGER.log(Level.DEBUG, "Setting site quality cloud cover to " + value);

            CloudCover cloudCover = CloudCover.fromValue(value);
            fromValueBuffer.append(condition.getCloudCover().getDisplayName());            
            condition.setCloudCover(cloudCover);
        }
        if (field.equals("ImageQuality")) {
            LOGGER.log(Level.DEBUG, "Setting site quality image quality to " + value);
            ImageQuality iq = ImageQuality.fromValue(value);
            fromValueBuffer.append(condition.getImageQuality().getDisplayName());
            condition.setImageQuality(iq);
        }
        if (field.equals("SkyBackground")) {
            LOGGER.log(Level.DEBUG, "Setting site quality sky background to " + value);
            SkyBackground sb = SkyBackground.fromValue(value);
            fromValueBuffer.append(condition.getSkyBackground().getDisplayName());
            condition.setSkyBackground(sb);
        }
        if (field.equals("WaterVapor")) {
            LOGGER.log(Level.DEBUG, "Setting site quality water vapor to " + value);
            WaterVapor wv = WaterVapor.fromValue(value);
            fromValueBuffer.append(condition.getWaterVapor().getDisplayName());
            condition.setWaterVapor(wv);
        }

        return invalidateQueues;
    }


    /**
     *
     * @param naturalId - refers to an "element" id that has sense in the domain, like "DefaultSiteQuality" or "Band3Conditions"
     * @param field     - human reable designation of exactly what kind of thing is being edited.
     * @param value     - new value of field
     * @param proposal  - proposal under edit
     * @param fromValueBuffer
     * @return true if the edit invalidates all queues containing a banding referencing the proposal, false if it's harmless.
     */
    private boolean editTacExtension(String naturalId, String field, String value, Session session, Proposal proposal, StringBuilder fromValueBuffer) {
        boolean edited = false;
        boolean invalidateQueues = false; // Some of the edits in this method modify will invalidate the queue

        Submission submission = null;
        final List<Submission> submissions = proposal.getPhaseIProposal().getSubmissions();
        for (Submission s: submissions) {
            if (s.getPartner().getPartnerCountryKey().equals(naturalId))
                submission = s;
        }
        Validate.notNull(submission);
        final SubmissionAccept accept = submission.getAccept();
        if (field.equals("partnerRanking")) {
            
            LOGGER.log(Level.DEBUG, "Setting partner ranking");
            fromValueBuffer.append(accept.getRanking());
            accept.setRanking(new BigDecimal(value));
            edited = true;
            invalidateQueues = true;
        }
        if (field.equals("partnerComment")) {
            LOGGER.log(Level.DEBUG, "Setting partner comment");

            for (Submission s: submissions)
                if (s.getPartner().getPartnerCountryKey().equals(naturalId)) {
                    fromValueBuffer.append(s.getComment());
                    s.setComment(value);
                }
            edited = true;
        }
        if (field.equals("ntacComment")) {
            LOGGER.log(Level.DEBUG, "Setting ntac comment");

            final Submission primary = proposal.getPhaseIProposal().getPrimary();
            fromValueBuffer.append(primary.getComment());
            primary.setComment(value);

            edited = true;
        }
        if (field.equals("partnerMinTime")) {
            LOGGER.log(Level.DEBUG, "Setting min time OF " + naturalId + " to " + value + " hours");
            
            fromValueBuffer.append(accept.getMinRecommend().toPrettyString());
            final TimeAmount oldTime = accept.getMinRecommend();
            accept.setMinRecommend(null);
            final TimeAmount t = new TimeAmount(new BigDecimal(value), TimeUnit.HR);
            accept.setMinRecommend(t);

            edited = true;
            proposal.setChecksBypassed(Boolean.FALSE);
            invalidateQueues = true;
        }

        if (field.equals("partnerTime")) {
            LOGGER.log(Level.DEBUG, "Setting partner reco. time to " + value + " hours");
            
            fromValueBuffer.append(accept.getRecommend().toPrettyString());
            final TimeAmount oldTime = accept.getRecommend();
            accept.setRecommend(null);
            final TimeAmount t = new TimeAmount(new BigDecimal(value), TimeUnit.HR);
            accept.setRecommend(t);
            edited = true;
            proposal.setChecksBypassed(Boolean.FALSE);
            invalidateQueues = true;
        }
        if (field.equals("partnerContact")) {
            LOGGER.log(Level.DEBUG, "Setting partner contact info to " + value + "");
            
            fromValueBuffer.append(accept.getEmail());
            accept.setEmail(value);
            edited = true;
        }
        if (field.equals("poorWeather")) {
            LOGGER.log(Level.DEBUG, "Setting partner poor weather info to " + value + "");
            
            fromValueBuffer.append(accept.isPoorWeather() ? "True" : "False");
            accept.setPoorWeather(Boolean.parseBoolean(value));
            edited = true;
            proposal.setChecksBypassed(Boolean.FALSE);
            invalidateQueues = true;
        }
        if (!edited) {
            LOGGER.log(Level.WARN, "Could not find TacExtension or edit field");
        }

        return invalidateQueues;
    }

    /**
     * Loads compoonent proposals with partners, submissions (and accepts), along with the observation
     * triumvirate.
     */
    @Override
    @Transactional
    public Proposal populateProposalForListing(Proposal proposal)
    {
        final Session s = sessionFactory.getCurrentSession();
        proposal = (Proposal) s.merge(proposal);
        
        final Set<Proposal> allComponentProposals = proposal.getProposals();
        final Set<Long> componentProposalP1Ids = new HashSet<Long>();
        for (Proposal cp : allComponentProposals) {
            componentProposalP1Ids.add(cp.getPhaseIProposal().getId());
        }

        s.createQuery("from QueueProposal p " +
                "left join fetch p.investigators.pi " +
                "left join fetch p.investigators.coi " +
                "where p.id in (:componentProposalP1Ids)").setParameterList("componentProposalP1Ids", componentProposalP1Ids).list();

        s.createQuery("from ClassicalProposal p " +
                "left join fetch p.investigators.pi " +
                "left join fetch p.investigators.coi " +
                "where p.id in (:componentProposalP1Ids)").setParameterList("componentProposalP1Ids", componentProposalP1Ids).list();


        HashSet<Long> componentProposalIds = new HashSet<Long>();
        for (Proposal p : allComponentProposals) {
            componentProposalIds.add(p.getId());
        }

        List<Proposal> populatedProposals = new ArrayList<Proposal>();
        if (allComponentProposals.size() > 0) {
            populatedProposals = s.createQuery("from Proposal p " +
                    "join fetch p.partner part " +
                    "join fetch p.phaseIProposal p1p " +
                    "where p.id in (:proposals)"
            ).setParameterList("proposals", componentProposalIds).list();
        }

        final HashSet<PhaseIProposal> allComponentPhaseIProposals = new HashSet<PhaseIProposal>();
        for (Proposal p : populatedProposals) {
            allComponentPhaseIProposals.add(p.getPhaseIProposal());
        }

        List<PhaseIProposal> wtf = null; // Why are my investigators coming back null fielded?
        if (allComponentPhaseIProposals.size() > 0) {
            wtf = s.createQuery("from ExchangeProposal p " +
                    "left join fetch p.ngos sub " +
                    "left join fetch sub.accept " +
                    "left join fetch p.investigators i " +
                    "left join fetch i.pi " +
                    "left join fetch i.coi " +
                    "where p in (:phaseIProposals)"
            ).setParameterList("phaseIProposals", allComponentPhaseIProposals).list();

            wtf = s.createQuery("from ClassicalProposal p " +
                    "left join fetch p.ngos sub " +
                    "left join fetch p.exchange exc " +
                    "left join fetch sub.accept " +
                    "left join fetch exc.accept " +
                    "left join fetch p.investigators i " +
                    "left join fetch i.pi " +
                    "left join fetch i.coi " +
                    "where p in (:phaseIProposals)"
            ).setParameterList("phaseIProposals", allComponentPhaseIProposals).list();

            wtf = s.createQuery("from QueueProposal p " +
                    "left join fetch p.ngos sub " +
                    "left join fetch p.exchange exc " +
                    "left join fetch sub.accept " +
                    "left join fetch exc.accept " +
                    "left join fetch p.investigators i " +
                    "left join fetch i.pi " +
                    "left join fetch i.coi " +
                    "where p in (:phaseIProposals)"
            ).setParameterList("phaseIProposals", allComponentPhaseIProposals).list();

            wtf = s.createQuery("from Observation o " +
                    "join fetch o.blueprint " +
                    "join fetch o.target " +
                    "join fetch o.condition " +
                    "where o.proposal in (:phaseIProposals)"
            ).setParameterList("phaseIProposals", allComponentPhaseIProposals).list();
        }

        if (proposal.isJoint()) {
            ((JointProposal) proposal).getPhaseIProposal(true);
        }

        return proposal;
    }


    @Override
    @Transactional(readOnly = true)
    public Set<String> getInstrumentNamesForProposal(final Proposal p) {
        Session s = sessionFactory.getCurrentSession();
        s.update(p);

        return p.collectInstrumentDisplayNames();
    }

    @Override
    @Transactional
    public Proposal duplicate(Proposal p, File pdfFolder) {
        Session s = sessionFactory.getCurrentSession();
        s.update(p);
        Proposal dup = p.duplicate();
        /*
        UX-1512: Alter SubmissionReceipt here (not in p.duplicate()) because p.duplicate() is called
        by multiple use-cases (splitting p's between N and S being one), in some of which the receipt modification
        is not appropriate
         */
        String append = " (Copy " + DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(new Date()) + ")";
        Collection<Submission> submissions = dup.getSubmissionsPartnerEntries().values();
        for(Submission submission : submissions){
            final SubmissionReceipt receipt = submission.getReceipt();
            String oldId = receipt.getReceiptId();
            String newId = oldId + append;
            receipt.setReceiptId(newId);
            s.saveOrUpdate(submission);
        }
        s.save(dup);
        s.saveOrUpdate(p);
        // duplicate pdf
        try {
            FileInputStream inputStream = new FileInputStream(p.getPdfLocation(pdfFolder));
            FileOutputStream outputStream = new FileOutputStream(dup.getPdfLocation(pdfFolder));
            FileCopyUtils.copy(inputStream, outputStream);
        } catch (IOException e) {
            LOGGER.debug("could not copy pdf file while duplication proposals", e);
        }
        // done
        return dup;
    }

    @Override
    @Transactional
    public List<Proposal> getProposalswithItacExtension(Long committeeId) {
        final Session session = sessionFactory.getCurrentSession();
        final Query query =
                session.getNamedQuery("proposal.proposalsByCommitteeToItacExtension").
                        setLong("committeeId", committeeId);
//        for (Proposal p : (List<Proposal>) query.list()) {
//            p.getPhaseIProposal().getSubmissions().getItac();
//            p.getDocument().getObservatory().getGeminiPart().getSubDetails().getSubmissions().getSubmissionMap();
//        }
        return (List<Proposal>) query.list();
    }

    @Override
    @Transactional
    public void setRolloverEligibility(final Long proposalId, final boolean rolloverEligible) {
        final Session session = sessionFactory.getCurrentSession();
        final Query query = session.createQuery("from Proposal p where p.id = :proposalId").setLong("proposalId", proposalId);
        final Proposal proposal = (Proposal) query.uniqueResult();
        
        Validate.notNull(proposal, "Could not find a proposal for proposal id [" + proposalId + "]");
        Validate.notNull(proposal.getItac(), "Could not find an Itac object for proposal [" + proposalId + "]");
        Validate.notNull(proposal.getItac().getAccept(), "Could not find an itac accept object for proposal [" + proposalId + "]");
        
        final Itac itac = proposal.getItac();
        itac.getAccept().setRollover(rolloverEligible);

        session.saveOrUpdate(itac);
    }

    @Override
    @Transactional
    public void deleteProposal(Long proposalId, File pdfFolder) {
        final Session session = sessionFactory.getCurrentSession();
        final Query query = session.createQuery("from Proposal p where p.id = :proposalId").setLong("proposalId", proposalId);
        final Proposal proposal = (Proposal) query.uniqueResult();
        proposal.delete(session);
        // we also try to delete the pdf that belongs to this proposal from the designated pdf folder
        final File pdf = new File(pdfFolder.getAbsolutePath() + File.separator + proposal.getCommittee().getId() + File.separator + proposalId + ".pdf");
        pdf.delete();
    }

    @Override
    @Transactional
    public void setClassical(final Proposal proposal, boolean shouldBeClassical) {
        final Session session = sessionFactory.getCurrentSession();
        final Proposal mergedProposal = (Proposal) session.merge(proposal);
        if (mergedProposal.isJoint()) {
            final JointProposal jointProposal = (JointProposal) mergedProposal;
            final Proposal primaryProposal = jointProposal.getPrimaryProposal();
            primaryProposal.setClassical(shouldBeClassical);
            session.saveOrUpdate(primaryProposal);
        } else {
            mergedProposal.setClassical(shouldBeClassical);
            session.saveOrUpdate(mergedProposal);
        }

        session.refresh(proposal);
    }

    @Override
    public Map<ProposalIssueCategory, List<Proposal>> proposalsInProblemCategories(List<Proposal> proposals) {
        Map<ProposalIssueCategory, List<Proposal>> dict;
        dict = new HashMap<ProposalIssueCategory, List<Proposal>>();
        for (Proposal p : proposals) {
            for (ProposalIssue pi : p.getIssues()) {
                final ProposalIssueCategory c = pi.getCategory();
                if (!dict.containsKey(c)) {
                    dict.put(c, new ArrayList<Proposal>());
                }
                //TODO: If Proposal implemented equals properly, we could switch this to a Set
                final List<Proposal> inCategory = dict.get(pi.getCategory());
                boolean add = true;
                for (Proposal aProposal : inCategory) {
                    if (aProposal.compareTo(p) == 0) {
                        add = false;
                        break;
                    }
                }
                if (add || inCategory.size() == 0) {
                    inCategory.add(p);
                }
            }
        }
        return dict;
    }

    @Override
    @Transactional
    public List<Proposal> getProposalsForQueueAnalysis(String committeeName) {
        final Session session = sessionFactory.getCurrentSession();
        final Query query = session.getNamedQuery("proposal.proposalsForQueueAnalysis").setParameter("committeeName", committeeName);
        final List<Proposal> proposals = (List<Proposal>) query.list();
        return proposals;
    }
}
