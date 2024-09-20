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
			try (Stream<Path> sparqlExamples = FindFiles.sparqlExamples(inputDirectory)){
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
			FindFiles.prefixFile(inputDirectory).forEach(p->{
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

	private void fix(IRI queryIri, Value query, Path file, Model model, Map<String, String> prefixes2) {
		String queryIriStr = queryIri.stringValue();
		String queryStr = query.stringValue()
				.replace("\\\"", "\"");
		

		String fixedPrefixes = Fixer.fixMissingPrefixes(queryStr, prefixes2);
		String fix = null;
		if (fixedPrefixes != null) {
			System.out.println("Fixed prefixes " + queryIriStr + " in file " + file);
			model.remove(queryIri, SHACL.SELECT, query);
			model.add(queryIri, SHACL.SELECT, VF.createLiteral(fixedPrefixes));
			writeFixedModel(file, model);
			queryStr = fixedPrefixes;
		}
		fix = Fixer.fixBlazeGraphIncludeWith(queryStr, queryIriStr, file);
	
		if (fix != null) {
			System.out.println("Fixed " + queryIriStr + " in file " + file);
			model.remove(queryIri, SHACL.SELECT, query);
			model.add(queryIri, SHACL.SELECT, VF.createLiteral(fix));
			model.add(queryIri, SIB.BIGDATA_SELECT, query);
			writeFixedModel(file, model);
			return;
		} 
		if (fixedPrefixes == null) {
			System.out.println("No change to:" + file);
		}
	}

	private void writeFixedModel(Path file, Model model) {
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

	public static String fixBlazeGraphIncludeWith(String original, String queryIriStr, Path fileStr) {
		Bigdata2ASTSPARQLParser blzp = new Bigdata2ASTSPARQLParser();
		try {
			BigdataParsedQuery pq = blzp.parseQuery(original, "https://example.org/");

			QueryRoot origAst = pq.getASTContainer().getOriginalAST();
			NamedSubqueriesNode nsq = origAst.getNamedSubqueries();
			if (nsq != null) {
				BOp bOp = nsq.get(0);

				origAst.clearProperty("namedSubqueries");

				StringBuilder sb = new StringBuilder(original);
				BOp fixed = replaceIncludes(origAst, bOp, sb);
				return sb.toString();
			}
			return null;
		} catch (MalformedQueryException e) {
			System.out.println("Failed to fix include " + queryIriStr + " in " + fileStr);
			return null;
		}
	}

	private static BOp replaceIncludes(BOp astContainer, BOp bOp, StringBuilder blazeGraphIncludeExample) {
		return switch (astContainer) {
		case QueryRoot qr -> {
			var nq = new QueryRoot(qr);
			nq.setGraphPattern((GraphPatternGroup<IGroupMemberNode>) replaceIncludes(nq.getGraphPattern(), bOp,
					blazeGraphIncludeExample));
			yield nq;
		}
		case NamedSubqueryRoot nsqr -> bOp;
		case SubqueryRoot sqb -> sqb;
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
				Matcher m = Pattern.compile("(INCLUDE|include)\\s+" + as).matcher(blazeGraphIncludeExample);
				if (m.find()) {
					
					Pattern asP = Pattern.compile(as.toString(), Pattern.LITERAL);
					Pattern origP = Pattern.compile("(?:(?:WITH|with)\\s*\\{([\\s\\S]*?)\\}\\s+AS\\s+" + asP.pattern() + ")",
							Pattern.MULTILINE);
					Matcher orig = origP.matcher(blazeGraphIncludeExample);
					if (orig.find()) {
						try {
							String r = m.replaceAll('{' + orig.group(1) + '}');
							blazeGraphIncludeExample.setLength(0);
							blazeGraphIncludeExample.append(r);
							blazeGraphIncludeExample.delete(orig.start(), orig.end());
						} catch (IllegalArgumentException e) {
							System.out.println("Can't fix due to regex issue:"+origP.pattern());
						}
					}
				}
				yield sqr;
			}
			yield nsq;
		}
		default -> astContainer;
		};
	}
}
