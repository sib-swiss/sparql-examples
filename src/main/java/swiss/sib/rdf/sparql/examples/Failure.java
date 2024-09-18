package swiss.sib.rdf.sparql.examples;

import org.eclipse.rdf4j.query.MalformedQueryException;

public enum Failure {
	CANT_READ_INPUT_DIRECTORY(1), CANT_PARSE_EXAMPLE(2), CANT_READ_EXAMPLE(3), CANT_WRITE_EXAMPLE_RQ(4), JUNIT(5),
	CONNECTION_TO_WIKIDATA_WIKI_FAIL(6);

	private final int exitCode;

	Failure(int i) {
		this.exitCode = i;
	}

	void exit(Exception e) {
		System.err.println(e.getMessage());
		e.printStackTrace();
		System.exit(exitCode);
	}

	void exit(String queryS, MalformedQueryException e) {
		System.err.println(queryS + "." + e.getMessage());
		e.printStackTrace();
		System.exit(exitCode);

	}

}