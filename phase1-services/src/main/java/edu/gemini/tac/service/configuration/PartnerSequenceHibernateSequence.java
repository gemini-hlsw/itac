package edu.gemini.tac.service.configuration;

import edu.gemini.tac.persistence.phase1.queues.PartnerSequence;
import edu.gemini.tac.service.ICommitteeService;
import org.apache.commons.lang.Validate;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;

/**
 * Hibernate implementation
 */
@Service(value = "partnerSequenceService")
public class PartnerSequenceHibernateSequence implements IPartnerSequenceService {
    @Resource(name = "sessionFactory")
    private SessionFactory sessionFactory;

    @Resource(name = "committeeService")
    private ICommitteeService committeeService;

    @Override
    @Transactional
    public Long create(Long committeeId, String name, String csv, Boolean repeat){
        Validate.notNull(committeeId);
        Validate.notNull(name, "Name is null");
        Validate.isTrue(name.length() > 0, "Name is empty");
        Validate.notNull(csv, "CSV is null");
        Validate.isTrue(csv.length() > 0, "CSV is empty");
        Validate.notNull(repeat);
        Validate.isTrue(getForName(committeeId, name) == null, "Name already exists");

        PartnerSequence ps = new PartnerSequence(committeeService.getCommittee(committeeId), name, csv, repeat);
        sessionFactory.getCurrentSession().saveOrUpdate(ps);
        return ps.id();
    }

    @Override
    @Transactional
    public PartnerSequence getForId(Long id) {
        Session session = sessionFactory.getCurrentSession();
        Query query = session.getNamedQuery("PartnerSequence.forId").setLong("id", id);
        return (PartnerSequence) query.uniqueResult();
    }

    @Override
    @Transactional
    public PartnerSequence getForName(Long committeeId, String name) {
        Session session = sessionFactory.getCurrentSession();
        Query query = session.getNamedQuery("PartnerSequence.forName")
                .setLong("committeeId", committeeId).setParameter("sequence_name", name);
        return (PartnerSequence) query.uniqueResult();
    }

    @Override
    @Transactional
    public List<PartnerSequence> getAll(Long committeeId) {
        Session session = sessionFactory.getCurrentSession();
        Query query = session.getNamedQuery("PartnerSequence.allForCommittee").setLong("committee_id", committeeId);
        return (List<PartnerSequence>) query.list();
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Session session = sessionFactory.getCurrentSession();
        PartnerSequence ps = getForId(id);
        session.delete(ps);
    }

    @Override
    @Transactional
    public void delete(Long committeeId, String name) {
        Session session = sessionFactory.getCurrentSession();
        PartnerSequence ps = getForName(committeeId, name);
        session.delete(ps);
    }

    @Override
    @Transactional
    public void delete(PartnerSequence ps) {
        Session session = sessionFactory.getCurrentSession();
        session.merge(ps);
        session.delete(ps);
    }
}
