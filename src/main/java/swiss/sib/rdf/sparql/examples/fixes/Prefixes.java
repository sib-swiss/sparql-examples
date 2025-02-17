package swiss.sib.rdf.sparql.examples.fixes;

import java.util.Map;
import java.util.regex.Pattern;

import swiss.sib.rdf.sparql.examples.Fixer.Fixed;

/*
 * A class to fix prefixes in SPARQL queries.
 */
 public class Prefixes {
	private Prefixes() {
		
	}

	/**
	 * A method to fix missing prefixes in a SPARQL query.
	 * 
	 * @param original  The original query.
	 * @param prefixes2 The prefixes to add.
	 * @return A Fixed object.
	 */
	public static Fixed fixMissingPrefixes(String original, Map<String, String> prefixes2) {
		StringBuilder changed = new StringBuilder(original);
		for (Map.Entry<String, String> entry : prefixes2.entrySet()) {
			Pattern prefix = Pattern.compile("(^|\\W+)(?i:prefix)(\\W+)" + entry.getKey() + ":");
			Pattern prefixInUse = Pattern.compile("(^|\\W+)" + entry.getKey() + ":");
			if (!prefix.matcher(original).find() && prefixInUse.matcher(original).find()) {
				changed.insert(0, "PREFIX " + entry.getKey() + ": <" + entry.getValue() + ">\n");
			}
		}
		if (changed.length() == original.length())
			return new Fixed(false, null, original);
		else
			return new Fixed(true, changed.toString(), original);
	}
}
