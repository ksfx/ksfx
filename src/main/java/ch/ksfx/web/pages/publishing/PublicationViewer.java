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

package ch.ksfx.web.pages.publishing;

import ch.ksfx.dao.PublishingConfigurationDAO;
import ch.ksfx.dao.publishing.PublishingResourceDAO;
import ch.ksfx.model.PublishingConfiguration;
import ch.ksfx.model.PublishingStrategy;
import ch.ksfx.model.publishing.PublishingConfigurationCacheData;
import ch.ksfx.model.publishing.PublishingResource;
import ch.ksfx.model.publishing.PublishingResourceCacheData;
import ch.ksfx.model.publishing.PublishingVisibility;
import ch.ksfx.util.Console;
import ch.ksfx.util.GenericStreamResponse;
import ch.ksfx.util.PublishingDataShare;
import ch.ksfx.web.services.ObjectLocatorService;
import ch.ksfx.web.services.logger.SystemLogger;
import ch.ksfx.web.services.users.UserService;
import groovy.lang.GroovyClassLoader;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.tapestry5.EventContext;
import org.apache.tapestry5.PersistenceConstants;
import org.apache.tapestry5.StreamResponse;
import org.apache.tapestry5.annotations.Persist;
import org.apache.tapestry5.annotations.Property;
import org.apache.tapestry5.ioc.ObjectLocator;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.util.TextStreamResponse;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;


public class PublicationViewer
{
    @Inject
    private PublishingConfigurationDAO publishingConfigurationDAO;
	
	@Inject
	private PublishingResourceDAO publishingResourceDAO;

    @Inject
    private ObjectLocatorService objectLocatorService;

    @Inject
    private SystemLogger systemLogger;
	
	@Property
	@Persist(PersistenceConstants.FLASH)
	private String layoutEmbeddedData;
	
	@Property
	@Persist(PersistenceConstants.FLASH)
	private String pageHeaderTitle;

    @Inject
    private UserService userService;
	
	public Object onActivate(EventContext eventContext)
	{
		Integer fromCache = 0;
		String stringContext = "";
		List<String> uriParameters = new ArrayList<String>();
		
		//For Fallback Reasons
		if (eventContext.getCount() == 1) {
			stringContext = eventContext.get(String.class, 0); 
		} else {
			fromCache = eventContext.get(Integer.class, 0); 
			stringContext = eventContext.get(String.class, 1); 
		}
		
		if (eventContext.getCount() > 2) {
			for (Integer i = 2; i < eventContext.getCount(); i++) {
				uriParameters.add(eventContext.get(String.class, i));
			}
		}
		
		if (StringUtils.isNumeric(stringContext)) {
			return activatePublishingConfigurationForId(Long.parseLong(stringContext), fromCache, uriParameters);
		} else {
			return activatePublishingConfigurationOrPublishingResourceForUri(stringContext, fromCache, uriParameters);
		}
	}

	public Object activatePublishingConfigurationOrPublishingResourceForUri(String uri, Integer fromCache, List<String> uriParameters)
	{
		if (!uri.contains("-")) {
			PublishingConfiguration publishingConfiguration = publishingConfigurationDAO.getPublishingConfigurationForUri(uri);
			return activatePublishingConfigurationForId(publishingConfiguration.getId(), fromCache, uriParameters);
		} else {
			String[] parts = StringUtils.split(uri, "-");
			return activatePublishingResourceForConfigurationLocatorAndResourceLocator(parts[0], parts[1], fromCache, uriParameters);
		}
	}
	
    public Object activatePublishingConfigurationForId(Long publishingConfigurationId, Integer fromCache, List<String> uriParameters)
    {
		PublishingConfiguration publishingConfiguration = publishingConfigurationDAO.getPublishingConfigurationForId(publishingConfigurationId);

        if (publishingConfiguration.getPublishingVisibility() == null || !publishingConfiguration.getPublishingVisibility().equals(PublishingVisibility.PUBLIC.toString())) {
            if (!userService.isAuthenticated() && (publishingConfiguration.getPublishingVisibility() == null || !publishingConfiguration.getPublishingVisibility().equals(PublishingVisibility.CACHE_FOR_ALL.toString()) || fromCache == 0)) {
                return new TextStreamResponse("text/plain", "You are not authorized to view this page");
            }
        }

		pageHeaderTitle = publishingConfiguration.getName();
		
		if (fromCache == 1) {
			PublishingConfigurationCacheData pccd = publishingConfiguration.getPublishingConfigurationCacheDataForUriParameter(uriParameters.toString());
			
			if (pccd.getContentType().contains("text") && publishingConfiguration.getEmbedInLayout()) {
				layoutEmbeddedData = new String(pccd.getCacheData());
				return null;
			}
			
			return new GenericStreamResponse(pccd.getCacheData(), publishingConfiguration.getUri(), pccd.getContentType(), false);
		}
		
		//purgeAllCaches(publishingConfiguration);
		
        try {
			Console.startConsole(publishingConfiguration);
			PublishingDataShare.startShare(publishingConfiguration);
			
			StreamResponse streamResponse = loadPublishingStrategy(publishingConfiguration.getPublishingStrategy(), uriParameters);
			InputStream inputStream = streamResponse.getStream();
			
			if (inputStream instanceof ByteArrayInputStream) {
				systemLogger.logMessage("PUBLICATION", "Data can be cached: " + publishingConfiguration.getName());
				
				ByteArrayInputStream byteArrayInputStream = (ByteArrayInputStream) inputStream;
				byte[] cacheData = IOUtils.toByteArray(byteArrayInputStream);
				String contentType = streamResponse.getContentType();
				
				publishingConfiguration = publishingConfigurationDAO.getPublishingConfigurationForId(publishingConfigurationId);
				
				if (!publishingConfiguration.getLockedForCacheUpdate()) {
					PublishingConfigurationCacheData pccd = publishingConfigurationDAO.getPublishingConfigurationCacheDataForPublishingConfigurationAndUriParameter(publishingConfiguration, uriParameters.toString());

					if (pccd == null) {
						pccd = new PublishingConfigurationCacheData();
						pccd.setPublishingConfiguration(publishingConfiguration);
						pccd.setUriParameter(uriParameters.toString());
					}

					pccd.setCacheData(cacheData);
					pccd.setContentType(contentType);
					publishingConfigurationDAO.saveOrUpdatePublishingConfigurationCacheData(pccd);
				}
				
				byteArrayInputStream.reset();
				
				if (contentType.contains("text") && publishingConfiguration.getEmbedInLayout()) {
					layoutEmbeddedData = new String(cacheData);
					return null;
				}
			} else {
				systemLogger.logMessage("PUBLICATION", "Data can NOT be cached: " + publishingConfiguration.getName());
			}

			return streamResponse;
        } catch (Exception e) {
            systemLogger.logMessage("PUBLICATION","Error while creating publication PublicationViewer",e);
            throw new RuntimeException(e);
        } finally {
        	Console.endConsole();
			PublishingDataShare.endShare();
        }
    }
	
	public Object activatePublishingResourceForConfigurationLocatorAndResourceLocator(String configurationLocator, String resourceLocator, Integer fromCache, List<String> uriParameters)
	{
		PublishingConfiguration publishingConfiguration = publishingConfigurationDAO.getPublishingConfigurationForUri(configurationLocator);

        if (publishingConfiguration.getPublishingVisibility() == null || !publishingConfiguration.getPublishingVisibility().equals(PublishingVisibility.PUBLIC.toString())) {
            if (!userService.isAuthenticated() && (publishingConfiguration.getPublishingVisibility() == null || !publishingConfiguration.getPublishingVisibility().equals(PublishingVisibility.CACHE_FOR_ALL.toString()) || fromCache == 0)) {
                return new TextStreamResponse("text/plain", "You are not authorized to view this page");
            }
        }

        pageHeaderTitle = publishingConfiguration.getName();

		PublishingResource publishingResource = publishingResourceDAO.getPublishingResourceForPublishingConfigurationAndUri(publishingConfiguration, resourceLocator);
	
		if (fromCache == 1) {
            PublishingResourceCacheData prcd = publishingResource.getPublishingResourceCacheDataForUriParameter(uriParameters.toString());

            if (prcd.getContentType().contains("text") && publishingResource.getEmbedInLayout()) {
                layoutEmbeddedData = new String(prcd.getCacheData());
                return null;
            }

			return new GenericStreamResponse(prcd.getCacheData(), publishingResource.getUri(), prcd.getContentType(), false);
		}
	
		try {
			Console.startConsole(publishingConfiguration);
			PublishingDataShare.startShare(publishingConfiguration);

			StreamResponse streamResponse = loadPublishingStrategy(publishingResource.getPublishingStrategy(), uriParameters);
			InputStream inputStream = streamResponse.getStream();
			
			if (inputStream instanceof ByteArrayInputStream) {
				systemLogger.logMessage("PUBLICATION", "Data can be cached: " + publishingResource.getTitle());
				
				ByteArrayInputStream byteArrayInputStream = (ByteArrayInputStream) inputStream;
				byte[] cacheData = IOUtils.toByteArray(byteArrayInputStream);
				String contentType = streamResponse.getContentType();
				
				if (!publishingConfiguration.getLockedForCacheUpdate()) {
					PublishingResourceCacheData prcd = publishingResourceDAO.getPublishingResourceCacheDataForPublishingResourceAndUriParameter(publishingResource, uriParameters.toString());

					if (prcd == null) {
						prcd = new PublishingResourceCacheData();
						prcd.setPublishingResource(publishingResource);
						prcd.setUriParameter(uriParameters.toString());
					}

					prcd.setCacheData(cacheData);
					prcd.setContentType(contentType);
					publishingResourceDAO.saveOrUpdatePublishingResourceCacheData(prcd);
				}
				
				byteArrayInputStream.reset();

                if (contentType.contains("text") && publishingResource.getEmbedInLayout()) {
                    layoutEmbeddedData = new String(cacheData);
                    return null;
                }
			} else {
				systemLogger.logMessage("PUBLICATION", "Data can NOT be cached: " + publishingResource.getTitle());
			}

			return streamResponse;
		} catch (Exception e) {
			systemLogger.logMessage("PUBLICATION","Error while creating publication PublicationViewer",e);
			throw new RuntimeException(e);
		} finally {
			Console.endConsole();
			PublishingDataShare.endShare();
		}
	}
	
	public StreamResponse loadPublishingStrategy(String publishingStrategyCode) throws Exception
	{
		return loadPublishingStrategy(publishingStrategyCode, new ArrayList<String>());
	}
	
	public StreamResponse loadPublishingStrategy(String publishingStrategyCode, List<String> uriParameters) throws Exception
	{
		GroovyClassLoader groovyClassLoader = new GroovyClassLoader();
		Class clazz = groovyClassLoader.parseClass(publishingStrategyCode);
		
		Constructor cons = null;
		PublishingStrategy publishingStrategy = null;
		
		try {
			cons = clazz.getDeclaredConstructor(ObjectLocator.class, List.class);
			publishingStrategy = (PublishingStrategy) cons.newInstance(objectLocatorService.getObjectLocator(), uriParameters);
		} catch (NoSuchMethodException e) {
			cons = clazz.getDeclaredConstructor(ObjectLocator.class);
			publishingStrategy = (PublishingStrategy) cons.newInstance(objectLocatorService.getObjectLocator());
		}

		return publishingStrategy.getPublishingData();
	}
	
//	private void purgeAllCaches(PublishingConfiguration publishingConfiguration)
//	{
//		if (!publishingConfiguration.getLockedForCacheUpdate()) {
//			publishingConfiguration.setConsole("");
//			publishingConfiguration.setCacheData(null);
//			publishingConfigurationDAO.saveOrUpdatePublishingConfiguration(publishingConfiguration);
//
//			for (PublishingResource publishingResourse : publishingResourceDAO.getAllPublishingResourcesForPublishingConfiguration(publishingConfiguration)) {
//				for (PublishingResourceCacheData prcd : publishingResourse.getPublishingResourceCacheDatas()) {
//					publishingResourceDAO.deletePublishingResourceCacheData(prcd);
//				}
//			}
//		}
//	}
}

