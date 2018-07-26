package net.jqwik.execution.lifecycle;

import java.util.function.*;

import net.jqwik.api.lifecycle.*;

public class GlobalStoreFactory<T> implements LifecycleStorage.StoreFactory<T> {
	private final Object key;

	public GlobalStoreFactory(Object key) {
		this.key = key;
	}

	@SuppressWarnings("unchecked")
	@Override
	public LifecycleStorage.Store<T> perRun() {
		return new LifecycleStorage.Store<T>() {
			@Override
			public T get() {
				return getStore().get();
			}

			@Override
			public LifecycleStorage.Store<T> init(T value) {
				return getStore().init(value);
			}

			@Override
			public void update(Function<T, T> updater) {
				getStore().update(updater);
			}

			private LifecycleStorage.Store<T> getStore() {
				return (LifecycleStorage.Store<T>)Stores.getPerRun(key);
			}

		};
	}
}
