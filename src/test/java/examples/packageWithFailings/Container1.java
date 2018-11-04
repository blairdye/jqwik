package examples.packageWithFailings;

import java.io.*;

import net.jqwik.api.*;
import net.jqwik.api.constraints.*;

public class Container1 {
	@Property
	boolean succeed1() {
		return true;
	}


	@Property
	boolean fail1(@ForAll @AlphaChars String test, @ForAll int anInt, @ForAll("my") MyObject anObject) {
		return test.length() < 2 && anInt < 3;
	}

	@Provide
	Arbitrary<MyObject> my() {
		return Arbitraries.constant(new MyObject());
	}

	static class MyObject implements Serializable {
		@Override
		public String toString() {
			return "MyObject";
		}
	}
}
