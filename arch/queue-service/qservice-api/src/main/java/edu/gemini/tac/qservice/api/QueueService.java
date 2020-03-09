package edu.gemini.tac.qservice.api;

import edu.gemini.tac.persistence.queues.Queue;
import edu.gemini.tac.service.AbstractLogService;
import edu.gemini.tac.service.ICommitteeService;

/**
 * The interface to the queue engine.
 */
public interface QueueService {

    /**
     * Generates a queue of proposals storing banding information, logs, and
     * partner time calculation details into the database.
     *
     * @param queue the queue object containing configuration information and
     * into which results are also stored
     *
     * @param committeService
     *
     * @param logService
     */
    void fill(Queue queue, ICommitteeService committeService, AbstractLogService logService);
}
