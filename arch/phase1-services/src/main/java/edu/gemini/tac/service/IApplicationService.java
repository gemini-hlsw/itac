package edu.gemini.tac.service;

import edu.gemini.tac.persistence.BroadcastMessage;

/**
 * Service for application wide concerns.
 */
public interface IApplicationService {
    public Object findLatestBroadcastMessage();
}
