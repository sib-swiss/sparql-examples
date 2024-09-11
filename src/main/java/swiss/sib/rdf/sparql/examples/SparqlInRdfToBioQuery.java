package swiss.sib.rdf.sparql.examples;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.model.vocabulary.SHACL;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.rio.RDFParser;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.helpers.StatementCollector;
import org.json.JSONObject;

import swiss.sib.rdf.sparql.examples.Converter.Failure;
import swiss.sib.rdf.sparql.examples.vocabularies.SIB;
import swiss.sib.rdf.sparql.examples.vocabularies.SchemaDotOrg;

public class SparqlInRdfToBioQuery {
	
	public static HashMap<String, Integer> kgNameToCategory = new HashMap<String, Integer>();
	public static HashSet<String> alreadyUploaded = new HashSet<String>();
	
	public static void uploadExamplesToBioquery(String kgName, Model ex) {
		kgNameToCategory.put("UniProt", 100);
		kgNameToCategory.put("Bgee", 101);
		kgNameToCategory.put("MetaNetX", 102);
		kgNameToCategory.put("OrthoDB", 103);
		kgNameToCategory.put("SwissLipids", 104);
		kgNameToCategory.put("neXtProt", 105);
		kgNameToCategory.put("OMA", 106);
		kgNameToCategory.put("Rhea", 107);
		kgNameToCategory.put("GlyConnect", 108);
		kgNameToCategory.put("HAMAP", 109);
		kgNameToCategory.put("dbgi", 110);
		
		// for each query, call the "upload new question-query" pair API
		// https://github.com/biosoda/bioquery-extend-app
		// assume this is running on localhost:3000/api/templates
		List<String> questions = new ArrayList<>();
		List<String> targets = new ArrayList<>();
		List<String> queries = new ArrayList<>();
		
		//TODO: GROUP BY CATEGORY HERE (Rhea, Uniprot, Glyconnect etc)

		streamOf(ex, null, RDF.TYPE, SHACL.SPARQL_EXECUTABLE).map(Statement::getSubject).distinct()
				.peek(s -> questions.add(s.stringValue())).forEach(s -> {
					streamOf(ex, s, RDFS.COMMENT, null).map(Statement::getObject).map(Value::stringValue)
							.map(o -> o.replaceAll("\n", " ").replaceAll("\r", "")).forEach(questions::add);

					String questionToUpload, queryToUpload, targetSPARQLendpoint;
					
					questionToUpload = questions.get(questions.size() - 1);


					if(!alreadyUploaded.contains(questionToUpload)) {
					
						alreadyUploaded.add(questionToUpload);
						
						//endpoints where the query can be executed - should we only keep first?
						streamOf(ex, s, SchemaDotOrg.TARGET, null).map(Statement::getObject).map(Value::stringValue)
							.findFirst().ifPresent(targets::add);
						
						targetSPARQLendpoint = targets.get(0);
						
						Stream.of(SHACL.ASK, SHACL.SELECT, SHACL.CONSTRUCT, SIB.DESCRIBE)
								.flatMap(qt -> streamOf(ex, s, qt, null)).map(Statement::getObject)
								.map(o -> o.stringValue()).map(q -> {
									ArrayList<String> l = new ArrayList<>();
									l.add(q);
									return l.stream().collect(Collectors.joining("\n"));
								}).forEach(q -> {
									queries.add(q);
								});
	
						queryToUpload = queries.get(queries.size() - 1);
						System.out.println(queries);
						//could we also have here the email and name of the contributor?
						//uploadUsingAPI(kgName, questionToUpload, queryToUpload, targetSPARQLendpoint);
					}
				});
		
	}
	
	public static void uploadUsingAPI(String kgName, String question, String query, String target) {
		HttpClient httpclient = HttpClients.createDefault();
		HttpPost httppost = new HttpPost("http://localhost:3000/api/templates");
		httppost.setHeader("Content-type", "application/json");

		// Request parameters as needed, see 
		// https://github.com/biosoda/bioquery-extend-app/blob/main/src/app/api/templates/route.ts
		JSONObject json = new JSONObject();
		json.put("category", kgNameToCategory.get(kgName));
		json.put("question", question);
		json.put("sparqlQuery", query);
		json.put("serviceUrl", target);
		
		//use a generic name and email for now
		json.put("name", "sparql-examples");
		json.put("email", "kru@sib.swiss");
		try {
			httppost.setEntity(new StringEntity(json.toString()));
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		//Execute and get the response.
		HttpResponse response = null;
		try {
			response = httpclient.execute(httppost);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		HttpEntity entity = response.getEntity();
		
		System.out.println("Request status " + response.getStatusLine());

		if (entity != null) {
			String responseString = null;
			try {
				responseString = EntityUtils.toString(entity, "UTF-8");
		    	
			} catch (ParseException | IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}
	}
	
	static Stream<Statement> streamOf(Model ex, Resource s, IRI p, Value o) {
		return StreamSupport.stream(ex.getStatements(s, p, o).spliterator(), false);
	}
	
	public static void main(String[] args) {
		
		Path inputDirectory = Paths.get("");
		Stream<Path> list = null;
		try {
			list = Files.list(inputDirectory);
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		list.filter(Files::isDirectory).forEach(pro -> {
			try {
				FindFiles.sparqlExamples(pro).forEach((p) -> { 
					Model model = new LinkedHashModel();
					
					//here we assume a folder structure: /examples/kgName/1.ttl
					String kgName = p.getParent().getFileName().toString();
					
					RDFParser rdfParser = Rio.createParser(RDFFormat.TURTLE);

					rdfParser.setRDFHandler(new StatementCollector(model));
					try (InputStream is = Files.newInputStream(p)) {
						rdfParser.parse(is);
					} catch (RDFParseException | RDFHandlerException e) {
						Failure.CANT_PARSE_EXAMPLE.exit(e);
					} catch (IOException e) {
						Failure.CANT_READ_EXAMPLE.exit(e);
					}
					
					uploadExamplesToBioquery(kgName, model);
				}
					);
			} catch (IOException e) {
				Failure.CANT_PARSE_EXAMPLE.exit(e);
				throw new RuntimeException(e);
			}
		});
	}
}
