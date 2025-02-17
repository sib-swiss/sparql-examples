package swiss.sib.rdf.sparql.examples;

import static swiss.sib.rdf.sparql.examples.SparqlInRdfToRq.streamOf;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.vocabulary.DC;
import org.eclipse.rdf4j.model.vocabulary.DCTERMS;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.model.vocabulary.SHACL;
import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.query.algebra.Avg;
import org.eclipse.rdf4j.query.algebra.Count;
import org.eclipse.rdf4j.query.algebra.GroupConcat;
import org.eclipse.rdf4j.query.algebra.Max;
import org.eclipse.rdf4j.query.algebra.Min;
import org.eclipse.rdf4j.query.algebra.Sum;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.query.parser.ParsedQuery;
import org.eclipse.rdf4j.query.parser.QueryParser;
import org.eclipse.rdf4j.query.parser.sparql.SPARQLParserFactory;
import org.jsoup.Jsoup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import swiss.sib.rdf.sparql.examples.mermaid.SparqlInRdfToMermaid;
import swiss.sib.rdf.sparql.examples.statistics.Counter;
import swiss.sib.rdf.sparql.examples.vocabularies.SIB;
import swiss.sib.rdf.sparql.examples.vocabularies.SchemaDotOrg;

public class SparqlInRdfToMd {
	private static final Logger log = LoggerFactory.getLogger(SparqlInRdfToMd.class);

	public static List<String> asMD(Model ex) {
		List<String> rq = new ArrayList<>();

		streamOf(ex, null, RDF.TYPE, SHACL.SPARQL_EXECUTABLE).map(Statement::getSubject).distinct().forEach(queryId -> {
			String ln = ((IRI) queryId).getLocalName();
			rq.add("# " + ln + "\n");
			rq.add("");
			streamOf(ex, queryId, DC.CONTRIBUTOR, null).map(Statement::getObject).map(Value::stringValue)
					.map(k -> " * Contributor: " + k).forEach(rq::add);
			rq.add("");
			streamOf(ex, queryId, DCTERMS.LICENSE, null).map(Statement::getObject).map(Value::stringValue)
					.map(k -> " * License: [" + k + "](" + k + ")").forEach(rq::add);
			rq.add("");
			streamOf(ex, null, SIB.FILE_NAME, null).map(s -> s.getObject().stringValue())
					.map(s -> s.replaceAll(".ttl", "")).map(s -> "[rq](" + s + ".rq) [turtle/ttl](" + s + ".ttl)")
					.forEach(rq::add);
			rq.add("");
			streamOf(ex, queryId, SchemaDotOrg.KEYWORDS, null).map(Statement::getObject).map(Value::stringValue)
					.map(k -> " * " + k).forEach(rq::add);
			rq.add("");
			streamOf(ex, queryId, RDFS.COMMENT, null).map(Statement::getObject).map(Value::stringValue)
					.forEach(rq::add);

			rq.add("");
			rq.add("## Use at ");
			streamOf(ex, queryId, SchemaDotOrg.TARGET, null).map(Statement::getObject).map(Value::stringValue)
					.map(v -> " * " + v).forEach(rq::add);

			rq.add("");
			rq.add("```sparql");
			Stream.of(SHACL.ASK, SHACL.SELECT, SHACL.CONSTRUCT, SIB.DESCRIBE)
					.flatMap(qt -> streamOf(ex, queryId, qt, null)).map(Statement::getObject).map(o -> o.stringValue())
					.forEach(q -> rq.add(q));
			rq.add("```");
			Iterator<Statement> iterator = streamOf(ex, queryId, DCTERMS.IS_PART_OF, null).iterator();
			if (iterator.hasNext()) {
				rq.add("## Query found at ");
				while (iterator.hasNext()) {
					String obj = iterator.next().getObject().stringValue();
					rq.add(" * [" + obj + "](" + obj + ")");
				}
			}

		});
		rq.add("");
		try {
			String asMermaid = SparqlInRdfToMermaid.asMermaid(ex);
			rq.add("```mermaid");
			rq.add(asMermaid);
			rq.add("```");
		} catch (MalformedQueryException | IllegalArgumentException e) {
			log.debug("Could not convert to mermaid", e);
		}
		return rq;
	}

	public static List<String> asIndexMD(Model ex) {
		List<String> rq = new ArrayList<>();
		streamOf(ex, null, SIB.PROJECT, null).map(Statement::getObject).distinct()
				.forEach(o -> rq.add("# " + o.stringValue()));
		rq.add("");

		streamOf(ex, null, SIB.FILE_NAME, null).flatMap(s -> streamOf(ex, s.getSubject(), RDFS.COMMENT, null)
				.map(Statement::getObject).map(asNiceLink(s.getObject().stringValue(), ".md"))).sorted()
				.forEach(rq::add);
		return rq;
	}

	public static List<String> asStatisticsMD(Model ex) {
		List<String> rq = new ArrayList<>();

		Counter counter = new Counter();
		streamOf(ex, null, RDF.TYPE, SHACL.SPARQL_EXECUTABLE).map(Statement::getSubject).distinct().forEach(queryId -> {
			Stream.of(SHACL.ASK, SHACL.SELECT, SHACL.CONSTRUCT, SIB.DESCRIBE)
					.flatMap(qt -> streamOf(ex, queryId, qt, null)).forEach(q -> {

						String base = streamOf(ex, q.getSubject(), SchemaDotOrg.TARGET, null).map(Statement::getObject)
								.map(Value::stringValue).findFirst().orElse("https://example.org/");
						QueryParser parser = new SPARQLParserFactory().getParser();
						String query = q.getObject().stringValue();

						ParsedQuery pq = parser.parseQuery(query, base);
						TupleExpr tq = pq.getTupleExpr();
						counter.count(tq);
					});
		});
		rq.add("# Statistics for SPARQL algebra features in use");
		rq.add("");
		rq.add("""
				Some basic statisics on SPARQL algebra features, as determined after parsing with RDF4j.
				""");
		rq.add("Statitics are collected over " +counter.getQueries() + " queries.");
		rq.add("");
		
		rq.add("| Type | Count | AVG |");
		rq.add("|------|-------|-----|");
		add(rq, "Statement patterns", counter.getBasicPatterns(), counter.getQueries());
		add(rq, "Filter", counter.getFilters(), counter.getQueries());
		add(rq, "Optional", counter.getOptionals(), counter.getQueries());
		add(rq, "Property paths", counter.getPropertyPaths(), counter.getQueries());
		add(rq, "Service", counter.getServiceClauses(), counter.getQueries());
		add(rq, "Unions", counter.getUnions(), counter.getQueries());
		add(rq, "Minus", counter.getMinus(), counter.getQueries());
		add(rq, "Exists", counter.getExists(), counter.getQueries());
		add(rq, "Group", counter.getGroups(), counter.getQueries());
		add(rq, "Order", counter.getOrder(), counter.getQueries());
		add(rq, "Aggregate", counter.getAggregates(), counter.getQueries());
		add(rq, " - Average", counter.getAverages(), counter.getQueries());
		add(rq, " - Count", counter.getCount(), counter.getQueries());
		add(rq, " - GroupConcat", counter.getGroupConcat(), counter.getQueries());
		add(rq, " - Max", counter.getMaxs(), counter.getQueries());
		add(rq, " - Min", counter.getMins(), counter.getQueries());	
		add(rq, " - Sample", counter.getSample(), counter.getQueries());
		add(rq, " - Sum", counter.getSums(), counter.getQueries());
		return rq;
	}

	private static void add(List<String> rq, String string, int basicPatterns, int queries) {
		rq.add("| " + string + " | " + basicPatterns + " | " + (queries == 0 ? 0 : basicPatterns / queries) + " |");
	}

	private static Function<Value, String> asNiceLink(String fileName, String extension) {
		return v -> {
			String comment = Jsoup.parse(v.stringValue()).text().replace("[", " ").replace("]", " ").strip();
			if (comment.length() > 75) {
				comment = comment.substring(0, 75) + "...";
			}
			String fileNameOfMdFile = fileName.substring(0, fileName.length() - 4) + extension;
			if (v instanceof Literal l && l.getLanguage().isPresent()) {
				// Escape "@" symbol in the link text
				return " - [" + comment.replace("@", "\\@") + "@" + l.getLanguage().get().replace("@", "\\@") + "](./"
						+ fileNameOfMdFile + ")";
			} else {
				// Escape "@" symbol in the link text
				return " - [" + comment.replace("@", "\\@") + "](./" + fileNameOfMdFile + ")";
			}
		};
	}
}
