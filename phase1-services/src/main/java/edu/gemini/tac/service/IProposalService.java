package edu.gemini.tac.service;

import edu.gemini.tac.persistence.Proposal;
import edu.gemini.tac.persistence.ProposalIssueCategory;
import edu.gemini.tac.util.ProposalImporter;
import edu.gemini.tac.util.ProposalUnwrapper;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.InputStream;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Service layer for Proposals
 *
 * <p/>
 * User: lobrien
 * Date: Dec 2, 2010
 */
public interface IProposalService {
    /**
     * This method is very heavily relied upon.  It is intended to efficiently pull in everything in this
     * proposal.  Everything.  No lazy proxy collections, no lazy proxy objects.  Be careful.
     *
     * @param committeeId
     * @param proposalId
     * @return
     */
    Proposal getProposal(final Long committeeId, final Long proposalId);
    void saveProposal(Proposal proposal);
    void saveEditedProposal(Proposal proposal);
    void switchSites(Proposal proposal);
    byte[] getProposalAsXml(Proposal proposal);
    byte[] getProposalPdf(Proposal proposal, File pdfFolder);
    void importSingleDocument(ProposalImporter importer, ProposalUnwrapper.Entry importFile, Long committeeId);
    void importDocuments(ProposalImporter importer, String fileName, InputStream inputStream, Long committeeId);
    List<Proposal> getProposals(Long committeeId, final IMatch<Proposal> where);
    List<Proposal> getProposalsForQueueAnalysis(String committeeName);
    Proposal editProposalWithAlreadyValidatedData(Long committeeId, Long proposalId, String targetType, String naturalId, String field, String value, StringBuilder fromValueBuffer);
    Proposal populateProposalForListing(final Proposal joint);
    Set<String> getInstrumentNamesForProposal(Proposal p);
    Proposal duplicate(Proposal original, File pdfFolder);

    List<Proposal> getProposalswithItacExtension(Long committeeId);

    void setRolloverEligibility(Long proposalId, boolean rolloverEligible);

    void deleteProposal(Long proposalId, File pdfFolder);

    void checkProposal(Proposal proposal);
    void checkProposals(Collection<Proposal> proposal);

    Set<Proposal> search(Long committeeId, String searchString);

    void setClassical(Proposal p, boolean b);

    Map<ProposalIssueCategory, List<Proposal>> proposalsInProblemCategories(List<Proposal> proposals);

    Proposal populateProposalForQueueCreation(Proposal proposal);

    @Transactional
    Proposal populateProposalForProposalChecking(Proposal p);
}
