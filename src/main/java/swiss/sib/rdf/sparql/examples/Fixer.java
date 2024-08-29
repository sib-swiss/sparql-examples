package swiss.sib.rdf.sparql.examples;

import java.nio.file.Path;
import java.util.concurrent.Callable;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.sail.memory.MemoryStore;

import picocli.CommandLine;
import picocli.CommandLine.Option;
import picocli.CommandLine.Spec;
import picocli.CommandLine.Model.CommandSpec;
import swiss.sib.rdf.sparql.examples.vocabularies.SIB;

@CommandLine.Command(name = "fix", description = "Attempts to fixes example files")
public class Fixer implements Callable<Integer> {
	
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

	private void fix(Value value, Value value2, Value value3, Model model) {
		// TODO Auto-generated method stub
		
	}

}
