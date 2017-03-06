package edu.gemini.tac.persistence.phase1;

/**
 * Visitor pattern. Target implements accept(TargetVisitor v){ v.visit(this); }
 *
 */
public interface TargetVisitor {
    public void visit(SiderealTarget st);
    public void visit(NonsiderealTarget nst);
    public void visit(TooTarget nst);
}
