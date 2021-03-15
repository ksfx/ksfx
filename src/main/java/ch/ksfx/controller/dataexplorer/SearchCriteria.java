package ch.ksfx.controller.dataexplorer;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

public class SearchCriteria implements Serializable
{
    private String allQuery;
    private String scalarValueQuery;
    private String seriesId;
    private Date dateFrom;
    private Date dateTo;
    private List<String> complexValueQueryKeys;
    private List<String> complexValueQueryValues;

    public String getAllQuery()
    {
        return allQuery;
    }

    public void setAllQuery(String allQuery)
    {
        this.allQuery = allQuery;
    }

    public String getScalarValueQuery()
    {
        return scalarValueQuery;
    }

    public void setScalarValueQuery(String scalarValueQuery)
    {
        this.scalarValueQuery = scalarValueQuery;
    }

    public String getSeriesId()
    {
        return seriesId;
    }

    public void setSeriesId(String seriesId)
    {
        this.seriesId = seriesId;
    }

    public Date getDateFrom()
    {
        return dateFrom;
    }

    public void setDateFrom(Date dateFrom)
    {
        this.dateFrom = dateFrom;
    }

    public Date getDateTo()
    {
        return dateTo;
    }

    public void setDateTo(Date dateTo)
    {
        this.dateTo = dateTo;
    }

    public List<String> getComplexValueQueryKeys()
    {
        return complexValueQueryKeys;
    }

    public void setComplexValueQueryKeys(List<String> complexValueQueryKeys)
    {
        this.complexValueQueryKeys = complexValueQueryKeys;
    }

    public List<String> getComplexValueQueryValues()
    {
        return complexValueQueryValues;
    }

    public void setComplexValueQueryValues(List<String> complexValueQueryValues)
    {
        this.complexValueQueryValues = complexValueQueryValues;
    }
}
