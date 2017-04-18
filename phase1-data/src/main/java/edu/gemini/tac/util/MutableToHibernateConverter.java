package edu.gemini.tac.util;

import edu.gemini.model.p1.mutable.*;
import edu.gemini.model.p1.mutable.CoInvestigator;
import edu.gemini.model.p1.mutable.Condition;
import edu.gemini.model.p1.mutable.DegDegCoordinates;
import edu.gemini.model.p1.mutable.EphemerisElement;
import edu.gemini.model.p1.mutable.GuideStar;
import edu.gemini.model.p1.mutable.HmsDmsCoordinates;
import edu.gemini.model.p1.mutable.Investigators;
import edu.gemini.model.p1.mutable.Magnitude;
import edu.gemini.model.p1.mutable.Meta;
import edu.gemini.model.p1.mutable.Observation;
import edu.gemini.model.p1.mutable.ObservationMetaData;
import edu.gemini.model.p1.mutable.PrincipalInvestigator;
import edu.gemini.model.p1.mutable.ProperMotion;
import edu.gemini.model.p1.mutable.SiderealTarget;
import edu.gemini.model.p1.mutable.Target;
import edu.gemini.model.p1.mutable.TooTarget;
import edu.gemini.tac.persistence.Partner;
import edu.gemini.tac.persistence.phase1.TimeAmount;
import edu.gemini.tac.persistence.phase1.proposal.PhaseIProposal;
import edu.gemini.tac.persistence.phase1.sitequality.CloudCover;
import edu.gemini.tac.persistence.phase1.sitequality.ImageQuality;
import edu.gemini.tac.persistence.phase1.sitequality.SkyBackground;
import edu.gemini.tac.persistence.phase1.sitequality.WaterVapor;
import org.apache.commons.lang.Validate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MutableToHibernateConverter {
    final public Map<String, edu.gemini.tac.persistence.phase1.Condition> conditionMap = new HashMap<String, edu.gemini.tac.persistence.phase1.Condition>();
    final public Map<String, edu.gemini.tac.persistence.phase1.Target> targetMap = new HashMap<String, edu.gemini.tac.persistence.phase1.Target>();
    final public Map<String, edu.gemini.tac.persistence.phase1.Investigator> investigatorMap = new HashMap<String, edu.gemini.tac.persistence.phase1.Investigator>();
    final public Map<String, edu.gemini.tac.persistence.phase1.blueprint.BlueprintBase> blueprintMap = new HashMap<String, edu.gemini.tac.persistence.phase1.blueprint.BlueprintBase>();

    final public Map<String, Partner> partnerMap = new HashMap<String, Partner>();

    public MutableToHibernateConverter(final List<Partner> partners) {
        for (Partner p: partners) {
            partnerMap.put(p.getPartnerCountryKey(), p);
        }
    }

    public PhaseIProposal toHibernate(final Proposal mutableProposal, final PhaseIProposal hibernateProposal) {
        final Proposal mp = mutableProposal;
        final PhaseIProposal hp = hibernateProposal;

        convertInvestigators(mp, hp);
        convertTargets(mp, hp);
        convertConditions(mp, hp);
        convertBlueprints(mp, hp);
        convertObservations(mp, hp);

        convertProposalAttributes(mp, hp);
        convertMeta(mp, hp);
        convertKeywords(mp, hp);

        return hp;
    }

    public void convertBlueprints(Proposal mp, PhaseIProposal hp) {
        if (mp.getBlueprints() != null) {
            final List<Object> blueprints = mp.getBlueprints().getFlamingos2OrGmosNOrGmosS();
            for (Object o : blueprints) {

                final edu.gemini.tac.persistence.phase1.blueprint.BlueprintBase blueprint =
                        edu.gemini.tac.persistence.phase1.blueprint.BlueprintBase.fromMutable(o);
                blueprint.setPhaseIProposal(hp);
                hp.getBlueprints().add(blueprint);
                blueprintMap.put(blueprint.getBlueprintId(), blueprint);
            }
        }
    }

    public void convertMeta(Proposal mp, PhaseIProposal hp) {
        final Meta mMeta = mp.getMeta();
        if (mMeta != null) {
            final edu.gemini.tac.persistence.phase1.Meta hMeta = new edu.gemini.tac.persistence.phase1.Meta();
            hp.setMeta(hMeta);
            hMeta.setAttachment(mMeta.getAttachment());
            hMeta.setBand3optionChosen(mMeta.isBand3OptionChosen());
        }
    }

    public void convertObservations(final Proposal mp,
                                    final PhaseIProposal hp) {
        final List<Observation> observations = mp.getObservations().getObservation();

        for (Observation mo : observations) {
            final edu.gemini.tac.persistence.phase1.Observation observation = new edu.gemini.tac.persistence.phase1.Observation();

            final String mutableConditionId = mo.getCondition().getId();
            edu.gemini.tac.persistence.phase1.Condition c = conditionMap.get(mo.getCondition().getId());
            Validate.notNull(c);
            observation.setCondition(c);
            observation.setBand(mo.getBand());

            final edu.gemini.tac.persistence.phase1.blueprint.BlueprintBase blueprintBase = blueprintMap.get(mo.getBlueprint().getId());
            Validate.notNull(blueprintBase);
            observation.setBlueprint(blueprintBase);

            final GuideGroup guide = mo.getGuide();
            if (guide != null) {
                for (GuideStar mgs : guide.getGuideStar()) {
                    final edu.gemini.tac.persistence.phase1.GuideStar guideStar = new edu.gemini.tac.persistence.phase1.GuideStar();
                    final Target target = (Target) mgs.getTarget();

                    guideStar.setGuider(mgs.getGuider());
                    guideStar.setTarget(targetMap.get(target.getId()));
                    guideStar.setObservation(observation);
                    observation.getGuideStars().add(guideStar);
                }
            }

            observation.setActive(mo.isEnabled());
            final ObservationMetaData mMeta = mo.getMeta();
            if (mMeta != null) {
                edu.gemini.tac.persistence.phase1.ObservationMetaData.GuidingEstimation guidingEstimation = null;
                if (mMeta.getGuiding() != null) {
                    guidingEstimation = new edu.gemini.tac.persistence.phase1.ObservationMetaData.GuidingEstimation(mMeta.getGuiding().getPercentage(), mMeta.getGuiding().getEvaluation());
                }
                final edu.gemini.tac.persistence.phase1.ObservationMetaData observationMetaData = new edu.gemini.tac.persistence.phase1.ObservationMetaData(guidingEstimation, mMeta.getVisibility(), mMeta.getGsa(), mMeta.getCk());
                observation.setMetaData(observationMetaData);
            }

            observation.setTarget(targetMap.get(mo.getTarget().getId()));

            final TimeAmount progTimeAmount = new TimeAmount(mo.getProgTime());
            observation.setProgTime(progTimeAmount);

            final TimeAmount partTimeAmount = new TimeAmount(mo.getPartTime());
            observation.setPartTime(partTimeAmount);

            final TimeAmount timeAmount = new TimeAmount(mo.getTime());
            observation.setTime(timeAmount);

            observation.setProposal(hp);
            hp.getAllObservations().add(observation);

        }
    }

    public void convertConditions(final Proposal mp, final PhaseIProposal hp) {
        final Conditions conditionContainer = mp.getConditions();
        final List<Condition> conditions = conditionContainer.getCondition();
        for (Condition c: conditions) {
            final edu.gemini.tac.persistence.phase1.Condition condition = new edu.gemini.tac.persistence.phase1.Condition();

            condition.setConditionId(c.getId());
            condition.setName(c.getName());
            condition.setCloudCover(CloudCover.fromValue(c.getCc().value()));
            condition.setImageQuality(ImageQuality.fromValue(c.getIq().value()));
            condition.setSkyBackground(SkyBackground.fromValue(c.getSb().value()));
            condition.setWaterVapor(WaterVapor.fromValue(c.getWv().value()));
            condition.setMaxAirmass(c.getMaxAirmass());

            condition.setPhaseIProposal(hp);
            hp.getConditions().add(condition);
            conditionMap.put(condition.getConditionId(), condition);
        }
    }

    public void convertTargets(final Proposal mp, final PhaseIProposal hp) {
        final Targets container = mp.getTargets();
        final List<Target> targets = container.getSiderealOrNonsiderealOrToo();

        for (Target t: targets) {
            edu.gemini.tac.persistence.phase1.Target ht;

            if (t instanceof SiderealTarget) {
                final SiderealTarget siderealTarget = ((SiderealTarget) t);
                final edu.gemini.tac.persistence.phase1.SiderealTarget hst = new edu.gemini.tac.persistence.phase1.SiderealTarget();
                ht = hst;
                hst.setEpoch(siderealTarget.getEpoch());
                final ProperMotion mProperMotion = siderealTarget.getProperMotion();
                if (mProperMotion != null)
                    hst.setProperMotion(new edu.gemini.tac.persistence.phase1.ProperMotion(mProperMotion));

                final DegDegCoordinates degDeg = siderealTarget.getDegDeg();
                final HmsDmsCoordinates hmsDms = siderealTarget.getHmsDms();
                edu.gemini.tac.persistence.phase1.Coordinates coordinates = null;
                if (degDeg != null) {
                    coordinates = new edu.gemini.tac.persistence.phase1.DegDegCoordinates(degDeg);
                } else if (hmsDms != null) {
                    coordinates = new edu.gemini.tac.persistence.phase1.HmsDmsCoordinates(hmsDms);
                } else {
                    throw new IllegalArgumentException("Unknown class of Coordinates detected on conversion.");
                }
                hst.setCoordinates(coordinates);

                final Magnitudes magnitudesContainer = siderealTarget.getMagnitudes();
                if (magnitudesContainer != null) {
                    final List<Magnitude> magnitudes = magnitudesContainer.getMagnitude();
                    for (Magnitude m : magnitudes) {
                        final edu.gemini.tac.persistence.phase1.Magnitude hm = new edu.gemini.tac.persistence.phase1.Magnitude();
                        hm.setBand(m.getBand());
                        hm.setSystem(m.getSystem());
                        hm.setValue(m.getValue());
                        hm.setTarget(ht);

                        hst.getMagnitudes().add(hm);
                    }
                }
            } else if (t instanceof NonSiderealTarget) {
                final NonSiderealTarget nonSiderealTarget = (NonSiderealTarget) t;
                final edu.gemini.tac.persistence.phase1.NonsiderealTarget hnst = new edu.gemini.tac.persistence.phase1.NonsiderealTarget();
                ht = hnst;
                hnst.setEpoch(nonSiderealTarget.getEpoch());

                for (EphemerisElement ee : nonSiderealTarget.getEphemeris()) {
                    final edu.gemini.tac.persistence.phase1.EphemerisElement hee = new edu.gemini.tac.persistence.phase1.EphemerisElement();
                    final DegDegCoordinates degDeg = ee.getDegDeg();
                    final HmsDmsCoordinates hmsDms = ee.getHmsDms();
                    edu.gemini.tac.persistence.phase1.Coordinates coordinates = null;
                    if (degDeg != null) {
                        coordinates = new edu.gemini.tac.persistence.phase1.DegDegCoordinates(degDeg);
                    } else if (hmsDms != null) {
                        coordinates = new edu.gemini.tac.persistence.phase1.HmsDmsCoordinates(hmsDms);
                    } else {
                        throw new IllegalArgumentException("Unknown class of Coordinates detected on conversion.");
                    }
                    hee.setCoordinates(coordinates);
                    hee.setMagnitude(ee.getMagnitude());
                    hee.setTarget(hnst);
                    hee.setValidAt(ee.getValidAt().toGregorianCalendar().getTime());
                    hnst.getEphemeris().add(hee);
                }
            } else if (t instanceof TooTarget) {
                ht = new edu.gemini.tac.persistence.phase1.TooTarget();
                // currently no other attributes for TooTargets
            } else {
                throw new IllegalArgumentException("Unknown class of targets detected on conversion.  " + t.getClass());
            }

            ht.setTargetId(t.getId());
            ht.setName(t.getName());
            ht.setPhaseIProposal(hp);

            targetMap.put(ht.getTargetId(), ht);

            hp.getTargets().add(ht);
        }
    }

    /**
     * Scheduling and investigators combined as scheduling import is potentially dependent on investigators.
     *
     * @param mp
     * @param hp
     */
    public void convertInvestigators(final Proposal mp, final PhaseIProposal hp) {
        final Investigators investigators = mp.getInvestigators();

        final PrincipalInvestigator pi = investigators.getPi();
        hp.getInvestigators().setPi(new edu.gemini.tac.persistence.phase1.PrincipalInvestigator(investigators.getPi()));
        investigatorMap.put(pi.getId(), hp.getInvestigators().getPi());

        final List<CoInvestigator> coi = investigators.getCoi();
        for (CoInvestigator c :coi) {
            final edu.gemini.tac.persistence.phase1.CoInvestigator coInvestigator = new edu.gemini.tac.persistence.phase1.CoInvestigator(c);
            hp.getInvestigators().getCoi().add(coInvestigator);
            investigatorMap.put(c.getId(), coInvestigator);
        }
    }

    public void convertKeywords(final Proposal mp, final PhaseIProposal hp) {
        for (Keyword k : mp.getKeywords().getKeyword()) {
            hp.getKeywords().add(k);
        }
    }


    public void convertProposalAttributes(Proposal mp, PhaseIProposal hp) {
        hp.setProposalAbstract(mp.getAbstract());
        hp.setSchemaVersion(mp.getSchemaVersion());
        hp.setTitle(mp.getTitle());
        hp.setTacCategory(mp.getTacCategory());
        hp.setScheduling(mp.getScheduling());
    }
}
