package edu.gemini.tac.service;

import edu.gemini.tac.persistence.restrictedbin.RestrictedBin;

import java.util.List;

public interface IRestrictedBinsService {
    List<RestrictedBin> getAllRestrictedbins();
    List<RestrictedBin> getDefaultRestrictedbins();
}
