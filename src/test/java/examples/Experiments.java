package examples;

import java.util.*;

import net.jqwik.api.*;

class Experiments {

	@Property(generation = GenerationMode.RANDOMIZED)
	void test(@ForAll Random random) {
		List aList = Arrays.asList(1, 2, 3, 4, 5);
		Collections.shuffle(aList, random);
		System.out.println(aList);
	}
}
