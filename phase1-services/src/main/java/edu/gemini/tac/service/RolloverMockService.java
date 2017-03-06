//TODO: Re-implement
//package edu.gemini.tac.service;
//
//import edu.gemini.phase1.model.P1Time;
//import edu.gemini.shared.skycalc.Coordinates;
//import edu.gemini.tac.persistence.Partner;
//import edu.gemini.tac.persistence.Site;
//import edu.gemini.tac.persistence.phase1.Observation;
//import edu.gemini.tac.persistence.phase1.SiteQuality;
//import edu.gemini.tac.persistence.phase1.Target;
//import edu.gemini.tac.persistence.phase1.Time;
//import edu.gemini.tac.persistence.phase1.coordinatesystem.CoordinateSystem;
//import edu.gemini.tac.persistence.phase1.coordinatesystem.HmsDegSystem;
//import edu.gemini.tac.persistence.rollover.*;
//import net.sf.json.JSONObject;
//import net.sf.json.JsonConfig;
//import net.sf.json.processors.JsonBeanProcessor;
//import net.sf.json.processors.JsonBeanProcessorMatcher;
//import net.sf.json.processors.PropertyNameProcessor;
//import net.sf.json.util.JSONUtils;
//import net.sf.json.util.PropertyFilter;
//import org.apache.log4j.Level;
//import org.apache.log4j.Logger;
//import net.sf.json.JSONArray;
//import org.hibernate.Session;
//import org.hibernate.SessionFactory;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//
//
//import javax.annotation.Resource;
//
//import org.hibernate.Query;
//
//import java.io.*;
//import java.util.*;
//
///**
// * TODO: Why does this class exist?
// * Author: lobrien
// * Date: 3/8/11
// */
//            /*
//public class RolloverMockService implements IRolloverService {
//    Logger LOGGER = Logger.getLogger(RolloverMockService.class);
//
//    private class CustomJsonBeanProcessorMatcher extends JsonBeanProcessorMatcher {
//        @Override
//        public Object getMatch(Class target, Set set) {
//            //LOGGER.log(Level.ERROR, "Matching " + target + " w " + set);
//            for (Object match : set) {
//                if (((Class) match).isAssignableFrom(target)) {
//                    return match;
//                }
//            }
//            return null;
//        }
//    }
//
//
//    private void handBuiltAndThenOutputJson() {
//        RolloverReport rpt = new RolloverReport();
//        rpt.setSite(new Site());
//        HashSet<IRolloverObservation> observations = new HashSet<IRolloverObservation>();
//
//        RolloverObservation rolloverObservation = new RolloverObservation();
//        rolloverObservation.setObservationId("My RolloverObservation");
//        Time time = new Time();
//        time.setId(0L);
//        time.setTimeAmount(8);
//        time.setTimeUnits(P1Time.Units.HOURS);
//        rolloverObservation.setObservationTime(time);
//
//        Partner partner = new Partner();
//        partner.setAbbreviation("USA");
//        partner.setId(0L);
//        partner.setName("United States of America");
//        partner.setPartnerCountryKey("USA");
//        partner.setIsCountry(true);
//        rolloverObservation.setPartner(partner);
//
//        SiteQuality siteQuality = new SiteQuality();
//        siteQuality.setName("My Site Quality");
//        siteQuality.setId(0L);
//
//        rolloverObservation.setSiteQuality(siteQuality);
//
//        Target target = new Target();
//        target.setId(0L);
//        target.setName("Some Galaxy");
//        HmsDegSystem coords = new HmsDegSystem();
//        coords.setC1AsString("8");
//        coords.setC2AsString("12");
//        target.setCoordinates(coords);
//        rolloverObservation.setTarget(target);
//
//        observations.add(rolloverObservation);
//        rpt.setObservations(observations);
//
//        JSONObject o = JSONObject.fromObject(rpt);
//        LOGGER.log(Level.DEBUG, o.toString(3));
//    }
//
//
//    @Override
//    public IRolloverReport report(Site site, IPartnerService partnerService) {
//        try {
//            String json = readFile();
//
//            JSONArray jsonArray = JSONArray.fromObject(json);
//
//            for (int i = 0; i < jsonArray.size(); i++) {
//                RolloverReport rr = jsonToRolloverReport(jsonArray.getJSONObject(i));
//                if (rr.getSiteOrNull().getDisplayName().equals(site.getDisplayName()) && (!(rr instanceof RolloverSet))) {
//                    return rr;
//                }
//            }
//        } catch (Exception x) {
//            throw new RuntimeException(x);
//        }
//        throw new RuntimeException("Do not have mock RolloverReport for site " + site.getDisplayName() + " -- you'll need to add it to rolloverreports.json");
//    }
//
//    @Override
//    public RolloverSet createRolloverSet(Site site, String setName, String[] observationIds, String[] timesAllocated) {
//        try {
//            RolloverSet rolloverSet = new RolloverSet();
//            rolloverSet.setSite(site);
//            rolloverSet.setName(setName);
//
//            JSONArray jsonArray = JSONArray.fromObject(readFile());
//
//            for (int i = 0; i < jsonArray.size(); i++) {
//                RolloverReport rr = jsonToRolloverReport(jsonArray.getJSONObject(i));
//                if (rr.getSiteOrNull().getDisplayName().equals(site.getDisplayName()) && (!(rr instanceof RolloverSet))) {
//                    Set<IRolloverObservation> originalObservations = rr.getObservations();
//                    Set<IRolloverObservation> filteredObservations = new HashSet<IRolloverObservation>();
//                    for (String observationId : observationIds) {
//                        for (IRolloverObservation observation : originalObservations) {
//                            if (observation.observationId().equals(observationId)) {
//                                filteredObservations.add(observation);
//                            }
//                        }
//                    }
//                    rolloverSet.setObservations(filteredObservations);
//                    return rolloverSet;
//                }
//            }
//        } catch (Exception x) {
//            throw new RuntimeException(x);
//        }
//        throw new RuntimeException("Could not create rolloverset");
//    }
//
//    @Override
//    public Set<RolloverSet> rolloverSetsFor(String siteName) {
//        Set<RolloverSet> resultSets = new HashSet<RolloverSet>();
//        try {
//
//            JSONArray jsonArray = JSONArray.fromObject(readFile());
//            for (int i = 0; i < jsonArray.size(); i++) {
//                RolloverReport rr = jsonToRolloverReport(jsonArray.getJSONObject(i));
//                if (rr.getSiteOrNull().getDisplayName().equals(siteName) && ((rr instanceof RolloverSet))) {
//                    resultSets.add((RolloverSet) rr);
//                }
//            }
//        } catch (IOException x) {
//            LOGGER.log(Level.ERROR, x);
//            throw new RuntimeException(x);
//        }
//        return resultSets;
//    }
//
//    @Override
//    public RolloverSet getRolloverSet(Long rolloverSetId) {
//        Set<RolloverSet> sets = rolloverSetsFor("North");
//        sets.addAll(rolloverSetsFor("South"));
//        for(RolloverSet set : sets){
//            if(set.getId().equals(rolloverSetId)){
//                return set;
//            }
//        }
//
//        throw new RuntimeException("Could not find rollover set " + rolloverSetId + " -- you need to add it to the json file");
//    }
//
//    private String readFile() throws IOException {
//        final InputStream inputStream = this.getClass().getResourceAsStream("/rolloverreports.json");
//        Writer writer = new StringWriter();
//        char[] buffer = new char[1024];
//        try {
//            Reader reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
//            int n;
//            while ((n = reader.read(buffer)) != -1) {
//                writer.write(buffer, 0, n);
//            }
//            return writer.toString();
//        } finally {
//            inputStream.close();
//        }
//    }
//
//    private RolloverReport jsonToRolloverReport(JSONObject jo) {
//        RolloverReport rr = null;
//        if (jo.get("name") != null) {
//            rr = new RolloverSet();
//            String name = jo.get("name").toString();
//            ((RolloverSet) rr).setName(name);
//        } else {
//            rr = new RolloverReport();
//        }
//
//        rr.setId(Long.parseLong(jo.get("id").toString()));
//
//        Site site = (Site) JSONObject.toBean(jo.getJSONObject("site"), Site.class);
//        rr.setSite(site);
//
//        Set<IRolloverObservation> observations = new HashSet<IRolloverObservation>();
//        JSONArray obs = JSONArray.fromObject(jo.getJSONArray("observations"));
//        for (int i = 0; i < obs.size(); i++) {
//            RolloverObservation observation = jsonToRolloverObservation(obs.getJSONObject(i));
//            observation.setParent(rr);
//            observations.add(observation);
//        }
//        rr.setObservations(observations);
//        return rr;
//    }
//
//    private RolloverObservation jsonToRolloverObservation(JSONObject jo) {
//        JSONObject targetJSON = jo.getJSONObject("target");
//
//        //Have to deal with "knowing" that coordinates will always be HmsDegSystem -- build explicitly and filter from auto-processing
//        if (targetJSON.size() > 0) {
//            JSONObject coordsJSON = targetJSON.getJSONObject("coordinates");
//            CoordinateSystem coords = (CoordinateSystem) JSONObject.toBean(coordsJSON, HmsDegSystem.class);
//            JsonConfig config = new JsonConfig();
//            config.setRootClass(Target.class);
//            config.setJavaPropertyFilter(new PropertyFilter() {
//                public boolean apply(Object source, String name, Object value) {
//                    if ("coordinates".equals(name)) {
//                        return true;
//                    }
//                    return false;
//                }
//            });
//
//
//            Target t = (Target) JSONObject.toBean(targetJSON, config);
//            t.setCoordinates(coords);
//
//            //Reuse config (which filters out "coordinates"
//            config.setRootClass(RolloverObservation.class);
//
//            RolloverObservation obs = (RolloverObservation) JSONObject.toBean(jo, config);
//
//            //Explicitly set time units to hours
//            obs.getObservationTime().setTimeUnits(P1Time.Units.HOURS);
//            obs.setTarget(t);
//            return obs;
//        }
//        return null;
//    }
//}
//*/