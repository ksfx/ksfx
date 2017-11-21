/**
 *
 * Copyright (C) 2011-2017 KSFX. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package ch.ksfx.web.pages.admin.timeseries;

import au.com.bytecode.opencsv.CSVReader;
import ch.ksfx.dao.ObservationDAO;
import ch.ksfx.dao.TimeSeriesDAO;
import ch.ksfx.model.ImportableField;
import ch.ksfx.model.Observation;
import ch.ksfx.model.TimeSeries;
import ch.ksfx.util.GenericSelectModel;
import org.apache.tapestry5.PersistenceConstants;
import org.apache.tapestry5.annotations.Persist;
import org.apache.tapestry5.annotations.Property;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.ioc.services.PropertyAccess;
import org.apache.tapestry5.upload.services.UploadedFile;
import org.springframework.security.access.annotation.Secured;

import java.io.File;
import java.io.FileReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;


@Secured({"ROLE_ADMIN"})
public class ImportData
{
    private List<ImportableField> mappedFields = new ArrayList<ImportableField>();

    private List<Boolean> ignoredLines = new ArrayList<Boolean>();

    private Boolean ignoreLine;

    private ImportableField importableField;

    @Inject
    private TimeSeriesDAO timeSeriesDAO;

    @Inject
    private ObservationDAO observationDAO;

    @Inject
    private PropertyAccess propertyAccess;

    @Property
    private UploadedFile dataImportFile;

    @Property
    private TimeSeries timeSeries;

    @Property
    private GenericSelectModel<ImportableField> importableFields;

    @Property
    private String csvSeparator;

    @Property
    private String csvQuoteChar;

    @Property
    @Persist
    private String dateFormat;

    @Property
    private String[] currentRow;

    @Property
    @Persist
    private List<String[]> importedLines;

    @Property
    @Persist(PersistenceConstants.FLASH)
    private String importLog;

    @Secured({"ROLE_ADMIN"})
    public void onActivate(Long timeSeriesId)
    {
        if (importLog == null) {
            importLog = "";
        }

        ignoredLines = new ArrayList<Boolean>();
        mappedFields = new ArrayList<ImportableField>();

        timeSeries = timeSeriesDAO.getTimeSeriesForId(timeSeriesId);
        importableFields = new GenericSelectModel<ImportableField>(timeSeriesDAO.getAllImportableFields(), ImportableField.class, "name", "id", propertyAccess);
    }

    public Long onPassivate()
    {
        if (timeSeries != null) {
            return timeSeries.getId();
        }

        return null;
    }

    public ImportableField getImportableField()
    {
        return importableField;
    }

    public void setImportableField(ImportableField importableField)
    {
        mappedFields.add(importableField);
        this.importableField = importableField;
    }

    public Boolean getIgnoreLine()
    {
        return ignoreLine;
    }

    public void setIgnoreLine(Boolean ignoreLine)
    {
        System.out.println("Ignoring line: " + ignoreLine);
        ignoredLines.add(ignoreLine);
        this.ignoreLine = ignoreLine;
    }

    public void onSuccessFromUploadForm()
    {
        importedLines = null;
        ignoredLines = new ArrayList<Boolean>();

        try {
            File file = File.createTempFile("data","import");
            dataImportFile.write(file);

            char seperator = 0;
            char quoteChar = 0;

            if (csvSeparator != null) {
                seperator = csvSeparator.charAt(0);
            }

            if (csvQuoteChar != null) {
                quoteChar = csvQuoteChar.charAt(0);
            }

            if (csvSeparator.equals("\\t")) {
                seperator = '\t';
            }

            CSVReader reader = new CSVReader(new FileReader(file),seperator,quoteChar);
            importedLines = reader.readAll();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public List<String> getDropdowns()
    {
        List<String> dropdowns = new ArrayList<String>();

        for (Integer iI = 0; iI < getMaxRowLength(); iI++) {
            dropdowns.add("dd");
        }

        return dropdowns;
    }

    public Integer getMaxRowLength()
    {
        Integer max = 0;

        if (importedLines == null) {
            return 0;
        }

        for (String[] string : importedLines) {
            if (string.length > max) {
                max = string.length;
            }
        }

        return max;
    }

    public String getCurrentRowHtml()
    {
        String html = "";

        if (currentRow == null) {
            return "";
        }

        for (Integer iI = 0; iI < currentRow.length; iI++) {
            html += "<td>" + currentRow[iI] + "</td>";
        }

        return html;
    }

    public List<String[]> getDisplayLines()
    {
        return importedLines;
    }

    public void onSuccessFromImportForm()
    {
        Integer x = 0;

//        System.out.println("Ign lines 0" + ignoredLines);
//
//        ignoredLines = new ArrayList<Boolean>();
//
//        for (Integer z = 0; z < importedLines.size(); z++) {
//            ignoredLines.add(Boolean.FALSE);
//        }
//
//        System.out.println("Ign lines " + ignoredLines);

        for (Boolean b : ignoredLines) {
            if (!b) {
                Observation observation = new Observation();
                observation.setTimeSeriesId(timeSeries.getId().intValue());
                observation.setSourceId("csvimportdata");

                String[] line = importedLines.get(x);

                Integer y =  0;
                Double ask = null;
                Double bid = null;
                String mainText = null;
                String scalar = null;
                Date pricingTime = null;

                for (ImportableField mappedField : mappedFields) {
                    if (mappedField != null) {
                        if (line.length > y && line[y] != null && !line[y].isEmpty()) {
                            //Ask
                            if (mappedField.getName().equalsIgnoreCase("ask")) {
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
                            if (mappedField.getName().equalsIgnoreCase("bid")) {
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
                            if (mappedField.getName().equalsIgnoreCase("main text")) {
                                mainText = null;


                                mainText = line[y];

                                if (mainText != null) {
                                    observation.addComplexValueFragment("mainText", mainText);
                                }
                            }

                            //Scalar
                            if (mappedField.getName().equalsIgnoreCase("value")) {
                                scalar = line[y];

                                if (scalar != null) {
                                    observation.setScalarValue(scalar);
                                }
                            }

                            //Pricing Time
                            if (mappedField.getName().equalsIgnoreCase("observation time")) {
                                pricingTime = null;

                                if (dateFormat == null || dateFormat.isEmpty()) {
                                    dateFormat = "dd.MM.yyyy";
                                }

                                SimpleDateFormat sdf = new SimpleDateFormat(dateFormat);
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

                observationDAO.saveObservation(observation);
            }

            x++;
        }

        importedLines = null;
        ignoredLines = new ArrayList<Boolean>();
    }
}