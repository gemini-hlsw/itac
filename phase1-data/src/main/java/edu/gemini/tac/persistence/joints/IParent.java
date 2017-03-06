package edu.gemini.tac.persistence.joints;

import org.hibernate.Session;

/**
 * Tagging interface that indicates that instances of a class will be referred to by some IParented class's PARENT_CLASS, PARENT_ID database columns
 *
 * @see ParentHelper
 * @see BaseParented
 * @see IParented
 *
 * Author: lobrien
 * Date: 5/31/11
 */
public interface IParent {
}
