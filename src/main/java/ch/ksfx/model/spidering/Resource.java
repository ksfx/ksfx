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

package ch.ksfx.model.spidering;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Kejo on 26.10.2014.
 */
@Entity
@Table(name = "resource")
public class Resource
{
    private Long id;
    private String url;
    private String content;
    private byte[] rawContent;
    private String mimeType;
    private HttpMethod httpMethod;
    private List<NameValuePair> nameValuePairs; //postData
    private boolean loadSucceed;
    private boolean isBinary;
    private Integer httpStatusCode;
    private String encoding;
    private List<ResponseHeader> responseHeaders;
    private Integer depth;
    private Resource previousResource;
    private Integer pagingDepth;
    private Long size;
    private Spidering spidering;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public boolean getLoadSucceed()
    {
        return loadSucceed;
    }

    public void setLoadSucceed(boolean loadSucceed)
    {
        this.loadSucceed = loadSucceed;
    }

    public boolean getIsBinary()
    {
        return isBinary;
    }

    public void setIsBinary(boolean isBinary)
    {
        this.isBinary = isBinary;
    }

    public Integer getHttpStatusCode()
    {
        return httpStatusCode;
    }

    public void setHttpStatusCode(Integer httpStatusCode)
    {
        this.httpStatusCode = httpStatusCode;
    }

    @OneToMany(mappedBy = "resource")
    public List<NameValuePair> getNameValuePairs()
    {
        return nameValuePairs;
    }

    public void setNameValuePairs(List<NameValuePair> nameValuePairs)
    {
        this.nameValuePairs = nameValuePairs;
    }

    @OneToMany(mappedBy = "resource")
    public List<ResponseHeader> getResponseHeaders()
    {
        return responseHeaders;
    }

    public void setResponseHeaders(List<ResponseHeader> responseHeaders)
    {
        this.responseHeaders = responseHeaders;
    }

    public String getMimeType()
    {
        return mimeType;
    }

    public void setMimeType(String mimeType)
    {
        this.mimeType = mimeType;
    }

    @Lob
    public byte[] getRawContent()
    {
        return rawContent;
    }

    public void setRawContent(byte[] rawContent)
    {
        this.rawContent = rawContent;
    }

    public String getEncoding()
    {
        return encoding;
    }

    public void setEncoding(String encoding)
    {
        this.encoding = encoding;
    }

    @Enumerated(EnumType.STRING)
    public HttpMethod getHttpMethod()
    {
        return httpMethod;
    }

    public void setHttpMethod(HttpMethod httpMethod)
    {
        this.httpMethod = httpMethod;
    }

    public Integer getDepth()
    {
        return depth;
    }

    public void setDepth(Integer depth)
    {
        this.depth = depth;
    }

    @ManyToOne
    @JoinColumn(name = "previous_resource")
    public Resource getPreviousResource()
    {
        return previousResource;
    }

    public void setPreviousResource(Resource previousResource)
    {
        this.previousResource = previousResource;
    }

    public Integer getPagingDepth()
    {
        return pagingDepth;
    }

    public void setPagingDepth(Integer pagingDepth)
    {
        this.pagingDepth = pagingDepth;
    }

    public Long getSize()
    {
        return size;
    }

    public void setSize(Long size)
    {
        this.size = size;
    }

    @ManyToOne
    @JoinColumn(name = "spidering")
    public Spidering getSpidering()
    {
        return spidering;
    }

    public void setSpidering(Spidering spidering)
    {
        this.spidering = spidering;
    }

    public void addResponseHeader(ResponseHeader responseHeader)
    {
        if (getResponseHeaders() == null) {
            setResponseHeaders(new ArrayList<ResponseHeader>());
        }

        getResponseHeaders().add(responseHeader);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Resource)) return false;

        Resource resource = (Resource) o;

        if (id != null ? !id.equals(resource.id) : resource.id != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }
}
