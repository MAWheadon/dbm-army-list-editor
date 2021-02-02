package uk.org.peltast.ald.models;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;

import javax.xml.stream.XMLStreamException;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class ArmyListDBMModelTest {
	private static final Logger log = LoggerFactory.getLogger(ArmyListDBMModelTest.class);
	private static ArmyListDBMModel mdl;

	@BeforeAll
	static void setUpBeforeClass() throws Exception {
		log.info("Setting up test army list");
		mdl = new ArmyListDBMModel();
		ArmyListCosts costs = new ArmyListCosts("3", "3");
		mdl.setArmyCosts(costs);

		mdl.setArmyBook("Book 2");
		mdl.setArmyName("Alexandrian Makedonia");
		mdl.setArmyYear("330BC");

		int rowIdx = mdl.addRow();
		mdl.setRowDescription(rowIdx, "Alexander");
		mdl.setRowDrill(rowIdx, "Reg");
		mdl.setRowType(rowIdx, "Kn");
		mdl.setRowGrade(rowIdx, "F");
		mdl.setRowAdjustment(rowIdx, "gen");
		mdl.setRowQuantity(rowIdx, 1);
		mdl.setRowCommandQuantity(rowIdx, 1, 1);

		rowIdx = mdl.addRow();
		mdl.setRowDescription(rowIdx, "Companions");
		mdl.setRowDrill(rowIdx, "Reg");
		mdl.setRowType(rowIdx, "Kn");
		mdl.setRowGrade(rowIdx, "F");
		mdl.setRowQuantity(rowIdx, 5);
		mdl.setRowCommandQuantity(rowIdx, 1, 5);

		rowIdx = mdl.addRow();
		mdl.setRowDescription(rowIdx, "Xenophon");
		mdl.setRowDrill(rowIdx, "Reg");
		mdl.setRowType(rowIdx, "Sp");
		mdl.setRowGrade(rowIdx, "O");
		mdl.setRowAdjustment(rowIdx, "ally");
		mdl.setRowQuantity(rowIdx, 1);
		mdl.setRowCommandQuantity(rowIdx, 2, 1);

		rowIdx = mdl.addRow();
		mdl.setRowDescription(rowIdx, "Hoplites");
		mdl.setRowDrill(rowIdx, "Reg");
		mdl.setRowType(rowIdx, "Sp");
		mdl.setRowGrade(rowIdx, "O");
		mdl.setRowQuantity(rowIdx, 11);
		mdl.setRowCommandQuantity(rowIdx, 2, 11);

		rowIdx = mdl.addRow();
		mdl.setRowDescription(rowIdx, "Psiloi");
		mdl.setRowDrill(rowIdx, "Irr");
		mdl.setRowType(rowIdx, "Ps");
		mdl.setRowGrade(rowIdx, "I");
		mdl.setRowQuantity(rowIdx, 5);
		mdl.setRowCommandQuantity(rowIdx, 2, 5);

		log.info("Completed setting up test army list");
	}

	@Test
	final void testGetArmyElements() {
		int elements = mdl.getArmyElements();
		log.info("Army elements is {}", elements);
		assertTrue(elements > 0);
	}

	@Test
	final void testGetArmyElementEquivalents() {
		float eq = mdl.getArmyEquivalents();
		log.info("Army equivalents is {}", eq);
		assertTrue(eq > 0);
	}

	@Test
	final void testDeleteRow() {
		mdl.deleteRow(4);	// psiloi row 
	}

	@Test
	final void testAddRowIntBoolean() {
		int idx = mdl.addRow(3, false);
		log.info("New row index is {}", idx);
		assertTrue(idx > 0);
	}

	@Test
	final void testMoveRowUp() {
		mdl.moveRowUp(3);
		String txt = mdl.getAsPlainText();
		log.info("After move up row 3:\n{}", txt);
	}

	@Test
	final void testMoveRowDown() {
		mdl.moveRowDown(2);
		String txt = mdl.getAsPlainText();
		log.info("After move down row 2:\n{}", txt);
	}

	@Test
	final void testGetId() {
		String id = mdl.getArmyId();
		log.info("Army ID is {}", id);
		assertNotNull(id);
	}

	@Test
	final void testGetArmyName() {
		String name = mdl.getArmyName();
		log.info("Army name is {}", name);
		assertNotNull(name);
	}

	@Test
	final void testGetArmyBook() {
		String book = mdl.getArmyBook();
		log.info("Army book is {}", book);
		assertNotNull(book);
	}

	@Test
	final void testGetArmyYear() {
		String year = mdl.getArmyYear();
		log.info("Army year is {}", year);
		assertNotNull(year);
	}

	@Test
	final void testGetArmyCost() {
		float cost = mdl.getArmyCost();
		log.info("Army cost is {}", cost);
		assertTrue(cost > 0);
	}

	@Test
	final void testGetRowQuantity() {
		int qty = mdl.getRowQuantity(0);
		log.info("Row 0 quantity is {}", qty);
		assertEquals(1, qty);
	}

	@Test
	final void testGetRowDescription() {
		String txt = mdl.getRowDescription(0);
		log.info("Row 0 description is {}", txt);
		assertNotNull(txt);
	}

	@Test
	final void testGetRowDrill() {
		String txt = mdl.getRowDrill(0);
		log.info("Row 0 drill is {}", txt);
		assertNotNull(txt);
	}

	@Test
	final void testGetRowType() {
		String txt = mdl.getRowType(0);
		log.info("Row 0 type is {}", txt);
		assertNotNull(txt);
	}

	@Test
	final void testGetRowGrade() {
		String txt = mdl.getRowGrade(0);
		log.info("Row 0 grade is {}", txt);
		assertNotNull(txt);
	}

	@Test
	final void testGetRowAdjustment() {
		String txt = mdl.getRowAdjustment(0);
		log.info("Row 0 adjustment is {}", txt);
		assertNotNull(txt);
	}

	@Test
	final void testGetRowUnusedQuantity() {
		int unused = mdl.getRowUnusedQuantity(0);
		log.info("Unused on row 0 is {}", unused);
		assertEquals(0, unused);
	}

	@Test
	final void testSaveAndLoadXML() throws XMLStreamException, IOException {
		String txt1 = mdl.getAsPlainText();
		String xml = mdl.getAsXML();
		assertNotNull(xml);
		log.info("Army list XML is:\n{}", xml);
		mdl.clearArmyist();
		mdl.loadFromXML(xml);
		String txt2 = mdl.getAsPlainText();
		log.info("Army list reloaded from XML and printed as plain text is:\n{}", txt2);
		assertEquals(txt1, txt2);
	}

	@Test
	final void testGetAsPlainText() {
		String txt = mdl.getAsPlainText();
		assertNotNull(txt);
		log.info("Army list as plain text is:\n{}", txt);
	}

	@Test
	final void testRecalcTotals() {
		mdl.getAsPlainText();	// force recalculate totals
		int cmdEls = 0;
		float cmdEqs = 0;
		float cmdCost = 0;
		for (int cc=1; cc<=4; cc++) {
			cmdEls += mdl.getCommandElements(cc);
			cmdEqs += mdl.getCommandEquivelents(cc);
			cmdCost += mdl.getCommandCost(cc);
		}
		assertEquals(mdl.getArmyCost(), cmdCost);
		assertEquals(mdl.getArmyElements(), cmdEls);
		assertEquals(mdl.getArmyEquivalents(), cmdEqs);
	}

}
