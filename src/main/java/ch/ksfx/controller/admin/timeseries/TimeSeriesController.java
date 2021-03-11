package ch.ksfx.controller.admin.timeseries;

import ch.ksfx.dao.TimeSeriesDAO;
import ch.ksfx.dao.spidering.ResultUnitModifierConfigurationDAO;
import ch.ksfx.model.TimeSeries;
import ch.ksfx.model.spidering.ResourceLoaderPluginConfiguration;
import ch.ksfx.services.ServiceProvider;
import ch.ksfx.services.systemlogger.SystemLogger;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin/timeseries")
public class TimeSeriesController
{
    private TimeSeriesDAO timeSeriesDAO;

    public TimeSeriesController(TimeSeriesDAO timeSeriesDAO)
    {
        this.timeSeriesDAO = timeSeriesDAO;
    }

    @GetMapping("/")
    public String timeSeriesIndex(Model model, Pageable pageable)
    {
        model.addAttribute("timeSeriesPage", timeSeriesDAO.getTimeSeriesForPageable(pageable));

        return "admin/timeseries/time_series";
    }

    @GetMapping({"/timeseriesedit", "/timeseriesedit/{id}"})
    public String timeSeriesEdit(@PathVariable(value = "id", required = false) Long timeSeriesId, Model model)
    {
        TimeSeries timeSeries = new TimeSeries();

        if (timeSeriesId != null) {
            timeSeries = timeSeriesDAO.getTimeSeriesForId(timeSeriesId);
        }

        model.addAttribute("allTimeSeriesTypes", timeSeriesDAO.getAllTimeSeriesTypes());
        model.addAttribute("timeSeries", timeSeries);

        return "admin/timeseries/time_series_edit";
    }

    @PostMapping({"/timeseriesedit", "/timeseriesedit/{id}"})
    public String timeSeriesSubmit(@PathVariable(value = "id", required = false) Long timeSeriesId, @Valid @ModelAttribute TimeSeries timeSeries, BindingResult bindingResult, Model model)
    {
        if (bindingResult.hasErrors()) {
            model.addAttribute("allTimeSeriesTypes", timeSeriesDAO.getAllTimeSeriesTypes());

            return "admin/timeseries/time_series_edit";
        }

        timeSeriesDAO.saveOrUpdate(timeSeries);

        return "redirect:/admin/timeseries/timeseriesedit/" + timeSeries.getId();
    }

    @GetMapping({"/timeseriesdelete/{id}"})
    public String timeSeriesDelete(@PathVariable(value = "id", required = true) Long timeSeriesId)
    {
        TimeSeries timeSeries = timeSeriesDAO.getTimeSeriesForId(timeSeriesId);
        timeSeriesDAO.delete(timeSeries);

        return "redirect:/informationretrieval/resourceloaderpluginconfiguration";
    }
}
