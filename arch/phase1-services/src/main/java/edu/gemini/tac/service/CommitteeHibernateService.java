package edu.gemini.tac.service;

import edu.gemini.model.p1.mutable.NonSiderealTarget;
import edu.gemini.shared.skycalc.Angle;
import edu.gemini.tac.persistence.Committee;
import edu.gemini.tac.persistence.LogEntry;
import edu.gemini.tac.persistence.Proposal;
import edu.gemini.tac.persistence.ProposalIssue;
import edu.gemini.tac.persistence.joints.JointProposal;
import edu.gemini.tac.persistence.phase1.*;
import edu.gemini.tac.persistence.phase1.blueprint.BlueprintBase;
import edu.gemini.tac.persistence.phase1.proposal.ExchangeProposal;
import edu.gemini.tac.persistence.phase1.proposal.LargeProgram;
import edu.gemini.tac.persistence.phase1.proposal.PhaseIProposal;
import edu.gemini.tac.persistence.phase1.proposal.QueueProposal;
import edu.gemini.tac.persistence.phase1.queues.PartnerSequence;
import edu.gemini.tac.persistence.phase1.sitequality.SkyBackground;
import edu.gemini.tac.persistence.phase1.submission.Submission;
import edu.gemini.tac.persistence.queues.Banding;
import edu.gemini.tac.persistence.queues.JointBanding;
import edu.gemini.tac.persistence.queues.Queue;
import edu.gemini.tac.persistence.daterange.Shutdown;
import org.apache.commons.lang.Validate;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.hibernate.Hibernate;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.*;

/**
 * Committee centered persistence concerns.
 *
 * @author ddawson
 */
@Service("committeeService")
@Transactional
public class CommitteeHibernateService implements ICommitteeService {

    @Resource(name = "sessionFactory")
    private SessionFactory sessionFactory;

    @Resource(name = "proposalService")
    private IProposalService proposalService;


    private static final Logger LOGGER = Logger.getLogger(CommitteeHibernateService.class.getName());

    @Override
    public List<Committee> getAllCommittees() {
        return namedQueryList("committee.findAllCommittees");
    }

    @Override
    public List<Committee> getAllActiveCommittees() {
        return namedQueryList("committee.findActiveCommittees");
    }

    /* (non-Javadoc)
      * @see edu.gemini.tac.service.ICommitteeService#getAllProposalsForCommittee(java.lang.Long)
      */
    @Override
    @Transactional(readOnly = true)
    @SuppressWarnings("unchecked")
    public List<Proposal> getProposalForCommittee(final Long committeeId, final Long proposalId) {
        final Session session = sessionFactory.getCurrentSession();
        final Query query =
                session.getNamedQuery("proposal.findProposalInCommittee").
                        setLong("committeeId", committeeId).
                        setLong("proposalId", proposalId);
        return loadProposalsForCommittee(session, query, true);
    }


    /* (non-Javadoc)
      * @see edu.gemini.tac.service.ICommitteeService#getAllProposalsForCommittee(java.lang.Long)
      */
    @Override
    @Transactional(readOnly = true)
    @SuppressWarnings("unchecked")
    public List<Proposal> getAllProposalsForCommittee(final Long committeeId) {
        final Session session = sessionFactory.getCurrentSession();
        final List<Investigator> investigators = loadInvestigatorsForCommittee(committeeId, session);

        final Query query = session.getNamedQuery("proposal.findProposalsByCommittee").setLong("committeeId", committeeId);
        final List<Proposal> proposals = loadProposalsForCommittee(session, query, true);
        validateNoLIE(proposals);
        return proposals;
    }

    private void validateNoLIE(Collection<Proposal> ps) {
        for (Proposal p : ps) {
            validateNoLIE(p);
        }
    }

    private void validateNoLIE(Proposal p) {
        try {
            p.getPhaseIProposal().validate();
        } catch (Exception x) {
            LOGGER.error("Exception with proposal " + p + "(" + x + ")");
        }
    }


    /* (non-Javadoc)
    * @see edu.gemini.tac.service.ICommitteeService#getAllProposalsForCommittee(java.lang.Long)
    */
    @Override
    @Transactional
    @SuppressWarnings("unchecked")
    public List<Proposal> checkProposalForCommittee(final Long committeeId, final Long proposalId) {
        final Proposal proposal = proposalService.getProposal(committeeId, proposalId);
        final Proposal populatedProposal = proposalService.populateProposalForProposalChecking(proposal);
        final List<Proposal> proposals = new ArrayList<>(1);
        proposals.add(populatedProposal);

        proposalService.checkProposals(proposals);

        return proposals;
    }

    /* (non-Javadoc)
      * @see edu.gemini.tac.service.ICommitteeService#getAllProposalsForCommittee(java.lang.Long)
      */
    @Override
    @Transactional
    @SuppressWarnings("unchecked")
    public List<Proposal> checkAllProposalsForCommittee(final Long committeeId) {
        final Session session = sessionFactory.getCurrentSession();

        final List<Proposal> committeeProposals = session.createQuery("from Proposal p where p.committee = :committeeId").setLong("committeeId", committeeId).list();
        final List<Proposal> populatedProposals = new ArrayList<>(committeeProposals.size());
        for (Proposal p : committeeProposals) {
            populatedProposals.add(proposalService.populateProposalForProposalChecking(p));
        }

        proposalService.checkProposals(populatedProposals);

        return committeeProposals;
    }

    /* (non-Javadoc)
      * @see edu.gemini.tac.service.ICommitteeService#getAllProposalsForCommittee(java.lang.Long)
      */
    @Override
    @Transactional
    @SuppressWarnings("unchecked")
    public List<Proposal> getAllProposalsForCommitteePartner(final Long committeeId, final String partnerAbbreviation) {
        final Session session = sessionFactory.getCurrentSession();

        final Query pQuery = session.getNamedQuery("proposal.findProposalsByCommitteeAndPartner").setLong("committeeId", committeeId).setString("partnerAbbreviation", partnerAbbreviation);
        final Query jpQuery = session.getNamedQuery("jointproposal.findByComponentAbbreviation").setLong("committeeId", committeeId).setString("partnerAbbreviation", partnerAbbreviation);
        final List<Proposal> proposals = pQuery.list();
        final List<Proposal> jointProposals = jpQuery.list();
        final Set<Proposal> allProposals = new HashSet<>();

        allProposals.addAll(proposals);
        allProposals.addAll(jointProposals);
        for (Proposal jp : jointProposals) {
            allProposals.addAll(jp.getProposals());
        }

        populateProposalsWithListingInformation(new ArrayList<Proposal>(allProposals));

        final List<Proposal> partnerProposalsWithoutComponents = new ArrayList<Proposal>();

        for (Proposal p : allProposals)
            if (p.getJointProposal() == null)
                partnerProposalsWithoutComponents.add(p);

        return partnerProposalsWithoutComponents;
    }

    /* (non-Javadoc)
      * @see edu.gemini.tac.service.ICommitteeService#getAllProposalsForCommittee(java.lang.Long)
      */
    @Override
    @Transactional(readOnly = true)
    @SuppressWarnings("unchecked")
    public List<Proposal> getAllComponentsForCommittee(final Long committeeId) {
        final Session session = sessionFactory.getCurrentSession();
        final Query query = session.getNamedQuery("proposal.findComponentProposalsByCommittee").setLong("committeeId", committeeId);
        return loadProposalsForCommittee(session, query, false);
    }

    /* (non-Javadoc)
      * @see edu.gemini.tac.service.ICommitteeService#getAllProposalsForCommittee(java.lang.Long)
      */
    @Override
    @Transactional
    @SuppressWarnings("unchecked")
    public List<Proposal> checkAllComponentsForCommittee(final Long committeeId) {
        List<Proposal> proposals = getAllComponentsForCommittee(committeeId);
        proposalService.checkProposals(proposals);
        return proposals;
    }

    private List<Investigator> loadInvestigatorsForCommittee(final long committeeId, Session session) {
        final Query query = session.getNamedQuery("Investigator.findInvestigatorsByCommittee");//.setLong("committeeId", committeeId);
        List<Investigator> investigators = query.list();
        return investigators;
    }

    private List<Proposal> loadProposalsForCommittee(Session session, Query initialProposalQuery, boolean filterComponents) {
        List<Proposal> proposals = initialProposalQuery.list();
        LOGGER.info(proposals.size() + " proposals found for committee.");
        if (proposals.size() == 0) {
            return proposals;
        }

        // Finish populating proposals with additional information needed to render
        // listing.  Might be worth consolidating the like's into single requests.
        populateProposalsWithListingInformation(proposals);

        // filter out component proposals, unfortunately they have to be loaded and can not be filtered directly
        // in the query (some information of the joint proposals will be generated on the fly using the components)
        if (filterComponents) {
            List<Proposal> filteredProposals = new LinkedList<Proposal>();
            for (Proposal proposal : proposals) {
                if (!proposal.isJointComponent()) {
                    filteredProposals.add(proposal);
                }
            }
            proposals = filteredProposals;
        }

        return proposals;
    }

    //TODO: Surely this ought to be in ProposalHibernateService, not Committee?
    @Transactional(readOnly = true)
    public void populateProposalsWithListingInformation(final List<Proposal> proposals) {
        if (proposals.size() == 0)
            return;

        final Session session = sessionFactory.getCurrentSession();
        final List<Investigator> investigators = loadInvestigatorsForCommittee(proposals.get(0).getCommittee().getId(), session);
        //Get the p1ps
        List<PhaseIProposal> p1ps = new ArrayList<PhaseIProposal>();
        List<Long> p1pIds = new ArrayList<Long>();
        for (Proposal p : proposals) {
            //Pull in p1ps and itacs here; can't use "from PhaseIProposal" query due to Joint Proposal complexity
            final PhaseIProposal phaseIProposal = p.getPhaseIProposal();
            if (phaseIProposal.getId() != null) {
                p1pIds.add(phaseIProposal.getId());
                p1ps.add(phaseIProposal);
            }
            Hibernate.initialize(p.getItac());
            Hibernate.initialize(p.getItac().getAccept());
            final TimeAmount totalAwardedTime = p.getTotalAwardedTime();
        }
        if (p1ps.size() == 0) {
            return;
        }

        final List<ProposalIssue> issues = session.createQuery("from ProposalIssue pi " +
                "where pi.proposal in (:proposals)").setParameterList("proposals", proposals).list();

        final List<Observation> observations = session.createQuery("from Observation o " +
                "left join fetch o.target " +
                "left join fetch o.blueprint " +
                "left join fetch o.condition " +
                "where o.proposal in (:proposals)").setParameterList("proposals", p1ps).list();

        session.createQuery("from PhaseIProposal p1p " +
                "left join fetch p1p.itac " +
                "where p1p.id in (:ids)").setParameterList("ids", p1pIds).list();


        session.createQuery("from PhaseIProposal p1p " +
                "left join fetch p1p.observations o " +
                "where p1p.id in (:ids)").setParameterList("ids", p1pIds).list();

        session.createQuery("from PhaseIProposal p1p " +
                "left join fetch p1p.conditions c " +
                "where p1p.id in (:ids)").setParameterList("ids", p1pIds).list();

        session.createQuery("from PhaseIProposal p1p " +
                "left join fetch p1p.blueprints b " +
                "where p1p.id in (:ids)").setParameterList("ids", p1pIds).list();

        final List<Target> targets = session.createQuery("from PhaseIProposal p1p " +
                "left join fetch p1p.targets t " +
                "where p1p.id in (:ids)").setParameterList("ids", p1pIds).list();

        final List<QueueProposal> queueProposals = session.createQuery("from GeminiNormalProposal p " +
                "left join fetch p.ngos n " +
                "left join fetch p.exchange e " +
                "left join fetch n.accept " +
                "left join fetch n.receipt " +
                "left join fetch n.request " +
                "left join fetch e.accept " +
                "left join fetch e.receipt " +
                "left join fetch e.request " +
                "where p in (:phaseIProposals)").setParameterList("phaseIProposals", p1ps).list();

        final List<ExchangeProposal> exchangeProposals = session.createQuery("from ExchangeProposal p " +
                "left join fetch p.ngos n " +
                "left join fetch n.accept " +
                "left join fetch n.receipt " +
                "left join fetch n.request " +
                "where p in (:phaseIProposals)").setParameterList("phaseIProposals", p1ps).list();

        final List<LargeProgram> largePrograms = session.createQuery("from LargeProgram p " +
                "left join fetch p.largeProgramSubmission n " +
                "left join fetch n.accept " +
                "left join fetch n.receipt " +
                "left join fetch n.request " +
                "where p in (:phaseIProposals)").setParameterList("phaseIProposals", p1ps).list();

        final List<BlueprintBase> blueprints = session.createQuery("from BlueprintBase b " +
                "where b.phaseIProposal in (:proposals)").setParameterList("proposals", p1ps).list();

        if (blueprints.size() > 0)
            for (String fq : BlueprintBase.filterQueryNames) {
                session.getNamedQuery(fq).setParameterList("blueprints", new HashSet<BlueprintBase>(blueprints)).list();
            }

        session.getNamedQuery("proposal.loadIssuesForProposals").setParameterList("proposals", proposals).list();

        final List<TooTarget> tooTargets = session.getNamedQuery("target.tooTargetsForEditingByEntityList").setParameterList("targets", targets).list();
        final List<NonSiderealTarget> nonSidereealTargets = session.getNamedQuery("target.nonsiderealTargetsForEditingByEntityList").setParameterList("targets", targets).list();
        final List<SiderealTarget> siderealTargets = session.getNamedQuery("target.siderealTargetsForEditingByEntityList").setParameterList("targets", targets).list();
        Hibernate.initialize(siderealTargets);
        Hibernate.initialize(nonSidereealTargets);
        Hibernate.initialize(tooTargets);
    }

    /* Used to load through HibernateProxies */
    TargetVisitor targetVisitor = new TargetVisitor() {
        @Override
        public void visit(SiderealTarget st) {
            final Coordinates c = st.getCoordinates();
            final Set<Magnitude> magnitudes = st.getMagnitudes();
            final Angle ra = c.getRa();
        }

        @Override
        public void visit(NonsiderealTarget nst) {
            final List<EphemerisElement> ephemeris = nst.getEphemeris();
            for (EphemerisElement ee : ephemeris) {
                final Coordinates coordinates = ee.getCoordinates();
                final Angle ra = coordinates.getRa();
            }
        }

        @Override
        public void visit(TooTarget tt) {
        }
    };

    @Override
    @Transactional(readOnly = true)
    @SuppressWarnings("unchecked")
    public List<Proposal> getAllCommitteeProposalsForQueueCreation(final Long committeeId) {
        LOGGER.log(Level.DEBUG, "getAllCommitteeProposalsForQueueCreation()");
        long start = System.currentTimeMillis();

        final Session session = sessionFactory.getCurrentSession();
        //final Query proposalQuery = session.getNamedQuery("gemininormalproposal.findProposalsForQueueCreationByCommittee").setLong("committeeId", committeeId);
        final Query proposalQuery = session.createQuery("from Proposal p join fetch p.committee c  where c.id = :committeeId").setLong("committeeId", committeeId);
        final List<Proposal> allProposalsInCommittee = proposalQuery.list();

        LOGGER.info(allProposalsInCommittee.size() + " allProposalsInCommittee found for committee.");
        if (allProposalsInCommittee.size() == 0)
            return allProposalsInCommittee;
        //~9sec
        LOGGER.log(Level.DEBUG, "Time spent finding allProposalsInCommittee " + (System.currentTimeMillis() - start));

        // Finish populating allProposalsInCommittee with additional information needed to render
        // listing.  Might be worth consolidating the like's into single requests.
        List<Long> ids = new ArrayList<Long>();

        List<Proposal> populatedProposals = new ArrayList<Proposal>();
        for (Proposal outerProposal : allProposalsInCommittee) {
            // Populate potentially joint proposal
            final Proposal proposal = proposalService.populateProposalForQueueCreation(outerProposal);
            if (proposal.isJoint()) {
                for (Proposal p : outerProposal.getProposals()) {
                    // Populate component allProposalsInCommittee
                    proposalService.populateProposalForQueueCreation(p);
                }
            }
            // Add only joints and independent allProposalsInCommittee.
            if (!outerProposal.isJointComponent()) {
                populatedProposals.add(proposal);
            }
        }

        LOGGER.log(Level.DEBUG, "Time to populate the proposals " + (System.currentTimeMillis() - start));

        return populatedProposals;
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Queue> getAllQueuesForCommittee(final Long committeeId) {
        return getAllQueuesForCommittee(committeeId, null);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Queue> getAllQueuesForCommittee(final Long committeeId, final Long siteId) {
        final Session session = sessionFactory.getCurrentSession();
        String where = "where q.committee = " + committeeId;
        if (siteId != null) {
            where += " and q.site.id = " + siteId;
        }
        final Query query = session.createQuery(
                "from Queue q " + where + " order by q.createdTimestamp desc");
        final List<Queue> queues = query.list();

        for (Queue q : queues) {
            Hibernate.initialize(q.getCommittee());
            Hibernate.initialize(q.getBandings());
        }

        return queues;
    }

    @Override
    public Committee getCommittee(final Long committeeId) {
        final Session session = sessionFactory.getCurrentSession();

        final Query query = session.getNamedQuery("committee.getCommitteeWithQueues").setLong("committee_id", committeeId);
        final Committee committee = (Committee) query.uniqueResult();
        if (committee == null) {
            return null;
        }
        //Bring in the Shutdowns
        for (Shutdown s : committee.getShutdowns()) {
            final Date start = s.getStart();
        }

        return committee;
    }

    @Override
    public Committee getCommitteeWithMemberships(final Long committeeId) {
        final Session session = sessionFactory.getCurrentSession();

        Committee committee = (Committee) session.createQuery("from Committee c " +
                "left join fetch c.members m " +
                "left join fetch m.partner p " +
                "where c.id = :id").setParameter("id", committeeId).uniqueResult();

        return committee;
    }


    @Override
    public void saveCommitee(final Committee committee) {
        final Session session = sessionFactory.getCurrentSession();

        session.saveOrUpdate(committee.getSemester());
        session.saveOrUpdate(committee);
    }

    /* (non-Javadoc)
      * @see edu.gemini.tac.service.ICommitteeService#getCommitteeForExchangeAnalysis(java.lang.Long)
      */
    @Override
    @Transactional(readOnly = true)
    public Committee getCommitteeForExchangeAnalysis(Long committeeId) {
        Validate.notNull(committeeId);
        final Session session = sessionFactory.getCurrentSession();
        final Query query = session.getNamedQuery("committee.findCommitteeForExchangeAnalysis").setLong("id", committeeId);

        final Committee committee = (Committee) query.uniqueResult();
        if (committee == null) {
            LOGGER.error("Could not retrieve committee");
        }

        final List<Submission> submissions = new ArrayList<Submission>();
        if (committee.getProposals().size() > 0) {
            for (Proposal p : committee.getProposals()) {
                //Got to bring in conditions, but not in named query above (two bags)
                final List<Condition> conditions = p.getPhaseIProposal().getConditions();
                for (Condition c : conditions) {
                    SkyBackground sb = c.getSkyBackground();
                }
                //Bring in targets
                final List<Target> targets = p.getPhaseIProposal().getTargets();
                for (Target t : targets) {
                    final String name = t.getName();
                }

                //Additionally...
                if (!p.isJoint())
                    submissions.addAll(p.getPhaseIProposal().getSubmissions());
            }
        }

        if (submissions.size() > 0) {
            session.getNamedQuery("Submission.findNgoSubmissions").setParameterList("submissions", submissions).list();
            session.getNamedQuery("Submission.findExchangeSubmissions").setParameterList("submissions", submissions).list();
        }

        return committee;
    }

    @Override
    @Transactional
    public void setPartnerSequenceProportional(Long committeeId) {
        Validate.notNull(committeeId);
        final Session session = sessionFactory.getCurrentSession();
        final Committee committee = getCommittee(committeeId);
        committee.setPartnerSequence(null);
        session.saveOrUpdate(committee);
    }

    @Override
    @Transactional
    public void setPartnerSequence(Long committeeId, PartnerSequence ps) {
        Validate.notNull(committeeId);
        Validate.notNull(ps);
        final Session session = sessionFactory.getCurrentSession();
        final Committee committee = getCommittee(committeeId);
        committee.setPartnerSequence(ps);
        session.saveOrUpdate(ps);
    }

    @Override
    @Transactional
    public PartnerSequence getPartnerSequenceOrNull(Long committeeId) {
        Validate.notNull(committeeId);
        final Session session = sessionFactory.getCurrentSession();
        final Committee committee = getCommittee(committeeId);
        final PartnerSequence sequence = committee.getPartnerSequence();
        if (sequence != null) {
            //Hibernate-load PartnerSequence
            final String csv = sequence.getCsv();
        }
        return sequence;
    }


    private List<Committee> namedQueryList(final String namedQuery) {
        final Session session = sessionFactory.getCurrentSession();

        final Query query = session.getNamedQuery(namedQuery);
        @SuppressWarnings("unchecked")
        final List<Committee> committees = query.list();

        return committees;
    }

    public void setSessionFactory(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    private Collection<Long> collectInvestigatorIds(final Collection<Proposal> proposals) {
        final List<Long> investigatorIds = new ArrayList<Long>(proposals.size());
        for (Proposal p : proposals) {
            PrincipalInvestigator pi = p.getPhaseIProposal().getInvestigators().getPi();
            investigatorIds.add(pi.getId());
            for (CoInvestigator coi : p.getPhaseIProposal().getInvestigators().getCoi()) {
                investigatorIds.add(coi.getId());
            }
            //TODO: Confirm this. Might double-add investigators or it might be the proper logic for joint proposals
            investigatorIds.addAll(collectInvestigatorIds(p.getProposals()));
        }
        return investigatorIds;
    }

    @Override
    @Transactional
    public void dissolve(Committee committee, JointProposal jointProposal) {
        LOGGER.log(Level.DEBUG, "dissolve");

        Session currentSession = sessionFactory.getCurrentSession();
        currentSession.update(jointProposal);

        Set<Queue> dirtyQueues = new HashSet<Queue>();

        //Also have to delete any *->JointProposal references
        //Set any referencing Queue to "dirty" to indicate that it's data are changed
        final Query logEntryQuery = currentSession.createQuery("from LogEntry le join fetch le.proposal p where p.id = :proposalId").setLong("proposalId", jointProposal.getId());
        List<LogEntry> entries = logEntryQuery.list();

        for (LogEntry entry : entries) {
            final Queue queue = entry.getQueue();
            if(queue != null){
                dirtyQueues.add(queue);
                queue.setDirty(true);
            }
            currentSession.delete(entry);
        }

        final List<Queue> queuesForCommittee = this.getAllQueuesForCommittee(committee.getId());
        for (Queue queue : queuesForCommittee) {
            boolean isDirty = false;
            Set<Proposal> classicalProposals = queue.getClassicalProposals();
            if(classicalProposals.remove(jointProposal)){
                isDirty = true;
            }
            queue.setClassicalProposals(classicalProposals);

            Set<Proposal> exchangeProposals = queue.getExchangeProposals();
            if(exchangeProposals.remove(jointProposal)){
                isDirty = true;
            }
            queue.setExchangeProposals(exchangeProposals);

            //Delete appropriate JointBandings and their component bandings
            final Set<JointBanding> jointBandings = new HashSet<JointBanding>();
            final SortedSet<Banding> queueBandings = queue.getBandings();
            final SortedSet<Banding> newQueueBandings = new TreeSet<Banding>();
            final Set<Banding> deletedBandings = new HashSet<Banding>();
            for (Banding b : queueBandings) {
                if (b.getProposal().getId() == jointProposal.getId()) {
                    currentSession.delete(b);
                    if (JointBanding.class.isInstance(b)) {
                        jointBandings.add((JointBanding) b);
                    }
                    isDirty = true;
                }else{
                    //Avoid ConcurrentModificationExceptions by accumulating the OK bandings
                    newQueueBandings.add(b);
                }
            }
            //Reset the Queue's bandings to be the ok ones
            queue.setBandings(newQueueBandings);
            //Delete component banding references to the now-gone JointBanding
            for (JointBanding jb : jointBandings) {
                LOGGER.log(Level.DEBUG, "Deleting JointBanding["+ jb.getId() + "]");
                queue.removeBanding(jb);
            }

            if(isDirty){
                queue.setDirty(true);
                dirtyQueues.add(queue);
            }
            LOGGER.log(Level.DEBUG, "Queue[" + queue.getId() + "] deleting " + deletedBandings.size() + " bandings");
        }

        List<ProposalIssue> issues = currentSession.createQuery("from ProposalIssue pi join fetch pi.proposal p where p.id = :proposalId").setLong("proposalId", jointProposal.getId()).list();
        for (ProposalIssue issue : issues) {
            currentSession.delete(issue);
        }

        for(Queue dirtyQueue : dirtyQueues){
            currentSession.saveOrUpdate(dirtyQueue);
        }

        currentSession.delete(jointProposal);
    }
}
