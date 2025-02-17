package swiss.sib.rdf.sparql.examples;

import org.eclipse.rdf4j.query.MalformedQueryException;

public class NeedToStopException extends RuntimeException {

	private static final long serialVersionUID = 1L;
	private final Failure failure;

	public NeedToStopException(Exception e, Failure failure) {
		super(e);
		this.failure = failure;
	}

	public NeedToStopException(MalformedQueryException e, Failure failure2, String message) {
		super(message, e);
		this.failure = failure2;
	}

	public Failure getFailure() {
		return failure;
	}

}
