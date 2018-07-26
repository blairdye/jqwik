package net.jqwik.execution.lifecycle;

import java.util.*;
import java.util.concurrent.*;

import org.junit.platform.engine.*;

import net.jqwik.api.lifecycle.LifecycleStorage.*;

public class Stores {

	public static TestDescriptor currentEngineDescriptor = null;

	static Map<StoreKey, Store> stores = new ConcurrentHashMap<>();

	static Store getPerRun(Object key) {
		StoreKey storeKey = new StoreKey(key, currentEngineDescriptor);
		return stores.computeIfAbsent(storeKey, s -> new StoreImpl());
	}

	static class StoreKey {
		private final Object key;
		private final TestDescriptor scope;

		public StoreKey(Object key, TestDescriptor scope) {
			this.key = key;
			this.scope = scope;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;

			StoreKey storeKey = (StoreKey) o;

			if (!key.equals(storeKey.key)) return false;
			return scope.equals(storeKey.scope);
		}

		@Override
		public int hashCode() {
			int result = key.hashCode();
			result = 31 * result + scope.hashCode();
			return result;
		}
	}
}
