package swiss.sib.rdf.sparql.examples;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.stream.Stream;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.eclipse.rdf4j.sparqlbuilder.core.Prefix;
import org.eclipse.rdf4j.sparqlbuilder.core.Projection;
import org.eclipse.rdf4j.sparqlbuilder.core.QueryElement;
import org.eclipse.rdf4j.sparqlbuilder.core.QueryPattern;
import org.eclipse.rdf4j.sparqlbuilder.core.SparqlBuilder;
import org.eclipse.rdf4j.sparqlbuilder.core.Variable;
import org.eclipse.rdf4j.sparqlbuilder.core.query.Queries;
import org.eclipse.rdf4j.sparqlbuilder.core.query.Query;
import org.eclipse.rdf4j.sparqlbuilder.core.query.SelectQuery;
import org.eclipse.rdf4j.sparqlbuilder.graphpattern.GraphPattern;
import org.eclipse.rdf4j.sparqlbuilder.graphpattern.GraphPatternNotTriples;
import org.eclipse.rdf4j.sparqlbuilder.graphpattern.GraphPatterns;
import org.eclipse.rdf4j.sparqlbuilder.graphpattern.TriplePattern;
import org.eclipse.rdf4j.sparqlbuilder.rdf.Iri;
import org.eclipse.rdf4j.sparqlbuilder.rdf.Rdf;
import org.eclipse.rdf4j.sparqlbuilder.rdf.RdfObject;
import org.eclipse.rdf4j.sparqlbuilder.rdf.RdfResource;
import org.eclipse.rdf4j.sparqlbuilder.rdf.RdfSubject;
import org.openrdf.query.MalformedQueryException;

import com.bigdata.bop.BOp;
import com.bigdata.bop.Var;
import com.bigdata.rdf.model.BigdataLiteral;
import com.bigdata.rdf.model.BigdataURI;
import com.bigdata.rdf.model.BigdataValue;
import com.bigdata.rdf.sail.sparql.Bigdata2ASTSPARQLParser;
import com.bigdata.rdf.sail.sparql.BigdataParsedQuery;
import com.bigdata.rdf.sparql.ast.ASTContainer;
import com.bigdata.rdf.sparql.ast.ConstantNode;
import com.bigdata.rdf.sparql.ast.GraphPatternGroup;
import com.bigdata.rdf.sparql.ast.IGroupMemberNode;
import com.bigdata.rdf.sparql.ast.JoinGroupNode;
import com.bigdata.rdf.sparql.ast.NamedSubqueriesNode;
import com.bigdata.rdf.sparql.ast.NamedSubqueryInclude;
import com.bigdata.rdf.sparql.ast.NamedSubqueryRoot;
import com.bigdata.rdf.sparql.ast.QueryRoot;
import com.bigdata.rdf.sparql.ast.QueryType;
import com.bigdata.rdf.sparql.ast.StatementPatternNode;
import com.bigdata.rdf.sparql.ast.SubqueryBase;
import com.bigdata.rdf.sparql.ast.SubqueryRoot;
import com.bigdata.rdf.sparql.ast.TermNode;
import com.bigdata.rdf.sparql.ast.VarNode;

import picocli.CommandLine;
import picocli.CommandLine.Option;
import picocli.CommandLine.Spec;
import picocli.CommandLine.Model.CommandSpec;
import swiss.sib.rdf.sparql.examples.vocabularies.SIB;

@CommandLine.Command(name = "fix", description = "Attempts to fixes example files")
public class Fixer implements Callable<Integer> {

	private static final ValueFactory VF = SimpleValueFactory.getInstance();

	@Spec
	CommandSpec spec;

	private static final String PREFIXES = "PREFIX sh:<http://www.w3.org/ns/shacl#> PREFIX sib:<" + SIB.NAMESPACE + ">";
	@Option(names = { "-i",
			"--input-directory" }, paramLabel = "directory containing example files to test", description = "The root directory where the examples and their prefixes can be found.", required = true)
	private Path inputDirectory;

	@Option(names = { "-h", "--help" }, usageHelp = true, description = "display this help message")
	private boolean usageHelpRequested;

	@Option(names = { "-p", "--project" }, paramLabel = "projects to test", defaultValue = "all")
	private String projects;

	@Option(names = { "--also-run-slow-tests" })
	private boolean alsoRunSlowTests;

	public Integer call() {
		Fixer converter = new Fixer();
		CommandLine commandLine = spec.commandLine();
		if (commandLine.isUsageHelpRequested()) {
			commandLine.usage(System.out);
			return 0;
		} else if (commandLine.isVersionHelpRequested()) {
			commandLine.printVersionHelp(System.out);
			return 0;
		} else {
			converter.test();
		}
		return 0;
	}

	private void test() {
		Model model = Converter.parseExampleFilesIntoModel(projects, inputDirectory);
		SailRepository sr = new SailRepository(new MemoryStore());
		sr.init();
		try (SailRepositoryConnection conn = sr.getConnection()) {
			conn.begin();
			conn.add(model);
			conn.commit();
		}
		try (SailRepositoryConnection conn = sr.getConnection()) {
			TupleQuery tq = conn.prepareTupleQuery(PREFIXES + """
						SELECT ?queryIri ?query ?file {
							?queryIri sh:select ?query ;
							    sib:file_name ?file .
						}
					""");
			try (TupleQueryResult tqr = tq.evaluate()) {
				while (tqr.hasNext()) {
					BindingSet tqrb = tqr.next();
					fix(tqrb.getValue("queryIri"), tqrb.getValue("query"), tqrb.getValue("file"), model);
				}
			}
		}
		sr.shutDown();
	}

	private void fix(Value queryIri, Value query, Value file, Model model) {

	}

	public static String fix(String blazeGraphIncludeExample) throws MalformedQueryException {
		Bigdata2ASTSPARQLParser blzp = new Bigdata2ASTSPARQLParser();
		BigdataParsedQuery pq = blzp.parseQuery(blazeGraphIncludeExample, "https://example.org/");
		ASTContainer astContainer = pq.getASTContainer();
		QueryRoot origAst = pq.getASTContainer().getOriginalAST();
		NamedSubqueriesNode nsq = origAst.getNamedSubqueries();
		if (nsq != null) {
			BOp bOp = nsq.get(0);
			String namedSet = (String) bOp.getProperty("namedSet");
			blazeGraphIncludeExample = blazeGraphIncludeExample.replaceAll("INCLUDE\\s+" + namedSet, "");
			origAst.clearProperty("namedSubqueries");

			BOp fixed = replaceIncludes(origAst, bOp);
			return toString(fixed);
		}
		return blazeGraphIncludeExample;
	}

	private static BOp replaceIncludes(BOp astContainer, BOp bOp) {
		return switch (astContainer) {
		case QueryRoot qr -> {
			var nq = new QueryRoot(qr);
			nq.setGraphPattern((GraphPatternGroup<IGroupMemberNode>) replaceIncludes(nq.getGraphPattern(), bOp));
			yield nq;
		}
		case NamedSubqueryRoot nsqr -> bOp;
		case SubqueryRoot sqb -> sqb;
		case GraphPatternGroup jgn -> {
			var nq = new JoinGroupNode(jgn);
			nq.getChildren().clear();
			for (Object iGroupMemberNode : jgn.getChildren()) {
				if (iGroupMemberNode instanceof IGroupMemberNode mn) {
					nq.addChild((IGroupMemberNode) replaceIncludes(mn, bOp));
				}
			}
//			nq.setLeftArg(visit(nq.getChildren(), bOp));
//			nq.setRightArg(visit(nq.getRightArg(), bOp));
			yield nq;
		}
		case NamedSubqueryInclude nsq -> {
			if (nsq.annotations().get("namedSet").equals(bOp.annotations().get("namedSet"))) {
				SubqueryRoot sqr = new SubqueryRoot((QueryType) bOp.annotations().get("queryType"));
				sqr.setGraphPattern((GraphPatternGroup<IGroupMemberNode>) bOp.annotations().get("graphPattern"));
				yield sqr;
			}
			yield nsq;
		}
		default -> astContainer;
		};
	}

	private static QueryElement buildSelectQuery(BOp astContainer, QueryElement parent) {
		if (astContainer == null) {
			return null;
		}
		switch (astContainer) {
		case QueryRoot qr: {
			QueryPattern where = SparqlBuilder.where();
			qr.annotations();
			buildSelectQuery(qr.getBindingsClause(), parent);
			buildSelectQuery(qr.getConstruct(), parent);
			buildSelectQuery(qr.getDataset(), parent);
//			buildSelectQuery(qr.getGraphPattern(), where);
			buildSelectQuery(qr.getGroupBy(), parent);
			buildSelectQuery(qr.getHaving(), parent);
			buildSelectQuery(qr.getOrderBy(), parent);
			buildSelectQuery(qr.getProjection(), parent);
			buildSelectQuery(qr.getSlice(), parent);
			buildSelectQuery(qr.getWhereClause(), where);
			((Query)parent).where(where);
			return parent;
		}
		case JoinGroupNode jgn:
			
			List<QueryElement> list = jgn.getChildren().stream().map(ign -> buildSelectQuery(ign, parent)).toList();
			List<GraphPattern> st = list.stream().map(GraphPattern.class::cast).toList();
//			GraphPattern[] tpn = (GraphPattern[]) st.toArray();
			if (parent instanceof GraphPattern gp) {
				gp.and(st.toArray(new GraphPattern[0]));
			} else if (parent instanceof Projection p)
			{
				for (GraphPattern g : st) {
					Projectable p2 = (Projectable) p;
					System.out.println(g.toString());
				}
			}
			return parent;
		case StatementPatternNode spn:
			BigdataValue subjectValue = asValue(spn.get(0));
			BigdataValue predicateValue = asValue(spn.get(1));
			BigdataValue objectValue = asValue(spn.get(2));
			Var subjectVar = asVar(spn.get(0));
			Var predicateVar = asVar(spn.get(1));
			Var objectVar = asVar(spn.get(2));
			QueryElement s = asQueryElement(subjectValue, subjectVar);
			QueryElement p	 = asQueryElement(predicateValue, predicateVar);
			QueryElement o	 = asQueryElement(objectValue, objectVar);
//			IRI p = asIri(subjectValue, subjectVar);
			if (p instanceof Iri pi) {
				return GraphPatterns.tp((RdfSubject) s, pi, (RdfObject) o);
			} else if (p instanceof Variable pv) {
				return GraphPatterns.tp((RdfSubject) s, pv, (RdfObject) o);			
			}
			throw new IllegalStateException("Should have been removed");
		case NamedSubqueryRoot nsqr:
			throw new IllegalStateException("Should have been removed");
		case SubqueryRoot sqb:{
			if (parent instanceof QueryPattern qp) {
				QueryPattern select = SparqlBuilder.where();
				QueryElement selectQuery = buildSelectQuery(sqb.getGraphPattern(), select);
				
				
//				qp.where(select);
				
//				GraphPattern subselect = GraphPatterns.and(select);
//				qp.and(subselect);
			}
			return parent;
//			parent.subquery(buildSelectQuery(sqb.getGraphPattern(), parent));
		}
		default:
			return parent;
		}
	}

	private static QueryElement asQueryElement(BigdataValue value, Var<?> var) {
		if (value instanceof BigdataURI)
			return Rdf.iri(value.stringValue());
		else if (value instanceof BigdataLiteral bl) {
			String language = bl.getLanguage();
			if (language == null) {
				return Rdf.literalOfType(bl.getLabel(), VF.createIRI(bl.getDatatype().getNamespace(), bl.getDatatype().getLocalName()));
			} else {
				return Rdf.literalOfLanguage(bl.getLabel(), language);
			}
		} else
			return SparqlBuilder.var(var.getName());
	}

	private static BigdataValue asValue(TermNode termNode) {
		return switch (termNode) {
		case ConstantNode tvn -> tvn.getValue();
		case VarNode cn -> null;
		default -> throw new IllegalStateException("Unexpected value: " + termNode);
		};
	}
	
	private static Var asVar(TermNode termNode) {
		return switch (termNode) {
		case ConstantNode tvn -> null;
		case VarNode cn -> {
			yield (Var) cn.getValueExpression();
		}
		default -> throw new IllegalStateException("Unexpected value: " + termNode);
		};
	}

	private static SelectQuery addPrefixes(BOp fixed, SelectQuery selectQuery) {
		Map<String, String> prefixes = (Map<String, String>) fixed.getProperty("prefixDecls");
		for (Map.Entry<String, String> e : prefixes.entrySet()) {
			Prefix prefix = SparqlBuilder.prefix(e.getKey(), VF.createIRI(e.getValue()));
			selectQuery.prefix(prefix);
		}
		return selectQuery;
	}

	private static String toString(BOp fixed) {

		if (fixed.getProperty("queryType") == QueryType.SELECT) {
			SelectQuery selectQuery = Queries.SELECT();
			addPrefixes(fixed, selectQuery);
			buildSelectQuery(fixed, selectQuery);
			return selectQuery.toString();
		}
		return fixed.toString();
	}
}
