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

package ch.ksfx.model.publishing;

import javax.persistence.*;
import java.util.List;


@Entity
@Table(name = "publishing_configuration")
public class PublishingConfiguration
{
    private Long id;
    private String name;
	private String uri;
    private PublishingCategory publishingCategory;
    private String publishingStrategy;
    private String console;
	private byte[] cacheData;
//	private String contentType;
	private boolean embedInLayout;
	private String layoutIntegration;
    private boolean lockedForEditing;
    private boolean lockedForCacheUpdate;
    private String publishingVisibility;
    private List<PublishingConfigurationCacheData> publishingConfigurationCacheDatas;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    public Long getId()
    {
        return id;
    }

    public void setId(Long id)
    {
        this.id = id;
    }
	
	public String getUri()
	{
		return uri;
	}
	
	public void setUri(String uri)
	{
		this.uri = uri;
	}

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    @ManyToOne
    @JoinColumn(name = "publishing_category")
    public PublishingCategory getPublishingCategory()
    {
        return publishingCategory;
    }

    public void setPublishingCategory(PublishingCategory publishingCategory)
    {
        this.publishingCategory = publishingCategory;
    }

    @Lob
    public String getPublishingStrategy()
    {
        return publishingStrategy;
    }

    public void setPublishingStrategy(String publishingStrategy)
    {
        this.publishingStrategy = publishingStrategy;
    }
    
    public String getConsole()
    {
    	return console;
    }
    
    public void setConsole(String console)
    {
    	this.console = console;
    }
	
	@Lob
	public byte[] getCacheData()
	{
		return cacheData;
	}
	
	public void setCacheData(byte[] cacheData)
	{
		this.cacheData = cacheData;
	}
	
//	public String getContentType()
//	{
//		return contentType;
//	}
	
//	public void setContentType(String contentType)
//	{
//		this.contentType = contentType;
//	}
	
	public boolean getEmbedInLayout()
	{
		return embedInLayout;
	}
	
	public void setEmbedInLayout(boolean embedInLayout)
	{
		this.embedInLayout = embedInLayout;
	}

	public String getLayoutIntegration() {
		return layoutIntegration;
	}

	public void setLayoutIntegration(String layoutIntegration) {
		this.layoutIntegration = layoutIntegration;
	}

	public boolean getLockedForEditing()
	{
		return lockedForEditing;
	}
	
	public void setLockedForEditing(boolean lockedForEditing)
	{
		this.lockedForEditing = lockedForEditing;
	}
    
	public boolean getLockedForCacheUpdate()
	{
		return lockedForCacheUpdate;
	}
	
	public void setLockedForCacheUpdate(boolean lockedForCacheUpdate)
	{
		this.lockedForCacheUpdate = lockedForCacheUpdate;
	}

    public String getPublishingVisibility()
    {
        return publishingVisibility;
    }

    public void setPublishingVisibility(String publishingVisibility)
    {
        this.publishingVisibility = publishingVisibility;
    }
    
	@OneToMany(mappedBy = "publishingConfiguration")
	public List<PublishingConfigurationCacheData> getPublishingConfigurationCacheDatas()
	{
		return publishingConfigurationCacheDatas;
	}
	
	public void setPublishingConfigurationCacheDatas(List<PublishingConfigurationCacheData> publishingConfigurationCacheDatas)
	{
		this.publishingConfigurationCacheDatas = publishingConfigurationCacheDatas;
	}
	
	@Transient
	public PublishingConfigurationCacheData getPublishingConfigurationCacheDataForUriParameter(String uriParameter)
	{
		for (PublishingConfigurationCacheData publishingConfigurationCacheData : publishingConfigurationCacheDatas) {
			if (publishingConfigurationCacheData.getUriParameter().equals(uriParameter)) {
				return publishingConfigurationCacheData;
			}
		}
		
		return null;
	}
}

