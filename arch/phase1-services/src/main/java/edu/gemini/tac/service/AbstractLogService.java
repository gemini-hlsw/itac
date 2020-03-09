package edu.gemini.tac.service;

import edu.gemini.tac.persistence.Committee;
import edu.gemini.tac.persistence.LogEntry;
import edu.gemini.tac.persistence.LogNote;
import edu.gemini.tac.persistence.Proposal;
import edu.gemini.tac.persistence.queues.Queue;
import edu.gemini.tac.util.ProposalImporter;

import java.util.List;
import java.util.Set;

public interface AbstractLogService {
	void addLogEntry(final LogEntry logEntry);
    /**
     * Finds all log entries for a committee except for those associated with a queue.
     *
     * @param committee committee that we need entries for
     * @return all log entries for a committee except for those associated with a queue
     */
	List<LogEntry> getLogEntriesForCommittee(final Committee committee);
	List<LogEntry> getLogEntriesForQueue(final Queue queue);
    List<LogEntry> getLogEntriesForProposal(final Proposal proposal);
	void addNoteToEntry(final LogEntry logEntry, final LogNote logNote);
    void createNoteForEntry(final Long entryId, final String text);
    void updateNote(final Long noteId, final String text);
    void deleteNote(final Long noteId);
    LogEntry createNewForCommittee(final String message, final Set<LogEntry.Type> types, final Committee committee);
    LogEntry createNewForQueue(final String message, final Set<LogEntry.Type> types, final Queue queue);
    LogEntry createNewForProposalOnly(final String message, final Set<LogEntry.Type> types, final Proposal proposal);
    LogEntry createNewForCommitteeAndProposal(final String message, final Set<LogEntry.Type> types, final Committee committee, final Proposal proposal);

    void logSuccessfullyImportedProposals(final List<ProposalImporter.Result> successfulResults);
    void logBandedProposals(final Queue queue);
}
