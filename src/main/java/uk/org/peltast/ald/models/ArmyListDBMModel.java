/*-------------------------------------------------------------------------------
-------------------------------------------------------------------------------*/

package uk.org.peltast.ald.models;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.TransformerException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/** A DBM army list.
 *
 * @author Mark Andrew Wheadon
 * @date 9th June 2012.
 * @copyright Mark Andrew Wheadon, 2012,2020.
 * @licence MIT License.
 */
public class ArmyListDBMModel {
	private static final Logger log = LoggerFactory.getLogger(ArmyListDBMModel.class);
	private static final int CMDS = 4;
	public enum ColumnNames {QUANTITY, DESCRIPTION, DRILL, TYPE, GRADE, ADJUSTMENT, COST, TOTAL, CMD1_QTY, CMD2_QTY, CMD3_QTY, CMD4_QTY, UNUSED}
	private enum AttributeNames{army, book, rules, version, id, name, year, rows, row, quantity, description, drill, type, grade, adjustment, cmdQty0, cmdQty1, cmdQty2, cmdQty3}

	private class Row {
		private int mQty;
		private String mDesc;
		private String mDrillName;
		private String mTypeName;
		private String mGradeName;
		private String mAdjustment;
		private float mCostPerElement;	// do not save
		private float mTotalRowCost; // do not save
		private int[] mCmdQty = new int[CMDS];
		private int mUnusedElements;	// do not save

		@Override
		public String toString() {
			String ss = String.format("%d %s %s %s %s %s %f %f %d %d %d %d %d", mQty, mDesc, mDrillName, mTypeName, mGradeName, mAdjustment, mCostPerElement, mTotalRowCost, mCmdQty[0], mCmdQty[1], mCmdQty[2], mCmdQty[3], mUnusedElements);
			return(ss);
		}
	}
	private final ArrayList<Row> mRows = new ArrayList<>();
	private String mArmyId = null;	// used to save the file and in the directory
	private String mArmyName;
	private String mArmyBook;
	private String mArmyYear;
	private ArmyListCosts mCosts;
	private Path mLastFileUsed;
	private boolean mChanged = false; 

	// Calculated values not to be saved.
	private class Totals {
		private int mElements;
		private float mEquivalents;
		private float mBreakPoint;
		private float mCost;
	}
	private Totals mArmyTotals = new Totals();
	private Totals[] mCommandTotals = new Totals[CMDS];

	//--------------------------------------------------------------------------
	public ArmyListDBMModel() {
		for (int cc=0; cc<CMDS; cc++) {
			mCommandTotals[cc] = new Totals();
		}
	}

	//--------------------------------------------------------------------------
	public ArmyListCosts getArmyCosts() throws ParserConfigurationException, SAXException, IOException {
		if (mCosts == null) {
			log.info("Costs are null so choose the most recent");
			List<ArmyListVersion> vers = ArmyListCosts.listAvailableVersions();
			int sz = vers.size();
			if (sz > 0) {
				ArmyListVersion ver = vers.get(sz-1);
				mCosts = new ArmyListCosts(ver);
			}
		}
		return(mCosts);
	}

	//--------------------------------------------------------------------------
	public void setArmyCosts(ArmyListCosts costs) {
		mCosts = costs;
	}

	//--------------------------------------------------------------------------
	public void setArmyCosts(ArmyListCosts costs, ArmyListModelChange changes) {
		mCosts = costs;
		updateAllTotals(changes);
		changes.changed(true);
	}

	//--------------------------------------------------------------------------
	public int getArmyElements() {
		return(mArmyTotals.mElements);
	}

	//--------------------------------------------------------------------------
	public float getArmyEquivalents() {
		return(mArmyTotals.mEquivalents);
	}

	//--------------------------------------------------------------------------
	public float getArmyBreakPoint() {
		return(mArmyTotals.mBreakPoint);
	}

	//--------------------------------------------------------------------------
	public float getArmyCost() {
		return(mArmyTotals.mCost);
	}

	//--------------------------------------------------------------------------
	/** Gets the command's total elements
	 * @param cmd 1-4
	 * @return Total elements */
	public int getCommandElements(int cmd) {
		return(mCommandTotals[cmd-1].mElements);
	}

	//--------------------------------------------------------------------------
	/** Gets the command's break point
	 * @param cmd 1-4
	 * @return The break point */
	public float getCommandBreakPoint(int cmd) {
		return(mCommandTotals[cmd-1].mBreakPoint);
	}

	//--------------------------------------------------------------------------
	/** Gets the command's total cost
	 * @param cmd 1-4
	 * @return The total cost */
	public float getCommandCost(int cmd) {
		return(mCommandTotals[cmd-1].mCost);
	}

	//--------------------------------------------------------------------------
	/** Gets the command's total equivalents
	 * @param cmd 1-4
	 * @return The total equivalents */
	public float getCommandEquivelents(int cmd) {
		return(mCommandTotals[cmd-1].mEquivalents);
	}

	//--------------------------------------------------------------------------
	public void deleteRow(int index) {
		mRows.remove(index);
	}

	//--------------------------------------------------------------------------
	public void deleteRow(int index, ArmyListModelChange changes) {
		mRows.remove(index);
		recalcTotals();
		changes.deleteRow(index);
		updateAllTotals(changes);
		changes.changed(true);
	}

	//--------------------------------------------------------------------------
	private void updateAllTotals(ArmyListModelChange changes) {
		changes.setField(ArmyListConstants.ARMY_HALF, Float.toString(mArmyTotals.mBreakPoint));
		changes.setField(ArmyListConstants.ARMY_POINTS, Float.toString(mArmyTotals.mCost));
		changes.setField(ArmyListConstants.ARMY_EL_COUNT, Integer.toString(mArmyTotals.mElements));
		changes.setField(ArmyListConstants.ARMY_EL_EQUIV, Float.toString(mArmyTotals.mEquivalents));

		changes.setField(ArmyListConstants.CMD1_BP, Float.toString(mCommandTotals[0].mBreakPoint));
		changes.setField(ArmyListConstants.CMD1_COST, Float.toString(mCommandTotals[0].mCost));
		changes.setField(ArmyListConstants.CMD1_EL_COUNT, Integer.toString(mCommandTotals[0].mElements));
		changes.setField(ArmyListConstants.CMD1_EQUIV, Float.toString(mCommandTotals[0].mEquivalents));

		changes.setField(ArmyListConstants.CMD2_BP, Float.toString(mCommandTotals[1].mBreakPoint));
		changes.setField(ArmyListConstants.CMD2_COST, Float.toString(mCommandTotals[1].mCost));
		changes.setField(ArmyListConstants.CMD2_EL_COUNT, Integer.toString(mCommandTotals[1].mElements));
		changes.setField(ArmyListConstants.CMD2_EQUIV, Float.toString(mCommandTotals[1].mEquivalents));

		changes.setField(ArmyListConstants.CMD3_BP, Float.toString(mCommandTotals[2].mBreakPoint));
		changes.setField(ArmyListConstants.CMD3_COST, Float.toString(mCommandTotals[2].mCost));
		changes.setField(ArmyListConstants.CMD3_EL_COUNT, Integer.toString(mCommandTotals[2].mElements));
		changes.setField(ArmyListConstants.CMD3_EQUIV, Float.toString(mCommandTotals[2].mEquivalents));

		changes.setField(ArmyListConstants.CMD4_BP, Float.toString(mCommandTotals[3].mBreakPoint));
		changes.setField(ArmyListConstants.CMD4_COST, Float.toString(mCommandTotals[3].mCost));
		changes.setField(ArmyListConstants.CMD4_EL_COUNT, Integer.toString(mCommandTotals[3].mElements));
		changes.setField(ArmyListConstants.CMD4_EQUIV, Float.toString(mCommandTotals[3].mEquivalents));
	}

	//--------------------------------------------------------------------------
	private void updateAllLineCosts(ArmyListModelChange changes) {
		int sz = mRows.size();
		for (int rowIndex=0; rowIndex<sz; rowIndex++) {
			Row row = mRows.get(rowIndex);
			changes.setRowField(ArmyListConstants.ROW_TROOP_COST, rowIndex, row.mCostPerElement);
			changes.setRowField(ArmyListConstants.ROW_LINE_COST, rowIndex, row.mTotalRowCost);
		}
	}

	//--------------------------------------------------------------------------
	/** Adds a blank row.
	 * @return The index of the row just added. */
	public int addRow() {
		Row row = new Row();
		mRows.add(row);
		int sz = mRows.size();
		log.info("Added row {}", sz-1);
		return(sz-1);
	}

	//--------------------------------------------------------------------------
	/** Adds a blank row.
	 * @return The index of the row just added. */
	public int addRow(ArmyListModelChange changes) {
		int row = addRow();
		changes.addRow();
		changes.changed(true);
		return(row);
	}

	//--------------------------------------------------------------------------
	/** Inserts a blank row.
	 * @param index The nought based row number.
	 * @param above True to add above the desired row.
	 * @return The index of the row just added. */
	public int addRow(int index, boolean above) {
		Row row = new Row();
		int idx = index;
		if (!above) {
			idx++;
		}
		mRows.add(idx, row);
		log.info("Added row {}", idx);
		return(idx);
	}

	//--------------------------------------------------------------------------
	public void moveRowUp(int index) {
		Row row = mRows.remove(index);
		mRows.add(index-1,row);
	}

	//--------------------------------------------------------------------------
	public void moveRowDown(int index) {
		Row row = mRows.remove(index);
		mRows.add(index+1,row);
	}

	//--------------------------------------------------------------------------
	public String getArmyId() {
		if (mArmyId == null) {
			Date now = new Date();
			long millis = now.getTime();
			mArmyId = Long.toString(millis, 36);
		}
		return(mArmyId);
	}

	//--------------------------------------------------------------------------
	public String getArmyName() {
		return(mArmyName);
	}

	//--------------------------------------------------------------------------
	public void setArmyName(String name) {
		mArmyName = name;
	}

	//--------------------------------------------------------------------------
	public void setArmyName(String name, ArmyListModelChange change) {
		mArmyName = name;
		setChanged(change);
	}

	//--------------------------------------------------------------------------
	public String getArmyBook() {
		return(mArmyBook);
	}

	//--------------------------------------------------------------------------
	public void setArmyBook(String book) {
		mArmyBook = book;
	}

	//--------------------------------------------------------------------------
	public void setArmyBook(String book, ArmyListModelChange change) {
		mArmyBook = book;
		setChanged(change);
	}

	//--------------------------------------------------------------------------
	public String getArmyYear() {
		return(mArmyYear);
	}

	//--------------------------------------------------------------------------
	public void setArmyYear(String year) {
		mArmyYear = year;
	}

	//--------------------------------------------------------------------------
	public void setArmyYear(String year, ArmyListModelChange change) {
		mArmyYear = year;
		setChanged(change);
	}

	//--------------------------------------------------------------------------
	/** Sets a quantity for the row.
	 * @param rowIndex 0 based row index.
	 * @param quantity The number of elements. */
	public void setRowQuantity(int rowIndex, int quantity) {
		Row row = mRows.get(rowIndex);
		row.mQty = quantity;
	}

	//--------------------------------------------------------------------------
	/** Sets a quantity for the row.
	 * @param rowIndex 0 based row index.
	 * @param quantity The number of elements. */
	public void setRowQuantity(int rowIndex, int quantity, ArmyListModelChange changes) {
		setRowQuantity(rowIndex, quantity);
		recalcTotals();
		changes.setField(ArmyListConstants.ARMY_EL_COUNT, Integer.toString(mArmyTotals.mElements));
		changes.setField(ArmyListConstants.ARMY_POINTS, Float.toString(mArmyTotals.mCost));
		changes.setField(ArmyListConstants.ARMY_EL_EQUIV, Float.toString(mArmyTotals.mEquivalents));
		changes.setRowField(ArmyListConstants.ROW_UNUSED, rowIndex, getRowUnusedQuantity(rowIndex));
		changes.changed(true);
	}

	//--------------------------------------------------------------------------
	/** Gets a quantity for the row.
	 * @param rowIndex The nought based row number.
	 * @return The number of elements. */
	public int getRowQuantity(int rowIndex) {
		Row row = mRows.get(rowIndex);
		return(row.mQty);
	}

	//--------------------------------------------------------------------------
	/** Sets a quantity of troops for a command.
	 * @param rowIndex The nought based row number.
	 * @param command The 1 based command number (1-4).
	 * @param quantity The number of elements. */
	public void setRowCommandQuantity(int rowIndex, int command, int quantity) {
		Row row = mRows.get(rowIndex);
		row.mCmdQty[command-1] = quantity;
	}

	//--------------------------------------------------------------------------
	/** Sets a quantity of troops for a command.
	 * @param rowIndex The nought based row number.
	 * @param command The 1 based command number (1-4).
	 * @param quantity The number of elements. */
	public void setRowCommandQuantity(int rowIndex, int command, int quantity, ArmyListModelChange changes) {
		setRowCommandQuantity(rowIndex, command, quantity);
		recalcTotals();
		final ArmyListConstants bp;
		final ArmyListConstants cost;
		final ArmyListConstants els;
		final ArmyListConstants equiv;
		switch (command) {
		case 1:
			bp = ArmyListConstants.CMD1_BP;
			cost = ArmyListConstants.CMD1_COST;
			els = ArmyListConstants.CMD1_EL_COUNT;
			equiv = ArmyListConstants.CMD1_EQUIV;
			break;
		case 2:
			bp = ArmyListConstants.CMD2_BP;
			cost = ArmyListConstants.CMD2_COST;
			els = ArmyListConstants.CMD2_EL_COUNT;
			equiv = ArmyListConstants.CMD2_EQUIV;
			break;
		case 3:
			bp = ArmyListConstants.CMD3_BP;
			cost = ArmyListConstants.CMD3_COST;
			els = ArmyListConstants.CMD3_EL_COUNT;
			equiv = ArmyListConstants.CMD3_EQUIV;
			break;
		case 4:
			bp = ArmyListConstants.CMD4_BP;
			cost = ArmyListConstants.CMD4_COST;
			els = ArmyListConstants.CMD4_EL_COUNT;
			equiv = ArmyListConstants.CMD4_EQUIV;
			break;
			default : 
				log.warn("Unknown command {}", command);
				return;
		}
		changes.setField(bp, Float.toString(mCommandTotals[command-1].mBreakPoint));
		changes.setField(cost, Float.toString(mCommandTotals[command-1].mCost));
		changes.setField(els, Integer.toString(mCommandTotals[command-1].mElements));
		changes.setField(equiv, Float.toString(mCommandTotals[command-1].mEquivalents));
		changes.changed(true);
	}

	//--------------------------------------------------------------------------
	/** Gets a quantity of troops of a command.
	 * @param rowIndex The nought based row number.
	 * @param command The 1 based command number (1-4).
	 * @return The number of elements. */
	public int getRowCommandQuantity(int rowIndex, int command) {
		Row row = mRows.get(rowIndex);
		return(row.mCmdQty[command-1]);
	}

	//--------------------------------------------------------------------------
	/** Sets a troop description
	 * @param rowIndex 0 based row index, perhaps returned from addRow.
	 * @param description The description, e.g. Companions. */
	public void setRowDescription(int rowIndex, String description) {
		Row row = mRows.get(rowIndex);
		row.mDesc = description;
	}

	//--------------------------------------------------------------------------
	/** Gets a troop's description
	 * @param rowIndex The nought based row number.
	 * @return The description, e.g. Companions. */
	public String getRowDescription(int rowIndex) {
		Row row = mRows.get(rowIndex);
		return(row.mDesc);
	}

	//--------------------------------------------------------------------------
	private void refreshAdjustments(int rowIndex, ArmyListModelChange changes) {
		Row row = mRows.get(rowIndex);
		String drillName = row.mDrillName;
		String typeName = row.mTypeName;
		String gradeName = row.mGradeName;
		List<NameValuePair> adjustmentTexts = mCosts.getAdjustments(drillName, typeName, gradeName);
		adjustmentTexts.add(0, new NameValuePair("", ""));	// not compulsory
		String adj = row.mAdjustment;
		if (adj != null && !adj.isEmpty()) {
			boolean found = false;
			for (NameValuePair pair : adjustmentTexts) {
				if (pair.getName().equals(adj)) found = true;				
			}
			if (!found) adj = "";
		}
		changes.setRowFieldList(ArmyListConstants.ROW_ADJ, rowIndex, adjustmentTexts, adj);
		changes.changed(true);
	}

	//--------------------------------------------------------------------------
	/** Sets the troop's drill.
	 * @param rowIndex The nought based row number.
	 * @param drill The drill e.g. Irr, Reg, Fort. */
	public void setRowDrill(int rowIndex, String drill) {
		Row row = mRows.get(rowIndex);
		row.mDrillName = drill;
	}

	//--------------------------------------------------------------------------
	/** Sets the troop's drill.
	 * @param rowIndex The nought based row number.
	 * @param drill The drill e.g. Irr, Reg, Fort. */
	public void setRowDrill(int rowIndex, String drill, ArmyListModelChange changes) {
		setRowDrill(rowIndex,  drill);
		Row row = mRows.get(rowIndex);
		String typeName = row.mTypeName;
		List<String> types = mCosts.getTypes(drill);
		if (!types.contains(typeName)) {
			typeName = null;
		}
		changes.setRowFieldList(ArmyListConstants.ROW_TYPE, rowIndex, types, typeName);
		refreshAdjustments(rowIndex, changes);
		recalcTotals();
		changes.setRowField(ArmyListConstants.ROW_TROOP_COST, rowIndex, row.mCostPerElement);
		changes.setRowField(ArmyListConstants.ROW_LINE_COST, rowIndex, row.mTotalRowCost);
		changes.changed(true);
	}

	//--------------------------------------------------------------------------
	/** Gets the troop's drill.
	 * @param rowIndex The nought based row number.
	 * @return The drill e.g. Irr, Reg, Fort. */
	public String getRowDrill(int rowIndex) {
		Row row = mRows.get(rowIndex);
		return(row.mDrillName);
	}

	//--------------------------------------------------------------------------
	/** Sets the troop's type.
	 * @param rowIndex The nought based row number.
	 * @param type The type e.g. Kn, Cv, Pk, Bl */
	public void setRowType(int rowIndex, String type) {
		Row row = mRows.get(rowIndex);
		row.mTypeName = type;
	}

	//--------------------------------------------------------------------------
	/** Sets the troop's type.
	 * @param rowIndex The nought based row number.
	 * @param type The type e.g. Kn, Cv, Pk, Bl */
	public void setRowType(int rowIndex, String type, ArmyListModelChange changes) {
		setRowType(rowIndex, type);
		Row row = mRows.get(rowIndex);
		String drill = row.mDrillName;
		String grade = row.mGradeName;
		List<String> grades = mCosts.getTroopGradeList(drill, type);
		if (!grades.contains(grade)) {
			grade = null;
		}
		changes.setRowFieldList(ArmyListConstants.ROW_GRADE, rowIndex, grades, grade);
		refreshAdjustments(rowIndex, changes);
		recalcTotals();
		changes.setRowField(ArmyListConstants.ROW_TROOP_COST, rowIndex, row.mCostPerElement);
		changes.setRowField(ArmyListConstants.ROW_LINE_COST, rowIndex, row.mTotalRowCost);
		changes.changed(true);
	}

	//--------------------------------------------------------------------------
	/** Gets the troops's type.
	 * @param rowIndex The nought based row number.
	 * @return The type e.g. Kn, Cv, Pk, Bl */
	public String getRowType(int rowIndex) {
		Row row = mRows.get(rowIndex);
		return(row.mTypeName);
	}

	//--------------------------------------------------------------------------
	/** Sets the troops's grade.
	 * @param rowIndex The nought based row number.
	 * @param grade The grade e.g. S, O, I, F, X. */
	public void setRowGrade(int rowIndex, String grade) {
		Row row = mRows.get(rowIndex);
		row.mGradeName = grade;
	}

	//--------------------------------------------------------------------------
	/** Sets the troops's grade.
	 * @param rowIndex The nought based row number.
	 * @param grade The grade e.g. S, O, I, F, X. */
	public void setRowGrade(int rowIndex, String grade, ArmyListModelChange changes) {
		setRowGrade(rowIndex, grade);
		refreshAdjustments(rowIndex, changes);
		recalcTotals();
		Row row = mRows.get(rowIndex);
		changes.setRowField(ArmyListConstants.ROW_TROOP_COST, rowIndex, row.mCostPerElement);
		changes.setRowField(ArmyListConstants.ROW_LINE_COST, rowIndex, row.mTotalRowCost);
		changes.changed(true);
	}

	//--------------------------------------------------------------------------
	/** Gets the troops's grade.
	 * @param rowIndex The nought based row number.
	 * @return The grade e.g. S, O, I, F, X. */
	public String getRowGrade(int rowIndex) {
		Row row = mRows.get(rowIndex);
		return(row.mGradeName);
	}

	//--------------------------------------------------------------------------
	/** Sets the troop's adjustment.
	 * @param rowIndex The nought based row number.
	 * @param adjustment The adjustment e.g. "Ally general, Chariot". */
	public void setRowAdjustment(int rowIndex, String adjustment) {
		Row row = mRows.get(rowIndex);
		row.mAdjustment = adjustment;
	}

	//--------------------------------------------------------------------------
	/** Sets the troop's adjustment.
	 * @param rowIndex The nought based row number.
	 * @param adjustment The adjustment e.g. "Ally general, Chariot". */
	public void setRowAdjustment(int rowIndex, String adjustment, ArmyListModelChange changes) {
		setRowAdjustment(rowIndex, adjustment);
		recalcTotals();
		Row row = mRows.get(rowIndex);
		changes.setRowField(ArmyListConstants.ROW_TROOP_COST, rowIndex, row.mCostPerElement);
		changes.setRowField(ArmyListConstants.ROW_LINE_COST, rowIndex, row.mTotalRowCost);
		changes.changed(true);
	}

	//--------------------------------------------------------------------------
	/** Gets the troop's adjustment.
	 * @param rowIndex The nought based row number.
	 * @return The adjustment e.g. "ally, ch". */
	public String getRowAdjustment(int rowIndex) {
		Row row = mRows.get(rowIndex);
		return(row.mAdjustment);
	}

	//--------------------------------------------------------------------------
	/** Gets the troop's adjustment description.
	 * @param rowIndex The nought based row number.
	 * @return The adjustment e.g. "Ally general, Chariot". */
	public String getRowAdjustmentDescription(int rowIndex) {
		Row row = mRows.get(rowIndex);
		String drillName = row.mDrillName;
		String adjustmentMnemonic = row.mAdjustment;
		String adj = mCosts.getAdjustmentText(drillName, adjustmentMnemonic);
		return(adj);
	}

	//--------------------------------------------------------------------------
	/** Gets unused element quantity which is the quantity on the row minus those allocated to each command.
	 * @param rowIndex The nought based row number.
	 * @return The number of unused elements. */
	public int  getRowUnusedQuantity(int rowIndex) {
		Row row = mRows.get(rowIndex);
		return(row.mUnusedElements);
	}

	//--------------------------------------------------------------------------
	/** Returns a string of XML representing the army in stored format.
	 * @param pathName The path name to be used to save the file.
	 * @return XML.
	 * @throws XMLStreamException
	 * @throws IOException 
	 * @throws ParserConfigurationException
	 * @throws TransformerException */
	public String getAsXML() throws XMLStreamException, IOException {
		recalcTotals();
		XMLOutputFactory factory = XMLOutputFactory.newFactory();
		try (StringWriter sw = new StringWriter()) {
			XMLStreamWriter writer = factory.createXMLStreamWriter(sw);
			writer.writeStartDocument();
			writer.writeStartElement(AttributeNames.army.toString());
			writeXMLAttribute(writer, AttributeNames.book, mArmyBook);
			writeXMLAttribute(writer, AttributeNames.rules, mCosts.getRules());
			writeXMLAttribute(writer, AttributeNames.version, mCosts.getVersion());
			writeXMLAttribute(writer, AttributeNames.id, getArmyId());
			writeXMLAttribute(writer, AttributeNames.name, mArmyName);
			writeXMLAttribute(writer, AttributeNames.year, mArmyYear);

			writer.writeStartElement(AttributeNames.rows.toString());
			for (Row row : mRows) {
			    writer.writeStartElement(AttributeNames.row.toString());
			    writeXMLAttribute(writer, AttributeNames.quantity, row.mQty);
			    writeXMLAttribute(writer, AttributeNames.description, row.mDesc);
			    writeXMLAttribute(writer, AttributeNames.drill, row.mDrillName);
			    writeXMLAttribute(writer, AttributeNames.type, row.mTypeName);
			    writeXMLAttribute(writer, AttributeNames.grade, row.mGradeName);
			    writeXMLAttribute(writer, AttributeNames.adjustment, row.mAdjustment);
			    writeXMLAttribute(writer, AttributeNames.cmdQty0, row.mCmdQty[0]);
			    writeXMLAttribute(writer, AttributeNames.cmdQty1, row.mCmdQty[1]);
			    writeXMLAttribute(writer, AttributeNames.cmdQty2, row.mCmdQty[2]);
			    writeXMLAttribute(writer, AttributeNames.cmdQty3, row.mCmdQty[3]);
			    writer.writeEndElement();	// row
			}
			writer.writeEndElement();	// rows

			writer.writeEndElement();	// army
			writer.writeEndDocument();
			writer.close();
			String xml = sw.toString();
			return(xml);
		}
    }

	//--------------------------------------------------------------------------
	private static void writeXMLAttribute(XMLStreamWriter writer, AttributeNames name, String value) throws XMLStreamException {
		if (value != null && !value.isEmpty()) {
		    writer.writeAttribute(name.toString(), value);
		}
	}

	//--------------------------------------------------------------------------
	private static void writeXMLAttribute(XMLStreamWriter writer, AttributeNames name, int value) throws XMLStreamException {
		if (value != 0) {
		    writer.writeAttribute(name.toString(), Integer.toString(value));
		}
	}

	//--------------------------------------------------------------------------
	/** Loads an army list from the data provided.
	 * @param xml The XML string representing an army list.
	 * @return A YAML string of the army list. */
	void loadFromXML(String xml) {
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		dbf.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, ""); // Compliant
		dbf.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, ""); // compliant
		try {
			dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
			DocumentBuilder db = dbf.newDocumentBuilder();
			StringReader sr = new StringReader(xml);
			InputSource is = new InputSource(sr);
			Document doc = db.parse(is);
			NodeList armyNodes = doc.getElementsByTagName(AttributeNames.army.toString()); // should only be one
			int count = armyNodes.getLength();
			log.info("There are {} army nodes. There should be 1.", count);
			Element armyNode = (Element) armyNodes.item(0);
			mArmyId = armyNode.getAttribute("id");
			mArmyName = armyNode.getAttribute(AttributeNames.name.toString());
			mArmyBook = armyNode.getAttribute(AttributeNames.book.toString());
			mArmyYear = armyNode.getAttribute(AttributeNames.year.toString());

			mRows.clear();
			NodeList rowNodes = doc.getElementsByTagName(AttributeNames.row.toString());
			count = rowNodes.getLength();
			log.info("There are {} rows.", count);
			for (int rr = 0; rr < count; rr++) {
				Element rowNode = (Element) rowNodes.item(rr);
				Row row = new Row();
				row.mQty = getAttributeAsInt(rowNode, AttributeNames.quantity.toString());
				row.mDesc = rowNode.getAttribute(AttributeNames.description.toString());
				row.mDrillName = rowNode.getAttribute(AttributeNames.drill.toString());
				row.mTypeName = rowNode.getAttribute(AttributeNames.type.toString());
				row.mGradeName = rowNode.getAttribute(AttributeNames.grade.toString());
				row.mAdjustment = rowNode.getAttribute(AttributeNames.adjustment.toString());
				row.mCmdQty[0] = getAttributeAsInt(rowNode, AttributeNames.cmdQty0.toString());
				row.mCmdQty[1] = getAttributeAsInt(rowNode, AttributeNames.cmdQty1.toString());
				row.mCmdQty[2] = getAttributeAsInt(rowNode, AttributeNames.cmdQty2.toString());
				row.mCmdQty[3] = getAttributeAsInt(rowNode, AttributeNames.cmdQty3.toString());
				mRows.add(row);
				log.info("Added row {}.", row);
			}
		}
		catch (Exception e) {
			log.warn("Error loading army from XML.", e);
		}
	}

	//--------------------------------------------------------------------------
	private static String makeFileName(String dataDir, String armyId) {
		String path = dataDir + File.separator + "ald_army_" + armyId + ".xml";
		return(path);
	}

	//--------------------------------------------------------------------------
	public void loadFromFile(String dataDir, String armyId) throws IOException {
		String path = makeFileName(dataDir, armyId);
	    mLastFileUsed = Paths.get(path);
	    log.info("About to read {}", mLastFileUsed);
	    String content = new String(Files.readAllBytes(mLastFileUsed));
	    loadFromXML(content);
	}

	//--------------------------------------------------------------------------
	public void loadFromFile() throws IOException {
	    log.info("About to read {}", mLastFileUsed);
	    String content = new String(Files.readAllBytes(mLastFileUsed));
	    loadFromXML(content);
	}

	//--------------------------------------------------------------------------
	public void saveToFile(String dataDir) throws IOException, XMLStreamException {
		String path = makeFileName(dataDir, mArmyId);
	    Path pth = Paths.get(path);
	    String content = getAsXML();
	    Files.write(pth, content.getBytes());
	    mChanged = false;
	}

	//--------------------------------------------------------------------------
	public void saveToFile() throws IOException, XMLStreamException {
	    String content = getAsXML();
	    Files.write(mLastFileUsed, content.getBytes());
	    mChanged = false;
	}

	//--------------------------------------------------------------------------
	public boolean isYetToBeSaved() {
	    return(mLastFileUsed == null);
	}

	//--------------------------------------------------------------------------
	public void deleteArmy(String dataDir) throws IOException {
		String path = makeFileName(dataDir, mArmyId);
	    Path pth = Paths.get(path);
	    Files.deleteIfExists(pth);
	    log.info("Army list {} deleted", mArmyId);
	}

	//--------------------------------------------------------------------------
	public static void deleteArmy(String dataDir, String armyId) throws IOException {
		String path = makeFileName(dataDir, armyId);
	    Path pth = Paths.get(path);
	    Files.deleteIfExists(pth);
	    log.info("Army list {} deleted", armyId);
	}

	//--------------------------------------------------------------------------
	private static int getAttributeAsInt(Element el, String attrName) {
		String value = el.getAttribute(attrName);
		if (value == null || value.isEmpty()) {
			return (0);
		}
		int val = Integer.parseInt(value);
		return (val);
	}

	//--------------------------------------------------------------------------
	/** Returns a plain text version of the army list for basic printing or
	 * emailing or whatever else may be required. 
	 * @return The army list laid out as plain text. */
	public String getAsPlainText() {
		recalcTotals();
		StringBuilder sb = new StringBuilder();
		String str = MessageFormat.format("{0} - {1} ({2})",mArmyName,mArmyYear,mArmyBook);
		sb.append(str);
		sb.append("\r\n");
		int len = str.length();
		for (int ii=0; ii<len; ii++) {
			sb.append('-');
		}	// for
		sb.append("\r\n\r\n");
		for (int rowIndex=0; rowIndex < mRows.size(); rowIndex++) {
			Row row = mRows.get(rowIndex);
			double elCost = mCosts.getLineCost(row.mDrillName,row.mTypeName,row.mGradeName,row.mAdjustment, 1);
			double lineCost = mCosts.getLineCost(row.mDrillName,row.mTypeName,row.mGradeName,row.mAdjustment, row.mQty);
	        StringBuilder sb2 = new StringBuilder();
	        sb2.append(row.mDrillName);
	        sb2.append(' ');
	        sb2.append(row.mTypeName);
	        sb2.append('(');
	        sb2.append(row.mGradeName);
	        sb2.append(')');
	        String adj = getRowAdjustmentDescription(rowIndex);
	        if (!adj.isEmpty()) {
		        sb2.append(", ");
				sb2.append(adj);
	        }
			str = String.format("%3s    %-13s %-23s %5s %5s %3s   %3s   %3s   %3s%n",
					row.mQty, row.mDesc, sb2.toString(), fmt(elCost, 5, 1), fmt(lineCost, 5, 1), 
					fmt(getRowCommandQuantity(rowIndex,1), 3, 0), fmt(getRowCommandQuantity(rowIndex,2), 3, 0),
					fmt(getRowCommandQuantity(rowIndex,3), 3, 0), fmt(getRowCommandQuantity(rowIndex,4), 3, 0));
			sb.append(str);
		}	// for - each row

		sb.append("-----                                              ----- ----- ----- ----- -----\r\n");
		str = String.format("                                           Points: %5s %5s %5s %5s %5s%n",
				fmt(mArmyTotals.mCost, 5, 1),
				fmt(mCommandTotals[0].mCost, 5, 1), fmt(mCommandTotals[1].mCost, 5, 1),
				fmt(mCommandTotals[2].mCost, 5, 1), fmt(mCommandTotals[3].mCost, 5, 1));
		sb.append(str);

		str = String.format("%3s   (elements)                           Elements:     %3s   %3s   %3s   %3s%n",
				mArmyTotals.mElements,
				fmt(mCommandTotals[0].mElements, 3, 0), fmt(mCommandTotals[1].mElements, 3, 0),
				fmt(mCommandTotals[2].mElements, 3, 0), fmt(mCommandTotals[3].mElements, 3, 0));
		sb.append(str);

		str = String.format("%5s (equivalents)                        Equivalents:   %4s  %4s  %4s  %4s%n",
				fmt(mArmyTotals.mEquivalents, 5, 1),
				fmt(mCommandTotals[0].mEquivalents, 4, 1), fmt(mCommandTotals[1].mEquivalents, 4, 1),
				fmt(mCommandTotals[2].mEquivalents, 4, 1), fmt(mCommandTotals[3].mEquivalents, 4, 1));
		sb.append(str);

		str = String.format("%5s (half the army)                      Break points:  %4s  %4s  %4s  %4s%n",
				fmt(mArmyTotals.mBreakPoint, 5, 1),
				fmt(mCommandTotals[0].mBreakPoint, 4, 1), fmt(mCommandTotals[1].mBreakPoint, 4, 1),
				fmt(mCommandTotals[2].mBreakPoint, 4, 1), fmt(mCommandTotals[3].mBreakPoint, 4, 1));
		sb.append(str);
		str = sb.toString();
		return(str);
	}

	//--------------------------------------------------------------------------
	/** For printing a fixed width number. If there are decimal places and the 
	 * number is integral then no decimal point or following digits are printed 
	 * but spaces instead. If the number is nil then blank is printed.
	 * @param val A number.
	 * @param width The width of the result in characters.
	 * @return If the number is 0 then the width number of spaces, otherwise the number right justified. */
	private static String fmt(Number nbr, int width, int dps) {
		StringBuilder sb = new StringBuilder();
		for (int ii=0; ii<width; ii++) {
			sb.append('#');
		}
		if (dps > 0) {
			sb.setCharAt(width-dps-1, '.');
		}
		DecimalFormat df = new DecimalFormat(sb.toString());
		String txt = df.format(nbr);
		if (txt.equals("0")) {
			txt = "";
		}
		sb.setLength(0);
		sb.append(txt);

		if ((dps > 0) && (txt.indexOf('.') == -1)) {
			sb.append(' ');	// for the decimal point
			for (int ii=0; ii<dps; ii++) {
				sb.append(' ');	// for each decimal place
			}
		}

		int len = sb.length();
		for (int ii=0; ii<width-len; ii++) {
			sb.insert(0, ' ');
		}
		return(sb.toString());
	}

	//--------------------------------------------------------------------------
	private static float roundUpToNearestHalf(float val) {
		float ret = (float)(Math.ceil(val * 2) / 2);
		return(ret);
	}

	//--------------------------------------------------------------------------
	private void resetAllTotals() {
		mArmyTotals.mElements  =0;
		mArmyTotals.mEquivalents = 0f;
		mArmyTotals.mBreakPoint = 0f;
		mArmyTotals.mCost = 0f;
		for (int cc=0; cc< CMDS; cc++) {
			mCommandTotals[cc].mElements = 0;
			mCommandTotals[cc].mEquivalents = 0f;
			mCommandTotals[cc].mBreakPoint = 0f;
			mCommandTotals[cc].mCost = 0f;
		}
	}

	//--------------------------------------------------------------------------
	/** Recalculates these totals:
	 * <ol>
	 * <li>Army elements</li>
	 * <li>Army element equivalents</li>
	 * <li>Army break point (half the army equivalents)</li>
	 * <li>Army cost</li>
	 * <li>Command elements</li>
	 * <li>Command equivalents</li>
	 * <li>Command break point (third of the command equivalents)</li>
	 * <li>Command cost</li>
	 * </ol>
	 * */
	private void recalcTotals() {
		resetAllTotals();
		int rowCount = mRows.size();
		for (int rr=0; rr<rowCount; rr++) {
			Row row = mRows.get(rr);
			mArmyTotals.mElements += row.mQty;
			row.mCostPerElement = mCosts.getLineCost(row.mDrillName, row.mTypeName, row.mGradeName, row.mAdjustment, 1);
			row.mTotalRowCost = row.mCostPerElement * row.mQty;
			mArmyTotals.mCost += row.mTotalRowCost;
			float eq = mCosts.getTroopEquivalents(row.mDrillName,row.mTypeName,row.mGradeName);
			mArmyTotals.mEquivalents += (eq * row.mQty);
			row.mUnusedElements = row.mQty - row.mCmdQty[0] - row.mCmdQty[1] - row.mCmdQty[2] - row.mCmdQty[3];
			for (int cc=0; cc<4; cc++) {
				int cmdQty = row.mCmdQty[cc];
				if (cmdQty > 0) {
					mCommandTotals[cc].mElements += cmdQty;
					mCommandTotals[cc].mEquivalents += (cmdQty * eq);
					mCommandTotals[cc].mCost += (cmdQty * row.mCostPerElement);
				}
			}	// for - reset all command totals
		}	// for - each row
		mArmyTotals.mBreakPoint = roundUpToNearestHalf(mArmyTotals.mEquivalents / 2f);
		for (int cc=0; cc<4; cc++) {
			mCommandTotals[cc].mBreakPoint = roundUpToNearestHalf(mCommandTotals[cc].mEquivalents / 3f);
		}
	}

	//--------------------------------------------------------------------------
	public void clearArmyist() {
		setArmyBook(null);
		setArmyName(null);
		setArmyYear(null);
		mArmyId = null;
		mRows.clear();
		resetAllTotals();
	}

	//--------------------------------------------------------------------------
	/** Assumes the caller has none of the army or has discarded it and sends 
	 * all the changes for the whole army from scratch.
	 * @param changes To call the necessary change methods. */
	public void getWholeArmy(ArmyListModelChange changes) {
		changes.setField(ArmyListConstants.ARMY_NAME, mArmyName);
		changes.setField(ArmyListConstants.ARMY_BOOK, mArmyBook);
		changes.setField(ArmyListConstants.ARMY_YEAR, mArmyYear);
		int rowCount = mRows.size();
		for (int rr=0; rr<rowCount; rr++) {
			Row row = mRows.get(rr);
			changes.addRow();
			changes.setRowField(ArmyListConstants.ROW_QTY, rr, row.mQty);
			changes.setRowField(ArmyListConstants.ROW_DESC, rr, row.mDesc);
			changes.setRowFieldList(ArmyListConstants.ROW_DRILL, rr, mCosts.getDrillList(), row.mDrillName);
			if (row.mDrillName != null && !row.mDrillName.isEmpty()) {
				changes.setRowFieldList(ArmyListConstants.ROW_TYPE, rr, mCosts.getTypes(row.mDrillName), row.mTypeName);
				if (row.mTypeName != null && !row.mTypeName.isEmpty()) {
					changes.setRowFieldList(ArmyListConstants.ROW_GRADE, rr, mCosts.getTroopGradeList(row.mDrillName, row.mTypeName), row.mGradeName);
					if (row.mGradeName != null && !row.mGradeName.isEmpty()) {
						List<NameValuePair> adjs = mCosts.getAdjustments(row.mDrillName, row.mTypeName, row.mGradeName);
						adjs.add(0, new NameValuePair("", ""));
						changes.setRowFieldList(ArmyListConstants.ROW_ADJ, rr, adjs, row.mAdjustment);
					}
				}
			}
			changes.setRowField(ArmyListConstants.ROW_TROOP_COST, rr, row.mCostPerElement);
			changes.setRowField(ArmyListConstants.ROW_LINE_COST, rr, row.mTotalRowCost);
			changes.setRowField(ArmyListConstants.ROW_CMD1_QTY, rr, row.mCmdQty[0]);
			changes.setRowField(ArmyListConstants.ROW_CMD2_QTY, rr, row.mCmdQty[1]);
			changes.setRowField(ArmyListConstants.ROW_CMD3_QTY, rr, row.mCmdQty[2]);
			changes.setRowField(ArmyListConstants.ROW_CMD4_QTY, rr, row.mCmdQty[3]);
			changes.setRowField(ArmyListConstants.ROW_UNUSED, rr, row.mUnusedElements);
		}
		recalcTotals();
		updateAllLineCosts(changes);
		updateAllTotals(changes);
	}

	//--------------------------------------------------------------------------
	private void setChanged(ArmyListModelChange change) {
		mChanged = true;
		change.changed(true);
	}

	//--------------------------------------------------------------------------
	public boolean getChanged() {
		return(mChanged);
	}
}
