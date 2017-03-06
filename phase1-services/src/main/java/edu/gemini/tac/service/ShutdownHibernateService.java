package edu.gemini.tac.service;


import edu.gemini.tac.persistence.Committee;
import edu.gemini.tac.persistence.daterange.Shutdown;
import org.apache.commons.lang.Validate;
import org.apache.log4j.Logger;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;

@Service("shutdownService")
@Transactional
public class ShutdownHibernateService implements IShutdownService {
    @Resource(name = "sessionFactory")
    private SessionFactory sessionFactory;

    private static final Logger LOGGER = Logger.getLogger(ShutdownHibernateService.class.getName());

    @Override
    @Transactional
    public Shutdown save(Shutdown in) {
        final Session session = sessionFactory.getCurrentSession();
        session.saveOrUpdate(in);
        session.saveOrUpdate(in.getCommittee());
        return in;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Shutdown> forCommittee(long committeeId) {
        final Session session = sessionFactory.getCurrentSession();
        final Query query = session.getNamedQuery("shutdown.forCommittee").setLong("committeeId", committeeId);
        final List<Shutdown> shutdowns = query.list();
        return shutdowns;
    }

    @Override
    @Transactional(readOnly = true)
    public Shutdown forId(long shutdownId) {
        final Session session = sessionFactory.getCurrentSession();
        final Query query = session.getNamedQuery("shutdown.forId").setLong("shutdownId", shutdownId);
        final Object result = query.uniqueResult();
        if(result == null){
            return null;
        }
        return (Shutdown) result;
    }

    @Override
    @Transactional
    public Shutdown update(Shutdown in) {
        throw new RuntimeException("TODO");
    }

    @Override
    @Transactional
    public void delete(Shutdown shutdown) {
        final Session session = sessionFactory.getCurrentSession();
        final Committee committee = shutdown.getCommittee();
        boolean confirm = committee.removeShutdown(shutdown);
        Validate.isTrue(confirm);
        session.delete(shutdown);
        session.saveOrUpdate(committee);
    }

    @Override
    @Transactional
    public void delete(long shutdownId) {
        Shutdown shutdown = forId(shutdownId);
        delete(shutdown);
    }
}