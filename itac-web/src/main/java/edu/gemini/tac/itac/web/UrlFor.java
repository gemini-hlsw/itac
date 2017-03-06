package edu.gemini.tac.itac.web;


import edu.gemini.tac.persistence.Proposal;
import edu.gemini.tac.persistence.queues.Queue;

public class UrlFor {
	public static final String CONTEXT_PATH = "/tac";

    protected UrlFor() {}

	public enum Controller {
		COMMITTEES("Committees", "committees"),
		PEOPLE("People", "people"),
		CONFIGURATION("Configuration", "configuration"),
		SESSION("Session", "session"),
		HOME("Home", "home"),
		
		// Configuration
		CONDITION_BINS("Condition Bins", "condition-bins"),
		RADEC("RA/DEC", "radec"),
		EMAIL_TEMPLATES("Email templates", "email-templates"),
        EMAIL_CONTACTS("Email addresses", "email-addresses"),
		RESTRICTED_BINS("Restricted bins", "restrictions"),
        COMMITTEES_MANAGEMENT("Committee Management", "committees"),
        QUEUE_CONFIGURATION("Partner %s", "partner-percentages"),

		// Committees
		QUEUES("Queues", "queues"),
		PROPOSALS("Proposals", "proposals"),
        JOINT_PROPOSALS("Joint proposals", "proposals"),
		MEMBERS("Members", "members"),
		WINDOWS("<del>Windows</del>", "windows"),
		EMAILS("Emails", "emails"),
		REPORTS("Committee Reports", "reports"),
        SHUTDOWN_CONFIGURATION("Shutdown dates", "shutdowns"),
        PARTNER_SEQUENCE_CONFIGURATION("Partner sequence", "partner_sequence"),
		ROLLOVERS("Rollovers", "rollovers"),
        BLACKOUTS("Blackouts", "blackouts"),
		
		// People
		ME("Me", "me"),
		ADMIN("<del>Admin</del>", "admin"),

        // Log
        LOG("Committee Log","log");

		public final String displayName;
		public final String canonical;

		private Controller(final String displayName, final String canonical) {
			this.displayName = displayName;
			this.canonical = canonical;
		}
		public String getDisplayName() {
			return displayName;
		}
		public String getUrlComponent() {
			return "/" + canonical;
		}
		public String getCanonical() {
			return canonical;
		}
	}
	
	public static String controller(final Controller controller) {
		return CONTEXT_PATH + controller.getUrlComponent();
	}

    public static String getProposalLink(final Long committeeId, final Long proposalId, final String receiptId) {
        return String.format("<a href=\"/%s/committees/%d/proposals/%d\">%s</a>", CONTEXT_PATH, committeeId, proposalId, receiptId);
    }
    public static String getProposalLink(final Long committeeId, final Proposal proposal) {
        return getProposalLink(committeeId,
                proposal.getId(),
                proposal.getPhaseIProposal().getPrimary().getReceipt().getReceiptId());
    }
    public static String getProposalLink(final Proposal proposal) {
        return getProposalLink(proposal.getCommittee().getId(),
                proposal.getId(),
                proposal.getPhaseIProposal().getPrimary().getReceipt().getReceiptId());
    }

    public static String getQueueLink(final Long committeeId, final Long queueId, final String queueName) {
        return String.format("<a href=\"/%s/committees/%d/queues/%d\">%s</a>", CONTEXT_PATH, committeeId, queueId, queueName);

    }
    public static String getQueueLink(final Queue queue) {
        return getQueueLink(queue.getCommittee().getId(), queue.getId(), queue.getName());
    }
}
