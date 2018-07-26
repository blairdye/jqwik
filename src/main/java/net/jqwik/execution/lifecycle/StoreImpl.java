package net.jqwik.execution.lifecycle;

import java.util.function.*;

import net.jqwik.api.lifecycle.LifecycleStorage.*;

class StoreImpl<T> implements Store<T> {

	private T value;

	@Override
	public synchronized T get() {
		return value;
	}

	@Override
	public synchronized Store<T> init(T value) {
		this.value = value;
		return this;
	}

	@Override
	public synchronized void update(Function<T, T> updater) {
		this.value = updater.apply(this.value);
	}
}
