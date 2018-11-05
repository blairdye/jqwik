package net.jqwik.execution;

import java.io.*;
import java.util.*;

import org.junit.platform.engine.*;
import org.junit.platform.engine.reporting.*;

import net.jqwik.api.*;
import net.jqwik.descriptor.*;
import net.jqwik.properties.*;
import net.jqwik.recording.*;
import net.jqwik.support.*;

public class RecordingExecutionListener implements EngineExecutionListener {

	private final TestRunRecorder recorder;
	private final EngineExecutionListener listener;
	private final boolean useJunitPlatformReporter;
	private Map<TestDescriptor, String> seeds = new IdentityHashMap<>();

	RecordingExecutionListener(TestRunRecorder recorder, EngineExecutionListener listener, boolean useJunitPlatformReporter) {
		this.recorder = recorder;
		this.listener = listener;
		this.useJunitPlatformReporter = useJunitPlatformReporter;
	}

	@Override
	public void dynamicTestRegistered(TestDescriptor testDescriptor) {
		listener.dynamicTestRegistered(testDescriptor);
	}

	@Override
	public void executionSkipped(TestDescriptor testDescriptor, String reason) {
		listener.executionSkipped(testDescriptor, reason);
	}

	@Override
	public void executionStarted(TestDescriptor testDescriptor) {
		listener.executionStarted(testDescriptor);
	}

	@Override
	public void executionFinished(TestDescriptor testDescriptor, TestExecutionResult testExecutionResult) {
		recordTestRun(testDescriptor, testExecutionResult);
		if (testExecutionResult.getStatus() == TestExecutionResult.Status.FAILED && testDescriptor instanceof PropertyMethodDescriptor) {
			PropertyMethodDescriptor methodDescriptor = (PropertyMethodDescriptor) testDescriptor;
			UniqueId id = methodDescriptor.getUniqueId().append("failed", "shrinkedExample");
			PropertyMethodDescriptor failingDescriptor = new PropertyMethodDescriptor(
				id, methodDescriptor.getTargetMethod(), methodDescriptor.getContainerClass(), methodDescriptor.getConfiguration()) {

			};
			testDescriptor.addChild(failingDescriptor);
			listener.dynamicTestRegistered(failingDescriptor);
			listener.executionStarted(failingDescriptor);
			listener.executionFinished(failingDescriptor, TestExecutionResult.failed(null));
		}

		listener.executionFinished(testDescriptor, testExecutionResult);
	}

	private void recordTestRun(TestDescriptor testDescriptor, TestExecutionResult testExecutionResult) {
		String seed = seeds.computeIfAbsent(testDescriptor, ignore -> Property.SEED_NOT_SET);
		TestRun run = new TestRun(testDescriptor.getUniqueId(), testExecutionResult.getStatus(), seed);
		recorder.record(run);
	}

	@Override
	public void reportingEntryPublished(TestDescriptor testDescriptor, ReportEntry entry) {
		rememberSeed(testDescriptor, entry);

		if (entry.getKeyValuePairs().containsKey("sample-serialized")) {

			String serialized = entry.getKeyValuePairs().get("sample-serialized");
			System.out.println("SERIALIZED: " + JqwikStringSupport.displayString(fromString(serialized)));

			return;
		}

		if (useJunitPlatformReporter) {
			listener.reportingEntryPublished(testDescriptor, entry);
		} else {
			ReportEntrySupport.printToStdout(testDescriptor, entry);
		}
	}

	private static List fromString( String s ) {
		try {
			byte[] data = Base64.getDecoder().decode(s);
			ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(data));
			List o = (List) ois.readObject();
			ois.close();
			return o;
		} catch (IOException io) {
			io.printStackTrace();
			return null;
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			return null;
		}
	}

	private void rememberSeed(TestDescriptor testDescriptor, ReportEntry entry) {
		Map<String, String> entries = entry.getKeyValuePairs();
		if (entries.containsKey(CheckResultReportEntry.SEED_REPORT_KEY)) {
			String reportedSeed = getReportedSeed(entries);
			seeds.put(testDescriptor, reportedSeed);
		}
	}

	private String getReportedSeed(Map<String, String> entries) {
		String reportedSeed = Property.SEED_NOT_SET;
		try {
			reportedSeed = entries.get(CheckResultReportEntry.SEED_REPORT_KEY);
		} catch (NumberFormatException ignore) {
		}
		return reportedSeed;
	}
}
