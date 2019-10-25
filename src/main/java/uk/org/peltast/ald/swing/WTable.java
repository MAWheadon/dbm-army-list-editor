package uk.org.peltast.ald.swing;

import java.awt.Component;
import java.util.ArrayList;
import java.util.logging.Level;

import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;

import com.maw.util.WLog;

/** A Table implemented with GroupLayout. It features a header section, a 
 * footer section and a body section. You have to specify the total number of
 * columns you want. Columns cannot vary, you can add, remove and shift rows.
 *  
 * @author Mark Andrew Wheadon.
 * @date 11th June 2012.
 * @Copyright, Mark Andrew Wheadon, 2012, 2013.
 * @licence MIT License.
 */
public class WTable {
	private final int m_nbr_columns;
	private final ArrayList<ArrayList<JComponent>> m_header_components = new ArrayList<ArrayList<JComponent>>();
	private final ArrayList<ArrayList<JComponent>> m_body_components = new ArrayList<ArrayList<JComponent>>();
	private final ArrayList<ArrayList<JComponent>> m_footer_components = new ArrayList<ArrayList<JComponent>>();
	private final JPanel m_panel = new JPanel();

	//--------------------------------------------------------------------------
	public WTable(int nbr_columns) {
		m_nbr_columns = nbr_columns;
	}	// table

	//--------------------------------------------------------------------------
	public JPanel getPanel() {return(m_panel);}

	//--------------------------------------------------------------------------
	/** Used for adding any JComponents to the row */
	public void addRow(WTableSection section, JComponent ... comps) {
		if (comps.length != m_nbr_columns) {
			throw new IllegalArgumentException("Number of components does not match number of columns.");
		}	// if

		ArrayList<JComponent> row = new ArrayList<JComponent>();
		for (JComponent comp : comps) {
			row.add(comp);
		}	// for - each component
		switch (section) {
			case HEADER : {
				m_header_components.add(row);
				break;
			}	// case
			case BODY : {
				m_body_components.add(row);
				break;
			}	// case
			case FOOTER : {
				m_footer_components.add(row);
				break;
			}	// case				
		}	// switch
		rebuild_layout();
	}	// addRow

	//--------------------------------------------------------------------------
	/** Convenience method used for adding a row of labels */
	public void addRow(WTableSection section, String ... labels) {
		JLabel[] label_arr = new JLabel[labels.length];
		int ii = 0;
		for (String label : labels) {
			label_arr[ii] = new JLabel(label);
			ii++;
		}	// for - each component
		addRow(section,label_arr);
	}	// addRow

	//--------------------------------------------------------------------------
	private void rebuild_layout() {
		m_panel.removeAll();
		GroupLayout m_grp_lyt = new GroupLayout(m_panel);
		m_panel.setLayout(m_grp_lyt);
		m_grp_lyt.setAutoCreateGaps(true);
		m_grp_lyt.setAutoCreateContainerGaps(true);

		int nbr_header_rows = m_header_components.size();
		int nbr_body_rows = m_body_components.size();
		int nbr_footer_rows = m_footer_components.size();

		// add the components by row.
		GroupLayout.SequentialGroup vGroup = m_grp_lyt.createSequentialGroup();
		for (int rr=0; rr<nbr_header_rows; rr++) {
			GroupLayout.ParallelGroup pg = m_grp_lyt.createParallelGroup(Alignment.BASELINE);
			for (JComponent comp : m_header_components.get(rr)) {
				pg.addComponent(comp);
			}	// for
			vGroup.addGroup(pg);
		}	// for
		for (int rr=0; rr<nbr_body_rows; rr++) {
			GroupLayout.ParallelGroup pg = m_grp_lyt.createParallelGroup(Alignment.BASELINE);
			for (JComponent comp : m_body_components.get(rr)) {
				pg.addComponent(comp);
			}	// for
			vGroup.addGroup(pg);
		}	// for
		for (int rr=0; rr<nbr_footer_rows; rr++) {
			GroupLayout.ParallelGroup pg = m_grp_lyt.createParallelGroup(Alignment.BASELINE);
			for (JComponent comp : m_footer_components.get(rr)) {
				pg.addComponent(comp);
			}	// for
			vGroup.addGroup(pg);
		}	// for
		m_grp_lyt.setVerticalGroup(vGroup);

		// add the components (again) by column
		GroupLayout.SequentialGroup hGroup = m_grp_lyt.createSequentialGroup();
		for (int cc=0; cc<m_nbr_columns; cc++) {
			GroupLayout.ParallelGroup pg = m_grp_lyt.createParallelGroup();
			for (int rr=0; rr<nbr_header_rows; rr++) {
				ArrayList<JComponent> row = m_header_components.get(rr);
				JComponent comp = row.get(cc);
				pg.addComponent(comp);
			}	// for
			for (int rr=0; rr<nbr_body_rows; rr++) {
				ArrayList<JComponent> row = m_body_components.get(rr);
				JComponent comp = row.get(cc);
				pg.addComponent(comp);
			}	// for
			for (int rr=0; rr<nbr_footer_rows; rr++) {
				ArrayList<JComponent> row = m_footer_components.get(rr);
				JComponent comp = row.get(cc);
				pg.addComponent(comp);
			}	// for			
			hGroup.addGroup(pg);
		}	// for - every column
		m_grp_lyt.setHorizontalGroup(hGroup);
	}	// rebuild_layout

	//--------------------------------------------------------------------------
	/** Assume the body. */
	public void deleteRow(int row) {
		m_body_components.remove(row);
		rebuild_layout();
	}	// deleteRow

	//--------------------------------------------------------------------------
	/** Assume the body. */
	public void removeAllRows() {
		m_body_components.clear();
		rebuild_layout();
	}	// deleteRow

	//--------------------------------------------------------------------------
	/** Assume the body. */
	public void moveRowUp(int row_index) {
		ArrayList<JComponent> row = m_body_components.remove(row_index);
		m_body_components.add(row_index-1,row);
		rebuild_layout();
	}	// moveRowUp

	//--------------------------------------------------------------------------
	/** Assume the body. */
	public void moveRowDown(int row_index) {
		ArrayList<JComponent> row = m_body_components.remove(row_index);
		m_body_components.add(row_index+1,row);
		rebuild_layout();
	}	// moveRowDown

	//--------------------------------------------------------------------------
	public int getNumberOfRows(WTableSection section) {
		switch (section) {
			case HEADER : return(m_header_components.size());
			case BODY : return(m_body_components.size());
			case FOOTER : return(m_footer_components.size());
			default : throw new IllegalArgumentException();
		}	// switch
	}	// getNumberOfRows

	//--------------------------------------------------------------------------
	public JComponent getComponent(WTableSection section, int row, int column) {
		WTableLocation loc = new WTableLocation(section,row,column);
		JComponent jcomp = getComponent(loc);
		return(jcomp);
	}	// getComponent

	//--------------------------------------------------------------------------
	public String getValue(WTableSection section, int row, int column) {
		JComponent jcomp = getComponent(section,row,column);
		String val = getFieldAsText(jcomp);
		return(val);
	}	// getValue

	//--------------------------------------------------------------------------
	public void setValue(WTableSection section, int row, int column, String val) {
		JComponent jcomp = getComponent(section,row,column);
		if (jcomp instanceof JTextComponent) {
			JTextComponent jtcomp = (JTextComponent)jcomp;
			String current_value = jtcomp.getText();
			if (current_value.equals(val) == false) {
				jtcomp.setText(val);
			}	// if - only change if different to avoid firing unnecessary events.
		}	// if
	}	// setValue

	//--------------------------------------------------------------------------
	public JComponent getComponent(WTableLocation location) {
		ArrayList<ArrayList<JComponent>> rows = m_body_components;
		switch (location.m_section) {
			case HEADER : rows = m_header_components;	break;
			case FOOTER : rows = m_footer_components;	break;
			case BODY: rows = m_body_components;	break;
		}	// switch
		ArrayList<JComponent> row2 = rows.get(location.m_row);
		JComponent jcomp = row2.get(location.m_col);
		return(jcomp);
	}	// getComponent

	//--------------------------------------------------------------------------
	public JComponent getComponentFromDocument(Document doc) {
		JComponent jcomp = get_component_from_document(WTableSection.BODY,doc);
		if (jcomp != null) {
			return(jcomp);
		}	// if
		jcomp = get_component_from_document(WTableSection.FOOTER,doc);
		if (jcomp != null) {
			return(jcomp);
		}	// if
		jcomp = get_component_from_document(WTableSection.HEADER,doc);
		if (jcomp != null) {
			return(jcomp);
		}	// if
		throw new IllegalArgumentException("Document "+doc.toString()+" not found.");
	}	// getComponentFromDocument

	//--------------------------------------------------------------------------
	private JComponent get_component_from_document(WTableSection section, Document doc) {
		ArrayList<ArrayList<JComponent>> comps = null;
		switch (section) {
			case HEADER : {
				comps = m_header_components;
				break;
			}	// case - header
			case FOOTER : {
				comps = m_footer_components;
				break;
			}	// case - footer
			default : {
				comps = m_body_components;
				break;
			}	// case - bidy
		}	// switch
		for (ArrayList<JComponent> row : comps) {
			for (JComponent jcomp : row) {
				if (jcomp instanceof JSpinner) {
					JSpinner spinner = (JSpinner)jcomp;
					JSpinner.NumberEditor ne = (JSpinner.NumberEditor)spinner.getEditor();
					JFormattedTextField tf = ne.getTextField();
					Document doc2 = tf.getDocument();
					if (doc == doc2) {
						return(jcomp);
					}	// if - a matching document within the component
				}	// if a text component
				if (jcomp instanceof JTextComponent) {
					JTextComponent jtcomp = (JTextComponent)jcomp;
					Document doc2 = jtcomp.getDocument();
					if (doc == doc2) {
						return(jcomp);
					}	// if - a matching document within the component
				}	// if a text component
			}	// for each component on a row
		}	// for each row
		return(null);
	}	// get_component_from_document

	//--------------------------------------------------------------------------
	public WTableLocation getLocation(Component comp) {
		JComponent jcomp = (JComponent)comp;
		WTableLocation loc = findComponent(m_body_components,jcomp);
		if (loc != null) {
			loc.m_section = WTableSection.BODY;
			return(loc);
		}	// if
		loc = findComponent(m_header_components,jcomp);
		if (loc != null) {
			loc.m_section = WTableSection.HEADER;
			return(loc);
		}	// if
		loc = findComponent(m_footer_components,jcomp);
		if (loc != null) {
			loc.m_section = WTableSection.FOOTER;
			return(loc);
		}	// if
		String txt = "unknown";
		if (comp instanceof JTextComponent) {
			txt = ((JTextComponent)comp).getText();
		}	// if
		throw new IllegalArgumentException("Component "+txt+" not found.");
	}	// getLocation

	//--------------------------------------------------------------------------
	private WTableLocation findComponent(ArrayList<ArrayList<JComponent>> comp_rows, JComponent comp) {
		int rows = comp_rows.size();
		for (int row=0; row<rows; row++) {
			ArrayList<JComponent> comps = comp_rows.get(row);
			int cols = comps.size();
			for (int col=0; col<cols; col++) {
				JComponent compx = comps.get(col);
				if (compx == comp) {
					WTableLocation loc = new WTableLocation(WTableSection.BODY,row,col);
					return(loc);
				}	// if - we found a matching component (we don't know which section it in in
			}	// for - each col
		}	// for - each row
		return(null);
	}	// findComponent

	//--------------------------------------------------------------------------
	public String getFieldAsText(Component comp) {
		String val = null;
		if (comp instanceof JTextField) {
			val = ((JTextField)comp).getText();
			return(val);
		}	// if
		if (comp instanceof JSpinner) {
			Object obj = ((JSpinner)comp).getModel().getValue();
			val = obj.toString();
			return(val);
		}	// if
		if (comp instanceof JComboBox) {
			Object obj = ((JComboBox)comp).getModel().getSelectedItem();
			if (obj == null) {
				WLog.log(Level.WARNING,"JComboBox named {0} has no selected item. the number of items is {1}.",comp.getName(),((JComboBox) comp).getItemCount());
				return("");
			}	// if 
			val = obj.toString();
			return(val);
		}	// if
		if (comp instanceof JCheckBox) {
			JCheckBox jcb = (JCheckBox)comp;
			if (jcb.isSelected()) {
				return("Y");
			}	// if
			return("");
		}	// if
		return(val);
	}	// getFieldAsText

	//--------------------------------------------------------------------------
	public class WTableLocation {
		public WTableSection m_section;
		public int m_row;
		public int m_col;
		public WTableLocation(WTableSection section, int row, int column) {
			m_section = section;
			m_row = row;
			m_col = column;
		}	// WTableLocation
		public String toString() {
			return(m_section.toString() + "," + m_row + "," + m_col);
		}	// toString
		public boolean equals(WTableLocation loc) {
			if (m_section != loc.m_section) {
				return(false);
			}	// if
			if (m_row != loc.m_row) {
				return(false);
			}	// if
			if (m_col != loc.m_col) {
				return(false);
			}	// if
			return(true);
		}	// equals
	}	// WTableLocation

	//--------------------------------------------------------------------------
	public enum WTableSection {
		HEADER,
		FOOTER,
		BODY;
	}	// WTableSection
}	// Table
