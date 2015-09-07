import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class XMLParser
{
	private DocumentBuilder mBuilder;
	private Transformer mTransformer;
	private PrintWriter mPrintWriter;
	
	public XMLParser() throws Exception
	{
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		mBuilder = factory.newDocumentBuilder();
		mTransformer = TransformerFactory.newInstance().newTransformer();
		mTransformer.setOutputProperty(OutputKeys.INDENT, "yes");
		mTransformer.setOutputProperty(OutputKeys.DOCTYPE_PUBLIC, "yes");
		mTransformer.setOutputProperty(OutputKeys.ENCODING, "utf-8");
		
		File file = new File("report.txt");
		file.createNewFile();
		FileWriter fw = new FileWriter(file);
		
		mPrintWriter = new PrintWriter(fw);
	}
	
	public static void main(String[] args)
	{
			System.out.println("The current defaultCharset is " + Charset.defaultCharset());
			System.out.println("Please change your project to utf-8");
			XMLParser parser = null;
			try
			{
				parser = new XMLParser();
				parser.createValuesDirectorys();
				parser.copyEnglishXMLToDirectorys();
				parser.fixAllXml();
			}
			catch (StringsFileNotFoundException e)
			{
				parser.println("Please copy the default strings.xml directory into this project");
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
			finally
			{
				parser.release();
			}

	}
	
	public void createNewXmlByOld(Document oldDoc) throws Exception
	{
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = factory.newDocumentBuilder();
		Document doc = builder.newDocument();
		doc.appendChild(doc.importNode(oldDoc.getDocumentElement(), true));
		doc.setXmlStandalone(true);
		DOMSource source = new DOMSource(doc);
		PrintWriter pw = new PrintWriter(new FileOutputStream("b.xml"));
		StreamResult result = new StreamResult(pw);
		Transformer transformer = TransformerFactory.newInstance()
				.newTransformer();
		transformer.transform(source, result);
	}
	
	public HashMap<String, String> loadMapFromXml(String path) throws Exception
	{
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = factory.newDocumentBuilder();
		File xmlFile = new File(path);
		if (!xmlFile.exists())
		{
			mPrintWriter.println(path + " is not exists!!!!");
			return null;
		}
		Document doc = builder.parse(xmlFile);
		NodeList nlist = doc.getElementsByTagName("string");
		HashMap<String, String> lanInfo = new HashMap<String, String>();
		for (int i = 0; i < nlist.getLength(); i++)
		{
			Element element = (Element) nlist.item(i);
			lanInfo.put(element.getAttribute("name"), changeCharSet(element.getTextContent()));
		}
		return lanInfo;
	}
	
	public HashMap<String, Element> loadNodesFromXml(String path) throws Exception
	{
		File xmlFile = new File(path);
		Document doc = mBuilder.parse(xmlFile);
		NodeList nlist = doc.getElementsByTagName("string");
		HashMap<String, Element> ret = new HashMap<String, Element>();
		for (int i = 0; i < nlist.getLength(); i++)
		{
			Element element = (Element) nlist.item(i);
			ret.put(element.getAttribute("name"), element);
		}
		return ret;
	}
	
	public void fixAllXml() throws Exception
	{
		List<String> folderNames = FileUtil.listPath("mylanguage");
		for (String f : folderNames)
		{
			if (f.startsWith("values"))
			{
				mPrintWriter.println("----------- The missing words in " + f + " -----------");
				fixXml("res/"+ f + "/strings.xml", "mylanguage/" + f + "/strings.xml");
			}
		}
	}
	
	public void fixXml(String oldXmlPath, String newXmlPath) throws Exception
	{
		File xmlFile = new File(newXmlPath);
		Document doc = mBuilder.parse(xmlFile);
		doc.setXmlStandalone(true);
		NodeList nlist = doc.getElementsByTagName("string");
		HashMap<String, String> oldXmlInfo = loadMapFromXml(oldXmlPath);
		if (oldXmlInfo == null)
		{
			return;
		}
		for (int i = 0; i < nlist.getLength(); i++)
		{
			Element element = (Element) nlist.item(i);
			String nameValue = element.getAttribute("name");
			if (oldXmlInfo.containsKey(nameValue))
			{
				element.setTextContent(oldXmlInfo.get(nameValue));
			}
			else
			{
				mPrintWriter.println(nameValue );
			}
		}
		DOMSource source = new DOMSource(doc);
		PrintWriter pw = new PrintWriter(new FileOutputStream(newXmlPath));
		StreamResult result = new StreamResult(pw);
		mTransformer.transform(source, result);
	}
	
	public void fixXml2(String oldXmlPath, String newXmlPath) throws Exception
	{
		File xmlFile = new File(newXmlPath);
		Document doc = mBuilder.parse(xmlFile);
		doc.setXmlStandalone(true);
		NodeList nlist = doc.getElementsByTagName("string");
		HashMap<String, Element> oldXmlInfo = loadNodesFromXml(oldXmlPath);
		for (int i = 0; i < nlist.getLength(); i++)
		{
			Element element = (Element) nlist.item(i);
			String nameValue = element.getAttribute("name");
			if (oldXmlInfo.containsKey(nameValue))
			{
				element.getParentNode().replaceChild(oldXmlInfo.get(nameValue), element);
			}
		}
		DOMSource source = new DOMSource(doc);
		PrintWriter pw = new PrintWriter(new FileOutputStream(newXmlPath));
		StreamResult result = new StreamResult(pw);
		mTransformer.transform(source, result);
	}
	
	public void createNewXml(Element root, String newXmlPath) throws Exception
	{
		Document doc = mBuilder.newDocument();
		doc.appendChild(doc.importNode(root, true));
		doc.setXmlStandalone(true);
		DOMSource source = new DOMSource(doc);
		PrintWriter pw = new PrintWriter(new FileOutputStream(newXmlPath));
		StreamResult result = new StreamResult(pw);
		mTransformer.transform(source, result);
	}
	
	public void createNewXmls() throws Exception
	{
		List<String> folderNames = FileUtil.listPath("mylanguage");
		Document oriDoc = mBuilder.parse(new File("mylanguage/values/strings.xml"));
		Element root = oriDoc.getDocumentElement();
		for (String s : folderNames)
		{
			if (s.startsWith("values-"))
			{
				createNewXml(root, "mylanguage/"+s+"/strings.xml");
			}
		}
	}
	
	public void createValuesDirectorys()
	{
		File file = new File("res");
		if (!file.exists())
		{
			mPrintWriter.println("Please copy the res directory into this project");
			return;
		}
		FileUtil.createDirectory(".", "mylanguage");
		List<String> folderNames = FileUtil.listPath("res");
		for (String f : folderNames)
		{
			if (f.startsWith("values"))
			{
				FileUtil.createDirectory("mylanguage", f);
			}
		}
	}
	
	
	
	public void copyEnglishXMLToDirectorys() throws StringsFileNotFoundException
	{
		File file = new File("strings.xml");
		if (!file.exists())
		{
			throw new StringsFileNotFoundException();
		}
		FileUtil.fileChannelCopy(file,new File("mylanguage/values/strings.xml"));
		List<String> folderNames = FileUtil.listPath("mylanguage");
		for (String f : folderNames)
		{
			if (f.startsWith("values-"))
			{
				FileUtil.fileChannelCopy(new File("mylanguage/values/strings.xml"),new File("mylanguage/"+f+"/strings.xml"));
			}
		}
	}
	
	public void removeOneNode(String attriName) throws Exception
	{
		File xmlFile = new File("mylanguage/values-ar/strings.xml");
		Document doc = mBuilder.parse(xmlFile);
		doc.setXmlStandalone(true);
		NodeList nlist = doc.getElementsByTagName("string");
		for (int i = 0; i < nlist.getLength(); i++)
		{
			Element element = (Element) nlist.item(i);
			String nameValue = element.getAttribute("name");
			if (nameValue.equals(attriName))
			{
				element.getParentNode().removeChild(element);
			}
		}
		DOMSource source = new DOMSource(doc);
		PrintWriter pw = new PrintWriter(new FileOutputStream("mylanguage/values-ar/strings.xml"));
		StreamResult result = new StreamResult(pw);
		mTransformer.transform(source, result);
	}
	
	public String changeCharSet(String a) throws UnsupportedEncodingException
	{
		byte[] b = a.getBytes("utf-8");
		String n = new String(b,"utf-8");
		return n;
	}
	
	public void println(String str)
	{
		mPrintWriter.println(str);
	}
	
	public void release()
	{
		mPrintWriter.close();
	}

}


class FileUtil
{
	public static void fileChannelCopy(File s, File t)
	{
		FileInputStream fi = null;
		FileOutputStream fo = null;
		try
		{
			fi = new FileInputStream(s);
			fo = new FileOutputStream(t);
			FileChannel in = fi.getChannel();
			FileChannel out = fo.getChannel();
			in.transferTo(0, in.size(), out);
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		finally
		{
			try
			{
				if (fo != null)
					fo.close();
				if (fi != null)
					fi.close();
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
			
		}
	}
	
	public static File createFile(String folderPath, String fileName)
	{
		File destDir = new File(folderPath);
		if (!destDir.exists())
		{
			destDir.mkdirs();
		}
		return new File(folderPath, fileName);
	}
	
	public static boolean createDirectory(String root, String directoryName)
	{
		boolean status;
		if (!directoryName.equals(""))
		{
			File newPath = new File(root + File.separator +directoryName);
			status = newPath.mkdir();
			status = true;
		}
		else
		{
			status = false;
		}
		return status;
	}
	
	public static List<String> listPath(String root)
	{
		List<String> allDir = new ArrayList<String>();
		File path = new File(root);
		if (path.isDirectory())
		{
			for (File f : path.listFiles())
			{
				if (f.isDirectory() && !f.getName().startsWith("."))
				{
					allDir.add(f.getName());
				}
			}
		}
		return allDir;
	}
	
}

class StringsFileNotFoundException extends Exception
{
	private static final long serialVersionUID = 1L;
	
}
