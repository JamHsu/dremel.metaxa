package dremel.dataset.impl;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Vector;

//import dremel.compiler.impl.FieldDescriptor;

//import schema.Schema.Document;

/**
 * (this class is taken from test assignment)
 * @author nhsan
 *
 */
public class FileFieldReader implements FieldReader
{
	HashMap<String, BufferedReader> sources;
	HashMap<String, RowData> datas;
	String sourceDir;

	public FileFieldReader(String sourceDir) throws FileNotFoundException
	{
		this.sourceDir = sourceDir;
		sources = new HashMap<String, BufferedReader>();
		datas = new HashMap<String, RowData>();

		File dir = new File(sourceDir);

		String[] children = dir.list();
		if (children != null)
		{
			for (int i = 0; i < children.length; i++)
			{
				String filename = children[i];
				if (filename.endsWith(".txt"))
				{
					FileInputStream fstream = new FileInputStream(sourceDir + "/" + filename);
					DataInputStream in = new DataInputStream(fstream);
					BufferedReader br = new BufferedReader(new InputStreamReader(in));
					String fieldName = filename.substring(0, filename.length() - 4);
					sources.put(fieldName, br);
				}
			}
		}
	}

	private RowData parseLine(String line)
	{
		String[] lst = line.split("\t");
		if (lst.length != 3) return null;
		int rlevel = Integer.parseInt(lst[0]);
		int dlevel = Integer.parseInt(lst[1]);
		RowData ret = new RowData(lst[2], rlevel, dlevel);
		return ret;
	}

	@Override
	public RowData take(String field)
	{
		RowData ret = datas.get(field);
		if (ret == null)
		{
			BufferedReader br = sources.get(field);
			try
			{
				String line = br.readLine();
				if (line != null)
				{
					RowData data = parseLine(line);
					return data;
				}
				else
				{
					br.close();
					return null;
				}
			}
			catch (IOException e)
			{
				//e.printStackTrace();
				return null;
			}
		}
		else
		{
			datas.put(field, null);// remove
			return ret;
		}
	}

	@Override
	public RowData head(String field)
	{
		RowData ret = datas.get(field);

		if (ret == null)
		{
			BufferedReader br = sources.get(field);
			try
			{
				String line = br.readLine();
				if (line != null)
				{
					RowData data = parseLine(line);
					datas.put(field, data);
					return data;
				}
				else
				{
					br.close();
					return null;
				}
			}
			catch (IOException e)
			{
				//e.printStackTrace();
				return null;
			}
		}
		else
		{
			return ret;
		}
	}

//	public static void main(String[] args) throws FileNotFoundException
//	{
//		//test
//		FileFieldReader reader = new FileFieldReader("output");
//		SchemaInfo info = new SchemaInfo(Document.getDescriptor());
//		Vector<GenericFieldDesc> fields = info.getFields();
//
//		for (int i = 0; i < fields.size(); i++)
//		{
//			String fieldname = fields.get(i).getFullName();
//			System.out.println(fieldname);
//			RowData data=reader.take(fieldname);
//			while (data!=null)
//			{
//				System.out.println(data.getRlevel()+"\t"+data.getDlevel()+"\t"+data.getValue());
//				data=reader.take(fieldname);
//			}
//		}
//	}
}
