package edu.gemini.tac.persistence;

import edu.gemini.model.p1.mutable.Band;
import edu.gemini.model.p1.mutable.ExchangePartner;
import edu.gemini.tac.persistence.joints.BaseProposalElement;
import edu.gemini.tac.persistence.joints.JointProposal;
import edu.gemini.tac.persistence.phase1.*;
import edu.gemini.tac.persistence.phase1.blueprint.BlueprintBase;
import edu.gemini.tac.persistence.phase1.proposal.*;
import edu.gemini.tac.persistence.phase1.submission.ExchangeSubmission;
import edu.gemini.tac.persistence.phase1.submission.Submission;
import edu.gemini.tac.persistence.phase1.submission.SubmissionAccept;
import edu.gemini.tac.persistence.phase1.submission.SubmissionReceipt;
import edu.gemini.tac.persistence.queues.Banding;
import edu.gemini.tac.persistence.queues.Queue;
import org.apache.commons.lang.Validate;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.hibernate.HibernateException;
import org.hibernate.LazyInitializationException;
import org.hibernate.Session;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import javax.management.RuntimeErrorException;
import javax.persistence.*;
import java.io.File;
import java.math.BigDecimal;
import java.util.*;


/**
 * A TAC specific information container for application specific surrounding the phase
 * 1 documents that serve as input for the system.  A proposal is something to be accepted
 * or ranked.  It is specific to a committee (although we must also consider rolling
 * proposals over between semesters.)
 *
 * @author ddawson
 */
@NamedQueries({
        // find all proposals of a committee (single, joints and components)
        @NamedQuery(name = "proposal.findProposalsByCommittee",
                query = "from Proposal p " +
                        "join fetch p.committee comm " +
                        "join fetch p.partner partner " +
                        "where p.committee = :committeeId"
        ),
        @NamedQuery(name = "proposal.findProposalsForProposalCheckingByCommittee",
                query = "from Proposal p " +
                        "join fetch p.committee comm " +
                        "join fetch p.partner partner " +
                        "left join fetch p.itac " +
                        "left join fetch p.joint " +
                        "left join fetch p.phaseIProposal p1p " +
                        "where p.committee = :committeeId"
        ),
        @NamedQuery(name="proposal.findProposalsWitPartnerEmptyByCommittee", // No collections are to be added to this query.
                query = "from Proposal p " +
                            "join fetch p.committee comm " +
                            "join fetch p.phaseIProposal p1p " +
                            "join fetch p1p.parent " +
                            "join fetch p.partner partner " +
                        "where p.committee.id = :committeeId"
        ),
        @NamedQuery(name="proposal.findProposalsForQueueCreationByCommittee",
                query = "from Proposal p " +
                            "join fetch p.committee comm " +
                            "left outer join fetch p.phaseIProposal p1p " +
                            "join fetch p.partner partner " +
                            "left outer join fetch p.issues " +
                        "where p.committee = :committeeId"

        ),
        @NamedQuery(name = "proposal.findProposalsByCommitteeAndPartner",   // Not ready for prime time, does not pull in joint proposals
                query = "from Proposal p " +
                        "left outer join fetch p.partner part " +
                        "where p.committee = :committeeId and " +
                        "part.abbreviation = :partnerAbbreviation"
        ),
        // find all component and single proposals of a committee (i.e. the originally imported proposals)
        @NamedQuery(name = "proposal.findComponentProposalsByCommittee",
                query = "from Proposal p " +
                        "join fetch p.committee comm " +
                        "left outer join fetch p.partner part " +
                        "join fetch p.phaseIProposal p1p " +
                        "join fetch p1p.investigators i " +
                        "left outer join fetch p1p.itac " +
                        "left outer join fetch i.pi pi " +
                        "where p.committee = :committeeId and p.class != JointProposal"
        ),
        @NamedQuery(name = "proposal.findProposalInCommittee",
                query = "from Proposal p where p.committee = :committeeId and p.id = :proposalId"
        ),
        @NamedQuery(name = "proposal.findProposalForEditingById",
                query = "from Proposal p " +
                        "join fetch p.committee comm " +
                        "left outer join fetch p.partner part " +
                        "join fetch p.phaseIProposal p1p " +
                        "join fetch p1p.investigators i " +
                        "join fetch p1p.itac " +
                        "left outer join fetch p1p.observations obs " +
                        "left outer join fetch p.issues " +
                        "where p.committee = :committeeId and p.id = :proposalId"
        ),
        @NamedQuery(name = "proposal.findProposalBySubmissionsKey",
                query = "from Proposal p " +
                        "join fetch p.phaseIProposal p1p " +
                        "join fetch p.committee comm " +
                        "where p.committee = :committeeId and p1p.submissionsKey = :submissionsKey"
        ),
        @NamedQuery(name = "proposal.proposalsByCommitteeToItacExtension",
                query = "from Proposal p " +
                        "join fetch p.committee comm " +
                        "left outer join fetch p.partner part " +
                        "join fetch p.phaseIProposal p1p " +
                        "join fetch p1p.investigators i " +
                        "left outer join fetch p1p.itac " +
                        "left outer join fetch i.pi pi " +
                        "where p.committee = :committeeId " +
                        "and ite is not null"
        ),
        @NamedQuery(name="proposal.proposalsForQueueAnalysis",
                query = "from Proposal p " +
                        "join fetch p.phaseIProposal p1p " +
                        "join fetch p.committee comm " +
                        "left outer join fetch p.partner part " +
                        "join fetch p.phaseIProposal p1p " +
                        "join fetch p1p.investigators i " +
                        "where comm.name = :committeeName"
        ),
        @NamedQuery(name="proposal.findProposalForCloningById",
            query = "from Proposal p " +
                    "join fetch p.phaseIProposal p1p " +
                    "join fetch p1p.investigators i " +
                    "join fetch p1p.targets ts " +
                    "where p.id = :proposalId"
        ),
        @NamedQuery(name="proposal.loadIssuesForProposals",
            query = "from Proposal p " +
                    "left join fetch p.issues " +
                    "where p in (:proposals)"
        )
})
@Entity
@Table(name = "proposals")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "proposal_type")
@DiscriminatorValue("class edu.gemini.tac.persistence.Proposal")
public class Proposal extends BaseProposalElement implements Comparable<Proposal>, IValidateable {
    private final static String GEMINI_ID_UNKNOWN = "NOT AVAILABLE - QUEUE NOT FINALIZED YET";
    private final static String DETAILS_URL = "<a href=\"/tac/committees/%s/proposals/%s\">%s</a>";
    public static final String NO_PROPOSAL_KEY = "not present";
    private static final Logger LOGGER = Logger.getLogger(Proposal.class.getName());

    @ManyToOne(fetch = FetchType.LAZY, cascade = { CascadeType.MERGE })
    @JoinColumn(name = "joint_proposal_id", nullable = true)
    protected JointProposal joint;

    /**
     * Awkward.  This instance variable has no meaning in a subclass, JointProposal, as we
     * are creating it's equivalent dynamically, not persisting it to the database.  Access to it is
     * managed via an accessor method.  Need to be very careful not to refer to this field directly.
     * OK.  Not so much awkward, as downright ugly.
     * 
     * Please read
     * https://community.jboss.org/wiki/SomeExplanationsOnLazyLoadingone-to-one
     * http://stackoverflow.com/questions/1444227/making-a-onetoone-relation-lazy
     * and understand the issue before you revert this.  Yes.  It's awful.  An eager load with no workaround
     * for every phaseIProposal loaded is even more awful.  
     * Another option is to come up with a phaseIProposal that has no meaning, joint proposal will continue to
     * route around that.
     */
    @OneToMany(fetch = FetchType.LAZY, cascade = { CascadeType.PERSIST, CascadeType.MERGE })
    @JoinColumn(name = "proposal_id")
    @Cascade(org.hibernate.annotations.CascadeType.SAVE_UPDATE)
    protected Set<PhaseIProposal> phaseIProposal = new HashSet<PhaseIProposal>();

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "committee_id", nullable = false)
    private Committee committee;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @OneToMany(fetch = FetchType.LAZY, cascade = {CascadeType.ALL})
    @JoinColumn(name = "proposal_id", nullable = true)
    @Fetch(org.hibernate.annotations.FetchMode.SUBSELECT)
    private Set<ProposalIssue> issues;

    @ManyToOne(fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinColumn(name = "partner_id", nullable = false)
    @Cascade(org.hibernate.annotations.CascadeType.SAVE_UPDATE)
    private Partner partner;

    @OneToMany(fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinColumn(name = "proposal_id")
    @Fetch(FetchMode.SUBSELECT)
    @Cascade(org.hibernate.annotations.CascadeType.SAVE_UPDATE)
    private Set<LogEntry> logEntries = new HashSet<LogEntry>();

    @Column(name = "checks_bypassed")
    protected Boolean checksBypassed = Boolean.FALSE;

    public Proposal() {
    }

    public Proposal(Proposal that) {
        memberClone(that);
    }

    protected void memberClone(Proposal that) {
        this.setCommittee(that.getCommittee());
        this.setPartner(that.getPartner());
        this.setPhaseIProposal(that.getPhaseIProposal().memberClone());
        this.setIssues(new HashSet<ProposalIssue>(that.getIssues()));
    }

    public Proposal(Proposal that, Session session) {
        if (that.getId() != null) {
            reattachIfNecessary(session, that);
        }

        memberClone(that);
    }

    public Committee getCommittee() {
        return committee;
    }

    public void setCommittee(Committee committee) {
        this.committee = committee;
    }

    public Long getId() {
        return id;
    }

    public Set<ProposalIssue> getIssues() {
        return issues;
    }

    public void setIssues(final Set<ProposalIssue> issues) {
        if ((issues == null) || (this.issues == null)) {
            this.issues = issues;
        } else {
            //Hibernate requires re-use of existing collection if all-delete-orphan
            this.issues.clear();
            this.issues.addAll(issues);
        }
    }

    /**
     * This seems like a useless pass-through, but in fact it's necessary as the JointProposal has it's own component
     * ITAC element that is not a part of a phaseIProposal.
     *
     * Return type changed between 1.1 and 2.0.
     *
     * @return the itac of it's phaseIProposal
     */
    public Itac getItac() {
        if (getJointProposal() == null)
            return getPhaseIProposal().getItac();
        else
            return getJointProposal().getItac();
    }

    public Partner getPartner() {
        return partner;
    }

    public void setPartner(Partner partner) {
        this.partner = partner;
    }

    public PhaseIProposal getPhaseIProposal() {
        Validate.isTrue(phaseIProposal != null && phaseIProposal.size() == 1, "Pass through phaseIProposal set in invalid state.");

        return phaseIProposal.iterator().next();
    }

    public void setPhaseIProposal(final PhaseIProposal phaseIProposal) {
        phaseIProposal.setParent(this);
        this.phaseIProposal.clear();
        this.phaseIProposal.add(phaseIProposal);
    }

    public JointProposal getJointProposal() {
        return joint;
    }

    public void setJointProposal(JointProposal joint) {
        this.joint = joint;
    }

    public Set<Observation> getObservations() {
        return getPhaseIProposal().getObservations();
    }
    public Set<Observation> getAllObservations() {
        return getPhaseIProposal().getAllObservations();
    }
    public List<Target> getTargets() {
        return getPhaseIProposal().getTargets();
    }
    public List<Condition> getConditions() {
        return getPhaseIProposal().getConditions();
    }
    public List<BlueprintBase> getBlueprints() {
        return getPhaseIProposal().getBlueprints();
    }

    public Set<Observation> getObservationsForBand(Band band) {
        Set<Observation> obs = new HashSet<Observation>();
        for (Observation o : getPhaseIProposal().getObservations()) {
            if (o.getBand() == band) {
                obs.add(o);
            }
        }
        return obs;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Proposal)) return false;
        if (!super.equals(o)) return false;

        Proposal proposal = (Proposal) o;

        if (committee != null ? !committee.equals(proposal.committee) : proposal.committee != null) return false;
        if (joint != null ? !joint.equals(proposal.joint) : proposal.joint != null) return false;
        if (partner != null ? !partner.equals(proposal.partner) : proposal.partner != null) return false;
        if (phaseIProposal != null ? !phaseIProposal.equals(proposal.phaseIProposal) : proposal.phaseIProposal != null)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (committee != null ? committee.hashCode() : 0);
        result = 31 * result + (partner != null ? partner.hashCode() : 0);
        result = 31 * result + (joint != null ? joint.hashCode() : 0);

        return result;
    }

// --------------------- Interface Comparable ---------------------

    @Override
    public int compareTo(Proposal proposal) {
        return id.compareTo(proposal.getId());
    }

// --------------------- Interface IProposalElement ---------------------

    public String toDot() {
        String dot = "digraph Proposal {\n";

        String myDot = "Proposal_" + (id == null ? "NULL" : id.toString());
        dot += myDot + ";\n";
        dot += myDot + "->" + getPhaseIProposal().toDot() + ";\n";
        dot += "}\n";
        return dot;
    }

// --------------------- Interface IValidateable ---------------------

    public void validate() {
        Validate.notNull(getPhaseIProposal(), "Proposal must have a phase 1 proposal.");
        Validate.notNull(getCommittee(), "Proposal must have a committee.");
        Validate.notNull(getPartner(), "Proposal must have a partner.");
        getPhaseIProposal().validate();
    }

    public boolean belongsToSite(final Site site) {
        for (BlueprintBase b : getPhaseIProposal().getBlueprints()) {
            // Subaru and Keck blueprints legitimately have no site.
            if (b.getSite() == null || b.getSite().equals(site))
                return true;
        }

        return false;
    }

    public String createProgramId(final int nr) {
        String proposalTypeLetter = "Q";
        if (isClassical()) {
            proposalTypeLetter = "C";
        } else if (isLargeProgram()) {
            proposalTypeLetter = "LP";
        }
        Site site = getSite();
        if (site == null) {
            // this is the case for Exchange proposals
            site = Site.NORTH; // TODO: is this correct? how do we deal with this?
        }
        StringBuilder sb = new StringBuilder();
        sb.append("G");
        sb.append(site.getDisplayName().substring(0, 1));
        sb.append("-");
        sb.append(getCommittee().getSemester().getDisplayName());
        sb.append("-");
        sb.append(proposalTypeLetter);
        sb.append("-");
        sb.append(nr);
        return sb.toString();
    }
    
    public Site getSite() {
        return getPhaseIProposal().getSite();
    }

    /**
     * Gets the location of the pdf attachment that belongs to this proposal as a File object.
     * The location is always relative to a parent path, i.e. the main folder for all pdfs on the server.
     * 
     * @param parentPath the main folder for all pdfs on the server
     * @return the location of the pdf attachment
     */
    public File getPdfLocation(File parentPath) {
        StringBuilder sb = new StringBuilder().
        append(parentPath.getAbsolutePath()).
        append(File.separator).
        append(getCommittee().getId()).
        append(File.separator).
        append(getId()).
        append(".pdf");
        return new File(sb.toString());    
    } 

    /**
     * @return <code>true</code> if this proposal has to be executed in classical mode,
     *         <code>false</code> otherwise
     */
    public boolean isClassical() {
        return getPhaseIProposal().isClassical();
    }

    /**
     * @return <code>true</code> if this proposal is a large program,
     *         <code>false</code> otherwise
     */
    public boolean isLargeProgram() {
        return getPhaseIProposal().isLargeProgram();
    }

    /**
     * Deletes a proposal.
     * Before deleting a proposal any bandings that refer to it have to be deleted, too.
     * TODO: The bulk of the business logic in here is likely encodable using the relationship annotations.
     *
     * @param session a hibernate session to be used for operations.
     */
    public void delete(Session session) {
        LOGGER.log(Level.DEBUG, "Proposal.delete()");
        try {
            nullifyLogEntries(session);

            deleteIssues(session);
            final Set<Queue> queuesChanged = deleteBandings(session);
            deleteEnqueuedClassicalProposals(session, queuesChanged);
            markQueuesDirty(session, queuesChanged);
            removeFromJointProposalParent(session);
            deletePhaseIProposal(session);

            LOGGER.log(Level.DEBUG, "deleting proposal " + getId());
            session.delete(this);
            session.flush();
        } catch (Throwable e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    private void deletePhaseIProposal(Session session) {
        // now we're good to go ahead and delete the proposal and the document that belongs to it.  Phase I proposal
        // can be null (for example, if this is a joint proposal (which should really have it's own delete))
        if (phaseIProposal != null && phaseIProposal.size() != 0) {
            final PhaseIProposal p1p = getPhaseIProposal();
            LOGGER.log(Level.DEBUG, "deleting phaseIProposal " + p1p.getId() + " in preparation for deleting proposal");
            //Have to explicitly delete P1p<->Observation<->Conditions graph
            for(Observation o : p1p.getAllObservations()){
                session.delete(o);
            }
            for(Condition c : p1p.getConditions()){
                session.delete(c);
            }
            session.delete(p1p);
        }
    }

    private void removeFromJointProposalParent(Session session) {
        //If it's a component of a JP, remove it from the association
        JointProposal maybeParent = this.getJointProposal();
        if(maybeParent != null){
            maybeParent.getProposals().remove(this);
            session.saveOrUpdate(maybeParent);
            session.flush();
        }
    }

    private void markQueuesDirty(Session session, Set<Queue> queuesChanged) {
        for (Queue queue : queuesChanged) {
            // at least we should mark the queue as being "dirty" i.e. the data that was used to create the
            // queue has been messed with...
            queue.setDirty(true);
            session.update(queue);
        }
        session.flush();
    }

    private void deleteEnqueuedClassicalProposals(Session session, Set<Queue> queuesChanged) {
        // ITAC-452: Additionally delete classical proposals that are enqueued
        final org.hibernate.Query classicalQuery = session.createQuery("from Queue q where :proposal in elements(q.classicalProposals)");
        classicalQuery.setParameter("proposal", this);
        List<Queue> queues = classicalQuery.list();
        for (Queue q : queues) {
            q.getClassicalProposals().remove(this);
            queuesChanged.add(q);
        }
    }

    private Set<Queue> deleteBandings(Session session) {
        // ITAC-358: We can not delete proposals that are referred to (banded) by a queue.
        // A nice solution would be to mark the proposal as "deleted" and then not show it in the committee anymore
        // but that would mean to change a lot of queries and make sure that "deleted" proposals are not returned.
        // The quick (and dirty) and low risk fix is to just delete the banding before deleting the proposal.
        final org.hibernate.Query query = session.createQuery("from Banding b where b.proposal = :proposalId");
        final List<Banding> bandings = query.setLong("proposalId", getId()).list();
        final Set<Queue> queuesChanged = new HashSet<Queue>();
        for (Banding banding : bandings) {
            LOGGER.log(Level.DEBUG, "deleting banding " + banding.getId() + " in preparation for deleting proposal");
            final Queue queue = banding.getQueue();

            if (queue == null) {
                LOGGER.warn("Banding " + banding.getId() + ":" + banding + " for proposal " + banding.getProposal().getId() + " is missing it's queue.  Data corruption elsewhere?");
            } else {
                queue.removeBanding(banding);
                queuesChanged.add(queue);
            }
            if (banding.isJoint()) {
                Set<Banding> bandingBandings = banding.getBandings();
                for (Banding bb : bandingBandings) {
                    session.delete(bb);
                }
            }else{
                session.delete(banding);
            }
        }
        session.flush();
        return queuesChanged;
    }

    private void deleteIssues(Session session) {
        for(ProposalIssue i : issues){
            session.delete(i);
        }
        issues.clear();
    }

    private void nullifyLogEntries(Session session) {
        for (LogEntry le : logEntries) {
            le.setProposal(null);
            session.saveOrUpdate(le);
        }
        logEntries.clear();
    }

    public Proposal duplicate() {
        LOGGER.log(Level.DEBUG, "Proposal.duplicate()");

        /* UX-1485 Duplicating proposal results in same key and leads to import problems
            There is something interesting happening down in member clone where the intention was to create
            a new submissions key, but then later, reminiscent of a a bug fix, it's "corrected" back to the
            original.  It smells of two use-cases clashing, so I'm moving the new random generation up to
            here, within the single use-case because time.
         */
        final Proposal duplicatedProposal = new Proposal(this);
        duplicatedProposal.getPhaseIProposal().setSubmissionsKey(PhaseIProposal.generateNewSubmissionsKey());

        return duplicatedProposal;
    }

    public String getPartnerAbbreviation() {
        return partner.getAbbreviation();
    }

    public String getPartnerAwardedTime() {
        return getPhaseIProposal().getTotalAwardedTime().toPrettyString();
    }

    public String getPartnerRecommendedTime() {
        return getPhaseIProposal().getPrimary().getAccept().getRecommend().toPrettyString();
    }

    public String getPartnerRanking() {
        final Submission primarySubmission = getPhaseIProposal().getPrimary();
        SubmissionAccept primarySubmissionAccept = primarySubmission.getAccept();
        final BigDecimal ranking = primarySubmissionAccept.getRanking();

        return ranking.toString();
    }

    public String getPartnerReferenceNumber() {
        final Submission primarySubmission = getPhaseIProposal().getPrimary();
        final SubmissionReceipt receipt = primarySubmission.getReceipt();

        return receipt.getReceiptId();
    }

    public Submission getPartnerTacExtension() {
        return getPhaseIProposal().getPrimary();
    }

    public Set<Proposal> getProposals() {
        final Set<Proposal> smallSet = new HashSet<Proposal>(); // A small set indeed.
        smallSet.add(this);

        return smallSet;
    }

    /**
     * Changed return types between 1.1 and 2.0.  Changed data structure as well to something a little more expressive.
     *
     * @return a mapping from partner to their submission element
     */
    public Map<Partner, Submission> getSubmissionsPartnerEntries() {
        final List<Submission> submissions = getPhaseIProposal().getSubmissions();
        final Map<Partner, Submission> partnerEntries = new HashMap<Partner, Submission>();
        for (Submission n : submissions) {
            partnerEntries.put(n.getPartner(), n);
        }

        return partnerEntries;
    }

    /**
     * Changed return types between 1.1 and 2.0
     *
     * @return the primary submission from the accepting partner
     */
    public Submission getSubmissionsPartnerEntry() {
        return getPhaseIProposal().getPrimary();
    }

    /**
     * Changed significantly since 1.1.  TacExtensions are not identical to ngoSubmissions.  Almost more of an analog to
     * the submission accepts within those elements, but comments for example live up at this level.  Deprecating so that
     * hopefully will trigger higher level changes.
     *
     * @return a new list containing all submissions associated with the phaseIProposal
     */
    @Deprecated
    public List<Submission> getTacExtensions() {
        final List<Submission> submissions = new ArrayList<Submission>();
        submissions.addAll(getPhaseIProposal().getSubmissions());

        return submissions;
    }

    public TimeAmount getTotalAwardedTime() {
        return getPhaseIProposal().getTotalAwardedTime();
    }

    public TimeAmount getTotalRecommendedTime() {
        return getPhaseIProposal().getTotalRecommendedTime();
    }

    public TimeAmount getTotalMinRecommendTime() {
        return getPhaseIProposal().getTotalMinRecommendedTime();
    }

    public TimeAmount getTotalRequestedTime() {
        return getPhaseIProposal().getTotalRequestedTime();
    }

    public TimeAmount getTotalMinRequestedTime() {
        return getPhaseIProposal().getTotalMinRequestedTime();
    }
    
    public TimeAmount getTotalTimeForBand(edu.gemini.model.p1.mutable.Band band) {
        TimeAmount total = new TimeAmount(0, TimeUnit.HR);
        for (Observation o : getObservationsForBand(band)) {
            total = total.sum(o.getTime());
        }
        return total;
    }

    /**
     * Is proposal eligible for band 3 scheduling?
     *
     * @return true if the proposal is eligble for band 3 scheduling.
     */
    public boolean isBand3() {
        return getPhaseIProposal().isBand3();
    }

    /**
     * @return true if ALL of the ngo submissions support consideration for poor weather
     * @see #containsPoorWeather
     */
    boolean isEntirelyPoorWeather() {
        final List<Submission> submissions = getPhaseIProposal().getSubmissions();
        boolean atLeastOnePoorWeather = false; // Could be an exchange proposal for example.

        for (Submission s : submissions) {
            final SubmissionAccept submissionAccept = s.getAccept();
            if (!submissionAccept.isPoorWeather())
                return false;
            else
                atLeastOnePoorWeather = true;
        }

        return atLeastOnePoorWeather;
    }

    public boolean isErrors() {
        for (ProposalIssue issue : issues) {
            if (issue.getSeverity().equals(ProposalIssue.Severity.ERROR)) {
                return true;
            }
        }
        return false;
    }

    public boolean isExchange() {
        if (getExchangeFrom() != null) {
            return true;
        }
        if (getExchangeFor() != null) {
            return true;
        }
        return false;
    }

    public Partner getExchangeFrom() {
        final List<Submission> submissions = getPhaseIProposal().getSubmissions();
        for (Submission s: submissions) {
            if (s instanceof ExchangeSubmission) {
                ExchangeSubmission es = (ExchangeSubmission) s;
                return s.getPartner();
            }
        }

        return null;
    }

    /**
     * There are "exchange" elements that are similar to "ngo" elements, but that would
     * indicate a proposal to do observations at Gemini that come from Keck or Subaru clients
     * Note: An identically named method in 1.1 used to return a PartnerCountry.  Be on the lookout
     * for consequences of this turning up in the more dynamic parts of our application.
     *
     * @return either null if the proposal is not an exchange proposal, or the exchange partner from whom time is being requested.
     */
    public ExchangePartner getExchangeFor() {
        if (getPhaseIProposal() instanceof ExchangeProposal) {
            ExchangeProposal ep = (ExchangeProposal) getPhaseIProposal();

            return ep.getPartner();
        }

        return null;
    }

    public boolean isForExchangePartner() {
        return ((getExchangeFor() != null));
    }

    public boolean isFromExchangePartner() {
        return ((getExchangeFrom() != null));
    }

    /**
     * Business rule is "Only proposals from the viewer's partner country have their details visible"  This method
     * evaluates whether the proposal's partner is the same entity as the person parameter's partner.
     *
     * @param person Who are we checking against?
     *
     * @return true if the proposal is fully visible, else false
     */
    public boolean isFullyVisibleTo(Person person) {
        LOGGER.log(Level.DEBUG, "Proposal[" + this.getId() + "] evaluating access for " + person.toString());
        Partner theirPartner = person.getPartner();
        if (theirPartner == null) {
            return false;
        }
        return getPartner().getId().equals(theirPartner.getId());
    }

    public boolean isJoint() {
        return false;
    }

    public boolean isJointComponent() {
        return joint != null;
    }

    /**
     * Returns true if at least one instrument is using a laser component.
     * @return
     */
    public boolean isLgs() {
        for (BlueprintBase b : getBlueprints()) {
            if (b.hasLgs()) {
                return true;
            }
        }
        return false;
    }

    public boolean isMcao() {
        return false; //TODO: Implement -- from blueprints presumably
    }

    /**
     * @return returns this.containsPoorWeather() (N.B.: alternate biz rule could return this.isEntirelyPoorWeather() )
     * @see #isEntirelyPoorWeather()
     * @see #containsPoorWeather()
     */
    public boolean isPw() {
        return containsPoorWeather();
    }

    /**
     * @return 'true' if ANY of the ngo submissions will support a poor weather proposal
     *         has a "Poor Weather" flag set true.
     * @see #isEntirelyPoorWeather()
     */
    boolean containsPoorWeather() {
        final List<Submission> submissions = getPhaseIProposal().getSubmissions();

        for (Submission s : submissions) {
            final SubmissionAccept submissionAccept = s.getAccept();
            if (submissionAccept != null && submissionAccept.isPoorWeather())
                return true;
        }

        return false;
    }

    /**
     * @return <code>true</code> if this proposal deals with a target of opportunity,
     *         <code>false</code> otherwise
     */
    public boolean isToo() {
        return getPhaseIProposal().isToo();
    }

    public boolean isWarnings() {
        for (ProposalIssue issue : issues) {
            if (issue.getSeverity().equals(ProposalIssue.Severity.WARNING)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Modifies the mode of the proposal to classical.  Nulls out any other current modes
     * and adds the principal investigator only as the default visitor.
     *
     * @param shouldBeClassical true if we are turning the proposal into a classical proposal, false if into a queue proposal
     */
    public void setClassical(boolean shouldBeClassical) {
        if(shouldBeClassical){
            setClassical();
        }else{
            setQueueProposal();
        }
    }

    protected void setClassical() {
        final PhaseIProposal p1p = getPhaseIProposalForModification();
        if(ClassicalProposal.class.isAssignableFrom(p1p.getClass())){
            //It already is a clasical -- no behavior necessary;
        }else if(GeminiNormalProposal.class.isAssignableFrom(p1p.getClass())){
            final GeminiNormalProposal gnp = (GeminiNormalProposal) p1p;
            setPhaseIProposal(new ClassicalProposal(gnp));
        }else {
            throw new IllegalArgumentException("Expected PhaseIProposal to be Gemini Normal, not " + p1p.getClass() + " Converting that type into a classical program is not implemented. ");
        }
    }

    protected void setQueueProposal(){
        final PhaseIProposal p1p = getPhaseIProposalForModification();
        if(QueueProposal.class.isAssignableFrom(p1p.getClass())){
            //Already QP

        }else if(ClassicalProposal.class.isAssignableFrom(p1p.getClass())){
            final ClassicalProposal cp = (ClassicalProposal) p1p;
            setPhaseIProposal(new QueueProposal(cp));
        }else{
            throw new IllegalArgumentException("Expected PhaseIProposal to be Classical, not " + p1p.getClass());
        }
    }

    public String toKml() {
        try {
            StringBuilder stringBuilder = new StringBuilder();
            final PhaseIProposal p1p = getPhaseIProposal();
            String placemarkNameBase = p1p.getInvestigators().getPi().getLastName() + "/" + p1p.getSubmissionsKey() + " ";
            for (Observation o : p1p.getObservations()) {
                final Target scienceTarget = o.getTarget();
                String placemarkName = placemarkNameBase + scienceTarget.getName();
                String instrumentName = o.getBlueprint().getInstrument().name();
                String desc = instrumentName + " " + o.getTime().toPrettyString() + "<p/>";

                Condition condition = o.getCondition();
                desc += condition.getName();

                String kml = "<Placemark>\n" +
                        "<name>" + placemarkName + "</name>\n" +
                        "<description>" + desc + "</description>\n" +
                        scienceTarget.toKml() +
                        "</Placemark>";
                stringBuilder.append(kml);
            }

            return stringBuilder.toString();
        } catch (Exception x) {
            return "";
        }
    }

    public String toStringLong() {
        try {
            return new ToStringBuilder(this).
                    append("id", id).
                    append("committee", committee).
                    append("partner", partner).
                    append("phaseIProposal", getPhaseIProposal()).
                    toString();
        } catch (LazyInitializationException lie) {
            return "Proposal[" + id + "].toString() [<-- threw a LazyInitializationException internally -->";
        }
    }

    public String getClassString(){
        String classString = "";
        //Partner
        classString += " " + getPartner().getAbbreviation();
        //North or South; Exchange proposals are assumed to belong to North
        if (getSite() == null) {
            classString += " NORTH";
        } else if(belongsToSite(Site.NORTH)){
            classString += " NORTH";
        } else {
                classString += " SOUTH";
        }
        //Laser Guide Star?
        if (isLgs()) {
            classString += " LGS";
        }
        //MCAO?
        if (isMcao()) {
            classString += " MCAO";
        }
        //Poor Weather?
        if (isPw()) {
            classString += " PW";
        }
        //Not Band 3?
        if (!isBand3()) {
            classString += " NotBand3";
        }
        //Joint?
        if (isJoint()) {
            classString += " Joint";
        }
        //Classical or Queue
        if(getPhaseIProposal().isClassical()){
            classString += " Classical";
        }
        if(getPhaseIProposal().isQueue()){
            classString += " Queue";
        }
        // ToO?
        if (getPhaseIProposal().isToo()){
            classString += " " + getPhaseIProposal().getTooOptionObservingModeClassString();
        }
        // OK, errors, warnings?
        boolean hasErrors = isErrors();
        boolean hasWarnings = isWarnings();
        if (hasErrors) {
            classString += " Errors";
        }
        if (!hasErrors & hasWarnings) {
            classString += " Warnings";
        }
        if (!hasErrors && !hasWarnings) {
            classString += " OK";
        }
        if (getExchangeFrom() != null) {
            classString += " KS4G";
        }

        if (getExchangeFor() != null) {
            classString += " G4KS";
        }

        return classString;
    }


    public boolean isAssignedGeminiId() {
        try {
            final Itac itac = this.getItac();
            final Boolean proposalAccepted = (itac.getAccept() != null);

            if (proposalAccepted &&
                    itac.getAccept().getProgramId() != null &&
                    !(itac.getAccept().getProgramId().equals(Proposal.GEMINI_ID_UNKNOWN))) {
                return true;
            } else {
                return false;
            }
        } catch (NullPointerException e) {
            return false;
        }
    }

    public Set<String> collectInstrumentDisplayNames() {
        final Set<String> instrumentNames = new HashSet<String>();

        for (BlueprintBase b : getPhaseIProposal().getBlueprints()) {
            instrumentNames.add(b.getInstrument().getDisplayName());
        }
        
        return instrumentNames;
    }

    public PhaseIProposal getPhaseIProposalForModification() {
        return getPhaseIProposal();
    }

    public void switchTo(final Site siteToSwitchTo){
        getPhaseIProposalForModification().switchTo(siteToSwitchTo);
    }

    public String getDetailsUrl() {
        return getDetailsUrl(getPhaseIProposal().getPrimary().getReceipt().getReceiptId());
    }

    public String getDetailsUrl(final String userDisplay) {
        return String.format(DETAILS_URL, getCommittee().getId(), getId(), userDisplay);
    }

    public Boolean getChecksBypassed() {
        return checksBypassed;
    }

    public void setChecksBypassed(Boolean checksBypassed) {
        this.checksBypassed = checksBypassed;
    }
}

