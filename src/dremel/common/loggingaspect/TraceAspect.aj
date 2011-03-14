package dremel.common.loggingaspect;

import dremel.dataset.impl.Slice;
import dremel.compiler.parser.AstNode;

public aspect TraceAspect {

	before(String bql): execution(AstNode parseBql(String))&& args(bql)
	{
		System.out.println("BQL:"+bql);
	}
	
	after() returning(AstNode nodes): call(AstNode parseBql(String))
	{
		System.out.println("AST:"+nodes.toStringTree());
	}

	before(String code) : execution(public dremel.executor.impl.MetaxaExecutor.JavaLangScript.new(String)) && args(code) {
		System.out.println("GENERATED CODE:");
		System.out.println(code);
	}

	after(Object[] objs): execution(void dremel.executor.impl.MetaxaExecutor.JavaLangScript.evaluate(Object[]))&& args(objs)
	{
		assert (objs.length==4);
		assert (objs[0] instanceof Slice);
		assert (objs[1] instanceof Slice);
		Slice inSlice = (Slice)objs[0];
		Slice outSlice = (Slice)objs[1];
		
		System.out.print("ISLICE:\t");
		for (int i = 0; i < inSlice.count(); i++) {
			System.out.print(inSlice.getValue(i) + "\t\t");
		}
		if (outSlice.isNull())
			System.out.println("OSLICE:\tSLICE IS NULL");
		else {
			System.out.print("OSLICE:\t");
			for (int i = 0; i < outSlice.count(); i++) {
				System.out.print(outSlice.getValue(i) + "\t\t");
			}
			System.out.println();
		}
	}
}
