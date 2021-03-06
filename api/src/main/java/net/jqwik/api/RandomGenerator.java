package net.jqwik.api;

import java.util.*;
import java.util.function.*;
import java.util.stream.*;

import org.apiguardian.api.*;

import static org.apiguardian.api.API.Status.*;

@API(status = STABLE, since = "1.0")
public interface RandomGenerator<T> {

	@API(status = INTERNAL)
	abstract class RandomGeneratorFacade {
		private static RandomGeneratorFacade implementation;

		static  {
			implementation = FacadeLoader.load(RandomGeneratorFacade.class);
		}

		public abstract <T, U> Shrinkable<U> flatMap(Shrinkable<T> self, Function<T, RandomGenerator<U>> mapper, long nextLong);

		public abstract <T, U> Shrinkable<U> flatMap(Shrinkable<T> wrappedShrinkable, Function<T, Arbitrary<U>> mapper, int genSize, long nextLong);

		public abstract <T> RandomGenerator<T> filter(RandomGenerator<T> self, Predicate<T> filterPredicate);

		public abstract <T> RandomGenerator<T> withEdgeCases(RandomGenerator<T> self, int genSize, List<Shrinkable<T>> edgeCases);

		public abstract <T> RandomGenerator<T> withSamples(RandomGenerator<T> self, T[] samples);

		public abstract <T> RandomGenerator<T> unique(RandomGenerator<T> self);
	}

	/**
	 * @param random the source of randomness. Injected by jqwik itself.
	 *
	 * @return the next generated value wrapped within the Shrinkable interface. The method must ALWAYS return a next value.
	 */
	Shrinkable<T> next(Random random);

	default <U> RandomGenerator<U> map(Function<T, U> mapper) {
		return random -> RandomGenerator.this.next(random).map(mapper);
	}

	default <U> RandomGenerator<U> flatMap(Function<T, RandomGenerator<U>> mapper) {
		return random -> {
			Shrinkable<T> wrappedShrinkable = RandomGenerator.this.next(random);
			return RandomGeneratorFacade.implementation.flatMap(wrappedShrinkable, mapper, random.nextLong());
		};
	}

	default <U> RandomGenerator<U> flatMap(Function<T, Arbitrary<U>> mapper, int genSize) {
		return random -> {
			Shrinkable<T> wrappedShrinkable = RandomGenerator.this.next(random);
			return RandomGeneratorFacade.implementation.flatMap(wrappedShrinkable, mapper, genSize, random.nextLong());
		};
	}

	default RandomGenerator<T> filter(Predicate<T> filterPredicate) {
		return RandomGeneratorFacade.implementation.filter(this, filterPredicate);
	}

	default RandomGenerator<T> injectNull(double nullProbability) {
		return random -> {
			if (random.nextDouble() <= nullProbability) return Shrinkable.unshrinkable(null);
			return RandomGenerator.this.next(random);
		};
	}

	default RandomGenerator<T> withEdgeCases(int genSize, List<Shrinkable<T>> edgeCases) {
		return RandomGeneratorFacade.implementation.withEdgeCases(this, genSize, edgeCases);
	}

	@SuppressWarnings("unchecked")
	default RandomGenerator<T> withSamples(T... samples) {
		return RandomGeneratorFacade.implementation.withSamples(this, samples);
	}

	default RandomGenerator<T> unique() {
		return RandomGeneratorFacade.implementation.unique(this);
	}

	default Stream<Shrinkable<T>> stream(Random random) {
		return Stream.generate(() -> this.next(random));
	}
}
