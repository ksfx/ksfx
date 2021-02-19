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

package ch.ksfx.dao.spidering;

import ch.ksfx.model.spidering.Result;
import ch.ksfx.model.spidering.ResultUnit;
import ch.ksfx.model.spidering.Spidering;
import ch.ksfx.model.spidering.SpideringConfiguration;
//import org.apache.tapestry5.grid.GridDataSource;

import java.util.Date;
import java.util.List;


public interface ResultDAO
{
    public void saveOrUpdate(Result result);
    public List<Result> getAllResults();
    public List<Result> getResultsForSpideringConfiguration(SpideringConfiguration spideringConfiguration);
	public List<Result> getResultsNotFoundSince(Date date);
	public List<Result> getResultsNotFoundSinceForSpideringConfiguration(Date date, SpideringConfiguration spideringConfiguration);
    public List<Result> getResultsForSpidering(Spidering spidering);
//    public GridDataSource getResultGridDataSourceForSpideringConfiguration(SpideringConfiguration spideringConfiguration, boolean filterInvalid);
    public Result getResultForId(Long resultId);
    public void deleteResult(Result result);
    public void deleteResultsForSpideringConfiguration(SpideringConfiguration spideringConfiguration);
    public Result getResultForResultHash(String resultHash);
    public Result getResultForSiteIdentifier(Long spideringConfigurationId, ResultUnit resultUnit);
    public Result getResultForSiteIdentifierHash(Long spideringConfigurationId, String siteIdentifierHash);
    public List<Result> searchResults(String query, Date fromDate, Date toDate, Integer startPosition, Integer resultSize, Long spideringConfigurationId);
}
