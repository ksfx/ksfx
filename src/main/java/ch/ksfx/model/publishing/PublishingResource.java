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
@Table(name = "publishing_resource")
public class PublishingResource
{
	private Long id;
	private String title;
	private PublishingConfiguration publishingConfiguration;
	private String uri;
	private String publishingStrategy;
//	private String contentType;
	private List<PublishingResourceCacheData> publishingResourceCacheDatas;
    private boolean embedInLayout;
	
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
	
	public String getTitle()
	{
		return title;
	}
	
	public void setTitle(String title)
	{
		this.title = title;
	}
	
	@ManyToOne
	@JoinColumn(name = "publishing_configuration")
	public PublishingConfiguration getPublishingConfiguration()
	{
		return publishingConfiguration;
	}
	
	public void setPublishingConfiguration(PublishingConfiguration publishingConfiguration)
	{
		this.publishingConfiguration = 	publishingConfiguration;
	}
	
	public String getUri()
	{
		return uri;
	}
	
	public void setUri(String uri)
	{
		this.uri = uri;
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
	
//	public String getContentType()
//	{
//		return contentType;
//	}
	
//	public void setContentType(String contentType)
//	{
//		this.contentType = contentType;
//	}
	
	@OneToMany(mappedBy = "publishingResource")
	public List<PublishingResourceCacheData> getPublishingResourceCacheDatas()
	{
		return publishingResourceCacheDatas;
	}
	
	public void setPublishingResourceCacheDatas(List<PublishingResourceCacheData> publishingResourceCacheDatas)
	{
		this.publishingResourceCacheDatas = publishingResourceCacheDatas;
	}
	
	@Transient
	public PublishingResourceCacheData getPublishingResourceCacheDataForUriParameter(String uriParameter)
	{
		for (PublishingResourceCacheData publishingResourceCacheData : publishingResourceCacheDatas) {
			if (publishingResourceCacheData.getUriParameter().equals(uriParameter)) {
				return publishingResourceCacheData;
			}
		}
		
		return null;
	}

    public boolean getEmbedInLayout()
    {
        return embedInLayout;
    }

    public void setEmbedInLayout(boolean embedInLayout)
    {
        this.embedInLayout = embedInLayout;
    }
}
 


