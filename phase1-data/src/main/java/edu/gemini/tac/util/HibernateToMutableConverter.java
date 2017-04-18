package edu.gemini.tac.util;

import edu.gemini.model.p1.mutable.*;
import edu.gemini.model.p1.mutable.CoInvestigator;
import edu.gemini.model.p1.mutable.InstitutionAddress;
import edu.gemini.model.p1.mutable.Investigators;
import edu.gemini.model.p1.mutable.ItacAccept;
import edu.gemini.model.p1.mutable.ItacReject;
import edu.gemini.model.p1.mutable.Meta;
import edu.gemini.model.p1.mutable.Observation;
import edu.gemini.model.p1.mutable.PrincipalInvestigator;
import edu.gemini.model.p1.mutable.Proposal;
import edu.gemini.model.p1.mutable.Semester;
import edu.gemini.model.p1.mutable.SubmissionAccept;
import edu.gemini.model.p1.mutable.SubmissionReceipt;
import edu.gemini.model.p1.mutable.SubmissionRequest;
import edu.gemini.model.p1.mutable.Target;
import edu.gemini.tac.persistence.*;
import edu.gemini.tac.persistence.phase1.Condition;
import edu.gemini.tac.persistence.phase1.GuideStar;
import edu.gemini.tac.persistence.phase1.Investigator;
import edu.gemini.tac.persistence.phase1.Itac;
import edu.gemini.tac.persistence.phase1.proposal.*;
import edu.gemini.tac.persistence.phase1.submission.*;
import edu.gemini.tac.persistence.phase1.blueprint.BlueprintPair;
import edu.gemini.tac.persistence.phase1.submission.NgoSubmission;
import edu.gemini.tac.persistence.util.Conversion;
import org.apache.log4j.Logger;

import java.util.*;

/**
 * Rather than an ugly but clean set of static conversions, the increasing dependence on inheritance in the mutable
 * structure pointed towards implementing the conversion as instance methods in the no-longer VO persistence.model
 *
 * Currently mixed, that's not by design, but rather due to Sprint 10 schedule pressures.  Don't hesitate to finish
 * the job.
 */
public class HibernateToMutableConverter {
    protected static final Logger LOGGER = Logger.getLogger(HibernateToMutableConverter.class.getName());

    protected HibernateToMutableConverter() {}

    public static Proposal toMutable(final PhaseIProposal hibernateProposal) {
        final HashMap<Investigator, String> hibernateInvestigatorToIdMap = new HashMap<Investigator, String>();
        createInvestigatorMaps(hibernateProposal, hibernateInvestigatorToIdMap);

        final Proposal mp = hibernateProposal.toMutable(hibernateInvestigatorToIdMap);
        final PhaseIProposal hp = hibernateProposal;

        final Map<String, edu.gemini.model.p1.mutable.Target> targetMap = new HashMap<String, Target>();
        final Map<String, edu.gemini.model.p1.mutable.Condition> conditionMap = new HashMap<String, edu.gemini.model.p1.mutable.Condition>();
        final Map<edu.gemini.tac.persistence.phase1.blueprint.BlueprintBase, BlueprintBase> blueprintMap = new HashMap<edu.gemini.tac.persistence.phase1.blueprint.BlueprintBase, BlueprintBase>();

        convertSemester(hp, mp);
        convertMeta(hp, mp);
        convertKeywords(hp, mp);
        convertTargets(hp, mp, targetMap);
        convertConditions(hp, mp, conditionMap);
        convertBlueprints(hp, mp, blueprintMap);
        convertObservations(hp, mp, targetMap, conditionMap, blueprintMap);
        convertProposalClass(hp, mp, hibernateInvestigatorToIdMap);

        return mp;
    }

    /**
     * No-op.
     *
     * @param hp
     * @param mp
     * @param hibernateInvestigatorToIdMap
     */
    private static void convertProposalClass(final PhaseIProposal hp, final Proposal mp, HashMap<Investigator, String> hibernateInvestigatorToIdMap) {
        // Should be getting taken care of in the toMutable
    }

    private static void convertSemester(PhaseIProposal hp, Proposal mp) {
        final Semester semester = new Semester();
        final edu.gemini.tac.persistence.Semester hSemester = hp.getParent().getCommittee().getSemester();

        final String semesterName = hSemester.getName();
        if ("A".equals(semesterName)) {
            semester.setHalf(SemesterOption.A);
        } else if ("B".equals(semesterName)) {
            semester.setHalf(SemesterOption.B);
        } else {
            throw new IllegalStateException("Semester 'name' [" + semesterName + "] is not understood.  Must be A or B.");
        }
        semester.setYear(hSemester.getYear());

        mp.setSemester(semester);
    }

    private static void convertBlueprints(PhaseIProposal hp, Proposal mp, Map<edu.gemini.tac.persistence.phase1.blueprint.BlueprintBase, BlueprintBase> blueprintMap) {
        final List<edu.gemini.tac.persistence.phase1.blueprint.BlueprintBase> hBlueprints = hp.getBlueprints();
        if (hBlueprints != null) {
            Blueprints mBlueprints = new Blueprints();
            mp.setBlueprints(mBlueprints);
            for (edu.gemini.tac.persistence.phase1.blueprint.BlueprintBase hb : hBlueprints) {
                final BlueprintPair blueprintPair = hb.toMutable();
                mBlueprints.getFlamingos2OrGmosNOrGmosS().add(blueprintPair.getBlueprintChoice());

                blueprintMap.put(hb, blueprintPair.getBlueprintBase());
            }
        }
    }

    private static void convertMeta(PhaseIProposal hp, Proposal mp) {
        if (hp.getMeta() != null) {
            final Meta meta = new Meta();
            mp.setMeta(meta);
            meta.setAttachment(hp.getMeta().getAttachment());
            mp.getMeta().setBand3OptionChosen(hp.getMeta().getBand3optionChosen());
        }
    }

    private static void convertObservations(PhaseIProposal hp, Proposal mp, Map<String, Target> targetMap, Map<String, edu.gemini.model.p1.mutable.Condition> conditionMap, Map<edu.gemini.tac.persistence.phase1.blueprint.BlueprintBase, BlueprintBase> blueprintMap) {
        final Observations observationsContainer = new Observations();
        mp.setObservations(observationsContainer);

        final Set<edu.gemini.tac.persistence.phase1.Observation> hObservations = hp.getAllObservations();
        final List<edu.gemini.tac.persistence.phase1.Observation> hSortableObservations = new ArrayList<edu.gemini.tac.persistence.phase1.Observation>();
        hSortableObservations.addAll(hObservations);
        Collections.sort(hSortableObservations, new Comparator<edu.gemini.tac.persistence.phase1.Observation>() {
            public int compare(edu.gemini.tac.persistence.phase1.Observation o1, edu.gemini.tac.persistence.phase1.Observation o2) {
                return o1.getTarget().getTargetId().compareTo(o2.getTarget().getTargetId());
            }
        });
        for (edu.gemini.tac.persistence.phase1.Observation ho : hSortableObservations) {
            if (ho.getActive()) {
                final Observation mo = new Observation();
                observationsContainer.getObservation().add(mo);

                mo.setCondition(conditionMap.get(ho.getCondition().getConditionId()));
                mo.setTarget(targetMap.get(ho.getTarget().getTargetId()));
                mo.setBlueprint(blueprintMap.get(ho.getBlueprint()));


    // TODO: -- It seems that the GuideStar objects are never really used (database is empty, there is no way to
    // TODO: define GuideStars as part of Phase 1 (PIT etc). I assume the GuideStar object can be removed without
    // TODO: doing any damage to ITAC. It was probably only added in order to have an object that corresponds to the
    // TODO: Phase 2 model, but since it can not be created/edited during Phase 1 it does probably not belong in
    // TODO: the ITAC model. (I had to remove this to fix a LazyInitializationException that crept up on us
    // TODO: during skeleton export.) FN 2012-12-06

    //            final Set<GuideStar> guideStars = ho.getGuideStars();
    //            if (guideStars != null && guideStars.size() > 0) {
    //                final GuideGroup guideGroup = new GuideGroup();
    //                mo.setGuide(guideGroup);
    //                for (GuideStar hgs : guideStars) {
    //                    final edu.gemini.model.p1.mutable.GuideStar mgs = new edu.gemini.model.p1.mutable.GuideStar();
    //                    mgs.setGuider(hgs.getGuider());
    //                    mgs.setTarget(targetMap.get(hgs.getTarget().getTargetId()));
    //                    guideGroup.getGuideStar().add(mgs);
    //                }
    //            }

                if (ho.getMetaData() != null) {
                    mo.setMeta(ho.getMetaData().toMutable());
                }

                mo.setProgTime(ho.getProgTime().toMutable());
                mo.setPartTime(ho.getPartTime().toMutable());
                mo.setTime(ho.getTime().toMutable());
                mo.setBand(ho.getBand());

                mo.setEnabled(ho.getActive());
            }
        }
    }

    private static void convertConditions(PhaseIProposal hp, Proposal mp, Map<String, edu.gemini.model.p1.mutable.Condition> conditionMap) {
        final Conditions conditionContainer = new Conditions();
        mp.setConditions(conditionContainer);
        for (Condition c: hp.getConditions()) {
            final edu.gemini.model.p1.mutable.Condition mCondition = new edu.gemini.model.p1.mutable.Condition();
            mCondition.setId(c.getConditionId());
            mCondition.setName(c.getName());
            mCondition.setMaxAirmass(c.getMaxAirmass());
            mCondition.setCc(CloudCover.fromValue(c.getCloudCover().value()));
            mCondition.setIq(ImageQuality.fromValue(c.getImageQuality().value()));
            mCondition.setSb(SkyBackground.fromValue(c.getSkyBackground().value()));
            mCondition.setWv(WaterVapor.fromValue(c.getWaterVapor().value()));

            conditionContainer.getCondition().add(mCondition);
            conditionMap.put(mCondition.getId(), mCondition);
        }
    }

    private static void convertTargets(final PhaseIProposal hp, final Proposal mp, final Map<String, Target> targetMap) {
        final Targets targetContainer = new Targets();
        mp.setTargets(targetContainer);
        for (edu.gemini.tac.persistence.phase1.Target t : hp.getTargets()) {
            final Target target = t.toMutable();
            targetContainer.getSiderealOrNonsiderealOrToo().add(target);
            targetMap.put(t.getTargetId(), target);
        }
    }

    private static void createInvestigatorMaps(final PhaseIProposal hp, HashMap<Investigator, String> hibernateInvestigatorToIdMap) {
        int investigatorIndex = 1;

        String investigatorId = "investigator-" + investigatorIndex++;
        final edu.gemini.tac.persistence.phase1.Investigators investigators = hp.getInvestigators();
        hibernateInvestigatorToIdMap.put(investigators.getPi(), investigatorId);

        final Set<edu.gemini.tac.persistence.phase1.CoInvestigator> cois = investigators.getCoi();
        for (edu.gemini.tac.persistence.phase1.CoInvestigator c : cois) {
            investigatorId = "investigator-" + investigatorIndex++;
            hibernateInvestigatorToIdMap.put(c,  investigatorId);
        }
    }

    private static void convertKeywords(PhaseIProposal hp, Proposal mp) {
        final Keywords keywords = new Keywords();
        keywords.getKeyword();
        for (Keyword k : hp.getKeywords()) {
            keywords.getKeyword().add(k);
        }
        mp.setKeywords(keywords);
    }
}
