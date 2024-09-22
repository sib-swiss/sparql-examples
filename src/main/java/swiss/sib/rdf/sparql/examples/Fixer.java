package swiss.sib.rdf.sparql.examples;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.DCTERMS;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.model.vocabulary.SHACL;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.query.parser.sparql.SPARQLParser;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
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

import picocli.CommandLine;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Spec;
import swiss.sib.rdf.sparql.examples.vocabularies.SIB;
import swiss.sib.rdf.sparql.examples.vocabularies.SchemaDotOrg;

@CommandLine.Command(name = "fix", description = "Attempts to fixes example files")
public class Fixer implements Callable<Integer> {

	private static final ValueFactory VF = SimpleValueFactory.getInstance();

	@Spec
	CommandSpec spec;

	private static final String PREFIXES = "PREFIX sh:<" + SHACL.NAMESPACE + "> PREFIX sib:<" + SIB.NAMESPACE + ">";
	@Option(names = { "-i",
			"--input-directory" }, paramLabel = "directory containing example files to test", description = "The root directory where the examples and their prefixes can be found.", required = true)
	private Path inputDirectory;

	@Option(names = { "-h", "--help" }, usageHelp = true, description = "display this help message")
	private boolean usageHelpRequested;

	public Integer call() {
		CommandLine commandLine = spec.commandLine();
		if (commandLine.isUsageHelpRequested()) {
			commandLine.usage(System.out);
		} else if (commandLine.isVersionHelpRequested()) {
			commandLine.printVersionHelp(System.out);
		} else {
			findFilesToFix();
		}
		return 0;
	}

	private void findFilesToFix() {
		try {
			Map<String, String> prefixes = loadPrefixes();
			try (Stream<Path> sparqlExamples = FindFiles.sparqlExamples(inputDirectory)) {
				sparqlExamples.forEach(ttl -> {
					System.out.println("Looking at:" + ttl);
					try (FileInputStream in = new FileInputStream(ttl.toFile())) {
						Model model = Rio.parse(in, RDFFormat.TURTLE);
						IRI queryIri = null;
						Value query = null;
						Statement select = has(model, SHACL.SELECT);
						Statement construct = has(model, SHACL.CONSTRUCT);
						Statement ask = has(model, SHACL.ASK);
						if (select != null) {
							queryIri = (IRI) select.getSubject();
							query = select.getObject();
						} else if (construct != null) {
							queryIri = (IRI) construct.getSubject();
							query = construct.getObject();
						} else if (ask != null) {
							queryIri = (IRI) ask.getSubject();
							query = ask.getObject();
						}
						if (queryIri != null && query != null) {
							fix(queryIri, query, ttl, model, prefixes);
						}
					} catch (IOException | RDFParseException e) {
						System.err.println("RDF error in " + ttl);
						Failure.CANT_READ_EXAMPLE.exit(e);
					}
				});
			}
		} catch (IOException e) {
			Failure.CANT_READ_INPUT_DIRECTORY.exit(e);
		}
	}

	private Statement has(Model model, IRI iri) {
		Iterator<Statement> iterator = model.getStatements(null, iri, null).iterator();
		if (iterator.hasNext()) {
			return iterator.next();
		}
		return null;
	}

	private Map<String, String> loadPrefixes() throws IOException {
		SailRepository sr = new SailRepository(new MemoryStore());
		sr.init();
		try (SailRepositoryConnection conn = sr.getConnection()) {
			FindFiles.prefixFile(inputDirectory).forEach(p -> {
				conn.begin();
				try {
					conn.add(p.toFile());
				} catch (RDFParseException | RepositoryException | IOException e) {
					Failure.CANT_PARSE_PREFIXES.exit(e);
				}
				conn.commit();
			});
		}
		Map<String, String> prefixes = new LinkedHashMap<>();
		try (SailRepositoryConnection conn = sr.getConnection()) {
			TupleQuery tq = conn.prepareTupleQuery(PREFIXES + """
						SELECT ?prefix ?namespace {
							[] sh:prefix ?prefix;
							   sh:namespace ?namespace .
						}
					""");
			try (TupleQueryResult tqr = tq.evaluate()) {
				while (tqr.hasNext()) {
					BindingSet tqrb = tqr.next();
					String prefix = tqrb.getValue("prefix").stringValue();
					String namespace = tqrb.getValue("namespace").stringValue();
					prefixes.put(prefix, namespace);
				}
			}
		}
		sr.shutDown();
		return prefixes;
	}

	static void fix(IRI queryIri, Value query, Path file, Model model, Map<String, String> prefixes2) {
		String queryIriStr = queryIri.stringValue();
		String queryStr = query.stringValue();

		String fixedPrefixes = Fixer.fixMissingPrefixes(queryStr, prefixes2);
		String fix = null;
		if (fixedPrefixes != null) {
			System.out.println("Fixed prefixes " + queryIriStr + " in file " + file);
			model.remove(queryIri, SHACL.SELECT, query);
			model.add(queryIri, SHACL.SELECT, VF.createLiteral(fixedPrefixes));
			writeFixedModel(file, model);
			queryStr = fixedPrefixes;
		}
		fix = Fixer.fixBlazeGraph(queryStr, queryIriStr, file);

		if (fix != null) {
			System.out.println("Fixed blaze graph " + queryIriStr + " in file " + file);
			model.remove(queryIri, SHACL.SELECT, query);
			model.add(queryIri, SHACL.SELECT, VF.createLiteral(fix));
			model.remove(queryIri, SIB.BIGDATA_SELECT, null);
			model.add(queryIri, SIB.BIGDATA_SELECT, query);
			writeFixedModel(file, model);
			return;
		}
		if (fixedPrefixes == null) {
			System.out.println("No change to:" + file);
		}
	}

	private static void writeFixedModel(Path file, Model model) {
		try (OutputStream out = Files.newOutputStream(file, StandardOpenOption.TRUNCATE_EXISTING)) {
			model.getNamespaces().add(SHACL.NS);
			model.getNamespaces().add(RDF.NS);
			model.getNamespaces().add(RDFS.NS);
			model.getNamespaces().add(SchemaDotOrg.NS);
			model.getNamespaces().add(DCTERMS.NS);
			Rio.write(model, out, RDFFormat.TURTLE);

		} catch (RDFHandlerException | IOException e) {
			Failure.CANT_WRITE_FIXED_EXAMPLE.exit(e);
		}
	}

	public static String fixMissingPrefixes(String original, Map<String, String> prefixes2) {
		StringBuilder changed = new StringBuilder(original);
		for (Map.Entry<String, String> entry : prefixes2.entrySet()) {
			Pattern prefix = Pattern.compile("(^|\\W+)(?i:prefix)(\\W+)" + entry.getKey() + ":");
			Pattern prefixInUse = Pattern.compile("(^|\\W+)" + entry.getKey() + ":");
			if (!prefix.matcher(original).find() && prefixInUse.matcher(original).find()) {
				changed.insert(0, "PREFIX " + entry.getKey() + ": <" + entry.getValue() + ">\n");
			}
		}
		if (changed.length() == original.length())
			return null;
		else
			return changed.toString();
	}

	public static String fixBlazeGraph(String original, String queryIriStr, Path fileStr) {
		String fix = fixBlazeGraphIncludeWith(original, queryIriStr, fileStr);
		if (fix != null) {
			original = fix;
		}
		String fix2 = fixBlazeGraphHints(original, queryIriStr, fileStr);
		if (fix2 != null) {
			return fix2;
		} else if (fix != null) {
			return fix;
		} else {
			return null;
		}
	}

	public static String fixBlazeGraphHints(String original, String queryIriStr, Path fileStr) {
		if (original.contains("hint:")) {
			try {
				new SPARQLParser().parseQuery(original, QueryHints.NAMESPACE);
			} catch (org.eclipse.rdf4j.query.MalformedQueryException e) {
				String testQ = "PREFIX hint:<" + QueryHints.NAMESPACE + ">\n" + original;
				try {
					new SPARQLParser().parseQuery(testQ, QueryHints.NAMESPACE);
					// we now know we have hints that are in the query and we need to remove them.
					return original.replaceAll("hint:([^.;,])+[.;,]", "");

				} catch (org.eclipse.rdf4j.query.MalformedQueryException e2) {
					return null;
				}
			}

		}
		return null;

	}

	public static String fixBlazeGraphIncludeWith(String original, String queryIriStr, Path fileStr) {
		Bigdata2ASTSPARQLParser blzp = new Bigdata2ASTSPARQLParser();
		try {
			BigdataParsedQuery pq = blzp.parseQuery(original, "https://example.org/");

			QueryRoot origAst = pq.getASTContainer().getOriginalAST();
			NamedSubqueriesNode nsq = origAst.getNamedSubqueries();
			if (nsq != null) {
				StringBuilder sb = new StringBuilder(original);
				for (int i = 0; i < nsq.size(); i++) {
					NamedSubqueryRoot bOp = (NamedSubqueryRoot) nsq.get(i);

					origAst.clearProperty("namedSubqueries");
					
					Pattern asP = Pattern.compile(bOp.getName() + "\\s");
					Matcher matcher = asP.matcher(sb);

					if (matcher.find()) {
						int startAsP = matcher.start();
						int lastClosingBracket = sb.lastIndexOf("}", startAsP);
						int openingBracket = findBlockInMatchingBrackets(sb, lastClosingBracket - 1);
						int withStart = findWithJustBeforeOpenBracket(sb, openingBracket);
						String toInclude = sb.substring(openingBracket, lastClosingBracket + 1);
						bOp.annotations().put("original", toInclude);
						sb.delete(withStart, matcher.end());
					}
					BOp fixed = replaceIncludes(origAst, bOp, sb);
				}
				return sb.toString();
			}
			return null;
		} catch (MalformedQueryException e) {
			System.out.println("Failed to fix include " + queryIriStr + " in " + fileStr);
			return null;
		}
	}
	
	private static final Pattern WITH = Pattern.compile("with", Pattern.CASE_INSENSITIVE);

	private static BOp replaceIncludes(BOp astContainer, BOp bOp, StringBuilder blazeGraphIncludeExample) {
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
				Pattern includeAs = Pattern.compile("(INCLUDE|include)\\s+" + as+"\\s");
				Matcher m = includeAs.matcher(blazeGraphIncludeExample);
				if (m.find()) {
					do {
						blazeGraphIncludeExample.delete(m.start(), m.end());
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
