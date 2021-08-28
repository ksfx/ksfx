package ch.ksfx.controller.publishing;

import ch.ksfx.dao.PublishingConfigurationDAO;
import ch.ksfx.dao.publishing.PublishingResourceDAO;
import ch.ksfx.model.publishing.*;
import ch.ksfx.services.ServiceProvider;
import ch.ksfx.services.systemlogger.SystemLogger;
import ch.ksfx.util.Console;
import ch.ksfx.util.GenericResponse;
import ch.ksfx.util.PublishingDataShare;
import groovy.lang.GroovyClassLoader;
import org.apache.commons.lang.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Controller
@RequestMapping("/publishing")
public class PublicationViewerController
{
    private PublishingConfigurationDAO publishingConfigurationDAO;
    private PublishingResourceDAO publishingResourceDAO;
    private ServiceProvider serviceProvider;
    private SystemLogger systemLogger;

    public PublicationViewerController(PublishingConfigurationDAO publishingConfigurationDAO, PublishingResourceDAO publishingResourceDAO, ServiceProvider serviceProvider, SystemLogger systemLogger)
    {
        this.publishingConfigurationDAO = publishingConfigurationDAO;
        this.publishingResourceDAO = publishingResourceDAO;
        this.serviceProvider = serviceProvider;
        this.systemLogger = systemLogger;
    }

    @GetMapping("/publicationviewer/**")
    public /*@ResponseBody*/ Object viewPublication(HttpServletRequest request, ServletResponse response, Model model)
    {
        List<String> pathVariables = extractPathVariables(request);

        System.out.println("HTTP Servlet Request Remote IP: " + request.getRemoteAddr());

        for (String headerName : Collections.list(request.getHeaderNames())) {
            System.out.println("HTTP Request:" + headerName + ": " + request.getHeader(headerName));
        }

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

        if (StringUtils.isNumeric(stringContext)) { //Request by ID
            return preparePublishingConfigurationForId(Long.parseLong(stringContext), fromCache, uriParameters, request, response, model);
        } else if (!stringContext.contains("-")) { //Request by URL / Name
            PublishingConfiguration publishingConfiguration = publishingConfigurationDAO.getPublishingConfigurationForUri(stringContext);
            return preparePublishingConfigurationForId(publishingConfiguration.getId(), fromCache, uriParameters, request, response, model);
        } else { //Request for additional publishing resource
            String[] parts = StringUtils.split(stringContext, "-");
            return preparePublishingResourceForConfigurationLocatorAndResourceLocator(parts[0], parts[1], fromCache, uriParameters, request, response, model);
        }

        //return "Hello World " + request.getRequestURL() + " --> Path variables: " + pathVariables.toString() + " --> URI Parameters " + uriParameters;
    }

    public Object preparePublishingConfigurationForId(Long publishingConfigurationId, Integer fromCache, List<String> uriParameters, HttpServletRequest request, ServletResponse servletResponse, Model model)
    {
        PublishingConfiguration publishingConfiguration = publishingConfigurationDAO.getPublishingConfigurationForId(publishingConfigurationId);

        List<String> requestHeaderNames = Collections.list(request.getHeaderNames());

        if (!publishingConfiguration.getAllowInternalLoad() || (!request.getRemoteAddr().equals("127.0.0.1") || requestHeaderNames.contains("x-forwarded-for"))) {
            if (publishingConfiguration.getPublishingVisibility() == null || !publishingConfiguration.getPublishingVisibility().equals(PublishingVisibility.PUBLIC.toString())) {
                if ((!SecurityContextHolder.getContext().getAuthentication().isAuthenticated() || (SecurityContextHolder.getContext().getAuthentication() instanceof AnonymousAuthenticationToken)) && (publishingConfiguration.getPublishingVisibility() == null || !publishingConfiguration.getPublishingVisibility().equals(PublishingVisibility.CACHE_FOR_ALL.toString()) || fromCache == 0)) {
                    return new ResponseEntity(HttpStatus.FORBIDDEN);
                } else {
                    System.out.println("Load from cache allowed, no authentication required");
//                    System.out.println("Security Context Authentication: " + SecurityContextHolder.getContext().getAuthentication().isAuthenticated());
//                    System.out.println("Publishing visibility: " + publishingConfiguration.getPublishingVisibility());
//                    System.out.println("Is visibility cache for all? " + publishingConfiguration.getPublishingVisibility().equals(PublishingVisibility.CACHE_FOR_ALL.toString()));
//                    System.out.println("From cache " + fromCache);
//                    System.out.println("Authentication " + SecurityContextHolder.getContext().getAuthentication());
                }
            } else {
                System.out.println("Public visibility, no authentication required");
            }
        } else {
            System.out.println("Internal load, no authentication required");
//            System.out.println("Remote address: " + request.getRemoteAddr());
        }

        if (fromCache == 1) {
            PublishingConfigurationCacheData pccd = publishingConfiguration.getPublishingConfigurationCacheDataForUriParameter(uriParameters.toString());


            if (pccd.getContentType().contains("text") && publishingConfiguration.getLayoutIntegration() != null && !publishingConfiguration.getLayoutIntegration().trim().isEmpty() && !publishingConfiguration.getLayoutIntegration().equals("NONE")) {
                model.addAttribute("content", new String(pccd.getCacheData()));
                model.addAttribute("pageHeaderTitle", publishingConfiguration.getName());

                return "publishing/" + publishingConfiguration.getLayoutIntegration();
            } else { // Binary image etc...
                servletResponse.setContentType(pccd.getContentType());

                servletResponse.setContentLength(pccd.getCacheData().length);

                OutputStream os = null;

                try {
                    os = servletResponse.getOutputStream();
                    os.write(pccd.getCacheData(), 0, pccd.getCacheData().length);
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
            }

            return null;
        }

        try { //Not from cache but live loading....
            Console.startConsole(publishingConfiguration);
            PublishingDataShare.startShare(publishingConfiguration);

            GenericResponse streamResponse = loadPublishingStrategy(publishingConfiguration.getPublishingStrategy(), uriParameters);

            systemLogger.logMessage("PUBLICATION", "Data can be cached: " + publishingConfiguration.getName());

            //ByteArrayInputStream byteArrayInputStream = (ByteArrayInputStream) inputStream;
            byte[] cacheData = streamResponse.getResponse(); //IOUtils.toByteArray(byteArrayInputStream);
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

            //byteArrayInputStream.reset();

            if (contentType.contains("text") && publishingConfiguration.getLayoutIntegration() != null && !publishingConfiguration.getLayoutIntegration().trim().isEmpty() && !publishingConfiguration.getLayoutIntegration().equals("NONE")) {
            //if (contentType.contains("text") && publishingConfiguration.getEmbedInLayout()) {
                model.addAttribute("content", new String(cacheData));
                model.addAttribute("pageHeaderTitle", publishingConfiguration.getName());

                return "publishing/" + publishingConfiguration.getLayoutIntegration();
            } else { // Binary image etc...
                servletResponse.setContentType(contentType);

                servletResponse.setContentLength(cacheData.length);

                OutputStream os = null;

                try {
                    os = servletResponse.getOutputStream();
                    os.write(cacheData , 0, cacheData.length);
                } catch (Exception excp) {
                    throw(excp);
                } finally {
                    if (os != null) {
                        try {
                            os.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }

            return null;
        } catch (Exception e) {
            systemLogger.logMessage("PUBLICATION","Error while creating publication PublicationViewer",e);
            throw new RuntimeException(e);
        } finally {
            Console.endConsole();
            PublishingDataShare.endShare();
        }
    }

    public Object preparePublishingResourceForConfigurationLocatorAndResourceLocator(String configurationLocator, String resourceLocator, Integer fromCache, List<String> uriParameters, HttpServletRequest request, ServletResponse servletResponse, Model model)
    {
        PublishingConfiguration publishingConfiguration = publishingConfigurationDAO.getPublishingConfigurationForUri(configurationLocator);

        List<String> requestHeaderNames = Collections.list(request.getHeaderNames());

        if (!publishingConfiguration.getAllowInternalLoad() || (!request.getRemoteAddr().equals("127.0.0.1") || requestHeaderNames.contains("x-forwarded-for"))) {
            if (publishingConfiguration.getPublishingVisibility() == null || !publishingConfiguration.getPublishingVisibility().equals(PublishingVisibility.PUBLIC.toString())) {
                if ((!SecurityContextHolder.getContext().getAuthentication().isAuthenticated() || (SecurityContextHolder.getContext().getAuthentication() instanceof AnonymousAuthenticationToken)) && (publishingConfiguration.getPublishingVisibility() == null || !publishingConfiguration.getPublishingVisibility().equals(PublishingVisibility.CACHE_FOR_ALL.toString()) || fromCache == 0)) {
                    return new ResponseEntity(HttpStatus.FORBIDDEN);
                }
            }
        }

        PublishingResource publishingResource = publishingResourceDAO.getPublishingResourceForPublishingConfigurationAndUri(publishingConfiguration, resourceLocator);

        if (fromCache == 1) {
            PublishingResourceCacheData prcd = publishingResource.getPublishingResourceCacheDataForUriParameter(uriParameters.toString());

            if (prcd.getContentType().contains("text") && publishingResource.getLayoutIntegration() != null && !publishingResource.getLayoutIntegration().trim().isEmpty() && !publishingResource.getLayoutIntegration().equals("NONE")) {
                model.addAttribute("content", new String(prcd.getCacheData()));
                model.addAttribute("pageHeaderTitle", publishingConfiguration.getName());

                return "publishing/" + publishingResource.getLayoutIntegration();
            } else { // Binary image etc...

                servletResponse.setContentType(prcd.getContentType());

                servletResponse.setContentLength(prcd.getCacheData().length);

                OutputStream os = null;

                try {
                    os = servletResponse.getOutputStream();
                    os.write(prcd.getCacheData(), 0, prcd.getCacheData().length);
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
            }

            return null;
        }

        try { //Not from cache -> live loading....
            Console.startConsole(publishingConfiguration);
            PublishingDataShare.startShare(publishingConfiguration);

            GenericResponse streamResponse = loadPublishingStrategy(publishingResource.getPublishingStrategy(), uriParameters);
//            InputStream inputStream = streamResponse.getStream();

            systemLogger.logMessage("PUBLICATION", "Data can be cached: " + publishingResource.getTitle());

//            ByteArrayInputStream byteArrayInputStream = (ByteArrayInputStream) inputStream;
            byte[] cacheData = streamResponse.getResponse();
            String contentType = streamResponse.getContentType();

            if (!publishingConfiguration.getLockedForCacheUpdate()) {
                PublishingResourceCacheData prcd = publishingResourceDAO.getPublishingResourceCacheDataForPublishingResourceAndUriParameter(publishingResource, uriParameters.toString());

                if (prcd == null) {
                    prcd = new PublishingResourceCacheData();
                    prcd.setPublishingResource(publishingResource);
                }

                //Tapestry backwards compatibility
                prcd.setUriParameter(uriParameters.toString().replaceAll("\\$0020", " "));

                prcd.setCacheData(cacheData);
                prcd.setContentType(contentType);
                publishingResourceDAO.saveOrUpdatePublishingResourceCacheData(prcd);
            }

            if (contentType.contains("text") && publishingResource.getLayoutIntegration() != null && !publishingResource.getLayoutIntegration().trim().isEmpty() && !publishingResource.getLayoutIntegration().equals("NONE")) {
//            if (contentType.contains("text") && publishingResource.getEmbedInLayout()) {
                model.addAttribute("content", new String(cacheData));
                model.addAttribute("pageHeaderTitle", publishingConfiguration.getName());

                return "publishing/" + publishingResource.getLayoutIntegration();
            } else { // Binary image etc...
                servletResponse.setContentType(contentType);

                servletResponse.setContentLength(cacheData.length);

                OutputStream os = null;

                try {
                    os = servletResponse.getOutputStream();
                    os.write(cacheData, 0, cacheData.length);
                } catch (Exception excp) {
                    throw(excp);
                } finally {
                    if (os != null) {
                        try {
                            os.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }

            return null;
        } catch (Exception e) {
            systemLogger.logMessage("PUBLICATION","Error while creating publication PublicationViewer",e);
            throw new RuntimeException(e);
        } finally {
            Console.endConsole();
            PublishingDataShare.endShare();
        }
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

    public GenericResponse loadPublishingStrategy(String publishingStrategyCode) throws Exception
    {
        return loadPublishingStrategy(publishingStrategyCode, new ArrayList<String>());
    }

    public GenericResponse loadPublishingStrategy(String publishingStrategyCode, List<String> uriParameters) throws Exception
    {
        GroovyClassLoader groovyClassLoader = new GroovyClassLoader();
        Class clazz = groovyClassLoader.parseClass(publishingStrategyCode);

        Constructor cons = null;
        PublishingStrategy publishingStrategy = null;

        try {
            cons = clazz.getDeclaredConstructor(ServiceProvider.class, List.class);
            publishingStrategy = (PublishingStrategy) cons.newInstance(serviceProvider, uriParameters);
        } catch (NoSuchMethodException e) {
            cons = clazz.getDeclaredConstructor(ServiceProvider.class);
            publishingStrategy = (PublishingStrategy) cons.newInstance(serviceProvider);
        }

        return publishingStrategy.getPublishingData();
    }
}
