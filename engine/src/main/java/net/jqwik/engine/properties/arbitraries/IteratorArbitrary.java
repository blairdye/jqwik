package net.jqwik.engine.properties.arbitraries;

import java.util.*;

import net.jqwik.api.*;
import net.jqwik.engine.properties.arbitraries.exhaustive.*;

public class IteratorArbitrary<T> extends DefaultCollectionArbitrary<T, Iterator<T>> {

	public IteratorArbitrary(Arbitrary<T> elementArbitrary) {
		super(elementArbitrary);
	}

	@Override
	public RandomGenerator<Iterator<T>> generator(int genSize) {
		return createListGenerator(genSize).map(List::iterator);
	}

	@Override
	public Optional<ExhaustiveGenerator<Iterator<T>>> exhaustive() {
		return ExhaustiveGenerators.list(elementArbitrary, minSize, maxSize)
								   .map(generator -> generator.map(List::iterator));
	}
}
