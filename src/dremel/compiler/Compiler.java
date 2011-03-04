package dremel.compiler;

import org.codehaus.commons.compiler.IScriptEvaluator;

import dremel.executor.Executor;

public interface Compiler {
	
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
	 * compile a query (annotated) and return script evaluator for wiring
	 * @param query: ASM
	 * @return an executor or null
	 */
	public IScriptEvaluator compileToScript(Query query);
}	
