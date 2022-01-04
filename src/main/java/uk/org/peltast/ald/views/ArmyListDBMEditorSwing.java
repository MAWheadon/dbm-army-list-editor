package uk.org.peltast.ald.views;

import java.awt.BorderLayout;
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
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
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
import javax.swing.text.Document;
import javax.xml.parsers.ParserConfigurationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import uk.org.peltast.ald.models.ArmyIndexModelChange;
import uk.org.peltast.ald.models.ArmyListConstants;
import uk.org.peltast.ald.models.ArmyListCosts;
import uk.org.peltast.ald.models.ArmyListDBMModel;
import uk.org.peltast.ald.models.ArmyListModelChange;
import uk.org.peltast.ald.swing.WTable;
import uk.org.peltast.ald.swing.WTable.WTableSection;

/** An editor for an individual army list. This editor is 'dumb' in that it only
 * ever displays values and accepts input. All changes are sent to the model, 
 * which then tells this class what the updates are.
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
	private boolean mChanged = false;
	private WTable mTable = new WTable(14);
	private final Changes mChanges = new Changes();
	private ArmyIndexModelChange mIndexChanges;
	
	// Army list header fields
	JComboBox<String> mCbBooks = new JComboBox<>();
	JTextField mTfYear = new JTextField(10);
	JTextField mTfDescription = new JTextField(30);
	JTextField mTfCostFile = new JTextField();
	
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
		JButton btnPrint = new JButton("Print ...");
		JButton btnExport = new JButton("Export to txt ...");
		JPanel pnl = new JPanel();
		pnl.add(btnClose);
		pnl.add(btnPrint);
		pnl.add(btnExport);
		btnClose.addActionListener(this::doButtonClose);
		btnPrint.addActionListener(this::doButtonPrint);
		btnExport.addActionListener(e -> doButtonExportToText());
		return(pnl);
	}

	//--------------------------------------------------------------------------
	private JPanel setupArmyHeaderFields() {
		JPanel pnl = new JPanel();
		pnl.add(new JLabel("Book"));
		mCbBooks.addActionListener(e -> {
			mChanged = true;
			if (mIndexChanges != null) {
				mIndexChanges.change(mModel.getArmyId(), ArmyListConstants.BOOK, mCbBooks.getSelectedItem().toString());
			}
		});
		mArmyYearDoc = mTfYear.getDocument();
		
		mArmyYearDoc.addDocumentListener(new DocumentListenerAnychange());
		mArmyDescriptionDoc = mTfDescription.getDocument();
		mArmyDescriptionDoc.addDocumentListener(new DocumentListenerAnychange());
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
	private class DocumentListenerAnychange implements DocumentListener {

		@Override
		public void changedUpdate(DocumentEvent e) {
			mChanged = true;
		}

		@Override
		public void insertUpdate(DocumentEvent e) {
			mChanged = true;
		}

		@Override
		public void removeUpdate(DocumentEvent e) {
			mChanged = true;
		}
	}

	//--------------------------------------------------------------------------
	private JPanel setupArmyButtons() {
		mBtnAdd.addActionListener(this::doButtonAdd);
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
		JScrollPane mSpTbl = new JScrollPane(mTable.getPanel());
		String[] headings = new String[] {"?","Qty","Description","Drill","Type","Grade","Adjustment","Cost","Total","Cmd 1","Cmd 2","Cmd 3","Cmd 4","Unused"};
		mTable.addRow(WTableSection.HEADER,headings);

		JLabel lbl = new JLabel("");
		JTextField tfQty = new JTextField("");
		tfQty.setEditable(false);
		JTextField tfCosts = new JTextField("");
		tfCosts.setEditable(false);
		JTextField tfCmd1 = new JTextField("");
		tfCmd1.setEditable(false);
		JTextField tfCmd2 = new JTextField("");
		tfCmd2.setEditable(false);
		JTextField tfCmd3 = new JTextField("");
		tfCmd3.setEditable(false);
		JTextField tfCmd4 = new JTextField("");
		tfCmd4.setEditable(false);
		JComponent[] arr = new JComponent[] {lbl,tfQty,lbl,lbl,lbl,lbl,lbl,lbl,tfCosts,tfCmd1,tfCmd2,tfCmd3,tfCmd4,lbl};
		mTable.addRow(WTableSection.FOOTER,arr);

		JTextField tfArmyEquiv= new JTextField("");
		tfArmyEquiv.setEditable(false);
		JTextField tfEeCmd1 = new JTextField("");
		tfEeCmd1.setEditable(false);
		JTextField tfEeCmd2 = new JTextField("");
		tfEeCmd2.setEditable(false);
		JTextField tfEeCmd3 = new JTextField("");
		tfEeCmd3.setEditable(false);
		JTextField tfEeCmd4 = new JTextField("");
		tfEeCmd4.setEditable(false);
		JComponent[] arr2 = new JComponent[] {lbl,tfArmyEquiv,new JLabel("(equivalents)"),lbl,lbl,lbl,lbl,lbl,new JLabel("Equivalents:"),tfEeCmd1,tfEeCmd2,tfEeCmd3,tfEeCmd4,lbl};
		mTable.addRow(WTableSection.FOOTER,arr2);

		JTextField tfHalfArmy= new JTextField("");
		tfHalfArmy.setEditable(false);
		JTextField tfHaCmd1 = new JTextField("");
		tfHaCmd1.setEditable(false);
		JTextField tfHaCmd2 = new JTextField("");
		tfHaCmd2.setEditable(false);
		JTextField tfHaCmd3 = new JTextField("");
		tfHaCmd3.setEditable(false);
		JTextField tfHaCmd4 = new JTextField("");
		tfHaCmd4.setEditable(false);
		JComponent[] arr3 = new JComponent[] {lbl,tfHalfArmy,new JLabel("(half the army)"),lbl,lbl,lbl,lbl,lbl,new JLabel("Break points:"),tfHaCmd1,tfHaCmd2,tfHaCmd3,tfHaCmd4,lbl};
		mTable.addRow(WTableSection.FOOTER,arr3);
		return(mTable.getPanel());
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
			// TODO Auto-generated method stub
			
		}

		@Override
		public void insertRow(int afterRow) {
			// TODO Auto-generated method stub
		}

		@Override
		public void deleteRow(int row) {
			// TODO Auto-generated method stub
		}

		@Override
		public void clear() {
			// TODO Auto-generated method stub
		}

		@Override
		public void setField(ArmyListConstants field, String value) {
			switch (field) {
				case BOOK : mCbBooks.setSelectedItem(value); break;
				case DESCRIPTION : mTfDescription.setText(value); break;
				case YEAR: mTfYear.setText(value); break;
				default : log.warn("Unknown field {}", field);
			}
		}

		@Override
		public void setRowField(ArmyListConstants field, int row, String value) {
			// TODO Auto-generated method stub
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
		addRow();
	}

	//--------------------------------------------------------------------------
	private void doButtonClose(ActionEvent ae) {
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
	private void addRow() {
		// set all values, mostly defaulted
		int qty = 1;
		String desc = "";
		String drill = "";
		String type = "";
		String grade = "";
		String adj = "";
		int cmd1Qty = 0;
		int cmd2Qty = 0;
		int cmd3Qty = 0;
		int cmd4Qty = 0;

		JCheckBox chkBox = new JCheckBox();
		chkBox.addActionListener(e -> enableDeleteAndMoveButtons());
		
		SpinnerNumberModel snmQty = new SpinnerNumberModel(1,1,200,1);
		JSpinner spnrQty = new JSpinner(snmQty);
		spnrQty.setName(ArmyListConstants.QTY.toString());
		spnrQty.addChangeListener(e -> mModel.setRowQuantity(mTable.getNumberOfRows(WTableSection.BODY), (Integer)spnrQty.getValue(), mChanges));

		JTextField tfDesc = new JTextField(desc);
		tfDesc.setName(ArmyListConstants.DESC.toString());
		tfDesc.getDocument().addDocumentListener(new DocumentListenerAnychange());

		JComboBox<String> cbDrill = new JComboBox<>(mDrills.toArray(new String[0]));
		cbDrill.setName(ArmyListConstants.DRILL.toString());
		cbDrill.addActionListener(e -> mChanged = true);

		JComboBox<String> cbType = new JComboBox<>();
		cbType.setName(ArmyListConstants.TYPE.toString());
		cbType.addActionListener(e -> mChanged = true);

		JComboBox<String> cbGrade = new JComboBox<>();
		cbGrade.setName(ArmyListConstants.GRADE.toString());
		cbGrade.addActionListener(e -> mChanged = true);

		JComboBox<String> cbAdj = new JComboBox<>();
		cbAdj.setName(ArmyListConstants.ADJ.toString());
		cbAdj.addActionListener(e -> mChanged = true);

		JTextField tfCostEach = new JTextField(4);
		tfCostEach.setEditable(false);

		JTextField tfCostTotal = new JTextField(4);
		tfCostTotal.setEditable(false);

		SpinnerNumberModel snmCmd1 = new SpinnerNumberModel(0,0,100,1);
		JSpinner spnrCmd1 = new JSpinner(snmCmd1);
		spnrCmd1.setName(ArmyListConstants.CMD1_QTY.toString());
		spnrCmd1.addChangeListener(e -> mChanged = true);
		SpinnerNumberModel snmCmd2 = new SpinnerNumberModel(0,0,100,1);
		JSpinner spnrCmd2 = new JSpinner(snmCmd2);
		spnrCmd2.setName(ArmyListConstants.CMD2_QTY.toString());
		spnrCmd2.addChangeListener(e -> mChanged = true);
		SpinnerNumberModel snmCmd3 = new SpinnerNumberModel(0,0,100,1);
		JSpinner spnrCmd3 = new JSpinner(snmCmd3);
		spnrCmd3.setName(ArmyListConstants.CMD3_QTY.toString());
		spnrCmd3.addChangeListener(e -> mChanged = true);
		SpinnerNumberModel snmCmd4 = new SpinnerNumberModel(0,0,100,1);
		JSpinner spnrCmd4 = new JSpinner(snmCmd4);
		spnrCmd4.setName(ArmyListConstants.CMD4_QTY.toString());
		spnrCmd4.addChangeListener(e -> mChanged = true);

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

		// Now set values into the row to force recalculation
		if (drill.length() > 0) {
			cbDrill.setSelectedItem(drill);
		}
		if (type.length() > 0) {
			cbType.setSelectedItem(type);
		}
		if (grade.length() > 0) {
			cbGrade.setSelectedItem(grade);
		}
		if (adj.length() > 0) {
			cbAdj.setSelectedItem(adj);
		}
		snmQty.setValue(qty);
		snmCmd1.setValue(cmd1Qty);
		snmCmd2.setValue(cmd2Qty);
		snmCmd3.setValue(cmd3Qty);
		snmCmd4.setValue(cmd4Qty);

		mModel.addRow(mChanges);

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
