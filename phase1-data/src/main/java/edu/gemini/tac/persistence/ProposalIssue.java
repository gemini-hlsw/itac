package edu.gemini.tac.persistence;

import javax.persistence.*;

/**
 * Java side implementation class for proposal issues that allows to make the Scala implementation issues
 * persistent. This is a bit a redundant, but I don't know how to annotate Scala classes to use them with
 * Hibernate...
 */
@Entity
@Table(name = "proposal_issues")
public class ProposalIssue {

    public ProposalIssueCategory getCategory() {
        for(ProposalIssueCategory c : ProposalIssueCategory.values()){
            if(c.ordinal() == this.category){
                return c;
            }
        }
        throw new IllegalArgumentException(String.format("Persisted value for ProposalIssueCategory (%d) does not have corresponding enum value", this.category));
    }

    public void setCategory(ProposalIssueCategory category) {
        this.category = category.ordinal();
    }

    /**
     * A very simple enum for different severities of issues.
     * TODO: is there a simple and easy way to map enums? I am too lazy to implement a user type right now.
     */
    public enum Severity {
        WARNING,
        ERROR;

        public static Severity parseInteger(int integer) {
            switch (integer) {
                case 0 : return WARNING;
                case 1 : return ERROR;
                default:
                    throw new RuntimeException("unknown severity");
            }
        }
    }

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @OneToOne
    @JoinColumn(name = "proposal_id")
    private Proposal proposal;

    @Column(name = "severity")
    private int severity;

    @Column(name = "message")
    private String message;

    @Column(name = "category")
    private int category;


    // needed by Hibernate
    public ProposalIssue() {
    }

    /**
     * Creates a proposal issue.
     * @param proposal
     * @param severity
     * @param message
     */
    public ProposalIssue(Proposal proposal, int severity, String message, ProposalIssueCategory category) {
        this.proposal = proposal;
        this.severity = severity;
        this.message = message;
        setCategory(category);
    }

    public Long getId() {
        return id;
    }

    public Proposal getProposal() {
        return proposal;
    }

    public Severity getSeverity() {
        return Severity.parseInteger(severity);
    }

    public String getMessage() {
        return message;
    }

    public boolean isError() {
        return getSeverity().equals(Severity.ERROR);
    }

    public boolean isWarning() {
        return getSeverity().equals(Severity.WARNING);
    }
}
