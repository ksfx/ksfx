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

package ch.ksfx.services.lucene;

import ch.ksfx.dao.ObservationDAO;
import ch.ksfx.model.Observation;
import ch.ksfx.util.DateFormatUtil;
import ch.ksfx.services.systemlogger.SystemLogger;
import ch.ksfx.services.SystemEnvironment;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.index.Term;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class AsynchronousIndexer extends Thread
{
    private BlockingQueue<IndexEvent> queuedIndexEvents;
    private Logger logger = LoggerFactory.getLogger(AsynchronousIndexer.class);
    
    private ObservationDAO observationDAO;
    private SystemEnvironment systemEnvironment;
    private SystemLogger systemLogger;

    private Directory dir = null;
    private IndexWriter writer;
    Analyzer analyzer = new StandardAnalyzer();

    public AsynchronousIndexer(ObservationDAO observationDAO, SystemEnvironment systemEnvironment, SystemLogger systemLogger)
    {
    	this.observationDAO = observationDAO;
    	this.systemEnvironment = systemEnvironment;
    	this.systemLogger = systemLogger;
    	
        queuedIndexEvents = new LinkedBlockingQueue<IndexEvent>();

        System.out.println("Index file path: " + systemEnvironment.getApplicationIndexfilePath());

        try {
            dir = FSDirectory.open(Paths.get(systemEnvironment.getApplicationIndexfilePath()));
        } catch (IOException e) {
            e.printStackTrace();
        }
        Analyzer analyzer = new StandardAnalyzer();

    }

    public void run()
    {
        while (true) {
            try {
                if (writer == null || !writer.isOpen()) {
                    IndexWriterConfig iwc = new IndexWriterConfig(analyzer);
                    iwc.setOpenMode(OpenMode.CREATE_OR_APPEND);
                    writer = new IndexWriter(dir, iwc);
                }

                IndexEvent indexEvent = queuedIndexEvents.take();
                
                String sortableDateTime = null;
                
                if (indexEvent.getDeleteEvent()) {
                	if (indexEvent instanceof DeleteSeriesObservationsEvent) {
                		writer.deleteDocuments(new Term("series_id", indexEvent.getSeriesId().toString()));	
                	} else {
                		sortableDateTime = DateFormatUtil.formatToLexicographicallySortableTimeAndDateString(indexEvent.getObservationTime());
                		writer.deleteDocuments(new Term("internal_id", indexEvent.getSeriesId().toString() + sortableDateTime + indexEvent.getSourceId()));
                	}
                	
                	if (queuedIndexEvents.size() == 0) {
                    	writer.close();
                	}
                	
                	continue;
                }
                
                Observation obs = observationDAO.getObservationForTimeSeriesIdObservationTimeAndSourceId(indexEvent.getSeriesId(), indexEvent.getObservationTime(), indexEvent.getSourceId());
                
                Document doc = new Document();
                
               	sortableDateTime = DateFormatUtil.formatToLexicographicallySortableTimeAndDateString(obs.getObservationTime());
               	String isoDateTime = DateFormatUtil.formatToISO8601TimeAndDateString(obs.getObservationTime());
                
                doc.add(new StringField("internal_id", obs.getTimeSeriesId().toString() + sortableDateTime + obs.getSourceId(), Field.Store.NO));                
                doc.add(new StringField("series_id", obs.getTimeSeriesId().toString(), Field.Store.YES));
                doc.add(new StringField("observation_time", isoDateTime, Field.Store.YES));
                doc.add(new StringField("sortable_observation_time", sortableDateTime, Field.Store.NO));
                doc.add(new StringField("source_id", obs.getSourceId(), Field.Store.YES));
                
                addField(doc, "source_uri", obs.getSourceId());
                addField(doc, "scalar_value", obs.getScalarValue());
                
                for (String key : obs.getComplexValue().keySet()) {
                	addField(doc, key, obs.getComplexValue().get(key));
                }
                
                //System.out.println("Meta data to index: " + obs.getMetaData() + " Size: " + obs.getMetaData().size());
                for (String key : obs.getMetaData().keySet()) {
                	System.out.println("Indexing meta data: " + key + " --> " + obs.getMetaData().get(key));
                	addField(doc, key, obs.getMetaData().get(key));
                }
                
                
                
                writer.updateDocument(new Term("internal_id", obs.getTimeSeriesId().toString() + sortableDateTime + obs.getSourceId()), doc);


                if (queuedIndexEvents.size() == 0) {
                    writer.close();
                }
            } catch (Exception e) {
                logger.error("Error while Asynchronous Indexing", e);
            }
        }
    }
    
    private void addField(Document doc, String fieldName, String data)
    {
    	doc.add(new TextField(fieldName, data, Field.Store.NO));
    	doc.add(new TextField("catch_all", data, Field.Store.NO));
    }

    public BlockingQueue<IndexEvent> getQueuedIndexEvents()
    {
        return queuedIndexEvents;
    }

    public void setQueuedIndexEvents(BlockingQueue<IndexEvent> queuedIndexEvents)
    {
        this.queuedIndexEvents = queuedIndexEvents;
    }
}