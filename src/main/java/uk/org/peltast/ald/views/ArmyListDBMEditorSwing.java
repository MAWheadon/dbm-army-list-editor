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
import java.io.File;
import java.rmi.server.Operation;
import java.security.AccessControlException;
import java.text.MessageFormat;
import java.util.AbstractCollection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

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
import javax.swing.event.ChangeEvent;
import javax.swing.event.DocumentEvent;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.Document;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.org.peltast.ald.models.ArmyListDBMModel;
import uk.org.peltast.ald.models.ArmyListModelChange;
import uk.org.peltast.ald.swing.WTable;
import uk.org.peltast.ald.swing.WTable.WTableLocation;
import uk.org.peltast.ald.swing.WTable.WTableSection;

/** An editor for an individual army list.
 * 
 * @author Mark Andrew Wheadon
 * @date 26th June 2012.
 * @copyright Mark Andrew Wheadon, 2012,2021.
 * @licence MIT License.
 */
public class ArmyListDBMEditorSwing implements ArmyListModelChange {
	private static final Logger log = LoggerFactory.getLogger(ArmyListDBMEditorSwing.class);
	private enum ColNo {OPT,QTY,DESC,DRILL,TYPE,GRADE,ADJ,COST,TOTAL,CMD1,CMD2,CMD3,CMD4,UNUSED};

	private final ArmyListDBMModel mModel;
	private JPanel mPnlMain = new JPanel(new BorderLayout());	// buttons go in north, inner panel goes in centre.
	private Document mArmyDescriptionDoc;
	private Document mArmyYearDoc;

	private WTable mTable = new WTable(14);
	private JPanel mPnlTbl = mTable.getPanel();
	private JScrollPane mSpTbl = new JScrollPane(mPnlTbl);

	//--------------------------------------------------------------------------
	ArmyListDBMEditorSwing(ArmyListDBMModel model) {
		mModel = model;
		mModel.setChangeListener(this);
		setupGui();
	}

	//--------------------------------------------------------------------------
	private void setupGui() {
		JButton btnClose = new JButton("Close");
		JButton btnPrint = new JButton("Print ...");
		JButton btnExport = new JButton("Export to txt ...");
		JPanel pnlTopBtns = new JPanel();
		pnlTopBtns.add(btnClose);
		pnlTopBtns.add(btnPrint);
		pnlTopBtns.add(btnExport);
		btnClose.addActionListener(this::doButtonClose);
		btnPrint.addActionListener(this::doButtonPrint);
		btnExport.addActionListener(this::doButtonExportToText);

		mPnlMain.add(pnlTopBtns,BorderLayout.NORTH);
		String armyId = mModel.getArmyId();
		mPnlMain.setName(armyId);
		
		// Central panel
		JPanel pnlCentral = new JPanel();
		mPnlMain.add(pnlCentral,BorderLayout.CENTER);

		mPnlCentral.add(mPnlTop,BorderLayout.NORTH);
		mPnlTop.add(new JLabel("Book"));
		mCbBooks.addActionListener(m_this);
		mArmyYearDoc = mTfYear.getDocument();
		mArmyYearDoc.addDocumentListener(m_this);
		mArmyDescriptionDoc = mTfDescription.getDocument();
		mArmyDescriptionDoc.addDocumentListener(m_this);
		mTfCostFile.setEditable(false);
		mPnlTop.add(mCbBooks);
		mPnlTop.add(new JLabel("        Year"));
		mPnlTop.add(mTfYear);
		mPnlTop.add(new JLabel("        Army"));
		mPnlTop.add(mTfDescription);
		mPnlTop.add(new JLabel("        Cost file"));
		mPnlTop.add(mTfCostFile);

		mPnlCentral.add(mPnlListBtns,BorderLayout.SOUTH);
		JButton btnAdd = new JButton("Add");
		JButton btnDelete = new JButton("Delete");
		JButton btnMoveUp = new JButton("Move up");
		JButton btnMoveDown = new JButton("Move down");
		btnAdd.addActionListener(this::doButtonAdd);
		btnDelete.addActionListener(this::doButtonDelete);
		btnMoveUp.addActionListener(this::doButtonMoveUp);
		btnMoveDown.addActionListener(this::doButtonMoveDown);
		mPnlListBtns.add(btnAdd);
		mPnlListBtns.add(btnDelete);
		mPnlListBtns.add(btnMoveUp);
		mPnlListBtns.add(btnMoveDown);
		enableDeleteAndMoveButtons();

		String[] headings = new String[] {"?","Qty","Description","Drill","Type","Grade","Adjustment","Cost","Total","Cmd 1","Cmd 2","Cmd 3","Cmd 4","Unused"};
		mTable.addRow(WTableSection.HEADER,headings);
		mPnlCentral.add(mSpTbl,BorderLayout.CENTER);

		JLabel lbl = new JLabel("");
		JTextField tf_qty = new JTextField("");
		tf_qty.setEditable(false);
		JTextField tf_costs = new JTextField("");
		tf_costs.setEditable(false);
		JTextField tf_cmd1 = new JTextField("");
		tf_cmd1.setEditable(false);
		JTextField tf_cmd2 = new JTextField("");
		tf_cmd2.setEditable(false);
		JTextField tf_cmd3 = new JTextField("");
		tf_cmd3.setEditable(false);
		JTextField tf_cmd4 = new JTextField("");
		tf_cmd4.setEditable(false);
		JComponent[] arr = new JComponent[] {lbl,tf_qty,lbl,lbl,lbl,lbl,lbl,lbl,tf_costs,tf_cmd1,tf_cmd2,tf_cmd3,tf_cmd4,lbl};
		mTable.addRow(WTableSection.FOOTER,arr);

		JTextField tf_army_equiv= new JTextField("");
		tf_army_equiv.setEditable(false);
		JTextField tf_ee_cmd1 = new JTextField("");
		tf_ee_cmd1.setEditable(false);
		JTextField tf_ee_cmd2 = new JTextField("");
		tf_ee_cmd2.setEditable(false);
		JTextField tf_ee_cmd3 = new JTextField("");
		tf_ee_cmd3.setEditable(false);
		JTextField tf_ee_cmd4 = new JTextField("");
		tf_ee_cmd4.setEditable(false);
		JComponent[] arr2 = new JComponent[] {lbl,tf_army_equiv,new JLabel("(equivalents)"),lbl,lbl,lbl,lbl,lbl,new JLabel("Equivalents:"),tf_ee_cmd1,tf_ee_cmd2,tf_ee_cmd3,tf_ee_cmd4,lbl};
		mTable.addRow(WTableSection.FOOTER,arr2);

		JTextField tf_half_army= new JTextField("");
		tf_half_army.setEditable(false);
		JTextField tf_ha_cmd1 = new JTextField("");
		tf_ha_cmd1.setEditable(false);
		JTextField tf_ha_cmd2 = new JTextField("");
		tf_ha_cmd2.setEditable(false);
		JTextField tf_ha_cmd3 = new JTextField("");
		tf_ha_cmd3.setEditable(false);
		JTextField tf_ha_cmd4 = new JTextField("");
		tf_ha_cmd4.setEditable(false);
		JComponent[] arr3 = new JComponent[] {lbl,tf_half_army,new JLabel("(half the army)"),lbl,lbl,lbl,lbl,lbl,new JLabel("Break points:"),tf_ha_cmd1,tf_ha_cmd2,tf_ha_cmd3,tf_ha_cmd4,lbl};
		mTable.addRow(WTableSection.FOOTER,arr3);

		// get the data
		m_rest_req.setRequest(RestRequest.Operation.READ,ArmyListConstants.c_army_list,mArmyId);
		Instructions instructions = m_army_list_controller.processRequest(m_rest_req);
		process_update(instructions);
	}

	//--------------------------------------------------------------------------
	@Override
	public void actionPerformed(ActionEvent ae) {
		String cmd = ae.getActionCommand();
		Instructions instructions = new Instructions();

		if (cmd.equals("Copy")) {
			try {
				m_army_list_dbm_model.saveArmy(m_army_list_path);
				m_army_list_connector.copyArmy(m_army_list_path);
			}	// try
			catch (Exception e) {
				error_message("Error: "+e);
			}	// catch
			return;
		}	// if - copy



		if (cmd.equals("Delete ...")) {
			try {
				String msg = "Are you sure you want to delete this army list?";
				int ret = JOptionPane.showConfirmDialog(this,msg);
				if (ret != JOptionPane.OK_OPTION) {
					return;
				}	// if
				String id = m_army_list_dbm_model.getId();
				m_army_list_connector.deleteArmy(id);
			}	// try
			catch (Exception e) {
				error_message("Error: "+e);
			}	// catch
			return;
		}	// if - delete

		Object obj = ae.getSource();
		JComponent jc = (JComponent)obj;
		if (jc instanceof JCheckBox) {
			enableDeleteAndMoveButtons();
		}	// if - one of the check boxes
		else {
			// check to see if one of the north components
			String updates = null;
			m_changed = true;
			if (jc == mCbBooks) {
				String book = (String)mCbBooks.getSelectedItem();
				m_army_list_dbm_model.processInput(e_rqs.put.toString(),ArmyListConstants.c_army_book,book);
			}
			else {
				WTableLocation loc = mTable.getLocation(jc);
				updates = send_change(loc);
			}	// else
			if (updates != null && updates.length() > 0) {
				process_update(updates);
			}	// if
		}	// else - not a checkbox
	}

	//--------------------------------------------------------------------------
	private void enableDeleteAndMoveButtons() {
		int checkedCount = 0;
		int rowCount = mTable.getNumberOfRows(WTableSection.BODY);
		for (int row_nbr=0; row_nbr<rowCount; row_nbr++) {
			String chk = mTable.getValue(WTableSection.BODY,row_nbr,0);
			if (chk.length() > 0) {
				checkedCount++;
			}	// if
		}	// for - each row
		mBtnDelete.setEnabled(checkedCount>0);
		if (checkedCount == 0) {
			mBtnMoveDown.setEnabled(false);
			mBtnMoveUp.setEnabled(false);
		}	// if
		else {
			String chk = mTable.getValue(WTableSection.BODY,0,0);	// top row
			mBtnMoveUp.setEnabled(chk.length() <= 0);
			chk = mTable.getValue(WTableSection.BODY,rowCount-1,0);	// bottom row
			mBtnMoveDown.setEnabled(chk.length() <= 0);
		}	// else
	}	// enable_delete_and_move_buttons

	//--------------------------------------------------------------------------
	private class page_printer implements Printable {
		public int print(Graphics graphics, PageFormat pg_fmt, int pg_nbr) throws PrinterException {
			int ret = NO_SUCH_PAGE;
			final int c_left_margin_1 = 75;
			final int c_left_margin_2 = 310;
			WLog.log(Level.INFO,"About to print page {0}.",pg_nbr);

			Graphics2D g2d = (Graphics2D)graphics;
			g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
			Font font_heading = new Font("Arial", Font.BOLD, 11);
			Font font_plain = new Font("Arial", Font.PLAIN, 8);

			ret = PAGE_EXISTS;
			g2d.setFont(font_heading);
			String str = mTfDescription.getText();
			g2d.drawString(str,c_left_margin_1,100);
			str = mTfYear.getText();
			g2d.drawString(str,c_left_margin_1+200,100);
			str = (String)mCbBooks.getSelectedItem();
			g2d.drawString(str,c_left_margin_1+275,100);
			g2d.drawLine(0,105,2000,105);
			String total_cost = mTable.getValue(WTableSection.FOOTER,0,ColNo.TOTAL.ordinal());
			String total_qty = mTable.getValue(WTableSection.FOOTER,0,ColNo.QTY.ordinal());
			String total_el_eq = mTable.getValue(WTableSection.FOOTER,1,ColNo.QTY.ordinal());
			String half_army = mTable.getValue(WTableSection.FOOTER,2,ColNo.QTY.ordinal());
			str = MessageFormat.format("{0} total cost, {1} total elements, {2} equivalents, half the army is {3} elements",total_cost,total_qty,total_el_eq,half_army);
			g2d.setFont(font_plain);
			g2d.drawString(str,c_left_margin_1,120);
			int yy = 160;
			int yy1 = print_command(g2d,1,yy,c_left_margin_1);
			int yy2 = print_command(g2d,2,yy,c_left_margin_2);
			yy = Math.max(yy1,yy2);
			yy1 = print_command(g2d,3,yy,c_left_margin_1);
			yy2 = print_command(g2d,4,yy,c_left_margin_2);
			yy = Math.max(yy1,yy2);
			print_table_top_grid(g2d,yy,c_left_margin_1);
			WLog.log(Level.INFO,"Finished printing page {0}.",pg_nbr);
			return(ret);
		}	// print
	}	// page_printer

	//--------------------------------------------------------------------------
	private int print_command(Graphics2D g2d, int cmd, int yy, int left_margin) {
		final int c_line_height = 16;
		Font font_heading = new Font("Arial", Font.BOLD, 10);
		Font font_plain = new Font("Arial", Font.PLAIN, 8);
		int row_count = mTable.getNumberOfRows(WTableSection.BODY);
		boolean cmd_heading_printed = false;
		for (int rr=0; rr<row_count; rr++) {
			String str = mTable.getValue(WTableSection.BODY,rr,ColNo.CMD1.ordinal()-1+cmd);
			int cmd_qty = str.length()>0 ? Integer.parseInt(str) : 0;
			if (cmd_qty == 0) {
				continue;
			}	// if - no elements for this row in this command so skip
			String desc = mTable.getValue(WTableSection.BODY,rr,ColNo.DESC.ordinal());
			String drill = mTable.getValue(WTableSection.BODY,rr,ColNo.DRILL.ordinal());
			String type = mTable.getValue(WTableSection.BODY,rr,ColNo.TYPE.ordinal());
			String grade = mTable.getValue(WTableSection.BODY,rr,ColNo.GRADE.ordinal());
			String adj = mTable.getValue(WTableSection.BODY,rr,ColNo.ADJ.ordinal());
			if (cmd_heading_printed == false) {
				g2d.setFont(font_heading);
				g2d.drawString("Command "+cmd,left_margin,yy);
				yy += c_line_height;
				cmd_heading_printed = true;
			}	// if
			str = MessageFormat.format("{0} {1}, {2} {3}({4}) {5}",cmd_qty,desc,drill,type,grade,adj);
			g2d.setFont(font_plain);
			g2d.drawString(str,left_margin,yy);
			yy += c_line_height;
		}	// for - each row
		if (cmd_heading_printed) {
			String total_el = mTable.getValue(WTableSection.FOOTER,0,ColNo.CMD1.ordinal()-1+cmd);
			String eq = mTable.getValue(WTableSection.FOOTER,1,ColNo.CMD1.ordinal()-1+cmd);
			String break_point = mTable.getValue(WTableSection.FOOTER,2,ColNo.CMD1.ordinal()-1+cmd);
			String str = MessageFormat.format("{0} total elements,  break point is {1}",total_el,break_point);
			yy += c_line_height / 2;
			g2d.drawString(str,left_margin,yy);
		}	// if
		yy += c_line_height * 2d;
		return(yy);
	}	// print_command

	//--------------------------------------------------------------------------
	private void print_table_top_grid(Graphics2D g2d, int top_margin, int left_margin) {
		//	draw the table top grid, 3 boxes across and 2 boxes high
		final int c_size = 130; 	// of the box

		final int left_margin2 = left_margin + (c_size);
		final int left_margin3 = left_margin + (c_size * 2);
		final int right_margin = left_margin + (c_size * 3);	// 3 boxes
		final int middle_margin = top_margin + c_size;
		final int bottom_margin = middle_margin + c_size;

		// draw the 3 horizontal lines
		g2d.drawLine(left_margin,top_margin,right_margin,top_margin);
		g2d.drawLine(left_margin,middle_margin,right_margin,middle_margin);
		g2d.drawLine(left_margin,bottom_margin,right_margin,bottom_margin);

		// draw the 4 vertical lines
		g2d.drawLine(left_margin,top_margin,left_margin,bottom_margin);
		g2d.drawLine(left_margin2,top_margin,left_margin2,bottom_margin);
		g2d.drawLine(left_margin3,top_margin,left_margin3,bottom_margin);
		g2d.drawLine(right_margin,top_margin,right_margin,bottom_margin);
	}	// print_table_top_grid

	//--------------------------------------------------------------------------
	private void doButtonAdd(ActionEvent ae) {
		mModel.addRow();
		addRow(null);
		validate();
	}

	//--------------------------------------------------------------------------
	private void doButtonClose(ActionEvent ae) {
	}

	//--------------------------------------------------------------------------
	private void doButtonDelete(ActionEvent ae) {
	}

	//--------------------------------------------------------------------------
	private void doButtonExportToText(ActionEvent ae) {
		String dir_path = getArmyListPath();
		JFileChooser chooser = new JFileChooser(dir_path);
		if (m_army_list_path != null) {
			chooser.setCurrentDirectory(new File(m_army_list_path));
			String name = WFileAccess.getPathFileName(m_army_list_path);
			int ii = name.lastIndexOf('.');
			if (ii > 0) {
				name = name.substring(0,ii);
			}	// if
			name += ".txt";
			File ff = new File(name);
			chooser.setSelectedFile(ff);
		}	// if
		FileNameExtensionFilter filter = new FileNameExtensionFilter("Exported DBM army list", "txt");
		chooser.setFileFilter(filter);
		int ret = chooser.showSaveDialog(this);
		if (ret != JFileChooser.APPROVE_OPTION) {
			return;
		}	// if
		File export_txt_file = chooser.getSelectedFile();
		try {
			m_army_list_dbm_model.export_to_txt(export_txt_file.getPath());
		}	// try
		catch (Exception e) {
			WLog.log(Level.WARNING,e);
			error_message("Error: "+e);
		}	// catch
		return;
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
			PrintRequestAttributeSet prt_rqs_att_set = new HashPrintRequestAttributeSet();
			PrinterJob pj = null;
			try {
				pj = PrinterJob.getPrinterJob();
			}
			catch (AccessControlException ace) {
				log.warning("Error trying to print:",ace);
				JOptionPane.showMessageDialog(this,"Sorry, you do not appear to have authority to print.","Printing Error",JOptionPane.WARNING_MESSAGE);
				return;
			}
			Book bk = new Book();
			int nbr_of_pages = 1;
			page_printer pp = new page_printer();
			PageFormat page_fmt = pj.defaultPage();
			bk.append(pp,page_fmt,nbr_of_pages);
			pj.setPageable(bk);
			boolean ok = pj.printDialog(prt_rqs_att_set);
			if (ok) {
				try {
					WLog.log(Level.INFO,"About to print.","");
					RepaintManager currentManager = RepaintManager.currentManager(this);
					boolean dbl_buf = currentManager.isDoubleBufferingEnabled();
					if (dbl_buf) {
						currentManager.setDoubleBufferingEnabled(false);
					}	// if
					pj.print(prt_rqs_att_set);
					if (dbl_buf) {
						currentManager.setDoubleBufferingEnabled(true);
					}	// if
				}	// try
				catch (PrinterException pe) {
					WLog.log(Level.WARNING,"Printer error.",pe);
				}	// catch
			}	// if - user okayed print dialog
		}	// try
		catch (Throwable t) {
			WLog.log(Level.WARNING,"Error trying to print:",t);
			JOptionPane.showMessageDialog(this, t.toString(), "Printing Error", JOptionPane.WARNING_MESSAGE);
		}	// catch
		return;				
	}

	//--------------------------------------------------------------------------
	private void addRow(Map<String,Object> rowMap) {
		// set all values, mostly defaulted
		int qty = 0;
		String desc = "";
		String drill = "";
		String type = "";
		String grade = "";
		String adj = "";
		int cmd1_qty = 0;
		int cmd2_qty = 0;
		int cmd3_qty = 0;
		int cmd4_qty = 0;
		if (rowMap != null) {
			qty = getMapValue(rowMap,ArmyListConstants.QTY,qty);
			desc = getMapValue(rowMap,ArmyListConstants.DESC,desc);
			drill = getMapValue(rowMap,ArmyListConstants.DRILL,drill);
			type = getMapValue(rowMap,ArmyListConstants.TYPE,type);
			grade = getMapValue(rowMap,ArmyListConstants.GRADE,grade);
			adj = getMapValue(rowMap,ArmyListConstants.ADJUSTMENT,adj);
			cmd1_qty = getMapValue(rowMap,ArmyListConstants.ROW_CMD1_QTY,cmd1_qty);
			cmd2_qty = getMapValue(rowMap,ArmyListConstants.ROW_CMD2_QTY,cmd2_qty);
			cmd3_qty = getMapValue(rowMap,ArmyListConstants.ROW_CMD3_QTY,cmd3_qty);
			cmd4_qty = getMapValue(rowMap,ArmyListConstants.ROW_CMD4_QTY,cmd4_qty);
		}	// if - setting an existing row rather than inserting a blank row

		JCheckBox chk_box = new JCheckBox();
		chk_box.addActionListener(this);
		
		SpinnerNumberModel snm_qty = new SpinnerNumberModel(0,0,200,1);
		JSpinner spnr_qty = new JSpinner(snm_qty);
		spnr_qty.setName(ArmyListConstants.QTY);
		spnr_qty.addChangeListener(this);

		JTextField tf_desc = new JTextField(desc);
		tf_desc.setName(ArmyListConstants.DESC);
		Document doc_desc = tf_desc.getDocument();
		doc_desc.addDocumentListener(this);

		JComboBox cb_drill = new JComboBox(m_drills);
		cb_drill.setName(ArmyListConstants.DRILL);
		cb_drill.addActionListener(this);

		JComboBox cb_type = new JComboBox();
		cb_type.setName(ArmyListConstants.TYPE);
		cb_type.addActionListener(this);

		JComboBox cb_grade = new JComboBox();
		cb_grade.setName(ArmyListConstants.GRADE);
		cb_grade.addActionListener(this);

		JComboBox cb_adj = new JComboBox();
		cb_adj.setName(ArmyListConstants.ADJUSTMENT);
		cb_adj.addActionListener(this);

		JTextField tf_cost_each = new JTextField(4);
		tf_cost_each.setEditable(false);

		JTextField tf_cost_total = new JTextField(4);
		tf_cost_total.setEditable(false);

		SpinnerNumberModel snm_cmd1 = new SpinnerNumberModel(0,0,100,1);
		JSpinner spnr_cmd1 = new JSpinner(snm_cmd1);
		spnr_cmd1.setName(ArmyListConstants.ROW_CMD1_QTY);
		spnr_cmd1.addChangeListener(this);
		SpinnerNumberModel snm_cmd2 = new SpinnerNumberModel(0,0,100,1);
		JSpinner spnr_cmd2 = new JSpinner(snm_cmd2);
		spnr_cmd2.setName(ArmyListConstants.ROW_CMD2_QTY);
		spnr_cmd2.addChangeListener(this);
		SpinnerNumberModel snm_cmd3 = new SpinnerNumberModel(0,0,100,1);
		JSpinner spnr_cmd3 = new JSpinner(snm_cmd3);
		spnr_cmd3.setName(ArmyListConstants.ROW_CMD3_QTY);
		spnr_cmd3.addChangeListener(this);
		SpinnerNumberModel snm_cmd4 = new SpinnerNumberModel(0,0,100,1);
		JSpinner spnr_cmd4 = new JSpinner(snm_cmd4);
		spnr_cmd4.setName(ArmyListConstants.ROW_CMD4_QTY);
		spnr_cmd4.addChangeListener(this);

		JTextField tf_elements_unused = new JTextField(4);
		tf_elements_unused.setEditable(false);
		
		JComponent[] arr = new JComponent[] {
				chk_box,
				spnr_qty,
				tf_desc,
				cb_drill,
				cb_type,
				cb_grade,
				cb_adj,
				tf_cost_each,
				tf_cost_total,
				spnr_cmd1, spnr_cmd2, spnr_cmd3, spnr_cmd4,
				tf_elements_unused};
		mTable.addRow(WTableSection.BODY,arr);

		// Now set values into the row to force recalculation
		if (drill.length() > 0) {
			cb_drill.setSelectedItem(drill);
		}	// if
		if (type.length() > 0) {
			cb_type.setSelectedItem(type);
		}	// if
		if (grade.length() > 0) {
			cb_grade.setSelectedItem(grade);
		}	// if
		if (adj.length() > 0) {
			cb_adj.setSelectedItem(adj);
		}	// if
		snm_qty.setValue(qty);
		snm_cmd1.setValue(cmd1_qty);
		snm_cmd2.setValue(cmd2_qty);
		snm_cmd3.setValue(cmd3_qty);
		snm_cmd4.setValue(cmd4_qty);

		JComponent editor = spnr_qty.getEditor();
		JFormattedTextField field = (JFormattedTextField) editor.getComponent(0);
		field.grabFocus();	// TODO: Doesn't seem to work!
	}	// add_row

	//--------------------------------------------------------------------------
	private String getMapValue(Map<String,Object> map, String key, String dft) {
		String ret = dft;
		if (map.containsKey(key)) {
			ret = (String)map.get(key);
		}	// if
		return(ret);
	}

	//--------------------------------------------------------------------------
	private int getMapValue(Map<String,Object> map, String key, int dft) {
		int ret = dft;
		if (map.containsKey(key)) {
			ret = (Integer)map.get(key);
		}	// if
		return(ret);
	}

	//--------------------------------------------------------------------------
	JPanel getJPanel() {
		return(mPnlMain);
	}

	//--------------------------------------------------------------------------
	private class ButtonDeleteMenuListener implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent ae) {
			int row_count = mTable.getNumberOfRows(WTableSection.BODY);
			// we delete rows from the bottom up so the row numbers don't change.
			for (int row_nbr=row_count-1; row_nbr>=0; row_nbr--) {
				String chk = mTable.getValue(WTableSection.BODY,row_nbr,0);
				if (chk.length() > 0) {
					mTable.deleteRow(row_nbr);
					String updates = m_army_list_dbm_model.deleteRow(row_nbr);
					if (updates != null && updates.length() > 0) {
						process_update(updates);
					}	// if
				}	// if
			}	// for - each row
			enableDeleteAndMoveButtons();	// there won't be any checked rows now
		}	// actionPerformed
	}	// MenuListener

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
						m_army_list_dbm_model.moveRowUp(row_nbr);
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
			int row_count = mTable.getNumberOfRows(WTableSection.BODY);
			// move rows down the bottom up so checked rows don't trip over each other.
			for (int row_nbr=row_count-1; row_nbr>=0; row_nbr--) {
				String chk = mTable.getValue(WTableSection.BODY,row_nbr,0);
				if (chk.length() > 0) {
					if (row_nbr < row_count-1) {
						mTable.moveRowDown(row_nbr);
						m_army_list_dbm_model.moveRowDown(row_nbr);
					}	// if
				}	// if
			}	// for - each row
			enableDeleteAndMoveButtons();	// there won't be any checked rows now
		}	// actionPerformed
	}	// ButtonMoveDownListener

	//--------------------------------------------------------------------------
	/** Sends location of change to the model and receives the reply (other
	 * fields updated as a result).
	 * @param loc The location of the field that cause the change.
	 * @return A YAML string of side effect changes (e.g. updated totals).
	 */
	private String send_change(WTableLocation loc) {
		String data = mTable.getValue(loc.m_section,loc.m_row,loc.m_col);
		String out = null;
		if (loc.m_section == WTableSection.BODY) {
			switch (loc.m_col) {
				case 1 : out = m_army_list_dbm_model.processInput(e_rqs.put.toString(),ArmyListConstants.QTY,Integer.toString(loc.m_row),data);	break;
				case 2 : m_army_list_dbm_model.processInput(e_rqs.put.toString(),ArmyListConstants.DESC,Integer.toString(loc.m_row),data);	break;
				case 3 : out = m_army_list_dbm_model.processInput(e_rqs.put.toString(),ArmyListConstants.DRILL,Integer.toString(loc.m_row),data);	break;
				case 4 : out = m_army_list_dbm_model.processInput(e_rqs.put.toString(),ArmyListConstants.TYPE,Integer.toString(loc.m_row),data);	break;
				case 5 : out = m_army_list_dbm_model.processInput(e_rqs.put.toString(),ArmyListConstants.GRADE,Integer.toString(loc.m_row),data);	break;
				case 6 : out = m_army_list_dbm_model.processInput(e_rqs.put.toString(),ArmyListConstants.ADJUSTMENT,Integer.toString(loc.m_row),data);	break;
				case 9 : out = m_army_list_dbm_model.processInput(e_rqs.put.toString(),ArmyListConstants.c_row_cmd_qty,Integer.toString(loc.m_row),"1",data);	break;
				case 10 : out = m_army_list_dbm_model.processInput(e_rqs.put.toString(),ArmyListConstants.c_row_cmd_qty,Integer.toString(loc.m_row),"2",data);	break;
				case 11 : out = m_army_list_dbm_model.processInput(e_rqs.put.toString(),ArmyListConstants.c_row_cmd_qty,Integer.toString(loc.m_row),"3",data);	break;
				case 12 : out = m_army_list_dbm_model.processInput(e_rqs.put.toString(),ArmyListConstants.c_row_cmd_qty,Integer.toString(loc.m_row),"4",data);	break;
			}	// switch
		}	// if - a row
		WLog.log(Level.INFO,"Received update: {0}.",out);
		return(out);
	}	// send_change

	//--------------------------------------------------------------------------
	private void process_update(Instructions instructions) {
		while (instructions.hasNext()) {
			instructions.next();
			Operation op = instructions.getOperation();
			String target = instructions.getTarget();
			Map<String,Object> map = instructions.getInstruction();
			if (target.equals(ArmyListConstants.c_army_list_header)) {
				// we only support setting the values
				
				continue;
			}	// if
		}	// while
		Map<String,Object> map = WFileAccess.unserialiseString(yaml);
		Set<String> keys = map.keySet();
		Iterator<String> iter = keys.iterator();
		while (iter.hasNext()) {
			String key = iter.next();
			if (key.equals(ArmyListConstants.ROWS)) {
				List<Map<String,Object>> row_array = (List<Map<String,Object>>)map.get(key);
				int row_count = row_array.size();
				for (int rr=0; rr<row_count; rr++) {
					Map<String,Object> row_map = (Map<String,Object>)row_array.get(rr);
					Set<String> row_keys = row_map.keySet();
					Iterator<String> row_iter = row_keys.iterator();
					int row_nbr = rr;
					if (row_keys.contains(ArmyListConstants.ID)) {
						row_nbr = (Integer)row_map.get(ArmyListConstants.ID);
					}	// if - the row number is specified
					else {
						addRow(row_map);
						continue;
					}	// else
					while (row_iter.hasNext()) {
						String row_key = row_iter.next();
						String row_value = null;
						Object row_obj = row_map.get(row_key);
						if (row_obj instanceof AbstractCollection == false) {
							row_value = (String)row_obj.toString();
						}	// 
						if (row_key.equals(ArmyListConstants.QTY)) {
							mTable.setValue(WTableSection.BODY,row_nbr,ColNo.QTY.ordinal(),row_value);
							continue;
						}	// if
						if (row_key.equals(ArmyListConstants.DESC)) {
							mTable.setValue(WTableSection.BODY,row_nbr,ColNo.DESC.ordinal(),row_value);
							continue;
						}	// if
						if (row_key.equals(ArmyListConstants.DRILL)) {
							continue;
						}	// if
						if (row_key.equals(ArmyListConstants.TYPE)) {
							if (row_value == null) {
								update_combo(row_nbr,4,row_obj);
							}	// if
							continue;
						}	// if
						if (row_key.equals(ArmyListConstants.GRADE)) {
							if (row_value == null) {
								update_combo(row_nbr,5,row_obj);
							}	// if
							continue;
						}	// if
						if (row_key.equals(ArmyListConstants.ADJUSTMENT)) {
							if (row_value == null) {
								update_combo(row_nbr,6,row_obj);
							}	// if
							continue;
						}	// if
						if (row_key.equals(ArmyListConstants.ELEMENT_COST)) {
							mTable.setValue(WTableSection.BODY,row_nbr,ColNo.COST.ordinal(),row_value);
							continue;
						}	// if
						if (row_key.equals(ArmyListConstants.TOTAL_COST)) {
							mTable.setValue(WTableSection.BODY,row_nbr,ColNo.TOTAL.ordinal(),row_value);
							continue;
						}	// if
						if (row_key.equals(ArmyListConstants.ROW_CMD1_QTY)) {
							mTable.setValue(WTableSection.BODY,row_nbr,ColNo.CMD1.ordinal(),row_value);
							continue;
						}	// if
						if (row_key.equals(ArmyListConstants.ROW_CMD2_QTY)) {
							mTable.setValue(WTableSection.BODY,row_nbr,ColNo.CMD2.ordinal(),row_value);
							continue;
						}	// if
						if (row_key.equals(ArmyListConstants.ROW_CMD3_QTY)) {
							mTable.setValue(WTableSection.BODY,row_nbr,ColNo.CMD3.ordinal(),row_value);
							continue;
						}	// if
						if (row_key.equals(ArmyListConstants.ROW_CMD4_QTY)) {
							mTable.setValue(WTableSection.BODY,row_nbr,ColNo.CMD4.ordinal(),row_value);
							continue;
						}	// if
						if (row_key.equals(ArmyListConstants.c_row_qty_unused)) {
							mTable.setValue(WTableSection.BODY,row_nbr,ColNo.UNUSED.ordinal(),row_value);
							continue;
						}	// if
					}	// while
				}	// for - each row
				continue;
			}	// if
			Object obj = map.get(key);
			if (obj == null) {
				continue;
			}	// if
			String value = obj.toString();
			if (key.equals(ArmyListConstants.c_army_book)) {
				mCbBooks.setSelectedItem(value);
				continue;
			}	// if
			if (key.equals(ArmyListConstants.c_army_name)) {
				mTfDescription.setText(value);
				continue;
			}	// if
			if (key.equals(ArmyListConstants.c_army_year)) {
				mTfYear.setText(value);
				continue;
			}	// if
			if (key.equals(ArmyListConstants.c_total_qty)) {
				mTable.setValue(WTableSection.FOOTER,0,ColNo.QTY.ordinal(),value);
				continue;
			}	// if
			if (key.equals(ArmyListConstants.c_total_equiv)) {
				mTable.setValue(WTableSection.FOOTER,1,ColNo.QTY.ordinal(),value);
				continue;
			}	// if
			if (key.equals(ArmyListConstants.c_half_army)) {
				mTable.setValue(WTableSection.FOOTER,2,ColNo.QTY.ordinal(),value);
				continue;
			}	// if
			if (key.equals(ArmyListConstants.c_total_cost)) {
				mTable.setValue(WTableSection.FOOTER,0,ColNo.TOTAL.ordinal(),value);
				continue;
			}	// if
			if (key.equals(ArmyListConstants.c_cmd1_total_qty)) {
				mTable.setValue(WTableSection.FOOTER,0,ColNo.CMD1.ordinal(),value);
				continue;
			}	// if
			if (key.equals(ArmyListConstants.c_cmd2_total_qty)) {
				mTable.setValue(WTableSection.FOOTER,0,ColNo.CMD2.ordinal(),value);
				continue;
			}	// if
			if (key.equals(ArmyListConstants.c_cmd3_total_qty)) {
				mTable.setValue(WTableSection.FOOTER,0,ColNo.CMD3.ordinal(),value);
				continue;
			}	// if
			if (key.equals(ArmyListConstants.c_cmd4_total_qty)) {
				mTable.setValue(WTableSection.FOOTER,0,ColNo.CMD4.ordinal(),value);
				continue;
			}	// if
			if (key.equals(ArmyListConstants.c_cmd1_break_point)) {
				mTable.setValue(WTableSection.FOOTER,2,ColNo.CMD1.ordinal(),value);
				continue;
			}	// if
			if (key.equals(ArmyListConstants.c_cmd2_break_point)) {
				mTable.setValue(WTableSection.FOOTER,2,ColNo.CMD2.ordinal(),value);
				continue;
			}	// if
			if (key.equals(ArmyListConstants.c_cmd3_break_point)) {
				mTable.setValue(WTableSection.FOOTER,2,ColNo.CMD3.ordinal(),value);
				continue;
			}	// if
			if (key.equals(ArmyListConstants.c_cmd4_break_point)) {
				mTable.setValue(WTableSection.FOOTER,2,ColNo.CMD4.ordinal(),value);
				continue;
			}	// if
		}	// while - for each key (outside rows)
	}	// process_update

	//--------------------------------------------------------------------------
	/** Updates a JComboBox first turning off the listener so that updates
	 * do not get fired.
	 * @param row_nbr The row number in the table.
	 * @param row_obj A List of values (String).	 */
	private void update_combo(int row_nbr, int col_nbr, Object row_obj) {
		JComponent jc = mTable.getComponent(WTableSection.BODY,row_nbr,col_nbr);
		JComboBox cb = (JComboBox)jc;
		ActionListener[] als = cb.getActionListeners();
		for (ActionListener al : als) {
			cb.removeActionListener(al);
		}	// for
		String str = (String)cb.getSelectedItem();
		cb.removeAllItems();
		List<String> items = (List<String>)row_obj;
		int item_nbr = 0;
		int ii = 0;
		for (Object item : items) {
			if (item.equals(str)) {
				item_nbr = ii;
			}	// if
			cb.addItem(item);
			ii++;
		}	// for
		for (ActionListener al : als) {
			cb.addActionListener(al);
		}	// for
		cb.setSelectedIndex(item_nbr);
	}	// update_combo

	//--------------------------------------------------------------------------
	private boolean confirm_message(String msg) {
		int result = JOptionPane.showConfirmDialog(this,msg);
		if (result == JOptionPane.YES_OPTION) {
			return(true);
		}	// if
		return(false);
	}	// confirm_message

	//--------------------------------------------------------------------------
	private void error_message(String msg) {
		JOptionPane.showMessageDialog(this,msg);
	}	// confirm_message

	//--------------------------------------------------------------------------
	ArmyListDBMModel getModel() {
		return(m_army_list_dbm_model);
	}	// getModel

	//--------------------------------------------------------------------------
	@Override
	public void stateChanged(ChangeEvent ce) {
		Object obj = ce.getSource();
		if (obj instanceof JComponent) {
			JComponent jc = (JComponent)obj;
			WTableLocation loc = mTable.getLocation(jc);
			String updates = send_change(loc);
			m_changed = true;
			if (updates != null && updates.length() > 0) {
				process_update(updates);
			}	// if
		}	// if
	}	// stateChanged

	//--------------------------------------------------------------------------
	@Override
	public void changedUpdate(DocumentEvent de) {
		change_insert_remove_update(de); 
	}	// changedUpdate

	//--------------------------------------------------------------------------
	@Override
	public void insertUpdate(DocumentEvent de) {
		change_insert_remove_update(de); 
	}	// insertUpdate

	//--------------------------------------------------------------------------
	@Override
	public void removeUpdate(DocumentEvent de) {
		change_insert_remove_update(de); 
	}	// removeUpdate


	//--------------------------------------------------------------------------
	private void change_insert_remove_update(DocumentEvent de) {
		Document doc = de.getDocument();
		m_changed = true;
		if (doc == mArmyYearDoc) {
			String year = mTfYear.getText();
			m_army_list_dbm_model.processInput(e_rqs.put.toString(),ArmyListConstants.c_army_year,year);
		}	// if
		else {
			if (doc == mArmyDescriptionDoc) {
				String name = mTfDescription.getText();
				m_army_list_dbm_model.processInput(e_rqs.put.toString(),ArmyListConstants.c_army_name,name);
			}	// if
			else {
				int row_count = mTable.getNumberOfRows(WTableSection.BODY);
				for (int rr=0; rr<row_count; rr++) {
					JComponent jcomp = mTable.getComponent(WTableSection.BODY,rr,2);
					if (jcomp instanceof JTextField) {
						JTextField tf = (JTextField)jcomp;
						Document doc2 = tf.getDocument();
						if (doc == doc2) {
							String desc = tf.getText();
							m_army_list_dbm_model.processInput(e_rqs.put.toString(),ArmyListConstants.DESC,Integer.toString(rr),desc);
						}	// if
					}	// if
				}	// for - each row
			}	// else - check the table descriptions
		}	// else
	}	// change_insert_remove_update

	//--------------------------------------------------------------------------
	@Override
	public void change(ChangeType type, ChangeObject obj, ChangeItem item, String value) {
		// TODO Auto-generated method stub
		
	}

	//--------------------------------------------------------------------------
	@Override
	public void change(ChangeType type, ChangeObject obj, ChangeItem item, String[] values) {
		// TODO Auto-generated method stub
		
	}
}	// ArmyListDBMSwing
