package edu.gemini.tac.service;

import org.apache.log4j.Logger;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;


@Service("HibernateStatistics")
public class HibernateStatistics {
    private static final Logger LOGGER = Logger.getLogger(HibernateStatistics.class);

    @Resource(name = "sessionFactory")
    private SessionFactory sessionFactory;


    public Statistics getStatistics() {

        Statistics stats = sessionFactory.getStatistics();
        return stats;

    }
}
