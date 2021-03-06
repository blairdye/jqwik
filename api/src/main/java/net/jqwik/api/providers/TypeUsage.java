package net.jqwik.api.providers;

import java.lang.annotation.*;
import java.lang.reflect.*;
import java.util.*;

import org.apiguardian.api.*;

import net.jqwik.api.*;

import static org.apiguardian.api.API.Status.*;

/**
 * An instance of {@code TypeUsage} describes the information available for parameter or return types.
 * The class is supposed to relieve its users from all the intricacies of the Java reflection API.
 * Doing that it will resolve meta annotations, repeated annotations as well as annotated type parameters.
 * <p>
 * {@code TypeUsage} provides access to:
 * <ul>
 * <li>the native type of an object</li>
 * <li>the component type (if it's an array)</li>
 * <li>the type parameters (again as instances of {@code TypeUsage})</li>
 * <li>the annotations (if the object is derived from a parameter)</li>
 * <li>methods to test for compatibility of types that do also handle compatibility
 * between raw types and boxed type</li>
 * </ul>
 * <p>
 * Within the public API {@code TypeUsage} is used in two places:
 * <ul>
 * <li>@see {@link ArbitraryProvider}</li>
 * <li>@see {@link Arbitraries#defaultFor(Class, Class[])}</li>
 * </ul>
 */
@API(status = MAINTAINED, since = "1.0")
public interface TypeUsage {

	@API(status = INTERNAL)
	abstract class TypeUsageFacade {
		private static TypeUsageFacade implementation;

		static  {
			implementation = FacadeLoader.load(TypeUsageFacade.class);
		}

		public abstract TypeUsage of(Class<?> type, TypeUsage... typeParameters);
		public abstract TypeUsage wildcard(TypeUsage upperBound);
		public abstract TypeUsage forType(Type type);
	}

	static TypeUsage of(Class<?> type, TypeUsage... typeParameters) {
		return TypeUsageFacade.implementation.of(type, typeParameters);
	}

	static TypeUsage wildcard(TypeUsage upperBound) {
		return TypeUsageFacade.implementation.wildcard(upperBound);
	}

	static TypeUsage forType(Type type) {
		return TypeUsageFacade.implementation.forType(type);
	}

	/**
	 * Return the raw type which is usually the class or interface you see in a parameters or return values
	 * specification.
	 * <p>
	 * A raw type always exists.
	 */
	Class<?> getRawType();

	/**
	 * Return upper bounds if a generic type is a wildcard or type variable.
	 * {@code TypeUsage.of(Object.class)} is always included.
	 */
	List<TypeUsage> getUpperBounds();

	/**
	 * Return lower bounds if a generic type is a wildcard.
	 */
	List<TypeUsage> getLowerBounds();

	/**
	 * Return true if a generic type is a wildcard.
	 */
	boolean isWildcard();

	/**
	 * Return true if a generic type is a wildcard.
	 */
	boolean isTypeVariable();

	/**
	 * Return true if a generic type is a type variable or a wildcard.
	 */
	boolean isTypeVariableOrWildcard();

	/**
	 * Return the type arguments of a generic type in the order of there appearance in a type's declaration.
	 */
	List<TypeUsage> getTypeArguments();

	/**
	 * Check if an instance is of a specific raw type
	 * <p>
	 * Most of the time this is what you want to do when checking for applicability of a
	 * {@linkplain ArbitraryProvider}.
	 */
	boolean isOfType(Class<?> aRawType);

	/**
	 * Check if an instance can be assigned to another {@code TypeUsage} instance.
	 */
	boolean canBeAssignedTo(TypeUsage targetType);

	/**
	 * Return true if a type has any type arguments itself.
	 */
	boolean isGeneric();

	/**
	 * Return true if a type is an {@code enum} type.
	 */
	boolean isEnum();

	/**
	 * Return true if a type is an array type.
	 */
	boolean isArray();

	/**
	 * Return all annotations of a parameter (or an annotated type argument).
	 * <p>
	 * This list already contains all meta annotations, repeated annotations and annotations
	 * from annotated type arguments. Thus, it does much more than the usual Java reflection API.
	 */
	List<Annotation> getAnnotations();

	/**
	 * Return an {@code Optional} of the first instance of a specific {@code annotationType}
	 * if there is one (directly or indirectly through meta-annotations).
	 */
	<A extends Annotation> Optional<A> findAnnotation(Class<A> annotationType);

	/**
	 * Return true if the current instance is annotated (directly or indirectly through meta-annotations)
	 * with a specific {@code annotationType}.
	 */
	<A extends Annotation> boolean isAnnotated(Class<A> annotationType);

	/**
	 * Check if a given {@code providedClass} is assignable from this generic type.
	 */
	boolean isAssignableFrom(Class<?> providedClass);

	/**
	 * Return an {@code Optional} of an array's component type - if it is an array.
	 */
	Optional<TypeUsage> getComponentType();

}
