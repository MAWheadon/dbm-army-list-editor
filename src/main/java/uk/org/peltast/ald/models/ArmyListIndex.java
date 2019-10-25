package uk.org.peltast.ald.models;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.maw.armylistdesigner.ArmyListConstants;
import com.maw.util.WLog;

/** Maintains an index of all your army lists together with useful 
 * information extracted from each. Does not do any IO, the caller has to do that and just tell this index what is happening.
 * 
 * @author Mark Andrew Wheadon
 * @date 6th August 2013.
 * @copyright Mark Andrew Wheadon, 2013,2016.
 * @licence MIT License.
 */
public class ArmyListIndex {
	private class Entry {
		String m_id;
		String m_name;
		String m_book;
		String m_year;
		String m_points;
	}
	private final List<Entry> m_entries = new ArrayList<Entry>();

	//--------------------------------------------------------------------------
	public ArmyListIndex() {
	}

	//--------------------------------------------------------------------------
	public int getEntryCount() {
		int sz = m_entries.size();
		return(sz);
	}

	//--------------------------------------------------------------------------
	public String[] getEntryIDs() {
		String[] ids = new String[m_entries.size()];
		int ii = 0;
		for (Entry entry : m_entries) {
			ids[ii] = entry.m_id;
			ii++;
		}
		return (ids);
	}

	//--------------------------------------------------------------------------
	public String getName(String id) {
		Entry entry = find_entry2(id);
		return(entry.m_name);
	}

	//--------------------------------------------------------------------------
	public String getBook(String id) {
		Entry entry = find_entry2(id);
		return (entry.m_book);
	}

	//--------------------------------------------------------------------------
	public String getYear(String id) {
		Entry entry = find_entry2(id);
		return (entry.m_year);
	}

	//--------------------------------------------------------------------------
	public String getPoints(String id) {
		Entry entry = find_entry2(id);
		return (entry.m_points);
	}

	//--------------------------------------------------------------------------
	public void delete(String id) {
		Entry entry = find_entry2(id);
		m_entries.remove(entry);
		WLog.log(Level.INFO,"Army list with ID {0} and name {1} deleted.",id, entry.m_name);
	}

	//--------------------------------------------------------------------------
	public void clear() {
		m_entries.clear();
		WLog.log(Level.INFO, "Army index cleared.");
	}

	//--------------------------------------------------------------------------
	public boolean exists(String id) {
		Entry entry = find_entry(id);
		if (entry == null) {
			return (false);
		}
		WLog.log(Level.INFO, "Army with id {0} exists.", id);
		return (true);
	}

	//--------------------------------------------------------------------------
	/** Gets the index as XML (presumably for storing somewhere).
	 * @return XML representation of the index. */
	public String getAsXML() {
		StringBuilder sb = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
		sb.append("<army_index>\n");
		sb.append("\t<entries>\n");
		
		for (Entry entry : m_entries) {
			sb.append("\t\t<entry");
			append_xml_attr(sb, ArmyListConstants.c_army_id,entry.m_id);
			append_xml_attr(sb, ArmyListConstants.c_army_name,entry.m_name);
			append_xml_attr(sb, ArmyListConstants.c_army_book,entry.m_book);
			append_xml_attr(sb, ArmyListConstants.c_army_year,entry.m_year);
			append_xml_attr(sb, ArmyListConstants.c_army_points,entry.m_points);
			sb.append(" />\n");
		}	// for - each row

		sb.append("\t</entries>\n");
		sb.append("</army_index>");
		return(sb.toString());
	}

	//--------------------------------------------------------------------------
	private static void append_xml_attr(StringBuilder sb, String name, String value) {
		sb.append(' ');
		sb.append(name);
		sb.append("=\"");
		sb.append(value);
		sb.append("\"");
	}

	//--------------------------------------------------------------------------
	public void loadFromXML(String xml) throws Exception {
		m_entries.clear();
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db = dbf.newDocumentBuilder();
		Document doc = db.parse(xml);
		NodeList entry_nl = doc.getElementsByTagName("entry");
		int entry_count = entry_nl.getLength();
		for (int ii = 0; ii < entry_count; ii++) {
			Node entry_n = entry_nl.item(ii);
			NamedNodeMap attrs_nnm = entry_n.getAttributes();
			Entry entry = new Entry();
			entry.m_id = get_attribute(attrs_nnm, ArmyListConstants.c_army_id);
			entry.m_name = get_attribute(attrs_nnm, ArmyListConstants.c_army_name);
			entry.m_book = get_attribute(attrs_nnm, ArmyListConstants.c_army_book);
			entry.m_year = get_attribute(attrs_nnm, ArmyListConstants.c_army_year);
			entry.m_points = get_attribute(attrs_nnm, ArmyListConstants.c_army_points);
			m_entries.add(entry);
		} // for - each entry
	}

	//--------------------------------------------------------------------------
	private static String get_attribute(NamedNodeMap nnm, String name) {
		Node nn = nnm.getNamedItem(name);
		String value = nn.getNodeValue();
		return (value);
	}

	//--------------------------------------------------------------------------
	public void addEntry(String id, String name, String book, String year, String points) {
		Entry entry = find_entry(id);
		if (entry != null) {
			throw new IllegalArgumentException("ID " + id + " is already in the index. Duplicates not allowed");
		}
		entry = new Entry();
		entry.m_id = id;
		entry.m_name = name;
		entry.m_book = book;
		entry.m_year = year;
		entry.m_points = points;
		m_entries.add(entry);
	}

	//--------------------------------------------------------------------------
	public void updateEntry(String id, String name, String book, String year, String points) {
		Entry entry = find_entry(id);
		if (entry == null) {
			throw new IllegalArgumentException("ID " + id + " not in index");
		}
		entry.m_name = name;
		entry.m_book = book;
		entry.m_year = year;
		entry.m_points = points;
	}

	//--------------------------------------------------------------------------
	private Entry find_entry(String id) {
		for (Entry entry : m_entries) {
			if (entry.m_id.equals(id)) {
				return (entry);
			}
		}
		return (null);
	}

	//--------------------------------------------------------------------------
	private Entry find_entry2(String id) {
		for (Entry entry : m_entries) {
			if (entry.m_id.equals(id)) {
				return (entry);
			}
		}
		throw new IllegalArgumentException("Army list with id " + id + "{0} not found.");
	}

	//--------------------------------------------------------------------------
	public static void main(String[] args) {
		try {
			WLog.setLogFile(1);
			WLog.setLevel(Level.FINEST);
			ArmyListIndex ali = new ArmyListIndex();
			ali.addEntry("123", "Later Swiss", "4", "1400", "425");
			ali.addEntry("456", "Thracian", "1", "400BC", "400");
			String xml = ali.getAsXML();
			ali.clear();
			ali.loadFromXML(xml);
			ali.delete("123");
			boolean exists = ali.exists("123");
			exists = ali.exists("abc");
			String name = ali.getName("456");
			ali.clear();
		}	// try
		catch (Exception e) {
			System.out.print("Error: "+e);
		}	// catch
		WLog.close();
	}	// main
}	// ArmyListIndex
