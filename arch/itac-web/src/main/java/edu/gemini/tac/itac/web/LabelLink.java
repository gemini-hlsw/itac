package edu.gemini.tac.itac.web;

public class LabelLink {
	private final String label;
	private final String link;

	public LabelLink(final String label, final String link) {
		this.label = label;
		this.link = link;
	}

	public String getLabel() {
		return label;
	}

	public String getLink() {
		return link;
	}
}
