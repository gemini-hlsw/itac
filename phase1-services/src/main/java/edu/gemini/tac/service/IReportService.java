package edu.gemini.tac.service;

import edu.gemini.tac.service.reports.Report;
import edu.gemini.tac.service.reports.ReportBean;

import java.util.List;

/**
* Created by IntelliJ IDEA.
* User: fnussber
* Date: 12/22/10
* Time: 2:15 PM
* To change this template use File | Settings | File Templates.
*/
public interface IReportService {

    List<ReportBean> collectReportDataForCommittee(Report report, Long committeeId);
    List<ReportBean> collectReportDataForQueue(Report report, Long committeeId, Long queueId);
}
