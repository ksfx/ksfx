package ch.ksfx.controller.publishing;

import ch.ksfx.dao.PublishingConfigurationDAO;
import ch.ksfx.dao.publishing.PublishingResourceDAO;
import ch.ksfx.model.publishing.PublishingConfiguration;
import ch.ksfx.model.publishing.PublishingConfigurationCacheData;
import ch.ksfx.model.publishing.PublishingResource;
import ch.ksfx.model.publishing.PublishingResourceCacheData;
import ch.ksfx.model.publishing.PublishingVisibility;
import org.apache.commons.lang.StringUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Controller
@RequestMapping("/publishing")
public class PublicationViewerController
{
    private PublishingConfigurationDAO publishingConfigurationDAO;
    private PublishingResourceDAO publishingResourceDAO;

    public PublicationViewerController(PublishingConfigurationDAO publishingConfigurationDAO, PublishingResourceDAO publishingResourceDAO)
    {
        this.publishingConfigurationDAO = publishingConfigurationDAO;
        this.publishingResourceDAO = publishingResourceDAO;
    }

    @GetMapping("/publicationviewer/**")
    public /*@ResponseBody*/ Object viewPublication(HttpServletRequest request, ServletResponse response, Model model)
    {
        List<String> pathVariables = extractPathVariables(request);

        Integer fromCache = 0;
        String stringContext = "";

        List<String> uriParameters = new ArrayList<String>();

        //For Fallback Reasons
        if (pathVariables.size() == 1) {
            stringContext = pathVariables.get(0);
        } else {
            fromCache = Integer.parseInt(pathVariables.get(0));
            stringContext = pathVariables.get(1);
        }

        if (pathVariables.size() > 2) {
            for (Integer i = 2; i < pathVariables.size(); i++) {
                uriParameters.add(pathVariables.get(i));
            }
        }

        if (StringUtils.isNumeric(stringContext)) {
            return preparePublishingConfigurationForId(Long.parseLong(stringContext), fromCache, uriParameters, response, model);
        } else if (!stringContext.contains("-")) {
            PublishingConfiguration publishingConfiguration = publishingConfigurationDAO.getPublishingConfigurationForUri(stringContext);
            return preparePublishingConfigurationForId(publishingConfiguration.getId(), fromCache, uriParameters, response, model);
        } else {
            String[] parts = StringUtils.split(stringContext, "-");
            return preparePublishingResourceForConfigurationLocatorAndResourceLocator(parts[0], parts[1], fromCache, uriParameters, response);
        }

        //return "Hello World " + request.getRequestURL() + " --> Path variables: " + pathVariables.toString() + " --> URI Parameters " + uriParameters;
    }

    public Object preparePublishingConfigurationForId(Long publishingConfigurationId, Integer fromCache, List<String> uriParameters, ServletResponse servletResponse, Model model)
    {
        PublishingConfiguration publishingConfiguration = publishingConfigurationDAO.getPublishingConfigurationForId(publishingConfigurationId);

        if (publishingConfiguration.getPublishingVisibility() == null || !publishingConfiguration.getPublishingVisibility().equals(PublishingVisibility.PUBLIC.toString())) {
            if (!SecurityContextHolder.getContext().getAuthentication().isAuthenticated() && (publishingConfiguration.getPublishingVisibility() == null || !publishingConfiguration.getPublishingVisibility().equals(PublishingVisibility.CACHE_FOR_ALL.toString()) || fromCache == 0)) {
                return "You are not authorized to view this page";
            }
        }

        if (fromCache == 1) {
            PublishingConfigurationCacheData pccd = publishingConfiguration.getPublishingConfigurationCacheDataForUriParameter(uriParameters.toString());


            if (pccd.getContentType().contains("text") && publishingConfiguration.getEmbedInLayout()) {
                model.addAttribute("content", new String(pccd.getCacheData()));
                model.addAttribute("pageHeaderTitle", publishingConfiguration.getName());

                return "publishing/publishing_viewer_template";
            }


            servletResponse.setContentType(pccd.getContentType());

            servletResponse.setContentLength(pccd.getCacheData().length);

            OutputStream os = null;

            try {
                os = servletResponse.getOutputStream();
                os.write(pccd.getCacheData() , 0, pccd.getCacheData().length);
            } catch (Exception excp) {
                //handle error
            } finally {
                if (os != null) {
                    try {
                        os.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

            return null; //pccd.getCacheData();
 //           return new GenericStreamResponse(pccd.getCacheData(), publishingConfiguration.getUri(), pccd.getContentType(), false);
        }
        return "";

        /*
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
        */
    }

    public Object preparePublishingResourceForConfigurationLocatorAndResourceLocator(String configurationLocator, String resourceLocator, Integer fromCache, List<String> uriParameters, ServletResponse servletResponse)
    {
        PublishingConfiguration publishingConfiguration = publishingConfigurationDAO.getPublishingConfigurationForUri(configurationLocator);

        if (publishingConfiguration.getPublishingVisibility() == null || !publishingConfiguration.getPublishingVisibility().equals(PublishingVisibility.PUBLIC.toString())) {
            if (!SecurityContextHolder.getContext().getAuthentication().isAuthenticated() && (publishingConfiguration.getPublishingVisibility() == null || !publishingConfiguration.getPublishingVisibility().equals(PublishingVisibility.CACHE_FOR_ALL.toString()) || fromCache == 0)) {
                return "You are not authorized to view this page";
            }
        }

        //pageHeaderTitle = publishingConfiguration.getName();

        PublishingResource publishingResource = publishingResourceDAO.getPublishingResourceForPublishingConfigurationAndUri(publishingConfiguration, resourceLocator);

        if (fromCache == 1) {
            PublishingResourceCacheData prcd = publishingResource.getPublishingResourceCacheDataForUriParameter(uriParameters.toString());

            /*
            if (prcd.getContentType().contains("text") && publishingResource.getEmbedInLayout()) {
                layoutEmbeddedData = new String(prcd.getCacheData());
                return null;
            }
            */

            servletResponse.setContentType(prcd.getContentType());

            servletResponse.setContentLength(prcd.getCacheData().length);

            OutputStream os = null;

            try {
                os = servletResponse.getOutputStream();
                os.write(prcd.getCacheData() , 0, prcd.getCacheData().length);
            } catch (Exception excp) {
                //handle error
            } finally {
                if (os != null) {
                    try {
                        os.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

            return null;
//            return new GenericStreamResponse(prcd.getCacheData(), publishingResource.getUri(), prcd.getContentType(), false);
        }

        return "";
/*
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
 */
    }

    private List<String> extractPathVariables(HttpServletRequest request)
    {
        String pathVariablesUrl = request.getRequestURL().toString().replaceAll(".+\\/publishing\\/publicationviewer\\/","");
        pathVariablesUrl = pathVariablesUrl.replaceAll("\\?.+","");

        if (pathVariablesUrl.endsWith("/")) {
            pathVariablesUrl = pathVariablesUrl.substring(0,pathVariablesUrl.length() - 1);
        }

        return Arrays.asList(StringUtils.split(pathVariablesUrl,'/'));
    }
}
