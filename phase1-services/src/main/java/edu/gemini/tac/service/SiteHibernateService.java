package edu.gemini.tac.service;

import edu.gemini.tac.persistence.Site;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

/**
 * Concrete implementation of i'face
 *
 * Author: lobrien
 * Date: 3/10/11
 */
@Service("siteService")
@Transactional
public class SiteHibernateService implements ISiteService {
    @Resource(name = "sessionFactory")
    SessionFactory sessionFactory;
    private static final Logger LOGGER = Logger.getLogger(SiteHibernateService.class);

    @Override
    @Transactional(readOnly = true)
    public Site findByDisplayName(String siteName) {
        LOGGER.log(Level.DEBUG, "findById(" + siteName + ")");
        final Session session = sessionFactory.getCurrentSession();
        final Query query = session.createQuery("from Site where displayName = :siteName");
        query.setString("siteName", siteName);
        Site site = (Site) query.uniqueResult();
        return site;
    }

}
