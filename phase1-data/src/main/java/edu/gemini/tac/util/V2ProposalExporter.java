package edu.gemini.tac.util;

import edu.gemini.model.p1.mutable.Proposal;
import org.apache.log4j.Logger;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import java.io.FileWriter;
import java.io.IOException;

public class V2ProposalExporter {
    private static final Logger LOGGER = Logger.getLogger(V2ProposalExporter.class.getName());

    public void export(final Proposal proposal) {
        LOGGER.info("Exporting proposal " + proposal.getTitle());
        try {
            final JAXBContext jaxbContext = JAXBContext.newInstance( "edu.gemini.model.p1.mutable" );
            writeToFile(jaxbContext, proposal, "/tmp/crap.xml");
        } catch (JAXBException e) {
            LOGGER.error(e);
        } catch (IOException e) {
            LOGGER.error(e);
        }

    }

    public void writeToFile(JAXBContext context, Proposal proposal, String fileName)
            throws JAXBException, IOException {
        LOGGER.info("Writing proposal " + proposal.getTitle() + " to " + fileName);
        java.io.FileWriter fw = new FileWriter(fileName);

        Marshaller marshaller = context.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        marshaller.marshal(proposal, fw);

        fw.close();
    }
}
