package dremel.compiler.expression;

import dremel.compiler.ExpNode;
import dremel.compiler.Query;

/**
 * @author nhsan
 *
 */
public abstract class AbstractNode implements ExpNode {
	Query query;

	public AbstractNode(Query query) {
		this.query = query;
	}

	@Override
	public Query getQuery() {
		return query;
	}
}
