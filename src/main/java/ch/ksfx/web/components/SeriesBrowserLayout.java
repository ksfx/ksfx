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

package ch.ksfx.web.components;

import ch.ksfx.dao.TimeSeriesDAO;
import ch.ksfx.model.TimeSeries;
import ch.ksfx.web.services.seriesbrowser.SeriesBrowser;
import ch.ksfx.web.services.sitemap.Sitemap;
import ch.ksfx.web.services.version.Version;
import org.apache.commons.lang.StringUtils;
import org.apache.tapestry5.ComponentResources;
import org.apache.tapestry5.annotations.*;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.services.javascript.JavaScriptSupport;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;


@Import(module = { "bootstrap/modal" }, stylesheet = {"context:styles/main_tb.css"}, library = "context:scripts/layout.js")
public class SeriesBrowserLayout
{
    @Inject
    private Version version;
    
    @Inject
    private Sitemap sitemap;
    
    @Inject
    private TimeSeriesDAO timeSeriesDAO;
    
    @InjectComponent
    private Feedback feedback;

    @Environmental
    private JavaScriptSupport js;
     
    @Inject 
	private ComponentResources componentResources;
	
	@Inject
	private SeriesBrowser seriesBrowser;

    @SessionAttribute("ch.ksfx.web.components.SeriesBrowserLayout.openNodes")
	private List<String> openNodes;
	
	@SessionAttribute("ch.ksfx.web.components.SeriesBrowserLayout.filteredSeriesNames")
	private List<String> filteredSeriesNames;
	
	@SessionAttribute("ch.ksfx.web.components.SeriesBrowserLayout.seriesNameSearch")
    @Property
    private String seriesNameSearch;

    public void onCloseNode(String nodeString)
    {
        if (openNodes == null) {
            openNodes = new ArrayList<String>();
        }

        List<String> nodesToClose = new ArrayList<String>();

        for (String node : openNodes) {
            if (node.contains(nodeString)) {
                nodesToClose.add(node);
            }
        }

        for (String nodeToClose : nodesToClose) {
            openNodes.remove(nodeToClose);
        }

        Collections.sort(openNodes);
    }
    
    public void onOpenNode(String nodeString)
    {
    	if (openNodes == null) {
    		openNodes = new ArrayList<String>();
    	}
    	
	   	if (!openNodes.contains(nodeString)) {
    		openNodes.add(nodeString);
    	}

        Collections.sort(openNodes);
    }
    
    public String getBrowser()
    {
    	return seriesBrowser.getMarkupForNode(componentResources, openNodes, filteredSeriesNames);
    }
    

    public Version getVersion()
    {
        return version;
    }

    public Sitemap getSitemap()
    {
        return sitemap;
    }

    public Feedback getFeedback()
    {
        return feedback;
    }

    private void setupRender()
    {
        js.require("bootstrap/dropdown");
        js.require("bootstrap/collapse");
    }

    public List<String> onProvideCompletionsFromSeriesNameSearch(String partial)
    {
        List<String> seriesNames = new ArrayList<String>();
        
        List<TimeSeries> timeSeries = timeSeriesDAO.searchTimeSeries(partial, 100);

		for (TimeSeries ts : timeSeries) {
		    seriesNames.add('"' + ts.getName() + '"');
		}

        return seriesNames;
    }
    
    public boolean getSearchActive()
    {
    	return seriesNameSearch != null;
    }
    
    public void onActionFromResetSearch()
    {
    	filteredSeriesNames = new ArrayList<String>();
    	openNodes = new ArrayList<String>();	
    	seriesNameSearch = null;
    }
    
    public void onSuccessFromSeriesNameSearchForm()
    {
    	filteredSeriesNames = new ArrayList<String>();
    	openNodes = new ArrayList<String>();
    	
    	if (seriesNameSearch != null && seriesNameSearch.length() >= 3) {
    		List<TimeSeries> timeSeries = timeSeriesDAO.searchTimeSeries(seriesNameSearch, 100);	
    		
    		for (TimeSeries ts : timeSeries) {
    			String locator = ts.getLocator();
    			String[] parts = locator.split("-");
    			
    			for (Integer i = 0; i < parts.length; i++) {
    				List<String> listParts = Arrays.asList(parts);
    				List<String> subParts = listParts.subList(0,i+1);
    				
    				String locatorPart = StringUtils.join(subParts, "-");
    				
    				onOpenNode(locatorPart);
    			}
    			
    			
				filteredSeriesNames.add(ts.getName());	
    		}
		}
    }
}
