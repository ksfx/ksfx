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

package ch.ksfx.web.services.spidering;


import ch.ksfx.dao.activity.ActivityInstanceDAO;
import ch.ksfx.dao.ebean.activity.EbeanActivityInstanceDAO;
import ch.ksfx.dao.ebean.spidering.*;
import ch.ksfx.dao.spidering.*;
import ch.ksfx.model.activity.ActivityInstance;
import ch.ksfx.model.activity.ActivityInstanceParameter;
import ch.ksfx.model.spidering.*;
import ch.ksfx.util.MD5Helper;
import ch.ksfx.util.RunningSpideringCache;
import ch.ksfx.web.services.ObjectLocatorService;
import ch.ksfx.web.services.activity.ActivityInstanceRunner;
import ch.ksfx.web.services.logger.SystemLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class ParsingRun implements Runnable
{
    private SystemLogger systemLogger;
    private ObjectLocatorService objectLocatorService;
    private boolean isRunning = true;
    private Spidering spidering;
    private Logger logger = LoggerFactory.getLogger(ParsingRun.class);
    private Map<Integer,ResourceConfiguration> depthResourceConfiguration = new HashMap<Integer, ResourceConfiguration>();
    private ResultUnitModifierDAO resultUnitModifierDAO = null;

    public ParsingRun(SystemLogger systemLogger, Spidering spidering, ObjectLocatorService objectLocatorService)
    {
        this.systemLogger = systemLogger;
        this.spidering = spidering;
        this.objectLocatorService = objectLocatorService;
        this.resultUnitModifierDAO = new EbeanResultUnitModifierDAO(systemLogger);
    }

    public void terminateParsing()
    {
        isRunning = false;
    }

    @Override
    public void run()
    {
        ActivityInstanceDAO activityInstanceDAO = new EbeanActivityInstanceDAO();
        ResourceDAO resourceDAO = new EbeanResourceDAO();
        SpideringDAO spideringDAO = new EbeanSpideringDAO();
        ResultDAO resultDAO = new EbeanResultDAO();
        ResultUnitDAO resultUnitDAO = new EbeanResultUnitDAO();
        ResultVerifierDAO resultVerifierDAO = new EbeanResultVerifierDAO(objectLocatorService, systemLogger);

        try {
            ResultVerifier resultVerifier = null;

            if (spidering.getSpideringConfiguration().getResultVerifierConfiguration() != null) {
                resultVerifier = resultVerifierDAO.getResultVerifier(spidering.getSpideringConfiguration().getResultVerifierConfiguration());
            }

            List<ResultUnitType> notUpdateableResultUnitTypes = spidering.getSpideringConfiguration().getNotUpdateableResultUnitTypes();

            Integer validResultCount = 0;
            Integer invalidResultCount = 0;
            Integer newResultCount = 0;
            Integer updatedResultCount = 0;
            Integer unchangedResultCount = 0;

            //Begin Creepy Stuff...
            //Map<Resource, List<ResultUnit>> resultsPerResource = new HashMap<Resource, List<ResultUnit>>();

            if (spidering.getSpideringConfiguration().getResourceConfigurations() != null) {
                /*
                for (ResourceConfiguration resourceConfiguration : spidering.getSpideringConfiguration().getResourceConfigurations()) {

                    List<Resource> resources = resourceDAO.getResourcesForSpideringAndDepth(spidering, resourceConfiguration.getDepth());

                    for (Resource resource : resources) {
                        //TODO check http status codes
                        if (resource.getContent() == null) {
                            continue;
                        }

                        for (ResultUnitConfiguration resultUnitConfiguration : resourceConfiguration.getResultUnitConfigurations()) {

                            if (resultUnitConfiguration.getResultUnitFinder().getName().equalsIgnoreCase("xpath")) {
                                //TODO Implement Xpath
                                throw new IllegalArgumentException("Not yet implemented!!!");
                            }

                            if (resultUnitConfiguration.getResultUnitFinder().getName().equalsIgnoreCase("regex")) {
                                Pattern pattern = PcreRegExSyntax.convertPcrePattern(resultUnitConfiguration.getFinderQuery());
                                Matcher matcher = pattern.matcher(resource.getContent());

                                List<ResultUnitModifier> resultUnitModifiers = new ArrayList<ResultUnitModifier>();

                                if (resultUnitConfiguration.getResultUnitConfigurationModifiers() != null) {
                                    for (ResultUnitConfigurationModifiers rucm : resultUnitConfiguration.getResultUnitConfigurationModifiers()) {
                                        if (rucm.getResultUnitConfiguration() != null) {
                                            resultUnitModifiers.add(resultUnitModifierDAO.getResultUnitModifier(rucm.getResultUnitModifierConfiguration()));
                                        }
                                    }
                                }

                                while (matcher.find()) {

                                    if (!isRunning) {
                                        break;
                                    }

                                    if (!resultsPerResource.containsKey(resource)) {
                                        resultsPerResource.put(resource, new ArrayList<ResultUnit>());
                                    }

                                    ResultUnit resultUnit = new ResultUnit();
                                    resultUnit.setResultUnitType(resultUnitConfiguration.getResultUnitType());
                                    resultUnit.setValue(matcher.group(matcher.groupCount()));
                                    resultUnit.setSiteIdentifier(resultUnitConfiguration.getSiteIdentifier());

                                    for (ResultUnitModifier rum : resultUnitModifiers) {
                                        System.out.println("RUM " + rum);
                                        resultUnit = rum.modify(resultUnit);
                                    }

                                    resultsPerResource.get(resource).add(resultUnit);
                                    System.out.println("Regex RESULT finder: " + matcher.group(matcher.groupCount()));
                                }
                            }

                            if (!isRunning) {
                                break;
                            }
                        }

                        if (!isRunning) {
                            break;
                        }
                    }
                }
                */


                //=====================================================================================
                //=====================================================================================
                //=====================================================================================
                //=========================START BASELEVEL STUFF ======================================

                Integer baseLevelDepth = null;

				//Find the Baselevel depth. Every Baselevel Resource will create a new Result!
                for (ResourceConfiguration resourceConfiguration : spidering.getSpideringConfiguration().getResourceConfigurations()) {
                    depthResourceConfiguration.put(resourceConfiguration.getDepth(), resourceConfiguration);
                    if (resourceConfiguration.getBaseLevel()) {
                        baseLevelDepth = resourceConfiguration.getDepth();
                    }
                }

                if (baseLevelDepth == null) {
                    throw new IllegalArgumentException("Cannot find base level depth, maybe the base level is not defined...");
                }

                //TODO, das ist unsinn, die baseLevelResources hätten oben gesammelt werden können!!
                List baseLevelResourceIds = resourceDAO.getResourcesForSpideringAndDepthIds(spidering, baseLevelDepth);
                System.out.println("Found " + baseLevelResourceIds.size() + " base level resources");

                //QueryIterator<Resource> baseLevelResources = resourceDAO.getResourcesForSpideringAndDepthIterator(spidering, baseLevelDepth);

                //while (baseLevelResources.hasNext()) {
                for (Object id : baseLevelResourceIds) {
                    Resource baseLevelResource = resourceDAO.getResourceForId((Long) id);
                    //Resource baseLevelResource = baseLevelResources.next();
                    List<ResultUnit> resultUnits = new ArrayList<ResultUnit>();
                    Result result = new Result();
                    result.setSpidering(spidering);
                    result.setBaseLevelResource(baseLevelResource);
                    result.setFirstFound(new Date());
                    result.setLastFound(new Date());
                    result.setUriString(baseLevelResource.getUrl());

                    if (!isRunning) {
                        break;
                    }

					//Collect all results which are ON AND ABOVE the Baselevel resource
                    Resource resourceInPath = baseLevelResource;

                    while (resourceInPath != null) {
                        resultUnits.addAll(getResultUnitsForResource(resourceInPath));

                        if (!isRunning) {
                            break;
                        }

                        resourceInPath = resourceInPath.getPreviousResource();
                    }

					//Collect all results which are BELOW the Baselevel resource
                    //TODO very heavy weight operation, resources should be gathered above
                    List<Resource> resourcesAfterBaseLevel = resourceDAO.getChildResources(baseLevelResource);

                    for (Resource resourceAfterBaseLevel : resourcesAfterBaseLevel) {
                        resultUnits.addAll(getResultUnitsForResource(resourceAfterBaseLevel));

                        if (!isRunning) {
                            break;
                        }
                    }

					//Should be null here anyway. Not sure why there is this null check
                    if (result.getResultUnits() == null) {
                        result.setResultUnits(new ArrayList<ResultUnit>());
                    }

					//Add all collected ResultUnits to the result
                    for (ResultUnit resultUnit : resultUnits) {
                        result.getResultUnits().add(resultUnit);
                    }
                    
                    //All ResultUnits are added, now the SiteIdentifier can easily be identified
                    if (result.getSiteIdentifier() != null) {
                    	result.setSiteIdentifierHash(MD5Helper.md5HexHash(result.getSiteIdentifier().getValue()));
                    }

					/* *
					 * TODO This check is too heavy, because the ResultUnit value cannot be indexed and the search is too slow!
					 * This will be replaced with the PrimitiveSiteIdentifier
					 */
                    //Result existingResult = resultDAO.getResultForSiteIdentifier(spidering.getSpideringConfiguration().getId(), result.getSiteIdentifier());
					//This is the replacement
					
					Result existingResult = null;
					
                    if (result.getSiteIdentifier() != null) {
                        existingResult = resultDAO.getResultForSiteIdentifierHash(spidering.getSpideringConfiguration().getId(), MD5Helper.md5HexHash(result.getSiteIdentifier().getValue()));
                    }

                    if (existingResult == null && spidering.getSpideringConfiguration().getCheckDuplicatesGlobally()) {
                        //TODO Wenn der Hash null ist muss behandelt werden
                        existingResult = resultDAO.getResultForResultHash(result.calculateResultHash());
                    } else {
                        String siteIdentifier = "";

                        ResultUnit resultUnit = result.getSiteIdentifier();

                        if (resultUnit != null) {
                            siteIdentifier = resultUnit.getValue();
                        }

                        logger.info("Found EXISTING result for site identifier " + siteIdentifier);
                    }

                    if (existingResult != null) {
                        logger.info("Found existing result: " + existingResult.getResultHash());

                        existingResult.setLastFound(new Date());

                        if (result.calculateResultHash().equals(existingResult.getResultHash())) {
                            resultDAO.saveOrUpdate(existingResult);
                            unchangedResultCount++;
                            continue;
                        }

                        //If we are here we can be sure that the result has been updated on the site
                        updatedResultCount++;

                        existingResult.setUpdated(true);
                        existingResult.setBaseLevelResource(baseLevelResource);
                        existingResult.setSpideringConfiguration(spidering.getSpideringConfiguration());
                        existingResult.setSpidering(spidering);

                        for (ResultUnit resultUnit : existingResult.getResultUnits()) {
                            if (!notUpdateableResultUnitTypes.contains(resultUnit.getResultUnitType())) {
                                resultUnitDAO.deleteResultUnit(resultUnit);
                            }
                        }

                        for (ResultUnit resultUnit : result.getResultUnits()) {
                            if (!notUpdateableResultUnitTypes.contains(resultUnit.getResultUnitType())) {
                                resultUnit.setResult(existingResult);
                                resultUnitDAO.saveOrUpdate(resultUnit);
                            }
                        }

                        existingResult.getResultUnits().clear();
                        existingResult.getResultUnits().addAll(result.getResultUnits());

                        if (resultVerifier == null || resultVerifier.isResultValid(existingResult)) {
                            existingResult.setIsValid(true);
                            resultDAO.saveOrUpdate(existingResult);

                            validResultCount++;
                        } else {
                            existingResult.setIsValid(false);

                            resultDAO.saveOrUpdate(existingResult);
                            invalidResultCount++;
                        }
                    } else {
                        result.setResultHash(result.calculateResultHash());
                        result.setSpideringConfiguration(spidering.getSpideringConfiguration());
                        newResultCount++;

                        if (resultVerifier == null || resultVerifier.isResultValid(result)) {
                            result.setIsValid(true);
                            resultDAO.saveOrUpdate(result);

                            for (ResultUnit resultUnit : result.getResultUnits()) {
                                System.out.println(resultUnit.getResultUnitType().getName());
                                resultUnit.setResult(result);
                                resultUnitDAO.saveOrUpdate(resultUnit);
                            }

                            validResultCount++;
                        } else {
                            //TODO duplicated code
                            result.setIsValid(false);
                            resultDAO.saveOrUpdate(result);

                            for (ResultUnit resultUnit : result.getResultUnits()) {
                                resultUnit.setResult(result);
                                resultUnitDAO.saveOrUpdate(resultUnit);
                            }

                            invalidResultCount++;
                        }
                    }
                }
            }

            spidering.setNewResultCount(newResultCount);
            spidering.setUpdatedResultCount(updatedResultCount);
            spidering.setUnchangedResultCount(unchangedResultCount);
            spidering.setValidResultCount(validResultCount);
            spidering.setInvalidResultCount(invalidResultCount);
            spidering.setResultCount(validResultCount + invalidResultCount + unchangedResultCount);

            spideringDAO.saveOrUpdate(spidering);

            RunningSpideringCache.runningParsings.remove(spidering.getId());

            ActivityInstanceRunner activityInstanceRunner = objectLocatorService.getObjectLocator().getService(ActivityInstanceRunner.class);

            if (spidering.getSpideringConfiguration().getSpideringPostActivities() != null) {
                for (SpideringPostActivity spideringPostActivity : spidering.getSpideringConfiguration().getSpideringPostActivities()) {
                    ActivityInstance activityInstance = new ActivityInstance();
                    System.out.println("Activity: " + spideringPostActivity.getActivity());
                    activityInstance.setActivity(spideringPostActivity.getActivity());
                    activityInstance.setStarted(new Date());

                    activityInstanceDAO.saveOrUpdateActivityInstance(activityInstance);

                    ActivityInstanceParameter activityInstanceParameter = new ActivityInstanceParameter("spidering", spidering.getId().toString());
                    activityInstanceParameter.setActivityInstance(activityInstance);

                    activityInstanceDAO.saveOrUpdateActivityInstanceParameter(activityInstanceParameter);

                    activityInstanceRunner.runActivity(activityInstanceDAO.getActivityInstanceForId(activityInstance.getId()));
                }
            }
        } catch (Throwable e) {
            e.printStackTrace();
            systemLogger.logMessage("FATAL_PARSING", "Error while parsing", e);
            logger.error("Error while parsing",e);

            spidering.setValidResultCount(0);
            spidering.setResultCount(0);

            spideringDAO.saveOrUpdate(spidering);

            RunningSpideringCache.runningParsings.remove(spidering.getId());
        }
    }

    public List<ResultUnit> getResultUnitsForResource(Resource resource)
    {
        List<ResultUnit> resultUnits = new ArrayList<ResultUnit>();

        //TODO check http status codes
        if (resource.getContent() == null) {
            return resultUnits;
        }

        for (ResultUnitConfiguration resultUnitConfiguration : depthResourceConfiguration.get(resource.getDepth()).getResultUnitConfigurations()) {

            if (resultUnitConfiguration.getResultUnitFinder().getName().equalsIgnoreCase("xpath")) {
                //TODO Implement Xpath
                throw new IllegalArgumentException("Not yet implemented!!!");
            }

            if (resultUnitConfiguration.getResultUnitFinder().getName().equalsIgnoreCase("regex")) {
                Pattern pattern = PcreRegExSyntax.convertPcrePattern(resultUnitConfiguration.getFinderQuery());
                Matcher matcher = pattern.matcher(resource.getContent());

                List<ResultUnitModifier> resultUnitModifiers = new ArrayList<ResultUnitModifier>();

                if (resultUnitConfiguration.getResultUnitConfigurationModifiers() != null) {
                    for (ResultUnitConfigurationModifiers rucm : resultUnitConfiguration.getResultUnitConfigurationModifiers()) {
                        if (rucm.getResultUnitConfiguration() != null) {
                            resultUnitModifiers.add(resultUnitModifierDAO.getResultUnitModifier(rucm.getResultUnitModifierConfiguration()));
                        }
                    }
                }

                while (matcher.find()) {

                    if (!isRunning) {
                        break;
                    }

                    ResultUnit resultUnit = new ResultUnit();
                    resultUnit.setResultUnitType(resultUnitConfiguration.getResultUnitType());
                    resultUnit.setValue(matcher.group(matcher.groupCount()));
                    resultUnit.setSiteIdentifier(resultUnitConfiguration.getSiteIdentifier());

                    for (ResultUnitModifier rum : resultUnitModifiers) {
                        resultUnit = rum.modify(resultUnit);
                    }

                    resultUnits.add(resultUnit);
                }
            }

            if (!isRunning) {
                break;
            }
        }

        return resultUnits;
    }
}
