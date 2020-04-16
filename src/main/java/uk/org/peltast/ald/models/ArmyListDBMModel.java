package uk.org.peltast.ald.models;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.text.MessageFormat;
import java.util.ArrayList;

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

/** A DBM army list.
 * 
 * @author Mark Andrew Wheadon
 * @date 9th June 2012.
 * @copyright Mark Andrew Wheadon, 2012,2020.
 * @licence MIT License.
 */
public class ArmyListDBMModel {
	private static Logger log = LoggerFactory.getLogger(ArmyListDBMModel.class);
	private static final int CMDS = 4;
	public enum ColumnNames {QUANTITY, DESCRIPTION, DRILL, TYPE, GRADE, ADJUSTMENT, COST, TOTAL, CMD1_QTY, CMD2_QTY, CMD3_QTY, CMD4_QTY, UNUSED}
	private enum AttributeNames{army, book, costFileName, id, name, year, rows, row, quantity, description, drill, type, grade, adjustment, cmdQty0, cmdQty1, cmdQty2, cmdQty3}

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
	private int mArmyId	;	// used to save the file and in the directory
	private String mArmyName;
	private String mArmyBook;
	private String mArmyYear;
	private String mArmyCostsFileName;
	private ArmyListCosts mCosts;
	
	// Calculated values not to be saved.
	private int mArmyElements;
	private float mArmyElementEquivalents;
	private float mArmyCost;
	private int[] mCmdElements = new int[CMDS];
	private float[] mCmdElementEquivalents = new float[CMDS];
	private float[] mCmdBreakPoint = new float[CMDS];
	private float[] mCmdCost = new float[CMDS];

	//--------------------------------------------------------------------------
	public ArmyListDBMModel() {
	}

	//--------------------------------------------------------------------------
	public void setArmyCostsFile(ArmyListCosts costs) {
		mCosts = costs;
	}

	//--------------------------------------------------------------------------
	public void setArmyCostFileName(String armyCostFileName) {
		mArmyCostsFileName = armyCostFileName;
	}

	//--------------------------------------------------------------------------
	public String getArmyCostFileName() {
		return(mArmyCostsFileName);
	}

	//--------------------------------------------------------------------------
	public void deleteRow(int index) {
		mRows.remove(index);
		recalcTotals();
	}

	//--------------------------------------------------------------------------
	/** Adds a blank row.
	 * @return The index of the row just added. */
	public int addRow() {
		Row row = new Row();
		mRows.add(row);
		int sz = mRows.size();
		return(sz-1);
	}

	//--------------------------------------------------------------------------
	/** Inserts a blank row.
	 * @return The index of the row just added. */
	public int addRow(int index, boolean above) {
		Row row = new Row();
		int idx = index;
		if (!above) {
			idx++;
		}
		mRows.add(idx, row);
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
	public int getId() {
		return(mArmyId);
	}

	//--------------------------------------------------------------------------
	public void setId(int id) {
		mArmyId = id;
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
	public String getArmyBook() {
		return(mArmyBook);
	}

	//--------------------------------------------------------------------------
	public void setArmyBook(String book) {
		mArmyBook = book;
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
	public float getArmyTotalCost() {
		return(mArmyCost);
	}

	//--------------------------------------------------------------------------
	/** Sets a quantity for the row.
	 * @param rowIndex 0 based row index.
	 * @param quantity The number of elements. */
	public void setRowQuantity(int rowIndex, int quantity) {
		Row row = mRows.get(rowIndex);
		row.mQty = quantity;
		recalcTotals();
	}

	//--------------------------------------------------------------------------
	/** Gets a quantity for the row.
	 * @param row 0 based row index.
	 * @return The number of elements. */
	public int getRowQuantity(int rowIndex) {
		Row row = mRows.get(rowIndex);
		return(row.mQty);
	}

	//--------------------------------------------------------------------------
	/** Sets a quantity.
	 * @param row 0 based row index.
	 * @param command 1-4 = for specific command.
	 * @param quantity The number of elements. */
	public void setCommandQuantity(int rowIndex, int command, int quantity) {
		Row row = mRows.get(rowIndex);
		row.mCmdQty[command-1] = quantity;
		recalcTotals();
	}

	//--------------------------------------------------------------------------
	/** Gets a quantity.
	 * @param row 0 based row index.
	 * @param 1-4 = for specific command.
	 * @return The number of elements. */
	public int getCommandQuantity(int rowIndex, int command) {
		Row row = mRows.get(rowIndex);
		return(row.mCmdQty[command-1]);
	}

	//--------------------------------------------------------------------------
	/** Gets a quantity.
	 * @param row the row.
	 * @param 1-4 = for specific command.
	 * @return The number of elements. */
	public int getCommandQuantity(Row row, int command) {
		return(row.mCmdQty[command-1]);
	}

	//--------------------------------------------------------------------------
	/** Sets a troop description
	 * @param row 0 based row index, perhaps returned from addRow.
	 * @param description The description, e.g. Companions. */
	public void setDescription(int rowIndex, String description) {
		Row row = mRows.get(rowIndex);
		row.mDesc = description;
	}

	//--------------------------------------------------------------------------
	/** Gets a troop description
	 * @param row 0 based row index, perhaps returned from addRow.
	 * @return The description, e.g. Companions. */
	public String getDescription(int rowIndex) {
		Row row = mRows.get(rowIndex);
		return(row.mDesc);
	}

	//--------------------------------------------------------------------------
	/** Sets a drill.
	 * @param row 0 based row index, perhaps returned from addRow.
	 * @param drill The drill e.g. Irr, Reg, Fort. */
	public void setDrill(int rowIndex, String drill) {
		Row row = mRows.get(rowIndex);
		row.mDrillName = drill;
		recalcTotals();
	}

	//--------------------------------------------------------------------------
	/** Gets a drill.
	 * @param row 0 based row index, perhaps returned from addRow.
	 * @return The drill e.g. Irr, Reg, Fort. */
	public String getDrill(int rowIndex) {
		Row row = mRows.get(rowIndex);
		return(row.mDrillName);
	}

	//--------------------------------------------------------------------------
	/** Sets a type.
	 * @param row 0 based row index, perhaps returned from addRow.
	 * @param type The type e.g. Kn, Cv, Pk, Bl */
	public void setType(int rowIndex, String type) {
		Row row = mRows.get(rowIndex);
		row.mTypeName = type;
		recalcTotals();
	}

	//--------------------------------------------------------------------------
	/** Gets a type.
	 * @param row 0 based row index, perhaps returned from addRow.
	 * @return The type e.g. Kn, Cv, Pk, Bl */
	public String getType(int rowIndex) {
		Row row = mRows.get(rowIndex);
		return(row.mTypeName);
	}

	//--------------------------------------------------------------------------
	/** Sets a grade.
	 * @param row 0 based row index, perhaps returned from addRow.
	 * @param grade The grade e.g. S, O, I, F, X. */
	public void setGrade(int rowIndex, String grade) {
		Row row = mRows.get(rowIndex);
		row.mGradeName = grade;
		recalcTotals();
	}

	//--------------------------------------------------------------------------
	/** Gets a grade.
	 * @param row 0 based row index, perhaps returned from addRow.
	 * @return The grade e.g. S, O, I, F, X. */
	public String getGrade(int rowIndex) {
		Row row = mRows.get(rowIndex);
		return(row.mGradeName);
	}

	//--------------------------------------------------------------------------
	/** Sets an adjustment.
	 * @param row 0 based row index, perhaps returned from addRow.
	 * @param adjustment_csv The adjustment e.g. "Ally general, Chariot". */
	public void setAdjustment(int rowIndex, String adjustment) {
		Row row = mRows.get(rowIndex);
		row.mAdjustment = adjustment;
	}

	//--------------------------------------------------------------------------
	/** Gets an adjustment.
	 * @param row 0 based row index, perhaps returned from addRow.
	 * @return The adjustment e.g. "Ally general, Chariot". */
	public String getAdjustment(int rowIndex) {
		Row row = mRows.get(rowIndex);
		return(row.mAdjustment);
	}

	//--------------------------------------------------------------------------
	/** Sets unused element quantity which is the quantity on the row minus those allocated to each command.
	 * @param row 0 based row index, perhaps returned from addRow.
	 * @param unused The number of unused elements. */
	public void setUnusedQuantity(int rowIndex, int unused) {
		Row row = mRows.get(rowIndex);
		row.mUnusedElements = unused;
	}

	//--------------------------------------------------------------------------
	/** Gets unused element quantity which is the quantity on the row minus those allocated to each command.
	 * @param row 0 based row index, perhaps returned from addRow.
	 * @return The number of unused elements. */
	public int  getUnusedQuantity(int rowIndex) {
		Row row = mRows.get(rowIndex);
		return(row.mUnusedElements);
	}

	//--------------------------------------------------------------------------
	/** Returns a string of XML representing the army in stored format. 
	 * @return XML.	 
	 * @throws FileNotFoundException 
	 * @throws XMLStreamException 
	 * @throws ParserConfigurationException 
	 * @throws TransformerException */
	public void saveXML(String pathName) throws FileNotFoundException, XMLStreamException {
            XMLOutputFactory factory = XMLOutputFactory.newFactory();
            FileOutputStream fos = new FileOutputStream(pathName);
            XMLStreamWriter writer = factory.createXMLStreamWriter(fos);
            writer.writeStartDocument();
            writer.writeStartElement(AttributeNames.army.toString());
            writer.writeAttribute(AttributeNames.book.toString(), mArmyBook);
            writer.writeAttribute(AttributeNames.costFileName.toString(), mArmyCostsFileName);
            writer.writeAttribute(AttributeNames.id.toString(), Integer.toString(mArmyId));
            writer.writeAttribute(AttributeNames.name.toString(), mArmyName);
            writer.writeAttribute(AttributeNames.year.toString(), mArmyYear);
            
            writer.writeStartElement(AttributeNames.rows.toString());
            for (Row row : mRows) {
                writer.writeStartElement(AttributeNames.row.toString());
                writer.writeAttribute(AttributeNames.quantity.toString(), Integer.toString(row.mQty));
                writer.writeAttribute(AttributeNames.description.toString(), row.mDesc);
                writer.writeAttribute(AttributeNames.drill.toString(), row.mDrillName);
                writer.writeAttribute(AttributeNames.type.toString(), row.mTypeName);
                writer.writeAttribute(AttributeNames.grade.toString(), row.mGradeName);
                writer.writeAttribute(AttributeNames.adjustment.toString(), row.mAdjustment);
                writer.writeAttribute(AttributeNames.cmdQty0.toString(), Integer.toString(row.mCmdQty[0]));
                writer.writeAttribute(AttributeNames.cmdQty1.toString(), Integer.toString(row.mCmdQty[1]));
                writer.writeAttribute(AttributeNames.cmdQty2.toString(), Integer.toString(row.mCmdQty[2]));
                writer.writeAttribute(AttributeNames.cmdQty3.toString(), Integer.toString(row.mCmdQty[3]));
                writer.writeEndElement();	// row
            }
            writer.writeEndElement();	// rows
 
            writer.writeEndElement();	// army
            writer.writeEndDocument();
            writer.close();
    }

	//--------------------------------------------------------------------------
	/** Loads an army list from the data provided.
	 * @param data The date (YAML) for the army list
	 * @return A YAML string of the army list. */
	void loadFromXML(String xml) {
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		try {
			dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document doc = db.parse(xml);
			NodeList armyNodes = doc.getElementsByTagName(AttributeNames.army.toString()); // should only be one
			int count = armyNodes.getLength();
			log.info("There are {} army nodes. There should be 1.", count);
			Element armyNode = (Element) armyNodes.item(0);
			String ids = armyNode.getAttribute("id");
			mArmyId = Integer.parseInt(ids);
			mArmyName = armyNode.getAttribute(AttributeNames.name.toString());
			mArmyBook = armyNode.getAttribute(AttributeNames.book.toString());
			mArmyYear = armyNode.getAttribute(AttributeNames.year.toString());

			mRows.clear();
			NodeList rowNodes = doc.getElementsByTagName(AttributeNames.row.toString());
			count = rowNodes.getLength();
			log.info("There are {} rows.", count);
			for (int rr = 0; rr < count; rr++) {
				Element rowNode = (Element) armyNodes.item(rr);
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
				log.info("Added row {}.", row.toString());
			}
		}
		catch (Exception e) {
			log.warn("Error loading army from XML.", e);
		} 
	}

	//--------------------------------------------------------------------------
	private int getAttributeAsInt(Element el, String attr_name) {
		String value = el.getAttribute(attr_name);
		if (value == null || value.isEmpty()) {
			return (0);
		}
		int val = Integer.parseInt(value);
		return (val);
	}

	//--------------------------------------------------------------------------
	/** Returns a plain text version of the army list for basic printing or 
	 * emailing or whatever else may be required. */
	public String getAsPlainText() {
		StringBuilder sb = new StringBuilder();
		String str = MessageFormat.format("{0} - {1} ({2})",mArmyName,mArmyYear,mArmyBook);
		sb.append(str);
		sb.append("\r\n");
		int len = str.length();
		for (int ii=0; ii<len; ii++) {
			sb.append('-');
		}	// for
		sb.append("\r\n\r\n");
		for (Row row : mRows) {
			double el_cost = mCosts.getTroopCost(row.mDrillName,row.mTypeName,row.mGradeName);
			double line_cost = mCosts.getLineCost(row.mDrillName,row.mTypeName,row.mGradeName,row.mAdjustment, row.mQty);
	        String cmd1 = nbrOrSpaces(getCommandQuantity(row,1),3);
	        String cmd2 = nbrOrSpaces(getCommandQuantity(row,2),3);
	        String cmd3 = nbrOrSpaces(getCommandQuantity(row,3),3);
	        String cmd4 = nbrOrSpaces(getCommandQuantity(row,4),3);
	        StringBuilder sb2 = new StringBuilder();
	        sb2.append(row.mDrillName);
	        sb2.append(' ');
	        sb2.append(row.mTypeName);
	        sb2.append('(');
	        sb2.append(row.mGradeName);
	        sb2.append(')');
	        sb2.append(", ");
			sb2.append(row.mAdjustment);
			str = String.format("%3s    %-20s %-30s %5.1f %5.1f %3s   %3s   %3s   %3s\r\n",row.mQty,row.mDesc,sb2.toString(),el_cost,line_cost,cmd1,cmd2,cmd3,cmd4);
			sb.append(str);
		}	// for - each row

		int qty_total = getQuantityTotal();
		float cost_total = getCostTotal();
		int cmd1_el_total = getQuantityCommandTotal(1);
		int cmd2_el_total = getQuantityCommandTotal(2);
		int cmd3_el_total = getQuantityCommandTotal(3);
		int cmd4_el_total = getQuantityCommandTotal(4);
		sb.append("-----                                                            ----- ----- ----- ----- -----\r\n");
		str = String.format("%3d                                                              %5.1f %3d   %3d   %3d   %3d\r\n",qty_total,cost_total,cmd1_el_total,cmd2_el_total,cmd3_el_total,cmd4_el_total);
		sb.append(str);

		float army_equiv = getEquivalentTotal();
		float cmd1_ee = getEquivalentCommandTotal(1);
		float cmd2_ee = getEquivalentCommandTotal(2);
		float cmd3_ee = getEquivalentCommandTotal(3);
		float cmd4_ee = getEquivalentCommandTotal(4);
		str = String.format("%5.1f (equivalents)                                       Equivalents:  %4.1f  %4.1f  %4.1f  %4.1f\r\n",army_equiv,cmd1_ee,cmd2_ee,cmd3_ee,cmd4_ee);
		sb.append(str);

		float half_the_army = roundUpToNearestHalf(army_equiv / 2f);
		float cmd1_bp = roundUpToNearestHalf(cmd1_ee / 3f);
		float cmd2_bp = roundUpToNearestHalf(cmd2_ee / 3f);
		float cmd3_bp = roundUpToNearestHalf(cmd3_ee / 3f);
		float cmd4_bp = roundUpToNearestHalf(cmd4_ee / 3f);
		str = String.format("%5.1f (half the army)                                     Break points: %4.1f  %4.1f  %4.1f  %4.1f\r\n",half_the_army,cmd1_bp,cmd2_bp,cmd3_bp,cmd4_bp);
		sb.append(str);
		str = sb.toString();
		return(str);
	}

	//--------------------------------------------------------------------------
	/** For printing.
	 * @param val A number.
	 * @param width The width of the result in characters.
	 * @return If the number is 0 then the width number of spaces, otherwise the number right justified. */
	private String nbrOrSpaces(Number nbr, int width) {
		StringBuilder sb = new StringBuilder();
		int ww = 0;
		if (nbr.doubleValue() != 0) {
			sb.append(nbr);
		}
		for (int ii=ww; ii<width; ii++) {
			sb.insert(0, ' ');	// left pad
		}	// if
		return(sb.toString());
	}

	//--------------------------------------------------------------------------
	private float roundUpToNearestHalf(float val) {
		float ret = (float)(Math.ceil(val * 2) / 2);
		return(ret);
	}

	//--------------------------------------------------------------------------
	private int getQuantityTotal() {
		int qtyTotal = 0;
		for (Row row : mRows) {
			qtyTotal += row.mQty;
		}
		return(qtyTotal);
	}

	//--------------------------------------------------------------------------
	private int getQuantityCommandTotal(int cmd) {
		int qtyCmdTotal = 0;
		for (Row row : mRows) {
			float row_qty = getCommandQuantity(row,cmd);
			qtyCmdTotal += row_qty;
		}
		return(qtyCmdTotal);
	}

	//--------------------------------------------------------------------------
	private float getEquivalentTotal() {
		float equivTotal = 0;
		for (Row row : mRows) {
			float elEquiv = mCosts.getTroopEquivalents(row.mDrillName,row.mTypeName,row.mGradeName);
			equivTotal += row.mQty * elEquiv;
		}
		return(equivTotal);
	}

	//--------------------------------------------------------------------------
	private float getEquivalentCommandTotal(int cmd) {
		float equivTotal = 0;
		for (Row row : mRows) {
			float rowQty = row.mCmdQty[cmd];
			float eq = mCosts.getTroopEquivalents(row.mDrillName,row.mTypeName,row.mGradeName);
			equivTotal += (eq * rowQty);
		}
		return(equivTotal);
	}

	//--------------------------------------------------------------------------
	private float getCostTotal() {
		float costTotal = 0;
		for (Row row : mRows) {
			float lineCost = mCosts.getLineCost(row.mDrillName,row.mTypeName,row.mGradeName, row.mAdjustment, row.mQty);
			costTotal += lineCost;
		}	// for - each row
		return(costTotal);
	}

	//--------------------------------------------------------------------------
	/** Recalculates the 4 totals:
	 * <li>Army total quantity</li>
	 * <li>Army element equivalents</li>
	 * <li>Half the army</li>
	 * <li>Army total cost</li> */
	private void recalcTotals() {
		mTotals[0].mElements = 0;
		mTotals[0].mElementEquivalents = 0;
		mArmyTotalCost = 0;
		for (int ii=1; ii<=4; ii++) {
			mTotals[ii].mElements = 0;
			mTotals[ii].mElementEquivalents = 0;
			mTotals[ii].mBreakPoint = 0;
		}	// for - reset all command totals
		int row_count = mRows.size();
		for (int rr=0; rr<row_count; rr++) {
			Row row = mRows.get(rr);
			mTotals[0].mElements += row.mQty;
			float cost = mCosts.getElementCost(row.mDrillName,row.mTypeName,row.mGradeName);
			float adj = mCosts.getElementAdjustment(row.mDrillName,row.mTypeName,row.mGradeName,row.m_adj);
			cost += adj;
			cost *= row.mQty;
			mArmyTotalCost += cost;
			float eq = mCosts.getElementEquivalents(row.mDrillName,row.mTypeName,row.mGradeName);
			eq *= row.mQty;
			mTotals[0].mElementEquivalents += eq;
			row.mUnused = row.mQty = row.mCmdQty[0] - row.mCmdQty[1] - row.mCmdQty[2] - row.mCmdQty[3];
			for (int ii=0; ii<4; ii++) {
				mTotals[ii+1].mElements += row.mCmdQty[ii];
				mTotals[ii+1].mElementEquivalents += (row.mCmdQty[ii] * eq);
			}	// for - reset all command totals
		}	// for - each row
		mTotals[0].mBreakPoint = roundUpToNearestHalf(mTotals[0].mElementEquivalents / 2f);
		for (int ii=1; ii<=4; ii++) {
			mTotals[ii].mElementEquivalents = roundUpToNearestHalf(mTotals[ii].mElementEquivalents / 3f);
		}	// for - reset all command totals
	}
	
	//--------------------------------------------------------------------------
	public static void main(String argv[]) {
		// some tests
		ArmyListDBMModel aldm = new ArmyListDBMModel();
		aldm.setArmyName("Thracian");
		aldm.setArmyBook("1");
		aldm.setArmyYear("300BC");
		int index = aldm.addRow();
		aldm.setRowQuantity(index, 1);
		aldm.setCommandQuantity(index, 1, 1);
		aldm.setDescription(index, "General");
		aldm.setDrill(index, "Irr");
		aldm.setType(index, "Cv");
		aldm.setGrade(index, "O");
	}
}
