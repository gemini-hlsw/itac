package edu.gemini.tac.service;

import edu.gemini.tac.persistence.Site;

/**
 * Service layer for Sites
 *
 * Author: lobrien
 * Date: 3/10/11
 */
public interface ISiteService {

    Site findByDisplayName(String siteName);
}
