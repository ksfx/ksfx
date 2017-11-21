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

import ch.ksfx.model.TimeSeries;

import java.util.*;




public class NavigationTreeNode
{
	private String name;
	private NavigationTreeNode parent;
	private Map<String, NavigationTreeNode> children;
	private List<TimeSeries> series;

	public NavigationTreeNode(String name, NavigationTreeNode parent)
	{
		this.name = name;
		this.parent = parent;
		
		this.children = new HashMap<String, NavigationTreeNode>();
		this.series = new ArrayList<TimeSeries>();
	}
	
	public String getName()
	{
		return name;
	}
	
	public String getLocator()
	{
		String locator = name;
		
		NavigationTreeNode iter = this;
		
		while (iter.getParent() != null) {
			iter = iter.getParent();
			locator = iter.getName() + "-" + locator;
		}
		
		return locator;
	}
	
	public NavigationTreeNode getChildrenForName(String name)
	{
		if (children.containsKey(name)) {
			return children.get(name);
		}
		
		return null;
	}
	
	public List<NavigationTreeNode> getChildrens()
	{
		if (children != null) {
			Comparator<NavigationTreeNode> navigationTreeNodeComparator = new Comparator<NavigationTreeNode>() {
		    	public int compare(NavigationTreeNode x1, NavigationTreeNode x2) {
		        	return x1.getName().compareToIgnoreCase(x2.getName());
		    	}
			};
			
			List<NavigationTreeNode> childs = new ArrayList<NavigationTreeNode>(children.values());
			
			Collections.sort(childs, navigationTreeNodeComparator);
			
			return childs;
		}
		
		return null;
	}
	
	public NavigationTreeNode getParent()
	{
		return parent;
	}
	
	public List<TimeSeries> getSeries()
	{
		if (children != null) {
			Comparator<TimeSeries> timeSeriesComparator = new Comparator<TimeSeries>() {
		    	public int compare(TimeSeries x1, TimeSeries x2) {
		        	return x1.getName().compareToIgnoreCase(x2.getName());
		    	}
			};
			
			Collections.sort(series, timeSeriesComparator);
			
			return series;
		}
		
		return null;
	}
	
	public void addChildren(NavigationTreeNode navigationTreeNode)
	{
		children.put(navigationTreeNode.getName(), navigationTreeNode);
	}
	
	public void removeChildren(NavigationTreeNode navigationTreeNode)
	{
		children.remove(navigationTreeNode.getName());
	}
	
	public void addSeries(TimeSeries series)
	{
		this.series.add(series);
	}
	
	public void removeSeries(TimeSeries series)
	{
		this.series.remove(series);
	}
}