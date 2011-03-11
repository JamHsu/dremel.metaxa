package dremel.compiler;

import dremel.executor.Executor;
import dremel.compiler.parser.AstNode;

public interface Compiler {
	
	/**
	 * - build expressions
	 * - gather symbols
	 * @param root: Abstract Syntax Tree
	 * @return
	 */
	public Query parse(AstNode root);
	
	/**
	 * - match field names against schema
	 * - calculate repetition level of expressions and fields
	 * @param query : a parsed query with expressions + symbols
	 */
	public void analyse(Query query);
	
	/**
	 * compile a query (annotated) and build executor
	 * @param query: ASM
	 * @return an executor or null
	 */
	public Executor compile(Query query);

	/**
	 * @param query
	 * @return
	 */
	String compileToScript(Query query);
}	
