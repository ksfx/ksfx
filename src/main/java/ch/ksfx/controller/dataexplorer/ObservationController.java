package ch.ksfx.controller.dataexplorer;

import ch.ksfx.dao.ObservationDAO;
import ch.ksfx.dao.activity.ActivityDAO;
import ch.ksfx.dao.activity.ActivityInstanceDAO;
import ch.ksfx.model.Observation;
import ch.ksfx.model.activity.Activity;
import ch.ksfx.model.activity.ActivityInstance;
import ch.ksfx.model.activity.ActivityInstanceParameter;
import ch.ksfx.services.activity.ActivityInstanceRunner;
import ch.ksfx.util.DateFormatUtil;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriUtils;
import javax.validation.Valid;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/dataexplorer")
public class ObservationController
{
    private ObservationDAO observationDAO;
    private ActivityDAO activityDAO;
    private ActivityInstanceDAO activityInstanceDAO;
    private ActivityInstanceRunner activityInstanceRunner;

    private ObservationController(ObservationDAO observationDAO, ActivityDAO activityDAO, ActivityInstanceDAO activityInstanceDAO, ActivityInstanceRunner activityInstanceRunner)
    {
        this.observationDAO = observationDAO;
        this.activityDAO = activityDAO;
        this.activityInstanceDAO = activityInstanceDAO;
        this.activityInstanceRunner = activityInstanceRunner;
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

        List<String> metaDataNames = new ArrayList<String>();
        List<String> metaDataValues = new ArrayList<String>();

        for (String key : observation.getMetaData().keySet()) {
            metaDataNames.add(key);
            metaDataValues.add(observation.getMetaData().get(key));
        }

        observation.setMetaDataNames(metaDataNames);
        observation.setMetaDataValues(metaDataValues);

        model.addAttribute("dateFormatUtil", new DateFormatUtil());
        model.addAttribute("observation", observation);

        return "dataexplorer/observation_edit";
    }

    @PostMapping("/observationedit")
    public String observationSubmit(@Valid @ModelAttribute Observation observation, BindingResult bindingResult, Model model)
    {
        if (observation.getComplexValueNames() != null) {
            Map<String, String> complexValue = new HashMap<String, String>();

            for (Integer i = 0; i < observation.getComplexValueNames().size(); i++) {
                System.out.println("Complex value name: " + observation.getComplexValueNames().get(i));
                complexValue.put(observation.getComplexValueNames().get(i), observation.getComplexValueValues().get(i));
            }

            observation.setComplexValue(complexValue);
        }

        if (observation.getMetaDataNames() != null) {
            Map<String, String> metaDataValue = new HashMap<String, String>();

            for (Integer i = 0; i < observation.getMetaDataNames().size(); i++) {
                System.out.println("Meta data name: " + observation.getMetaDataNames().get(i));
                metaDataValue.put(observation.getMetaDataNames().get(i), observation.getMetaDataValues().get(i));
            }

            observation.setMetaData(metaDataValue);
        }

        System.out.println(bindingResult.toString());
        System.out.println(observation.getObservationTime());

        if (bindingResult.hasErrors()) {
            model.addAttribute("dateFormatUtil", new DateFormatUtil());

            return "dataexplorer/observation_edit";
        }

        observationDAO.saveObservation(observation);

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
        Observation observation = observationDAO.getObservationForTimeSeriesIdObservationTimeAndSourceId(Integer.parseInt(timeSeriesIdParam), DateFormatUtil.parseISO8601TimeAndDateString(observationTimeParam), UriUtils.decode(sourceIdParam,
                "UTF-8"));

        observation.addMetaDataFragment("META_VAL_XX", "");
        observationDAO.saveObservation(observation);

        return "redirect:/dataexplorer/observationedit/" + observation.getTimeSeriesId() + "/" + DateFormatUtil.formatToISO8601TimeAndDateString(observation.getObservationTime()) + "/" + UriUtils.encodePathSegment(observation.getSourceId(), "UTF-8").replaceAll("%2F","%252F");
    }

    @GetMapping({"/observationmetadatadelete/{timeSeriesId}/{observationTime}/{sourceId}/{metadataKey}"})
    public String observationMetadataDelete(@PathVariable(value = "timeSeriesId", required = true) String timeSeriesIdParam,
                                         @PathVariable(value = "observationTime", required = true) String observationTimeParam,
                                         @PathVariable(value = "sourceId", required = true) String sourceIdParam,
                                         @PathVariable(value = "metadataKey", required = true) String metadataKey,
                                         Model model)
    {
        Observation observation = observationDAO.getObservationForTimeSeriesIdObservationTimeAndSourceId(Integer.parseInt(timeSeriesIdParam), DateFormatUtil.parseISO8601TimeAndDateString(observationTimeParam), UriUtils.decode(sourceIdParam,
                "UTF-8"));

        observation.getMetaData().remove(metadataKey);
        observationDAO.saveObservation(observation);

        return "redirect:/dataexplorer/observationedit/" + observation.getTimeSeriesId() + "/" + DateFormatUtil.formatToISO8601TimeAndDateString(observation.getObservationTime()) + "/" + UriUtils.encodePathSegment(observation.getSourceId(), "UTF-8").replaceAll("%2F","%252F");
    }

    @PostMapping({"/observationsendtoactivity", "/observationsendtoactivity/{timeSeriesId}/{observationTime}/{sourceId}"})
    public String observationSendToActivity(@PathVariable(value = "timeSeriesId", required = false) String timeSeriesIdParam,
                                            @PathVariable(value = "observationTime", required = false) String observationTimeParam,
                                            @PathVariable(value = "sourceId", required = false) String sourceIdParam,
                                            @RequestParam(name = "sendToActivityId") Long sendToActivityId,
                                            Model model)
    {
        Observation observation = observationDAO.getObservationForTimeSeriesIdObservationTimeAndSourceId(Integer.parseInt(timeSeriesIdParam), DateFormatUtil.parseISO8601TimeAndDateString(observationTimeParam), UriUtils.decode(sourceIdParam,
                "UTF-8"));

        Activity sendToActivity = activityDAO.getActivityForId(sendToActivityId);

        System.out.println(sendToActivity.toString());
        System.out.println(observation.toString());

        ActivityInstance activityInstance = new ActivityInstance();
        activityInstance.setActivity(sendToActivity);

        activityInstanceDAO.saveOrUpdateActivityInstance(activityInstance);

        ActivityInstanceParameter activityInstanceParameter = new ActivityInstanceParameter("time_series_id", observation.getTimeSeriesId().toString());
        activityInstanceParameter.setActivityInstance(activityInstance);
        activityInstanceDAO.saveOrUpdateActivityInstanceParameter(activityInstanceParameter);

        activityInstanceParameter = new ActivityInstanceParameter("source_id", observation.getSourceId().toString());
        activityInstanceParameter.setActivityInstance(activityInstance);
        activityInstanceDAO.saveOrUpdateActivityInstanceParameter(activityInstanceParameter);

        activityInstanceParameter = new ActivityInstanceParameter("observation_time", DateFormatUtil.formatToISO8601TimeAndDateString(observation.getObservationTime()));
        activityInstanceParameter.setActivityInstance(activityInstance);
        activityInstanceDAO.saveOrUpdateActivityInstanceParameter(activityInstanceParameter);

        activityInstanceRunner.runActivity(activityInstanceDAO.getActivityInstanceForId(activityInstance.getId()));

        return "redirect:/dataexplorer/viewobservation/" + observation.getTimeSeriesId() + "/" + DateFormatUtil.formatToISO8601TimeAndDateString(observation.getObservationTime()) + "/" + UriUtils.encodePathSegment(observation.getSourceId(), "UTF-8").replaceAll("%2F","%252F");
    }
}
