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

package ch.ksfx.web.services.seriesbrowser;

import ch.ksfx.dao.CategoryDAO;
import ch.ksfx.dao.TimeSeriesDAO;
import ch.ksfx.model.Category;
import ch.ksfx.model.TimeSeries;
import org.apache.commons.lang.StringUtils;
import org.apache.tapestry5.ComponentResources;
import org.apache.tapestry5.services.PageRenderLinkSource;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;


public class SeriesBrowser
{
	private TimeSeriesDAO timeSeriesDAO;
	private CategoryDAO categoryDAO;
	private PageRenderLinkSource pageRenderLinkSource;
	private Map<String,NavigationTreeNode> rootNodes;
	
	String snip =
		"<li class=\"list-group-item\" style=\"overflow:hidden;white-space:nowrap;text-overflow:ellipsis;\">" +
		"<span style=\"margin-left:#MARGIN#px;margin-right:#MARGIN#px\"></span>" +
		"<span class=\"icon node-icon\"></span>" +
		"#NAME#" +
		"</li>";
		

	Integer margin = 10;
	
	public SeriesBrowser(TimeSeriesDAO timeSeriesDAO, CategoryDAO categoryDAO, PageRenderLinkSource pageRenderLinkSource)
	{
		this.timeSeriesDAO = timeSeriesDAO;
		this.categoryDAO = categoryDAO;
		this.pageRenderLinkSource = pageRenderLinkSource;
		
		rebuildNavigationTree();
	}
	
	public void rebuildNavigationTree()
	{
		rootNodes = new HashMap<String,NavigationTreeNode>();
		
		for (TimeSeries timeSeries : timeSeriesDAO.getAllTimeSeries()) {
			if (timeSeries.getLocator() != null && !timeSeries.getLocator().isEmpty()) {
				String[] parts = timeSeries.getLocator().split("-");
				
				if (!rootNodes.containsKey(parts[0])) {
					rootNodes.put(parts[0],new NavigationTreeNode(parts[0], null));
				}
				
				NavigationTreeNode rootNode = rootNodes.get(parts[0]);
				
				NavigationTreeNode currentDepth = rootNode;

				if (parts.length == 1) {
					currentDepth.addSeries(timeSeries);
				}
				
				//Integer i = 0;
				
				for (Integer i=1; i < parts.length; i++) {
					if (currentDepth.getChildrenForName(parts[i]) == null) {
						NavigationTreeNode newChild = new NavigationTreeNode(parts[i], currentDepth);
						currentDepth.addChildren(newChild);
					}
					
					currentDepth = currentDepth.getChildrenForName(parts[i]);
					
					if (i == (parts.length - 1)) {
						currentDepth.addSeries(timeSeries);
					}
					
					//i++;
				}
			}
		}
	}
	
	public void removeSeries(TimeSeries timeSeries)
	{
		NavigationTreeNode ntn = getNavigationTreeNodeForTimeSeries(timeSeries, false);
		
		if (ntn == null) {
			return;
		} 
		
		ntn.removeSeries(timeSeries);
		
		if ((ntn.getChildrens() == null || ntn.getChildrens().isEmpty()) && (ntn.getSeries() == null || ntn.getSeries().isEmpty())) {
			NavigationTreeNode parent = ntn.getParent();
			parent.removeChildren(ntn);
		} 
	}
	
	public void addSeries(TimeSeries timeSeries)
	{	
		NavigationTreeNode ntn = getNavigationTreeNodeForTimeSeries(timeSeries, true);
		
		if (ntn == null) {
			return;
		} 
		
		ntn.addSeries(timeSeries);
	}
	
	public NavigationTreeNode getNavigationTreeNodeForTimeSeries(TimeSeries timeSeries, boolean createNotExisting) 
	{
		if (timeSeries.getLocator() == null || timeSeries.getLocator().trim().isEmpty()) {
			return null;
		}
		
		String[] parts = timeSeries.getLocator().split("-");
		
		if (!rootNodes.containsKey(parts[0]) && createNotExisting) {
			rootNodes.put(parts[0],new NavigationTreeNode(parts[0], null));
		}
		
		NavigationTreeNode rootNode = rootNodes.get(parts[0]);
		
		NavigationTreeNode currentDepth = rootNode;
		
		for (Integer i=1; i < parts.length; i++) {
			if (createNotExisting && currentDepth.getChildrenForName(parts[i]) == null) {
				NavigationTreeNode newChild = new NavigationTreeNode(parts[i], currentDepth);
				currentDepth.addChildren(newChild);
			}
			
			currentDepth = currentDepth.getChildrenForName(parts[i]);
		}
		
		return currentDepth;
	}
	
	public String getMarkupForNode(ComponentResources componentResources, List<String> nodes, List<String> filteredSeriesNames)
	{
		List<String> openedNodes = new ArrayList<String>();
		StringBuilder markup = new StringBuilder();
		
		for (String key : rootNodes.keySet()) {
			Integer depth = 0;
			
			NavigationTreeNode rootNtn = rootNodes.get(key);

			if (nodes != null && nodes.contains(rootNtn.getLocator())) {
				markup.append(snip.replaceAll("#MARGIN#", (new Integer(depth * margin)).toString()).replaceAll("#NAME#", "<a href='" + componentResources.createEventLink("closeNode", rootNtn.getName()).toURI() + "'><span class=\"glyphicon glyphicon-folder-open\"></span>&nbsp;&nbsp;" + rootNtn.getName() + "</a> " + getCategoryTitleMarkupForLocator(rootNtn.getLocator())));
				
				for (TimeSeries ts : rootNtn.getSeries()) {
					if (filteredSeriesNames == null || filteredSeriesNames.isEmpty() || filteredSeriesNames.contains(ts.getName())) {
						markup.append(snip.replaceAll("#MARGIN#", (new Integer(margin * (StringUtils.countMatches(rootNtn.getLocator(),"-") + 1))).toString()).replaceAll("#NAME#", "<a href='" + pageRenderLinkSource.createPageRenderLinkWithContext("viewTimeSeries",ts.getId()).toURI() + "'><span class=\"glyphicon glyphicon-th-list\"></span>&nbsp;" + ts.getName() + "</a>"));
					}
				}
			} else {
				markup.append(snip.replaceAll("#MARGIN#", (new Integer(depth * margin)).toString()).replaceAll("#NAME#", "<a href='" + componentResources.createEventLink("openNode", rootNtn.getName()).toURI() + "'><span class=\"glyphicon glyphicon-folder-close\"></span>&nbsp;&nbsp;" + rootNtn.getName() + "</a> " + getCategoryTitleMarkupForLocator(rootNtn.getLocator())));
			}

			if (nodes != null) {
				for (String node : nodes) {
					NavigationTreeNode ntn = rootNtn;
					
					String[] parts = null;
					
					if (node != null && !node.isEmpty()) {
						parts = node.split("-");
					}
					
					if (parts != null && ntn.getName().equals(parts[0])) {

						for (Integer i = 1; i < parts.length; i++) {
							ntn = ntn.getChildrenForName(parts[i]);
						}

						if (ntn != null && !openedNodes.contains(ntn.getLocator())) {
							
							openNode(ntn, markup, componentResources, nodes, filteredSeriesNames, openedNodes);
							
						}
					}
				}
			}
		}
		
		return markup.toString();
	}
	
	public void openNode(NavigationTreeNode ntn, StringBuilder markup, ComponentResources componentResources, List<String> nodes, List<String> filteredSeriesNames, List<String> openedNodes)
	{
		for (NavigationTreeNode child : ntn.getChildrens()) {
			openedNodes.add(child.getLocator());
			
			if (nodes.contains(child.getLocator())) {
				markup.append(snip.replaceAll("#MARGIN#", (new Integer(margin * StringUtils.countMatches(child.getLocator(),"-"))).toString())
						.replaceAll("#NAME#", Matcher.quoteReplacement("<a href='" + componentResources.createEventLink("closeNode", child.getLocator()).toURI() + "'><span class=\"glyphicon glyphicon-folder-open\"></span>&nbsp;&nbsp;" + child.getName() + "</a> " + getCategoryTitleMarkupForLocator(child.getLocator()))));
				for (TimeSeries ts : child.getSeries()) {
					if (filteredSeriesNames == null || filteredSeriesNames.isEmpty() || filteredSeriesNames.contains(ts.getName())) {
						markup.append(snip.replaceAll("#MARGIN#", (new Integer(margin * (StringUtils.countMatches(child.getLocator(),"-") + 1))).toString()).replaceAll("#NAME#", "<a href='" + pageRenderLinkSource.createPageRenderLinkWithContext("viewTimeSeries",ts.getId()).toURI() + "'><span class=\"glyphicon glyphicon-th-list\"></span>&nbsp;" + ts.getName() + "</a>"));
					}
				}
				
				openNode(child, markup, componentResources, nodes, filteredSeriesNames, openedNodes);
			} else {
				markup.append(snip.replaceAll("#MARGIN#", (new Integer(margin * StringUtils.countMatches(child.getLocator(),"-"))).toString())
						.replaceAll("#NAME#", Matcher.quoteReplacement("<a href='" + componentResources.createEventLink("openNode", child.getLocator()).toURI() + "'><span class=\"glyphicon glyphicon-folder-close\"></span>&nbsp;&nbsp;" + child.getName() + "</a> " + getCategoryTitleMarkupForLocator(child.getLocator()))));
			}
		}
	}
	
	public String getCategoryTitleMarkupForLocator(String locator)
	{
		Category category = categoryDAO.getCategoryForLocator(locator);
		
		String title = "";
		
		if (category != null) {
			String description = "";
		
			if (category.getDescription() != null) {
				description = category.getDescription();
			}
		
			title += "<span style=\"font-size:xx-small;color:#CCCCCC\" title=\"" + category.getTitle() + ":\n" + description + "\">" + category.getTitle() + "</span>";
		}
		
		return title;
	}
}
