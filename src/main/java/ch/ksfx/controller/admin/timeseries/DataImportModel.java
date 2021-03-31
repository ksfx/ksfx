package ch.ksfx.controller.admin.timeseries;

import ch.ksfx.model.ImportableField;

import java.util.ArrayList;
import java.util.List;

public class DataImportModel
{
    private Long timeSeriesId;
    private String dateFormat;
    private List<Integer> ignoredLines = new ArrayList<Integer>();
    private List<String[]> importedLines;
    private List<ImportableField> mappedFields = new ArrayList<ImportableField>();

    public Long getTimeSeriesId()
    {
        return timeSeriesId;
    }

    public void setTimeSeriesId(Long timeSeriesId)
    {
        this.timeSeriesId = timeSeriesId;
    }

    public String getDateFormat()
    {
        return dateFormat;
    }

    public void setDateFormat(String dateFormat)
    {
        this.dateFormat = dateFormat;
    }

    public List<Integer> getIgnoredLines()
    {
        return ignoredLines;
    }

    public void setIgnoredLines(List<Integer> ignoredLines)
    {
        this.ignoredLines = ignoredLines;
    }

    public List<String[]> getImportedLines()
    {
        return importedLines;
    }

    public void setImportedLines(List<String[]> importedLines)
    {
        this.importedLines = importedLines;
    }

    public List<ImportableField> getMappedFields()
    {
        return mappedFields;
    }

    public void setMappedFields(List<ImportableField> mappedFields)
    {
        this.mappedFields = mappedFields;
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

    public String getHtmlForImportedLine(String[] importedLine)
    {
        String html = "";

        if (importedLine == null) {
            return "";
        }

        for (Integer iI = 0; iI < importedLine.length; iI++) {
            html += "<td>" + importedLine[iI] + "</td>";
        }

        return html;
    }
}
