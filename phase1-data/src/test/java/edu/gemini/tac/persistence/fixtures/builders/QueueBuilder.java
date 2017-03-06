package edu.gemini.tac.persistence.fixtures.builders;

import edu.gemini.tac.persistence.Committee;
import edu.gemini.tac.persistence.Proposal;
import edu.gemini.tac.persistence.phase1.TimeAmount;
import edu.gemini.tac.persistence.queues.Banding;
import edu.gemini.tac.persistence.queues.Queue;
import edu.gemini.tac.persistence.queues.ScienceBand;
import org.hibernate.Session;

/**
 * Builds simple queues for tests.
 */
public class QueueBuilder {
    
    private static int queueCnt = 0;
    
    private Queue queue;
    
    public QueueBuilder(Committee committee) {
        queue = new Queue("Queue "+queueCnt, committee);
        queueCnt++;
    }
    
    public QueueBuilder addBanding(Proposal proposal, ScienceBand band) {
        Banding b = new Banding(queue, proposal, band);
        queue.addBanding(b);
        b.setMergeIndex(1);
        b.setProgramId("No program id.");

        return this;
    }

    public QueueBuilder addBanding(Proposal proposal, ScienceBand band, TimeAmount awardedTime) {
        Banding b = new Banding(queue, proposal, band);
        queue.addBanding(b);
        b.setMergeIndex(1);
        b.setProgramId("No program id.");

        return this;
    }


    public Queue create(Session session) {
        session.save(queue);
        return queue;
    }
}
