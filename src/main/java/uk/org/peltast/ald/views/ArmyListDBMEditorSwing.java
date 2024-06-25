/*------------------------------------------------------------------------------
08/07/2022 MAW addRow() removed setting spnrQty to 0 then 1 to fire update as the value when loaded from file always got set to 1.
11/08/2022 MAW setupArmyButtons() set the default row quantity to 1 when a new row is added.
12/08/2022 MAW Fix minor bug in above change. Now updates points on the index page.
17/01/2023 MAW Printing enhancements.
------------------------------------------------------------------------------*/

package uk.org.peltast.ald.views;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.print.Book;
import java.awt.print.PageFormat;
import java.awt.print.Paper;
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
import uk.org.peltast.ald.models.NameValuePair;
import uk.org.peltast.ald.swing.WTable;
import uk.org.peltast.ald.swing.WTable.WTableLocation;
import uk.org.peltast.ald.swing.WTable.WTableSection;
import uk.org.peltast.ald.views.ArmyListDBMPanel.Choice;

/** An editor for an individual army list. This editor is 'dumb' in that it only
 * ever displays values and accepts input. All changes are sent to the model, 
 * which then tells this class what the updates are. Thus methods like addRow 
 * and deleteRow just inform the model and it then calls back via a change to 
 * get the row added or deleted.
 * 
 * @author Mark Andrew Wheadon
 * @date 26th June 2012.
 * @copyright Mark Andrew Wheadon, 2012,2022.
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
	private boolean mSetupPhase = true;
	
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

	// army action buttons
	JButton mBtnSave= new JButton("Save");
	JButton mBtnReload = new JButton("Reload");

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
		mSetupPhase = false;
	}

	//--------------------------------------------------------------------------
	private JPanel setupArmyActionButtons() {
		JButton btnClose = new JButton("Close");
		JButton btnPrint = new JButton("Print ...");
		JButton btnExport = new JButton("Export to txt ...");
		JPanel pnl = new JPanel();
		pnl.add(mBtnSave);
		pnl.add(mBtnReload);
		pnl.add(btnClose);
		pnl.add(btnPrint);
		pnl.add(btnExport);
		btnClose.addActionListener(this::doButtonClose);
		mBtnSave.addActionListener(this::doButtonSave);
		mBtnReload.addActionListener(this::doButtonReload);
		btnPrint.addActionListener(this::doButtonPrint);
		btnExport.addActionListener(e -> doButtonExportToText());
		mBtnSave.setEnabled(false);
		mBtnReload.setEnabled(false);
		return(pnl);
	}

	//--------------------------------------------------------------------------
	private JPanel setupArmyHeaderFields() {
		JPanel pnl = new JPanel();
		pnl.add(new JLabel("Book"));
		mCbBooks.addActionListener(e -> {
			int index = mCbBooks.getSelectedIndex();
			if (index >= 0) {
				final String book = mCbBooks.getItemAt(index);
				if (mIndexChanges != null) {
					mIndexChanges.change(mModel.getArmyId(), ArmyListConstants.ARMY_BOOK, book);
					log.info("Index change: book is {}", book);
				}
				if (!mSetupPhase) {
					mModel.setArmyBook(book, mChanges);
					log.info("After setup phase: book is {}", book);
				}
			}
		});

		mArmyYearDoc = mTfYear.getDocument();
		mArmyYearDoc.addDocumentListener(new DocumentListener() {
			private void change() {
				if (mIndexChanges != null) {
					mIndexChanges.change(mModel.getArmyId(), ArmyListConstants.ARMY_YEAR, mTfYear.getText());
				}
				if (mSetupPhase) {
					mModel.setArmyYear(mTfYear.getText());
				} else {
					mModel.setArmyYear(mTfYear.getText(), mChanges);
				}
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
				if (mSetupPhase) {
					mModel.setArmyName(mTfDescription.getText());
				} else {
					mModel.setArmyName(mTfDescription.getText(), mChanges);
				}
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
		pnl.add(new JLabel("     Army"));
		pnl.add(mTfDescription);
		pnl.add(new JLabel("     Year"));
		pnl.add(mTfYear);
		pnl.add(new JLabel("     Cost version"));
		pnl.add(mTfCostFile);
		try {
			ArmyListCosts costs = mModel.getArmyCosts();
			String[] books = mModel.getArmyCosts().getBooks();
			log.info("About to remove all book items");
			mCbBooks.removeAllItems();
			for (String book : books) {
				mCbBooks.addItem(book);
			}
			log.info("Added all book items");
			mTfCostFile.setText(costs.getVersion());
		} catch (ParserConfigurationException | SAXException | IOException e1) {
			log.warn("Cannot get costs");
		}
		return(pnl);
	}

	//--------------------------------------------------------------------------
	private JPanel setupArmyButtons() {
		mBtnAdd.addActionListener(e -> {
			int rowIndex = mModel.addRow(mChanges);
			mModel.setRowQuantity(rowIndex, 1, mChanges);
		});
		mBtnDelete.addActionListener(this::doButtonDelete);
		mBtnMoveUp.addActionListener(this::doButtonMoveUp);
		mBtnMoveDown.addActionListener(this::doButtonMoveDown);
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
		log.info("Field is {} value is {}", field, value);
			switch (field) {
				case ARMY_BOOK :
					int itemCount = mCbBooks.getItemCount();
					for (int ii=0; ii<itemCount; ii++) {
						String bb = mCbBooks.getItemAt(ii);
						log.info("Book at index {} is {}", ii, bb);
					}
					mCbBooks.setSelectedItem(value);
					break;
				case ARMY_NAME : mTfDescription.setText(value); break;
				case ARMY_YEAR: mTfYear.setText(value); break;

				case ARMY_POINTS : {
					mTfArmyCosts.setText(value);
					if (mIndexChanges != null) {
						mIndexChanges.change(mModel.getArmyId(), ArmyListConstants.ARMY_POINTS, value);
					}
					break;
				}
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
			log.info("Field is {} String value is {}", field, value);
			switch (field) {
				case ROW_DESC:
					mTable.setValue(WTableSection.BODY, row, 2, value);
					break;
				case ROW_UNUSED:
					mTable.setValue(WTableSection.BODY, row, 13, value);
					break;
				default : log.warn("Unknown field {}", field);
			}
		}

		@Override
		public void setRowField(ArmyListConstants field, int row, int nbr) {
			log.info("Field is {} integer value is {}", field, nbr);
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
				cb.setSelectedIndex(selectedIndex);
				for (ActionListener listener : listeners) {
					cb.addActionListener(listener);
				}
			}
		}

		@Override
		public void setArmyLevelRow(int rowIndex, boolean armyLevel) {
			for (int ii=9; ii<13; ii++) {
				mTable.setFieldEnabled(WTableSection.BODY, rowIndex, ii, !armyLevel);	// all command allocations
			}
			mTable.setFieldVisible(WTableSection.BODY, rowIndex, 13, !armyLevel);	// and unused
		}

		@Override
		public void changed(boolean changed) {
			mBtnSave.setEnabled(changed);
			mBtnReload.setEnabled(changed);
		}
	}

	//--------------------------------------------------------------------------
	private class PagePrinter implements Printable {
		private static final String ARIAL = "Arial";
		private static final String COMMAND = "Command";
		private static final String FORTIFICATIONS = "Fortifications";
		private int mColumn1Left;
		private int mColumn1Right;
		private int mColumn2Left;
		private int mColumn2Right;
		private int mPrintableHeight;
		private int mPageHeight;

		public int print(Graphics graphics, PageFormat pgFmt, int pgNbr) throws PrinterException {
			log.info("About to print page {}.", pgNbr);

			Graphics2D g2d = (Graphics2D)graphics;
			Paper paper = pgFmt.getPaper();
			int width72th = (int)paper.getWidth();
			
			int margin = (int)pgFmt.getImageableX();
			mColumn1Left = margin +1;
			mColumn1Right = width72th/2 - margin/6;
			mColumn2Left = width72th/2 + margin/6;
			mColumn2Right = width72th - margin;
			mPrintableHeight = (int)pgFmt.getImageableHeight();
			mPageHeight = (int)paper.getHeight();

			g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
			Font fontHeading = new Font(ARIAL, Font.BOLD, 13);
			Font fontSubheading = new Font(ARIAL, Font.PLAIN, 8);

			g2d.setFont(fontHeading);
			FontMetrics metricsHeading = g2d.getFontMetrics(fontHeading);
			final int heightHeading = metricsHeading.getHeight();
			FontMetrics metricsPlain = g2d.getFontMetrics(fontSubheading);
			final int heightPlain = metricsPlain.getHeight();
			int yy = (int)pgFmt.getImageableY();
			final int yyTop = yy;
			final int yyBottom = yyTop + heightHeading * 2 + heightPlain;
			g2d.setColor(new Color(0x264A9C));
			g2d.fillRect(mColumn1Left, yyTop, mColumn2Right-mColumn1Left, yyBottom - yyTop);
			g2d.setColor(java.awt.Color.WHITE);
			final int leftForText = mColumn1Left + 6;
			
			yy += heightHeading;
			String str = mTfDescription.getText();
			String year = mTfYear.getText();
			if (year != null && !year.isEmpty()) {
				str += ", " + year;
			}
			int width = metricsHeading.stringWidth(str);
			int pos = mColumn1Left + (int)(pgFmt.getImageableWidth() /2) - width / 2;	// centred
			g2d.drawString(str, pos, yy);
			yy += 4;
			String totalCost = mTable.getValue(WTableSection.FOOTER,0,ColNo.QTY.ordinal());
			String totalQty = mTable.getValue(WTableSection.FOOTER,1,ColNo.QTY.ordinal());
			String totalElEq = mTable.getValue(WTableSection.FOOTER,2,ColNo.QTY.ordinal());
			String halfArmy = mTable.getValue(WTableSection.FOOTER,3,ColNo.QTY.ordinal());
			str = MessageFormat.format("{0} total cost, {1} elements, {2} equivalents, half the army is {3} elements",totalCost,totalQty,totalElEq,halfArmy);
			g2d.setFont(fontSubheading);
			yy += heightHeading;
			g2d.drawString(str, leftForText, yy);

			String book = (String)mCbBooks.getSelectedItem();
			width = metricsPlain.stringWidth(book);
			pos = mColumn2Right - width - 6;
			g2d.drawString(book, pos, yy);

			// print commands
			yy += heightHeading * 3;
			int[] yyColumns = {yy, yy};

			int column = 0;
			yyColumns[column] = printOneCommand(g2d, COMMAND, false, 1, column, yyColumns[column]);
			yyColumns[column] += heightHeading / 2;

			column = 1;
			yyColumns[column] = printOneCommand(g2d, COMMAND, false, 2, column, yyColumns[column]);
			yyColumns[column] += heightHeading / 2;

			column = yyColumns[0] <= yyColumns[1] ? 0 : 1;
			yyColumns[column] = printOneCommand(g2d, COMMAND, false, 3, column, yyColumns[column]);
			yyColumns[column] += heightHeading / 2;

			column = yyColumns[0] <= yyColumns[1] ? 0 : 1;
			yyColumns[column] = printOneCommand(g2d, COMMAND, false, 4, column, yyColumns[column]);
			yyColumns[column] += heightHeading / 2;
			
			column = yyColumns[0] <= yyColumns[1] ? 0 : 1;
			printOneCommand(g2d, FORTIFICATIONS, true, 0, column, yyColumns[column]);

			printTableTopGrid(g2d);
			log.info("Finished printing page {}.", pgNbr);
			return(PAGE_EXISTS);
		}	// print

		//--------------------------------------------------------------------------
		private void printTableTopGrid(Graphics2D g2d) {
			//	draw the table top grid, 3 boxes across and 2 boxes high
			final int c_size = (mColumn2Right - mColumn1Left) / 3; 	// of the box
			final int topMargin = mPageHeight / 2 + ((mPageHeight - mPrintableHeight) / 2) - 36;

			final int left_margin2 = mColumn1Left + (c_size);
			final int left_margin3 = mColumn1Left + (c_size * 2);
			final int right_margin = mColumn1Left + (c_size * 3);	// 3 boxes
			final int middle_margin = topMargin + c_size;
			final int bottom_margin = middle_margin + c_size;

			// draw the 3 horizontal lines
			g2d.drawLine(mColumn1Left,topMargin,right_margin,topMargin);
			g2d.drawLine(mColumn1Left,middle_margin,right_margin,middle_margin);
			g2d.drawLine(mColumn1Left,bottom_margin,right_margin,bottom_margin);

			// draw the 4 vertical lines
			g2d.drawLine(mColumn1Left,topMargin,mColumn1Left,bottom_margin);
			g2d.drawLine(left_margin2,topMargin,left_margin2,bottom_margin);
			g2d.drawLine(left_margin3,topMargin,left_margin3,bottom_margin);
			g2d.drawLine(right_margin,topMargin,right_margin,bottom_margin);
		}

		//----------------------------------------------------------------------
		private int printOneCommand(Graphics2D g2d, String title, boolean fortifications, int cmd, int column, int yy) {
			Font fontHeading = new Font(ARIAL, Font.BOLD, 11);
			Font fontTroops = new Font(ARIAL, Font.PLAIN, 10);
			FontMetrics metricsHeading = g2d.getFontMetrics(fontHeading);
			final int heightHeading = metricsHeading.getHeight();
			FontMetrics metricsPlain = g2d.getFontMetrics(fontTroops);
			final int heightPlain = metricsPlain.getHeight() * 11 / 10;	// 10% leading
			final int yyCmdTop = yy - heightHeading + 3;
			g2d.setColor(java.awt.Color.GRAY);
			g2d.setStroke(new BasicStroke(0.4f));

			int rowCount = mTable.getNumberOfRows(WTableSection.BODY);
			boolean cmdHeadingPrinted = false;
			final int colLeft = column == 0 ? mColumn1Left+1:mColumn2Left;
			final int colRight = column == 0 ? mColumn1Right:mColumn2Right;
			for (int rr=0; rr<rowCount; rr++) {
				String drill = mTable.getValue(WTableSection.BODY,rr,ColNo.DRILL.ordinal());
				String str;
				if (fortifications) {
					if (drill.equals("Fort")) {
						str = mTable.getValue(WTableSection.BODY,rr,ColNo.QTY.ordinal());
					}
					else {
						str = "";
					}
				}
				else {
					if (drill.equals("Fort")) {
						str = "";
					}
					else {
						str = mTable.getValue(WTableSection.BODY,rr,ColNo.CMD1.ordinal()-1+cmd);
					}
				}
				int cmdQty = str.length()>0 ? Integer.parseInt(str) : 0;
				if (cmdQty == 0) {
					continue;
				}	// if - no elements for this row in this command so skip
				String desc = mTable.getValue(WTableSection.BODY,rr,ColNo.DESC.ordinal());
				String type = mTable.getValue(WTableSection.BODY,rr,ColNo.TYPE.ordinal());
				String grade = mTable.getValue(WTableSection.BODY,rr,ColNo.GRADE.ordinal());
				String adj = mTable.getValue(WTableSection.BODY,rr,ColNo.ADJ.ordinal());
				if (!cmdHeadingPrinted) {
					g2d.setFont(fontHeading);
					g2d.drawRect(colLeft, yyCmdTop, colRight-colLeft, heightHeading);
					g2d.setColor(new Color(0xffd700));	// gold
					g2d.fillRect(colLeft, yyCmdTop, colRight-colLeft, heightHeading);
					g2d.setColor(java.awt.Color.DARK_GRAY);
					str = fortifications ? title : title + " " + cmd;
					g2d.drawString(str, colLeft+2, yy);
					if (!fortifications) {
						final String points = mTable.getValue(WTableSection.FOOTER,0,ColNo.CMD1.ordinal()-1+cmd);
						str = MessageFormat.format("{0} points", points);
						final int length = metricsPlain.stringWidth(str);
						g2d.setFont(fontTroops);
						g2d.drawString(str, colRight - length - 6, yy);
					}
					yy += heightHeading + heightPlain / 3;
					cmdHeadingPrinted = true;
				}	// if
				if (fortifications) {
					str = MessageFormat.format("{0} {1}, {2} {3} {4}",cmdQty,desc,drill,type,adj);
				}
				else {
					str = MessageFormat.format("{0} {1}, {2} {3}({4}) {5}",cmdQty,desc,drill,type,grade,adj);
				}
				g2d.setFont(fontTroops);
				g2d.drawString(str,colLeft+2,yy);
				yy += heightPlain;
			}	// for - each row
			if (cmdHeadingPrinted) {
				if (!fortifications) {
					String totalEl = mTable.getValue(WTableSection.FOOTER,1,ColNo.CMD1.ordinal()-1+cmd);
					String eq = mTable.getValue(WTableSection.FOOTER,2,ColNo.CMD1.ordinal()-1+cmd);
					String breakPoint = mTable.getValue(WTableSection.FOOTER,3,ColNo.CMD1.ordinal()-1+cmd);
					String str = MessageFormat.format("{0} elements, {1} equivalents, {2} break", totalEl, eq, breakPoint);
					yy += heightPlain / 2;
					g2d.drawString(str,colLeft+2,yy);
				}

				// print surrounding box
				g2d.setColor(java.awt.Color.GRAY);
				g2d.drawRect(colLeft, yyCmdTop, colRight-colLeft, yy-yyCmdTop+4);

				yy += heightPlain * 2d;
			}
			return(yy);
		}
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
		enableDeleteAndMoveButtons();
	}

	//--------------------------------------------------------------------------
	private void doButtonClose(ActionEvent ae) {
		Choice choice = Choice.YES;
		if (mBtnSave.isEnabled()) {
			choice = ArmyListDBMPanel.confirmMessage(mPnlMain, "The list has unsaved changes, do you want to exit and loose the changes?");
		}
		if (choice == Choice.YES) {
			String armyId = mModel.getArmyId();
			mIndexChanges.change(armyId, ArmyListConstants.CLOSE, null);
		}
	}

	//--------------------------------------------------------------------------
	private void doButtonSave(ActionEvent ae) {
		try {
			mModel.save(mChanges);
		} catch (IOException | XMLStreamException e) {
			ArmyListDBMPanel.errorMessage(mPnlMain, "Saving army list failed because of : " + e.toString());
		}
	}

	//--------------------------------------------------------------------------
	private void doButtonReload(ActionEvent ae) {
		try {
			Choice choice = Choice.YES;
			if (mBtnReload.isEnabled()) {
				choice = ArmyListDBMPanel.confirmMessage(mPnlMain, "Are you sure you want to reload and loose your changes?");
			}
			if (choice == Choice.YES) {
				mModel.reloadFromFile(mChanges);
			}
		} catch (IOException e) {
			ArmyListDBMPanel.errorMessage(mPnlMain, "Reloading army list failed because of : " + e.toString());
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
			ArmyListDBMPanel.errorMessage(mPnlMain, "Export to text failed");
		}
		log.info("Army {} exported to text file {}", mModel.getArmyName(), ff.getPath());
	}

	//--------------------------------------------------------------------------
	private void doButtonMoveDown(ActionEvent ae) {
		int rowCount = mTable.getNumberOfRows(WTableSection.BODY);
		// always best to move down list entries backwards
		for (int rr=rowCount-1; rr>=0; rr--) {
			String tick = mTable.getValue(WTableSection.BODY, rr, 0);
			if (tick.equals("Y")) {
				mModel.moveRowDown(rr, mChanges);
			}
		}
		enableDeleteAndMoveButtons();
	}

	//--------------------------------------------------------------------------
	private void doButtonMoveUp(ActionEvent ae) {
		int rowCount = mTable.getNumberOfRows(WTableSection.BODY);
		for (int rr=0; rr<rowCount; rr++) {
			String tick = mTable.getValue(WTableSection.BODY, rr, 0);
			if (tick.equals("Y")) {
				mModel.moveRowUp(rr, mChanges);
			}
		}
		enableDeleteAndMoveButtons();
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
    		Component comp = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
    		log.info("Component is {}", comp);
    		
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
		final JSpinner spnrQty = new JSpinner(snmQty);
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
				mModel.setRowDescription(loc.getRow(), tfDesc.getText(), mChanges);
			}
			@Override
			public void changedUpdate(DocumentEvent e) { change(); }
			@Override
			public void insertUpdate(DocumentEvent e) { change(); }
			@Override
			public void removeUpdate(DocumentEvent e) { change(); }
		});

		JComboBox<String> cbDrill = new JComboBox<>(mDrills.toArray(new String[0]));
		cbDrill.setSelectedIndex(-1);	// unselecr
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
		//cbAdj.setRenderer(new NameValuePairRenderer());
		cbAdj.addActionListener(e -> {
			WTableLocation loc = mTable.getLocation(cbAdj);
			NameValuePair pair  = (NameValuePair)cbAdj.getSelectedItem();
			mModel.setRowAdjustment(loc.getRow(), pair.getName(), mChanges);
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
	void setIndexChanges(ArmyIndexModelChange indexChanges) {
		mIndexChanges = indexChanges;
	}

	//--------------------------------------------------------------------------
	ArmyListDBMModel getModel() {
		return(mModel);
	}
}
