package dremel.dataset;

import java.util.List;

import org.junit.Test;

import dremel.dataset.impl.SchemaTreeLoader;

public class TestSchemaTreeDb {

	@Test
	public void Test1() {
		SchemaTree schema = SchemaTreeLoader.loadSchema("[document]");
		System.out.println("--- "+schema.getName()+" ---");
		printNode(schema, "");
		//SchemaTreeLoader.cloneSchema(schema, "Document_copy1");
	}
	
	private void printNode(SchemaTree node, String ident) {
		List<SchemaTree> flds = node.getFieldsList();
		if (flds != null) {
			for (SchemaTree n : flds) {
				String hdr = ": ";
				if (n.isRequired())
					hdr = "REQ: ";
				if (n.isOptional())
					hdr = "OPT: ";
				if (n.isRepeated())
					hdr = "RPT: ";
				String cdr = ": ";
				if (n.isRequired())
					hdr = "REQ: ";
				if (n.isOptional())
					hdr = "OPT: ";
				if (n.isRepeated())
					hdr = "RPT: ";
				System.out.println(ident+"|\n"+ident+"\\- "+hdr+n.getName()+" |");
				printNode(n, ident+"\t");
			}
		}
	}
}
