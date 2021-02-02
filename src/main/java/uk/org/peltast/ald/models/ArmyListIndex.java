package uk.org.peltast.ald.models;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/** Maintains an index of all your army lists together with useful information
 * extracted from each. Does not do any IO, the caller has to do that and just
 * tell this index what is happening.
 * 
 * @author Mark Andrew Wheadon
 * @date 6th August 2013.
 * @copyright Mark Andrew Wheadon, 2013,2020.
 * @licence MIT License.
 */
public class ArmyListIndex {
	private static final Logger log = LoggerFactory.getLogger(ArmyListIndex.class);
	private enum NodeNames{ENTRY, ENTRIES}
	private enum AttrNames{ID, NAME, BOOK, YEAR, POINTS}

	private class Entry {
		String mId;
		String mName;
		String mBook;
		String mYear;
		String mPoints;
		public String toString() {
			return(MessageFormat.format("{0}: {1}, {2}, {3}. {4}", mId, mName, mBook, mYear, mPoints ));
		}
	}
	private final List<Entry> mEntries = new ArrayList<Entry>();

	//--------------------------------------------------------------------------
	public int getEntryCount() {
		int sz = mEntries.size();
		return(sz);
	}

	//--------------------------------------------------------------------------
	public String[] getEntryIDs() {
		String[] ids = new String[mEntries.size()];
		int ii = 0;
		for (Entry entry : mEntries) {
			ids[ii] = entry.mId;
			ii++;
		}
		return (ids);
	}

	//--------------------------------------------------------------------------
	public String getName(String id) {
		Entry entry = findEntry2(id);
		return(entry.mName);
	}

	//--------------------------------------------------------------------------
	public String getBook(String id) {
		Entry entry = findEntry2(id);
		return (entry.mBook);
	}

	//--------------------------------------------------------------------------
	public String getYear(String id) {
		Entry entry = findEntry2(id);
		return (entry.mYear);
	}

	//--------------------------------------------------------------------------
	public String getPoints(String id) {
		Entry entry = findEntry2(id);
		return (entry.mPoints);
	}

	//--------------------------------------------------------------------------
	public void delete(String id) {
		Entry entry = findEntry2(id);
		mEntries.remove(entry);
		log.info("Army list with ID {} and name {} deleted", id, entry.mName);
	}

	//--------------------------------------------------------------------------
	public void clear() {
		mEntries.clear();
		log.info("Army index cleared");
	}

	//--------------------------------------------------------------------------
	public boolean exists(String id) {
		Entry entry = findEntry(id);
		if (entry == null) {
			return (false);
		}
		log.info("Army with id {} exists", id);
		return (true);
	}

	//--------------------------------------------------------------------------
	/** Gets the index as XML (presumably for storing somewhere).
	 * @return XML representation of the index. 
	 * @throws IOException 
	 * @throws XMLStreamException */
	public String getAsXML() throws IOException, XMLStreamException {
		XMLOutputFactory factory = XMLOutputFactory.newFactory();
		try (StringWriter sw = new StringWriter()) {
			XMLStreamWriter writer = factory.createXMLStreamWriter(sw);
			writer.writeStartDocument();
			writer.writeStartElement(NodeNames.ENTRIES.toString().toLowerCase());

			for (Entry entry : mEntries) {
				writer.writeStartElement(NodeNames.ENTRY.toString().toLowerCase());
			    writeXMLAttribute(writer, AttrNames.ID, entry.mId);
			    writeXMLAttribute(writer, AttrNames.NAME, entry.mName);
			    writeXMLAttribute(writer, AttrNames.BOOK, entry.mBook);
			    writeXMLAttribute(writer, AttrNames.YEAR, entry.mYear);
			    writeXMLAttribute(writer, AttrNames.POINTS, entry.mPoints);
			    writer.writeEndElement();	// entry
			}
			writer.writeEndElement();	// entries
			writer.writeEndDocument();
			writer.close();
			String xml = sw.toString();
			return(xml);
		}
	}

	//--------------------------------------------------------------------------
	private static void writeXMLAttribute(XMLStreamWriter writer, AttrNames name, String value) throws XMLStreamException {
		if (value != null && !value.isEmpty()) {
		    writer.writeAttribute(name.toString().toLowerCase(), value);
		}
	}

	//--------------------------------------------------------------------------
	public void loadFromXML(String xml) throws ParserConfigurationException, SAXException, IOException {
		mEntries.clear();
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		dbf.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, ""); // Compliant
		dbf.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, ""); // compliant
		DocumentBuilder db = dbf.newDocumentBuilder();
		StringReader sr = new StringReader(xml);
		InputSource is = new InputSource(sr);
		Document doc = db.parse(is);
		NodeList entryNl = doc.getElementsByTagName(NodeNames.ENTRY.toString().toLowerCase());
		int entryCount = entryNl.getLength();
		for (int ii = 0; ii < entryCount; ii++) {
			Node entryNode = entryNl.item(ii);
			NamedNodeMap attrsNnm = entryNode.getAttributes();
			Entry entry = new Entry();
			entry.mId = getAttribute(attrsNnm, AttrNames.ID);
			entry.mName = getAttribute(attrsNnm, AttrNames.NAME);
			entry.mBook = getAttribute(attrsNnm, AttrNames.BOOK);
			entry.mYear = getAttribute(attrsNnm, AttrNames.YEAR);
			entry.mPoints = getAttribute(attrsNnm, AttrNames.POINTS);
			mEntries.add(entry);
		}
	}

	//--------------------------------------------------------------------------
	private static String getAttribute(NamedNodeMap nnm, AttrNames name) {
		Node nn = nnm.getNamedItem(name.toString().toLowerCase());
		String value = nn.getNodeValue();
		return (value);
	}

	//--------------------------------------------------------------------------
	public void addEntry(String id, String name, String book, String year, String points) {
		Entry entry = findEntry(id);
		if (entry != null) {
			throw new IllegalArgumentException("ID " + id + " is already in the index. Duplicates not allowed");
		}
		entry = new Entry();
		entry.mId = id;
		entry.mName = name;
		entry.mBook = book;
		entry.mYear = year;
		entry.mPoints = points;
		mEntries.add(entry);
	}

	//--------------------------------------------------------------------------
	public void updateEntry(String id, String name, String book, String year, String points) {
		Entry entry = findEntry(id);
		if (entry == null) {
			throw new IllegalArgumentException("ID " + id + " not in index");
		}
		entry.mName = name;
		entry.mBook = book;
		entry.mYear = year;
		entry.mPoints = points;
	}

	//--------------------------------------------------------------------------
	private Entry findEntry(String id) {
		for (Entry entry : mEntries) {
			if (entry.mId.equals(id)) {
				return (entry);
			}
		}
		return (null);
	}

	//--------------------------------------------------------------------------
	private Entry findEntry2(String id) {
		for (Entry entry : mEntries) {
			if (entry.mId.equals(id)) {
				return (entry);
			}
		}
		throw new IllegalArgumentException("Army list with id " + id + "{0} not found.");
	}

	//--------------------------------------------------------------------------
	public String toString() {
		StringBuilder sb = new StringBuilder();
		for (int ii=0; ii<mEntries.size(); ii++) {
			Entry entry = mEntries.get(ii);
			if (sb.length() > 0) {
				sb.append('\n');
			}
			sb.append(entry.toString());
		}
		return(sb.toString());
	}
}
