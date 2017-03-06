package edu.gemini.tac.itac.web;

import edu.gemini.tac.itac.web.UrlFor.Controller;

public class Link {
	private Controller controller = null;
	private Controller topLevelController = null;
	private String resource = null;
	private String topLevelResource = null;
	private String actionName = null;
	private String displayName = null;
    private String filter = null;
    private String filterResource = null;
    private String icon = null;

    public Link(final Controller controller, final Controller topLevelController, final String topLevelResource) {
		this(controller, topLevelController);
		this.topLevelResource = topLevelResource;
	}
	
	public Link(final Controller controller, final Controller topLevelController, final String topLevelResource, final String displayName) {
		this(controller, topLevelController, topLevelResource);
		this.displayName = displayName;
	}

	public Link(final Controller controller, final Controller topLevelController, final String resource, final String topLevelResource, 
			final String actionName, final String displayName) {
		this(controller, topLevelController, topLevelResource, actionName, displayName);
		this.resource = resource;
	}

	
	public Link(final Controller controller, final Controller topLevelController, final String topLevelResource,
			final String actionName, final String displayName) {
		this(controller, topLevelController, topLevelResource);
		this.actionName = actionName;
		this.displayName = displayName;
	}

	public Link(final Controller controller, final Controller topLevelController) {
		this.controller = controller;
		this.topLevelController = topLevelController;
	}
	
	public Link(final Controller controller) {
		this.controller = controller;
	}
	
	public String getHref() {
		String href = null;
		
		if (topLevelResource == null) {
			if (topLevelController == null) {
				href =  UrlFor.CONTEXT_PATH + controller.getUrlComponent();
			}
			else {
				href = UrlFor.CONTEXT_PATH + topLevelController.getUrlComponent() + 
					controller.getUrlComponent();
			}
		} else {
			if (actionName == null)
				href = UrlFor.CONTEXT_PATH + topLevelController.getUrlComponent() + 
					"/" + topLevelResource + controller.getUrlComponent();
			else {
				if (resource == null) {
                    if (filter == null) {
					    href = UrlFor.CONTEXT_PATH + topLevelController.getUrlComponent() +
						    "/" + topLevelResource + controller.getUrlComponent() + "/" + actionName;
                    } else {
                        href = UrlFor.CONTEXT_PATH + topLevelController.getUrlComponent() +
                            "/" + topLevelResource + controller.getUrlComponent() + "/" + actionName + "/" +
                            filter + "/" + filterResource;
                    }
				} else {
					href = UrlFor.CONTEXT_PATH + topLevelController.getUrlComponent() +
						"/" + topLevelResource + controller.getUrlComponent() + "/" + resource + "/" + actionName;
				}
			}
		}
		
		return href;
	}
	
	public String getDisplayName() {
		return (displayName == null) ? controller.getDisplayName() : displayName;
	}
	
	public Controller getController() {
		return controller;
	}

    public Link setFilter(String filter) {
        this.filter = filter;

        return this;
    }

    public Link setFilterResource(String filterResource) {
        this.filterResource = filterResource;

        return this;
    }

    public Link setIcon(String icon) {
        this.icon = icon;

        return this;
    }

    public String getIconImageLink() {
        if (icon != null)
            return "<img class=\"left\" src=\"/static/images/fromOCS2PIT/" + icon + "\"</img>";
        else
            return "";
    }
}
