/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.search.predicate;

import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert.assertThat;
import static org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMapperUtils.referenceProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.types.dsl.StandardIndexFieldTypeOptionsStep;
import org.hibernate.search.engine.backend.work.execution.spi.IndexIndexingPlan;
import org.hibernate.search.engine.backend.types.Searchable;
import org.hibernate.search.engine.backend.types.dsl.IndexFieldTypeFactory;
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.engine.backend.common.DocumentReference;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.integrationtest.backend.tck.testsupport.configuration.DefaultAnalysisDefinitions;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.FieldModelConsumer;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.FieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.StandardFieldMapper;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.ValueWrapper;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.common.FailureReportUtils;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingIndexManager;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingScope;
import org.hibernate.search.util.impl.test.SubTest;
import org.hibernate.search.util.impl.test.annotation.PortedFromSearch5;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.Assume;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class WildcardSearchPredicateIT {

	private static final String INDEX_NAME = "IndexName";
	private static final String COMPATIBLE_INDEX_NAME = "IndexWithCompatibleFields";
	private static final String RAW_FIELD_COMPATIBLE_INDEX_NAME = "IndexWithCompatibleRawFields";
	private static final String INCOMPATIBLE_INDEX_NAME = "IndexWithIncompatiblFields";
	private static final String UNSEARCHABLE_FIELDS_INDEX_NAME = "IndexWithUnsearchableFields";

	private static final String DOCUMENT_1 = "document1";
	private static final String DOCUMENT_2 = "document2";
	private static final String DOCUMENT_3 = "document3";
	private static final String DOCUMENT_4 = "document4";
	private static final String DOCUMENT_5 = "document5";
	private static final String EMPTY = "empty";

	private static final String PATTERN_1 = "local*n";
	private static final String PATTERN_2 = "inter*on";
	private static final String PATTERN_3 = "la*d";
	private static final String TEXT_MATCHING_PATTERN_1 = "Localization in English is a must-have.";
	private static final String TEXT_MATCHING_PATTERN_2 = "Internationalization allows to adapt the application to multiple locales.";
	private static final String TEXT_MATCHING_PATTERN_3 = "A had to call the landlord.";
	private static final String TEXT_MATCHING_PATTERN_2_AND_3 = "I had some interaction with that lad.";

	private static final String TERM_PATTERN_1 = "lOCAl*N";
	private static final String TERM_PATTERN_2 = "IN*oN";
	private static final String TERM_PATTERN_3 = "INteR*oN";
	private static final String TERM_PATTERN_1_EXACT_CASE = "Local*n";
	private static final String TERM_PATTERN_2_EXACT_CASE = "iN*On";
	private static final String TERM_PATTERN_3_EXACT_CASE = "Inter*on";
	private static final String TERM_MATCHING_PATTERN_1 = "Localization";
	private static final String TERM_MATCHING_PATTERN_2 = "iNTroSPEctiOn";
	private static final String TERM_MATCHING_PATTERN_2_AND_3 = "Internationalization";

	private static final String COMPATIBLE_INDEX_DOCUMENT_1 = "compatible_1";
	private static final String RAW_FIELD_COMPATIBLE_INDEX_DOCUMENT_1 = "raw_field_compatible_1";

	@Rule
	public SearchSetupHelper setupHelper = new SearchSetupHelper();

	private IndexMapping indexMapping;
	private StubMappingIndexManager indexManager;

	private OtherIndexMapping compatibleIndexMapping;
	private StubMappingIndexManager compatibleIndexManager;

	private OtherIndexMapping rawFieldCompatibleIndexMapping;
	private StubMappingIndexManager rawFieldCompatibleIndexManager;

	private StubMappingIndexManager incompatibleIndexManager;

	private StubMappingIndexManager unsearchableFieldsIndexManager;

	@Before
	public void setup() {
		setupHelper.start()
				.withIndex(
						INDEX_NAME,
						ctx -> this.indexMapping = new IndexMapping( ctx.getSchemaElement() ),
						indexManager -> this.indexManager = indexManager
				)
				.withIndex(
						COMPATIBLE_INDEX_NAME,
						ctx -> this.compatibleIndexMapping =
								OtherIndexMapping.createCompatible( ctx.getSchemaElement() ),
						indexManager -> this.compatibleIndexManager = indexManager
				)
				.withIndex(
						RAW_FIELD_COMPATIBLE_INDEX_NAME,
						ctx -> this.rawFieldCompatibleIndexMapping =
								OtherIndexMapping.createRawFieldCompatible( ctx.getSchemaElement() ),
						indexManager -> this.rawFieldCompatibleIndexManager = indexManager
				)
				.withIndex(
						INCOMPATIBLE_INDEX_NAME,
						ctx -> OtherIndexMapping.createIncompatible( ctx.getSchemaElement() ),
						indexManager -> this.incompatibleIndexManager = indexManager
				)
				.withIndex(
						UNSEARCHABLE_FIELDS_INDEX_NAME,
						ctx -> OtherIndexMapping.createUnsearchableFieldsIndexMapping( ctx.getSchemaElement() ),
						indexManager -> this.unsearchableFieldsIndexManager = indexManager
				)
				.setup();

		initData();
	}

	@Test
	@PortedFromSearch5(original = "org.hibernate.search.test.dsl.DSLTest.testWildcardQuery")
	public void wildcard() {
		StubMappingScope scope = indexManager.createScope();
		String absoluteFieldPath = indexMapping.analyzedStringField1.relativeFieldName;
		Function<String, SearchQuery<DocumentReference>> createQuery = queryString -> scope.query()
				.where( f -> f.wildcard().field( absoluteFieldPath ).matching( queryString ) )
				.toQuery();

		assertThat( createQuery.apply( PATTERN_1 ) )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_1 );

		assertThat( createQuery.apply( PATTERN_2 ) )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_2, DOCUMENT_4 );

		assertThat( createQuery.apply( PATTERN_3 ) )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_3, DOCUMENT_4 );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3612")
	public void wildcard_normalizeMatchingExpression() {
		StubMappingScope scope = indexManager.createScope();
		String absoluteFieldPath = indexMapping.normalizedField.relativeFieldName;
		Function<String, SearchQuery<DocumentReference>> createQuery = queryString -> scope.query()
				.where( f -> f.wildcard().field( absoluteFieldPath ).matching( queryString ) )
				.toQuery();

		assertThat( createQuery.apply( TERM_PATTERN_1 ) )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_1 );

		assertThat( createQuery.apply( TERM_PATTERN_2 ) )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_2, DOCUMENT_3 );

		assertThat( createQuery.apply( TERM_PATTERN_3 ) )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_3 );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3844") // Used to throw NPE
	public void wildcard_nonAnalyzedField() {
		StubMappingScope scope = indexManager.createScope();
		String absoluteFieldPath = indexMapping.nonAnalyzedField.relativeFieldName;
		Function<String, SearchQuery<DocumentReference>> createQuery = queryString -> scope.query()
				.where( f -> f.wildcard().field( absoluteFieldPath ).matching( queryString ) )
				.toQuery();

		assertThat( createQuery.apply( TERM_PATTERN_1 ) )
				.hasNoHits();
		assertThat( createQuery.apply( TERM_PATTERN_1_EXACT_CASE ) )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_1 );

		assertThat( createQuery.apply( TERM_PATTERN_2 ) )
				.hasNoHits();
		assertThat( createQuery.apply( TERM_PATTERN_2_EXACT_CASE ) )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_2 );

		assertThat( createQuery.apply( TERM_PATTERN_3 ) )
				.hasNoHits();
		assertThat( createQuery.apply( TERM_PATTERN_3_EXACT_CASE ) )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_3 );
	}

	@Test
	public void wildcard_unsearchable() {
		StubMappingScope scope = unsearchableFieldsIndexManager.createScope();
		String absoluteFieldPath = indexMapping.analyzedStringField1.relativeFieldName;

		SubTest.expectException( () ->
				scope.predicate().wildcard().field( absoluteFieldPath )
		).assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "is not searchable" )
				.hasMessageContaining( "Make sure the field is marked as searchable" )
				.hasMessageContaining( absoluteFieldPath );
	}

	/**
	 * Check that a wildcard predicate can be used on a field that has a DSL converter.
	 * The DSL converter should be ignored, and there shouldn't be any exception thrown
	 * (the field should be considered as a text field).
	 */
	@Test
	public void withDslConverter() {
		StubMappingScope scope = indexManager.createScope();
		String absoluteFieldPath = indexMapping.analyzedStringFieldWithDslConverter.relativeFieldName;

		SearchQuery<DocumentReference> query = scope.query()
				.where( f -> f.wildcard().field( absoluteFieldPath ).matching( PATTERN_1 ) )
				.toQuery();

		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_1 );
	}

	@Test
	public void emptyString() {
		StubMappingScope scope = indexManager.createScope();
		MainFieldModel fieldModel = indexMapping.analyzedStringField1;

		SearchQuery<DocumentReference> query = scope.query()
				.where( f -> f.wildcard().field( fieldModel.relativeFieldName ).matching( "" ) )
				.toQuery();

		assertThat( query )
				.hasNoHits();
	}

	@Test
	public void error_unsupportedFieldType() {
		StubMappingScope scope = indexManager.createScope();

		for ( ByTypeFieldModel fieldModel : indexMapping.unsupportedFieldModels ) {
			String absoluteFieldPath = fieldModel.relativeFieldName;

			SubTest.expectException(
					"wildcard() predicate with unsupported type on field " + absoluteFieldPath,
					() -> scope.predicate().wildcard().field( absoluteFieldPath )
			)
					.assertThrown()
					.isInstanceOf( SearchException.class )
					.hasMessageContaining( "Text predicates" )
					.hasMessageContaining( "are not supported by" )
					.hasMessageContaining( "'" + absoluteFieldPath + "'" )
					.satisfies( FailureReportUtils.hasContext(
							EventContexts.fromIndexFieldAbsolutePath( absoluteFieldPath )
					) );
		}
	}

	@Test
	public void error_null() {
		StubMappingScope scope = indexManager.createScope();
		String absoluteFieldPath = indexMapping.analyzedStringField1.relativeFieldName;

		SubTest.expectException(
				"wildcard() predicate with null pattern",
				() -> scope.predicate().wildcard().field( absoluteFieldPath ).matching( null )
		)
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Invalid pattern" )
				.hasMessageContaining( "must be non-null" )
				.hasMessageContaining( absoluteFieldPath );
	}

	@Test
	public void fieldLevelBoost() {
		StubMappingScope scope = indexManager.createScope();

		SearchQuery<DocumentReference> query = scope.query()
				.where( f -> f.wildcard()
						.field( indexMapping.analyzedStringField1.relativeFieldName ).boost( 42 )
						.field( indexMapping.analyzedStringField2.relativeFieldName )
						.matching( PATTERN_1 )
				)
				.sort( f -> f.score() )
				.toQuery();

		assertThat( query )
				.hasDocRefHitsExactOrder( INDEX_NAME, DOCUMENT_1, DOCUMENT_5 );

		query = scope.query()
				.where( f -> f.wildcard()
						.field( indexMapping.analyzedStringField1.relativeFieldName )
						.field( indexMapping.analyzedStringField2.relativeFieldName ).boost( 42 )
						.matching( PATTERN_1 )
				)
				.sort( f -> f.score() )
				.toQuery();

		assertThat( query )
				.hasDocRefHitsExactOrder( INDEX_NAME, DOCUMENT_5, DOCUMENT_1 );
	}

	@Test
	public void predicateLevelBoost() {
		StubMappingScope scope = indexManager.createScope();

		SearchQuery<DocumentReference> query = scope.query()
				.where( f -> f.bool()
						.should( f.wildcard().field( indexMapping.analyzedStringField1.relativeFieldName )
								.matching( PATTERN_1 )
						)
						.should( f.wildcard().field( indexMapping.analyzedStringField2.relativeFieldName )
								.matching( PATTERN_1 )
								.boost( 7 )
						)
				)
				.sort( f -> f.score() )
				.toQuery();

		assertThat( query )
				.hasDocRefHitsExactOrder( INDEX_NAME, DOCUMENT_5, DOCUMENT_1 );

		query = scope.query()
				.where( f -> f.bool()
						.should( f.wildcard().field( indexMapping.analyzedStringField1.relativeFieldName )
								.matching( PATTERN_1 )
								.boost( 39 )
						)
						.should( f.wildcard().field( indexMapping.analyzedStringField2.relativeFieldName )
								.matching( PATTERN_1 )
						)
				)
				.sort( f -> f.score() )
				.toQuery();

		assertThat( query )
				.hasDocRefHitsExactOrder( INDEX_NAME, DOCUMENT_1, DOCUMENT_5 );
	}

	@Test
	public void multiFields() {
		StubMappingScope scope = indexManager.createScope();
		String absoluteFieldPath1 = indexMapping.analyzedStringField1.relativeFieldName;
		String absoluteFieldPath2 = indexMapping.analyzedStringField2.relativeFieldName;
		String absoluteFieldPath3 = indexMapping.analyzedStringField3.relativeFieldName;
		Function<String, SearchQuery<DocumentReference>> createQuery;

		// field(...)

		createQuery = pattern -> scope.query()
				.where( f -> f.wildcard().field( absoluteFieldPath1 )
						.matching( pattern )
				)
				.toQuery();

		assertThat( createQuery.apply( PATTERN_1 ) )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_1 );
		assertThat( createQuery.apply( PATTERN_2 ) )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_2, DOCUMENT_4 );
		assertThat( createQuery.apply( PATTERN_3 ) )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_3, DOCUMENT_4 );

		// field(...).field(...)

		createQuery = pattern -> scope.query()
				.where( f -> f.wildcard().field( absoluteFieldPath1 )
						.field( absoluteFieldPath2 )
						.matching( pattern )
				)
				.toQuery();

		assertThat( createQuery.apply( PATTERN_1 ) )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_1, DOCUMENT_5 );
		assertThat( createQuery.apply( PATTERN_2 ) )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_2, DOCUMENT_4 );
		assertThat( createQuery.apply( PATTERN_3 ) )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_3, DOCUMENT_4 );

		// field().fields(...)

		createQuery = pattern -> scope.query()
				.where( f -> f.wildcard().field( absoluteFieldPath1 )
						.fields( absoluteFieldPath2, absoluteFieldPath3 )
						.matching( pattern )
				)
				.toQuery();

		assertThat( createQuery.apply( PATTERN_1 ) )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_1, DOCUMENT_5 );
		assertThat( createQuery.apply( PATTERN_2 ) )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_2, DOCUMENT_4 );
		assertThat( createQuery.apply( PATTERN_3 ) )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_3, DOCUMENT_4, DOCUMENT_5 );

		// fields(...)

		createQuery = pattern -> scope.query()
				.where( f -> f.wildcard().fields( absoluteFieldPath1, absoluteFieldPath2 )
						.matching( pattern )
				)
				.toQuery();

		assertThat( createQuery.apply( PATTERN_1 ) )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_1, DOCUMENT_5 );
		assertThat( createQuery.apply( PATTERN_2 ) )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_2, DOCUMENT_4 );
		assertThat( createQuery.apply( PATTERN_3 ) )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_3, DOCUMENT_4 );
	}

	@Test
	public void error_unknownField() {
		StubMappingScope scope = indexManager.createScope();
		String absoluteFieldPath = indexMapping.analyzedStringField1.relativeFieldName;

		SubTest.expectException(
				"wildcard() predicate with unknown field",
				() -> scope.predicate().wildcard().field( "unknown_field" )
		)
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Unknown field" )
				.hasMessageContaining( "'unknown_field'" );

		SubTest.expectException(
				"wildcard() predicate with unknown field",
				() -> scope.predicate().wildcard()
						.fields( absoluteFieldPath, "unknown_field" )
		)
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Unknown field" )
				.hasMessageContaining( "'unknown_field'" );

		SubTest.expectException(
				"wildcard() predicate with unknown field",
				() -> scope.predicate().wildcard().field( absoluteFieldPath )
						.field( "unknown_field" )
		)
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Unknown field" )
				.hasMessageContaining( "'unknown_field'" );

		SubTest.expectException(
				"wildcard() predicate with unknown field",
				() -> scope.predicate().wildcard().field( absoluteFieldPath )
						.fields( "unknown_field" )
		)
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Unknown field" )
				.hasMessageContaining( "'unknown_field'" );
	}

	@Test
	public void multiIndex_withCompatibleIndexManager() {
		StubMappingScope scope = indexManager.createScope(
				compatibleIndexManager
		);

		String absoluteFieldPath = indexMapping.analyzedStringField1.relativeFieldName;

		SearchQuery<DocumentReference> query = scope.query()
				.where( f -> f.wildcard().field( absoluteFieldPath ).matching( PATTERN_1 ) )
				.toQuery();

		assertThat( query ).hasDocRefHitsAnyOrder( b -> {
			b.doc( INDEX_NAME, DOCUMENT_1 );
			b.doc( COMPATIBLE_INDEX_NAME, COMPATIBLE_INDEX_DOCUMENT_1 );
		} );
	}

	@Test
	public void multiIndex_withRawFieldCompatibleIndexManager() {
		StubMappingScope scope = indexManager.createScope( rawFieldCompatibleIndexManager );

		String absoluteFieldPath = indexMapping.analyzedStringField1.relativeFieldName;

		SearchQuery<DocumentReference> query = scope.query()
				.where( f -> f.wildcard().field( absoluteFieldPath ).matching( PATTERN_1 ) )
				.toQuery();

		assertThat( query ).hasDocRefHitsAnyOrder( b -> {
			b.doc( INDEX_NAME, DOCUMENT_1 );
			b.doc( RAW_FIELD_COMPATIBLE_INDEX_NAME, RAW_FIELD_COMPATIBLE_INDEX_DOCUMENT_1 );
		} );
	}

	@Test
	public void multiIndex_withIncompatibleIndexManager() {
		// TODO HSEARCH-3307 re-enable this test once we properly take analyzer/normalizer into account when testing field compatibility for predicates in Elasticsearch
		Assume.assumeTrue( "This feature is not implemented yet", false );

		String absoluteFieldPath = indexMapping.analyzedStringField1.relativeFieldName;

		SubTest.expectException(
				() -> {
					indexManager.createScope( incompatibleIndexManager )
							.predicate().wildcard().field( absoluteFieldPath );
				}
		)
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Multiple conflicting types to build a predicate" )
				.hasMessageContaining( "'" + absoluteFieldPath + "'" )
				.satisfies( FailureReportUtils.hasContext(
						EventContexts.fromIndexNames( INDEX_NAME, INCOMPATIBLE_INDEX_NAME )
				) );
	}

	@Test
	public void multiIndex_incompatibleSearchable() {
		StubMappingScope scope = indexManager.createScope( unsearchableFieldsIndexManager );
		String absoluteFieldPath = indexMapping.analyzedStringField1.relativeFieldName;

		SubTest.expectException( () -> scope.predicate().wildcard().field( absoluteFieldPath ) )
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Multiple conflicting types to build a predicate" )
				.hasMessageContaining( absoluteFieldPath )
				.satisfies( FailureReportUtils.hasContext(
						EventContexts.fromIndexNames( INDEX_NAME, UNSEARCHABLE_FIELDS_INDEX_NAME )
				) )
		;
	}

	private void initData() {
		IndexIndexingPlan plan = indexManager.createIndexingPlan();
		plan.add( referenceProvider( DOCUMENT_1 ), document -> {
			document.addValue( indexMapping.analyzedStringField1.reference, TEXT_MATCHING_PATTERN_1 );
			document.addValue( indexMapping.analyzedStringFieldWithDslConverter.reference, TEXT_MATCHING_PATTERN_1 );
			document.addValue( indexMapping.normalizedField.reference, TERM_MATCHING_PATTERN_1 );
			document.addValue( indexMapping.nonAnalyzedField.reference, TERM_MATCHING_PATTERN_1 );
		} );
		plan.add( referenceProvider( DOCUMENT_2 ), document -> {
			document.addValue( indexMapping.analyzedStringField1.reference, TEXT_MATCHING_PATTERN_2 );
			document.addValue( indexMapping.normalizedField.reference, TERM_MATCHING_PATTERN_2 );
			document.addValue( indexMapping.nonAnalyzedField.reference, TERM_MATCHING_PATTERN_2 );
		} );
		plan.add( referenceProvider( DOCUMENT_3 ), document -> {
			document.addValue( indexMapping.analyzedStringField1.reference, TEXT_MATCHING_PATTERN_3 );
			document.addValue( indexMapping.normalizedField.reference, TERM_MATCHING_PATTERN_2_AND_3 );
			document.addValue( indexMapping.nonAnalyzedField.reference, TERM_MATCHING_PATTERN_2_AND_3 );
		} );
		plan.add( referenceProvider( DOCUMENT_4 ), document -> {
			document.addValue( indexMapping.analyzedStringField1.reference, TEXT_MATCHING_PATTERN_2_AND_3 );
		} );
		plan.add( referenceProvider( DOCUMENT_5 ), document -> {
			document.addValue( indexMapping.analyzedStringField2.reference, TEXT_MATCHING_PATTERN_1 );
			document.addValue( indexMapping.analyzedStringField3.reference, TEXT_MATCHING_PATTERN_3 );
		} );
		plan.add( referenceProvider( EMPTY ), document -> {
		} );
		plan.execute().join();

		plan = compatibleIndexManager.createIndexingPlan();
		plan.add( referenceProvider( COMPATIBLE_INDEX_DOCUMENT_1 ), document -> {
			document.addValue( compatibleIndexMapping.analyzedStringField1.reference, TEXT_MATCHING_PATTERN_1 );
		} );
		plan.execute().join();

		plan = rawFieldCompatibleIndexManager.createIndexingPlan();
		plan.add( referenceProvider( RAW_FIELD_COMPATIBLE_INDEX_DOCUMENT_1 ), document -> {
			document.addValue( rawFieldCompatibleIndexMapping.analyzedStringField1.reference, TEXT_MATCHING_PATTERN_1 );
		} );
		plan.execute().join();

		// Check that all documents are searchable
		StubMappingScope scope = indexManager.createScope();
		SearchQuery<DocumentReference> query = scope.query()
				.where( f -> f.matchAll() )
				.toQuery();
		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_1, DOCUMENT_2, DOCUMENT_3, DOCUMENT_4, DOCUMENT_5, EMPTY );
		query = compatibleIndexManager.createScope().query()
				.where( f -> f.matchAll() )
				.toQuery();
		assertThat( query ).hasDocRefHitsAnyOrder( COMPATIBLE_INDEX_NAME, COMPATIBLE_INDEX_DOCUMENT_1 );
		query = rawFieldCompatibleIndexManager.createScope().query()
				.where( f -> f.matchAll() )
				.toQuery();
		assertThat( query ).hasDocRefHitsAnyOrder( RAW_FIELD_COMPATIBLE_INDEX_NAME, RAW_FIELD_COMPATIBLE_INDEX_DOCUMENT_1 );
	}

	private static void forEachTypeDescriptor(Consumer<FieldTypeDescriptor<?>> action) {
		FieldTypeDescriptor.getAll().stream()
				.filter( typeDescriptor -> typeDescriptor.getMatchPredicateExpectations().isPresent() )
				.forEach( action );
	}

	private static void mapByTypeFields(IndexSchemaElement parent, String prefix,
			FieldModelConsumer<Void, ByTypeFieldModel> consumer) {
		forEachTypeDescriptor( typeDescriptor -> {
			ByTypeFieldModel fieldModel = ByTypeFieldModel.mapper( typeDescriptor )
					.map( parent, prefix + typeDescriptor.getUniqueName() );
			consumer.accept( typeDescriptor, null, fieldModel );
		} );
	}

	private static class IndexMapping {
		final List<ByTypeFieldModel> unsupportedFieldModels = new ArrayList<>();

		final MainFieldModel analyzedStringField1;
		final MainFieldModel analyzedStringField2;
		final MainFieldModel analyzedStringField3;
		final MainFieldModel analyzedStringFieldWithDslConverter;
		final MainFieldModel normalizedField;
		final MainFieldModel nonAnalyzedField;

		IndexMapping(IndexSchemaElement root) {
			mapByTypeFields(
					root, "byType_",
					(typeDescriptor, ignored, model) -> {
						if ( !String.class.equals( typeDescriptor.getJavaType() ) ) {
							unsupportedFieldModels.add( model );
						}
					}
			);
			analyzedStringField1 = MainFieldModel.mapper(
					c -> c.asString().analyzer( DefaultAnalysisDefinitions.ANALYZER_STANDARD_ENGLISH.name )
			)
					.map( root, "analyzedString1" );
			analyzedStringField2 = MainFieldModel.mapper(
					c -> c.asString().analyzer( DefaultAnalysisDefinitions.ANALYZER_STANDARD_ENGLISH.name )
			)
					.map( root, "analyzedString2" );
			analyzedStringField3 = MainFieldModel.mapper(
					c -> c.asString().analyzer( DefaultAnalysisDefinitions.ANALYZER_STANDARD_ENGLISH.name )
			)
					.map( root, "analyzedString3" );
			analyzedStringFieldWithDslConverter = MainFieldModel.mapper(
					c -> c.asString().analyzer( DefaultAnalysisDefinitions.ANALYZER_STANDARD_ENGLISH.name )
							.dslConverter( ValueWrapper.class, ValueWrapper.toIndexFieldConverter() )
			)
					.map( root, "analyzedStringWithDslConverter" );
			normalizedField = MainFieldModel.mapper(
					c -> c.asString().normalizer( DefaultAnalysisDefinitions.NORMALIZER_LOWERCASE.name )
			)
					.map( root, "normalized" );
			nonAnalyzedField = MainFieldModel.mapper( c -> c.asString() )
					.map( root, "nonAnalyzed" );
		}
	}

	private static class OtherIndexMapping {
		static OtherIndexMapping createCompatible(IndexSchemaElement root) {
			return new OtherIndexMapping(
					MainFieldModel.mapper(
							c -> c.asString().analyzer( DefaultAnalysisDefinitions.ANALYZER_STANDARD_ENGLISH.name )
					)
							.map( root, "analyzedString1" )
			);
		}

		static OtherIndexMapping createRawFieldCompatible(IndexSchemaElement root) {
			return new OtherIndexMapping(
					MainFieldModel.mapper(
							c -> c.asString().analyzer( DefaultAnalysisDefinitions.ANALYZER_STANDARD_ENGLISH.name )
									// Using a different DSL converter
									.dslConverter( ValueWrapper.class, ValueWrapper.toIndexFieldConverter() )
					)
							.map( root, "analyzedString1" )
			);
		}

		static OtherIndexMapping createIncompatible(IndexSchemaElement root) {
			return new OtherIndexMapping(
					MainFieldModel.mapper(
							// Using a different analyzer/normalizer
							c -> c.asString().normalizer( DefaultAnalysisDefinitions.NORMALIZER_LOWERCASE.name )
					)
							.map( root, "analyzedString1" )
			);
		}

		static OtherIndexMapping createUnsearchableFieldsIndexMapping(IndexSchemaElement root) {
			return new OtherIndexMapping(
					MainFieldModel.mapper(
							// make the field not searchable
							c -> c.asString().searchable( Searchable.NO )
					)
							.map( root, "analyzedString1" )
			);
		}

		final MainFieldModel analyzedStringField1;

		private OtherIndexMapping(MainFieldModel analyzedStringField1) {
			this.analyzedStringField1 = analyzedStringField1;
		}
	}

	private static class MainFieldModel {
		static StandardFieldMapper<String, MainFieldModel> mapper(
				Function<IndexFieldTypeFactory, StandardIndexFieldTypeOptionsStep<?, String>> configuration) {
			return StandardFieldMapper.of(
					configuration,
					(reference, name) -> new MainFieldModel( reference, name )
			);
		}

		final IndexFieldReference<String> reference;
		final String relativeFieldName;

		private MainFieldModel(IndexFieldReference<String> reference, String relativeFieldName) {
			this.reference = reference;
			this.relativeFieldName = relativeFieldName;
		}
	}

	private static class ByTypeFieldModel {
		static <F> StandardFieldMapper<F, ByTypeFieldModel> mapper(FieldTypeDescriptor<F> typeDescriptor) {
			return StandardFieldMapper.of(
					typeDescriptor::configure,
					(reference, name) -> new ByTypeFieldModel( name )
			);
		}

		final String relativeFieldName;

		private ByTypeFieldModel(String relativeFieldName) {
			this.relativeFieldName = relativeFieldName;
		}
	}

}