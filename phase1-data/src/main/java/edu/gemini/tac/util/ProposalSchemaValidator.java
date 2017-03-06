package edu.gemini.tac.util;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.w3c.dom.ls.LSInput;
import org.w3c.dom.ls.LSResourceResolver;

import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.io.Reader;
import java.util.HashSet;
import java.util.Set;

/**
 * Validator for validation of proposal data during import.
 * Detection of any corrupt data or inconsistencies in the proposals that is a reason to keep the proposal from being
 * imported must throw an exception in order to stop the import. The exception message will be displayed to the user.
 * Note: the difference to the proposal checker is, that the rules in the validator will keep propsals from being
 * imported while the checker checks business rules on already imported proposals. Any problems in proposals
 * that keep ITAC from working properly should be enforced here.
 */
public class ProposalSchemaValidator {

    private static final Logger LOGGER = Logger.getLogger("edu.gemini.tac.util.ProposalSchemaValidator");

    static private Schema schema;
    static private Validator schemaValidator;

    protected ProposalSchemaValidator() {}

    /**
     * Get the proposal schema.
     */
    public static Schema getSchema() {
        return schema;
    }
    
    public static boolean isValid(byte[] xml) {
        try { 
            validate(new ByteArrayInputStream(xml));
        } catch (Exception e) {
            LOGGER.log(Level.WARN, "invalid xml: " + e.getMessage());
            return false;
        }
        return true;
    }

    /**
     * Validates the xml file against the Proposal.xsd in edu.gemini.model.p1.
     * SAX and IO exceptions thrown by the validation are wrapped in a runtime exception and passed on to the caller.
     * @param stream
     */
    public static void validate(InputStream stream) {
        try {
            schemaValidator.validate(new StreamSource(stream));
        } catch (Exception ex) {
            // re-throw SAX or IO Exceptions, let the caller take care of them
            throw new RuntimeException(ex.getMessage(), ex);
        }
    }

    /**
     * Some static setup: load the proposal schema from the resources and prepare the infrastructure for validation.
     */
    static {
        try {
            SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            factory.setResourceResolver(createResourceResolver());
            InputStream schemaStream = ProposalSchemaValidator.class.getResourceAsStream("/Proposal.xsd");
            schema = factory.newSchema(new StreamSource(schemaStream));
            schemaValidator = schema.newValidator();
        } catch (Exception ex) {
            LOGGER.log(Level.WARN, "could not initialize schema validator: " + ex.getMessage());
            // re-throw SAX or IO Exceptions
            throw new RuntimeException(ex.getMessage(), ex);
        }
    }

    /**
     * Creates a LSResourceResolver that resolves the referenced xsd files to the files in the resources.
     * Loading a xsd file from the file system works nicely and referenced files are imported as expected.
     * Unfortunately the standard resolver does not work when loading xsd files from the resources therefore
     * we must provide a (very rudimentary) resource resolver here. Note that no xsd file must be loaded
     * twice in order to avoid duplicate definitions.
     * @return
     */
    private static LSResourceResolver createResourceResolver() {
        return new LSResourceResolver() {
            private Set<String> loaded = new HashSet<String>();
            @Override
            public LSInput resolveResource(final String s, final String s1, final String s2, final String s3, final String s4) {
                return new LSInput() {
                    @Override public Reader getCharacterStream() { return null; }
                    @Override public void setCharacterStream(Reader reader) {}

                    @Override public InputStream getByteStream() {
                        if (s3.equals("Proposal.xsd")) { return null; }
                        if (loaded.contains(s3)) { return null; } else loaded.add(s3);
                        return ProposalSchemaValidator.class.getResourceAsStream("/"+s3);
                    }

                    @Override public void setByteStream(InputStream inputStream) {}
                    @Override public String getStringData() { return null; }
                    @Override public void setStringData(String s) {}
                    @Override public String getSystemId() { return null; }
                    @Override public void setSystemId(String s) {}
                    @Override public String getPublicId() {  return null;  }
                    @Override public void setPublicId(String s) {}
                    @Override public String getBaseURI() { return null; }
                    @Override public void setBaseURI(String s) {}
                    @Override public String getEncoding() { return null; }
                    @Override public void setEncoding(String s) {}
                    @Override public boolean getCertifiedText() { return false; }
                    @Override public void setCertifiedText(boolean b) {}
                };
            }
        };
    }
}
