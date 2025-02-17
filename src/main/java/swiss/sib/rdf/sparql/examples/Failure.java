package swiss.sib.rdf.sparql.examples;

import org.eclipse.rdf4j.query.MalformedQueryException;

public enum Failure {
	CANT_READ_INPUT_DIRECTORY(1), CANT_PARSE_EXAMPLE(2), CANT_READ_EXAMPLE(3), CANT_WRITE_EXAMPLE_RQ(4), JUNIT(5),
	CONNECTION_TO_WIKIDATA_WIKI_FAIL(6), CANT_WRITE_FIXED_EXAMPLE(7), CANT_PARSE_PREFIXES(8), DID_NOTHING(9);

	private final int exitCode;

	Failure(int i) {
		this.exitCode = i;
	}

	NeedToStopException tothrow(Exception e) {
		return new NeedToStopException(e, this);
	}
	
	void exit(Exception e) throws NeedToStopException{
		throw new NeedToStopException(e, this);
	}

	void exit(String queryS, MalformedQueryException e) throws NeedToStopException {
		throw new NeedToStopException(e, this, queryS + "." + e.getMessage());
	}

	int exitCode() {
		
		return exitCode;
	}

}