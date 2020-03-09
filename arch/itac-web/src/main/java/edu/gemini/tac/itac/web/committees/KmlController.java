package edu.gemini.tac.itac.web.committees;

import edu.gemini.tac.persistence.Proposal;
import edu.gemini.tac.persistence.queues.Banding;
import edu.gemini.tac.persistence.queues.Queue;
import edu.gemini.tac.service.IProposalService;
import edu.gemini.tac.service.IQueueService;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import java.io.IOException;
import java.util.SortedSet;

/**
 * Fun.
 *
 * Author: lobrien
 * Date: 2/2/11
 */
@Controller(value = "kmlController")
public class KmlController {
    private static final Logger LOGGER = Logger.getLogger(KmlController.class.getName());

    @Resource(name = "proposalService")
    private IProposalService proposalService;

    //TODO: Re-implement @Resource(name = "queueService")
    private IQueueService queueService;

    @RequestMapping(value = "/committees/{committeeId}/proposals/{proposalId}/kml", method = RequestMethod.GET)
    public void index(@PathVariable final Long committeeId, @PathVariable final Long proposalId, HttpServletResponse response) throws IOException {
        Proposal proposal = proposalService.getProposal(committeeId, proposalId);

        String kmlProp = proposal.toKml();

        wrapKml(response, kmlProp, "Proposal-" + proposal.getEntityId() + ".kml");
    }

    @RequestMapping(value = "/committees/{committeeId}/queues/{queueId}/kml", method = RequestMethod.GET)
    public void queueAsKml(@PathVariable final Long committeeId, @PathVariable final Long queueId, HttpServletResponse response) throws IOException {
        StringBuilder sb = new StringBuilder();
        Queue q = queueService.getQueue(queueId);
        SortedSet<Banding> bandings = q.getBandings();
        for (Banding banding : bandings) {
            Proposal p = banding.getProposal();
            Proposal complete = proposalService.getProposal(committeeId, p.getEntityId());
            String kml = complete.toKml();
            sb.append(kml);
            LOGGER.log(Level.DEBUG, "Appending " + kml + " for proposal " + p.getEntityId());
        }
        String fName = "Committee-" + committeeId + "-Q-" + queueId + ".kml";

        wrapKml(response, sb.toString(), fName);
    }

    private void wrapKml(HttpServletResponse response, String kml, String fName) throws IOException {
        String kmlBody = kmlDocPrefix() + kml + kmlDocSuffix();
        HttpServletResponseWrapper wrapper = new HttpServletResponseWrapper(response);
        wrapper.setHeader("Content-disposition", "attachment; filename=" + fName);
        wrapper.setContentType("application/vnd.google-earth.kml+xml; charset=UTF-8");
        wrapper.setHeader("Content-length", "" + kmlBody.getBytes().length);
        wrapper.getWriter().print(kmlBody);
        LOGGER.log(Level.DEBUG, kmlBody);
    }

    private String kmlDocPrefix() {
        return "<kml xmlns=\"http://www.opengis.net/kml/2.2\" hint=\"target=sky\">\n" +
                "<Document>\n" +
                "  <Style id=\"ScienceTarget\">\n" +
                "    <BalloonStyle>\n" +
                "      <text><center><b>$[name]</b></center><br/>$[description]</text>\n" +
                "    </BalloonStyle>\n" +
                "  </Style>";
    }

    private String kmlDocSuffix() {
        return "</Document>\n" +
                "</kml> ";
    }
}
