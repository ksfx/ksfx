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

import ch.ksfx.model.PublishingConfiguration;

import javax.persistence.*;


@Entity
@Table(name = "publishing_configuration_cache_data")
public class PublishingConfigurationCacheData
{
	private Long id;
	private PublishingConfiguration publishingConfiguration;
	private String uriParameter;
	private byte[] cacheData;
	private String contentType;
	
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
	@JoinColumn(name = "publishing_configuration")
	public PublishingConfiguration getPublishingConfiguration()
	{
		return publishingConfiguration;
	}
	
	public void setPublishingConfiguration(PublishingConfiguration publishingConfiguration)
	{
		this.publishingConfiguration = publishingConfiguration;
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
}