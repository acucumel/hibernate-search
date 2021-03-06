/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.projection.dsl.impl;

import org.hibernate.search.backend.elasticsearch.search.projection.dsl.ElasticsearchSearchProjectionFactory;
import org.hibernate.search.backend.elasticsearch.search.projection.impl.ElasticsearchSearchProjectionBuilderFactory;
import org.hibernate.search.engine.search.projection.dsl.SearchProjectionFactory;
import org.hibernate.search.engine.search.projection.dsl.ProjectionFinalStep;
import org.hibernate.search.engine.search.projection.dsl.spi.DelegatingSearchProjectionFactory;
import org.hibernate.search.engine.search.projection.dsl.spi.StaticProjectionFinalStep;

import com.google.gson.JsonObject;

public class ElasticsearchSearchProjectionFactoryImpl<R, E>
		extends DelegatingSearchProjectionFactory<R, E>
		implements ElasticsearchSearchProjectionFactory<R, E> {

	private final ElasticsearchSearchProjectionBuilderFactory factory;

	public ElasticsearchSearchProjectionFactoryImpl(SearchProjectionFactory<R, E> delegate,
			ElasticsearchSearchProjectionBuilderFactory factory) {
		super( delegate );
		this.factory = factory;
	}

	@Override
	public ProjectionFinalStep<JsonObject> source() {
		return new StaticProjectionFinalStep<>( factory.source() );
	}

	@Override
	public ProjectionFinalStep<JsonObject> explanation() {
		return new StaticProjectionFinalStep<>( factory.explanation() );
	}

	@Override
	public ProjectionFinalStep<JsonObject> jsonHit() {
		return new StaticProjectionFinalStep<>( factory.jsonHit() );
	}
}
