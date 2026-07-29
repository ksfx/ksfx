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

package ch.ksfx.services.codelib;

import ch.ksfx.dao.CodeLibDAO;
import ch.ksfx.dao.GenericDataStoreDAO;
import ch.ksfx.dao.NoteDAO;
import ch.ksfx.model.CodeLib;
import ch.ksfx.model.GenericDataStore;
import ch.ksfx.model.note.NoteFile;
import ch.ksfx.services.git.ActivityGitRepositoryService;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * One-off migration of the single library script that today lives as a NoteFile blob, referenced
 * indirectly via a GenericDataStore key, into a proper {@link CodeLib} row. Idempotent - skips if
 * a CodeLib with the derived name already exists, so it's safe to click more than once.
 *
 * Deliberately out of scope: rewiring the runtime lookup (e.g. loadSqlWriter() in
 * UsExecutiveOrdersSlrpr.groovy, which still resolves the NoteFile via GenericDataStore) to read
 * from this new CodeLib instead. That stays untouched for now - a separate follow-up.
 */
@Service
public class CodeLibMigrationService
{
    private static final String DEFAULT_SQL_WRITER_KEY = "DEFAULT_SQL_WRITER";
    private static final Pattern CLASS_NAME_PATTERN = Pattern.compile("class\\s+(\\w+)");

    private final CodeLibDAO codeLibDAO;
    private final GenericDataStoreDAO genericDataStoreDAO;
    private final NoteDAO noteDAO;
    private final ActivityGitRepositoryService activityGitRepositoryService;

    public CodeLibMigrationService(CodeLibDAO codeLibDAO,
                                    GenericDataStoreDAO genericDataStoreDAO,
                                    NoteDAO noteDAO,
                                    ActivityGitRepositoryService activityGitRepositoryService)
    {
        this.codeLibDAO = codeLibDAO;
        this.genericDataStoreDAO = genericDataStoreDAO;
        this.noteDAO = noteDAO;
        this.activityGitRepositoryService = activityGitRepositoryService;
    }

    public String migrateSqlWriterFromNoteFile() throws GitAPIException, IOException
    {
        GenericDataStore reference = genericDataStoreDAO.getGenericDataStoreForKey(DEFAULT_SQL_WRITER_KEY);

        if (reference == null || reference.getDataValue() == null) {
            return "Kein '" + DEFAULT_SQL_WRITER_KEY + "' im Generic Data Store gefunden - nichts zu migrieren.";
        }

        Long noteFileId;

        try {
            noteFileId = Long.parseLong(reference.getDataValue().trim());
        } catch (NumberFormatException e) {
            return "'" + DEFAULT_SQL_WRITER_KEY + "' zeigt auf keine gültige NoteFile-ID (" + reference.getDataValue() + ").";
        }

        NoteFile noteFile = noteDAO.getNoteFileForId(noteFileId);

        if (noteFile == null || noteFile.getFileContent() == null) {
            return "NoteFile " + noteFileId + " nicht gefunden oder leer.";
        }

        String source = new String(noteFile.getFileContent(), StandardCharsets.UTF_8);
        String className = extractClassName(source, noteFile.getFileName());

        if (codeLibDAO.getCodeLibForName(className) != null) {
            return "CodeLib '" + className + "' existiert bereits - nichts getan.";
        }

        CodeLib codeLib = new CodeLib();
        codeLib.setName(className);
        codeLib.setDescription("Migriert aus NoteFile (GenericDataStore-Key " + DEFAULT_SQL_WRITER_KEY + ")");
        codeLib.setGroovyCode(source);

        if (activityGitRepositoryService.isActive()) {
            String slug = activityGitRepositoryService.uniqueSlug(
                    activityGitRepositoryService.slugify(className),
                    ActivityGitRepositoryService.LIBS_DIRECTORY);
            String gitPath = ActivityGitRepositoryService.LIBS_DIRECTORY + "/" + slug + ".groovy";

            activityGitRepositoryService.writeActivitySource(gitPath, source, "Migrate SQL writer code lib from NoteFile");

            codeLib.setGitPath(gitPath);
        }

        codeLibDAO.saveOrUpdateCodeLib(codeLib);

        return "CodeLib '" + className + "' aus NoteFile migriert"
                + (codeLib.getGitPath() != null ? " und nach Git geschrieben (" + codeLib.getGitPath() + ")." : " (Git nicht aktiv, nur in der DB gespeichert).");
    }

    private String extractClassName(String groovySource, String fallbackName)
    {
        Matcher matcher = CLASS_NAME_PATTERN.matcher(groovySource);

        if (matcher.find()) {
            return matcher.group(1);
        }

        return fallbackName != null ? fallbackName : "codelib";
    }
}
