package examples.docs.lifecycle;

import org.junit.platform.engine.*;

import net.jqwik.api.lifecycle.*;
import net.jqwik.api.lifecycle.LifecycleStorage.*;

public class Hook1WithGlobalStore implements AroundPropertyHook {

	Store<Integer> counter = LifecycleStorage.global("countProperties", Integer.class).perRun().init(0);

	@Override
	public TestExecutionResult aroundProperty(PropertyLifecycleContext context, PropertyExecutor property) throws Throwable {
		counter.update(count -> count++);
		return property.execute();
	}
}
