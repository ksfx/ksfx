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

package ch.ksfx.services.publishing;

import ch.ksfx.dao.PublishingConfigurationDAO;
import ch.ksfx.model.activity.ActivityInstancePersistentData;
import ch.ksfx.model.publishing.PublishingConfiguration;
import ch.ksfx.services.ServiceProvider;
import ch.ksfx.services.activity.RunningActivitiesCache;
import ch.ksfx.services.systemlogger.SystemLogger;
import ch.ksfx.util.Console;
import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.WebResponse;
import com.gargoylesoftware.htmlunit.html.HtmlImage;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class PublicationLoad implements Runnable
{
    private SystemLogger systemLogger;
    private ServiceProvider serviceProvider;
    private boolean isRunning = true;
    private PublishingConfiguration publishingConfiguration;
    private Logger logger = LoggerFactory.getLogger(PublicationLoad.class);

    private PublishingConfigurationDAO publishingConfigurationDAO;
//    private ActivityExecutionDAO activityExecutionDAO;

//    private PublishingStrategy publishingStrategy;

    public PublicationLoad(SystemLogger systemLogger, ServiceProvider serviceProvider, PublishingConfiguration publishingConfiguration, PublishingConfigurationDAO publishingConfigurationDAO)
    {
        this.systemLogger = systemLogger;
        this.serviceProvider = serviceProvider;
        this.publishingConfiguration = publishingConfiguration;
        this.publishingConfigurationDAO = publishingConfigurationDAO;

//        this.activityInstance = activityInstance;
//        activityInstanceDAO = new EbeanActivityInstanceDAO();
//        activityExecutionDAO = new EbeanActivityExecutionDAO(systemLogger, serviceProvider);

//        this.activityInstance.setApproved(true);
//        this.activityInstance.setStarted(new Date());
//        activityInstanceDAO.saveOrUpdateActivityInstance(this.activityInstance);

//        activityExecution = activityExecutionDAO.getActivityExecution(activityInstance.getActivity());
    }

//    public void terminateSpidering()
//    {
//        activityExecution.terminateActivity();
//    }

    @Override
    public void run()
    {
        try {
            PublishingConfiguration pc = publishingConfigurationDAO.getPublishingConfigurationForId(publishingConfiguration.getId());
            pc.setConsole("");
            publishingConfigurationDAO.saveOrUpdatePublishingConfiguration(pc);

            Console.startConsole(publishingConfiguration);
            Console.writeln("[!!!!PUBLICATION AUTO LOADER](" + new Date().toString() + ") Console cleared for auto load");
            Console.writeln("[!!!!PUBLICATION AUTO LOADER](" + new Date().toString() + ") Started auto load of publishing configuration: " + publishingConfiguration.getName());

            List<String> initialPage = new ArrayList<String>();

            initialPage.add("http://127.0.0.1:8080/publishing/publicationviewer/0/" + publishingConfiguration.getUri());

            loadAndFind(initialPage);

            Console.writeln("[!!!!PUBLICATION AUTO LOADER](" + new Date().toString() + ") Auto load successfully finished");
        } catch (Throwable e) {
            systemLogger.logMessage("FATAL_PUBLICATION_LOAD","Error while executing activity", e);
            logger.error("Error in automated publication load",e);
        } finally {
//        	activityInstance = activityInstanceDAO.getActivityInstanceForId(activityInstance.getId());
//            activityInstance.setFinished(new Date());
//            activityInstanceDAO.saveOrUpdateActivityInstance(activityInstance);

            RunningPublicationsCache.runningPublications.remove(publishingConfiguration.getId());
            
            Console.endConsole();
        }
    }

    private String loadPage(String url) throws Exception
    {
        WebClient webClient = null;
        webClient = new WebClient(BrowserVersion.FIREFOX_45);
        webClient.getOptions().setJavaScriptEnabled(true);
        webClient.getCookieManager().setCookiesEnabled(true);
        webClient.getOptions().setRedirectEnabled(true);
        webClient.getOptions().setThrowExceptionOnScriptError(false);
        webClient.getOptions().setCssEnabled(true);
        webClient.getOptions().setUseInsecureSSL(true);
        webClient.getOptions().setThrowExceptionOnFailingStatusCode(false);
        webClient.getOptions().setMaxInMemory(-1);
        webClient.waitForBackgroundJavaScript(20000);
        webClient.getOptions().setTimeout(0);


        Page page = webClient.getPage(url);

        WebResponse webResponse = page.getWebResponse();

        String pageContent = "";

        if (webResponse.getContentType().contains("text")) {
            pageContent = webResponse.getContentAsString();
        } else {
            Console.writeln("[!!!!PUBLICATION AUTO LOADER](" + new Date().toString() + ") No text content: " + webResponse.getContentType() + " -> " + url);
        }

        return pageContent;
    }

    private List<String> findLinksToFollow(String parentLink, String pageContent)
    {
        System.out.println("Searching for links to follow " + pageContent.contains("auto-load"));
        List<String> links = new ArrayList<String>();

        String autoloadPatternToFind = "<[^>]*auto-load=['\"]\\s*true\\s*['\"][^>]*>";

        int flags = 0;

        flags |= Pattern.CASE_INSENSITIVE;
        flags |= Pattern.MULTILINE;
        flags |= Pattern.DOTALL;

        Pattern pattern = Pattern.compile(autoloadPatternToFind, flags);
        Matcher matcher = pattern.matcher(pageContent);

        List<String> hrefTags = new ArrayList<String>();

        while (matcher.find()) {
            Console.writeln("Found auto-load match: " + matcher.group(matcher.groupCount()));
            hrefTags.add(matcher.group(matcher.groupCount()));
        }

        //Map<Url, Order>
        Map<String, Integer> orderedUrls = new HashMap<String, Integer>();

        for (String hrefTag : hrefTags) {
            String hrefPatternToFind = "<[^>]*(?:href|src)=['\"]\\s*([^'\"]+)";
            Pattern hrefPattern = Pattern.compile(hrefPatternToFind, flags);
            Matcher hrefMatcher = hrefPattern.matcher(hrefTag);

            if (hrefMatcher.find()) {
                Integer order = Integer.MAX_VALUE;

                String autoloadorderPatternToFind = "<[^>]*auto-load-order=['\"]\\s*([^'\"]+)";
                Pattern autoloadorderPattern = Pattern.compile(autoloadorderPatternToFind, flags);
                Matcher autoloadorderMatcher = autoloadorderPattern.matcher(hrefTag);

                if (autoloadorderMatcher.find()) {
                    order = Integer.parseInt(autoloadorderMatcher.group(autoloadorderMatcher.groupCount()));
                }

                String link = hrefMatcher.group(hrefMatcher.groupCount());

                if (!link.startsWith("http:")) {
                    link = parentLink.substring(0,parentLink.lastIndexOf("/")) + "/" + link;
                }

                Console.writeln("Found link to follow: " + link + " --> ORDER: " + order);

                orderedUrls.put(link, order);
            }
        }

        Set<Integer> uniqueOrders = new HashSet<Integer>(orderedUrls.values());
        List<Integer> orders = new ArrayList<Integer>(uniqueOrders);
        Collections.sort(orders);

        Console.writeln("Size of map with links: " + orderedUrls.size());
        Console.writeln("Found orders: " + orders);

        for (Integer order : orders) {
            for (String key : orderedUrls.keySet()) {
                Console.writeln("Order in Map: " + orderedUrls.get(key) + " actual order: " + order);
                if (orderedUrls.get(key).equals(order)) {
                    Console.writeln("Adding link to follow: " + key);
                    links.add(key);
                }
            }
        }

        return links;
    }

    private void loadAndFind(List<String> links)
    {
        for (String link : links) {
            String pageContent = "";

            try {
                Console.writeln("[!!!!PUBLICATION AUTO LOADER](" + new Date().toString() + ") Loading URL: " + link);

 //               if (!link.startsWith("http")) {
 //                   link = "http://127.0.0.1:8080/publishing/publicationviewer/0/" + link;
 //               }

                pageContent = loadPage(link);
//                Console.writeln("[!!!!PUBLICATION AUTO LOADER](" + new Date().toString() + ") Found content: " + pageContent);
            } catch (Exception e) {
                systemLogger.logMessage("FATAL_PUBLICATION_LOAD","Can't load page", e);
                logger.error("Error in automated publication load, can't load page",e);
            }

            List<String> linksToFollow = findLinksToFollow(link, pageContent);

            loadAndFind(linksToFollow);
        }
    }
}
