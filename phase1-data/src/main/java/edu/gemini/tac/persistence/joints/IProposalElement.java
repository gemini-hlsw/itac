package edu.gemini.tac.persistence.joints;

import org.hibernate.Session;
import org.hibernate.SessionFactory;

import java.util.List;

/**
 * Indicates that an implementing class may be asked to create a new instance that represents a combination of two others.
 * In other words, implementing classes might be merged. The merging is expected to happen via recursive calls of mergeWith().
 *
 * Strictly speaking, lots of calls to mergeWith() could probably be removed without affecting behavior (since many elements have
 * "master" proposal wins as their rule. But initially, I try to replicate as much of a Proposal's object graph as possible --
 * from Proposal -> Document ... ending at PartnerSubmission | PartnerCountry, ResourceReference | Resource, TargetReference | Target,
 * Investigator | Contact
 *
 * The toDot() method returns a String written in the "Dot" graph-visualizing language. It creates what's essentially an object diagram,
 * showing the class and database identities of objects in the IProposalElement graph.
 * <p/>
 * Date: Dec 8, 2010
 */
public interface IProposalElement {
    /**
     * Returns the value of the database identity field (generally ID)
     * @return
     */
    public Long getEntityId();

    /**
     * Much like toString() but produces a String in the "Dot" graphing language.
     *
     * Basically:
     * digraph foo {
     *    Foo_1;
     *    Foo_1->Bar_2;
     * }
     *
     * @param sf
     * @return
     */
    public String toDot(SessionFactory sf);

    /**
     * Much like toString() but produces a String in the "Dot" graphing language.
     *
     * General pattern is:
     *
     * String toDot() {
     *    String dot = toString() + ";\n"; //Creates "Foo_1;\n"
     *    dot += toString + "->" + bar.toDot() + ";\n"; //Recurse toDot() into the graph
     *    return dot;
     * }
     *
     * Additionally used to fetch into memory all the elements in a particular Proposal's IProposalElement graph, PLUS
     * non-IProposalElement boundary objects. In other words, calling this method OUGHT TO avoid any fear of LazyInitializationExceptions
     * when reading any Proposal's IProposalElement hierarchy.
     *
     *
     * @return
     */
    public String toDot();

    /**
     * When passed a "that" of the same type as "this", returns a new IProposalElement of the same type that is the "joint proposal"
     * merging of the two objects. The Session is necessarily part of the context, as it is likely that detached objects will
     * have to be re-joined.
     *
     * The basic pattern that I used is as follows for an IProposalElement class Foo which has a reference to an IProposalElement Bar
     *
     * 1) Create a copy-constructor for Foo:
     *  public Foo() {} //Necessary for Hibernate
     *  public Foo(Foo that, Session session) {
     *    if(that.getId() != null){
     *       reattachIfNecessary(session, that); //be sure that the other object is attached
     *    }
     *    this.setBar( that.getBar() ); //Copy all fields BUT NOT ID(!)
     *    ... etc ....
     *  }
     *
     * 2) Create a mergeWith function that recursively calls mergeWith on all sub-IProposalElement's in the graph
     *
     *  public Foo mergeWith(IProposalElement that, Session session){
     *     Foo thatt = (Foo) that; //Downcast parameter to same type as this
     *     if(that.getId() != null){
     *        reattachIfNecessary(session, that); //Attach other object
     *     }
     *
     *     Foo mergedFoo = new Foo(this, session); //Call the copy constructor, essentially creating a clone of _this_
     *     //At this point, mergedFoo points to existing Bar and Bats
     *     mergedFoo.bar = this.bar.mergeWith(that.bar, session); //Recurse into the graph
     *     //At this point, mergedFoo.bar points to a new Bar (assuming that Bar.mergeWith() is implemented properly)
     *
     *     //If necessary, call a separate private function to manipulate the DB so as to be compatible with edu.gemini.phase.* packages
     *     mergedFoo.compatibilityFixup(args);
     *
     *     return mergedFoo;
     *  }
     *
     *  The upshot of this pattern is some amount of waste in the copy constructor, since the newly-created Foo points for
     * a moment at the old Bar. But since a copy constructor may be more generally useful, I don't think it's that much of a
     * burden.
     *
     *  More debatable is the recursive mergeWith. Since the majority of elements in a Joint Proposal are "winner takes all" you
     *  could be considerably more efficient by short-circuiting the recursion or even using the Visitor pattern. I implemented a
     * Visitor pattern (and it's in the late Dec 2010 VCS) but I moved away from it when I experienced a lot of difficulty with detached
     * objects. There are many places where you need an explicit call to reattachIfNecessary(session, that) and, if you're going to recurse the graph
     * re-attaching objects, you
     *  might as well do the merge logic at the same time and not traverse the graph twice. On the other hand, if we ever moved to
     * session-per-view, the Visitor pattern might become more attractive again.
     *
     * @param that
     * @param session
     * @return
     */
    // public IProposalElement mergeWith(IProposalElement that, Session session);

}
