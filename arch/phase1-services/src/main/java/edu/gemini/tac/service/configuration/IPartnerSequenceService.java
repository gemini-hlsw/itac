package edu.gemini.tac.service.configuration;

import edu.gemini.tac.persistence.phase1.queues.PartnerSequence;

import java.util.List;

/**
 *
 */
public interface IPartnerSequenceService {
    Long create(Long committeeId, String name, String csv, Boolean repeat);

    PartnerSequence getForId(Long id);

    PartnerSequence getForName(Long committeeId, String name);

    List<PartnerSequence> getAll(Long committeeId);

    void delete(Long id);

    void delete(Long committeeId, String name);

    void delete(PartnerSequence ps);

}
