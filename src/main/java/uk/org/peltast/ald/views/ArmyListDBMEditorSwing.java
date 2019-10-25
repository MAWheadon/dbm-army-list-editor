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
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.Document;

import com.maw.GUI.WTable;
import com.maw.GUI.WTable.WTableLocation;
import com.maw.GUI.WTable.WTableSection;
import com.maw.armylistdesigner.ArmyListConstants;
import com.maw.armylistdesigner.controllers.ArmyListControllerI;
import com.maw.armylistdesigner.models.ArmyListDBMModel;
import com.maw.util.Instructions;
import com.maw.util.Instructions.Operation;
import com.maw.util.RestRequest;
import com.maw.util.WFileAccess;
import com.maw.util.WLog;

/** An editor for an individual army list.
 * 
 * @author Mark Andrew Wheadon
 * @date 26th June 2012.
 * @copyright Mark Andrew Wheadon, 2012,2014.
 * @licence MIT License.
 */
public class ArmyListDBMEditorSwing extends JPanel implements ActionListener, ChangeListener, DocumentListener {
	private static enum ColNo {e_opt,e_qty,e_desc,e_drill,e_type,e_grade,e_adj,e_cost,e_total,e_cmd1,e_cmd2,e_cmd3,e_cmd4,e_unused};
	private final ArmyListDBMEditorSwing m_this = this;	// not sure how else to refer to this instance inside mouse events etc.
	private final RestRequest m_rest_req = new RestRequest();

	private final ArmyListControllerI m_army_list_controller;
	private final String m_army_id;	// a unique identifier for this army
//	private JPanel m_pnl_main = new JPanel(new BorderLayout());	// buttons go in north, inner panel goes in centre.
	private JPanel m_pnl_inner = new JPanel(new BorderLayout());
	private JPanel m_pnl_btns = new JPanel();
	private JPanel m_pnl_top = new JPanel();
	private JPanel m_pnl_buttons = new JPanel();
	private JButton m_btn_add = new JButton("Add");
	private JButton m_btn_delete = new JButton("Delete ...");
	private JButton m_btn_move_up = new JButton("Move up");
	private JButton m_btn_move_down = new JButton("Move down");
	private ButtonAddMenuListener m_btn_add_pressed = new ButtonAddMenuListener();
	private ButtonDeleteMenuListener m_btn_delete_pressed = new ButtonDeleteMenuListener();
	private ButtonMoveUpListener m_btn_move_up_pressed = new ButtonMoveUpListener();
	private ButtonMoveDownListener m_btn_move_down_pressed = new ButtonMoveDownListener();
	private JComboBox m_cb_books;
	private JTextField m_tf_year = new JTextField(6);
	private JTextField m_tf_description = new JTextField(20);
	private JTextField m_tf_cost_file = new JTextField(20);
	private Document m_army_description_doc;
	private Document m_army_year_doc;

	private WTable m_table = new WTable(14);
	private JPanel m_pnl_tbl = m_table.getPanel();
	private JScrollPane m_sp_tbl = new JScrollPane(m_pnl_tbl);

	//--------------------------------------------------------------------------
	ArmyListDBMEditorSwing(ArmyListControllerI conntroller, String army_id) {
		m_army_list_controller = conntroller;
		m_army_id = army_id;
		this.setLayout(new 	BorderLayout());	// TODO: ????
		setup_gui();
	}	// ArmyListDBMSwing

	//--------------------------------------------------------------------------
	private void setup_gui() {
		JButton btn = new JButton("Close");
		m_pnl_btns.add(btn);
		btn.addActionListener(this);
		btn = new JButton("Copy");
		m_pnl_btns.add(btn);
		btn.addActionListener(this);
		btn = new JButton("Print ...");
		m_pnl_btns.add(btn);
		btn.addActionListener(this);
		btn = new JButton("Export to txt ...");
		m_pnl_btns.add(btn);
		btn.addActionListener(this);
		btn = new JButton("Delete ...");
		m_pnl_btns.add(btn);
		btn.addActionListener(this);
		add(m_pnl_btns,BorderLayout.NORTH);
		add(m_pnl_inner,BorderLayout.CENTER);

		m_pnl_inner.add(m_pnl_top,BorderLayout.NORTH);
		m_pnl_top.add(new JLabel("Book"));
		m_cb_books = new JComboBox();
		m_cb_books.addActionListener(m_this);
		m_army_year_doc = m_tf_year.getDocument();
		m_army_year_doc.addDocumentListener(m_this);
		m_army_description_doc = m_tf_description.getDocument();
		m_army_description_doc.addDocumentListener(m_this);
		m_tf_cost_file.setEditable(false);
		m_pnl_top.add(m_cb_books);
		m_pnl_top.add(new JLabel("        Year"));
		m_pnl_top.add(m_tf_year);
		m_pnl_top.add(new JLabel("        Army"));
		m_pnl_top.add(m_tf_description);
		m_pnl_top.add(new JLabel("        Cost file"));
		m_pnl_top.add(m_tf_cost_file);

		m_pnl_inner.add(m_pnl_buttons,BorderLayout.SOUTH);
		m_pnl_buttons.add(m_btn_add);
		m_pnl_buttons.add(m_btn_delete);
		m_pnl_buttons.add(m_btn_move_up);
		m_pnl_buttons.add(m_btn_move_down);
		m_btn_add.addActionListener(m_btn_add_pressed);
		m_btn_delete.addActionListener(m_btn_delete_pressed);
		m_btn_move_up.addActionListener(m_btn_move_up_pressed);
		m_btn_move_down.addActionListener(m_btn_move_down_pressed);
		enable_delete_and_move_buttons();

		String[] headings = new String[] {"?","Qty","Description","Drill","Type","Grade","Adjustment","Cost","Total","Cmd 1","Cmd 2","Cmd 3","Cmd 4","Unused"};
		m_table.addRow(WTableSection.HEADER,headings);
		m_pnl_inner.add(m_sp_tbl,BorderLayout.CENTER);

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
		m_table.addRow(WTableSection.FOOTER,arr);

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
		m_table.addRow(WTableSection.FOOTER,arr2);

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
		m_table.addRow(WTableSection.FOOTER,arr3);

		// get the data
		m_rest_req.setRequest(RestRequest.Operation.READ,ArmyListConstants.c_army_list,m_army_id);
		Instructions instructions = m_army_list_controller.processRequest(m_rest_req);
		process_update(instructions);
	}	// setup_gui

	//--------------------------------------------------------------------------
	@Override
	public void actionPerformed(ActionEvent ae) {
		String cmd = ae.getActionCommand();
		Instructions instructions = new Instructions();

		if (cmd.equals("Close")) {
			m_instructions.clear();
			m_instructions.addInstruction(Instructions.Operation.DELETE,ArmyListConstants.c_tab,ArmyListConstants.c_army_id,m_army_id);
			String update = instructions.serialiseAsString();
			m_army_list_conntroller.processUpdate(update);
			return;
		}	// if - close

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

		if (cmd.equals("Export to txt ...")) {
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
		}	// if - Export to txt

		if (cmd.equals("Print ...")) {
			try {
				PrintRequestAttributeSet prt_rqs_att_set = new HashPrintRequestAttributeSet();
				PrinterJob pj = null;
				try {
					pj = PrinterJob.getPrinterJob();
				}	// try
				catch (AccessControlException ace) {
					WLog.log(Level.WARNING,"Error trying to print:",ace);
					JOptionPane.showMessageDialog(this,"Sorry, you do not appear to have authority to print.","Printing Error",JOptionPane.WARNING_MESSAGE);
					return;
				}	// catch
//					check_page_format(pj);
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
		}	// print

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
			enable_delete_and_move_buttons();
		}	// if - one of the check boxes
		else {
			// check to see if one of the north components
			String updates = null;
			m_changed = true;
			if (jc == m_cb_books) {
				String book = (String)m_cb_books.getSelectedItem();
				m_army_list_dbm_model.processInput(e_rqs.put.toString(),ArmyListConstants.c_army_book,book);
			}
			else {
				WTableLocation loc = m_table.getLocation(jc);
				updates = send_change(loc);
			}	// else
			if (updates != null && updates.length() > 0) {
				process_update(updates);
			}	// if
		}	// else - not a checkbox

	}	// actionPerformed

	//--------------------------------------------------------------------------
	private void enable_delete_and_move_buttons() {
		int checked_count = 0;
		int row_count = m_table.getNumberOfRows(WTableSection.BODY);
		for (int row_nbr=0; row_nbr<row_count; row_nbr++) {
			String chk = m_table.getValue(WTableSection.BODY,row_nbr,0);
			if (chk.length() > 0) {
				checked_count++;
			}	// if
		}	// for - each row
		m_btn_delete.setEnabled(checked_count>0);
		if (checked_count == 0) {
			m_btn_move_down.setEnabled(false);
			m_btn_move_up.setEnabled(false);
		}	// if
		else {
			String chk = m_table.getValue(WTableSection.BODY,0,0);	// top row
			m_btn_move_up.setEnabled(chk.length() <= 0);
			chk = m_table.getValue(WTableSection.BODY,row_count-1,0);	// bottom row
			m_btn_move_down.setEnabled(chk.length() <= 0);
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
			String str = m_tf_description.getText();
			g2d.drawString(str,c_left_margin_1,100);
			str = m_tf_year.getText();
			g2d.drawString(str,c_left_margin_1+200,100);
			str = (String)m_cb_books.getSelectedItem();
			g2d.drawString(str,c_left_margin_1+275,100);
			g2d.drawLine(0,105,2000,105);
			String total_cost = m_table.getValue(WTableSection.FOOTER,0,ColNo.e_total.ordinal());
			String total_qty = m_table.getValue(WTableSection.FOOTER,0,ColNo.e_qty.ordinal());
			String total_el_eq = m_table.getValue(WTableSection.FOOTER,1,ColNo.e_qty.ordinal());
			String half_army = m_table.getValue(WTableSection.FOOTER,2,ColNo.e_qty.ordinal());
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
		int row_count = m_table.getNumberOfRows(WTableSection.BODY);
		boolean cmd_heading_printed = false;
		for (int rr=0; rr<row_count; rr++) {
			String str = m_table.getValue(WTableSection.BODY,rr,ColNo.e_cmd1.ordinal()-1+cmd);
			int cmd_qty = str.length()>0 ? Integer.parseInt(str) : 0;
			if (cmd_qty == 0) {
				continue;
			}	// if - no elements for this row in this command so skip
			String desc = m_table.getValue(WTableSection.BODY,rr,ColNo.e_desc.ordinal());
			String drill = m_table.getValue(WTableSection.BODY,rr,ColNo.e_drill.ordinal());
			String type = m_table.getValue(WTableSection.BODY,rr,ColNo.e_type.ordinal());
			String grade = m_table.getValue(WTableSection.BODY,rr,ColNo.e_grade.ordinal());
			String adj = m_table.getValue(WTableSection.BODY,rr,ColNo.e_adj.ordinal());
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
			String total_el = m_table.getValue(WTableSection.FOOTER,0,ColNo.e_cmd1.ordinal()-1+cmd);
			String eq = m_table.getValue(WTableSection.FOOTER,1,ColNo.e_cmd1.ordinal()-1+cmd);
			String break_point = m_table.getValue(WTableSection.FOOTER,2,ColNo.e_cmd1.ordinal()-1+cmd);
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
	private class ButtonAddMenuListener implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent ae) {
			int row = m_army_list_dbm_model.addRowBefore(999);
			add_row(null);
			validate();
		}	// actionPerformed
	}	// MenuListener

	//--------------------------------------------------------------------------
	private void add_row(Map<String,Object> row_map) {
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
		if (row_map != null) {
			qty = get_map_value(row_map,ArmyListConstants.QTY,qty);
			desc = get_map_value(row_map,ArmyListConstants.DESC,desc);
			drill = get_map_value(row_map,ArmyListConstants.DRILL,drill);
			type = get_map_value(row_map,ArmyListConstants.TYPE,type);
			grade = get_map_value(row_map,ArmyListConstants.GRADE,grade);
			adj = get_map_value(row_map,ArmyListConstants.ADJUSTMENT,adj);
			cmd1_qty = get_map_value(row_map,ArmyListConstants.ROW_CMD1_QTY,cmd1_qty);
			cmd2_qty = get_map_value(row_map,ArmyListConstants.ROW_CMD2_QTY,cmd2_qty);
			cmd3_qty = get_map_value(row_map,ArmyListConstants.ROW_CMD3_QTY,cmd3_qty);
			cmd4_qty = get_map_value(row_map,ArmyListConstants.ROW_CMD4_QTY,cmd4_qty);
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
		m_table.addRow(WTableSection.BODY,arr);

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
	private String get_map_value(Map<String,Object> map, String key, String dft) {
		String ret = dft;
		if (map.containsKey(key)) {
			ret = (String)map.get(key);
		}	// if
		return(ret);
	}	// get_map_value

	//--------------------------------------------------------------------------
	private int get_map_value(Map<String,Object> map, String key, int dft) {
		int ret = dft;
		if (map.containsKey(key)) {
			ret = (Integer)map.get(key);
		}	// if
		return(ret);
	}	// get_map_value

	//--------------------------------------------------------------------------
	private class ButtonDeleteMenuListener implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent ae) {
			int row_count = m_table.getNumberOfRows(WTableSection.BODY);
			// we delete rows from the bottom up so the row numbers don't change.
			for (int row_nbr=row_count-1; row_nbr>=0; row_nbr--) {
				String chk = m_table.getValue(WTableSection.BODY,row_nbr,0);
				if (chk.length() > 0) {
					m_table.deleteRow(row_nbr);
					String updates = m_army_list_dbm_model.deleteRow(row_nbr);
					if (updates != null && updates.length() > 0) {
						process_update(updates);
					}	// if
				}	// if
			}	// for - each row
			enable_delete_and_move_buttons();	// there won't be any checked rows now
		}	// actionPerformed
	}	// MenuListener

	//--------------------------------------------------------------------------
	private class ButtonMoveUpListener implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent ae) {
			int row_count = m_table.getNumberOfRows(WTableSection.BODY);
			for (int row_nbr=0; row_nbr<row_count; row_nbr++) {
				String chk = m_table.getValue(WTableSection.BODY,row_nbr,0);
				if (chk.length() > 0) {
					if (row_nbr > 0) {
						m_table.moveRowUp(row_nbr);
						m_army_list_dbm_model.moveRowUp(row_nbr);
					}	// if
				}	// if
			}	// for - each row
			enable_delete_and_move_buttons();	// there won't be any checked rows now
		}	// actionPerformed
	}	// ButtonMoveUpListener

	//--------------------------------------------------------------------------
	private class ButtonMoveDownListener implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent ae) {
			int row_count = m_table.getNumberOfRows(WTableSection.BODY);
			// move rows down the bottom up so checked rows don't trip over each other.
			for (int row_nbr=row_count-1; row_nbr>=0; row_nbr--) {
				String chk = m_table.getValue(WTableSection.BODY,row_nbr,0);
				if (chk.length() > 0) {
					if (row_nbr < row_count-1) {
						m_table.moveRowDown(row_nbr);
						m_army_list_dbm_model.moveRowDown(row_nbr);
					}	// if
				}	// if
			}	// for - each row
			enable_delete_and_move_buttons();	// there won't be any checked rows now
		}	// actionPerformed
	}	// ButtonMoveDownListener

	//--------------------------------------------------------------------------
	/** Sends location of change to the model and receives the reply (other
	 * fields updated as a result).
	 * @param loc The location of the field that cause the change.
	 * @return A YAML string of side effect changes (e.g. updated totals).
	 */
	private String send_change(WTableLocation loc) {
		String data = m_table.getValue(loc.m_section,loc.m_row,loc.m_col);
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
						add_row(row_map);
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
							m_table.setValue(WTableSection.BODY,row_nbr,ColNo.e_qty.ordinal(),row_value);
							continue;
						}	// if
						if (row_key.equals(ArmyListConstants.DESC)) {
							m_table.setValue(WTableSection.BODY,row_nbr,ColNo.e_desc.ordinal(),row_value);
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
							m_table.setValue(WTableSection.BODY,row_nbr,ColNo.e_cost.ordinal(),row_value);
							continue;
						}	// if
						if (row_key.equals(ArmyListConstants.TOTAL_COST)) {
							m_table.setValue(WTableSection.BODY,row_nbr,ColNo.e_total.ordinal(),row_value);
							continue;
						}	// if
						if (row_key.equals(ArmyListConstants.ROW_CMD1_QTY)) {
							m_table.setValue(WTableSection.BODY,row_nbr,ColNo.e_cmd1.ordinal(),row_value);
							continue;
						}	// if
						if (row_key.equals(ArmyListConstants.ROW_CMD2_QTY)) {
							m_table.setValue(WTableSection.BODY,row_nbr,ColNo.e_cmd2.ordinal(),row_value);
							continue;
						}	// if
						if (row_key.equals(ArmyListConstants.ROW_CMD3_QTY)) {
							m_table.setValue(WTableSection.BODY,row_nbr,ColNo.e_cmd3.ordinal(),row_value);
							continue;
						}	// if
						if (row_key.equals(ArmyListConstants.ROW_CMD4_QTY)) {
							m_table.setValue(WTableSection.BODY,row_nbr,ColNo.e_cmd4.ordinal(),row_value);
							continue;
						}	// if
						if (row_key.equals(ArmyListConstants.c_row_qty_unused)) {
							m_table.setValue(WTableSection.BODY,row_nbr,ColNo.e_unused.ordinal(),row_value);
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
				m_cb_books.setSelectedItem(value);
				continue;
			}	// if
			if (key.equals(ArmyListConstants.c_army_name)) {
				m_tf_description.setText(value);
				continue;
			}	// if
			if (key.equals(ArmyListConstants.c_army_year)) {
				m_tf_year.setText(value);
				continue;
			}	// if
			if (key.equals(ArmyListConstants.c_total_qty)) {
				m_table.setValue(WTableSection.FOOTER,0,ColNo.e_qty.ordinal(),value);
				continue;
			}	// if
			if (key.equals(ArmyListConstants.c_total_equiv)) {
				m_table.setValue(WTableSection.FOOTER,1,ColNo.e_qty.ordinal(),value);
				continue;
			}	// if
			if (key.equals(ArmyListConstants.c_half_army)) {
				m_table.setValue(WTableSection.FOOTER,2,ColNo.e_qty.ordinal(),value);
				continue;
			}	// if
			if (key.equals(ArmyListConstants.c_total_cost)) {
				m_table.setValue(WTableSection.FOOTER,0,ColNo.e_total.ordinal(),value);
				continue;
			}	// if
			if (key.equals(ArmyListConstants.c_cmd1_total_qty)) {
				m_table.setValue(WTableSection.FOOTER,0,ColNo.e_cmd1.ordinal(),value);
				continue;
			}	// if
			if (key.equals(ArmyListConstants.c_cmd2_total_qty)) {
				m_table.setValue(WTableSection.FOOTER,0,ColNo.e_cmd2.ordinal(),value);
				continue;
			}	// if
			if (key.equals(ArmyListConstants.c_cmd3_total_qty)) {
				m_table.setValue(WTableSection.FOOTER,0,ColNo.e_cmd3.ordinal(),value);
				continue;
			}	// if
			if (key.equals(ArmyListConstants.c_cmd4_total_qty)) {
				m_table.setValue(WTableSection.FOOTER,0,ColNo.e_cmd4.ordinal(),value);
				continue;
			}	// if
			if (key.equals(ArmyListConstants.c_cmd1_break_point)) {
				m_table.setValue(WTableSection.FOOTER,2,ColNo.e_cmd1.ordinal(),value);
				continue;
			}	// if
			if (key.equals(ArmyListConstants.c_cmd2_break_point)) {
				m_table.setValue(WTableSection.FOOTER,2,ColNo.e_cmd2.ordinal(),value);
				continue;
			}	// if
			if (key.equals(ArmyListConstants.c_cmd3_break_point)) {
				m_table.setValue(WTableSection.FOOTER,2,ColNo.e_cmd3.ordinal(),value);
				continue;
			}	// if
			if (key.equals(ArmyListConstants.c_cmd4_break_point)) {
				m_table.setValue(WTableSection.FOOTER,2,ColNo.e_cmd4.ordinal(),value);
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
		JComponent jc = m_table.getComponent(WTableSection.BODY,row_nbr,col_nbr);
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
			WTableLocation loc = m_table.getLocation(jc);
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

	private void change_insert_remove_update(DocumentEvent de) {
		Document doc = de.getDocument();
		m_changed = true;
		if (doc == m_army_year_doc) {
			String year = m_tf_year.getText();
			m_army_list_dbm_model.processInput(e_rqs.put.toString(),ArmyListConstants.c_army_year,year);
		}	// if
		else {
			if (doc == m_army_description_doc) {
				String name = m_tf_description.getText();
				m_army_list_dbm_model.processInput(e_rqs.put.toString(),ArmyListConstants.c_army_name,name);
			}	// if
			else {
				int row_count = m_table.getNumberOfRows(WTableSection.BODY);
				for (int rr=0; rr<row_count; rr++) {
					JComponent jcomp = m_table.getComponent(WTableSection.BODY,rr,2);
					if (jcomp instanceof JTextField) {
						JTextField tf = (JTextField)jcomp;
						Document doc2 = tf.getDocument();
						if (doc == doc2) {
							String desc = tf.getText();
							m_army_list_dbm_model.processInput(e_rqs.put.toString(),ArmyListConstants.DESC,Integer.toString(rr),desc);
						}	// if
					}	// if
				}	// for - each row
			}	// else - check the table desciptions
		}	// else
	}	// change_insert_remove_update
}	// ArmyListDBMSwing
