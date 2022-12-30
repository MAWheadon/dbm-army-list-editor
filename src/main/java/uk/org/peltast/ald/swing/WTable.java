package uk.org.peltast.ald.swing;

import java.awt.Component;
import java.util.ArrayList;
import java.util.List;

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
import javax.swing.SpinnerModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** A Table implemented with GroupLayout. It features a header section, a 
 * footer section and a body section. You have to specify the total number of
 * columns you want. Columns cannot vary, you can add, remove and shift rows.
 *  
 * @author Mark Andrew Wheadon.
 * @date 11th June 2012.
 * @Copyright, Mark Andrew Wheadon, 2012, 2021.
 * @licence MIT License.
 */
public class WTable {
	private static final Logger log = LoggerFactory.getLogger(WTable.class);
	private final int mNumberOfColumns;
	private final List<List<JComponent>> mHeaderComponents = new ArrayList<>();
	private final List<List<JComponent>> mBodyComponents = new ArrayList<>();
	private final List<List<JComponent>> mFooterComponents = new ArrayList<>();
	private final JPanel mPanel = new JPanel();

	//--------------------------------------------------------------------------
	public WTable(int nbrColumns) {
		mNumberOfColumns = nbrColumns;
	}

	//--------------------------------------------------------------------------
	public JPanel getPanel() {return(mPanel);}

	//--------------------------------------------------------------------------
	/** Used for adding any JComponents to the row */
	public void addRow(WTableSection section, JComponent ... comps) {
		if (comps.length != mNumberOfColumns) {
			throw new IllegalArgumentException("Number of components does not match number of columns.");
		}	// if

		List<JComponent> row = new ArrayList<>();
		for (JComponent comp : comps) {
			row.add(comp);
		}
		switch (section) {
			case HEADER : {
				mHeaderComponents.add(row);
				break;
			}
			case BODY : {
				mBodyComponents.add(row);
				break;
			}
			case FOOTER : {
				mFooterComponents.add(row);
				break;
			}				
		}
		rebuildLayout();
	}

	//--------------------------------------------------------------------------
	/** Convenience method used for adding a row of labels */
	public void addRow(WTableSection section, String ... labels) {
		JLabel[] labelArr = new JLabel[labels.length];
		int ii = 0;
		for (String label : labels) {
			labelArr[ii] = new JLabel(label);
			ii++;
		}	// for - each component
		addRow(section,labelArr);
	}

	//--------------------------------------------------------------------------
	private void rebuildLayout() {
		mPanel.removeAll();
		GroupLayout groupLayout = new GroupLayout(mPanel);
		mPanel.setLayout(groupLayout);
		groupLayout.setAutoCreateGaps(true);
		groupLayout.setAutoCreateContainerGaps(true);

		int nbrHeaderRows = mHeaderComponents.size();
		int nbrBodyRows = mBodyComponents.size();
		int nbrFooterRows = mFooterComponents.size();

		// add the components by row.
		GroupLayout.SequentialGroup vGroup = groupLayout.createSequentialGroup();
		for (int rr=0; rr<nbrHeaderRows; rr++) {
			GroupLayout.ParallelGroup pg = groupLayout.createParallelGroup(Alignment.BASELINE);
			for (JComponent comp : mHeaderComponents.get(rr)) {
				pg.addComponent(comp);
			}	// for
			vGroup.addGroup(pg);
		}
		for (int rr=0; rr<nbrBodyRows; rr++) {
			GroupLayout.ParallelGroup pg = groupLayout.createParallelGroup(Alignment.BASELINE);
			for (JComponent comp : mBodyComponents.get(rr)) {
				pg.addComponent(comp, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE);	// to stop jComboBox with other than String expanding to fill space
			}	// for
			vGroup.addGroup(pg);
		}
		for (int rr=0; rr<nbrFooterRows; rr++) {
			GroupLayout.ParallelGroup pg = groupLayout.createParallelGroup(Alignment.BASELINE);
			for (JComponent comp : mFooterComponents.get(rr)) {
				pg.addComponent(comp);
			}	// for
			vGroup.addGroup(pg);
		}
		groupLayout.setVerticalGroup(vGroup);

		// add the components (again) by column
		GroupLayout.SequentialGroup hGroup = groupLayout.createSequentialGroup();
		for (int cc=0; cc<mNumberOfColumns; cc++) {
			GroupLayout.ParallelGroup pg = groupLayout.createParallelGroup();
			for (int rr=0; rr<nbrHeaderRows; rr++) {
				List<JComponent> row = mHeaderComponents.get(rr);
				JComponent comp = row.get(cc);
				pg.addComponent(comp);
			}	// for
			for (int rr=0; rr<nbrBodyRows; rr++) {
				List<JComponent> row = mBodyComponents.get(rr);
				JComponent comp = row.get(cc);
				pg.addComponent(comp);
			}	// for
			for (int rr=0; rr<nbrFooterRows; rr++) {
				List<JComponent> row = mFooterComponents.get(rr);
				JComponent comp = row.get(cc);
				pg.addComponent(comp);
			}	// for			
			hGroup.addGroup(pg);
		}	// for - every column
		groupLayout.setHorizontalGroup(hGroup);
	}

	//--------------------------------------------------------------------------
	/** Assume the body. */
	public void deleteRow(int row) {
		mBodyComponents.remove(row);
		rebuildLayout();
	}

	//--------------------------------------------------------------------------
	/** Assume the body. */
	public void removeAllRows() {
		log.info("About to remove all rows");
		mBodyComponents.clear();
		rebuildLayout();
	}

	//--------------------------------------------------------------------------
	/** Assume the body. */
	public void moveRowUp(int rowIndex) {
		List<JComponent> row = mBodyComponents.remove(rowIndex);
		mBodyComponents.add(rowIndex-1,row);
		rebuildLayout();
	}

	//--------------------------------------------------------------------------
	/** Assume the body. */
	public void moveRowDown(int rowIndex) {
		List<JComponent> row = mBodyComponents.remove(rowIndex);
		mBodyComponents.add(rowIndex+1,row);
		rebuildLayout();
	}

	//--------------------------------------------------------------------------
	public int getNumberOfRows(WTableSection section) {
		switch (section) {
			case HEADER : return(mHeaderComponents.size());
			case BODY : return(mBodyComponents.size());
			case FOOTER : return(mFooterComponents.size());
			default : throw new IllegalArgumentException();
		}	// switch
	}

	//--------------------------------------------------------------------------
	public JComponent getComponent(WTableSection section, int row, int column) {
		WTableLocation loc = new WTableLocation(section,row,column);
		JComponent jcomp = getComponent(loc);
		return(jcomp);
	}

	//--------------------------------------------------------------------------
	public String getValue(WTableSection section, int row, int column) {
		JComponent jcomp = getComponent(section,row,column);
		String val = getFieldAsText(jcomp);
		if (val.endsWith(".0")) {
			val = val.substring(0,val.length()-2);
		}
		return(val);
	}

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
	}

	//--------------------------------------------------------------------------
	public void setValue(WTableSection section, int row, int column, int nbr) {
		JComponent jcomp = getComponent(section,row,column);
		if (jcomp instanceof JTextComponent) {
			JTextComponent jtcomp = (JTextComponent)jcomp;
			String currentValue = jtcomp.getText();
			String newValue = Integer.toString(nbr);
			if (!currentValue.equals(newValue)) {
				jtcomp.setText(newValue);
			}	// if - only change if different to avoid firing unnecessary events.
		}	// if
		else if (jcomp instanceof JSpinner) {
			JSpinner spin = (JSpinner)jcomp;
			SpinnerModel mdl = spin.getModel();
			if (mdl instanceof SpinnerNumberModel) {
				SpinnerNumberModel nbrMdl = (SpinnerNumberModel)mdl;
				if (nbrMdl.getNumber().intValue() != nbr) {
					nbrMdl.setValue(nbr);
				}
			}
		}
	}

	//--------------------------------------------------------------------------
	public void setValue(WTableSection section, int row, int column, float nbr) {
		JComponent jcomp = getComponent(section,row,column);
		if (jcomp instanceof JTextComponent) {
			JTextComponent jtcomp = (JTextComponent)jcomp;
			String currentValue = jtcomp.getText();
			String newValue = Float.toString(nbr);
			if (!currentValue.equals(newValue)) {
				jtcomp.setText(newValue);
			}	// if - only change if different to avoid firing unnecessary events.
		}	// if
		else if (jcomp instanceof JSpinner) {
			JSpinner spin = (JSpinner)jcomp;
			SpinnerModel mdl = spin.getModel();
			if (mdl instanceof SpinnerNumberModel) {
				SpinnerNumberModel nbrMdl = (SpinnerNumberModel)mdl;
				if (nbrMdl.getNumber().floatValue() != nbr) {
					nbrMdl.setValue(nbr);
				}
			}
		}
	}

	//--------------------------------------------------------------------------
	public JComponent getComponent(WTableLocation location) {
		List<List<JComponent>> rows = mBodyComponents;
		switch (location.mSection) {
			case HEADER : rows = mHeaderComponents;	break;
			case FOOTER : rows = mFooterComponents;	break;
			case BODY: rows = mBodyComponents;	break;
		}
		List<JComponent> row2 = rows.get(location.mRow);
		JComponent jcomp = row2.get(location.mCol);
		return(jcomp);
	}

	//--------------------------------------------------------------------------
	public JComponent getComponentFromDocument(Document doc) {
		JComponent jcomp = getComponentFromDocument(WTableSection.BODY,doc);
		if (jcomp != null) {
			return(jcomp);
		}
		jcomp = getComponentFromDocument(WTableSection.FOOTER,doc);
		if (jcomp != null) {
			return(jcomp);
		}
		jcomp = getComponentFromDocument(WTableSection.HEADER,doc);
		if (jcomp != null) {
			return(jcomp);
		}
		throw new IllegalArgumentException("Document "+doc.toString()+" not found.");
	}

	//--------------------------------------------------------------------------
	private JComponent getComponentFromDocument(WTableSection section, Document doc) {
		List<List<JComponent>> comps = null;
		switch (section) {
			case HEADER : {
				comps = mHeaderComponents;
				break;
			}
			case FOOTER : {
				comps = mFooterComponents;
				break;
			}
			default : {
				comps = mBodyComponents;
				break;
			}
		}
		for (List<JComponent> row : comps) {
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
	}

	//--------------------------------------------------------------------------
	public WTableLocation getLocation(Component comp) {
		JComponent jcomp = (JComponent)comp;
		WTableLocation loc = findComponent(mBodyComponents, jcomp);
		if (loc != null) {
			loc.mSection = WTableSection.BODY;
			return(loc);
		}	// if
		loc = findComponent(mHeaderComponents,jcomp);
		if (loc != null) {
			loc.mSection = WTableSection.HEADER;
			return(loc);
		}	// if
		loc = findComponent(mFooterComponents,jcomp);
		if (loc != null) {
			loc.mSection = WTableSection.FOOTER;
			return(loc);
		}	// if
		String txt = "unknown";
		if (comp instanceof JTextComponent) {
			txt = ((JTextComponent)comp).getText();
		}	// if
		throw new IllegalArgumentException("Component "+txt+" not found.");
	}

	//--------------------------------------------------------------------------
	private WTableLocation findComponent(List<List<JComponent>> compRows, JComponent comp) {
		int rows = compRows.size();
		for (int row=0; row<rows; row++) {
			List<JComponent> comps = compRows.get(row);
			int cols = comps.size();
			for (int col=0; col<cols; col++) {
				JComponent compx = comps.get(col);
				if (compx == comp) {
					WTableLocation loc = new WTableLocation(WTableSection.BODY,row,col);
					return(loc);
				}	// if - we found a matching component (we don't know which section it in in
			}	// for - each column
		}	// for - each row
		return(null);
	}

	//--------------------------------------------------------------------------
	public String getFieldAsText(Component comp) {
		String val = null;
		if (comp instanceof JTextField) {
			val = ((JTextField)comp).getText();
			return(val);
		}
		if (comp instanceof JSpinner) {
			Object obj = ((JSpinner)comp).getModel().getValue();
			val = obj.toString();
			return(val);
		}
		if (comp instanceof JComboBox) {
			Object obj = ((JComboBox)comp).getModel().getSelectedItem();
			if (obj == null) {
				log.warn("JComboBox named {} has no selected item. the number of items is {}", comp.getName(), ((JComboBox) comp).getItemCount());
				return("");
			} 
			val = obj.toString();
			return(val);
		}
		if (comp instanceof JCheckBox) {
			JCheckBox jcb = (JCheckBox)comp;
			if (jcb.isSelected()) {
				return("Y");
			}
			return("");
		}
		return(val);
	}

	//--------------------------------------------------------------------------
	public void setFieldEnabled(WTableSection section, int row, int column, boolean enabled) {
		JComponent jcomp = getComponent(section, row, column);
		jcomp.setEnabled(enabled);
	}

	//--------------------------------------------------------------------------
	public void setFieldVisible(WTableSection section, int row, int column, boolean enabled) {
		JComponent jcomp = getComponent(section, row, column);
		jcomp.setVisible(enabled);
	}

	//--------------------------------------------------------------------------
	public class WTableLocation {
		public WTableSection mSection;
		private final int mRow;
		private final int mCol;
		public WTableLocation(WTableSection section, int row, int column) {
			mSection = section;
			mRow = row;
			mCol = column;
		}
		public String toString() {
			return(mSection.toString() + "," + mRow + "," + mCol);
		}
		public boolean equals(WTableLocation loc) {
			if (mSection != loc.mSection) {
				return(false);
			}
			if (mRow != loc.mRow) {
				return(false);
			}
			if (mCol != loc.mCol) {
				return(false);
			}
			return(true);
		}
		public int getRow() {
			return(mRow);
		}
		public int getColumn() {
			return(mCol);
		}
	}

	//--------------------------------------------------------------------------
	public enum WTableSection {
		HEADER,
		FOOTER,
		BODY;
	}
}
