package edu.gemini.tac.persistence.daterange;

import edu.gemini.shared.util.DateRange;
import org.hibernate.Hibernate;
import org.hibernate.HibernateException;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.usertype.CompositeUserType;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;

/**
 * Serialize a DateRange
 *
 * Author: lobrien
 * Date: 4/4/11
 */

public class DateRangeUserType implements CompositeUserType {

    @Override
    public Class returnedClass() {
        return DateRange.class;
    }

    @Override
    public boolean isMutable() {
        return false;
    }

    @Override
    public Object deepCopy(Object value) throws HibernateException {
        return new DateRange((DateRange) value);
    }

    @Override
    public boolean equals(Object x, Object y) throws HibernateException {
        if(x == y){
            return true;
        }
        if(x == null || y == null){
            return false;
        }
        return x.equals(y);
    }

    @Override
    public int hashCode(Object x) throws HibernateException {
        return x.hashCode();
    }

    @Override
    public Serializable disassemble(Object value, SessionImplementor session) throws HibernateException {
        return (Serializable) value;
    }

    @Override
    public Object assemble(Serializable cached, SessionImplementor session, Object owner) throws HibernateException {
        return cached;
    }

    @Override
    public Object replace(Object original, Object target, SessionImplementor session, Object owner) throws HibernateException {
        return original;
    }


    @Override
    public Object nullSafeGet(ResultSet rs, String[] names, SessionImplementor session, Object owner) throws HibernateException, SQLException {
        Date start = rs.getDate(names[0]);
        if (rs.wasNull()) {
            return null;
        }
        Date end = rs.getDate(names[1]);
        DateRange dr = new DateRange(start, end);
        return dr;
    }

    @Override
    public void nullSafeSet(PreparedStatement st, Object value, int index, SessionImplementor session) throws HibernateException, SQLException {
        if (value == null) {
            st.setNull(index, Hibernate.DATE.sqlType());
            st.setNull(index + 1, Hibernate.DATE.sqlType());
        } else {
            DateRange dr = (DateRange) value;
            java.sql.Date start = new java.sql.Date(dr.getStartDate().getTime());
            st.setDate(index, start);
            java.sql.Date end = new java.sql.Date(dr.getEndDate().getTime());
            st.setDate(index + 1, end);
        }
    }

    public String[] getPropertyNames() {
        return new String[] { "start", "end" };
    }

    public org.hibernate.type.Type[] getPropertyTypes() {
        return new org.hibernate.type.Type[] { Hibernate.DATE, Hibernate.DATE };
    }

    public Object getPropertyValue(Object component, int property){
        DateRange dr = (DateRange) component;
        if(property == 0){
            return dr.getStartDate();
        }else{
            return dr.getEndDate();
        }
    }

    public void setPropertyValue(Object component, int property, Object value){
        throw new UnsupportedOperationException("Immutable DateRange");
    }

}
