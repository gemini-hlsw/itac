package edu.gemini.tac.persistence.phase1.blueprint.gmoss;

import edu.gemini.tac.persistence.phase1.blueprint.BlueprintBase;
import edu.gemini.tac.persistence.phase1.blueprint.BlueprintPair;
import edu.gemini.tac.persistence.phase1.blueprint.gmosn.GmosNBlueprintNull;
import org.apache.commons.lang.NotImplementedException;

import javax.persistence.Entity;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * An empty blueprint for GMOS-N. This type of blueprint is not creatable in PIT, but can be instantiated
 * by "switch sites" use-case
 */

@Entity
public class GmosSBlueprintNull extends BlueprintBase {

    @Override
    public BlueprintPair toMutable() {
        throw new NotImplementedException();
    }

    @Override
    public BlueprintBase getComplementaryInstrumentBlueprint() {
        return new GmosNBlueprintNull();
    }

    @Override
    public Map<String, Set<String>> getResourcesByCategory(){
        return new HashMap<>();
    }

    @Override
    public boolean isMOS() {
        return false;
    }

    @Override
    public String getDisplayCamera() {
        throw new NotImplementedException();
    }

    @Override
    public String getDisplayFocalPlaneUnit() {
        throw new NotImplementedException();
    }

    @Override
    public String getDisplayDisperser() {
        throw new NotImplementedException();
    }

    @Override
    public String getDisplayFilter() {
        throw new NotImplementedException();
    }

    @Override
    public String getDisplayAdaptiveOptics() {
        throw new NotImplementedException();
    }

    @Override
    public String getDisplayOther() {
        throw new NotImplementedException();
    }
}
