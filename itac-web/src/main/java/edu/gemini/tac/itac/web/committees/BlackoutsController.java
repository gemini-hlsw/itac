package edu.gemini.tac.itac.web.committees;

import edu.gemini.tac.itac.web.AbstractApplicationController;
import edu.gemini.tac.itac.web.UrlFor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Allows the user to specify Blackout / "Instrument unavailable" periods for a given resource
 *
 * Author: lobrien
 * Date: 4/7/11
 */

@Controller("BlackoutsController")
@RequestMapping("/committees/{committeeId}/blackouts")
public class BlackoutsController extends AbstractApplicationController {
//    private static final Logger LOGGER = Logger.getLogger(BlackoutsController.class);
//
//    public BlackoutsController() {
//        super();
//    }
//
//    @javax.annotation.Resource(name = "resourceService")
//    IResourceService resourceService;

    @Override
    protected UrlFor.Controller getController() {
        return UrlFor.Controller.BLACKOUTS;
    }

    @Override
    protected UrlFor.Controller getTopLevelController() {
        return UrlFor.Controller.COMMITTEES;
    }

//    @RequestMapping(method = RequestMethod.GET)
//    public ModelAndView index(@PathVariable Long committeeId) {
//        LOGGER.log(Level.DEBUG, "BlackoutsController.index()");
//        ModelAndView modelAndView = boilerPlateModelAndView(committeeId);
//
//        Map<String, List<Blackout>> blackoutsByInstrumentName = new HashMap<String, List<Blackout>>();
//        List<String> instrumentNames = resourceService.getResourceNames();
//        for(String instrumentName : instrumentNames){
//            List<Blackout> blackouts = resourceService.getBlackoutsFor(instrumentName);
//            blackoutsByInstrumentName.put(instrumentName, blackouts);
//
//        }
//
//        modelAndView.addObject("blackoutsByInstrument", blackoutsByInstrumentName);
//
//        //Point to JSP
//        modelAndView.setViewName("committees/blackouts/index");
//        return modelAndView;
//    }
//
//    @RequestMapping(method=RequestMethod.POST)
//    public ModelAndView createBlackout(@PathVariable Long committeeId, @RequestParam String instrumentName, @RequestParam String startDate, @RequestParam String endDate) throws ParseException {
//        LOGGER.log(Level.DEBUG, "BlackoutController.createBlackout()");
//        //Create it
//        Resource rsc = resourceService.getRepresentativeResource(instrumentName);
//        DateFormat df = new SimpleDateFormat("yy-MM-dd");
//        Date sDate = df.parse(startDate);
//        Date eDate = df.parse(endDate);
//        DateRange dr = new DateRange(sDate, eDate);
//        DateRangePersister drp = new DateRangePersister(dr);
//        Blackout b = new Blackout();
//        b.setDateRangePersister(drp);
//        b.setParent(rsc);
//        resourceService.addBlackout(b);
//        Long blackoutId = b.getId();
//
//        ModelAndView mav = new ModelAndView();
//        mav.addObject("blackoutId", blackoutId);
//        mav.addObject("startDate", startDate);
//        mav.addObject("endDate", endDate);
//
//        mav.setViewName("committees/blackouts/blackoutJson");
//        //and return ajax-y data
//        return mav;
//    }
//
//    @RequestMapping(value = "blackout/{blackoutId}", method = RequestMethod.DELETE)
//    public ModelAndView deleteBlackout(@PathVariable Long committeeId, @PathVariable Long blackoutId){
//        LOGGER.log(Level.DEBUG, "BlackoutController.deleteBlackout()");
//        resourceService.deleteBlackout(blackoutId);
//        ModelAndView mav = new ModelAndView();
//
//        //Not really any content to return, so just point it to an existing small json response
//        mav.setViewName("committees/blackouts/blackoutJson");
//        return mav;
//    }
//
//    public Link[] getCollectionLinks(final String topLevelResourceId) {
//        ArrayList<Link> links = new ArrayList<Link>();
//        Link[] objects = links.toArray(new Link[]{});
//        return objects;
//    }
//
//    private ModelAndView boilerPlateModelAndView(Long committeeId) {
//        ModelAndView modelAndView = new ModelAndView();
//
//        modelAndView.addObject("user", getUser());
//        CommitteesController.addCommitteeToModel(committeeService, committeeId, modelAndView);
//        addResourceInformation(modelAndView.getModelMap());
//
//        addSubHeaders(modelAndView.getModelMap(), CommitteesController.getSubheaderLinks(committeeId));
//        addSubSubHeaders(modelAndView.getModelMap(), getCollectionLinks(String.valueOf(committeeId)));
//        addTopLevelResource(modelAndView.getModelMap(), String.valueOf(committeeId));
//
//        return modelAndView;
//    }
}
