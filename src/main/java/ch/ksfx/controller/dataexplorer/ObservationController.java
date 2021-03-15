package ch.ksfx.controller.dataexplorer;

import ch.ksfx.dao.ObservationDAO;
import ch.ksfx.model.Observation;
import ch.ksfx.model.spidering.ResultUnitModifierConfiguration;
import ch.ksfx.util.DateFormatUtil;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriUtils;
import org.thymeleaf.expression.Uris;
import org.unbescape.uri.UriEscape;

import javax.validation.Valid;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/dataexplorer")
public class ObservationController
{
    private ObservationDAO observationDAO;

    private ObservationController(ObservationDAO observationDAO)
    {
        this.observationDAO = observationDAO;
    }

    @GetMapping({"/observationedit", "/observationedit/{timeSeriesId}/{observationTime}/{sourceId}"})
    public String observationEdit(@PathVariable(value = "timeSeriesId", required = false) String timeSeriesIdParam,
                                  @PathVariable(value = "observationTime", required = false) String observationTimeParam,
                                  @PathVariable(value = "sourceId", required = false) String sourceIdParam,
                                  Model model)
    {
        Observation observation = observationDAO.getObservationForTimeSeriesIdObservationTimeAndSourceId(Integer.parseInt(timeSeriesIdParam), DateFormatUtil.parseISO8601TimeAndDateString(observationTimeParam), UriUtils.decode(sourceIdParam,
                "UTF-8"));

        List<String> complexValueNames = new ArrayList<String>();
        List<String> complexValueValues = new ArrayList<String>();

        for (String key : observation.getComplexValue().keySet()) {
            complexValueNames.add(key);
            complexValueValues.add(observation.getComplexValue().get(key));
        }

        observation.setComplexValueNames(complexValueNames);
        observation.setComplexValueValues(complexValueValues);

        model.addAttribute("dateFormatUtil", new DateFormatUtil());
        model.addAttribute("observation", observation);

        return "dataexplorer/observation_edit";
    }

    @PostMapping("/observationedit")
    public String observationSubmit(@Valid @ModelAttribute Observation observation, BindingResult bindingResult, Model model)
    {
        Map<String,String> complexValue = new HashMap<String, String>();

        for (Integer i = 0; i < observation.getComplexValueNames().size(); i++) {
            complexValue.put(observation.getComplexValueNames().get(i), observation.getComplexValueValues().get(i));
        }

        System.out.println("DEreèèè");
        System.out.println(complexValue.toString());
        System.out.println(bindingResult.toString());
        System.out.println(observation.getObservationTime());

        if (bindingResult.hasErrors()) {
            model.addAttribute("dateFormatUtil", new DateFormatUtil());

            return "dataexplorer/observation_edit";
        }

//        observationDAO.saveObservation(observation);

        return "redirect:/dataexplorer/viewobservation/" + observation.getTimeSeriesId() + "/" + DateFormatUtil.formatToISO8601TimeAndDateString(observation.getObservationTime()) + "/" + UriUtils.encodePathSegment(observation.getSourceId(), "UTF-8").replaceAll("%2F","%252F");
    }

    @GetMapping({"/observationcomplexvalueadd/{timeSeriesId}/{observationTime}/{sourceId}"})
    public String observationComplexValueAdd(@PathVariable(value = "timeSeriesId", required = true) String timeSeriesIdParam,
                                  @PathVariable(value = "observationTime", required = true) String observationTimeParam,
                                  @PathVariable(value = "sourceId", required = true) String sourceIdParam,
                                  Model model)
    {
        Observation observation = observationDAO.getObservationForTimeSeriesIdObservationTimeAndSourceId(Integer.parseInt(timeSeriesIdParam), DateFormatUtil.parseISO8601TimeAndDateString(observationTimeParam), UriUtils.decode(sourceIdParam,
                "UTF-8"));

        observation.addComplexValueFragment("COMPLEX_VAL_XX", "");
        observationDAO.saveObservation(observation);

        return "redirect:/dataexplorer/observationedit/" + observation.getTimeSeriesId() + "/" + DateFormatUtil.formatToISO8601TimeAndDateString(observation.getObservationTime()) + "/" + UriUtils.encodePathSegment(observation.getSourceId(), "UTF-8").replaceAll("%2F","%252F");
    }

    @GetMapping({"/observationcomplexvaluedelete/{timeSeriesId}/{observationTime}/{sourceId}/{complexValueKey}"})
    public String observationComplexValueDelete(@PathVariable(value = "timeSeriesId", required = true) String timeSeriesIdParam,
                                         @PathVariable(value = "observationTime", required = true) String observationTimeParam,
                                         @PathVariable(value = "sourceId", required = true) String sourceIdParam,
                                         @PathVariable(value = "complexValueKey", required = true) String complexValueKey,
                                         Model model)
    {
        Observation observation = observationDAO.getObservationForTimeSeriesIdObservationTimeAndSourceId(Integer.parseInt(timeSeriesIdParam), DateFormatUtil.parseISO8601TimeAndDateString(observationTimeParam), UriUtils.decode(sourceIdParam,
                "UTF-8"));

        observation.getComplexValue().remove(complexValueKey);
        observationDAO.saveObservation(observation);

        return "redirect:/dataexplorer/observationedit/" + observation.getTimeSeriesId() + "/" + DateFormatUtil.formatToISO8601TimeAndDateString(observation.getObservationTime()) + "/" + UriUtils.encodePathSegment(observation.getSourceId(), "UTF-8").replaceAll("%2F","%252F");
    }

    @GetMapping({"/observationmetadataadd/{timeSeriesId}/{observationTime}/{sourceId}"})
    public String observationMetadataAdd(@PathVariable(value = "timeSeriesId", required = true) String timeSeriesIdParam,
                                             @PathVariable(value = "observationTime", required = true) String observationTimeParam,
                                             @PathVariable(value = "sourceId", required = true) String sourceIdParam,
                                             Model model)
    {
        return "redirect:/dataexplorer/observationedit/" + timeSeriesIdParam + "/" + observationTimeParam + "/" + sourceIdParam;
    }

    @GetMapping({"/observationmetadatadelete/{timeSeriesId}/{observationTime}/{sourceId}/{metadataKey}"})
    public String observationMetadataDelete(@PathVariable(value = "timeSeriesId", required = true) String timeSeriesIdParam,
                                         @PathVariable(value = "observationTime", required = true) String observationTimeParam,
                                         @PathVariable(value = "sourceId", required = true) String sourceIdParam,
                                         @PathVariable(value = "metadataKey", required = true) String metadataKey,
                                         Model model)
    {
        return "redirect:/dataexplorer/observationedit/" + timeSeriesIdParam + "/" + observationTimeParam + "/" + sourceIdParam;
    }

    /*
    private Integer seriesId;
    private String observationTimeString;
    private String sourceId;

    @Property
    @Persist
    private Integer numberOfAddedComplexValueFragments;

    @Property
    @Persist
    private Integer numberOfAddedMetaDataFragments;

    @Persist
    private List<String> complexValueKeys;

    @Persist
    private List<String> complexValueValues;

    @Persist
    private List<String> metaDataKeys;

    @Persist
    private List<String> metaDataValues;

    @Persist
    private String sourceIdBefore;

    @Property
    private Observation observation;

    @Inject
    private ObservationDAO observationDAO;

    private String complexValueKey;

    @Property
    private String complexValueKeyLoop;

    @Property
    private String metaDataKeyLoop;

    public void onActivate(Object[] objects)
    {
        complexValueKeys = new ArrayList<String>();
        complexValueValues = new ArrayList<String>();
        metaDataKeys = new ArrayList<String>();
        metaDataValues = new ArrayList<String>();

        this.seriesId = Integer.parseInt((String) objects[0]);
        this.observationTimeString = (String) objects[1];
        this.sourceId = (String) objects[2];

        if (!sourceId.equals(sourceIdBefore)) {
            numberOfAddedComplexValueFragments = 1;
            numberOfAddedMetaDataFragments = 1;
        }

        sourceIdBefore = sourceId;

        System.out.println("Series Id " + seriesId);
        System.out.println("Date " + DateFormatUtil.parseISO8601TimeAndDateString(observationTimeString));
        System.out.println("Source Id " + sourceId);

        observation = observationDAO.getObservationForTimeSeriesIdObservationTimeAndSourceId(seriesId, DateFormatUtil.parseISO8601TimeAndDateString(observationTimeString), sourceId);
    }

    public void onActionFromAddMetaDataFragment()
    {
        System.out.println("Adding meta data fragment");

        observation.addMetaDataFragment("META_DATA_" + numberOfAddedMetaDataFragments, "");
        observationDAO.saveObservation(observation);

        numberOfAddedMetaDataFragments++;
    }

    public void onActionFromDeleteMetaDataFragment(String fragmentKey)
    {
        observation.getMetaData().remove(fragmentKey);
        observationDAO.saveObservation(observation);
    }

    public String getMetaDataKey()
    {
        return metaDataKeyLoop;
    }

    public void setMetaDataKey(String metaDataKey)
    {
        metaDataKeys.add(metaDataKey);
    }

    public String getMetaDataValue()
    {
        return observation.getMetaData().get(metaDataKeyLoop);
    }

    public void setMetaDataValue(String metaDataValue)
    {
        metaDataValues.add(metaDataValue);
    }

    public void onActionFromAddComplexValueFragment()
    {
        observation.addComplexValueFragment("COMPLEX_VAL_" + numberOfAddedComplexValueFragments, "");
        observationDAO.saveObservation(observation);

        numberOfAddedComplexValueFragments++;
    }

    public void onActionFromDeleteComplexValueFragment(String fragmentKey)
    {
        observation.getComplexValue().remove(fragmentKey);
        observationDAO.saveObservation(observation);
    }

    public String getComplexValueKey()
    {
        return complexValueKeyLoop;
    }

    public void setComplexValueKey(String complexValueKey)
    {
        complexValueKeys.add(complexValueKey);
    }

    public String getComplexValueValue()
    {
        return observation.getComplexValue().get(complexValueKeyLoop);
    }

    public void setComplexValueValue(String complexValueValue)
    {
        complexValueValues.add(complexValueValue);
    }

    public void onSuccess()
    {
        Map<String, String> complexValue = new HashMap<String,String>();

        for (Integer i = 0; i < complexValueKeys.size(); i++) {
            System.out.println(complexValueKeys.get(i) + ": " + complexValueValues.get(i));
            complexValue.put(complexValueKeys.get(i), complexValueValues.get(i));
        }

        observation.setComplexValue(complexValue);

        Map<String, String> metaData = new HashMap<String,String>();

        for (Integer i = 0; i < metaDataKeys.size(); i++) {
            System.out.println(metaDataKeys.get(i) + ": " + metaDataValues.get(i));
            metaData.put(metaDataKeys.get(i), metaDataValues.get(i));
        }

        observation.setMetaData(metaData);

        observationDAO.saveObservation(observation);
    }

    public Object onPassivate() {
        List<Object> p = new ArrayList<Object>();

        if (seriesId != null) {
            p.add(seriesId);
        }

        if (observationTimeString != null) {
            p.add(observationTimeString);
        }

        if (sourceId != null) {
            p.add(sourceId);
        }

        return p;
    }
    */
}
