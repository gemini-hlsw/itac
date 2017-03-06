package edu.gemini.tac.util;

import edu.gemini.tac.persistence.Proposal;

import java.io.IOException;

/**
 * Interface for ProposalExporter.
 */
public interface ProposalExporter {
    public byte[] getAsXml(Proposal proposal);
    public byte[] getAsXml(Long documentId);
}
