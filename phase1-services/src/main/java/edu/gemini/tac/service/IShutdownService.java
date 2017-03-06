package edu.gemini.tac.service;


import edu.gemini.tac.persistence.daterange.*;

import java.util.List;

public interface IShutdownService {
    Shutdown save(Shutdown in);
    List<Shutdown> forCommittee(long committeeId);
    Shutdown forId(long shutdownId);
    Shutdown update(Shutdown in);
    void delete(Shutdown shutdown);
    void delete(long shutdownId);
}
