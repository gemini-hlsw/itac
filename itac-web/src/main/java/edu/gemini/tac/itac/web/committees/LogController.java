package edu.gemini.tac.itac.web.committees;

import edu.gemini.tac.itac.web.AbstractApplicationController;
import edu.gemini.tac.itac.web.Link;
import edu.gemini.tac.itac.web.UrlFor;
import edu.gemini.tac.persistence.Committee;
import edu.gemini.tac.persistence.LogEntry;
import edu.gemini.tac.persistence.Person;
import edu.gemini.tac.persistence.Proposal;
import edu.gemini.tac.persistence.queues.Queue;
import edu.gemini.tac.service.*;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import javax.annotation.Resource;
import javax.management.ObjectName;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * Logs are more associated with queues and committees than proposals, so I'm promoting this to a top-level, always
 * accessible bit.
 */
@Controller
public class LogController extends AbstractApplicationController {
    private static final Logger LOGGER = Logger.getLogger(CommitteesController.class.getName());

    @Resource(name = "logService")
    private AbstractLogService logService;

    @Resource(name = "committeeService")
    private ICommitteeService committeeService;

    @Resource(name = "queueService")
    private IQueueService queueService;

    @Resource(name = "proposalService")
    private IProposalService proposalService;

    @Resource(name = "partnerService")
    private IPartnerService partnerService;

    public LogController() {
        super();
    }

    public static Link[] getSubheaderLinks(final Long topLevelResourceId) {
        return CommitteesController.getSubheaderLinks(topLevelResourceId);
    }

    @RequestMapping(value = "/committees/{committeeId}/log", method = RequestMethod.GET)
    public ModelAndView showLog(@PathVariable final Long committeeId,
                                @RequestParam(required = false) final String initialSearch) {
        LOGGER.log(Level.ALL, "show log");

        final ModelAndView modelAndView = createModelAndView(committeeId);

        modelAndView.addObject("initialSearch", initialSearch);
        addCommitteeNonQueueEntriesToLog(modelAndView, committeeService.getCommittee(committeeId));
        modelAndView.setViewName("committees/log");

        return modelAndView;
    }

    @RequestMapping(value = "/committees/{committeeId}/queues/{queueId}/log", method = RequestMethod.GET)
    public ModelAndView log(@PathVariable final Long committeeId,
                            @PathVariable final Long queueId,
                            @RequestParam(required = false) final String initialSearch) {
        LOGGER.debug("show log");

        final Queue queue = queueService.getQueue(queueId);
        final ModelAndView modelAndView = createModelAndView(committeeId);
        modelAndView.setViewName("committees/queues/log");

        final List<LogEntry> logEntries = logService.getLogEntriesForQueue(queue);

        modelAndView.addObject("queue", queueService.getQueueWithoutData(queueId));
        modelAndView.addObject("partners", partnerService.findAllPartners());
        modelAndView.addObject("logEntries", logEntries);
        modelAndView.addObject("initialSearch", initialSearch);

        return modelAndView;
    }



    @RequestMapping(value = "/committees/{committeeId}/log", method = RequestMethod.POST)
    public ModelAndView editLog(
            @PathVariable final Long committeeId,
            @RequestParam(required = false) final String submit,
            @RequestParam(required = false) final Long entryId,
            @RequestParam(required = false) final Long noteId,
            @RequestParam(required = false) final String text
            ) {
        LOGGER.log(Level.ALL, "edit log");

        final Person user = getUser();
        final Committee committee = committeeService.getCommittee(committeeId);

        handleLogNoteAction(entryId, noteId, submit, text);

        final String logMessage = getUser().getName() + " performed action " + submit + " on committee log notes with text " + text + " on " + noteId + "/" + entryId;
        logService.addLogEntry(logService.createNewForCommittee(logMessage, new HashSet<LogEntry.Type>(), committeeService.getCommittee(committeeId)));

        // and reload the whole thing...
        final ModelAndView modelAndView = createModelAndView(committeeId);
        addCommitteeNonQueueEntriesToLog(modelAndView, committeeService.getCommittee(committeeId));
        modelAndView.setViewName("redirect:/tac/committees/" + committeeId + "/log");

        return modelAndView;
    }

    @RequestMapping(value = "/committees/{committeeId}/queues/{queueId}/log", method = RequestMethod.POST)
    public ModelAndView editQueueLog(
            @PathVariable final Long committeeId,
            @PathVariable final Long queueId,
            @RequestParam(required = false) final String submit,
            @RequestParam(required = false) final Long entryId,
            @RequestParam(required = false) final Long noteId,
            @RequestParam(required = false) final String text
    ) {
        LOGGER.debug("edit queue log");

        handleLogNoteAction(entryId, noteId, submit, text);

        final String logMessage = getUser().getName() + " performed action " + submit + " on queue log notes with text " + text + " on " + noteId + "/" + entryId;
        logService.addLogEntry(logService.createNewForCommittee(logMessage, new HashSet<LogEntry.Type>(), committeeService.getCommittee(committeeId)));

        // reload the whole log
        final ModelAndView modelAndView = createModelAndView(committeeId);
        modelAndView.setViewName("redirect:/tac/committees/" + committeeId + "/queues/" + queueId + "/log");

        return modelAndView;
    }

    @RequestMapping(value = "/committees/{committeeId}/proposals/{proposalId}/log", method = RequestMethod.GET)
    public ModelAndView proposalLog(@PathVariable final Long committeeId,
                            @PathVariable final Long proposalId,
                            @RequestParam(required = false) final String initialSearch) {
        LOGGER.log(Level.DEBUG, "log(" + committeeId + ", " + proposalId + ")");

        final Proposal proposal = proposalService.getProposal(committeeId, proposalId);
        final List<LogEntry> logEntriesForProposal = logService.getLogEntriesForProposal(proposal);

        final ModelAndView modelAndView = createModelAndView(committeeId);
        modelAndView.addObject("proposal", proposal);
        modelAndView.addObject("logEntries", logEntriesForProposal);
        modelAndView.addObject("initialSearch", initialSearch);
        modelAndView.setViewName("committees/proposals/log");

        return modelAndView;
    }



    @RequestMapping(value = "/committees/{committeeId}/proposals/{proposalId}/log", method = RequestMethod.POST)
    public ModelAndView editProposalLog(
            @PathVariable final Long committeeId,
            @PathVariable final Long proposalId,
            @RequestParam(required = false) final String submit,
            @RequestParam(required = false) final Long entryId,
            @RequestParam(required = false) final Long noteId,
            @RequestParam(required = false) final String text
    ) {
        LOGGER.debug("edit proposal log");

        handleLogNoteAction(entryId, noteId, submit, text);

        final String logMessage = getUser().getName() + " performed action " + submit + " on proposal log notes with text " + text + " on " + noteId + "/" + entryId;
        logService.addLogEntry(logService.createNewForCommittee(logMessage, new HashSet<LogEntry.Type>(), committeeService.getCommittee(committeeId)));

        // reload the whole log
        final ModelAndView modelAndView = createModelAndView(committeeId);
        modelAndView.setViewName("redirect:/tac/committees/" + committeeId + "/proposals/" + proposalId + "/log");


        return modelAndView;
    }

    private void handleLogNoteAction(Long entryId, Long noteId, String action, String text) {
        // do edit / update...
        if ("Add Note".equals(action)) {
            logService.createNoteForEntry(entryId, text);
        } else if ("Save Note".equals(action)) {
            logService.updateNote(noteId, text);
        } else if ("Delete Note".equals(action)) {
            logService.deleteNote(noteId);
        }
    }


    /**
     * By default, will return a model and view with view set to the committee log.
     *
     * @param committeeId
     * @return
     */
    private ModelAndView createModelAndView(Long committeeId) {
        ModelAndView modelAndView = new ModelAndView();
        final Committee committee = CommitteesController.addCommitteeToModel(committeeService, committeeId, modelAndView);
        addUserToModel(modelAndView);
        final ModelMap modelMap = modelAndView.getModelMap();
        addResourceInformation(modelMap);

        addSubHeaders(modelMap, CommitteesController.getSubheaderLinks(committeeId));
        addTopLevelResource(modelMap, String.valueOf(committeeId));

        modelAndView.addObject("committeeId", committeeId);

        return modelAndView;
    }

    private void addCommitteeNonQueueEntriesToLog(ModelAndView modelAndView, Committee committee) {
        final List<LogEntry> logEntries = logService.getLogEntriesForCommittee(committee);
        final List<LogEntry> nonqueueLogEntries = new ArrayList<LogEntry>();
        for (LogEntry le : logEntries) {
            if (le.getQueue() == null) {
                nonqueueLogEntries.add(le);
            }
        }

        modelAndView.addObject("logEntries", nonqueueLogEntries);
    }

    @Override
    protected UrlFor.Controller getTopLevelController() {
        return UrlFor.Controller.COMMITTEES;
    }

    @Override
    protected UrlFor.Controller getController() {
        return UrlFor.Controller.LOG;
    }
}
