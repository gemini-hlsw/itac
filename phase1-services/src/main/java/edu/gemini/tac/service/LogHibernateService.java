package edu.gemini.tac.service;

import edu.gemini.tac.persistence.Committee;
import edu.gemini.tac.persistence.LogEntry;
import edu.gemini.tac.persistence.LogNote;
import edu.gemini.tac.persistence.Proposal;
import edu.gemini.tac.persistence.queues.Banding;
import edu.gemini.tac.persistence.queues.Queue;
import edu.gemini.tac.util.ProposalImporter;
import org.apache.commons.lang.Validate;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.*;

@Service("logService")
public class LogHibernateService implements AbstractLogService {
	@Resource(name = "sessionFactory")
	private SessionFactory sessionFactory;

	@Override
    @Transactional
	public void addLogEntry(final LogEntry logEntry) {
		final Session session = sessionFactory.getCurrentSession();

		session.save(logEntry);
	}

	@Override
    @Transactional
	public void createNoteForEntry(final Long entryId, final String text) {
		final Session session = sessionFactory.getCurrentSession();
        LogEntry logEntry = getLogEntry(entryId);
        LogNote logNote = new LogNote(text);
		logEntry.getNotes().add(logNote);
		session.saveOrUpdate(logEntry);

	}

	@Override
    @Transactional
	public void addNoteToEntry(final LogEntry logEntry, final LogNote logNote) {
		final Session session = sessionFactory.getCurrentSession();

		logEntry.getNotes().add(logNote);
		session.saveOrUpdate(logEntry);

	}

	@Override
    @Transactional
	public void updateNote(final Long logNoteId, final String text) {
		final Session session = sessionFactory.getCurrentSession();
        final LogNote note = getNote(logNoteId);
        note.setText(text);
        session.saveOrUpdate(note);
	}


	@Override
    @Transactional
	public void deleteNote(final Long logNoteId) {
		final Session session = sessionFactory.getCurrentSession();
        final LogNote note = getNote(logNoteId);
        final LogEntry entry = note.getLogEntry();
        entry.getNotes().remove(note);
		session.saveOrUpdate(entry);
	}

    /**
     * Finds all log entries for a committee except for those associated with a queue.
     *
     * @param committee committee that we need entries for
     * @return all log entries for a committee except for those associated with a queue
     */
	@Override
	@SuppressWarnings("unchecked")
    @Transactional(readOnly = true)
	public List<LogEntry> getLogEntriesForCommittee(final Committee committee) {
		final Session session = sessionFactory.getCurrentSession();

		final Query logEntryQuery = session.getNamedQuery("logEntry.getForCommittee").setLong("committeeId", committee.getId());
		final List<LogEntry> list = logEntryQuery.list();
		
		return list;
	}

	@Override
	@SuppressWarnings("unchecked")
    @Transactional(readOnly = true)
	public List<LogEntry> getLogEntriesForQueue(final Queue queue) {
		final Session session = sessionFactory.getCurrentSession();

		final Query logEntryQuery = session.getNamedQuery("logEntry.getForQueue").setLong("queueId", queue.getId());
		final List<LogEntry> list = logEntryQuery.list();
		
		return list;
	}

    @Override
    @SuppressWarnings("unchecked")
    @Transactional(readOnly = true)
    public List<LogEntry> getLogEntriesForProposal(final Proposal proposal) {
        final Session session = sessionFactory.getCurrentSession();

        final Query logEntryQuery = session.getNamedQuery("logEntry.getForProposal").setLong("proposalId", proposal.getId());
        final List<LogEntry> list = logEntryQuery.list();

        return list;
    }

    private LogEntry getLogEntry(final Long entryId) {
        final Session session = sessionFactory.getCurrentSession();
        final Query logEntryQuery = session.createQuery("from LogEntry e where e.id = :entryId").setLong("entryId", entryId);
        final LogEntry entry = (LogEntry)logEntryQuery.uniqueResult();
        return entry;
    }

    private LogNote getNote(final Long noteId) {
        final Session session = sessionFactory.getCurrentSession();
        final Query logEntryQuery = session.createQuery("from LogNote n where n.id = :noteId").setLong("noteId", noteId);
        final LogNote note = (LogNote)logEntryQuery.uniqueResult();
        return note;
    }

    @Override
    public LogEntry createNewForCommittee(final String message, final Set<LogEntry.Type> types, final Committee committee) {
        final LogEntry logEntry = createNewLogEntry(message, types);
        logEntry.setCommittee(committee);
        logEntry.setQueue(null);
        logEntry.setProposal(null);

        return logEntry;
    }

    @Override
    public LogEntry createNewForCommitteeAndProposal(final String message, final Set<LogEntry.Type> types, final Committee committee, final Proposal proposal) {
        Validate.isTrue(committee.getId().equals(proposal.getCommittee().getId()));

        final LogEntry logEntry = createNewLogEntry(message, types);
        logEntry.setCommittee(committee);
        logEntry.setQueue(null);
        logEntry.setProposal(proposal);

        return logEntry;
    }

    @Override
    public LogEntry createNewForQueue(final String message, final Set<LogEntry.Type> types, final Queue queue) {
        final LogEntry logEntry = createNewLogEntry(message, types);
        logEntry.setCommittee(queue.getCommittee());
        logEntry.setQueue(queue);
        logEntry.setProposal(null);

        return logEntry;
    }

    @Override
    public LogEntry createNewForProposalOnly(final String message, final Set<LogEntry.Type> types, final Proposal proposal) {
        final LogEntry logEntry = createNewLogEntry(message, types);
        logEntry.setCommittee(null);
        logEntry.setQueue(null);
        logEntry.setProposal(proposal);

        return logEntry;
    }

    private LogEntry createNewLogEntry(final String message, final Set<LogEntry.Type> types) {
        final LogEntry logEntry = new LogEntry();
        logEntry.setCreatedAt(new Date());
        logEntry.setUpdatedAt(new Date());
        logEntry.setMessage(message);
        logEntry.setNotes(new HashSet<LogNote>());
        logEntry.setTypes(types);
        return logEntry;
    }

    /**
     * Logs individually to the proposal, does not connect it to the committee.
     *
     * @param successfulResults
     */
    @Override
    @Transactional
    public void logSuccessfullyImportedProposals(final List<ProposalImporter.Result> successfulResults) {
        final Session session = sessionFactory.getCurrentSession();
        for (ProposalImporter.Result result : successfulResults) {
            final String message = "Imported from " + result.getFileName() + " => " + result.getMessage();
            final Proposal proposal = (Proposal) session.createQuery("from Proposal p where p.id = :proposalId").setLong("proposalId", result.getProposalId()).uniqueResult();
            final LogEntry logEntry = createNewForProposalOnly(message,
                    new HashSet<LogEntry.Type>() {{
                        add(LogEntry.Type.PROPOSAL_ADDED);
                    }},
                    proposal);

            session.saveOrUpdate(logEntry);
        }
    }

    /**
     * Logs individually to the proposal, does not connect it to the committee.
     *
     * @param queue
     */
    @Override
    @Transactional
    public void logBandedProposals(final Queue queue) {
        final Session session = sessionFactory.getCurrentSession();
        for (Banding b : queue.getBandings()) {
            final String queueHref = "<a href=\"/tac/committees/" + queue.getCommittee().getId() + "/queues/" + queue.getId() + "\">" + queue.getName() + "</a>";
            final String message = "Proposal accepted into " + queueHref + " in band " + b.getBand().getDescription() + " at merge index " + b.getMergeIndex();
            final LogEntry logEntry = createNewForProposalOnly(message,
                    new HashSet<LogEntry.Type>() {{
                        add(LogEntry.Type.PROPOSAL_QUEUE_GENERATED);
                    }},
                    b.getProposal());

            session.saveOrUpdate(logEntry);
        }
    }
}
