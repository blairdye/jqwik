package examples.docs.lifecycle;

import net.jqwik.api.*;
import net.jqwik.api.lifecycle.*;

@AddLifecycleHook(Hook1WithGlobalStore.class)
@AddLifecycleHook(Hook2WithGlobalStore.class)
public class TestsUsingGlobalStoreHooks {

	@Property
	void property1() {}

	@Property
	void property2() {}

	@Property
	void property3() {}

	@Property
	void property4() {}

	@Property
	void property5() {}

	@Property
	void property6() {}
}
