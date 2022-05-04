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


@Entity
@Table(name = "publishing_resource_cache_data")
public class PublishingResourceCacheData
{
	private Long id;
	private PublishingResource publishingResource;
	private String uriParameter;
	private byte[] cacheData;
	private String contentType;
	private String fileNameOrPageTitle;
	
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
	
	@ManyToOne
	@JoinColumn(name = "publishing_resource")
	public PublishingResource getPublishingResource()
	{
		return publishingResource;
	}
	
	public void setPublishingResource(PublishingResource publishingResource)
	{
		this.publishingResource = publishingResource;
	}
	
	public String getUriParameter()
	{
		return uriParameter;
	}
	
	public void setUriParameter(String uriParameter)
	{
		this.uriParameter = uriParameter;
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
	
	public String getContentType()
	{
		return contentType;
	}
	
	public void setContentType(String contentType)
	{
		this.contentType = contentType;
	}

	public String getFileNameOrPageTitle()
	{
		return fileNameOrPageTitle;
	}

	public void setFileNameOrPageTitle(String fileNameOrPageTitle)
	{
		this.fileNameOrPageTitle = fileNameOrPageTitle;
	}
}