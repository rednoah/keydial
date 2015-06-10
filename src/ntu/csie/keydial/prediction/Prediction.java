package ntu.csie.keydial.prediction;

import static java.util.Collections.*;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Prediction {

	static final Prediction instance = new Prediction();

	public static Prediction getInstance() {
		return instance;
	}

	static final String OMEGA = "Ω";

	TreeMap<String, Integer> corpus = new TreeMap<String, Integer>(String.CASE_INSENSITIVE_ORDER);

	Pattern SENTENCE = Pattern.compile("^.*\\w+\\s?$", Pattern.CASE_INSENSITIVE);
	Pattern SPACE = Pattern.compile("\\s+");
	Pattern ALPHA = Pattern.compile("[A-Z]+", Pattern.CASE_INSENSITIVE);

	public String getLastWord(String s) {
		if (SENTENCE.matcher(s).matches()) {
			String[] input = SPACE.split(s);
			if (input.length > 0) {
				String word = input[input.length - 1];
				return s.substring(s.lastIndexOf(word));
			}
		}
		return "";
	}

	public List<String> completeSentence(String s, int limit) {
		String prefix = getLastWord(s);
		if (prefix.length() > 0) {
			List<String> options = completeWord(prefix, limit);
			if (options.size() > 0) {
				return options;
			}
		}
		return ALPHA.matcher(prefix).matches() ? singletonList(prefix) : emptyList();
	}

	public List<String> completeWord(String s, int limit) {
		Map<String, Integer> subMap = corpus.subMap(s, s + OMEGA);

		Stream<String> stream = subMap.entrySet().stream().sorted((o2, o1) -> o1.getValue().compareTo(o2.getValue())).limit(limit).map((m) -> m.getKey());

		return stream.collect(Collectors.toList());
	}

	public Set<String> guessNextCharacter(String s, int limit) {
		String prefix = getLastWord(s);
		if (prefix.length() > 0) {
			List<String> options = completeWord(prefix, limit);
			if (options.size() > 0) {
				Set<String> letters = new HashSet<String>();
				for (String it : options) {
					if (it.length() > prefix.length()) {
						String letter = it.substring(prefix.length(), prefix.length() + 1).toUpperCase();
						if (ALPHA.matcher(letter).matches()) {
							letters.add(letter);
						}
					}
				}
				return letters;
			}
		}
		return emptySet();
	}

	public Prediction() {
		readFile("engus_inclusion_utf8.txt");
		readFile("engus_corpus_utf8.txt");
	}

	public void readFile(String path) {
		try (Scanner s = new Scanner(getClass().getResourceAsStream(path), "UTF-8")) {
			while (s.hasNextLine()) {
				corpus.compute(s.nextLine().trim(), (k, c) -> c == null ? 1 : c + 1);
			}
		}
	}

}
