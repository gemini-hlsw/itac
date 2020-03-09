package edu.gemini.tac.service;

import edu.gemini.tac.persistence.BroadcastMessage;
import org.apache.log4j.Logger;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

@Service("applicationService")
@Transactional
public class ApplicationHibernateService implements IApplicationService {
    private static final Logger LOGGER = Logger.getLogger(ApplicationHibernateService.class.getName());

    @Resource(name = "sessionFactory")
    private SessionFactory sessionFactory;

    @Override
    public BroadcastMessage findLatestBroadcastMessage() {
        final Session session = sessionFactory.getCurrentSession();
        final Query query =session.getNamedQuery("broadcastMessage.findLatest").setMaxResults(1);

        final BroadcastMessage broadcastMessage = (BroadcastMessage) query.uniqueResult();

        return (broadcastMessage != null) ? broadcastMessage : null;
    }
}
