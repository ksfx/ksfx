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

package ch.ksfx.web.services.rest;

import ch.ksfx.dao.ebean.spidering.EbeanResultDAO;
import ch.ksfx.dao.ebean.spidering.EbeanSpideringConfigurationDAO;
import ch.ksfx.dao.spidering.ResultDAO;
import ch.ksfx.dao.spidering.SpideringConfigurationDAO;
import ch.ksfx.model.spidering.Result;
import ch.ksfx.model.spidering.SpideringConfiguration;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * Created by Kejo on 06.01.2015.
 *
 */
@Path("/spidering")
public class SpideringResultRS
{
    @GET
    @Path("/results.json")
    @Produces(MediaType.APPLICATION_JSON)
    public List<Result> getAllResultsJSON(@QueryParam("q") String q, @QueryParam("fromdate") String fromDateString, @QueryParam("todate") String toDateString, @QueryParam("startposition") Integer startPosition, @QueryParam("resultsize") Integer resultSize, @QueryParam("sourceid") Long sourceId)
    {
        ResultDAO resultDAO = new EbeanResultDAO();

        Date fromDate = parseRestDate(fromDateString);
        Date toDate = parseRestDate(toDateString);

        return resultDAO.searchResults(q, fromDate, toDate, startPosition, resultSize, sourceId);
    }

    @GET
    @Path("/results.xml")
    @Produces(MediaType.TEXT_XML)
    public List<Result> getAllResultsXML(@QueryParam("q") String q, @QueryParam("fromdate") String fromDateString, @QueryParam("todate") String toDateString, @QueryParam("startposition") Integer startPosition, @QueryParam("resultsize") Integer resultSize, @QueryParam("sourceid") Long sourceId)
    {
        ResultDAO resultDAO = new EbeanResultDAO();

        Date fromDate = parseRestDate(fromDateString);
        Date toDate = parseRestDate(toDateString);

        return resultDAO.searchResults(q, fromDate, toDate, startPosition, resultSize, sourceId);
    }

    @GET
    @Path("/sources.xml")
    @Produces(MediaType.TEXT_XML)
    public List<SpideringConfiguration> getAllSourcesXML()
    {
        SpideringConfigurationDAO spideringConfigurationDAO = new EbeanSpideringConfigurationDAO();

        return spideringConfigurationDAO.getAllSpideringConfigurations();
    }

    @GET
    @Path("/sources.json")
    @Produces(MediaType.APPLICATION_JSON)
    public List<SpideringConfiguration> getAllSourcesJSON()
    {
        SpideringConfigurationDAO spideringConfigurationDAO = new EbeanSpideringConfigurationDAO();

        return spideringConfigurationDAO.getAllSpideringConfigurations();
    }

    private Date parseRestDate(String restDate)
    {
        if (restDate == null) {
            return null;
        }

        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");

        try {
            return dateFormat.parse(restDate);
        } catch (Exception e) {
            throw new IllegalArgumentException("Unparseable date " + restDate + " Date must be yyyy-MM-ddTHH:mm:ss");
        }
    }
}
