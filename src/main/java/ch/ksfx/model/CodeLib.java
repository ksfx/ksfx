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

package ch.ksfx.model;

import javax.persistence.*;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * A named, reusable Groovy library script (e.g. the "SQL writer" used by spidering Activities).
 * Deliberately not tied to Activities - Publishing Strategies or other future callers can load
 * these too, so this lives as a standalone concept next to {@link GenericDataStore} rather than
 * under ch.ksfx.model.activity. Named "CodeLib" (not "Library") to avoid confusion with Lexaris'
 * unrelated "Libraries" concept.
 */
@Entity
@Table(name = "code_lib")
public class CodeLib
{
    private Long id;

    @NotNull
    @Size(min = 2, max = 200)
    @NotEmpty
    private String name;
    private String description;
    private String groovyCode;
    private String gitPath;

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

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public String getDescription()
    {
        return description;
    }

    public void setDescription(String description)
    {
        this.description = description;
    }

    @Lob
    public String getGroovyCode()
    {
        return groovyCode;
    }

    public void setGroovyCode(String groovyCode)
    {
        this.groovyCode = groovyCode;
    }

    public String getGitPath()
    {
        return gitPath;
    }

    public void setGitPath(String gitPath)
    {
        this.gitPath = gitPath;
    }
}
