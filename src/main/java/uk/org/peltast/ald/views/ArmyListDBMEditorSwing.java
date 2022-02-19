package uk.org.peltast.ald.views;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.print.Book;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.security.AccessControlException;
import java.text.MessageFormat;
import java.util.List;

import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.RepaintManager;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.plaf.basic.BasicComboBoxRenderer;
import javax.swing.text.Document;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLStreamException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import uk.org.peltast.ald.models.ArmyIndexModelChange;
import uk.org.peltast.ald.models.ArmyListConstants;
import uk.org.peltast.ald.models.ArmyListCosts;
import uk.org.peltast.ald.models.ArmyListDBMModel;
import uk.org.peltast.ald.models.ArmyListModelChange;
import uk.org.peltast.ald.models.ArmyListModelUtils;
import uk.org.peltast.ald.models.NameValuePair;
import uk.org.peltast.ald.swing.WTable;
import uk.org.peltast.ald.swing.WTable.WTableLocation;
import uk.org.peltast.ald.swing.WTable.WTableSection;

/** An editor for an individual army list. This editor is 'dumb' in that it only
 * ever displays values and accepts input. All changes are sent to the model, 
 * which then tells this class what the updates are. Thus methods like addRow 
 * and deleteRow just inform the model and it then calls back via a change to 
 * get the row added or deleted.
 * 
 * @author Mark Andrew Wheadon
 * @date 26th June 2012.
 * @copyright Mark Andrew Wheadon, 2012,2021.
 * @licence MIT License.
 */
public class ArmyListDBMEditorSwing {
	private static final Logger log = LoggerFactory.getLogger(ArmyListDBMEditorSwing.class);
	private enum ColNo {OPT,QTY,DESC,DRILL,TYPE,GRADE,ADJ,COST,TOTAL,CMD1,CMD2,CMD3,CMD4,UNUSED};

	private final ArmyListDBMModel mModel;
	private final List<String> mDrills;
	private JPanel mPnlMain = new JPanel(new BorderLayout());	// buttons go in north, inner panel goes in centre.
	private Document mArmyDescriptionDoc;
	private Document mArmyYearDoc;
	private WTable mTable = new WTable(14);
	private final Changes mChanges = new Changes();
	private ArmyIndexModelChange mIndexChanges;
	
	// Army list header fields
	private JComboBox<String> mCbBooks = new JComboBox<>();
	private JTextField mTfYear = new JTextField(10);
	private JTextField mTfDescription = new JTextField(30);
	private JTextField mTfCostFile = new JTextField();

	// army footer fields
	private JTextField mTfArmyCosts = new JTextField("");
	private JTextField mTfArmyElementCount = new JTextField("");
	private JTextField mTfArmyEquiv = new JTextField("");
	private JTextField mTfArmyHalf = new JTextField("");

	private JTextField mTfCmd1Cost = new JTextField("");
	private JTextField mTfCmd2Cost = new JTextField("");
	private JTextField mTfCmd3Cost = new JTextField("");
	private JTextField mTfCmd4Cost = new JTextField("");
	private JTextField mTfCmd1ElCount = new JTextField("");
	private JTextField mTfCmd2ElCount = new JTextField("");
	private JTextField mTfCmd3ElCount = new JTextField("");
	private JTextField mTfCmd4ElCount = new JTextField("");
	private JTextField mTfCmd1Eq = new JTextField("");
	private JTextField mTfCmd2Eq = new JTextField("");
	private JTextField mTfCmd3Eq = new JTextField("");
	private JTextField mTfCmd4Eq = new JTextField("");
	private JTextField mTfCmd1Bp = new JTextField("");
	private JTextField mTfCmd2Bp = new JTextField("");
	private JTextField mTfCmd3Bp = new JTextField("");
	private JTextField mTfCmd4Bp = new JTextField("");
	
	// army list action buttons.
	JButton mBtnAdd = new JButton("Add");
	JButton mBtnDelete = new JButton("Delete");
	JButton mBtnMoveUp = new JButton("Move up");
	JButton mBtnMoveDown = new JButton("Move down");

	//--------------------------------------------------------------------------
	ArmyListDBMEditorSwing(ArmyListDBMModel model) throws ParserConfigurationException, SAXException, IOException {
		mModel = model;
		ArmyListCosts costs = mModel.getArmyCosts();
		mDrills = costs.getDrillList();
		setupGui();
	}

	//--------------------------------------------------------------------------
	private void setupGui() {
		// Central panel that contains the actual army list the headings and row manipulation buttons.
		JPanel pnlCentral = new JPanel(new BorderLayout());
		pnlCentral.add(setupArmyHeaderFields(),BorderLayout.NORTH);
		pnlCentral.add(setupArmyTable(),BorderLayout.CENTER);
		pnlCentral.add(setupArmyButtons(),BorderLayout.SOUTH);

		// set up the main panel, with the above in the centre
		mPnlMain.add(setupArmyActionButtons(),BorderLayout.NORTH);
		mPnlMain.add(pnlCentral,BorderLayout.CENTER);
		String armyId = mModel.getArmyId();
		mPnlMain.setName(armyId);
		
		mModel.getWholeArmy(mChanges);
	}

	//--------------------------------------------------------------------------
	private JPanel setupArmyActionButtons() {
		JButton btnClose = new JButton("Close");
		JButton btnSave= new JButton("Save");
		JButton btnReload = new JButton("Reload");
		JButton btnPrint = new JButton("Print ...");
		JButton btnExport = new JButton("Export to txt ...");
		JPanel pnl = new JPanel();
		pnl.add(btnSave);
		pnl.add(btnReload);
		pnl.add(btnClose);
		pnl.add(btnPrint);
		pnl.add(btnExport);
		btnClose.addActionListener(this::doButtonClose);
		btnSave.addActionListener(this::doButtonSave);
		btnReload.addActionListener(this::doButtonReload);
		btnPrint.addActionListener(this::doButtonPrint);
		btnExport.addActionListener(e -> doButtonExportToText());
		return(pnl);
	}

	//--------------------------------------------------------------------------
	private JPanel setupArmyHeaderFields() {
		JPanel pnl = new JPanel();
		pnl.add(new JLabel("Book"));
		mCbBooks.addActionListener(e -> {
			if (mIndexChanges != null) {
				mIndexChanges.change(mModel.getArmyId(), ArmyListConstants.ARMY_BOOK, mCbBooks.getSelectedItem().toString());
			}
			mModel.setArmyBook(mCbBooks.getSelectedItem().toString());
		});

		mArmyYearDoc = mTfYear.getDocument();
		mArmyYearDoc.addDocumentListener(new DocumentListener() {
			private void change() {
				if (mIndexChanges != null) {
					mIndexChanges.change(mModel.getArmyId(), ArmyListConstants.ARMY_YEAR, mTfYear.getText());
				}
				mModel.setArmyYear(mTfYear.getText());
			}
			@Override
			public void changedUpdate(DocumentEvent e) { change(); }
			@Override
			public void insertUpdate(DocumentEvent e) { change(); }
			@Override
			public void removeUpdate(DocumentEvent e) { change(); }
		});

		mArmyDescriptionDoc = mTfDescription.getDocument();
		mArmyDescriptionDoc.addDocumentListener(new DocumentListener() {
			private void change() {
				if (mIndexChanges != null) {
					mIndexChanges.change(mModel.getArmyId(), ArmyListConstants.ARMY_NAME, mTfDescription.getText());
				}
				mModel.setArmyName(mTfDescription.getText());
			}
			@Override
			public void changedUpdate(DocumentEvent e) { change(); }
			@Override
			public void insertUpdate(DocumentEvent e) { change(); }
			@Override
			public void removeUpdate(DocumentEvent e) { change(); }
		});
		mTfCostFile.setEditable(false);
		pnl.add(mCbBooks);
		pnl.add(new JLabel("        Army"));
		pnl.add(mTfDescription);
		pnl.add(new JLabel("        Year"));
		pnl.add(mTfYear);
		pnl.add(new JLabel("        Cost version"));
		pnl.add(mTfCostFile);
		try {
			ArmyListCosts costs = mModel.getArmyCosts();
			String[] books = mModel.getArmyCosts().getBooks();
			mCbBooks.removeAllItems();
			for (String book : books) {
				mCbBooks.addItem(book);
			}
			mTfCostFile.setText(costs.getVersion());
		} catch (ParserConfigurationException | SAXException | IOException e1) {
			log.warn("Cannot get costs");
		}
		return(pnl);
	}

	//--------------------------------------------------------------------------
	private JPanel setupArmyButtons() {
		mBtnAdd.addActionListener(this::doButtonAdd);
		mBtnDelete.addActionListener(this::doButtonDelete);
		mBtnMoveUp.addActionListener(this::doButtonMoveUp);
		mBtnMoveDown.addActionListener(e -> {
			int rowCount = mTable.getNumberOfRows(WTableSection.BODY);
			// move rows down the bottom up so checked rows don't trip over each other.
			for (int row_nbr=rowCount-1; row_nbr>=0; row_nbr--) {
				String chk = mTable.getValue(WTableSection.BODY,row_nbr,0);
				if (chk.length() > 0 && row_nbr < rowCount-1) {
					mTable.moveRowDown(row_nbr);
					mModel.moveRowDown(row_nbr);
				}
			}	// for - each row
			enableDeleteAndMoveButtons();	// there won't be any checked rows now
		});
		JPanel pnl = new JPanel();
		pnl.add(mBtnAdd);
		pnl.add(mBtnDelete);
		pnl.add(mBtnMoveUp);
		pnl.add(mBtnMoveDown);
		enableDeleteAndMoveButtons();
		return(pnl);
	}

	//--------------------------------------------------------------------------
	private JPanel setupArmyTable() {
		String[] headings = new String[] {"?","Qty","Description","Drill","Type","Grade","Adjustment","Cost","Total","Cmd 1","Cmd 2","Cmd 3","Cmd 4","Unused"};
		mTable.addRow(WTableSection.HEADER,headings);

		JLabel lbl = new JLabel("");

		mTfArmyCosts.setEditable(false);
		mTfCmd1Cost.setEditable(false);
		mTfCmd2Cost.setEditable(false);
		mTfCmd3Cost.setEditable(false);
		mTfCmd4Cost.setEditable(false);
		JComponent[] arr1 = new JComponent[] {lbl,mTfArmyCosts,new JLabel("(cost)"),lbl,lbl,lbl,lbl,lbl,new JLabel("Cost:"),mTfCmd1Cost,mTfCmd2Cost,mTfCmd3Cost,mTfCmd4Cost,lbl};
		mTable.addRow(WTableSection.FOOTER,arr1);

		mTfArmyElementCount.setEditable(false);
		mTfCmd1ElCount.setEditable(false);
		mTfCmd2ElCount.setEditable(false);
		mTfCmd3ElCount.setEditable(false);
		mTfCmd4ElCount.setEditable(false);
		JComponent[] arr2 = new JComponent[] {lbl,mTfArmyElementCount,new JLabel("(elements)"),lbl,lbl,lbl,lbl,lbl,new JLabel("Elements:"),mTfCmd1ElCount,mTfCmd2ElCount,mTfCmd3ElCount,mTfCmd4ElCount,lbl};
		mTable.addRow(WTableSection.FOOTER,arr2);

		mTfArmyEquiv.setEditable(false);
		mTfCmd1Eq.setEditable(false);
		mTfCmd2Eq.setEditable(false);
		mTfCmd3Eq.setEditable(false);
		mTfCmd4Eq.setEditable(false);
		JComponent[] arr3 = new JComponent[] {lbl,mTfArmyEquiv,new JLabel("(equivalents)"),lbl,lbl,lbl,lbl,lbl,new JLabel("Equivalents:"),mTfCmd1Eq,mTfCmd2Eq,mTfCmd3Eq,mTfCmd4Eq,lbl};
		mTable.addRow(WTableSection.FOOTER,arr3);

		mTfArmyHalf.setEditable(false);
		mTfCmd1Bp.setEditable(false);
		mTfCmd2Bp.setEditable(false);
		mTfCmd3Bp.setEditable(false);
		mTfCmd4Bp.setEditable(false);
		JComponent[] arr4 = new JComponent[] {lbl,mTfArmyHalf,new JLabel("(half the army)"),lbl,lbl,lbl,lbl,lbl,new JLabel("Break point:"),mTfCmd1Bp,mTfCmd2Bp,mTfCmd3Bp,mTfCmd4Bp, lbl};
		mTable.addRow(WTableSection.FOOTER,arr4);

		JPanel pnl = new JPanel(new BorderLayout());
		JScrollPane spTbl = new JScrollPane(mTable.getPanel());
		pnl.add(spTbl, BorderLayout.CENTER);
		return(pnl);
	}

	//--------------------------------------------------------------------------
	private void enableDeleteAndMoveButtons() {
		int checkedCount = 0;
		int rowCount = mTable.getNumberOfRows(WTableSection.BODY);
		for (int row_nbr=0; row_nbr<rowCount; row_nbr++) {
			String chk = mTable.getValue(WTableSection.BODY,row_nbr,0);
			if (chk.length() > 0) {
				checkedCount++;
			}
		}	// for - each row
		mBtnDelete.setEnabled(checkedCount>0);
		if (checkedCount == 0) {
			mBtnMoveDown.setEnabled(false);
			mBtnMoveUp.setEnabled(false);
		}
		else {
			String chk = mTable.getValue(WTableSection.BODY,0,0);	// top row
			mBtnMoveUp.setEnabled(chk.length() <= 0);
			chk = mTable.getValue(WTableSection.BODY,rowCount-1,0);	// bottom row
			mBtnMoveDown.setEnabled(chk.length() <= 0);
		}
	}

	//--------------------------------------------------------------------------
	private class Changes implements ArmyListModelChange {

		@Override
		public void addRow() {
			ArmyListDBMEditorSwing.this.addRow();
		}

		@Override
		public void deleteRow(int rowIndex) {
			mTable.deleteRow(rowIndex);
		}

		@Override
		public void moveRowDown(int rowIndex) {
			mTable.moveRowDown(rowIndex);
		}

		@Override
		public void moveRowUp(int rowIndex) {
			mTable.moveRowUp(rowIndex);
		}

		@Override
		public void clear() {
			mTable.removeAllRows();
		}

		@Override
		public void setField(ArmyListConstants field, String value) {
			switch (field) {
				case ARMY_BOOK : mCbBooks.setSelectedItem(value); break;
				case ARMY_NAME : mTfDescription.setText(value); break;
				case ARMY_YEAR: mTfYear.setText(value); break;

				case ARMY_POINTS : mTfArmyCosts.setText(value); break;
				case ARMY_EL_COUNT : mTfArmyElementCount.setText(value); break;
				case ARMY_EL_EQUIV : mTfArmyEquiv.setText(value); break;
				case ARMY_HALF : mTfArmyHalf.setText(value); break;

				case CMD1_COST : mTfCmd1Cost.setText(value); break;
				case CMD1_EL_COUNT : mTfCmd1ElCount.setText(value); break;
				case CMD1_EQUIV : mTfCmd1Eq.setText(value); break;
				case CMD1_BP : mTfCmd1Bp.setText(value); break;

				case CMD2_COST : mTfCmd2Cost.setText(value); break;
				case CMD2_EL_COUNT : mTfCmd2ElCount.setText(value); break;
				case CMD2_EQUIV : mTfCmd2Eq.setText(value); break;
				case CMD2_BP : mTfCmd2Bp.setText(value); break;

				case CMD3_COST : mTfCmd3Cost.setText(value); break;
				case CMD3_EL_COUNT : mTfCmd3ElCount.setText(value); break;
				case CMD3_EQUIV : mTfCmd3Eq.setText(value); break;
				case CMD3_BP : mTfCmd3Bp.setText(value); break;

				case CMD4_COST : mTfCmd4Cost.setText(value); break;
				case CMD4_EL_COUNT : mTfCmd4ElCount.setText(value); break;
				case CMD4_EQUIV : mTfCmd4Eq.setText(value); break;
				case CMD4_BP : mTfCmd4Bp.setText(value); break;

				default : log.warn("Unknown field {}", field);
			}
		}

		@Override
		public void setRowField(ArmyListConstants field, int row, String value) {
			switch (field) {
				case ROW_DESC:
					mTable.setValue(WTableSection.BODY, row, 2, value);
					break;
				default : log.warn("Unknown field {}", field);
			}
		}

		@Override
		public void setRowField(ArmyListConstants field, int row, int nbr) {
			switch (field) {
				case ROW_QTY:
					mTable.setValue(WTableSection.BODY, row, 1, nbr);
					break;
				case ROW_CMD1_QTY:
					mTable.setValue(WTableSection.BODY, row, 9, nbr);
					break;
				case ROW_CMD2_QTY:
					mTable.setValue(WTableSection.BODY, row, 10, nbr);
					break;
				case ROW_CMD3_QTY:
					mTable.setValue(WTableSection.BODY, row, 11, nbr);
					break;
				case ROW_CMD4_QTY:
					mTable.setValue(WTableSection.BODY, row, 12, nbr);
					break;
				case ROW_UNUSED:
					mTable.setValue(WTableSection.BODY, row, 13, nbr);
					break;
				default : log.warn("Unknown field {}", field);
			}
		}

		@Override
		public void setRowField(ArmyListConstants field, int row, float nbr) {
			switch (field) {
				case ROW_TROOP_COST:
					mTable.setValue(WTableSection.BODY, row, 7, nbr);
					break;
				case ROW_LINE_COST:
					mTable.setValue(WTableSection.BODY, row, 8, nbr);
					break;
				default : log.warn("Unknown field {}", field);
			}
		}

		@Override
		public <E> void setRowFieldList(ArmyListConstants field, int row, List<E> values, String selectedValue) {
			JComponent comp = null;
			switch (field) {
				case ROW_DRILL:
					comp = mTable.getComponent(WTableSection.BODY, row, 3);
					setCombo(comp, values, selectedValue);
					break;
				case ROW_TYPE:
					comp = mTable.getComponent(WTableSection.BODY, row, 4);
					setCombo(comp, values, selectedValue);
					break;
				case ROW_GRADE:
					comp = mTable.getComponent(WTableSection.BODY, row, 5);
					setCombo(comp, values, selectedValue);
					break;
				case ROW_ADJ:
					comp = mTable.getComponent(WTableSection.BODY, row, 6);
					setCombo(comp, values, selectedValue);
					break;
				default : log.warn("Unknown field {}", field);
			}
		}

		private <E> void setCombo(JComponent comp, List<E> values, String selectedValue) {
			if (comp instanceof JComboBox) {
				JComboBox<E> cb = (JComboBox<E>)comp;
				ActionListener[] listeners = cb.getActionListeners();
				for (ActionListener listener : listeners) {
					cb.removeActionListener(listener);
				}
				DefaultComboBoxModel<E> model = (DefaultComboBoxModel<E>) cb.getModel();
				model.removeAllElements();
				int selectedIndex = -1;
				int index = 0;
				for (E value : values) {
					model.addElement(value);
					if (value instanceof NameValuePair) {
						NameValuePair pair = (NameValuePair)value;
						if (pair.getName().equals(selectedValue)) {
							selectedIndex = index;
						}
					} else {
						if (value.toString().equals(selectedValue)) {
							selectedIndex = index;
						}
					}
					index++;
				}
				if (selectedIndex != -1) {
					cb.setSelectedIndex(selectedIndex);
				}
				for (ActionListener listener : listeners) {
					cb.addActionListener(listener);
				}
			}
		}
	}

	//--------------------------------------------------------------------------
	private class PagePrinter implements Printable {
		private static final String ARIAL = "Arial";
		public int print(Graphics graphics, PageFormat pgFmt, int pgNbr) throws PrinterException {
			final int c_left_margin_1 = 75;
			final int c_left_margin_2 = 310;
			log.info("About to print page {}.", pgNbr);

			Graphics2D g2d = (Graphics2D)graphics;
			g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
			Font fontHeading = new Font(ARIAL, Font.BOLD, 11);
			Font fontPlain = new Font(ARIAL, Font.PLAIN, 8);

			g2d.setFont(fontHeading);
			String str = mTfDescription.getText();
			g2d.drawString(str,c_left_margin_1,100);
			str = mTfYear.getText();
			g2d.drawString(str,c_left_margin_1+200,100);
			str = (String)mCbBooks.getSelectedItem();
			g2d.drawString(str,c_left_margin_1+275,100);
			g2d.drawLine(0,105,2000,105);
			String totalCost = mTable.getValue(WTableSection.FOOTER,0,ColNo.TOTAL.ordinal());
			String totalQty = mTable.getValue(WTableSection.FOOTER,0,ColNo.QTY.ordinal());
			String totalElEq = mTable.getValue(WTableSection.FOOTER,1,ColNo.QTY.ordinal());
			String halfArmy = mTable.getValue(WTableSection.FOOTER,2,ColNo.QTY.ordinal());
			str = MessageFormat.format("{0} total cost, {1} total elements, {2} equivalents, half the army is {3} elements",totalCost,totalQty,totalElEq,halfArmy);
			g2d.setFont(fontPlain);
			g2d.drawString(str,c_left_margin_1,120);
			int yy = 160;
			int yy1 = printOneCommand(g2d,1,yy,c_left_margin_1);
			int yy2 = printOneCommand(g2d,2,yy,c_left_margin_2);
			yy = Math.max(yy1,yy2);
			yy1 = printOneCommand(g2d,3,yy,c_left_margin_1);
			yy2 = printOneCommand(g2d,4,yy,c_left_margin_2);
			yy = Math.max(yy1,yy2);
			printTableTopGrid(g2d,yy,c_left_margin_1);
			log.info("Finished printing page {}.", pgNbr);
			return(PAGE_EXISTS);
		}	// print

		//--------------------------------------------------------------------------
		private void printTableTopGrid(Graphics2D g2d, int topMargin, int leftMargin) {
			//	draw the table top grid, 3 boxes across and 2 boxes high
			final int c_size = 130; 	// of the box

			final int left_margin2 = leftMargin + (c_size);
			final int left_margin3 = leftMargin + (c_size * 2);
			final int right_margin = leftMargin + (c_size * 3);	// 3 boxes
			final int middle_margin = topMargin + c_size;
			final int bottom_margin = middle_margin + c_size;

			// draw the 3 horizontal lines
			g2d.drawLine(leftMargin,topMargin,right_margin,topMargin);
			g2d.drawLine(leftMargin,middle_margin,right_margin,middle_margin);
			g2d.drawLine(leftMargin,bottom_margin,right_margin,bottom_margin);

			// draw the 4 vertical lines
			g2d.drawLine(leftMargin,topMargin,leftMargin,bottom_margin);
			g2d.drawLine(left_margin2,topMargin,left_margin2,bottom_margin);
			g2d.drawLine(left_margin3,topMargin,left_margin3,bottom_margin);
			g2d.drawLine(right_margin,topMargin,right_margin,bottom_margin);
		}

		//----------------------------------------------------------------------
		private int printOneCommand(Graphics2D g2d, int cmd, int yy, int leftMargin) {
			final int c_line_height = 16;
			Font fontHeading = new Font(ARIAL, Font.BOLD, 10);
			Font fontPlain = new Font(ARIAL, Font.PLAIN, 8);
			int rowCount = mTable.getNumberOfRows(WTableSection.BODY);
			boolean cmdHeadingPrinted = false;
			for (int rr=0; rr<rowCount; rr++) {
				String str = mTable.getValue(WTableSection.BODY,rr,ColNo.CMD1.ordinal()-1+cmd);
				int cmdQty = str.length()>0 ? Integer.parseInt(str) : 0;
				if (cmdQty == 0) {
					continue;
				}	// if - no elements for this row in this command so skip
				String desc = mTable.getValue(WTableSection.BODY,rr,ColNo.DESC.ordinal());
				String drill = mTable.getValue(WTableSection.BODY,rr,ColNo.DRILL.ordinal());
				String type = mTable.getValue(WTableSection.BODY,rr,ColNo.TYPE.ordinal());
				String grade = mTable.getValue(WTableSection.BODY,rr,ColNo.GRADE.ordinal());
				String adj = mTable.getValue(WTableSection.BODY,rr,ColNo.ADJ.ordinal());
				if (!cmdHeadingPrinted) {
					g2d.setFont(fontHeading);
					g2d.drawString("Command "+cmd,leftMargin,yy);
					yy += c_line_height;
					cmdHeadingPrinted = true;
				}	// if
				str = MessageFormat.format("{0} {1}, {2} {3}({4}) {5}",cmdQty,desc,drill,type,grade,adj);
				g2d.setFont(fontPlain);
				g2d.drawString(str,leftMargin,yy);
				yy += c_line_height;
			}	// for - each row
			if (cmdHeadingPrinted) {
				String totalEl = mTable.getValue(WTableSection.FOOTER,0,ColNo.CMD1.ordinal()-1+cmd);
				String eq = mTable.getValue(WTableSection.FOOTER,1,ColNo.CMD1.ordinal()-1+cmd);
				String breakPoint = mTable.getValue(WTableSection.FOOTER,2,ColNo.CMD1.ordinal()-1+cmd);
				String str = MessageFormat.format("{0} total elements,  break point is {1}",totalEl,breakPoint);
				yy += c_line_height / 2;
				g2d.drawString(str,leftMargin,yy);
			}
			yy += c_line_height * 2d;
			return(yy);
		}
	}

	//--------------------------------------------------------------------------
	private void doButtonAdd(ActionEvent ae) {
		mModel.addRow(mChanges);
	}

	//--------------------------------------------------------------------------
	/** Tells the model which rows have been deleted.
	 * @param ae */
	private void doButtonDelete(ActionEvent ae) {
		int rowCount = mTable.getNumberOfRows(WTableSection.BODY);
		// always best to delete list entries backwards
		for (int rr=rowCount-1; rr>=0; rr--) {
			String tick = mTable.getValue(WTableSection.BODY, rr, 0);
			if (tick.equals("Y")) {
				mModel.deleteRow(rr, mChanges);
			}
		}
	}

	//--------------------------------------------------------------------------
	private void doButtonClose(ActionEvent ae) {
	}

	//--------------------------------------------------------------------------
	private void doButtonSave(ActionEvent ae) {
		if (mModel.isYetToBeSaved()) {
			String dataDir = ArmyListModelUtils.getDataPath();
			try {
				mModel.saveToFile(dataDir);
			} catch (IOException | XMLStreamException e) {
				errorMessage("Saving army list failed because of : " + e.toString());
			}
		} else {
			try {
				mModel.saveToFile();
			} catch (IOException | XMLStreamException e) {
				errorMessage("Saving army list failed because of : " + e.toString());
			}
		}
	}

	//--------------------------------------------------------------------------
	private void doButtonReload(ActionEvent ae) {
		if (mModel.isYetToBeSaved()) {
			errorMessage("Cannot reload as army list has yet to be saved");
		}
		try {
			mModel.loadFromFile();
		} catch (IOException e) {
			errorMessage("Reloading army list failed because of : " + e.toString());
		}
	}

	//--------------------------------------------------------------------------
	private void doButtonExportToText() {
		JFileChooser chooser = new JFileChooser();
		FileNameExtensionFilter filter = new FileNameExtensionFilter("Exported DBM army list", "txt");
		chooser.setFileFilter(filter);
		int ret = chooser.showSaveDialog(mPnlMain);
		if (ret != JFileChooser.APPROVE_OPTION) {
			return;
		}
		File ff = chooser.getSelectedFile();
		String txt = mModel.getAsPlainText();
		try (FileWriter fr = new FileWriter(ff.getPath()); BufferedWriter br = new BufferedWriter(fr)) {
			br.write(txt);
		}
		catch (IOException ioe) {
			errorMessage("Export to text failed");
		}
		log.info("Army {} exported to text file {}", mModel.getArmyName(), ff.getPath());
	}

	//--------------------------------------------------------------------------
	private void doButtonMoveDown(ActionEvent ae) {
	}

	//--------------------------------------------------------------------------
	private void doButtonMoveUp(ActionEvent ae) {
	}

	//--------------------------------------------------------------------------
	private void doButtonPrint(ActionEvent ae) {
		try {
			PrintRequestAttributeSet prtRqsAttSet = new HashPrintRequestAttributeSet();
			PrinterJob pj = null;
			try {
				pj = PrinterJob.getPrinterJob();
			}
			catch (AccessControlException ace) {
				log.warn("Error trying to print:", ace);
				JOptionPane.showMessageDialog(mPnlMain, "Sorry, you do not appear to have authority to print.","Printing Error",JOptionPane.WARNING_MESSAGE);
				return;
			}
			Book bk = new Book();
			int nbrOfPages = 1;
			PagePrinter pp = new PagePrinter();
			PageFormat pageFmt = pj.defaultPage();
			bk.append(pp,pageFmt,nbrOfPages);
			pj.setPageable(bk);
			boolean ok = pj.printDialog(prtRqsAttSet);
			if (ok) {
				try {
					log.info("About to print");
					RepaintManager currentManager = RepaintManager.currentManager(mPnlMain);
					boolean dblBuf = currentManager.isDoubleBufferingEnabled();
					if (dblBuf) {
						currentManager.setDoubleBufferingEnabled(false);
					}
					pj.print(prtRqsAttSet);
					if (dblBuf) {
						currentManager.setDoubleBufferingEnabled(true);
					}
				}	// try
				catch (PrinterException pe) {
					log.warn("Printer error.", pe);
				}
			}	// if - user said okay to print dialog
		}
		catch (Exception e) {
			log.warn("Error trying to print:", e);
			JOptionPane.showMessageDialog(mPnlMain, e.toString(), "Printing Error", JOptionPane.WARNING_MESSAGE);
		}
	}

	//--------------------------------------------------------------------------
    class NameValuePairRenderer extends BasicComboBoxRenderer {
    	@Override
        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

            if (value != null) {
            	NameValuePair pair = (NameValuePair)value;
                setText(pair.getValue());
            }
            return this;
        }
    }

	//--------------------------------------------------------------------------
	private void addRow() {
		JCheckBox chkBox = new JCheckBox();
		chkBox.addActionListener(e -> enableDeleteAndMoveButtons());
		
		SpinnerNumberModel snmQty = new SpinnerNumberModel(1,1,200,1);
		JSpinner spnrQty = new JSpinner(snmQty);
		spnrQty.setValue(1);
		spnrQty.setName(ArmyListConstants.ROW_QTY.toString());
		spnrQty.addChangeListener(e -> {
			WTableLocation loc = mTable.getLocation(spnrQty);
			mModel.setRowQuantity(loc.getRow(), (Integer)spnrQty.getValue(), mChanges);
		});

		JTextField tfDesc = new JTextField();
		tfDesc.setName(ArmyListConstants.ROW_DESC.toString());
		tfDesc.getDocument().addDocumentListener(new DocumentListener() {
			private void change() {
				WTableLocation loc = mTable.getLocation(spnrQty);
				mModel.setRowDescription(loc.getRow(), tfDesc.getText());
			}
			@Override
			public void changedUpdate(DocumentEvent e) { change(); }
			@Override
			public void insertUpdate(DocumentEvent e) { change(); }
			@Override
			public void removeUpdate(DocumentEvent e) { change(); }
		});

		JComboBox<String> cbDrill = new JComboBox<>(mDrills.toArray(new String[0]));
		cbDrill.setName(ArmyListConstants.ROW_DRILL.toString());
		cbDrill.addActionListener(e -> {
			WTableLocation loc = mTable.getLocation(cbDrill);
			mModel.setRowDrill(loc.getRow(), cbDrill.getSelectedItem().toString(), mChanges);
		});

		JComboBox<String> cbType = new JComboBox<>();
		cbType.setName(ArmyListConstants.ROW_TYPE.toString());
		cbType.addActionListener(e -> {
			WTableLocation loc = mTable.getLocation(cbType);
			mModel.setRowType(loc.getRow(), cbType.getSelectedItem().toString(), mChanges);
		});

		JComboBox<String> cbGrade = new JComboBox<>();
		cbGrade.setName(ArmyListConstants.ROW_GRADE.toString());
		cbGrade.addActionListener(e -> {
			WTableLocation loc = mTable.getLocation(cbGrade);
			mModel.setRowGrade(loc.getRow(), cbGrade.getSelectedItem().toString(), mChanges);
		});

		JComboBox<NameValuePair> cbAdj = new JComboBox<>();
		cbAdj.setName(ArmyListConstants.ROW_ADJ.toString());
		cbAdj.setRenderer(new NameValuePairRenderer());
		cbAdj.addActionListener(e -> {
			WTableLocation loc = mTable.getLocation(cbAdj);
			mModel.setRowAdjustment(loc.getRow(), cbAdj.getSelectedItem().toString(), mChanges);
		});

		JTextField tfCostEach = new JTextField(4);
		tfCostEach.setEditable(false);

		JTextField tfCostTotal = new JTextField(4);
		tfCostTotal.setEditable(false);

		SpinnerNumberModel snmCmd1 = new SpinnerNumberModel(0,0,100,1);
		JSpinner spnrCmd1 = new JSpinner(snmCmd1);
		spnrCmd1.setName(ArmyListConstants.ROW_CMD1_QTY.toString());
		spnrCmd1.addChangeListener(e -> {
			WTableLocation loc = mTable.getLocation(spnrCmd1);
			mModel.setRowCommandQuantity(loc.getRow(), 1, (Integer)spnrCmd1.getValue(), mChanges);
		});
		SpinnerNumberModel snmCmd2 = new SpinnerNumberModel(0,0,100,1);
		JSpinner spnrCmd2 = new JSpinner(snmCmd2);
		spnrCmd2.setName(ArmyListConstants.ROW_CMD2_QTY.toString());
		spnrCmd2.addChangeListener(e -> {
			WTableLocation loc = mTable.getLocation(spnrCmd2);
			mModel.setRowCommandQuantity(loc.getRow(), 2, (Integer)spnrCmd2.getValue(), mChanges);
		});
		SpinnerNumberModel snmCmd3 = new SpinnerNumberModel(0,0,100,1);
		JSpinner spnrCmd3 = new JSpinner(snmCmd3);
		spnrCmd3.setName(ArmyListConstants.ROW_CMD3_QTY.toString());
		spnrCmd3.addChangeListener(e -> {
			WTableLocation loc = mTable.getLocation(spnrCmd3);
			mModel.setRowCommandQuantity(loc.getRow(), 3, (Integer)spnrCmd3.getValue(), mChanges);
		});
		SpinnerNumberModel snmCmd4 = new SpinnerNumberModel(0,0,100,1);
		JSpinner spnrCmd4 = new JSpinner(snmCmd4);
		spnrCmd4.setName(ArmyListConstants.ROW_CMD4_QTY.toString());
		spnrCmd4.addChangeListener(e -> {
			WTableLocation loc = mTable.getLocation(spnrCmd4);
			mModel.setRowCommandQuantity(loc.getRow(), 4, (Integer)spnrCmd4.getValue(), mChanges);
		});

		JTextField tfElementsUnused = new JTextField(4);
		tfElementsUnused.setEditable(false);
		
		JComponent[] arr = new JComponent[] {
				chkBox,
				spnrQty,
				tfDesc,
				cbDrill,
				cbType,
				cbGrade,
				cbAdj,
				tfCostEach,
				tfCostTotal,
				spnrCmd1, spnrCmd2, spnrCmd3, spnrCmd4,
				tfElementsUnused};
		mTable.addRow(WTableSection.BODY, arr);

		JComponent editor = spnrQty.getEditor();
		JFormattedTextField field = (JFormattedTextField) editor.getComponent(0);
		field.grabFocus();	// TODO: Doesn't seem to work!
	}

	//--------------------------------------------------------------------------
	JPanel getJPanel() {
		return(mPnlMain);
	}

	//--------------------------------------------------------------------------
	private class ButtonMoveUpListener implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent ae) {
			int row_count = mTable.getNumberOfRows(WTableSection.BODY);
			for (int row_nbr=0; row_nbr<row_count; row_nbr++) {
				String chk = mTable.getValue(WTableSection.BODY,row_nbr,0);
				if (chk.length() > 0) {
					if (row_nbr > 0) {
						mTable.moveRowUp(row_nbr);
						mModel.moveRowUp(row_nbr);
					}	// if
				}	// if
			}	// for - each row
			enableDeleteAndMoveButtons();	// there won't be any checked rows now
		}	// actionPerformed
	}	// ButtonMoveUpListener

	//--------------------------------------------------------------------------
	private class ButtonMoveDownListener implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent ae) {
		}	// actionPerformed
	}

	//--------------------------------------------------------------------------
	private boolean confirmMessage(String msg) {
		int result = JOptionPane.showConfirmDialog(mPnlMain, msg);
		if (result == JOptionPane.YES_OPTION) {
			return(true);
		}	// if
		return(false);
	}	// confirm_message

	//--------------------------------------------------------------------------
	private void errorMessage(String msg) {
		JOptionPane.showMessageDialog(mPnlMain, msg);
	}

	//--------------------------------------------------------------------------
	ArmyListDBMModel getModel() {
		return(mModel);
	}

	//--------------------------------------------------------------------------
	void setIndexChanges(ArmyIndexModelChange indexChanges) {
		mIndexChanges = indexChanges;
	}
}
