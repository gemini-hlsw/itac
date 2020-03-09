package edu.gemini.tac.service;

import edu.gemini.tac.persistence.Committee;
import edu.gemini.tac.persistence.LogEntry;
import edu.gemini.tac.persistence.fixtures.FastHibernateFixture;
import edu.gemini.tac.persistence.queues.Queue;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.annotation.Resource;
import java.util.HashSet;
import java.util.List;

import static org.junit.Assert.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations={"/services-applicationContext.xml"})
public class LogServiceTest extends FastHibernateFixture.WithQueues {
	@Resource(name = "logService")
	private AbstractLogService service;

    @Resource(name = "committeeService")
    private ICommitteeService committeeService;

	@Test
    @Ignore //TODO: Need a persisted queue with proposals or you get a JDBC exception on this (null IDs)
	public void addLogEntry() {
        final Committee c = committeeService.getAllActiveCommittees().get(0);
        final List<LogEntry> entriesBefore = service.getLogEntriesForCommittee(c);
		final LogEntry entry = service.createNewForCommittee("Hola!", new HashSet<LogEntry.Type>(), c);
        service.addLogEntry(entry);
        final List<LogEntry> entriesAfter = service.getLogEntriesForCommittee(c);
        assertEquals(entriesAfter.size(), entriesBefore.size()+1);
	}

	@Test
	@Ignore
	public void addNoteToEntry() {
		fail("not implemented yet");
	}

	@Test
	public void getLogEntriesForCommittee() {
		final Committee c = committeeService.getAllActiveCommittees().get(0);
		final List<LogEntry> logEntriesForCommittee = service.getLogEntriesForCommittee(c);
		assertNotNull(logEntriesForCommittee);
		assertEquals(0, logEntriesForCommittee.size()); // Changed from 7 to 3 as the semantics have been changed so that it does not return entries associated with a queue.

        final Committee c2 = committeeService.getAllActiveCommittees().get(1);
		final List<LogEntry> logEntriesForCommittee2 = service.getLogEntriesForCommittee(c2);
		assertNotNull(logEntriesForCommittee2);
		assertEquals(0, logEntriesForCommittee2.size());

	}

	@Test
    @Ignore //TODO: Build up a fixture with a queue
	public void getLogEntriesForQueue() {
		final Committee c = committeeService.getAllActiveCommittees().get(0);
        final Queue queue = c.getQueues().iterator().next();
        final List<LogEntry> logEntries = service.getLogEntriesForQueue(queue);
		assertNotNull(logEntries);
		assertEquals(3, logEntries.size());
	}
}