/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.types.dsl.impl;

import org.hibernate.search.backend.elasticsearch.document.model.impl.esnative.DataType;
import org.hibernate.search.backend.elasticsearch.document.model.impl.esnative.PropertyMapping;
import org.hibernate.search.backend.elasticsearch.types.codec.impl.ElasticsearchBooleanFieldCodec;
import org.hibernate.search.backend.elasticsearch.types.impl.ElasticsearchIndexFieldType;
import org.hibernate.search.backend.elasticsearch.types.predicate.impl.ElasticsearchStandardFieldPredicateBuilderFactory;
import org.hibernate.search.backend.elasticsearch.types.projection.impl.ElasticsearchStandardFieldProjectionBuilderFactory;
import org.hibernate.search.backend.elasticsearch.types.sort.impl.ElasticsearchStandardFieldSortBuilderFactory;
import org.hibernate.search.engine.backend.types.converter.FromDocumentFieldValueConverter;
import org.hibernate.search.engine.backend.types.converter.ToDocumentFieldValueConverter;

class ElasticsearchBooleanIndexFieldTypeContext
		extends AbstractElasticsearchScalarFieldTypeContext<ElasticsearchBooleanIndexFieldTypeContext, Boolean> {

	ElasticsearchBooleanIndexFieldTypeContext(ElasticsearchIndexFieldTypeBuildContext buildContext) {
		super( buildContext, Boolean.class, DataType.BOOLEAN );
	}

	@Override
	protected ElasticsearchIndexFieldType<Boolean> toIndexFieldType(PropertyMapping mapping) {
		ToDocumentFieldValueConverter<?, ? extends Boolean> dslToIndexConverter =
				createDslToIndexConverter();
		FromDocumentFieldValueConverter<? super Boolean, ?> indexToProjectionConverter =
				createIndexToProjectionConverter();
		ElasticsearchBooleanFieldCodec codec = ElasticsearchBooleanFieldCodec.INSTANCE;

		return new ElasticsearchIndexFieldType<>(
				codec,
				new ElasticsearchStandardFieldPredicateBuilderFactory<>( dslToIndexConverter, createToDocumentRawConverter(), codec ),
				new ElasticsearchStandardFieldSortBuilderFactory<>( resolvedSortable, dslToIndexConverter, createToDocumentRawConverter(), codec ),
				new ElasticsearchStandardFieldProjectionBuilderFactory<>( resolvedProjectable, indexToProjectionConverter, createFromDocumentRawConverter(), codec ),
				mapping
		);
	}

	@Override
	protected ElasticsearchBooleanIndexFieldTypeContext thisAsS() {
		return this;
	}
}