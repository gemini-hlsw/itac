package edu.gemini.tac.service;

import edu.gemini.tac.service.reports.Report;
import edu.gemini.tac.service.reports.ReportBean;
import org.hibernate.SessionFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;

/**
* Created by IntelliJ IDEA.
* User: fnussber
* Date: 12/22/10
* Time: 2:15 PM
* To change this template use File | Settings | File Templates.
*/
@Service("reportService")
public class ReportHibernateService implements IReportService {

    @Resource(name = "sessionFactory")
    private SessionFactory sessionFactory;

    @Transactional (readOnly = true)
    @Override
    public List<ReportBean> collectReportDataForCommittee(Report report, Long committeeId) {
        return report.collectDataForCommittee(sessionFactory.getCurrentSession(), committeeId);
    }

    @Transactional (readOnly = true)
    @Override
    public List<ReportBean> collectReportDataForQueue(Report report, Long committeeId, Long queueId) {
        return report.collectDataForQueue(sessionFactory.getCurrentSession(), committeeId, queueId);
    }

//    @Transactional (readOnly = true)
//    @Override
//    public List<ReportBean> collectReportDataForProposal(Report report, Long committeeId, Long proposalId) {
//        return report.collectDataForProposal(sessionFactory.getCurrentSession(), committeeId, proposalId);
//    }

}
