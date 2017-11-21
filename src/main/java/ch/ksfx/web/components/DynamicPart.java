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

import ch.ksfx.web.services.StringResource;
import org.apache.tapestry5.Block;
import org.apache.tapestry5.ComponentResources;
import org.apache.tapestry5.ioc.annotations.InjectService;
import org.apache.tapestry5.ioc.util.AvailableValues;
import org.apache.tapestry5.ioc.util.UnknownValueException;
import org.apache.tapestry5.runtime.RenderCommand;
import org.apache.tapestry5.services.dynamic.DynamicDelegate;
import org.apache.tapestry5.services.dynamic.DynamicTemplate;
import org.apache.tapestry5.services.dynamic.DynamicTemplateParser;

import javax.inject.Inject;
import java.util.Date;

/**
 * Created by Kejo on 30.04.2015.
 */
public class DynamicPart {

    /**
     * The dynamic template containing the content to be rendered by the component.
     */
    private DynamicTemplate template;

    @Inject
    private ComponentResources resources;

    @InjectService("DynamicTemplateParser")
    DynamicTemplateParser dynamicTemplateParser;

    public DynamicPart()
    {
        template = dynamicTemplateParser.parseTemplate(
                new StringResource(getTemplateString(), new Date())
        );
    }

    public String getTestString() {
        return "uuuuuuuu";
    }

    private String getTemplateString() {
        StringBuilder sb = new StringBuilder(
                "<t:container xmlns:t=\"http://tapestry.apache.org/schema/tapestry_5_3.xsd\" xmlns:p=\"tapestry:parameter\">");
        sb.append("<t:user.userinfo/>");
        sb.append("</t:container>");
        return sb.toString();
    }

    private final DynamicDelegate delegate = new DynamicDelegate() {
        public ComponentResources getComponentResources() {
            return resources;
        }

        public Block getBlock(String name) {
            Block result = resources.getBlockParameter(name);

            if (result != null)
                return result;

            throw new UnknownValueException(String.format(
                    "Component %s does not have an informal Block parameter with id '%s'.", resources.getCompleteId(),
                    name), null, null, new AvailableValues("Available Blocks", resources.getInformalParameterNames()));
        }
    };

    RenderCommand beginRender() {
        // Probably some room for caching here as well. It shouldn't be necessary to re-create the outermost
        // RenderCommand every time, unless the template has changed from the previous render.
        return template.createRenderCommand(delegate);
    }
}
