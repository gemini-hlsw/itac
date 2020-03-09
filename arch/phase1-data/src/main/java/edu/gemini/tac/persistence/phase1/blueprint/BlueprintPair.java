package edu.gemini.tac.persistence.phase1.blueprint;

import edu.gemini.model.p1.mutable.BlueprintBase;

public class BlueprintPair {
    protected final Object blueprintChoice;
    protected final edu.gemini.model.p1.mutable.BlueprintBase blueprintBase;

    public BlueprintPair(final Object blueprintChoice, final edu.gemini.model.p1.mutable.BlueprintBase blueprintBase) {
        this.blueprintBase = blueprintBase;
        this.blueprintChoice = blueprintChoice;
    }

    @Override
    public String toString() {
        return "BlueprintPair{" +
                "blueprintChoice=" + blueprintChoice +
                ", blueprintBase=" + blueprintBase +
                '}';
    }

    public Object getBlueprintChoice() {
        return blueprintChoice;
    }

    public BlueprintBase getBlueprintBase() {
        return blueprintBase;
    }
}
