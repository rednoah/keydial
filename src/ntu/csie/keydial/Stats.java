package ntu.csie.keydial;

import static java.util.Collections.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class Stats {

	public static final Stats stats = new Stats();

	private String file = "stats.tsv";
	private String user;

	private String output;
	private List<String> record;
	private Instant start;
	private Instant end;

	public void reset() {
		output = null;
		record = new ArrayList<String>();
		start = null;
		end = null;
	}

	public void setUser(String s) {
		user = s;
		System.out.println("USER = " + user);

		startRecord();
	}

	public void startRecord() {
		reset();
		start = Instant.now();
		System.out.println("START_RECORD: " + start);
	}

	public void record(String s) {
		record.add(s);
	}

	public void endRecord(String s) {
		output = s;
		end = Instant.now();

		// record
		Map<String, Object> stats = new LinkedHashMap<String, Object>();
		stats.put("user", user);
		stats.put("date", start);
		stats.put("output", output);
		stats.put("entered", record.stream().filter((it) -> !Watch.CONTROL_KEYS.contains(it)).count());
		stats.put("deleted", record.stream().filter((it) -> it.equals(Watch.BACKSPACE)).count());
		stats.put("duration", Duration.between(start, end).toMillis());
		stats.put("cps", output.length() / ((double) Duration.between(start, end).toMillis() / 1000));
		stats.put("wpm", ((double) output.length() / 5) / ((double) Duration.between(start, end).toMillis() / 60000));
		stats.put("record", record);

		System.out.println("STATS: " + stats);
		try {
			Files.write(Paths.get(file), singleton(stats.values().stream().map(Objects::toString).collect(Collectors.joining("\t"))), StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
