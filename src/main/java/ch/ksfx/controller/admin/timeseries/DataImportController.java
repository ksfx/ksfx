package ch.ksfx.controller.admin.timeseries;

import au.com.bytecode.opencsv.CSVReader;
import ch.ksfx.dao.ObservationDAO;
import ch.ksfx.dao.TimeSeriesDAO;
import ch.ksfx.model.ImportableField;
import ch.ksfx.model.Observation;
import ch.ksfx.model.TimeSeries;
import ch.ksfx.model.note.Note;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.io.File;
import java.io.FileReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Controller
@RequestMapping("/admin")
public class DataImportController
{
    private TimeSeriesDAO timeSeriesDAO;
    private ObservationDAO observationDAO;

    public DataImportController(TimeSeriesDAO timeSeriesDAO, ObservationDAO observationDAO)
    {
        this.timeSeriesDAO = timeSeriesDAO;
        this.observationDAO = observationDAO;
    }

    @GetMapping("/timeseriesimport/{timeSeriesId}")
    public String dataImportStart(@PathVariable(value = "timeSeriesId", required = true) Long timeSeriesId, Model model)
    {
        TimeSeries timeSeries = timeSeriesDAO.getTimeSeriesForId(timeSeriesId);

        model.addAttribute("timeSeries", timeSeries);

        return "admin/timeseries/data_import";
    }

    @PostMapping("/timeseriesimport")
    public String onSuccessFromUploadForm(@RequestParam(value = "uploadFile", required = true) MultipartFile uploadFile, @RequestParam(value = "csvSeparator", required = true) String csvSeparator, @RequestParam(value = "csvQuoteChar", required = false) String csvQuoteChar, @RequestParam(value = "timeSeriesId", required = true) Long timeSeriesId, Model model, HttpServletRequest request)
    {
        DataImportModel dataImportModel = new DataImportModel();

        dataImportModel.setIgnoredLines(new ArrayList<Integer>());
//        ignoredLines = new ArrayList<Boolean>();

        try {
            File file = File.createTempFile("data","import");
            uploadFile.transferTo(file);

            char seperator = 0;
            char quoteChar = 0;

            if (csvSeparator != null) {
                seperator = csvSeparator.charAt(0);

                if (csvSeparator.equals("\\t")) {
                    seperator = '\t';
                }
            }

            if (csvQuoteChar != null && !csvQuoteChar.isEmpty()) {
                quoteChar = csvQuoteChar.charAt(0);
            }

            CSVReader reader = new CSVReader(new FileReader(file), seperator, quoteChar);

            List<String[]> importedLines = reader.readAll();

            request.getSession().setAttribute("importedLines", importedLines);

            dataImportModel.setTimeSeriesId(timeSeriesId);
            dataImportModel.setImportedLines(importedLines);

            ArrayList<Integer> ignoredLinesStub = new ArrayList<Integer>();

            for (String[] line : importedLines) {
                ignoredLinesStub.add(0);
            }

            dataImportModel.setIgnoredLines(ignoredLinesStub);

            TimeSeries timeSeries = timeSeriesDAO.getTimeSeriesForId(timeSeriesId);

            model.addAttribute("timeSeries", timeSeries);
            model.addAttribute("allImportableFields", timeSeriesDAO.getAllImportableFields());
            model.addAttribute("dataImportModel", dataImportModel);

        } catch (Exception e) {
            e.printStackTrace();
        }

        return "admin/timeseries/data_import";
    }

    @PostMapping("/timeseriesimportexecute")
    public String onSuccessFromImportForm(@Valid @ModelAttribute DataImportModel dataImportModel, BindingResult bindingResult, @RequestParam(value = "cers" , required = false) int[] cers, Model model, HttpServletRequest request)
    {
        StringBuilder importLog = new StringBuilder();

        Integer x = 0;

        TimeSeries timeSeries = timeSeriesDAO.getTimeSeriesForId(dataImportModel.getTimeSeriesId());

        dataImportModel.setImportedLines((List<String[]>) request.getSession().getAttribute("importedLines"));

        System.out.println(dataImportModel.getIgnoredLines().size());

        for (Integer i : dataImportModel.getIgnoredLines()) {
            System.out.println(i);
        }

        for (Integer i = 0; i < dataImportModel.getImportedLines().size(); i++) {
            if (!dataImportModel.getIgnoredLines().contains(i)) {
                Observation observation = new Observation();
                observation.setTimeSeriesId(timeSeries.getId().intValue());
                observation.setSourceId("csvimportdata");

                String[] line = dataImportModel.getImportedLines().get(x);

                Integer y =  0;
                Double ask = null;
                Double bid = null;
                String mainText = null;
                String scalar = null;
                Date pricingTime = null;

                for (ImportableField mappedField : dataImportModel.getMappedFields()) {
                    if (mappedField != null && mappedField.getId() != 0l) {
                        if (line.length > y && line[y] != null && !line[y].isEmpty()) {
                            //Ask
                            if (mappedField.getId() == 1l /*.getName().equalsIgnoreCase("ask")*/) {
                                ask = null;

                                try {
                                    ask = Double.parseDouble(line[y]);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }

                                if (ask != null) {
                                    observation.addComplexValueFragment("ask", ask.toString());
                                }
                            }

                            //Bid
                            if (mappedField.getId() == 2l /*.getName().equalsIgnoreCase("bid")*/) {
                                bid = null;

                                try {
                                    bid = Double.parseDouble(line[y]);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }

                                if (bid != null) {
                                    observation.addComplexValueFragment("bid", bid.toString());
                                }
                            }

                            //Main Text
                            if (mappedField.getId() == 5l /*.getName().equalsIgnoreCase("main text")*/) {
                                mainText = null;


                                mainText = line[y];

                                if (mainText != null) {
                                    observation.addComplexValueFragment("mainText", mainText);
                                }
                            }

                            //Scalar
                            if (mappedField.getId() == 4l /*.getName().equalsIgnoreCase("value")*/) {
                                scalar = line[y];

                                if (scalar != null) {
                                    observation.setScalarValue(scalar);
                                }
                            }

                            //Pricing Time
                            if (mappedField.getId() == 3l/*.getName().equalsIgnoreCase("observation time")*/) {
                                pricingTime = null;

                                if (dataImportModel.getDateFormat() == null || dataImportModel.getDateFormat().isEmpty()) {
                                    dataImportModel.setDateFormat("dd.MM.yyyy");
                                }

                                SimpleDateFormat sdf = new SimpleDateFormat(dataImportModel.getDateFormat());
                                sdf.setLenient(true);

                                try {
                                    pricingTime = sdf.parse(line[y]);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }

                                observation.setObservationTime(pricingTime);
                            }
                        }
                    }

                    y++;
                }

                System.out.println("Saving observation: " + observation.getScalarValue() + " Time: " + observation.getObservationTime());
                importLog.append("Saving observation: " + observation.getScalarValue() + " Time: " + observation.getObservationTime()).append('\r').append('\n');

                observationDAO.saveObservation(observation);
            }

            x++;
        }

        request.getSession().removeAttribute("importedLines");

        model.addAttribute("importLog", importLog.toString());
        model.addAttribute("timeSeries", timeSeries);
        model.addAttribute("allImportableFields", timeSeriesDAO.getAllImportableFields());
        model.addAttribute("dataImportModel", dataImportModel);

        return "admin/timeseries/data_import";
    }
}
