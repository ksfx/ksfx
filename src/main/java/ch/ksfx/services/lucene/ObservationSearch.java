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
import ch.ksfx.services.SystemEnvironment;
import org.apache.commons.lang.StringUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.FSDirectory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class ObservationSearch
{
	private SystemEnvironment systemEnvironment;
	private ObservationDAO observationDAO;
	private Query query;
    private IndexSearcher searcher;

    public ObservationSearch(SystemEnvironment systemEnvironment, ObservationDAO observationDAO)
	{
		this.systemEnvironment = systemEnvironment;
		this.observationDAO = observationDAO;
	}
	
	public void prepare(String allQuery, String scalarValueQuery, Map<String, String> complexValueQuery, Map<String, String> metaDataQuery, Date dateFrom, Date dateTo, String seriesId)
	{
		try {
	        IndexReader reader = DirectoryReader.open(FSDirectory.open(Paths.get(systemEnvironment.getApplicationIndexfilePath())));
	
	        searcher = new IndexSearcher(reader);
			Analyzer analyzer = new StandardAnalyzer();
			
			QueryParser parser = new QueryParser("catch_all", analyzer);

            System.out.println("Complex value query: " + complexValueQuery);

			String luceneQuery = buildQuery(allQuery, scalarValueQuery, complexValueQuery, metaDataQuery, dateFrom, dateTo, seriesId);

            System.out.println("Lucene query: " + luceneQuery);

	        query = parser.parse(luceneQuery);
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("Error in Lucene query PREPARE");
		}
	}
	
	private String buildQuery(String allQuery, String scalarValueQuery, Map<String, String> complexValueQuery, Map<String, String> metaDataQuery, Date dateFrom, Date dateTo, String seriesId)
	{
		//https://lucene.apache.org/core/2_9_4/queryparsersyntax.html
		//Search might be optimized by FIELD GROUPING
		//sortableDateTime = DateFormatUtil.formatToLexicographicallySortableTimeAndDateString(obs.getObservationTime());
        //String isoDateTime = DateFormatUtil.formatToISO8601TimeAndDateString(obs.getObservationTime());
        //doc.add(new StringField("internal_id", obs.getTimeSeriesId().toString() + sortableDateTime + obs.getSourceId(), Field.Store.NO));                
        //doc.add(new StringField("series_id", obs.getTimeSeriesId().toString(), Field.Store.YES));
        //doc.add(new StringField("observation_time", isoDateTime, Field.Store.YES));
        //doc.add(new StringField("sortable_observation_time", sortableDateTime, Field.Store.NO));
        //doc.add(new StringField("source_id", obs.getSourceId(), Field.Store.YES));
		
		List<String> queryFragments = new ArrayList<String>();
	
		if (dateFrom != null && dateTo != null) {
			queryFragments.add("sortable_observation_time:[" + DateFormatUtil.formatToLexicographicallySortableTimeAndDateString(dateFrom) + " TO " + DateFormatUtil.formatToLexicographicallySortableTimeAndDateString(dateTo) + "]");
		}
	
		if (seriesId != null && !seriesId.isEmpty()) {
			queryFragments.add("series_id:\"" + seriesId + "\"");
		}
		
		if (allQuery != null && !allQuery.isEmpty()) {
			queryFragments.add("catch_all:\"" + allQuery + "\"");
		}
		
		if (scalarValueQuery != null && !scalarValueQuery.isEmpty()) {
			queryFragments.add("scalar_value:\"" + scalarValueQuery + "\"");
		}
		
		if (complexValueQuery != null) {
			for (String key : complexValueQuery.keySet()) {
				if (key != null && !key.isEmpty() && complexValueQuery.get(key) != null && !complexValueQuery.get(key).isEmpty()) {
					queryFragments.add(key + ":\"" + complexValueQuery.get(key) + "\"");
				}		
			}
		}
		
		if (metaDataQuery != null) {
			for (String key : metaDataQuery.keySet()) {
				if (key != null && !key.isEmpty() && metaDataQuery.get(key) != null && !metaDataQuery.get(key).isEmpty()) {
					queryFragments.add(key + ":\"" + metaDataQuery.get(key) + "\"");
				}		
			}
		}

		System.out.println("QUERY: " + StringUtils.join(queryFragments, " AND "));
		
		return StringUtils.join(queryFragments, " AND ");
	}
	
	public Integer getTotalHits()
	{
		TopDocs results = null;
		
		try {
			 results = searcher.search(query, 1);
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("Error in Lucene query GET TOTAL HITS");
		}

		System.out.println("Results: " + results.totalHits + " total hits value: " + results.totalHits.value);
		
		return new Long(results.totalHits.value).intValue(); //.value right???
	}
	
	public List<Observation> getObservations(Integer startIndex, Integer endIndex)
	{
		List<Observation> observations = new ArrayList<Observation>();

		try {
			TopDocs results = searcher.search(query, endIndex + 1);
	        ScoreDoc[] hits = results.scoreDocs;
	    	
	    	for (int i = startIndex; i < results.totalHits.value; i++) { //.value right???
	      		if (i > (endIndex)) {
					break;
	      		}
	
	            Document doc = searcher.doc(hits[i].doc);
	
	            observations.add(
	      			observationDAO.getObservationForTimeSeriesIdObservationTimeAndSourceId(
	      				Integer.parseInt(doc.get("series_id")), 
	      				DateFormatUtil.parseISO8601TimeAndDateString(doc.get("observation_time")),
	      				doc.get("source_id")
	      			)
	      		);
	 		}
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("Error in Lucene query GET OBSERVATIONS");
		}
 		
 		return observations;
	}

	public Page<Observation> getPagedSearch(Pageable pageable, String allQuery, String scalarValueQuery, Map<String, String> complexValueQuery, Map<String, String> metaDataQuery, Date dateFrom, Date dateTo, String seriesId)
	{
		prepare(allQuery, scalarValueQuery, complexValueQuery, metaDataQuery, dateFrom, dateTo, seriesId);

		List<Observation> preparedResults = getObservations(new Long(pageable.getOffset()).intValue(), new Long(pageable.getOffset() + pageable.getPageSize()).intValue());

		Page<Observation> page = new PageImpl<Observation>(preparedResults, pageable, getTotalHits());

		return page;
	}
}