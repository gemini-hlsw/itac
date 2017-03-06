package edu.gemini.tac.itac.web.committees;

import edu.gemini.tac.itac.web.AbstractApplicationController;
import edu.gemini.tac.itac.web.Link;
import edu.gemini.tac.itac.web.UrlFor;
import edu.gemini.tac.persistence.Site;
import edu.gemini.tac.persistence.rollover.IRolloverObservation;
import edu.gemini.tac.persistence.rollover.RolloverReport;
import edu.gemini.tac.persistence.rollover.RolloverSet;
import edu.gemini.tac.service.IPartnerService;
import edu.gemini.tac.service.IRolloverService;
import edu.gemini.tac.service.ISiteService;
import org.apache.commons.lang.Validate;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.ModelAndView;

import javax.annotation.Resource;
import java.io.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Allows user to specify rollover-eligible programs
 * <p/>
 * Author: lobrien
 * Date: 3/9/11
 */
@Controller("RolloversController")
@RequestMapping("/committees/{committeeId}/rollovers")
public class RolloversController extends AbstractApplicationController {
    public RolloversController() {
        LOGGER.log(Level.WARN, "RolloversController initialized");
    }

    private static final Logger LOGGER = Logger.getLogger(RolloversController.class);

    public Link[] getCollectionLinks(final String topLevelResourceId) {
        ArrayList<Link> links = new ArrayList<Link>();
        /* Collections.addAll(links, new Link[]{
                new Link(UrlFor.Controller.PROPOSALS, UrlFor.Controller.COMMITTEES, topLevelResourceId, "", "List"),
                new Link(UrlFor.Controller.PROPOSALS, UrlFor.Controller.COMMITTEES, topLevelResourceId, "import", "Import"),
                new Link(UrlFor.Controller.PROPOSALS, UrlFor.Controller.COMMITTEES, topLevelResourceId, "log", "Log"),
                new Link(UrlFor.Controller.PROPOSALS, UrlFor.Controller.COMMITTEES, topLevelResourceId, "exchange", "Exchange"),
                new Link(UrlFor.Controller.PROPOSALS, UrlFor.Controller.COMMITTEES, topLevelResourceId, "export-html", "Export HTML")
        });
        */
        Link[] objects = links.toArray(new Link[]{});
        return objects;
    }

    @Value("${itac.rolloverURL.GS}")
    private String gsRolloverUrl;

    @Value("${itac.rolloverURL.GN}")
    private String gnRolloverUrl;

    @Resource(name = "rolloverService")
    IRolloverService rolloverService;

    @Resource(name = "partnerService")
    IPartnerService partnerService;

    @Resource(name = "siteService")
    ISiteService siteService;

    @Override
    protected UrlFor.Controller getController() {
        return UrlFor.Controller.ROLLOVERS;
    }

    @Override
    protected UrlFor.Controller getTopLevelController() {
        return UrlFor.Controller.COMMITTEES;
    }

    @RequestMapping(method = RequestMethod.GET)
    public ModelAndView index(@PathVariable Long committeeId) {
        LOGGER.log(Level.DEBUG, "RolloversController.index()");
        ModelAndView modelAndView = boilerPlateModelAndView(committeeId);

        //Point to JSP
        modelAndView.setViewName("committees/proposals/rollovers");
        return modelAndView;
    }

    @RequestMapping("/{siteName}")
    public ModelAndView rolloverReport(@PathVariable Long committeeId, @PathVariable String siteName) {
        LOGGER.log(Level.DEBUG, "RolloversController.rolloverReport(" + siteName + ")");
        LOGGER.info("itacProperties:" + itacProperties);
        LOGGER.info("gsRolloverUrl:" + gsRolloverUrl);
        LOGGER.info("gnRolloverUrl:" + gnRolloverUrl);
        ModelAndView mav = boilerPlateModelAndView(committeeId);

        mav.addObject("siteName", siteName);
        try {
            URL rolloverServer = new URL(gsRolloverUrl);
            if (siteName.compareToIgnoreCase("North") == 0) {
                rolloverServer = new URL(gnRolloverUrl);
            }

            final String content = readPage(rolloverServer);
            RolloverReport memReport = RolloverReport.parse(content.toString(), partnerService.getPartnersByName());
            //Save it
            RolloverReport report = rolloverService.convert(memReport);
            mav.addObject("report", report);
            Validate.notNull(report);
            Validate.notNull(report.observations());
            Validate.noNullElements(report.observations());
            for (IRolloverObservation observation : report.observations()) {
                Validate.notNull(observation.target());
                Validate.notNull(observation.target().getName());
                Validate.notNull(observation.target().getPositionDisplay());
                Validate.notNull(observation.siteQuality());
                Validate.notNull(observation.siteQuality().getDisplay());
                Validate.notNull(observation.observationTime());
                Validate.notNull(observation.observationTime().getValue());
            }

            mav.setViewName("committees/proposals/rollovers/report");
        } catch (Exception x) {
            mav.addObject("errorMap", new HashMap());
            mav.addObject("errors", x.toString());
            mav.addObject("exception", x.toString());
            mav.setViewName("error");
            LOGGER.log(Level.ERROR, x);

        } finally {
            return mav;
        }
    }

    private String readPage(URL server) throws IOException {
        InputStream is = server.openStream();
        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();
        String line = null;
        while ((line = br.readLine()) != null) {
            sb.append(line);
        }
        return sb.toString();
    }

    @RequestMapping(value = "/{siteName}/RolloverSets", method = RequestMethod.POST)
    public ModelAndView createRolloverSet(@PathVariable Long committeeId,
                                          @PathVariable String siteName,
                                          WebRequest request) {
        LOGGER.log(Level.DEBUG, "RolloversController.createRolloverSite");
        ModelAndView mav = boilerPlateModelAndView(committeeId);
        Site site = siteService.findByDisplayName(siteName);

        String[] timesAllocated = request.getParameterValues("time");
        String[] observationIds = request.getParameterValues("observation");
        String rolloverSetName = request.getParameter("setName");


        RolloverSet rs = rolloverService.createRolloverSet(site, rolloverSetName, observationIds, timesAllocated);
        mav.addObject("set", rs);
        if (true) {
            Validate.notNull(rs);
            Validate.notNull(rs.getId());
        }

        mav.setViewName("redirect:/tac/committees/" + committeeId + "/rollovers/" + siteName + "/rolloverset/" + rs.getId().toString());
        return mav;
    }

    @RequestMapping(value = "/{siteName}/rolloverset/{rolloverSetId}")
    public ModelAndView getRolloverSet(@PathVariable Long committeeId, @PathVariable String siteName, @PathVariable Long rolloverSetId) {
        ModelAndView mav = boilerPlateModelAndView(committeeId);
        RolloverSet rs = rolloverService.getRolloverSet(rolloverSetId);
        mav.addObject("set", rs);
        mav.setViewName("committees/proposals/rollovers/rolloverset");

        return mav;
    }

    private ModelAndView boilerPlateModelAndView(Long committeeId) {
        ModelAndView modelAndView = new ModelAndView();

        modelAndView.addObject("user", getUser());
        CommitteesController.addCommitteeToModel(committeeService, committeeId, modelAndView);
        addResourceInformation(modelAndView.getModelMap());

        addSubHeaders(modelAndView.getModelMap(), CommitteesController.getSubheaderLinks(committeeId));
        addSubSubHeaders(modelAndView.getModelMap(), getCollectionLinks(String.valueOf(committeeId)));
        addTopLevelResource(modelAndView.getModelMap(), String.valueOf(committeeId));

        return modelAndView;
    }
}
