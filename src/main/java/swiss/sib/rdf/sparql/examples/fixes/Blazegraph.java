package swiss.sib.rdf.sparql.examples.fixes;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


import org.eclipse.rdf4j.query.parser.sparql.SPARQLParser;
import org.openrdf.query.MalformedQueryException;

import com.bigdata.bop.BOp;
import com.bigdata.rdf.sail.sparql.Bigdata2ASTSPARQLParser;
import com.bigdata.rdf.sail.sparql.BigdataParsedQuery;
import com.bigdata.rdf.sparql.ast.GraphPatternGroup;
import com.bigdata.rdf.sparql.ast.IGroupMemberNode;
import com.bigdata.rdf.sparql.ast.JoinGroupNode;
import com.bigdata.rdf.sparql.ast.NamedSubqueriesNode;
import com.bigdata.rdf.sparql.ast.NamedSubqueryInclude;
import com.bigdata.rdf.sparql.ast.NamedSubqueryRoot;
import com.bigdata.rdf.sparql.ast.QueryHints;
import com.bigdata.rdf.sparql.ast.QueryRoot;
import com.bigdata.rdf.sparql.ast.QueryType;
import com.bigdata.rdf.sparql.ast.SubqueryRoot;

import swiss.sib.rdf.sparql.examples.Fixer;
import swiss.sib.rdf.sparql.examples.Fixer.Fixed;


public class Blazegraph {
	private Blazegraph() {
		
	}

	private static final Pattern HINT_REMOVE = Pattern.compile("(hint:([^.;,\\}])+[\\.;,\\}]?)");
	public static Fixer.Fixed fixBlazeGraphHints(Fixed prior, String queryIriStr, Path fileStr) {
		String original;
		if (prior.changed()) {
			original = prior.fixed();
		} else {
			original = prior.original();
		}
		if (original.contains("hint:")) {
			SPARQLParser sparqlParser = new SPARQLParser();
			try {
				sparqlParser.parseQuery(original, QueryHints.NAMESPACE);
			} catch (org.eclipse.rdf4j.query.MalformedQueryException e) {
				String testQ = "PREFIX hint:<" + QueryHints.NAMESPACE + ">\n" + original;
				try {
					sparqlParser.parseQuery(testQ, QueryHints.NAMESPACE);
					// we now know we have hints that are in the query and we need to remove them.
					StringBuilder temp = new StringBuilder(original);
					Matcher matcher = HINT_REMOVE.matcher(temp);
					while (matcher.find()) {
						int gl = matcher.group(1).length();
						if (matcher.group(1).endsWith("}")) {
							temp.delete(matcher.start(), matcher.start() + gl - 1);	
						} else {
							temp.delete(matcher.start(), matcher.start() + gl);
						}
						matcher.reset(temp);
					}
					return new Fixed(true, temp.toString(), original);
	
				} catch (org.eclipse.rdf4j.query.MalformedQueryException e2) {
					return prior;
				}
			}
		}
		return prior;
	}

	public static Fixed fixBlazeGraphIncludeWith(Fixed prior, String queryIriStr, Path fileStr) {
		String toFix;
		if (prior.changed()) {
			toFix = prior.fixed();
		} else {
			toFix = prior.original();
		}
		Bigdata2ASTSPARQLParser blzp = new Bigdata2ASTSPARQLParser();
		try {
			BigdataParsedQuery pq = blzp.parseQuery(toFix, "https://example.org/");
	
			QueryRoot origAst = pq.getASTContainer().getOriginalAST();
			NamedSubqueriesNode nsq = origAst.getNamedSubqueries();
			if (nsq != null) {
				StringBuilder sb = new StringBuilder(toFix);
				attachOriginalInclude(nsq, sb);
				for (int i = 0; i < nsq.size(); i++) {
					NamedSubqueryRoot bOp = (NamedSubqueryRoot) nsq.get(i);
					for (int j = 0; j < nsq.size(); j++) {
						if (j != i) {
							NamedSubqueryRoot bOpO = (NamedSubqueryRoot) nsq.get(j);
							BOp fixed = replaceIncludes(bOp.getGraphPattern(), bOpO, sb);
						}
					}
				}
				
				
				pq = blzp.parseQuery(sb.toString(), "https://example.org/");
	
				origAst = pq.getASTContainer().getOriginalAST();
				nsq = origAst.getNamedSubqueries();
				List<String> toRemove = attachOriginalInclude(nsq, sb);
				for (int i = 0; i < nsq.size(); i++) {
					NamedSubqueryRoot bOp = (NamedSubqueryRoot) nsq.get(i);
	
					origAst.clearProperty("namedSubqueries");
					
					
					BOp fixed = replaceIncludes(origAst, bOp, sb);
				}
				String withoutInclude = sb.toString();
				for (String remove:toRemove) {
					withoutInclude = withoutInclude.replace(remove, "");
				}
				return new Fixed(true, withoutInclude, toFix);
			}
			return prior;
		} catch (MalformedQueryException e) {
			System.out.println("Failed to fix include " + queryIriStr + " in " + fileStr);
			return prior;
		}
	}

	public static Fixed fixBlazeGraph(Fixed original, String queryIriStr, Path fileStr) {
		Fixed fix = fixBlazeGraphIncludeWith(original, queryIriStr, fileStr);
		return fixBlazeGraphHints(fix, queryIriStr, fileStr);
	}
	
	public static List<String> attachOriginalInclude(NamedSubqueriesNode nsq, StringBuilder sb) {
		List<String> originalIncludes = new ArrayList<>();
		for (int i = 0; i < nsq.size(); i++) {
			NamedSubqueryRoot bOp = (NamedSubqueryRoot) nsq.get(i);
			Pattern asP = Pattern.compile(bOp.getName() + "\\s");
			Matcher matcher = asP.matcher(sb);

			if (matcher.find()) {
				int startAsP = matcher.start();
				int lastClosingBracket = sb.lastIndexOf("}", startAsP);
				int openingBracket = findBlockInMatchingBrackets(sb, lastClosingBracket - 1);
				int withStart = findWithJustBeforeOpenBracket(sb, openingBracket);
				String toInclude = sb.substring(openingBracket, lastClosingBracket + 1);
				bOp.annotations().put("original", toInclude);
				originalIncludes.add(sb.substring(withStart, matcher.end()));
			}
		}
		return originalIncludes;
	}
	
	private static final Pattern WITH = Pattern.compile("with", Pattern.CASE_INSENSITIVE);

	public static BOp replaceIncludes(BOp astContainer, BOp bOp, StringBuilder blazeGraphIncludeExample) {
		return switch (astContainer) {
		case QueryRoot qr -> {
			var nq = new QueryRoot(qr);
			nq.setGraphPattern((GraphPatternGroup<IGroupMemberNode>) replaceIncludes(nq.getGraphPattern(), bOp,
					blazeGraphIncludeExample));
			yield nq;
		}
		case SubqueryRoot sqb -> {
			replaceIncludes(sqb.getGraphPattern(), bOp, blazeGraphIncludeExample);
			yield sqb;
		}
		case NamedSubqueryRoot sqb -> {
			replaceIncludes(sqb.getGraphPattern(), bOp, blazeGraphIncludeExample);
			yield sqb;
		}
		case GraphPatternGroup jgn -> {
			var nq = new JoinGroupNode(jgn);
			nq.getChildren().clear();
			for (Object iGroupMemberNode : jgn.getChildren()) {
				if (iGroupMemberNode instanceof IGroupMemberNode mn) {
					nq.addChild((IGroupMemberNode) replaceIncludes(mn, bOp, blazeGraphIncludeExample));
				}
			}
//			nq.setLeftArg(visit(nq.getChildren(), bOp));
//			nq.setRightArg(visit(nq.getRightArg(), bOp));
			yield nq;
		}
		case NamedSubqueryInclude nsq -> {
			Object as = bOp.annotations().get("namedSet");
			if (nsq.annotations().get("namedSet").equals(as)) {
				SubqueryRoot sqr = new SubqueryRoot((QueryType) bOp.annotations().get("queryType"));
				sqr.setGraphPattern((GraphPatternGroup<IGroupMemberNode>) bOp.annotations().get("graphPattern"));
				Pattern includeAs = Pattern.compile("(INCLUDE|include)\\s+" + as+"[\\s|\\.\\}]");
				Matcher m = includeAs.matcher(blazeGraphIncludeExample);
				if (m.find()) {
					do {
						if (m.group().endsWith("}")) {
							blazeGraphIncludeExample.delete(m.start(), m.end() - 1);
						} else {
							blazeGraphIncludeExample.delete(m.start(), m.end());
						}
						blazeGraphIncludeExample.insert(m.start(), bOp.annotations().get("original"));
						m = includeAs.matcher(blazeGraphIncludeExample);
					} while (m.find()); 
				}
				yield sqr;
			}
			yield nsq;
		}
		default -> astContainer;
		};
	}

	private static int findBlockInMatchingBrackets(StringBuilder blazeGraphIncludeExample, int at) {
		// We look to find a matching closing pair of brackets.
		int open = 1;
		while (open > 0 && at > 0) {
			char cat = blazeGraphIncludeExample.charAt(at);
			if (cat == '{') {
				open--;
			} else if (cat == '}') {
				open++;
			}
			at--;
		}
		return at;
	}

	private static int findWithJustBeforeOpenBracket(StringBuilder blazeGraphIncludeExample, int at) {
		Matcher toFindLastWith = WITH.matcher(blazeGraphIncludeExample.substring(0, at));
		int withStart = 0;
		while (toFindLastWith.find()) {
			withStart = toFindLastWith.start();
			// Loop is important.
		}
		return withStart;
	}
}
