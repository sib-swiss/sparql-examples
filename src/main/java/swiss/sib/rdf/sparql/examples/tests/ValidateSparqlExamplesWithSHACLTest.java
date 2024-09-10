package swiss.sib.rdf.sparql.examples.tests;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import org.eclipse.rdf4j.common.exception.ValidationException;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.vocabulary.RDF4J;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.eclipse.rdf4j.sail.shacl.ShaclSail;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import swiss.sib.rdf.sparql.examples.FindFiles;

public class ValidateSparqlExamplesWithSHACLTest {
	private static final String shaclOneIdOneQuery = """
			PREFIX sh:<http://www.w3.org/ns/shacl#>
			PREFIX schema:<https://schema.org/>
			PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
			PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
			[] sh:targetClass sh:SPARQLExecutable ;
				sh:property [
					sh:path [ sh:alternativePath ( sh:select sh:ask sh:describe sh:construct ) ] ;
					sh:maxCount 1 ;
					sh:minCount 1
				] , 
				[
					sh:path rdfs:comment ;
					sh:minCount 1 ; 
					sh:or ( [
							sh:datatype rdf:langString ;
							# sh:uniqueLang true ;
						] 
						[
							sh:datatype rdf:HTML ;
						] ) 
				], [
				sh:path schema:target;
					sh:minCount 1 ] , [
				sh:path schema:keyword;  # Typo not allowed
					sh:maxCount 0 ] , [
				sh:path schema:keywords; # Encouraged but not required.
					sh:minCount 0 ] .
			""";

	private static MemoryStore memoryStore;
	private static Repository repo;

	private static ShaclSail shaclSail;

	@BeforeAll
	public static void setup() {
		memoryStore = new MemoryStore();
		memoryStore.init();

		shaclSail = new ShaclSail(memoryStore);
		repo = new SailRepository(shaclSail);

		try (RepositoryConnection connection = repo.getConnection();
				StringReader sr = new StringReader(shaclOneIdOneQuery)) {
			// add shapes
			connection.begin();
			connection.add(sr, RDFFormat.TURTLE, RDF4J.SHACL_SHAPE_GRAPH);
			connection.commit();
		} catch (RDFParseException | RepositoryException | IOException e) {
			fail(e);
		}
	}

	@AfterAll
	public static void tearDown() {
		repo.shutDown();
		shaclSail.shutDown();
		memoryStore.shutDown();
	}

	/**
	 * Use shacl to test all the turtle files contain at least one rdfs:comment and one query.
	 * Also makes a test that all example IRIs are unique.
	 * 
	 * @param paths
	 * @return
	 * @throws URISyntaxException
	 * @throws IOException
	 */
	@TestFactory
	public Stream<DynamicTest> testShaclConstraints() throws URISyntaxException, IOException {
		Stream<Path> paths = FindFiles.sparqlExamples();
		return paths.map(p -> DynamicTest.dynamicTest(
				"Shacl testing: " + p.getParent().getFileName() + '/' + p.getFileName(), () -> testValidatingAFile(p)));
	}

	private static void testValidatingAFile(Path p) {
		assertTrue(Files.exists(p));
		try (RepositoryConnection connection = repo.getConnection()) {

			IRI iri = connection.getValueFactory().createIRI(p.toUri().toString());
			connection.begin();
			connection.add(p.toFile(), iri);
			connection.commit();
			assertFalse(connection.size() == 0);
		} catch (RDFParseException | RepositoryException | IOException e) {
			if (e.getCause() instanceof ValidationException ve) {
				String report = validationReportAsString(ve);
				fail(p.toUri() + " failed " + ve + '\n' + report);
			} else {
				fail(p.toUri() + " failed with a non SHACL error", e);
			}
		}
	}

	private static String validationReportAsString(ValidationException ve) {
		Model vem = ve.validationReportAsModel();
		var boas = new ByteArrayOutputStream();
		Rio.write(vem, boas, RDFFormat.TURTLE);
		return boas.toString();
	}
}