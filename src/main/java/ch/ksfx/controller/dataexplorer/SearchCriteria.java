package ch.ksfx.controller.dataexplorer;

import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SearchCriteria implements Serializable
{
    private String allQuery;
    private String scalarValueQuery;
    private String seriesId;
    private Date dateFrom;
    private Date dateTo;
    private List<String> complexValueQueryKeys;
    private List<String> complexValueQueryValues;
    private List<String> metaDataQueryKeys;
    private List<String> metaDataQueryValues;

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

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    public Date getDateFrom()
    {
        return dateFrom;
    }

    public void setDateFrom(Date dateFrom)
    {
        this.dateFrom = dateFrom;
    }

    @DateTimeFormat(pattern = "yyyy-MM-dd")
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

    public Map<String, String> getComplexValueQuery()
    {
        Map<String, String> complexValueQuery = new HashMap<String, String>();

        if (complexValueQueryKeys != null) {
            for (Integer i = 0; i < complexValueQueryKeys.size(); i++) {
                complexValueQuery.put(complexValueQueryKeys.get(i), complexValueQueryValues.get(i));
            }
        }

        return complexValueQuery;
    }
    public List<String> getMetaDataQueryKeys()
    {
        return metaDataQueryKeys;
    }

    public void setMetaDataQueryKeys(List<String> metaDataQueryKeys)
    {
        this.metaDataQueryKeys = metaDataQueryKeys;
    }

    public List<String> getMetaDataQueryValues()
    {
        return metaDataQueryValues;
    }

    public void setMetaDataValueQueryValues(List<String> metaDataQueryValues)
    {
        this.metaDataQueryValues = metaDataQueryValues;
    }

    public Map<String, String> getMetaDataQuery()
    {
        Map<String, String> metaDataQuery = new HashMap<String, String>();

        if (metaDataQueryKeys != null) {
            for (Integer i = 0; i < metaDataQueryKeys.size(); i++) {
                metaDataQuery.put(metaDataQueryKeys.get(i), metaDataQueryValues.get(i));
            }
        }

        return metaDataQuery;
    }

    public boolean searchActive()
    {
        boolean searchActive = false;

        if (getAllQuery() != null && getAllQuery().length() > 0) {
            searchActive = true;
        }

        if (getScalarValueQuery() != null && getScalarValueQuery().length() > 0) {
            searchActive = true;
        }

        if (getSeriesId() != null && getSeriesId().length() > 0) {
            searchActive = true;
        }

        if (getDateFrom() != null) {
            searchActive = true;
        }

        if (getDateTo() != null) {
            searchActive = true;
        }

        if (getComplexValueQuery().size() > 0) {
            if (getComplexValueQueryKeys().get(0) != null && !getComplexValueQueryKeys().get(0).isEmpty()) {
                searchActive = true;
            }
        }

        if (getMetaDataQuery().size() > 0) {
            if (getMetaDataQueryKeys().get(0) != null && !getMetaDataQueryKeys().get(0).isEmpty()) {
                searchActive = true;
            }
        }

        return searchActive;
    }
}
