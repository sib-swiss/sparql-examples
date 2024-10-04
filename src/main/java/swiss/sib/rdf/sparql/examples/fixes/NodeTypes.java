package swiss.sib.rdf.sparql.examples.fixes;

import java.util.function.BiConsumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import swiss.sib.rdf.sparql.examples.Fixer.Fixed;

public class NodeTypes {
	private static final Pattern DATE_TIME_THAT_SHOULD_BE_DATE = Pattern.compile("(\"\\d{4}-\\d{2}-\\d{2}\"\\^\\^xsd:date)Time");
	private static final Pattern DAT_THAT_SHOULD_BE_DATE = Pattern.compile("(\"\\d{4}-\\d{2}-\\d{2}\"\\^\\^xsd:dat)[\\s,\\.]");
	
	public static Fixed fix(Fixed prior, String queryIri) {
		String original;
		if (prior.changed()) {
			original = prior.fixed();
		} else {
			original = prior.original();
		}
		try {
			StringBuilder changed = new StringBuilder(original);
			fix(changed, DATE_TIME_THAT_SHOULD_BE_DATE, NodeTypes::removeTimeAtEnd);
			fix(changed, DAT_THAT_SHOULD_BE_DATE, NodeTypes::addE);
			if (changed.length() != original.length())
				return new Fixed(true, changed.toString(), prior.original());
		} catch (IllegalArgumentException e) {
			System.out.println("Could not fix federation metadata in:" + queryIri);
		}
		return prior;
	}

	public static void fix(StringBuilder changed, Pattern pattern, BiConsumer<StringBuilder, Matcher> fixer) {
		Matcher matcher = pattern.matcher(changed);
		
		while (matcher.find()) {
			fixer.accept(changed, matcher);
			
			matcher.reset(changed);
		}
	}

	public static StringBuilder removeTimeAtEnd(StringBuilder changed, Matcher matcher) {
		return changed.delete(matcher.end()-5, matcher.end());
	}
	
	public static StringBuilder addE(StringBuilder changed, Matcher matcher) {
		return changed.insert(matcher.end()-1, "e");
	}
}
