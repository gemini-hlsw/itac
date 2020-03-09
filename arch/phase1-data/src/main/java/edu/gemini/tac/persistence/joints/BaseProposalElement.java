package edu.gemini.tac.persistence.joints;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.SessionFactory;

import javax.persistence.Id;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Map;

/**
 * Abstract class to help out with IProposalElement boilerplate
 * <p/>
 * User: lobrien
 * Date: 12/29/10
 */
public abstract class BaseProposalElement implements IProposalElement {
    private static final Logger LOGGER = Logger.getLogger(BaseProposalElement.class);
    public static final String DOT_END_LINE = ";\n";

    @Override
    public Long getEntityId() {
        try {
            //Mostly "id"
            Method m = this.getClass().getMethod("getId");
            return (Long) m.invoke(this);
        } catch (Exception x) {
            //Cycle over 'em, looking for annotation
            try {
                for (Field f : this.getClass().getDeclaredFields()) {
                    f.setAccessible(true);
                    if (null != f.getAnnotation(Id.class)) {
                        return (Long) f.get(this);
                    }
                }
                //No identity value
                throw new RuntimeException("No field with ID annotation");
            } catch (Exception x2) {
                throw new RuntimeException(x2);
            }
        }
    }

    @Override
    public String toString() {
        try {
            String i = this.getEntityId().toString();
            return String.format("%s_%s", this.getClass().getSimpleName().toString(), i);
        } catch (NullPointerException x) {
            return String.format("%s_NULL", this.getClass().getSimpleName().toString());
        }
    }

    public String toDot(SessionFactory sf) {
        Session s = sf.openSession();
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("digraph IProposalElement{\n");
            sb.append("graph [rankdir=LR, size=\"9.5,11\", page=\"11.000000,8.500000\", margin=\"0.25,0.25\", center=true];\nnode [label=\"\\N\"];\n");
            String in = toDot(this, "", s);
            return sb.append(in) + "}";
        } finally {
            s.close();
        }
    }

    protected String toDot(IProposalElement src, String accumulator, Session session) {
        reattachIfNecessary(session, src);
        return src.toDot();
    }

    @Override
    public String toDot() {
        String dot = this.toString() + DOT_END_LINE;
        try {
            for (Field f : this.getClass().getDeclaredFields()) {
                f.setAccessible(true);
                if (IProposalElement.class.isAssignableFrom(f.getType())) {
                    IProposalElement child = (IProposalElement) f.get(this);
                    dot = addToDot(dot, f, child);
                }
                if (Collection.class.isAssignableFrom(f.getType())) {
                    if (f.get(this) != null) {
                        for (Object o : ((Collection) f.get(this))) {
                            if (IProposalElement.class.isAssignableFrom(o.getClass())) {
                                IProposalElement child = (IProposalElement) o;
                                dot = addToDot(dot, f, child);
                            }
                        }
                    }
                }
                if (Map.class.isAssignableFrom(f.getType())) {
                    Map childMap = (Map) f.get(this);
                    //Type erasure means we lose the ability to test Map<K,V>() type
                    for (Object key : childMap.keySet()) {
                        if (IProposalElement.class.isAssignableFrom(key.getClass())) {
                            IProposalElement child = (IProposalElement) key;
                            dot += this.toString() + "->" + child.toDot() + DOT_END_LINE;
                        }
                    }
                    for (Object value : childMap.values()) {
                        if (IProposalElement.class.isAssignableFrom(value.getClass())) {
                            IProposalElement v = (IProposalElement) value;
                            dot += this.toString() + "->" + v.toDot() + DOT_END_LINE;
                        }
                    }
                }
            }
        } catch (Exception x) {
            LOGGER.log(Level.WARN, "Exception in toDot() for " + this.toString(), x);
        }
        return dot;
    }

    private String addToDot(String dot, Field f, IProposalElement child) {
        if (child != null) {
            dot += this.toString() + "->" + child.toDot() + DOT_END_LINE;
        } else {
            dot += this.toString() + "->" + f.getType().getSimpleName() + "_NULL;\n";
        }
        return dot;
    }

    protected void reattachIfNecessary(Session session, IProposalElement proposalElement) {
        if (proposalElement.getEntityId() != null) {
            session.update(proposalElement);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o.getClass().equals(this.getClass())) {
            IProposalElement that = (IProposalElement) o;
            if (this.getEntityId() == null || that.getEntityId() == null || !this.getEntityId().equals(getEntityId())) {
                return false;
            }
            return true;
        }
        return false;
    }

    @Override
     public int hashCode() {
        int hashCode = 1;
        hashCode = 29 * hashCode + (getEntityId() == null ? 0 : getEntityId().hashCode());

        return hashCode;
    }
}
