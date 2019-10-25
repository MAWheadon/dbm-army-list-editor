package uk.org.peltast.ald.views;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import javax.swing.ButtonModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.DefaultTableModel;

import com.maw.GUI.WFrame;
import com.maw.armylistdesigner.ArmyListConstants;
import com.maw.armylistdesigner.controllers.ArmyListControllerI;
import com.maw.armylistdesigner.models.ArmyListDBMModel;
import com.maw.util.Instructions;
import com.maw.util.Instructions.Operation;
import com.maw.util.RestRequest;
import com.maw.util.WLog;

/** Shows a list of all your army lists and allows you to edit them.
 * 
 * @author Mark Andrew Wheadon
 * @date 16th June 2012.
 * @copyright Mark Andrew Wheadon, 2012,2014.
 * @licence MIT License.
 */
public class ArmyListDBMSwing implements ArmyListViewI {
	private static final String[] c_index_table_columns = {"Name","Book","Year","Last modified"};
	private final ArmyListControllerI m_army_list_controller;
	private final RestRequest m_rest_req = new RestRequest();
	
	ArmyListDBMSwing m_this = this;	// not sure how else to refer to this instance inside mouse events etc.

	private WFrame m_frame = new WFrame("Army List DBM");

	private JTabbedPane m_tab_pane = new JTabbedPane();
	private HiddenColumnTableModel m_index_table_model = new HiddenColumnTableModel();
	private JTable m_index_table = new JTable(m_index_table_model);
	private final JTextField m_status_line = new JTextField("Welcome ... Click New to create army, or double-click a listed one to edit");

	private JButton m_new_btn = new JButton(new ImageIcon(getClass().getResource("/com/maw/res/document-new.png")));

	//--------------------------------------------------------------------------
	public ArmyListDBMSwing(ArmyListControllerI contolleri) {
		m_army_list_controller = contolleri;
		SwingUtilities.invokeLater(new SetupGui());
	}	// ArmyListDBMSwing

	//--------------------------------------------------------------------------
	private class SetupGui implements Runnable {
		@Override
		public void run() {
			Container cont = m_frame.getContentPane();
			JPanel pnl = new JPanel(new BorderLayout());
			pnl.add(m_tab_pane, BorderLayout.CENTER);
			m_status_line.setEditable(false);
			pnl.add(m_status_line,BorderLayout.SOUTH);
			cont.add(pnl);

			JPanel pnl_main = new JPanel(new BorderLayout());
			JPanel pnl_btns = new JPanel(new FlowLayout(FlowLayout.LEFT));
			m_new_btn.setBorderPainted(false);
			m_new_btn.setContentAreaFilled(false);
			LineBorder bdr = new LineBorder(Color.DARK_GRAY,1,true);
			Border margin = new EmptyBorder(4,4,4,4);
			m_new_btn.setBorder(new CompoundBorder(bdr, margin));
			m_new_btn.getModel().addChangeListener(new ChangeListener() {
		            @Override
		            public void stateChanged(ChangeEvent e) {
		                ButtonModel model = (ButtonModel) e.getSource();
		                m_new_btn.setBorderPainted(model.isRollover());
//		                if (model.isPressed()) {
//		                    tip1Null.setBorder(compound2);
//		                }
		            }
		        });

			// code to support creation of a new army list
			m_new_btn.addActionListener(new ActionListener(){
				@Override
				public void actionPerformed(ActionEvent arg0) {
					m_rest_req.setRequest(RestRequest.Operation.CREATE,ArmyListConstants.c_army_list);
					Instructions instructions = m_army_list_controller.processRequest(m_rest_req);
					process_update(instructions);
				}	// actionPerformed
			});
			pnl_btns.add(m_new_btn);
			pnl_main.add(pnl_btns,BorderLayout.NORTH);

			// set up index tab
			m_index_table_model.setColumnIdentifiers(c_index_table_columns);
			m_index_table.setAutoCreateRowSorter(true);
			m_index_table.addMouseListener(new MouseAdapter() {
				public void mouseClicked(MouseEvent me) {
					if (me.getClickCount() == 2) {
						JTable target = (JTable)me.getSource();
						int row = target.getSelectedRow();
						row = target.convertRowIndexToModel(row);
						String army_id = (String)m_index_table_model.getHiddenValue(row);
						int tab_index = get_tab(army_id);
						if (tab_index == -1) {
							ArmyListDBMEditorSwing ed = new ArmyListDBMEditorSwing(m_army_list_controller,army_id);
							m_tab_pane.add(ed);
							int tab_count = m_tab_pane.getTabCount();
							m_tab_pane.setSelectedIndex(tab_count-1);
						}	// if - we need to open a new tab
						else {
							m_tab_pane.setSelectedIndex(tab_index);
						}	// else - just switch to that tab
					}	// if - double click
				}	// mouseClicked
			});	// MouseAdapter

			JScrollPane index_sp = new JScrollPane(m_index_table);
			pnl_main.setName("Index");
			m_index_table.setFillsViewportHeight(true);
			pnl_main.add(index_sp,BorderLayout.CENTER);
			m_tab_pane.add(pnl_main);

			// The WindowListener we are adding need to happen first because the WFrame version does a system.Exit.
			// remove any current, add this one here, add back previous one (just the WFrame one).
			WindowListener wls[] = m_frame.getWindowListeners();
			for (WindowListener wl : wls) {
				m_frame.removeWindowListener(wl);
			}	// for - remove them all
			m_frame.addWindowListener(new WindowAdapter() {
				public void windowClosing(WindowEvent we) {
					int tab_count = m_tab_pane.getTabCount();
					for (int ii=0; ii<tab_count; ii++) {
						Component comp = m_tab_pane.getComponentAt(ii);
						if (comp instanceof ArmyListDBMEditorSwing) {
							ArmyListDBMEditorSwing ed = (ArmyListDBMEditorSwing)comp;
							ArmyListDBMModel mdl = ed.getModel();
							try {
//								mdl.saveArmy(ii);
							}	// try
							catch (Exception e) {
								WLog.log(Level.WARNING,"Error",e);
								error_message("Error: "+e);
							}	// catch
						}	// if
					}	// for - each tab
				}	// windowClosing
			});
			for (WindowListener wl : wls) {
				m_frame.addWindowListener(wl);
			}	// for - remove them all

			m_frame.setVisible(true);
			m_rest_req.setRequest(RestRequest.Operation.READ,ArmyListConstants.c_army_list_index,"*ALL");
			Instructions instructions = m_army_list_controller.processRequest(m_rest_req);
			process_update(instructions);
		}	// run
	}	// setup_gui

	//--------------------------------------------------------------------------
	private void error_message(String msg) {
		JOptionPane.showMessageDialog(m_frame.getContentPane(),msg);
	}	// confirm_message

	//--------------------------------------------------------------------------
	private int get_tab(String id) {
		int tab_count = m_tab_pane.getTabCount();
		for (int ii=0; ii<tab_count; ii++) {
			Component comp = m_tab_pane.getComponentAt(ii);
			if (comp instanceof ArmyListDBMEditorSwing) {
				ArmyListDBMEditorSwing ed = (ArmyListDBMEditorSwing)comp;
				ArmyListDBMModel mdl = ed.getModel();
				String id2 = mdl.getId();
				if (id.equals(id2)) {
					return(ii);
				}	// if - found an existing tab
			}	// if
		}	// for - each tab
		return(-1);
	}	// get_tab

	//--------------------------------------------------------------------------
	private void write_status(String msg) {
		m_status_line.setText(msg);
	}	// write_status

	//--------------------------------------------------------------------------
	private void process_update(Instructions instructions) {
		int count = instructions.getCount();
		for (int ii=0; ii<count; ii++) {
			Operation op = instructions.getOperation(ii);
			String target = instructions.getTarget(ii);
			Map<String,Object> inst = instructions.getInstruction(ii);
			switch (op) {
				case ADD : {
					if (target.equals(ArmyListConstants.c_index_row)) {
						Object row_data[] = new Object[c_index_table_columns.length];
						row_data[0] = inst.get(ArmyListConstants.c_army_name);
						row_data[1] = inst.get(ArmyListConstants.c_army_book);
						row_data[2] = inst.get(ArmyListConstants.c_army_year);
						String mod_s = (String)inst.get(ArmyListConstants.c_last_modified);
						Long mod = Long.parseLong(mod_s);
						Date dte = new Date(mod);
						row_data[3] = dte;
						Object id = inst.get(ArmyListConstants.c_army_id);
						m_index_table_model.addRow(row_data);
						m_index_table_model.addHiddenValue(id);
						break;
					}	// if - new army

					if (target.equals(ArmyListConstants.c_army_list)) {
						try {
						}	// try
						catch (Exception e) {
							WLog.log(Level.WARNING,e);
							write_status("Oops, error creating new army ...");
						}	// catch
						break;
					}	// if - new army
					break;
				}	// case - ADD
				default : {
					WLog.log(Level.WARNING,"Unknown operation {0}.",op);
				}	// default
			}
		}	// for
	}	// processUpdate

	//--------------------------------------------------------------------------
	private void popup_message(String msg) {
		JOptionPane.showConfirmDialog(m_frame, msg);
	}	// popup_message

	//--------------------------------------------------------------------------
	private class HiddenColumnTableModel extends DefaultTableModel {
		List<Object> m_hidden = new ArrayList<Object>();

	    public Object getHiddenValue(int row_index) {
	    	int row_count = m_hidden.size();
	    	if (row_index >= row_count) {
	    		return(null);
	    	}	// if
	    	Object obj = m_hidden.get(row_index);
	    	return(obj);
	    }	// getHiddenValue

	    public void addHiddenValue(Object value) {
	    	m_hidden.add(value);
	    }	// addHiddenValue

	    public void setHiddenValue(int row_index, Object value) {
	    	m_hidden.set(row_index,value);
	    }	// setHiddenValue

	    @Override
	    public boolean isCellEditable(int row, int column) {
	       return false;	// all cells false
	    }	// isCellEditable
	}	// IndexTableModel
}	// ArmyListDBMSwing
