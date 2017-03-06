package edu.gemini.tac.service;

import edu.gemini.tac.persistence.LogEntry;
import edu.gemini.tac.persistence.Proposal;
import edu.gemini.tac.persistence.ProposalIssue;
import edu.gemini.tac.persistence.joints.JointProposal;
import edu.gemini.tac.persistence.queues.Banding;
import edu.gemini.tac.persistence.queues.JointBanding;
import edu.gemini.tac.persistence.queues.Queue;
import org.apache.commons.lang.Validate;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;

/**
 * Supports JointProposal-specific service operations
 *
 * Author: lobrien
 * Date: 1/25/11
 */
@Service("jointProposalService")
@Transactional
public class JointProposalHibernateService extends ProposalHibernateService implements IJointProposalService {
    private static final Logger LOGGER = Logger.getLogger(JointProposalHibernateService.class);

    @Resource(name = "sessionFactory")
    private SessionFactory sessionFactory;

    @Transactional
    @Override
    public JointProposal mergeProposals(final Proposal master, final Proposal secondary) {
        LOGGER.log(Level.DEBUG, "mergeProposals");
        Session session = sessionFactory.getCurrentSession();

        final Proposal mergedMaster = (Proposal) session.merge(master);
        final Proposal mergedSecondary = (Proposal) session.merge(secondary);

        Validate.isTrue(!mergedMaster.getPartner().equals(mergedSecondary.getPartner()), "Cannot create a joint proposal between two component proposals from the same partner.");

        JointProposal merged = new JointProposal();
        mergedMaster.setJointProposal(merged);
        mergedSecondary.setJointProposal(merged);

        Set<Proposal> ps = new HashSet<Proposal>();
        ps.add(mergedMaster);
        ps.add(mergedSecondary);
        merged.setProposals(ps);

        merged.setPrimaryProposal(mergedMaster);

        session.saveOrUpdate(mergedMaster);
        session.saveOrUpdate(mergedSecondary);
        session.save(merged);

        session.refresh(master);
        session.refresh(secondary);

        return merged;
    }

    @Transactional
    @Override
    public JointProposal add(final JointProposal master, final Proposal secondary) {
        LOGGER.log(Level.DEBUG, "add");
        org.hibernate.classic.Session session = sessionFactory.getCurrentSession();

        final JointProposal mergedMaster = (JointProposal) session.merge(master);
        final Proposal mergedSecondary = (Proposal) session.merge(secondary);

        mergedMaster.add(mergedSecondary);
        mergedSecondary.setJointProposal(mergedMaster);

        session.saveOrUpdate(mergedSecondary);
        session.saveOrUpdate(mergedMaster);

        return mergedMaster;
    }

    @Transactional
    @Override
    public void delete(JointProposal p) {
        LOGGER.log(Level.DEBUG, "delete");
        Session currentSession = sessionFactory.getCurrentSession();
        currentSession.update(p);
        p.delete(currentSession);
    }

    @Transactional
    @Override
    public JointProposal duplicateWithNewMaster(JointProposal originalJointProposal, Proposal newMaster) {
        LOGGER.log(Level.DEBUG, "duplicateWithNewMaster");
        final Session s = sessionFactory.getCurrentSession();

        originalJointProposal = (JointProposal) s.merge(originalJointProposal);
        originalJointProposal.setPrimaryProposal(newMaster);
        newMaster = (Proposal) s.merge(newMaster);

        final Set<Proposal> originalComponentProposals = originalJointProposal.getProposals();
        final Set<Proposal> clonedComponentProposals = new HashSet<Proposal>();
        Proposal newClonedMaster = null;
        for (Proposal p : originalComponentProposals) {
            final Proposal clonedProposal = new Proposal(p);
            s.save(clonedProposal);
            if (p.equals(newMaster))
                newClonedMaster = clonedProposal;
            else
                clonedComponentProposals.add(clonedProposal);
        }

        final JointProposal duplicateJointProposal = new JointProposal();
        duplicateJointProposal.add(newClonedMaster);
        duplicateJointProposal.addAll(clonedComponentProposals, s);

        //Delete original
        for(Proposal original : originalComponentProposals){
            s.delete(original);
        }
        s.delete(originalJointProposal);

        s.save(duplicateJointProposal);
       return duplicateJointProposal;
    }

    @Transactional
    @Override
    public Proposal recreateWithoutComponent(final Long committeeId, final Long proposalId, final Long componentId) {
        LOGGER.log(Level.DEBUG, "recreateWithoutComponent(" + proposalId + "," + componentId + ")");

        final Session session = sessionFactory.getCurrentSession();
        final Proposal originalProposal = getProposal(committeeId, proposalId);
        Validate.isTrue(originalProposal instanceof JointProposal);
        final Proposal proposalToBeRemovedFromJoint = getProposal(committeeId, componentId);
        final JointProposal jointProposal = (JointProposal) originalProposal;
        Validate.isTrue(!proposalToBeRemovedFromJoint.getId().equals(jointProposal.getPrimaryProposal().getId()));

        final Set<Proposal> componentProposals = jointProposal.getProposals();
        for (Proposal p : componentProposals) {
            if (p.getId().equals(componentId)) {
                componentProposals.remove(p); // Order is important here...
                p.setJointProposal(null);

                break;
            }
        }

        Proposal p = null;
        if (jointProposal.getProposals().size() > 1) {
            session.saveOrUpdate(jointProposal);
            p = jointProposal;
        } else {
            p = jointProposal.getProposals().iterator().next();
            session.delete(jointProposal);
            session.saveOrUpdate(proposalToBeRemovedFromJoint);
        }
        // Fix for bug REL-1037, ITAC-643
        List deletedIds = session.createQuery("select b.id from JointBanding b where b.proposal.id=:proposal_id").setLong("proposal_id", jointProposal.getId()).list();
        for (Object id: deletedIds) {
            session.createQuery("delete from Banding b where b.joint.id=:joint_id").setLong("joint_id", (Long)id).executeUpdate();
        }

        return p;
    }
}
