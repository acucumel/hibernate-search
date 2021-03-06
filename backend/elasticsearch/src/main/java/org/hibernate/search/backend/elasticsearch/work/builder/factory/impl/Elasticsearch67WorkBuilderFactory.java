/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.work.builder.factory.impl;

import org.hibernate.search.backend.elasticsearch.client.impl.Paths;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.RootTypeMapping;
import org.hibernate.search.backend.elasticsearch.gson.spi.GsonProvider;
import org.hibernate.search.backend.elasticsearch.util.spi.URLEncodedString;
import org.hibernate.search.backend.elasticsearch.work.builder.impl.CreateIndexWorkBuilder;
import org.hibernate.search.backend.elasticsearch.work.builder.impl.DeleteWorkBuilder;
import org.hibernate.search.backend.elasticsearch.work.builder.impl.ExplainWorkBuilder;
import org.hibernate.search.backend.elasticsearch.work.builder.impl.GetIndexMetadataWorkBuilder;
import org.hibernate.search.backend.elasticsearch.work.builder.impl.IndexWorkBuilder;
import org.hibernate.search.backend.elasticsearch.work.builder.impl.PutIndexMappingWorkBuilder;
import org.hibernate.search.backend.elasticsearch.work.builder.impl.SearchWorkBuilder;
import org.hibernate.search.backend.elasticsearch.work.impl.CreateIndexWork;
import org.hibernate.search.backend.elasticsearch.work.impl.DeleteWork;
import org.hibernate.search.backend.elasticsearch.work.impl.ElasticsearchSearchResultExtractor;
import org.hibernate.search.backend.elasticsearch.work.impl.ExplainWork;
import org.hibernate.search.backend.elasticsearch.work.impl.GetIndexMetadataWork;
import org.hibernate.search.backend.elasticsearch.work.impl.IndexWork;
import org.hibernate.search.backend.elasticsearch.work.impl.PutIndexTypeMappingWork;
import org.hibernate.search.backend.elasticsearch.work.impl.SearchWork;

import com.google.gson.JsonObject;

/**
 * A work builder factory for ES6.7 and later 6.x.
 * <p>
 * Compared to ES7:
 * <ul>
 *     <li>Mappings are assigned a "type name"; we use the hardcoded "doc" type name</li>
 *     <li>Some URLs require to include this type name instead of the "_doc" keyword used in ES7.</li>
 *     <li>We set an "include_type_name=true" parameter in index creation and mapping APIs</li>
 * </ul>
 */
@SuppressWarnings("deprecation") // We use Paths.DOC on purpose
public class Elasticsearch67WorkBuilderFactory extends Elasticsearch7WorkBuilderFactory {

	public Elasticsearch67WorkBuilderFactory(GsonProvider gsonProvider) {
		super( gsonProvider );
	}

	@Override
	public IndexWorkBuilder index(String mappedTypeName, URLEncodedString elasticsearchIndexName,
			URLEncodedString id, String routingKey, JsonObject document) {
		return IndexWork.Builder.forElasticsearch67AndBelow( mappedTypeName, elasticsearchIndexName,
				Paths.DOC, id, routingKey, document );
	}

	@Override
	public DeleteWorkBuilder delete(String mappedTypeName, URLEncodedString elasticsearchIndexName,
			URLEncodedString id, String routingKey) {
		return DeleteWork.Builder.forElasticsearch67AndBelow( mappedTypeName, elasticsearchIndexName,
				Paths.DOC, id, routingKey );
	}

	@Override
	public <T> SearchWorkBuilder<T> search(JsonObject payload,
			ElasticsearchSearchResultExtractor<T> searchResultExtractor) {
		return SearchWork.Builder.forElasticsearch63to68( payload, searchResultExtractor );
	}

	@Override
	public ExplainWorkBuilder explain(URLEncodedString indexName, URLEncodedString id, JsonObject payload) {
		return ExplainWork.Builder.forElasticsearch67AndBelow( indexName, Paths.DOC, id, payload );
	}

	@Override
	public CreateIndexWorkBuilder createIndex(URLEncodedString indexName) {
		return CreateIndexWork.Builder.forElasticsearch67( gsonProvider, indexName, Paths.DOC );
	}

	@Override
	public GetIndexMetadataWorkBuilder getIndexMetadata() {
		return GetIndexMetadataWork.Builder.forElasticsearch67( Paths.DOC );
	}

	@Override
	public PutIndexMappingWorkBuilder putIndexTypeMapping(URLEncodedString indexName, RootTypeMapping mapping) {
		return PutIndexTypeMappingWork.Builder.forElasticsearch67( gsonProvider, indexName, Paths.DOC, mapping );
	}
}
