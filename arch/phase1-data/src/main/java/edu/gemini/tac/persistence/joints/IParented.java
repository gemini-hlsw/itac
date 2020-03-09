package edu.gemini.tac.persistence.joints;

import edu.gemini.tac.persistence.Proposal;
import org.hibernate.Session;

/**
 * Implementors have PARENT_CLASS, PARENT_ID columns in their database columns. By implementing this and @see IParent,
 * these values can be set using @see ParentHelper and reflection.
 *
 * Author: lobrien
 * Date: 3/23/11
 */
public interface IParented {
    void setParent(IParent parent, Session session);

    //public i'face to Hibernate columns
    Long getParentId();
    void setParentId(Long parentId);

    String getParentClass();
    void setParentClass(String className);

    IParent getParent(Session session);
    IParent getGreatestAncestor(Session session);
}
