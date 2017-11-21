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

package ch.ksfx.dao.ebean.spidering;

import ch.ksfx.dao.spidering.ResultDAO;
import ch.ksfx.model.spidering.Result;
import ch.ksfx.model.spidering.ResultUnit;
import ch.ksfx.model.spidering.Spidering;
import ch.ksfx.model.spidering.SpideringConfiguration;
import ch.ksfx.util.EbeanGridDataSource;
import io.ebean.Ebean;
import io.ebean.ExpressionList;
import io.ebean.SqlUpdate;
import org.apache.tapestry5.grid.GridDataSource;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


public class EbeanResultDAO implements ResultDAO
{
    @Override
    public void saveOrUpdate(Result result)
    {
        if (result.getId() != null) {
            Ebean.update(result);
        } else {
            Ebean.save(result);
        }
    }

    @Override
    public List<Result> getAllResults()
    {
        return Ebean.find(Result.class).findList();
    }

    @Override
    public List<Result> getResultsForSpideringConfiguration(SpideringConfiguration spideringConfiguration)
    {
//        List<Long> spideringIds = new ArrayList<Long>();

//        for (Spidering spidering : spideringConfiguration.getSpiderings()) {
//            spideringIds.add(spidering.getId());
//        }

        return Ebean.find(Result.class).where().in("spideringConfiguration.id", spideringConfiguration.getId()).findList();
    }

    public List<Result> getResultsForSpidering(Spidering spidering)
    {
        return Ebean.find(Result.class).where().in("spidering.id", spidering.getId()).findList();
    }

    @Override
    public GridDataSource getResultGridDataSourceForSpideringConfiguration(SpideringConfiguration spideringConfiguration, boolean filterInvalid)
    {
        ExpressionList expressionList = Ebean.find(Result.class).where().eq("spideringConfiguration.id", spideringConfiguration.getId());
		
		if (filterInvalid) {
			expressionList = expressionList.eq("isValid", false);
		}
		
        return new EbeanGridDataSource(expressionList, Result.class);
    }

    @Override
    public Result getResultForId(Long resultId)
    {
        return Ebean.find(Result.class, resultId);
    }

    @Override
    public void deleteResult(Result result)
    {
    	//Not needed due to referencial integrity
        //for (ResultUnit resultUnit : result.getResultUnits()) {
        //    Ebean.delete(resultUnit);
        //}

        Ebean.delete(result);
    }
	
    @Override
    public List<Result> getResultsNotFoundSince(Date date)
    {
        return Ebean.find(Result.class).where().le("lastFound",date).findList();
    }
    
    @Override
	public List<Result> getResultsNotFoundSinceForSpideringConfiguration(Date date, SpideringConfiguration spideringConfiguration)
	{
		return Ebean.find(Result.class).where().le("lastFound",date).eq("spideringConfiguration.id", spideringConfiguration.getId()).findList();
	}

    @Override
    public void deleteResultsForSpideringConfiguration(SpideringConfiguration spideringConfiguration)
    {
        SqlUpdate update = Ebean.createSqlUpdate("DELETE FROM result WHERE spidering_configuration = :spideringConfiguration");
        update.setParameter("spideringConfiguration", spideringConfiguration.getId());
        update.execute();
    }

    @Override
    public Result getResultForResultHash(String resultHash)
    {
        return Ebean.find(Result.class).where().eq("resultHash", resultHash).findUnique();
    }

    @Override
    public Result getResultForSiteIdentifier(Long spideringConfigurationId, ResultUnit resultUnit)
    {
        if (resultUnit == null) {
            return null;
        }

        //FindList instead of findUnique to prevent Exceptions on wrong spidering configurations
        List<ResultUnit> resultUnitsExisting = Ebean.find(ResultUnit.class).where().eq("result.spideringConfiguration.id",spideringConfigurationId).eq("value",resultUnit.getValue()).eq("resultUnitType.id", resultUnit.getResultUnitType().getId()).findList();

        if (resultUnitsExisting == null || resultUnitsExisting.size() == 0) {
            return null;
        }

        return resultUnitsExisting.get(0).getResult();
    }
    
    @Override
    public Result getResultForSiteIdentifierHash(Long spideringConfigurationId, String siteIdentifierHash)
    {
    	List<Result> results = Ebean.find(Result.class).where().eq("spideringConfiguration.id",spideringConfigurationId).eq("siteIdentifierHash",siteIdentifierHash).findList();	
    
    	if (results == null || results.size() == 0) {
            return null;
        }
        
        return results.get(0);
    }

    @Override
    public List<Result> searchResults(String query, Date fromDate, Date toDate, Integer startPosition, Integer resultSize, Long spideringConfigurationId)
    {
        ExpressionList<Result> expressionList = Ebean.find(Result.class).where();

        if (query != null) {
            List<ResultUnit> resultUnits = Ebean.find(ResultUnit.class).where().like("value","%" + query + "%").findList();

            Set<Long> resultIds = new HashSet<Long>();

            for (ResultUnit resultUnit : resultUnits) {
                System.out.println("Result " + resultUnit.getResult().getId());
                resultIds.add(resultUnit.getResult().getId());
            }

            expressionList.in("id",resultIds);
        }

        if (fromDate != null) {
            expressionList.ge("lastFound", fromDate);
        }

        if (toDate != null) {
            expressionList.le("lastFound", toDate);
        }

        if (spideringConfigurationId != null) {
            expressionList.eq("spideringConfiguration.id", spideringConfigurationId);
        }

        expressionList.eq("isValid", true);

        if (startPosition != null) {
            expressionList.setFirstRow(startPosition);
        }

        if (resultSize != null) {
            expressionList.setMaxRows(resultSize);
        } else {
            expressionList.setMaxRows(100);
        }

        expressionList.order().desc("firstFound");


        return expressionList.findList();
    }
}
