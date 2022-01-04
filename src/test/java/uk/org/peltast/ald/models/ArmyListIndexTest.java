package uk.org.peltast.ald.models;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.util.Arrays;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLStreamException;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

class ArmyListIndexTest {
	private static final Logger log = LoggerFactory.getLogger(ArmyListIndexTest.class);
	private static ArmyListIndex index;

	@BeforeAll
	static void setUpBeforeClass() {
		index = new ArmyListIndex();
		index.addEntry("12345", "", "Alexandrian Makedonian", "Book 2", "330BC", "400");
		index.addEntry("84725", "", "Bagratid Armenian", "Book 3", "850AD", "350");
		index.addEntry("h73cd", "Jewish Revolt", "Jewish Revolt - Zealots", "Book 2", "70AD", "350");
		index.addEntry("lk3jl", "Jewish Revolt", "Jewish Revolt - Josephus", "Book 2", "70AD", "350");
		index.addEntry("9jbnd", "", "Teutonic Orders", "Book 4", "1400AD", "450");
	}

	@Test
	final void testGetEntryCount() {
		int count = index.getEntryCount();
		log.info("Entry count is {}", count);
		assertTrue(count > 0);
	}

	@Test
	final void testGetEntryIDs() {
		String[] ids = index.getEntryIDs();
		log.info("Entry IDs are {}", Arrays.asList(ids));
		assertNotNull(ids);
	}

	@Test
	final void testGetName() {
		String txt = index.getName("12345");
		log.info("Entry 12345 name is {}", txt);
		assertEquals("Alexandrian Makedonian", txt);
	}

	@Test
	final void testGetBook() {
		String txt = index.getBook("12345");
		log.info("Entry 12345 book is {}", txt);
		assertEquals("Book 2", txt);
	}

	@Test
	final void testGetYear() {
		String txt = index.getYear("12345");
		log.info("Entry 12345 year is {}", txt);
		assertEquals("330BC", txt);
	}

	@Test
	final void testGetPoints() {
		String txt = index.getPoints("12345");
		log.info("Entry 12345 points is {}", txt);
		assertEquals("400", txt);
	}

	@Test
	final void testDelete() {
		index.delete("9jbnd");
		int count = index.getEntryCount();
		assertEquals(4, count);
	}

	@Test
	final void testExists() {
		boolean exists = index.exists("12345");
		assertTrue(exists);
		log.info("Entry 12345 exists? {}", exists);
	}

	@Test
	final void testGetAsXML() throws IOException, XMLStreamException, ParserConfigurationException, SAXException {
		String xml = index.getAsXML();
		log.info("Index XML is:\n{}", xml);
		index.clear();
		index.loadFromXML(xml);
		String xml2 = index.getAsXML();
		log.info("Index XML after reload is:\n{}", xml2);
		assertEquals(xml, xml2);
	}

	@Test
	final void testUpdateEntry() {
		String txt = index.toString();
		log.info("Entries are:\n{}", txt);
		index.updateEntry("84725", "", "Thracian", "Book 1", "250BC", "425");
		txt = index.toString();
		assertNotNull(txt);
		log.info("After update entries are:\n{}", txt);
	}

	@Test
	final void testGetGroups() {
		String[] groups = index.getGroups();
		assertEquals(1, groups.length);
		log.info("Groups are: {}", (Object[])groups);
	}

	@Test
	final void testGetArmiesInGroup() {
		String group = "Jewish Revolt";
		String[] armies = index.getArmiesInGroup(group);
		assertEquals(2, armies.length);
		log.info("Armies in group {} are: {}", group, armies);
	}

}
