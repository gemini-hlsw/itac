package edu.gemini.tac.service;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import edu.gemini.tac.persistence.Partner;
import edu.gemini.tac.persistence.Proposal;
import edu.gemini.tac.persistence.bin.BinConfiguration;
import edu.gemini.tac.persistence.condition.ConditionSet;
import edu.gemini.tac.persistence.phase1.TimeUnit;
import edu.gemini.tac.persistence.queues.partnerCharges.PartnerCharge;
import edu.gemini.tac.persistence.restrictedbin.RestrictedBin;

import java.text.DecimalFormat;
import java.util.*;

/**
 * Container for queue creation parameters that are fed into the jsp page rendering process in order to use settings
 * of the latest queue as presets for new queue creations.
 */
public class QueueCreationParameters {
    private DecimalFormat formatTwoFractionDigits;
    private Map<Partner, Map<Class, PartnerCharge>> partnerChargesMap;

    private Integer totalTimeAvailable;
    private Partner partnerWithInitialPick;
    private ConditionSet conditionSet;
    private BinConfiguration binConfiguration;

    private Integer band1Cutoff = 30;
    private Integer band2Cutoff = 60;
    private Integer band3Cutoff = 80;
    private Integer band3ConditionsThreshold = 80;
    private Boolean useBand3AfterThresholdCrossed = Boolean.TRUE;

    private Integer overfillLimit = 5;
    private Boolean scheduleSubaruAsPartner = Boolean.FALSE;

    private Long rolloverSetId = 0L;

    private List<Long> subaruProposalsSelected = Lists.newArrayList();
    private Set<RestrictedBin> restrictedBins = Sets.newHashSet();
    private Set<RestrictedBin> defaultRestrictedBins = Sets.newHashSet();

    public QueueCreationParameters() {
        formatTwoFractionDigits =  new DecimalFormat("#0.00");
        partnerChargesMap = new HashMap<Partner, Map<Class, PartnerCharge>>();
    }

    public Long getRolloverSetId() {
        return rolloverSetId;
    }

    public void setRolloverSetId(Long rolloverSetId) {
        this.rolloverSetId = rolloverSetId;
    }

    public List<Long> getSubaruProposalsSelected() {
        return subaruProposalsSelected;
    }

    public Boolean getScheduleSubaruAsPartner() {
        return scheduleSubaruAsPartner;
    }

    public void setScheduleSubaruAsPartner(Boolean scheduleSubaruAsPartner) {
        this.scheduleSubaruAsPartner = scheduleSubaruAsPartner;
    }

    public Map<Partner, Map<Class, PartnerCharge>> getPartnerChargesMap() {
        return partnerChargesMap;
    }

    public void setPartnerChargesMap(Map<Partner, Map<Class, PartnerCharge>> partnerChargesMap) {
        this.partnerChargesMap = partnerChargesMap;
    }

    public void setPartnerCharges(List<PartnerCharge> partnerCharges) {
        for (PartnerCharge partnerCharge : partnerCharges) {
            Map innerMap = partnerChargesMap.get(partnerCharge.getPartner());
            if (innerMap == null) {
                innerMap = new HashMap<Class, PartnerCharge>();
                partnerChargesMap.put(partnerCharge.getPartner(), innerMap);
            }
            innerMap.put(partnerCharge.getClass(), partnerCharge);
        }
    }

    public String getPartnerCharge(Partner partner, Class partnerChargeClass) {
        Map<Class, PartnerCharge> pcMap = partnerChargesMap.get(partner);
        if (pcMap == null) {
            return "0.00";
        }

        PartnerCharge partnerCharge = pcMap.get(partnerChargeClass);
        if (partnerCharge == null) {
            return "0.00";
        }

        double hours = partnerCharge.getCharge().convertTo(TimeUnit.HR).getValue().doubleValue();
        return formatTwoFractionDigits.format(hours); // formatting should be done in gui, but it's just so much easier to do it on the server side...
    }

    public Integer getTotalTimeAvailable() {
        return totalTimeAvailable;
    }

    public void setTotalTimeAvailable(Integer totalTimeAvailable) {
        this.totalTimeAvailable = totalTimeAvailable;
    }

    public Partner getPartnerWithInitialPick() {
        return partnerWithInitialPick;
    }

    public void setPartnerWithInitialPick(Partner partnerWithInitialPick) {
        this.partnerWithInitialPick = partnerWithInitialPick;
    }

    public ConditionSet getConditionSet() {
        return conditionSet;
    }

    public void setConditionSet(ConditionSet conditionSet) {
        this.conditionSet = conditionSet;
    }

    public BinConfiguration getBinConfiguration() {
        return binConfiguration;
    }

    public void setBinConfiguration(BinConfiguration binConfiguration) {
        this.binConfiguration = binConfiguration;
    }

    public Integer getBand1Cutoff() {
        return band1Cutoff;
    }

    public void setBand1Cutoff(Integer band1Cutoff) {
        this.band1Cutoff = band1Cutoff;
    }

    public Integer getBand2Cutoff() {
        return band2Cutoff;
    }

    public void setBand2Cutoff(Integer band2Cutoff) {
        this.band2Cutoff = band2Cutoff;
    }

    public Integer getBand3Cutoff() {
        return band3Cutoff;
    }

    public void setBand3Cutoff(Integer band3Cutoff) {
        this.band3Cutoff = band3Cutoff;
    }

    public Integer getBand3ConditionsThreshold() {
        return band3ConditionsThreshold;
    }

    public void setBand3ConditionsThreshold(Integer band3ConditionsThreshold) {
        this.band3ConditionsThreshold = band3ConditionsThreshold;
    }

    public Boolean getUseBand3AfterThresholdCrossed() {
        return useBand3AfterThresholdCrossed;
    }

    public void setUseBand3AfterThresholdCrossed(Boolean useBand3AfterThresholdCrossed) {
        this.useBand3AfterThresholdCrossed = useBand3AfterThresholdCrossed;
    }

    public Integer getOverfillLimit() {
        return overfillLimit;
    }

    public void setOverfillLimit(Integer overfillLimit) {
        this.overfillLimit = overfillLimit;
    }

    public void setSubaruProposalsSelected(Set<Proposal> subaruProposalsEligibleForQueue) {
        for(Proposal p:subaruProposalsEligibleForQueue) {
            this.subaruProposalsSelected.add(p.getId());
        }
    }

    public void addRestrictionBins(Collection<RestrictedBin> bins) {
        this.restrictedBins.addAll(bins);
    }

    public Set<RestrictedBin> getRestrictedBins() {
        return ImmutableSet.copyOf(restrictedBins);
    }

    public void addDefaultRestrictionBins(Collection<RestrictedBin> bins) {
        this.defaultRestrictedBins.addAll(bins);
    }

    public Set<RestrictedBin> getDefaultRestrictedBins() {
        return ImmutableSet.copyOf(defaultRestrictedBins);
    }
}
