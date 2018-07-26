package examples.docs.lifecycle;

import org.junit.platform.engine.*;

import net.jqwik.api.lifecycle.*;
import net.jqwik.api.lifecycle.LifecycleStorage.*;

public class Hook2WithGlobalStore implements AroundPropertyHook {

	Store<Integer> counter = LifecycleStorage.global("countProperties", Integer.class).perRun().init(0);

	@Override
	public TestExecutionResult aroundProperty(PropertyLifecycleContext context, PropertyExecutor property) throws Throwable {
		try {
			return property.execute();
		} finally {
			System.out.println("Properties called: " + counter.get());
		}
	}
}
