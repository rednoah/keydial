package ntu.csie.keydial;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.stream.Collectors;

public class Prediction {

	TreeMap<String, Integer> corpus = new TreeMap<String, Integer>(String.CASE_INSENSITIVE_ORDER);

	public List<String> complete(String s, int limit) {
		String from = s;
		String to = s.substring(0, s.length() - 1) + (char) (s.charAt(s.length() - 1) + 1);

		NavigableMap<String, Integer> subMap = corpus.subMap(from, true, to, true);

		return subMap.entrySet().stream().sorted((o2, o1) -> o1.getValue().compareTo(o2.getValue())).limit(limit).map((m) -> m.getKey()).collect(Collectors.toList());
	}

	public Prediction() {
		readFile("engus_inclusion_utf8.txt");
		readFile("engus_corpus_utf8.txt");
	}

	public void readFile(String path) {
		try {
			Files.lines(Paths.get(path), StandardCharsets.UTF_8).forEach((s) -> {
				corpus.compute(s.trim(), (k, c) -> c == null ? 1 : c + 1);
			});
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

}
