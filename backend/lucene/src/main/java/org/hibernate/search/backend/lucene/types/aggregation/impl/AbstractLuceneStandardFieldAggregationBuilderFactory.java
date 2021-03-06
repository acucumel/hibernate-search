/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.aggregation.impl;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.backend.lucene.types.codec.impl.LuceneFieldCodec;
import org.hibernate.search.engine.backend.types.converter.spi.ProjectionConverter;
import org.hibernate.search.engine.backend.types.converter.spi.DslConverter;
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.engine.search.common.ValueConvert;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

abstract class AbstractLuceneStandardFieldAggregationBuilderFactory<F>
		implements LuceneFieldAggregationBuilderFactory {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final boolean aggregable;

	private final DslConverter<?, ? extends F> toFieldValueConverter;
	private final DslConverter<? super F, ? extends F> rawToFieldValueConverter;
	private final ProjectionConverter<? super F, ?> fromFieldValueConverter;
	private final ProjectionConverter<? super F, F> rawFromFieldValueConverter;

	AbstractLuceneStandardFieldAggregationBuilderFactory(boolean aggregable,
			DslConverter<?, ? extends F> toFieldValueConverter,
			DslConverter<? super F, ? extends F> rawToFieldValueConverter,
			ProjectionConverter<? super F, ?> fromFieldValueConverter,
			ProjectionConverter<? super F, F> rawFromFieldValueConverter) {
		this.aggregable = aggregable;
		this.toFieldValueConverter = toFieldValueConverter;
		this.rawToFieldValueConverter = rawToFieldValueConverter;
		this.fromFieldValueConverter = fromFieldValueConverter;
		this.rawFromFieldValueConverter = rawFromFieldValueConverter;
	}

	@Override
	public boolean hasCompatibleCodec(LuceneFieldAggregationBuilderFactory other) {
		if ( !getClass().equals( other.getClass() ) ) {
			return false;
		}
		AbstractLuceneStandardFieldAggregationBuilderFactory<?> castedOther =
				(AbstractLuceneStandardFieldAggregationBuilderFactory<?>) other;
		return aggregable == castedOther.aggregable && getCodec().isCompatibleWith( castedOther.getCodec() );
	}

	@Override
	public boolean hasCompatibleConverter(LuceneFieldAggregationBuilderFactory other) {
		if ( !getClass().equals( other.getClass() ) ) {
			return false;
		}
		AbstractLuceneStandardFieldAggregationBuilderFactory<?> castedOther =
				(AbstractLuceneStandardFieldAggregationBuilderFactory<?>) other;
		return toFieldValueConverter.isCompatibleWith( castedOther.toFieldValueConverter )
				&& fromFieldValueConverter.isCompatibleWith( castedOther.fromFieldValueConverter );
	}

	protected abstract LuceneFieldCodec<F> getCodec();

	protected void checkAggregable(String absoluteFieldPath) {
		if ( !aggregable ) {
			throw log.nonAggregableField( absoluteFieldPath,
					EventContexts.fromIndexFieldAbsolutePath( absoluteFieldPath ) );
		}
	}

	protected <T> DslConverter<?, ? extends F> getToFieldValueConverter(
			String absoluteFieldPath, Class<T> expectedType, ValueConvert convert) {
		DslConverter<?, ? extends F> result;
		switch ( convert ) {
			case NO:
				result = rawToFieldValueConverter;
				break;
			case YES:
			default:
				result = toFieldValueConverter;
				break;
		}
		if ( !result.isValidInputType( expectedType ) ) {
			throw log.invalidAggregationInvalidType(
					absoluteFieldPath, expectedType, EventContexts.fromIndexFieldAbsolutePath( absoluteFieldPath )
			);
		}
		return result;
	}

	@SuppressWarnings("unchecked") // We check the cast is legal by asking the converter
	protected <T> ProjectionConverter<? super F, ? extends T> getFromFieldValueConverter(
			String absoluteFieldPath, Class<T> expectedType, ValueConvert convert) {
		ProjectionConverter<? super F, ?> result;
		switch ( convert ) {
			case NO:
				result = rawFromFieldValueConverter;
				break;
			case YES:
			default:
				result = fromFieldValueConverter;
				break;
		}
		if ( !result.isConvertedTypeAssignableTo( expectedType ) ) {
			throw log.invalidAggregationInvalidType(
					absoluteFieldPath, expectedType, EventContexts.fromIndexFieldAbsolutePath( absoluteFieldPath )
			);
		}
		return (ProjectionConverter<? super F, ? extends T>) result;
	}
}
