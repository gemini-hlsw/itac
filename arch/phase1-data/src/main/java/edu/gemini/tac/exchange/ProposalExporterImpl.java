package edu.gemini.tac.exchange;

import edu.gemini.tac.persistence.Proposal;
import edu.gemini.tac.persistence.phase1.proposal.PhaseIProposal;
import edu.gemini.tac.util.HibernateToMutableConverter;
import edu.gemini.tac.util.ProposalExporter;
import edu.gemini.tac.util.ProposalSchemaValidator;
import org.apache.commons.lang.Validate;
import org.hibernate.Session;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class ProposalExporterImpl implements ProposalExporter {

    private Session session;

    public ProposalExporterImpl() {
    }

    public ProposalExporterImpl(Session session) {
        this.session = session;
    }

    /**
     * Exports a proposal to xml.
     * This method validates that the produced xml is actually valid.
     * @param proposal
     * @return
     * @throws IOException
     */
    public byte[] getAsXml(Proposal proposal) {
        return getAsXml(proposal.getPhaseIProposal());
    }

    public byte[] getAsXml(PhaseIProposal phaseIProposal) {
        byte[] xml;
        try {
            xml = phaseIProposalToXMLBytes(phaseIProposal);
        } catch (Exception e) {
            throw new RuntimeException("converting to xml failed", e);
        }
        Validate.isTrue(ProposalSchemaValidator.isValid(xml), "Internal error: invalid xml produced by exporter.");
        return xml;
    }

    /**
     * This method is not going to work if it's being called with a joint proposals transient phaseIProposal.
     * @param phaseIProposalId
     * @return
     */
    public byte[] getAsXml(final Long phaseIProposalId) {
        Validate.notNull(phaseIProposalId);
        final PhaseIProposal phaseIProposal = (PhaseIProposal) session.createQuery(
                "from PhaseIProposal p where p.id = :id"
        ).setLong("id", phaseIProposalId).uniqueResult();

        return getAsXml(phaseIProposal);
    }

    private byte[] phaseIProposalToXMLBytes(PhaseIProposal phaseIProposal) throws Exception {
        final edu.gemini.model.p1.mutable.Proposal mutableProposal = HibernateToMutableConverter.toMutable(phaseIProposal);

        final ByteArrayOutputStream byteWriter = new ByteArrayOutputStream(100*1024);

        final JAXBContext jaxbContext = JAXBContext.newInstance( "edu.gemini.model.p1.mutable" );

        Marshaller marshaller = jaxbContext.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        marshaller.marshal(mutableProposal, byteWriter);
        byteWriter.close();

        return byteWriter.toByteArray();
    }
}
