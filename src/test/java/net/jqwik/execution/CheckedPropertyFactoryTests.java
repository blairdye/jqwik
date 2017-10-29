package net.jqwik.execution;

import static org.assertj.core.api.Assertions.*;

import java.lang.reflect.Method;
import java.util.*;

import org.junit.platform.engine.UniqueId;

import net.jqwik.TestHelper;
import net.jqwik.api.*;
import net.jqwik.descriptor.PropertyMethodDescriptor;

public class CheckedPropertyFactoryTests {

	private CheckedPropertyFactory factory = new CheckedPropertyFactory();

	@Example
	void simple() {
		PropertyMethodDescriptor descriptor = createDescriptor("prop", 42L, 11, 4, ShrinkingMode.OFF);
		CheckedProperty property = factory.fromDescriptor(descriptor, new PropertyExamples());

		assertThat(property.propertyName).isEqualTo("prop");

		assertThat(property.forAllParameters).size().isEqualTo(2);
		assertThat(property.forAllParameters.get(0).getType()).isEqualTo(int.class);
		assertThat(property.forAllParameters.get(1).getType()).isEqualTo(String.class);

		List<Object> argsTrue = Arrays.asList(1, "test");
		List<Object> argsFalse = Arrays.asList(2, "test");
		assertThat(property.forAllPredicate.test(argsTrue)).isTrue();
		assertThat(property.forAllPredicate.test(argsFalse)).isFalse();

		assertThat(property.randomSeed).isEqualTo(42);
		assertThat(property.tries).isEqualTo(11);
		assertThat(property.maxDiscardRatio).isEqualTo(4);
	}

	@Example
	void withUnboundParams() {
		PropertyMethodDescriptor descriptor = createDescriptor("propWithUnboundParams", 42L, 11, 5, ShrinkingMode.OFF);
		CheckedProperty property = factory.fromDescriptor(descriptor, new PropertyExamples());

		assertThat(property.forAllParameters).size().isEqualTo(2);
		assertThat(property.forAllParameters.get(0).getType()).isEqualTo(int.class);
		assertThat(property.forAllParameters.get(0).getDeclaredAnnotation(ForAll.class)).isNotNull();
		assertThat(property.forAllParameters.get(1).getType()).isEqualTo(String.class);
		assertThat(property.forAllParameters.get(1).getDeclaredAnnotation(ForAll.class)).isNotNull();
	}

	@Example
	void withNoParamsAndVoidResult() {
		PropertyMethodDescriptor descriptor = createDescriptor("propWithVoidResult", 42L, 11, 5, ShrinkingMode.OFF);
		CheckedProperty property = factory.fromDescriptor(descriptor, new PropertyExamples());

		assertThat(property.forAllParameters).size().isEqualTo(0);

		List<Object> noArgs = Arrays.asList();
		assertThat(property.forAllPredicate.test(noArgs)).isTrue();
	}

	private PropertyMethodDescriptor createDescriptor(String methodName, long seed, int tries, int maxDiscardRatio,
			ShrinkingMode shrinking) {
		UniqueId uniqueId = UniqueId.root("test", "i dont care");
		Method method = TestHelper.getMethod(PropertyExamples.class, methodName);
		return new PropertyMethodDescriptor(uniqueId, method, PropertyExamples.class, seed, tries, maxDiscardRatio, shrinking,
				ReportingMode.MINIMAL);
	}

	private static class PropertyExamples {

		@Property
		boolean prop(@ForAll int anInt, @ForAll String aString) {
			return anInt == 1 && aString.equals("test");
		}

		@Property
		boolean propWithUnboundParams(int otherInt, @ForAll int anInt, @ForAll String aString, String otherString) {
			return true;
		}

		@Property(tries = 42)
		boolean propWithTries(@ForAll int anInt, @ForAll String aString) {
			return true;
		}

		@Property(seed = 4242L)
		boolean propWithSeed(@ForAll int anInt, @ForAll String aString) {
			return true;
		}

		@Property
		void propWithVoidResult() {
		}

	}
}
