package net.jqwik.engine.support;

import java.io.*;
import java.lang.annotation.*;
import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;
import java.util.function.*;
import java.util.stream.*;

import org.junit.platform.commons.support.*;

import net.jqwik.api.lifecycle.*;
import net.jqwik.api.providers.*;
import net.jqwik.engine.discovery.predicates.*;

import static java.util.stream.Collectors.*;

public class JqwikReflectionSupport {

	private final static IsTopLevelClass isTopLevelClass = new IsTopLevelClass();

	public static Stream<Object> streamInstancesFromInside(Object inner) {
		return addInstances(inner, new ArrayList<>()).stream();
	}

	private static List<Object> addInstances(Object inner, List<Object> instances) {
		instances.add(inner);
		Optional<Object> outer = getOuterInstance(inner);
		outer.ifPresent(o -> addInstances(o, instances));
		return instances;
	}

	private static Optional<Object> getOuterInstance(Object inner) {
		// This is risky since it depends on the name of the field which is nowhere guaranteed
		// but has been stable so far in all JDKs

		return Arrays
			.stream(inner.getClass().getDeclaredFields())
			.filter(field -> field.getName().startsWith("this$"))
			.findFirst()
			.map(field -> {
				try {
					return makeAccessible(field).get(inner);
				} catch (SecurityException | IllegalArgumentException | IllegalAccessException ex) {
					return Optional.empty();
				}
			});
	}

	private static <T extends AccessibleObject> T makeAccessible(T object) {
		if (!object.isAccessible()) {
			object.setAccessible(true);
		}
		return object;
	}

	/**
	 * Create instance of a class that can potentially be a non static inner class
	 *
	 * @param <T>   The type of the instance to create
	 * @param clazz The class to instantiate
	 * @return the newly created instance
	 */
	public static <T> T newInstanceWithDefaultConstructor(Class<T> clazz) {
		if (isTopLevelClass.test(clazz) || ModifierSupport.isStatic(clazz))
			return ReflectionSupport.newInstance(clazz);
		Object parentInstance = newInstanceWithDefaultConstructor(clazz.getDeclaringClass());
		return ReflectionSupport.newInstance(clazz, parentInstance);
	}

	/**
	 * Create instance of a class that can potentially be a non static inner class
	 * and its outer instance might be {@code context}
	 *
	 * @param <T>   The type of the instance to create
	 * @param clazz The class to instantiate
	 * @param context The potential context instance
	 * @return the newly created instance
	 */
	public static <T> T newInstanceInTestContext(Class<T> clazz, Object context) {
		if (isTopLevelClass.test(clazz) || ModifierSupport.isStatic(clazz))
			return ReflectionSupport.newInstance(clazz);
		Class<?> outerClass = clazz.getDeclaringClass();
		Object parentInstance = outerClass.equals(context.getClass()) ?
			context : newInstanceWithDefaultConstructor(outerClass);
		return ReflectionSupport.newInstance(clazz, parentInstance);
	}

	/**
	 * Find all {@linkplain Method methods} as in ReflectionSupport.findMethods(..) but also use outer classes to look for
	 * methods.
	 *
	 * @param clazz         The class in which you start the search
	 * @param predicate     The condition to check for all candidate methods
	 * @param traversalMode Traverse hierarchy up or down. Determines the order in resulting list.
	 * @return List of found methods
	 */
	public static List<Method> findMethodsPotentiallyOuter(
		Class<?> clazz, Predicate<Method> predicate,
		HierarchyTraversalMode traversalMode
	) {

		List<Method> foundMethods = new ArrayList<>();
		foundMethods.addAll(ReflectionSupport.findMethods(clazz, predicate, traversalMode));
		Class<?> searchClass = clazz;
		while (searchClass.getDeclaringClass() != null) {
			searchClass = searchClass.getDeclaringClass();
			foundMethods.addAll(ReflectionSupport.findMethods(searchClass, predicate, traversalMode));
		}
		return foundMethods;
	}

	/**
	 * Invoke the supplied {@linkplain Method method} as in ReflectionSupport.invokeMethod(..) but potentially use the outer
	 * instance if the method belongs to the outer instance of an object.
	 *
	 * @param method The method to invoke
	 * @param target The object to invoke the method on
	 * @param args   The arguments of the method invocation
	 * @return Result of method invocation if there is one, otherwise null
	 */
	public static Object invokeMethodPotentiallyOuter(Method method, Object target, Object... args) {
		if (method.getDeclaringClass().isAssignableFrom(target.getClass())) {
			return ReflectionSupport.invokeMethod(method, target, args);
		} else {
			if (target.getClass().getDeclaringClass() != null) {
				Optional<Object> newTarget = getOuterInstance(target);
				if (newTarget.isPresent()) {
					return invokeMethodPotentiallyOuter(method, newTarget.get(), args);
				}
			}
			throw new IllegalArgumentException(String.format("Method [%s] cannot be invoked on target [%s].", method, target));
		}
	}

	public static Set<Path> getAllClasspathRootDirectories() {
		// TODO: This is quite a hack, since sometimes the classpath is quite different.
		// Especially under Java >=9's module system this will probably no longer work.
		String classpath = System.getProperty("java.class.path");
		return Arrays.stream(classpath.split(File.pathSeparator)) //
					 .map(Paths::get).filter(Files::isDirectory) //
					 .collect(toSet());
	}

	public static MethodParameter[] getMethodParameters(Method method, Class<?> containerClass) {

		List<MethodParameter> list = new ArrayList<>();
		Parameter[] parameters = method.getParameters();
		GenericsClassContext containerClassContext = GenericsSupport.contextFor(containerClass);

		for (int i = 0; i < parameters.length; i++) {
			Parameter parameter = parameters[i];
			TypeResolution resolution = containerClassContext.resolveParameter(parameter);
			MethodParameter methodParameter = new MethodParameter(parameter, resolution);
			list.add(methodParameter);
		}
		return list.toArray(new MethodParameter[parameters.length]);
	}

	public static Optional<Method> findGeneratorMethod(
		String generatorToFind,
		Class<?> containerClass,
		Class<? extends Annotation> requiredGeneratorAnnotation,
		Function<Method, String> generatorNameSupplier,
		TypeUsage targetType
	) {
		List<Method> creators = findMethodsPotentiallyOuter(
			containerClass,
			isGeneratorMethod(targetType, requiredGeneratorAnnotation),
			HierarchyTraversalMode.BOTTOM_UP
		);
		return creators.stream().filter(generatorMethod -> {
			String generatorName = generatorNameSupplier.apply(generatorMethod);
			if (generatorName.isEmpty()) {
				generatorName = generatorMethod.getName();
			}
			return generatorName.equals(generatorToFind);
		}).findFirst();
	}

	public static Predicate<Method> isGeneratorMethod(TypeUsage targetType, Class<? extends Annotation> requiredAnnotation) {
		return method -> {
			if (!method.isAnnotationPresent(requiredAnnotation)) {
				return false;
			}
			TypeUsage generatorReturnType = TypeUsage.forType(method.getAnnotatedReturnType().getType());
			return generatorReturnType.canBeAssignedTo(targetType);
		};
	}

	public static boolean isInnerClass(Class<? extends LifecycleHook> hookClass) {
		return hookClass.isMemberClass() && !ModifierSupport.isStatic(hookClass);
	}

	/**
	 * Throw the supplied {@link Throwable}, <em>masked</em> as an
	 * unchecked exception.
	 *
	 * @param t type returns a throwable to make usage simpler
	 */
	public static <T> T throwAsUncheckedException(Throwable t) {
		JqwikReflectionSupport.throwAs(t);

		// Will never get here
		return null;
	}

	@SuppressWarnings("unchecked")
	private static <T extends Throwable> void throwAs(Throwable t) throws T {
		throw (T) t;
	}

}
