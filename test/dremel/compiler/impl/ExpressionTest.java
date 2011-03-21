/**
 * Copyright 2010, BigDataCraft.com
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package dremel.compiler.impl;

import static org.junit.Assert.assertTrue;

import org.antlr.runtime.RecognitionException;
import org.junit.Test;

import dremel.compiler.Compiler;
import dremel.compiler.Expression.ReturnType;
import dremel.compiler.Expression.Symbol;
import dremel.compiler.Query;
import dremel.compiler.Expression.BinaryOp;
import dremel.compiler.Expression.Function;
import dremel.compiler.Expression.Node;
import dremel.compiler.parser.AstNode;
import dremel.compiler.parser.Parser;

/**
 * @author camuelg
 * @author nhsan
 * 
 */
public class ExpressionTest {
	@Test
	public void testExpression1() throws RecognitionException {
		AstNode nodes = Parser.parseBql("SELECT (3+w-6)+count(f1)-length(f2) as exp1 FROM [document]");
		Compiler compiler = new CompilerImpl();
		Query query = compiler.parse(nodes);
		AstNode node = (AstNode) nodes.getChild(1);
		Node exp = Expression.buildNode((AstNode) node.getChild(0).getChild(0), query);

		assertTrue(exp instanceof BinaryOp); // subtract
		BinaryOp bin1 = (BinaryOp) exp;
		assertTrue(bin1.getOperator().equals("-"));
		assertTrue(exp.getChild(0) instanceof BinaryOp); // add
		BinaryOp bin2 = (BinaryOp) exp.getChild(0);
		assertTrue(bin2.getOperator().equals("+"));
		assertTrue(exp.getChild(1) instanceof Function);
		assertTrue(((Function) exp.getChild(1)).getName().equals("length"));
	}

	@Test
	public void testExpression2() throws RecognitionException {
		//FIXME: PARSER-logical NOT is not recognized
		//FIXME: PARSER-bitwise NOT is not recognized
		AstNode nodes = Parser.parseBql("SELECT (3+8.4-6), (3&4), (5>6) AND (7>8), \"abc\"-100, (\"abc\">\"def\") FROM [document]");
		Compiler compiler = new CompilerImpl();
		Query query = compiler.parse(nodes);
		AstNode node = (AstNode) nodes.getChild(1);
		Node exp = Expression.buildNode((AstNode) node.getChild(0).getChild(0), query);
		assertTrue(exp.getReturnType() == ReturnType.FLOAT);
		exp = Expression.buildNode((AstNode) node.getChild(1).getChild(0), query);
		assertTrue(exp.getReturnType() == ReturnType.INT);
		exp = Expression.buildNode((AstNode) node.getChild(2).getChild(0), query);
		assertTrue(exp.getReturnType() == ReturnType.BOOL);
		exp = Expression.buildNode((AstNode) node.getChild(3).getChild(0), query);
		assertTrue(exp.getReturnType() == ReturnType.INVALID);
		exp = Expression.buildNode((AstNode) node.getChild(4).getChild(0), query);
		assertTrue(exp.getReturnType() == ReturnType.BOOL);
	}

	@Test
	public void testExpression3() throws RecognitionException {
		AstNode nodes = Parser.parseBql("SELECT \ndocid-5.6, links.forward>links.backward, count(links.forward) FROM [document]");
		Compiler compiler = new CompilerImpl();
		Query query = compiler.parse(nodes);
		compiler.analyse(query);
		assertTrue(query.getTables().size() == 1);
		assertTrue(query.getSelectExpressions().size() == 3);
		assertTrue(query.getSelectExpressions().get(0).getReturnType() == ReturnType.FLOAT);
		assertTrue(query.getSelectExpressions().get(1).getReturnType() == ReturnType.BOOL);
		assertTrue(query.getSelectExpressions().get(2).getReturnType() == ReturnType.INT);
	}
}