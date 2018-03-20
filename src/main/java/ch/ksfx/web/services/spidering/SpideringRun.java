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

import ch.ksfx.dao.ebean.spidering.EbeanResourceDAO;
import ch.ksfx.dao.ebean.spidering.EbeanResourceLoaderPluginDAO;
import ch.ksfx.dao.ebean.spidering.EbeanSpideringDAO;
import ch.ksfx.dao.spidering.ResourceDAO;
import ch.ksfx.dao.spidering.ResourceLoaderPluginDAO;
import ch.ksfx.dao.spidering.SpideringDAO;
import ch.ksfx.model.spidering.*;
import ch.ksfx.util.RunningSpideringCache;
import ch.ksfx.web.services.ObjectLocatorService;
import ch.ksfx.web.services.logger.SystemLogger;
import ch.ksfx.web.services.spidering.http.HttpUserAgentHeaders;
import ch.ksfx.web.services.spidering.http.WebEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class SpideringRun implements Runnable
{
    private SystemLogger systemLogger;
    private ParsingRunner parsingRunner;
    private boolean isRunning = true;
    private Spidering spidering;
    private Logger logger = LoggerFactory.getLogger(SpideringRun.class);
    private ResourceDAO resourceDAO = new EbeanResourceDAO();
    private SpideringDAO spideringDAO = new EbeanSpideringDAO();
    private ResourceLoaderPluginDAO resourceLoaderPluginDAO = null;
    private WebEngine webEngine = new WebEngine(HttpUserAgentHeaders.getDefaultHeaders());

    public SpideringRun(SystemLogger systemLogger, Spidering spidering, ParsingRunner parsingRunner, ObjectLocatorService objectLocatorService)
    {
        this.systemLogger = systemLogger;
        this.spidering = spidering;
        this.parsingRunner = parsingRunner;
        this.resourceLoaderPluginDAO = new EbeanResourceLoaderPluginDAO(systemLogger, objectLocatorService);
    }

    public void terminateSpidering()
    {
        isRunning = false;
    }

    @Override
    public void run()
    {
        try {
            if (spidering.getSpideringConfiguration().getResourceConfigurations() != null) {
                for (ResourceConfiguration resourceConfiguration : spidering.getSpideringConfiguration().getResourceConfigurations()) {

                    //List<Resource> resources = new ArrayList<Resource>();

                    if (resourceConfiguration.getDepth() == 0) {
                        boolean wrongFinder = false;

                        String url = "";

                        for (UrlFragment urlFragment : resourceConfiguration.getUrlFragments()) {
                            if (urlFragment.getUrlFragmentFinder().getName().equalsIgnoreCase("string")) {
                                url += urlFragment.getFragmentQuery();
                            }

                            if (urlFragment.getUrlFragmentFinder().getName().equalsIgnoreCase("xpath")) {
                                wrongFinder = true;
                            }

                            if (urlFragment.getUrlFragmentFinder().getName().equalsIgnoreCase("regex")) {
                                wrongFinder = true;
                            }

                            if (!isRunning) {
                                break;
                            }
                        }

                        if (wrongFinder) {
                            throw new IllegalArgumentException("Kann ja wohl nich regex oder xpath sein! Gibt ja garkeinen vorgänger!");
                        }

                        Resource resource = new Resource();
                        resource.setUrl(url);
                        resource.setPreviousResource(null); //Depth 0
                        resource.setDepth(0);
                        resource.setSpidering(spidering);

                        //resources.add(resource);

						if (resourceConfiguration.getResourceLoaderPluginConfiguration() != null) {
							ResourceLoaderPlugin rlp = resourceLoaderPluginDAO.getResourceLoaderPlugin(resourceConfiguration.getResourceLoaderPluginConfiguration());
							rlp.loadResource(resource);
						} else {
							webEngine.loadResource(resource);	
						}
								
                        resourceDAO.saveOrUpdate(resource);
                    } else {
                    	debugSpidering("Calculating resources");
                        
                        List<Resource> previousResources = resourceDAO.getResourcesForSpideringAndDepth(spidering, resourceConfiguration.getDepth() - 1);

                        for (Resource previousResource : previousResources) {
                            debugSpidering("Previous Resource " + previousResource.getUrl());
                        	
                            //Depth, Parts
                            Map<Integer, List<String>> urlParts = new HashMap<Integer, List<String>>();
                            Integer depth = 0;

                            for (UrlFragment urlFragment : resourceConfiguration.getUrlFragments()) {
                                if (!urlParts.containsKey(depth)) {
                                    urlParts.put(depth, new ArrayList<String>());
                                }

                                if (urlFragment.getUrlFragmentFinder().getName().equalsIgnoreCase("string")) {
                                    urlParts.get(depth).add(urlFragment.getFragmentQuery());
                                }

                                if (urlFragment.getUrlFragmentFinder().getName().equalsIgnoreCase("xpath")) {
                                    //TODO Implement Xpath
                                    throw new IllegalArgumentException("Not yet implemented!!!");
                                }

                                if (urlFragment.getUrlFragmentFinder().getName().equalsIgnoreCase("regex")) {
                                    Pattern pattern = PcreRegExSyntax.convertPcrePattern(urlFragment.getFragmentQuery());
                                    Matcher matcher = pattern.matcher(previousResource.getContent());

                                    while (matcher.find()) {
                                        urlParts.get(depth).add(matcher.group(matcher.groupCount()));
                                        
                                        debugSpidering("Regex URL finder: " + matcher.group(matcher.groupCount()));

                                        if (!isRunning) {
                                            break;
                                        }
                                    }
                                }

                                if (!isRunning) {
                                    break;
                                }

                                depth++;
                            }

                            assertUrlPartsValid(urlParts);

                            Integer calculatedSize = 0;

                            for (Integer i : urlParts.keySet()) {
                                if (urlParts.get(i).size() > calculatedSize) {
                                    calculatedSize = urlParts.get(i).size();
                                }

                                if (!isRunning) {
                                    break;
                                }
                            }

                            List<Integer> depths = new ArrayList(urlParts.keySet());
                            Collections.sort(depths);

                            for (Integer i = 0; i < calculatedSize; i++) {
                                String url = "";

                                for (Integer j : depths) {
                                    if (urlParts.get(j).size() == 1) {
                                        url += urlParts.get(j).get(0);
                                    } else {
                                        url += urlParts.get(j).get(i);
                                    }
                                }

                                if (!isRunning) {
                                    break;
                                }
                                
                                debugSpidering("---> SpideringRun loading url: " + url);

                                Resource resource = new Resource();
                                resource.setUrl(url);
                                resource.setSpidering(spidering);
                                resource.setPreviousResource(previousResource);
                                resource.setDepth(resourceConfiguration.getDepth());
                                resource.setPagingDepth(0);

                                //resources.add(resource);

								if (resourceConfiguration.getResourceLoaderPluginConfiguration() != null) {
									debugSpidering("---> SpideringRun loading url webbefore: " + url + " with: " + resourceConfiguration.getResourceLoaderPluginConfiguration().getName());
									ResourceLoaderPlugin rlp = resourceLoaderPluginDAO.getResourceLoaderPlugin(resourceConfiguration.getResourceLoaderPluginConfiguration());
									rlp.loadResource(resource);
									debugSpidering("---> SpideringRun loading url webafter: " + url + " with: " + resourceConfiguration.getResourceLoaderPluginConfiguration().getName());
								} else {
									debugSpidering("---> SpideringRun loading url webbefore: " + url);
									webEngine.loadResource(resource);	
									debugSpidering("---> SpideringRun loading url webafter: " + url);
								}

								debugSpidering("---> SpideringRun loading url savebefore: " + url);
                                resourceDAO.saveOrUpdate(resource);
								debugSpidering("---> SpideringRun loading url saveafter: " + url);
		
                                if (resourceConfiguration.getPaging()) {
                                	debugSpidering("---> SpideringRun loading url pagingbefore: " + url);
                                    retrievePagingResources(resourceConfiguration, resource);
                                }
                                
                                debugSpidering("---> SpideringRun loading url finished: " + url);
                                debugSpidering("----------------------------------------------------------------------------------------");
                                debugSpidering("----------------------------------------------------------------------------------------");
                            }
                        }
                    }
                }
            }

            spidering.setFinished(new Date());
            spideringDAO.saveOrUpdate(spidering);

            RunningSpideringCache.runningSpiderings.remove(spidering.getId());

            parsingRunner.runParsing(spidering);
        } catch (Throwable e) {
            systemLogger.logMessage("FATAL_SPIDERING", "Error while spidering", e);
            logger.error("Error while spidering",e);

            spidering.setFinished(new Date());
            spideringDAO.saveOrUpdate(spidering);

            RunningSpideringCache.runningSpiderings.remove(spidering.getId());
        }
    }
    
    private void debugSpidering(String debugMessage)
    {
    	if (spidering.getSpideringConfiguration().getDebugSpidering()) {
    		systemLogger.logMessage("DEBUG_SPIDERING", debugMessage);
    	}
    }

    private void retrievePagingResources(ResourceConfiguration resourceConfiguration, Resource startResource) {
        boolean hasMorePages = true;

        Resource previousResource = startResource;
        Integer pagingDepth = 1;

        while (hasMorePages) {
            Map<Integer, List<String>> urlParts = new HashMap<Integer, List<String>>();
            Integer depth = 0;

            for (PagingUrlFragment urlFragment : resourceConfiguration.getPagingUrlFragments()) {
                if (!urlParts.containsKey(depth)) {
                    urlParts.put(depth, new ArrayList<String>());
                }

                if (urlFragment.getUrlFragmentFinder().getName().equalsIgnoreCase("string")) {
                    urlParts.get(depth).add(urlFragment.getFragmentQuery());
                }

                if (urlFragment.getUrlFragmentFinder().getName().equalsIgnoreCase("xpath")) {
                    //TODO Implement Xpath
                    throw new IllegalArgumentException("Not yet implemented!!!");
                }

                if (urlFragment.getUrlFragmentFinder().getName().equalsIgnoreCase("regex")) {
                    Pattern pattern = PcreRegExSyntax.convertPcrePattern(urlFragment.getFragmentQuery());
                    Matcher matcher = pattern.matcher(previousResource.getContent());

                    while (matcher.find()) {
                        urlParts.get(depth).add(matcher.group(matcher.groupCount()));
                        debugSpidering("Regex URL finder (Paging resources): " + matcher.group(matcher.groupCount()));

                        if (!isRunning) {
                            break;
                        }
                    }
                }

                if (!isRunning) {
                    break;
                }

                depth++;
            }

            assertUrlPartsValid(urlParts);

            Integer calculatedSize = 0;

            for (Integer i : urlParts.keySet()) {
                if (urlParts.get(i).size() > calculatedSize) {
                    calculatedSize = urlParts.get(i).size();
                }

                if (!isRunning) {
                    break;
                }
            }

            if (calculatedSize == 0) {
                hasMorePages = false;
            }

            List<Integer> depths = new ArrayList(urlParts.keySet());
            Collections.sort(depths);

            debugSpidering("Paging URLs size " + calculatedSize);

            for (Integer i = 0; i < calculatedSize; i++) {
                String url = "";

                for (Integer j : depths) {
                    if (urlParts.get(j).size() == 1) {
                        url += urlParts.get(j).get(0);
                    } else {
                        url += urlParts.get(j).get(i);
                    }

                    if (!isRunning) {
                        break;
                    }
                }

                Resource resource = new Resource();
                resource.setUrl(url);
                resource.setSpidering(spidering);
                resource.setPreviousResource(startResource);
                resource.setDepth(resourceConfiguration.getDepth());
                resource.setPagingDepth(pagingDepth);

                if (resourceConfiguration.getPagingResourceLoaderPluginConfiguration() != null) {
                    ResourceLoaderPlugin rlp = resourceLoaderPluginDAO.getResourceLoaderPlugin(resourceConfiguration.getPagingResourceLoaderPluginConfiguration());
                    rlp.loadResource(resource);
                } else {
                    webEngine.loadResource(resource);
                }

                resourceDAO.saveOrUpdate(resource);

                previousResource = resource;

                if (!isRunning) {
                    break;
                }
            }

            pagingDepth++;
        }
    }

    private void assertUrlPartsValid(Map<Integer, List<String>> urlParts)
    {
        //Assert that every depth contains a fragment
        boolean hasInvalidPart = false;

        for (Integer i : urlParts.keySet()) {
            if (urlParts.get(i).size() == 0) {
                hasInvalidPart = true;
            }

            if (!isRunning) {
                break;
            }
        }

        //If there is an invalid part remove other parts
        if (hasInvalidPart) {
            for (Integer i : urlParts.keySet()) {
                urlParts.put(i,new ArrayList<String>());

                if (!isRunning) {
                    break;
                }
            }
        }
    }
}
