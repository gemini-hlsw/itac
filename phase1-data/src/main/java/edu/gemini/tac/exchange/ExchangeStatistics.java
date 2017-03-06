package edu.gemini.tac.exchange;

import edu.gemini.model.p1.mutable.ExchangePartner;
import edu.gemini.tac.persistence.Partner;
import edu.gemini.tac.persistence.Proposal;
import edu.gemini.tac.persistence.Site;
import edu.gemini.tac.persistence.phase1.TimeAmount;
import edu.gemini.tac.persistence.phase1.TimeUnit;
import edu.gemini.tac.persistence.phase1.blueprint.BlueprintBase;
import edu.gemini.tac.persistence.phase1.proposal.*;
import edu.gemini.tac.persistence.phase1.submission.ExchangeSubmission;
import edu.gemini.tac.persistence.phase1.submission.NgoSubmission;
import edu.gemini.tac.persistence.phase1.submission.SubmissionAccept;
import org.apache.commons.lang.Validate;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import java.util.*;

/**
 * Very simple value object that contains information related to exchange proposals.  Total hours of time at partner
 * telescopes requested, total hours of time for each site requested by partner telescopes.  Breakdown of each
 * partner's request for exchange partner time.
 *
 * @author ddawson
 */

public class ExchangeStatistics {
    private static final Logger LOGGER = Logger.getLogger(ExchangeStatistics.class);

    private double totalHours = 0;
    private double totalHoursAtNorth = 0;
    private double totalHoursAtSouth = 0;

    private Map<ExchangeSiteTuple, Double> exchangeSiteTupleHoursMap =
            Collections.synchronizedMap(new HashMap<ExchangeSiteTuple, Double>());

    private final Map<Partner, Double> partnerRequestKeck = Collections.synchronizedMap(new HashMap<Partner, Double>());
    private final Map<Partner, Double> partnerRequestSubaru = Collections.synchronizedMap(new HashMap<Partner, Double>());

    protected ExchangeStatistics() {};

    /**
     * Simplified constructor to build statistics from a general list of proposals, filtering out
     * non-exchange proposals instead of requiring a String->List map that pushed knowledge outside.
     * This will certainly show up when upper layers are being rewritten.
     * @param allPartners 
     * @param proposals
     */
    public ExchangeStatistics(final Collection<Partner> allPartners, final Collection<Proposal> proposals) {
        for (Partner n : allPartners) {
            partnerRequestKeck.put(n, 0.0d);
            partnerRequestSubaru.put(n, 0.0d);
        }

        exchangeSiteTupleHoursMap.put(ExchangeSiteTuple.KECK_NORTH, 0.0d);
        exchangeSiteTupleHoursMap.put(ExchangeSiteTuple.KECK_SOUTH, 0.0d);
        exchangeSiteTupleHoursMap.put(ExchangeSiteTuple.SUBARU_NORTH, 0.0d);
        exchangeSiteTupleHoursMap.put(ExchangeSiteTuple.SUBARU_SOUTH, 0.0d);

        for (Proposal p: proposals) {
            if (p.isExchange()) {
                if (p.isForExchangePartner()) {
                    collectProposalFor(p, Partner.forExchangePartner(p.getExchangeFor(), allPartners));
                } else if (p.isFromExchangePartner()) {
                    collectProposalFrom(p, p.getPartner());
                }
            }
        }
    }

    private void collectProposalFrom(final Proposal p, final Partner partner) {
        Validate.isTrue(partner.isExchange());
        Validate.isTrue((p.getPhaseIProposal() instanceof QueueProposal) || (p.getPhaseIProposal() instanceof ClassicalProposal));
        final PhaseIProposal phaseIProposal = p.getPhaseIProposal();
        final GeminiNormalProposal geminiNormalProposal = (GeminiNormalProposal) phaseIProposal;

        final ExchangeSubmission exchangeSubmission = geminiNormalProposal.getExchange();
        if (exchangeSubmission.getPartner().isExchange() && exchangeSubmission.getPartner().equals(partner)) {
            final SubmissionAccept ngoAccept = exchangeSubmission.getAccept();
            final List<BlueprintBase> blueprints = phaseIProposal.getBlueprints();
            boolean isNorth = false;
            boolean isSouth = false;
            for (BlueprintBase b : blueprints) {
                if (b.isAtNorth()) isNorth = true;
                if (b.isAtSouth()) isSouth = true;
            }

            final TimeAmount exchangeSubmissionRequestTime = ngoAccept.getRecommend();
            final TimeAmount timeAmountInHours = exchangeSubmissionRequestTime.convertTo(TimeUnit.HR);

            if (isNorth && isSouth) {
                for (Site s : Site.sites) {
                    Double current = exchangeSiteTupleHoursMap.get(ExchangeSiteTuple.fromPartnerSite(partner, s));
                    final double increment = timeAmountInHours.getValue().doubleValue() / 2.0d;
                    current += increment;
                    exchangeSiteTupleHoursMap.put(ExchangeSiteTuple.fromPartnerSite(partner, s), current);
                    totalHoursAtNorth += increment;
                    totalHoursAtSouth += increment;
                }
            } else if (isNorth) {
                Double current = exchangeSiteTupleHoursMap.get(ExchangeSiteTuple.fromPartnerSite(partner, Site.NORTH));
                final double increment = timeAmountInHours.getValue().doubleValue();
                current += increment;
                exchangeSiteTupleHoursMap.put(ExchangeSiteTuple.fromPartnerSite(partner, Site.NORTH), current);
                totalHoursAtNorth += increment;
            } else if (isSouth) {
                Double current = exchangeSiteTupleHoursMap.get(ExchangeSiteTuple.fromPartnerSite(partner, Site.SOUTH));
                final double increment = timeAmountInHours.getValue().doubleValue();
                current += increment;
                exchangeSiteTupleHoursMap.put(ExchangeSiteTuple.fromPartnerSite(partner, Site.SOUTH), current);
                totalHoursAtSouth += increment;
            } else {
                LOGGER.log(Level.WARN, "Proposal " + p.getId() + " was not categorized as North or South");
            }
        }
    }

    private void collectProposalFor(final Proposal p, final Partner exchangePartner) {
        Validate.notNull(p);
        Validate.notNull(exchangePartner);
        Validate.isTrue(exchangePartner.isExchange());
        Validate.isTrue(p.getPhaseIProposal() instanceof ExchangeProposal);

        final PhaseIProposal phaseIProposal = p.getPhaseIProposal();
        final ExchangeProposal exchangeProposal = (ExchangeProposal) phaseIProposal;
        final List<BlueprintBase> blueprints = phaseIProposal.getBlueprints();
        final Set<NgoSubmission> ngos = exchangeProposal.getNgos();
        final TimeAmount submissionsTotalRequestedTime = exchangeProposal.getTotalRecommendedTime();

        totalHours += submissionsTotalRequestedTime.convertTo(TimeUnit.HR).getValue().doubleValue();

        for (NgoSubmission n : ngos) {
            Map<Partner, Double> partnerRequest = null;
            switch(exchangeProposal.getPartner()) {
                case KECK:
                    partnerRequest = partnerRequestKeck;
                    break;
                case SUBARU:
                    partnerRequest = partnerRequestSubaru;
                    break;
                default:
                    throw new IllegalArgumentException(exchangePartner.toString());
            }
            final Double currentTime = partnerRequest.get(n.getPartner());
            partnerRequest.put(n.getPartner(), currentTime + submissionsTotalRequestedTime.convertTo(TimeUnit.HR).getValue().doubleValue());
        }
    }

    public String getNorthPercentage() {
        return String.format("%3.2g%%", totalHoursAtNorth / totalHours);
    }

    public String getSouthPercentage() {
        return String.format("%3.2g%%", totalHoursAtSouth / totalHours);
    }

    public double getTotalHours() {
        return totalHours;
    }

    public double getTotalHoursAtNorth() {
        return totalHoursAtNorth;
    }

    public double getTotalHoursAtSouth() {
        return totalHoursAtSouth;
    }

    public double getKeckHoursAtNorth() {
        return exchangeSiteTupleHoursMap.get(ExchangeSiteTuple.KECK_NORTH);
    }

    public double getKeckHoursAtSouth() {
        return exchangeSiteTupleHoursMap.get(ExchangeSiteTuple.KECK_SOUTH);
    }

    public double getSubaruHoursAtNorth() {
        return exchangeSiteTupleHoursMap.get(ExchangeSiteTuple.SUBARU_NORTH);
    }

    public double getSubaruHoursAtSouth() {
        return exchangeSiteTupleHoursMap.get(ExchangeSiteTuple.SUBARU_SOUTH);
    }

    public Map<Partner, Double> getPartnerRequestKeck() {
        return partnerRequestKeck;
    }

    public Map<Partner, Double> getPartnerRequestSubaru() {
        return partnerRequestSubaru;
    }

    private enum ExchangeSiteTuple {
        KECK_NORTH(ExchangePartner.KECK, Site.NORTH),
        KECK_SOUTH(ExchangePartner.KECK, Site.SOUTH),
        SUBARU_NORTH(ExchangePartner.SUBARU, Site.NORTH),
        SUBARU_SOUTH(ExchangePartner.SUBARU, Site.SOUTH);

        private final ExchangePartner exchangePartner;
        private final Site site;

        ExchangeSiteTuple(final ExchangePartner exchangePartner, final Site site) {
            this.exchangePartner = exchangePartner;
            this.site = site;
        }

        public static ExchangeSiteTuple fromPartnerSite(final Partner partner, final Site site) {
            Validate.isTrue(partner.isExchange());
            for (ExchangeSiteTuple est : ExchangeSiteTuple.values()) {
                if (est.exchangePartner.name().equals(partner.getPartnerCountryKey()) && est.site.equals(site))
                    return est;
            }
            
            throw new IllegalArgumentException();
        }
    }
}

