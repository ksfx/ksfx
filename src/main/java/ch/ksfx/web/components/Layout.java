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

import ch.ksfx.web.services.sitemap.Sitemap;
import ch.ksfx.web.services.version.Version;
import org.apache.tapestry5.annotations.Environmental;
import org.apache.tapestry5.annotations.Import;
import org.apache.tapestry5.annotations.InjectComponent;
import org.apache.tapestry5.annotations.Property;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.services.javascript.JavaScriptSupport;

import java.util.List;


@Import(module = { "bootstrap/modal" }, stylesheet = {"context:styles/main_tb.css"}, library = "context:scripts/layout.js")
public class Layout
{
    @Inject
    private Version version;
    
    @Inject
    private Sitemap sitemap;
    
    @InjectComponent
    private Feedback feedback;

     @Environmental
     private JavaScriptSupport js;

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
}
