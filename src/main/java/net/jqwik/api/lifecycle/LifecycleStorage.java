package net.jqwik.api.lifecycle;

import java.util.function.*;

import net.jqwik.execution.lifecycle.*;

/**
 * Experimental feature. Not ready for public usage yet.
 */
public class LifecycleStorage {

	public static <T> StoreFactory<T> global(Object key, Class<T> clazz) {
		return new GlobalStoreFactory<>(key);
	}

	public interface Store<T> {
		T get();

		Store<T> init(T value);

		void update(Function<T, T> updater);
	}

	public interface StoreFactory<T> {
		Store<T> perRun();
	}
}
