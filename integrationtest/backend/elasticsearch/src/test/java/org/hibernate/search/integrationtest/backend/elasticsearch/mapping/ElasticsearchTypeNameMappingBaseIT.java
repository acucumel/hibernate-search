/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.elasticsearch.mapping;

import static org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.ElasticsearchIndexMetadataTestUtils.defaultReadAlias;
import static org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.ElasticsearchIndexMetadataTestUtils.defaultWriteAlias;
import static org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.ElasticsearchIndexMetadataTestUtils.mappingWithDiscriminatorProperty;
import static org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.ElasticsearchIndexMetadataTestUtils.mappingWithoutAnyProperty;
import static org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMapperUtils.referenceProvider;

import org.hibernate.search.backend.elasticsearch.cfg.ElasticsearchBackendSettings;
import org.hibernate.search.backend.elasticsearch.cfg.ElasticsearchIndexSettings;
import org.hibernate.search.backend.elasticsearch.index.layout.impl.IndexNames;
import org.hibernate.search.backend.elasticsearch.index.IndexLifecycleStrategyName;
import org.hibernate.search.backend.elasticsearch.util.spi.URLEncodedString;
import org.hibernate.search.engine.backend.common.DocumentReference;
import org.hibernate.search.engine.backend.work.execution.spi.IndexIndexingPlan;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.rule.TestElasticsearchClient;
import org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingIndexManager;
import org.hibernate.search.util.impl.test.SubTest;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import com.google.gson.JsonObject;

/**
 * Test the base functionality of type name mapping strategies.
 */
@RunWith(Parameterized.class)
public class ElasticsearchTypeNameMappingBaseIT {

	private static final String TYPE1_NAME = "type1_name";
	private static final String INDEX1_NAME = "index1_name";
	private static final String TYPE2_NAME = "type2_name";
	private static final String INDEX2_NAME = "index2_name";

	private static final String ID_1 = "id_1";
	private static final String ID_2 = "id_2";

	private enum IrregularIndexNameSupport {
		YES,
		NO
	}

	@Parameterized.Parameters(name = "{0}")
	public static Object[][] configurations() {
		return new Object[][] {
				{ null, mappingWithDiscriminatorProperty( "_entity_type" ), IrregularIndexNameSupport.YES },
				{ "index-name", mappingWithoutAnyProperty(), IrregularIndexNameSupport.NO },
				{ "discriminator", mappingWithDiscriminatorProperty( "_entity_type" ), IrregularIndexNameSupport.YES }
		};
	}

	@Rule
	public SearchSetupHelper setupHelper = new SearchSetupHelper();

	@Rule
	public TestElasticsearchClient elasticsearchClient = new TestElasticsearchClient();

	private final String strategyName;
	private final JsonObject expectedMappingContent;
	private final IrregularIndexNameSupport irregularIndexNameSupport;

	private StubMappingIndexManager index1Manager;
	private StubMappingIndexManager index2Manager;

	public ElasticsearchTypeNameMappingBaseIT(String strategyName, JsonObject expectedMappingContent,
			IrregularIndexNameSupport irregularIndexNameSupport) {
		this.strategyName = strategyName;
		this.expectedMappingContent = expectedMappingContent;
		this.irregularIndexNameSupport = irregularIndexNameSupport;
	}

	@Test
	public void singleIndexScope() {
		setup( IndexLifecycleStrategyName.DROP_AND_CREATE_AND_DROP );
		SearchResultAssert.assertThat(
				index1Manager.createScope().query().where( f -> f.matchAll() ).toQuery()
		)
				.hasDocRefHitsAnyOrder( c -> c
						.doc( TYPE1_NAME, ID_1 )
						.doc( TYPE1_NAME, ID_2 )
				);
	}

	@Test
	public void multiIndexScope() {
		setup( IndexLifecycleStrategyName.DROP_AND_CREATE_AND_DROP );

		SearchResultAssert.assertThat(
				index1Manager.createScope( index2Manager ).query().where( f -> f.matchAll() ).toQuery()
		)
				.hasDocRefHitsAnyOrder( c -> c
						.doc( TYPE1_NAME, ID_1 )
						.doc( TYPE1_NAME, ID_2 )
						.doc( TYPE2_NAME, ID_1 )
						.doc( TYPE2_NAME, ID_2 )
				);
	}

	@Test
	public void irregularIndexName_correctNamingSchemeAndIncorrectUniqueKey_singleIndexScope() {
		createIndexesWithCorrectNamingSchemeIncorrectUniqueKeyAndCorrectAliases();
		setup( IndexLifecycleStrategyName.NONE );

		SearchQuery<DocumentReference> query = index1Manager.createScope().query()
				.where( f -> f.matchAll() )
				.toQuery();

		// Should work even if the index has an irregular name: the selected type-name mapping strategy is not actually used.
		SearchResultAssert.assertThat( query )
				.hasDocRefHitsAnyOrder( c -> c
						.doc( TYPE1_NAME, ID_1 )
						.doc( TYPE1_NAME, ID_2 )
				);
	}

	@Test
	public void irregularIndexName_correctNamingSchemeAndIncorrectUniqueKey_multiIndexScope() {
		createIndexesWithCorrectNamingSchemeIncorrectUniqueKeyAndCorrectAliases();
		setup( IndexLifecycleStrategyName.NONE );

		SearchQuery<DocumentReference> query = index1Manager.createScope( index2Manager ).query()
				.where( f -> f.matchAll() )
				.toQuery();

		if ( IrregularIndexNameSupport.YES.equals( irregularIndexNameSupport ) ) {
			SearchResultAssert.assertThat( query )
					.hasDocRefHitsAnyOrder( c -> c
							.doc( TYPE1_NAME, ID_1 )
							.doc( TYPE1_NAME, ID_2 )
							.doc( TYPE2_NAME, ID_1 )
							.doc( TYPE2_NAME, ID_2 )
					);
		}
		else {
			SubTest.expectException( () -> query.fetch( 20 ) )
					.assertThrown()
					.isInstanceOf( SearchException.class );
		}
	}

	@Test
	public void irregularIndexName_incorrectNamingScheme_singleIndexScope() {
		createIndexesWithIncorrectNamingSchemeAndCorrectAliases();
		setup( IndexLifecycleStrategyName.NONE );

		SearchQuery<DocumentReference> query = index1Manager.createScope().query()
				.where( f -> f.matchAll() )
				.toQuery();

		// Should work even if the index has an irregular name: the selected type-name mapping strategy is not actually used.
		SearchResultAssert.assertThat( query )
				.hasDocRefHitsAnyOrder( c -> c
						.doc( TYPE1_NAME, ID_1 )
						.doc( TYPE1_NAME, ID_2 )
				);
	}

	@Test
	public void irregularIndexName_incorrectNamingScheme_multiIndexScope() {
		createIndexesWithIncorrectNamingSchemeAndCorrectAliases();
		setup( IndexLifecycleStrategyName.NONE );

		SearchQuery<DocumentReference> query = index1Manager.createScope( index2Manager ).query()
				.where( f -> f.matchAll() )
				.toQuery();

		if ( IrregularIndexNameSupport.YES.equals( irregularIndexNameSupport ) ) {
			SearchResultAssert.assertThat( query )
					.hasDocRefHitsAnyOrder( c -> c
							.doc( TYPE1_NAME, ID_1 )
							.doc( TYPE1_NAME, ID_2 )
							.doc( TYPE2_NAME, ID_1 )
							.doc( TYPE2_NAME, ID_2 )
					);
		}
		else {
			SubTest.expectException( () -> query.fetch( 20 ) )
					.assertThrown()
					.isInstanceOf( SearchException.class );
		}
	}

	private void createIndexesWithCorrectNamingSchemeIncorrectUniqueKeyAndCorrectAliases() {
		URLEncodedString index1PrimaryName = IndexNames.encodeName( INDEX1_NAME + "-000001-somesuffix-000001" );
		URLEncodedString index1WriteAlias = defaultWriteAlias( INDEX1_NAME );
		URLEncodedString index1ReadAlias = defaultReadAlias( INDEX1_NAME );
		elasticsearchClient.index( index1PrimaryName, index1WriteAlias, index1ReadAlias )
				.deleteAndCreate()
				.type().putMapping( expectedMappingContent );
		URLEncodedString index2PrimaryName = IndexNames.encodeName( INDEX2_NAME + "-000001-somesuffix-000001" );
		URLEncodedString index2WriteAlias = defaultWriteAlias( INDEX2_NAME );
		URLEncodedString index2ReadAlias = defaultReadAlias( INDEX2_NAME );
		elasticsearchClient.index( index2PrimaryName, index2WriteAlias, index2ReadAlias )
				.deleteAndCreate()
				.type().putMapping( expectedMappingContent );
	}

	private void createIndexesWithIncorrectNamingSchemeAndCorrectAliases() {
		URLEncodedString index1PrimaryName = IndexNames.encodeName( INDEX1_NAME + "-somesuffix" );
		URLEncodedString index1WriteAlias = defaultWriteAlias( INDEX1_NAME );
		URLEncodedString index1ReadAlias = defaultReadAlias( INDEX1_NAME );
		elasticsearchClient.index( index1PrimaryName, index1WriteAlias, index1ReadAlias )
				.deleteAndCreate()
				.type().putMapping( expectedMappingContent );
		URLEncodedString index2PrimaryName = IndexNames.encodeName( INDEX2_NAME + "-somesuffix" );
		URLEncodedString index2WriteAlias = defaultWriteAlias( INDEX2_NAME );
		URLEncodedString index2ReadAlias = defaultReadAlias( INDEX2_NAME );
		elasticsearchClient.index( index2PrimaryName, index2WriteAlias, index2ReadAlias )
				.deleteAndCreate()
				.type().putMapping( expectedMappingContent );
	}

	private void setup(IndexLifecycleStrategyName lifecycleStrategy) {
		setupHelper.start()
				.withIndexDefaultsProperty(
						ElasticsearchIndexSettings.LIFECYCLE_STRATEGY, lifecycleStrategy
				)
				.withBackendProperty(
						ElasticsearchBackendSettings.MAPPING_TYPE_NAME_STRATEGY, strategyName
				)
				.withIndex(
						INDEX1_NAME,
						options -> options.mappedType( TYPE1_NAME ),
						ignored -> { },
						indexManager -> this.index1Manager = indexManager
				)
				.withIndex(
						INDEX2_NAME,
						options -> options.mappedType( TYPE2_NAME ),
						ignored -> { },
						indexManager -> this.index2Manager = indexManager
				)
				.setup();

		initData();
	}

	private void initData() {
		IndexIndexingPlan plan = index1Manager.createIndexingPlan();
		plan.add( referenceProvider( ID_1 ), document -> { } );
		plan.add( referenceProvider( ID_2 ), document -> { } );
		plan.execute().join();

		plan = index2Manager.createIndexingPlan();
		plan.add( referenceProvider( ID_1 ), document -> { } );
		plan.add( referenceProvider( ID_2 ), document -> { } );
		plan.execute().join();
	}

}
