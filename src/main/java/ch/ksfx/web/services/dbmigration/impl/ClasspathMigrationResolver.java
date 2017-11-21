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

package ch.ksfx.web.services.dbmigration.impl;

import com.carbonfive.db.migration.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;


public class ClasspathMigrationResolver implements MigrationResolver
{
    private String classPath;
    private VersionExtractor versionExtractor = new SimpleVersionExtractor();
    private MigrationFactory migrationFactory = new MigrationFactory();

    private Logger logger = LoggerFactory.getLogger(ClasspathMigrationResolver.class);

    public ClasspathMigrationResolver(String classPath)
    {
        this.classPath = classPath;
    }

    public Set<Migration> resolve()
    {
        Set<Migration> migrations = new HashSet<Migration>();

        try {
            Enumeration<URL> enumeration = getClass().getClassLoader().getResources(classPath);

            while (enumeration.hasMoreElements()) {

                URL url = enumeration.nextElement();

                if (url.toExternalForm().startsWith("jar:")) {
                    String file = url.getFile();

                    file = file.substring(0, file.indexOf("!"));
                    file = file.substring(file.indexOf(":") + 1);

                    InputStream is = new FileInputStream(file);

                    ZipInputStream zip = new ZipInputStream(is);

                    ZipEntry ze;

                    while ((ze = zip.getNextEntry()) != null) {
                        if (!ze.isDirectory() && ze.getName().startsWith(classPath) && ze.getName().endsWith(".sql")) {

                            Resource r = new UrlResource(getClass().getClassLoader().getResource(ze.getName()));

                            String version = versionExtractor.extractVersion(r.getFilename());
                            migrations.add(migrationFactory.create(version, r));
                        }
                    }
                } else {
                    File file = new File(url.getFile());

                    for (String s : file.list()) {
                        Resource r = new UrlResource(getClass().getClassLoader().getResource(classPath + "/" + s));

                        String version = versionExtractor.extractVersion(r.getFilename());
                        migrations.add(migrationFactory.create(version, r));
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error while resolving migrations", e);
        }

        return migrations;
    }
}
