/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.analysis;

import org.hibernate.search.backend.lucene.analysis.model.dsl.LuceneAnalysisDefinitionContainerContext;
import org.hibernate.search.backend.lucene.analysis.model.dsl.LuceneAnalyzerTypeStep;
import org.hibernate.search.backend.lucene.analysis.model.dsl.LuceneNormalizerTypeStep;

/**
 * A context allowing the definition of named analyzers and normalizers in a Lucene backend.
 */
public interface LuceneAnalysisConfigurationContext extends LuceneAnalysisDefinitionContainerContext {

	/**
	 * Start a new analyzer definition.
	 *
	 * @param name The name used to reference this analyzer in Hibernate Search.
	 * @return The initial step of a DSL where the analyzer can be defined.
	 */
	LuceneAnalyzerTypeStep analyzer(String name);

	/**
	 * Start a new normalizer definition.
	 *
	 * @param name The name used to reference this normalizer in Hibernate Search.
	 * @return The initial step of a DSL where the normalizer can be defined.
	 */
	LuceneNormalizerTypeStep normalizer(String name);

}