package dremel.compiler.impl;

import java.util.List;
import dremel.compiler.ExpNode;
import dremel.compiler.expression.ExpNodeFactory;
import dremel.compiler.expression.Symbol;
import dremel.compiler.parser.AstNode;
import dremel.dataset.Table;
import dremel.parser.impl.BqlParser;

/**
 * @author nhsan
 * 
 */
public class MetaxaQuery extends DefaultQuery {
	public MetaxaQuery(AstNode root) {
		parseSelectStatement(root);
	}

	void parseSelectStatement(AstNode node) {
		assert (node.getType() == BqlParser.N_SELECT_STATEMENT);
		int count = node.getChildCount();
		assert (count >= 2);
		parseFromClause((AstNode) node.getChild(0));
		parseSelectClause((AstNode) node.getChild(1));
		parseWhereClause((AstNode) node.getChild(2));
	}

	void parseFromClause(AstNode node) {
		assert (node.getType() == BqlParser.N_FROM);
		int count = node.getChildCount();
		assert (count > 0);
		for (int i = 0; i < count; i++) {
			AstNode node2 = (AstNode) node.getChild(i);
			if (node2.getType() == BqlParser.N_TABLE_NAME) {

				assert (node2.getChildCount() == 1);
				AstNode node3 = (AstNode) node2.getChild(0);
				List<Table> tables = this.getTables();
				tables.add(new dremel.dataset.impl.Table(node3.getText()));
			} else if (node2.getType() == BqlParser.N_SELECT_STATEMENT) {
				List<dremel.compiler.Query> queries = this.getSubQueries();
				queries.add(new MetaxaQuery(node2));
			} else
				assert (false);
		}
	}

	void parseSelectClause(AstNode node) {
		assert (node.getType() == BqlParser.N_SELECT);
		int count = node.getChildCount();
		assert (count > 0);
		for (int i = 0; i < count; i++) {
			parseCreateColumn((AstNode) node.getChild(i));
		}
	}

	void parseWhereClause(AstNode node) {
		if (node == null) {
			filter = null;
			return;
		}
		assert (node.getType() == BqlParser.N_WHERE);
		int count = node.getChildCount();
		assert (count == 1);
		ExpNode filterNode = ExpNodeFactory.buildNode((AstNode) node.getChild(0), this);
		dremel.compiler.impl.Expression f = new dremel.compiler.impl.Expression();
		f.setRoot(filterNode);
		filter = f;
	}

	void parseCreateColumn(AstNode node) {
		StringBuffer alias = new StringBuffer();
		StringBuffer within = new StringBuffer();
		boolean isWithinRecord = false;
		boolean hasAlias = false;
		assert (node.getType() == BqlParser.N_CREATE_COLUMN);
		int count = node.getChildCount();
		assert ((count >= 1) && (count <= 3));
		if (count == 3) {
			parseColumnAlias((AstNode) node.getChild(1), alias);
			hasAlias = true;
			if (node.getChild(2).getType() == BqlParser.N_WITHIN)
				parseWithinClause((AstNode) node.getChild(2), within);
			else if (node.getChild(2).getType() == BqlParser.N_WITHIN_RECORD)
				isWithinRecord = parseWithinRecordClause((AstNode) node.getChild(2));
		} else if (count == 2) {
			if (node.getChild(1).getType() == BqlParser.N_ALIAS) {
				hasAlias = true;
				parseColumnAlias((AstNode) node.getChild(1), alias);
			} else if (node.getChild(1).getType() == BqlParser.N_WITHIN)
				parseWithinClause((AstNode) node.getChild(1), within);
			else if (node.getChild(1).getType() == BqlParser.N_WITHIN_RECORD)
				isWithinRecord = parseWithinRecordClause((AstNode) node.getChild(1));
			else {
				assert (false);
			}
		} else if (node.getChild(0).getChildCount() == 1 && node.getChild(0).getChild(0).getType() == BqlParser.N_ID) {
			alias.append(getID((AstNode) node.getChild(0).getChild(0)));
		}

		assert (node.getChild(0).getType() == BqlParser.N_EXPRESSION);
		ExpNode root = ExpNodeFactory.buildNode((AstNode) node.getChild(0), this);
		dremel.compiler.impl.Expression expression = new dremel.compiler.impl.Expression();

		if (isWithinRecord)
			expression.setScope("RECORD");
		else if (within.toString().length() > 0)
			expression.setScope(within.toString());
		else
			expression.setScope(null);

		if (hasAlias) {
			String aStr = alias.toString();
			expression.setAlias(aStr);
			assert (!symbolTable.containsKey(aStr));
			Symbol symbol = new Symbol(aStr, this);
			symbol.setReference(expression);
			symbolTable.put(aStr, symbol);
		} else if (alias.toString().length() > 0) {
			expression.setAlias(alias.toString());
		}

		expression.setRoot(root);
		this.getSelectExpressions().add(expression);
	};

	private boolean parseWithinRecordClause(AstNode node) {
		assert (node.getType() == BqlParser.N_WITHIN_RECORD);
		return true;
	}

	private void parseWithinClause(AstNode node, StringBuffer within) {
		assert (node.getType() == BqlParser.N_WITHIN);
		int count = node.getChildCount();
		assert ((count == 1));
		within.append(getID((AstNode) node.getChild(0)));
	}

	private void parseColumnAlias(AstNode node, StringBuffer alias) {
		assert (node.getType() == BqlParser.N_ALIAS);
		int count = node.getChildCount();
		assert ((count == 1));
		node = (AstNode) node.getChild(0);
		assert (node.getType() == BqlParser.N_NAME);
		assert (node.getChildCount() == 1);
		alias.append(node.getChild(0).getText());
	}

	public static String getID(AstNode node) {
		StringBuilder ret = new StringBuilder();
		assert (node.getType() == BqlParser.N_ID);
		assert (node.getChildCount() >= 1);
		AstNode node2 = (AstNode) node.getChild(0);
		assert (node2.getType() == BqlParser.N_NAME);
		assert (node2.getChildCount() == 1);
		ret.append(node2.getChild(0).toString());

		for (int i = 1; i < node.getChildCount(); i++) {
			node2 = (AstNode) node.getChild(i);
			assert (node2.getType() == BqlParser.N_NAME);
			assert (node2.getChildCount() == 1);
			ret.append(".");
			ret.append(node2.getChild(0).toString());
		}
		return ret.toString();
	}
}
