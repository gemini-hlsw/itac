package edu.gemini.tac.persistence.util;

import org.apache.log4j.Logger;

import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.util.Date;
import java.util.GregorianCalendar;

public class Conversion {
    protected static final Logger LOGGER = Logger.getLogger(Conversion.class.getName());

    protected Conversion() {}

    public static final XMLGregorianCalendar dateToXmlGregorian(final Date date) {
        GregorianCalendar calendar = new GregorianCalendar();
        calendar.setTime(date);
        try {
            return DatatypeFactory.newInstance().newXMLGregorianCalendar(calendar);
        } catch (Exception e) {
            LOGGER.error("Configuration problem when converting from date[" + date.toString() + "] to XML calendar.", e);
            throw new RuntimeException("Unable to get instance of datatype xml gregorian calendar for conversion.");
        }
    }
}
