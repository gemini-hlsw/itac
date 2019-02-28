package edu.gemini.tac.persistence.phase1.blueprint;


import edu.gemini.model.p1.mutable.*;
import edu.gemini.tac.persistence.IValidateable;
import edu.gemini.tac.persistence.Site;
import edu.gemini.tac.persistence.phase1.Instrument;
import edu.gemini.tac.persistence.phase1.Observation;
import edu.gemini.tac.persistence.phase1.blueprint.alopeke.AlopekeBlueprint;
import edu.gemini.tac.persistence.phase1.blueprint.zorro.ZorroBlueprint;
import edu.gemini.tac.persistence.phase1.blueprint.gmoss.GmosSBlueprintLongSlit;
import edu.gemini.tac.persistence.phase1.blueprint.dssi.DssiBlueprint;
import edu.gemini.tac.persistence.phase1.blueprint.visitor.VisitorGNBlueprint;
import edu.gemini.tac.persistence.phase1.blueprint.visitor.VisitorGSBlueprint;
import edu.gemini.tac.persistence.phase1.proposal.PhaseIProposal;

import javax.persistence.*;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Procedure adding a new instrument (spread out across multiple classes unfortunately) with GSAOI as an example.  Also
 * see JIRA issue, http://swgserv01.cl.gemini.edu:8080/browse/ITAC-612
 *
 * <ul>
 * <li>Add any new components as in http://source.gemini.edu:8060/browse/Software/time-allocation-committee/trunk/phase1-data/src/main/database/078_gsaoi_new_instrument.sql?r=48026</li>
 * <li>Add instrument to ITAC model as in http://source.gemini.edu:8060/browse/Software/time-allocation-committee/trunk/phase1-data/src/main/java/edu/gemini/tac/persistence/phase1/Instrument.java?r=48026</li>
 * <li>Add specialized component query as in http://source.gemini.edu:8060/browse/Software/time-allocation-committee/trunk/phase1-data/src/main/java/edu/gemini/tac/persistence/phase1/blueprint/BlueprintBase.java?r=48026</li>
 * <li>Add new query to array of queries as in http://source.gemini.edu:8060/browse/Software/time-allocation-committee/trunk/phase1-data/src/main/java/edu/gemini/tac/persistence/phase1/blueprint/BlueprintBase.java?r=48026</li>
 * <li>Add to factory method as in http://source.gemini.edu:8060/browse/Software/time-allocation-committee/trunk/phase1-data/src/main/java/edu/gemini/tac/persistence/phase1/blueprint/BlueprintBase.java?r=48026</li>
 * <li>Create new blueprint as in http://source.gemini.edu:8060/changelog/Software?cs=48026
 * <ul>
 *     <li>Needs a constructor from the mutable blueprint.</li>
 *     <li>Needs a toMutable</li>
 *     <li>Implement the abstract methods from BlueprintBase related to display for the configuration information (necessary for reporting et al.</li>
 *     <li>Identify the complementary instrument for switch site use case as in http://source.gemini.edu:8060/browse/Software/time-allocation-committee/trunk/phase1-data/src/main/java/edu/gemini/tac/persistence/phase1/blueprint/niri/NiriBlueprint.java?r=48026</li>
 * </ul>
 * </li>
 * </ul>
 */
@NamedQueries({
        @NamedQuery(name = "BlueprintBase.resourcesFlamingos2BlueprintImaging",
                query = "from Flamingos2BlueprintBase b " +
                        "left join fetch b.filters f " +
                        "where b in (:blueprints)"
        ),
        @NamedQuery(name = "BlueprintBase.resourcesGmosNBlueprintImaging",
                query = "from GmosNBlueprintImaging b " +
                        "left join fetch b.filters f " +
                        "where b in (:blueprints)"
        ),
        @NamedQuery(name = "BlueprintBase.resourcesGmosSBlueprintImaging",
                query = "from GmosSBlueprintImaging b " +
                        "left join fetch b.filters f " +
                        "where b in (:blueprints)"
        ),
        @NamedQuery(name = "BlueprintBase.resourcesMichelleBlueprintImaging",
                query = "from MichelleBlueprintImaging b " +
                        "left join fetch b.filters f " +
                        "where b in (:blueprints)"
        ),
        @NamedQuery(name = "BlueprintBase.resourcesNiciBlueprintCoronagraphicRed",
                query = "from NiciBlueprintCoronagraphic b " +
                        "left join fetch b.redFilters f " +
                        "where b in (:blueprints)"
        ),
        @NamedQuery(name = "BlueprintBase.resourcesNiciBlueprintCoronagraphicBlue",
                query = "from NiciBlueprintCoronagraphic b " +
                        "left join fetch b.blueFilters f " +
                        "where b in (:blueprints)"
        ),
        @NamedQuery(name = "BlueprintBase.resourcesNiciBlueprintStandardRed",
                query = "from NiciBlueprintStandard b " +
                        "left join fetch b.redFilters f " +
                        "where b in (:blueprints)"
        ),
        @NamedQuery(name = "BlueprintBase.resourcesNiciBlueprintStandardBlue",
                query = "from NiciBlueprintStandard b " +
                        "left join fetch b.blueFilters f " +
                        "where b in (:blueprints)"
        ),
        @NamedQuery(name = "BlueprintBase.resourcesNiriBlueprint",
                query = "from NiriBlueprint b " +
                        "left join fetch b.filters f " +
                        "where b in (:blueprints)"
        ),
        @NamedQuery(name = "BlueprintBase.resourcesTrecsBlueprintImaging",
                query = "from TrecsBlueprintImaging b " +
                        "left join fetch b.filters f " +
                        "where b in (:blueprints)"
        ),
        @NamedQuery(name = "BlueprintBase.resourcesGsaoiBlueprint",
                query = "from GsaoiBlueprint b " +
                        "left join fetch b.filters f " +
                        "where b in (:blueprints)"
        ),
        @NamedQuery(name = "BlueprintBase.resourcesDssiBlueprint",
                query = "from DssiBlueprint b where b in (:blueprints)"
        ),
        @NamedQuery(name = "BlueprintBase.resourcesAlopekeBlueprint",
                query = "from AlopekeBlueprint b where b in (:blueprints)"
        ),
        @NamedQuery(name = "BlueprintBase.resourceZorroBlueprint",
                query = "from ZorroBlueprint b where b in (:blueprnts)"
        ),
        @NamedQuery(name = "BlueprintBase.resourcesGpiBlueprint",
                query = "from GpiBlueprint b where b in (:blueprints)"
        ),
        @NamedQuery(name = "BlueprintBase.resourcesPhoenixBlueprint",
                query = "from PhoenixBlueprint b where b in (:blueprints)"
        ),
        @NamedQuery(name = "BlueprintBase.resourcesTexesBlueprint",
                query = "from TexesBlueprint b where b in (:blueprints)"
        ),
        @NamedQuery(name = "BlueprintBase.resourcesVisitorGSBlueprint",
                query = "from VisitorGSBlueprint b where b in (:blueprints)"
        ),
        @NamedQuery(name = "BlueprintBase.resourcesVisitorGNBlueprint",
                query = "from VisitorGNBlueprint b where b in (:blueprints)"
        ),
        @NamedQuery(name = "BlueprintBase.resourcesGracesBlueprint",
                query = "from GracesBlueprint b where b in (:blueprints)"
        )
})
@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "type", discriminatorType = DiscriminatorType.STRING)
@Table(name = "v2_blueprints")
abstract public class BlueprintBase implements IValidateable, Serializable {
    public static final String DISPLAY_NOT_APPLICABLE = "-";
    public final static String[] filterQueryNames = {
        "BlueprintBase.resourcesFlamingos2BlueprintImaging",
        "BlueprintBase.resourcesGmosNBlueprintImaging",
        "BlueprintBase.resourcesGmosSBlueprintImaging",
        "BlueprintBase.resourcesMichelleBlueprintImaging",
        "BlueprintBase.resourcesNiciBlueprintCoronagraphicRed",
        "BlueprintBase.resourcesNiciBlueprintCoronagraphicBlue",
        "BlueprintBase.resourcesNiciBlueprintStandardRed",
        "BlueprintBase.resourcesNiciBlueprintStandardBlue",
        "BlueprintBase.resourcesNiriBlueprint",
        "BlueprintBase.resourcesTrecsBlueprintImaging",
        "BlueprintBase.resourcesGsaoiBlueprint",
        "BlueprintBase.resourcesGpiBlueprint",
        "BlueprintBase.resourcesPhoenixBlueprint",
        "BlueprintBase.resourcesDssiBlueprint",
        "BlueprintBase.resourcesAlopekeBlueprint",
        "BlueprintBase.resourcesZorroBlueprint",
        "BlueprintBase.resourcesTexesBlueprint",
        "BlueprintBase.resourcesVisitorGSBlueprint",
        "BlueprintBase.resourcesVisitorGNBlueprint",
        "BlueprintBase.resourcesGracesBlueprint"
    };
    public static final String NO_FACTORY_FOR = "No factory for ";

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    protected Long id;

    @Column(name = "name")
    protected String name;

    @Column(name = "blueprint_id")
    protected String blueprintId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "phase_i_proposal_id")
    protected PhaseIProposal phaseIProposal;

    @OneToMany(cascade = CascadeType.REMOVE, mappedBy = "blueprint", fetch = FetchType.LAZY)
    protected Set<Observation> observations = new HashSet<>();

    @Enumerated(value = EnumType.STRING)
    @Column(name = "instrument")
    protected Instrument instrument;

    protected BlueprintBase() {}

    public void validate() {
        if ((name == null) || (instrument == null))
            throw new IllegalStateException("Blueprints must have a name and instrument" + toString());
    }

    public BlueprintBase(final String id, final String name, final  Instrument instrument){
        this(id, name, instrument, new HashSet<>());
    }

    public BlueprintBase(final String id, final String name, final Instrument instrument, final Set<Observation> observations) {
        setBlueprintId(id);
        setName(name);
        setInstrument(instrument);
        setObservations(observations);
    }

    /**
     * Converts instrument into it's mutable cousin.  This is usually done in preparation for serialization to
     * or from XML.
     */
    public abstract BlueprintPair toMutable();

    public final boolean isAtNorth() { return instrument.getSite() == Site.NORTH; }
    public final boolean isAtSouth() { return instrument.getSite() == Site.SOUTH; }
    public final boolean isAtSubaru() { return instrument == Instrument.SUBARU; }
    public final boolean isAtKeck() { return instrument == Instrument.KECK; }
    public final boolean isExchange() { return instrument.getSite() == null; }
    public final Site getSite() {if (instrument == null) return null; else return instrument.getSite();}

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BlueprintBase)) return false;

        BlueprintBase that = (BlueprintBase) o;

        if (blueprintId != null ? !blueprintId.equals(that.blueprintId) : that.blueprintId != null) return false;
        if (instrument != that.instrument) return false;
        return !(name != null ? !name.equals(that.name) : that.name != null);

    }

    /**
     * Looser version of equals in order to support joint->component observation identification for
     * propagation of edits.  Needs equivalent instrument, name and resolution of display values.
     */
    public boolean equalsForComponent(Object o) {
        if (this == o) return true;
        if (!(o instanceof BlueprintBase)) return false;

        BlueprintBase that = (BlueprintBase) o;

        if (instrument != that.instrument) return false;
        return !(name != null ? !name.equals(that.name) : that.name != null) && getDisplay().equals(that.getDisplay());

    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (blueprintId != null ? blueprintId.hashCode() : 0);
        result = 31 * result + (instrument != null ? instrument.hashCode() : 0);
        return result;
    }

    /**
     * Converts mutable "choices" into the ITAC model equivalent, which is very similar but a little less awkward
     * to work with.
     *
     * Grawr.  Auto generated classes that's type happy instead of property happy.  Fragile.
     *
     * @param blueprintChoice - a mutable blueprint choice that may be any of a number of different instrument bases.
     *
     * @return an instance that has converted that blueprint choice into a persistence hierarchy blueprint.
     */
    public static BlueprintBase fromMutable(final Object blueprintChoice) {
        if (blueprintChoice instanceof Flamingos2BlueprintChoice) {
            return convertFlamingos2Blueprint((Flamingos2BlueprintChoice) blueprintChoice);
        } else if (blueprintChoice instanceof GmosNBlueprintChoice) {
            return convertGmosNBlueprint((GmosNBlueprintChoice) blueprintChoice);
        } else if (blueprintChoice instanceof GmosSBlueprintChoice) {
            return convertGmosSBlueprint((GmosSBlueprintChoice) blueprintChoice);
        } else if (blueprintChoice instanceof NiriBlueprintChoice) {
            return convertNiriBlueprint((NiriBlueprintChoice) blueprintChoice);
        } else if (blueprintChoice instanceof GnirsBlueprintChoice) {
            return convertGnirsBlueprint((GnirsBlueprintChoice) blueprintChoice);
        } else if (blueprintChoice instanceof MichelleBlueprintChoice) {
            return convertMichelleBlueprint((MichelleBlueprintChoice) blueprintChoice);
        } else if (blueprintChoice instanceof NiciBlueprintChoice) {
            return convertNiciBlueprint((NiciBlueprintChoice) blueprintChoice);
        } else if (blueprintChoice instanceof NifsBlueprintChoice) {
            return convertNifsBlueprint((NifsBlueprintChoice) blueprintChoice);
        } else if (blueprintChoice instanceof TrecsBlueprintChoice) {
            return convertTrecsBlueprint((TrecsBlueprintChoice) blueprintChoice);
        } else if (blueprintChoice instanceof GsaoiBlueprintChoice) {
            return convertGsaoiBlueprint((GsaoiBlueprintChoice) blueprintChoice);
        } else if (blueprintChoice instanceof GracesBlueprintChoice) {
            return convertGracesBlueprint((GracesBlueprintChoice) blueprintChoice);
        } else if (blueprintChoice instanceof GpiBlueprintChoice) {
            return convertGpiBlueprint((GpiBlueprintChoice) blueprintChoice);
        } else if (blueprintChoice instanceof PhoenixBlueprintChoice) {
            return convertPhoenixBlueprint((PhoenixBlueprintChoice) blueprintChoice);
        } else if (blueprintChoice instanceof DssiBlueprintChoice) {
            return convertDssiBlueprint((DssiBlueprintChoice) blueprintChoice);
        } else if (blueprintChoice instanceof AlopekeBlueprintChoice) {
            return convertAlopekeBlueprint((AlopekeBlueprintChoice) blueprintChoice);
        } else if (blueprintChoice instanceof ZorroBlueprintChoice) {
            return convertZorroBlueprint((ZorroBlueprintChoice) blueprintChoice);
        } else if (blueprintChoice instanceof TexesBlueprintChoice) {
            return convertTexesBlueprint((TexesBlueprintChoice) blueprintChoice);
        } else if (blueprintChoice instanceof VisitorBlueprintChoice) {
            return convertVisitorBlueprint((VisitorBlueprintChoice) blueprintChoice);
        } else if (blueprintChoice instanceof KeckBlueprint) {
            return new edu.gemini.tac.persistence.phase1.blueprint.exchange.KeckBlueprint((KeckBlueprint) blueprintChoice);
        } else if (blueprintChoice instanceof SubaruBlueprint) {
            return new edu.gemini.tac.persistence.phase1.blueprint.exchange.SubaruBlueprint((SubaruBlueprint) blueprintChoice);
        } else {
            throw new IllegalArgumentException(NO_FACTORY_FOR + blueprintChoice.toString());
        }
    }

    private static BlueprintBase convertGsaoiBlueprint(final GsaoiBlueprintChoice choice) {
        if (choice.getGsaoi() != null)
            return new edu.gemini.tac.persistence.phase1.blueprint.gsaoi.GsaoiBlueprint(choice.getGsaoi());
        else
            throw new IllegalArgumentException(NO_FACTORY_FOR + choice.toString());
    }

    private static BlueprintBase convertGracesBlueprint(final GracesBlueprintChoice choice) {
        if (choice.getGraces() != null)
            return new edu.gemini.tac.persistence.phase1.blueprint.graces.GracesBlueprint(choice.getGraces());
        else
            throw new IllegalArgumentException(NO_FACTORY_FOR + choice.toString());
    }

    private static BlueprintBase convertGpiBlueprint(final GpiBlueprintChoice choice) {
        if (choice.getGpi() != null)
            return new edu.gemini.tac.persistence.phase1.blueprint.gpi.GpiBlueprint(choice.getGpi());
        else
            throw new IllegalArgumentException(NO_FACTORY_FOR + choice.toString());
    }

    private static BlueprintBase convertPhoenixBlueprint(final PhoenixBlueprintChoice choice) {
        if (choice.getPhoenix() != null)
            return new edu.gemini.tac.persistence.phase1.blueprint.phoenix.PhoenixBlueprint(choice.getPhoenix());
        else
            throw new IllegalArgumentException(NO_FACTORY_FOR + choice.toString());
    }

    private static BlueprintBase convertTrecsBlueprint(TrecsBlueprintChoice choice) {
        if (choice.getImaging() != null) {
            return new edu.gemini.tac.persistence.phase1.blueprint.trecs.TrecsBlueprintImaging(choice.getImaging());
        } else if (choice.getSpectroscopy() != null) {
            return new edu.gemini.tac.persistence.phase1.blueprint.trecs.TrecsBlueprintSpectroscopy(choice.getSpectroscopy());
        } else
            throw new IllegalArgumentException(NO_FACTORY_FOR + choice.toString());

    }

    private static BlueprintBase convertNiciBlueprint(NiciBlueprintChoice choice) {
        if (choice.getCoronagraphic() != null) {
            return new edu.gemini.tac.persistence.phase1.blueprint.nici.NiciBlueprintCoronagraphic(choice.getCoronagraphic());
        } else if (choice.getStandard() != null) {
            return new edu.gemini.tac.persistence.phase1.blueprint.nici.NiciBlueprintStandard(choice.getStandard());
        } else
            throw new IllegalArgumentException(NO_FACTORY_FOR + choice.toString());
    }

    private static BlueprintBase convertFlamingos2Blueprint(final Flamingos2BlueprintChoice choice) {
        if (choice.getImaging() != null) {
            return new edu.gemini.tac.persistence.phase1.blueprint.flamingos2.Flamingos2BlueprintImaging(choice.getImaging());
        } else if (choice.getLongslit() != null) {
            return new edu.gemini.tac.persistence.phase1.blueprint.flamingos2.Flamingos2BlueprintLongslit(choice.getLongslit());
        } else if (choice.getMos() != null) {
            return new edu.gemini.tac.persistence.phase1.blueprint.flamingos2.Flamingos2BlueprintMos(choice.getMos());
        } else
            throw new IllegalArgumentException(NO_FACTORY_FOR + choice.toString());
    }

    private static BlueprintBase convertNifsBlueprint(NifsBlueprintChoice choice) {
        if (choice.getAo() != null)
            return new edu.gemini.tac.persistence.phase1.blueprint.nifs.NifsBlueprintAo(choice.getAo());
        else if (choice.getNonAo() != null)
            return new edu.gemini.tac.persistence.phase1.blueprint.nifs.NifsBlueprint(choice.getNonAo());
        else
            throw new IllegalArgumentException(NO_FACTORY_FOR + choice.toString());
    }

    private static BlueprintBase convertMichelleBlueprint(MichelleBlueprintChoice choice) {
        if (choice.getImaging() != null)
            return new edu.gemini.tac.persistence.phase1.blueprint.michelle.MichelleBlueprintImaging(choice.getImaging());
        else if (choice.getSpectroscopy() != null)
            return new edu.gemini.tac.persistence.phase1.blueprint.michelle.MichelleBlueprintSpectroscopy(choice.getSpectroscopy());
        else
            throw new IllegalArgumentException(NO_FACTORY_FOR + choice.toString());
    }

    private static BlueprintBase convertGnirsBlueprint(GnirsBlueprintChoice choice) {
        if (choice.getImaging() != null) {
            return new edu.gemini.tac.persistence.phase1.blueprint.gnirs.GnirsBlueprintImaging(choice.getImaging());
        } else if (choice.getSpectroscopy() != null) {
            return new edu.gemini.tac.persistence.phase1.blueprint.gnirs.GnirsBlueprintSpectroscopy(choice.getSpectroscopy());
        } else {
            throw new IllegalArgumentException(NO_FACTORY_FOR + choice.toString());
        }
    }

    private static BlueprintBase convertGmosNBlueprint(GmosNBlueprintChoice choice) {
        if (choice.getImaging() != null) {
            return new edu.gemini.tac.persistence.phase1.blueprint.gmosn.GmosNBlueprintImaging(choice.getImaging(), choice.getRegime());
        } else if (choice.getLongslit() != null) {
            return new edu.gemini.tac.persistence.phase1.blueprint.gmosn.GmosNBlueprintLongSlit(choice.getLongslit(), choice.getRegime());
        } else if (choice.getLongslitNs() != null) {
            return new edu.gemini.tac.persistence.phase1.blueprint.gmosn.GmosNBlueprintLongSlitNs(choice.getLongslitNs(), choice.getRegime());
        } else if (choice.getMos() != null) {
            return new edu.gemini.tac.persistence.phase1.blueprint.gmosn.GmosNBlueprintMos(choice.getMos(), choice.getRegime());
        } else if (choice.getIfu() != null) {
            return new edu.gemini.tac.persistence.phase1.blueprint.gmosn.GmosNBlueprintIfu(choice.getIfu(), choice.getRegime());
        } else {
            throw new IllegalArgumentException(NO_FACTORY_FOR + choice.toString());
        }
    }

    private static BlueprintBase convertNiriBlueprint(NiriBlueprintChoice choice) {
        if (choice.getNiri() != null) {
            return new edu.gemini.tac.persistence.phase1.blueprint.niri.NiriBlueprint(choice.getNiri());
        } else {
            throw new IllegalArgumentException(NO_FACTORY_FOR + choice.toString());
        }
    }

    private static BlueprintBase convertGmosSBlueprint(GmosSBlueprintChoice choice) {
        if (choice.getImaging() != null) {
            return new edu.gemini.tac.persistence.phase1.blueprint.gmoss.GmosSBlueprintImaging(choice.getImaging(), choice.getRegime());
        } else if (choice.getLongslit() != null) {
            return new GmosSBlueprintLongSlit(choice.getLongslit(), choice.getRegime());
        } else if (choice.getLongslitNs() != null) {
            return new edu.gemini.tac.persistence.phase1.blueprint.gmoss.GmosSBlueprintLongSlitNs(choice.getLongslitNs(), choice.getRegime());
        } else if (choice.getMos() != null) {
            return new edu.gemini.tac.persistence.phase1.blueprint.gmoss.GmosSBlueprintMos(choice.getMos(), choice.getRegime());
        } else if (choice.getIfu() != null) {
            return new edu.gemini.tac.persistence.phase1.blueprint.gmoss.GmosSBlueprintIfu(choice.getIfu(), choice.getRegime());
        } else if (choice.getIfuNs() != null) {
            return new edu.gemini.tac.persistence.phase1.blueprint.gmoss.GmosSBlueprintIfuNs(choice.getIfuNs(), choice.getRegime());
        } else {
            throw new IllegalArgumentException(NO_FACTORY_FOR + choice.toString());
        }
    }

    private static BlueprintBase convertDssiBlueprint(DssiBlueprintChoice choice) {
        if (choice.getDssi() != null) {
            return new DssiBlueprint(choice.getDssi());
        } else {
            throw new IllegalArgumentException(NO_FACTORY_FOR + choice.toString());
        }
    }

    private static BlueprintBase convertAlopekeBlueprint(AlopekeBlueprintChoice choice) {
        if (choice.getAlopeke() != null) {
            return new AlopekeBlueprint(choice.getAlopeke());
        } else {
            throw new IllegalArgumentException(NO_FACTORY_FOR + choice.toString());
        }
    }

    private static BlueprintBase convertZorroBlueprint(ZorroBlueprintChoice choice) {
        if (choice.getZorro() != null) {
            return new ZorroBlueprint(choice.getZorro());
        } else {
            throw new IllegalArgumentException(NO_FACTORY_FOR + choice.toString());
        }
     }

    private static BlueprintBase convertVisitorBlueprint(VisitorBlueprintChoice choice) {
        if (choice.getVisitor() != null) {
            if (choice.getVisitor().getSite() == edu.gemini.model.p1.mutable.Site.GEMINI_NORTH) {
                return new VisitorGNBlueprint(choice.getVisitor());
            } else {
                return new VisitorGSBlueprint(choice.getVisitor());
            }
        } else {
            throw new IllegalArgumentException(NO_FACTORY_FOR + choice.toString());
        }
    }

    private static BlueprintBase convertTexesBlueprint(TexesBlueprintChoice choice) {
        if (choice.getTexes() != null) {
            return new edu.gemini.tac.persistence.phase1.blueprint.texes.TexesBlueprint(choice.getTexes());
        } else {
            throw new IllegalArgumentException(NO_FACTORY_FOR + choice.toString());
        }
    }

    @Override
    public String toString() {
        return "BlueprintBase{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", blueprintId='" + blueprintId + '\'' +
                ", instrument=" + instrument +
                '}';
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getBlueprintId() {
        return blueprintId;
    }

    public void setBlueprintId(String blueprintId) {
        this.blueprintId = blueprintId;
    }

    public PhaseIProposal getPhaseIProposal() {
        return phaseIProposal;
    }

    public void setPhaseIProposal(PhaseIProposal phaseIProposal) {
        this.phaseIProposal = phaseIProposal;
    }

    public Long getId() {

        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Set<Observation> getObservations() {
        return observations;
    }

    public void setObservations(Set<Observation> observations) {
        this.observations = observations;
    }

    public Instrument getInstrument() {
        return instrument;
    }

    public void setInstrument(Instrument instrument) {
        this.instrument = instrument;
    }

    abstract public BlueprintBase getComplementaryInstrumentBlueprint();

    abstract public Map<String, Set<String>> getResourcesByCategory();

    /** 
     * Returns true if the blueprint uses a laser guide star.
     * This information is mainly used for phase 2 checks and reporting purposes.
     * @return defaults to false, but is overridden by blueprints that can have an LGS component.
     */
    public boolean hasLgs(){
        return false;
    }

    /**
     * Returns true if the blueprint uses adaptive optics.
     * This information is mainly used for phase 2 checks and reporting purposes.
     * @return defaults to false, but is overridden by blueprints that can have an altair component.
     */
    // NOTE: Do not override this method, override hasAltair for Gemini north instruments and
    // hasMcao for Gemini South instruments instead. Made final to avoid confusion.
    public final boolean hasAO(){
        return hasAltair() || hasMcao();
    }


    /**
     * Returns true if the blueprint uses altair adaptive optics (Gemini North).
     * This information is mainly used for phase 2 checks and reporting purposes.
     * @return defaults to false, but is overridden by blueprints that can have an altair component.
     */
    public boolean hasAltair(){
        return false;
    }

    /**
     * Returns true if the blueprint uses mcao adaptive optics (Gemini South).
     * This information is mainly used for phase 2 checks and reporting purposes.
     * @return defaults to false, but is overridden by blueprints that can have an mcao component.
     */
    public boolean hasMcao(){
        return false;
    }

    /**
     * Returns true if the blueprint is a MOS blueprint
     */
    public abstract boolean isMOS();

    // A number of display oriented abstract methods that support getting information into the reports as well as
    // UI elements of the ITAC application itself.
    abstract public String getDisplayCamera();
    abstract public String getDisplayFocalPlaneUnit();
    abstract public String getDisplayDisperser();
    abstract public String getDisplayFilter();
    abstract public String getDisplayAdaptiveOptics();
    abstract public String getDisplayOther();

    public String getDisplay() {
        final StringBuilder buffer = new StringBuilder(getInstrument().getDisplayName());
        buffer.append(" ");

        buffer.append((!getDisplayCamera().equals(DISPLAY_NOT_APPLICABLE) ? getDisplayCamera() + " " : ""));
        buffer.append((!getDisplayFocalPlaneUnit().equals(DISPLAY_NOT_APPLICABLE) ? getDisplayFocalPlaneUnit() + " " : ""));
        buffer.append((!getDisplayDisperser().equals(DISPLAY_NOT_APPLICABLE) ? getDisplayDisperser() + " " : ""));
        buffer.append((!getDisplayFilter().equals(DISPLAY_NOT_APPLICABLE) ? getDisplayFilter() + " " : ""));
        buffer.append((!getDisplayAdaptiveOptics().equals(DISPLAY_NOT_APPLICABLE) ? getDisplayAdaptiveOptics() + " " : ""));
        buffer.append((!getDisplayOther().equals(DISPLAY_NOT_APPLICABLE) ? getDisplayOther() + " " : ""));

        return buffer.toString();
    }
}
