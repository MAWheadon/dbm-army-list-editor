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
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

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
import javax.swing.table.DefaultTableModel;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLStreamException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import uk.org.peltast.ald.App;
import uk.org.peltast.ald.models.ArmyListIndex;
import uk.org.peltast.ald.swing.WFrame;

/** Shows a list of all your army lists and allows you to edit them.
 * 
 * @author Mark Andrew Wheadon
 * @date 16th June 2012.
 * @copyright Mark Andrew Wheadon, 2012,2021.
 * @licence MIT License.
 */
public class ArmyListDBMSwing {
	private static final Logger log = LoggerFactory.getLogger(ArmyListDBMSwing.class);
	private static final String[] TABLE_COLUMNS = {"Name","Book","Year", "Points"};

	private WFrame mFrame = new WFrame("Army List DBM");

	private JTabbedPane mTabPane = new JTabbedPane();
	private HiddenColumnTableModel mIndexTableModel = new HiddenColumnTableModel();
	private JTable mIndexTable = new JTable(mIndexTableModel);
	private final JTextField mStatusLine = new JTextField("Welcome ... Click New to create army, or double-click a listed one to edit");
	private final ArmyListIndex mArmyListIndex = new ArmyListIndex();

	private JButton mNewBtn = new JButton(new ImageIcon(getClass().getResource("/icons/document-new.png")));
	private JButton mDeleteBtn = new JButton(new ImageIcon(getClass().getResource("/icons/edit-delete.png")));
	private JButton mCopyBtn = new JButton(new ImageIcon(getClass().getResource("/icons/edit-copy.png")));

	//--------------------------------------------------------------------------
	public void start() {
		URL url = getClass().getResource("/");
		log.info("Resource URL is {}", url);
		SwingUtilities.invokeLater(new SetupGui());
	}

	//--------------------------------------------------------------------------
	private class SetupGui implements Runnable {
		@Override
		public void run() {
			Container cont = mFrame.getContentPane();
			JPanel pnl = new JPanel(new BorderLayout());
			pnl.add(mTabPane, BorderLayout.CENTER);
			mStatusLine.setEditable(false);
			pnl.add(mStatusLine,BorderLayout.SOUTH);
			cont.add(pnl);

			JPanel pnlMain = new JPanel(new BorderLayout());
			JPanel pnlBtns = new JPanel(new FlowLayout(FlowLayout.LEFT));

			styleButtons(mNewBtn);
			mNewBtn.getModel().addChangeListener(e -> {
				ButtonModel model = (ButtonModel) e.getSource();
				mNewBtn.setBorderPainted(model.isRollover());
			});

			// code to support creation of a new army list
			mNewBtn.addActionListener(new ActionListener(){
				@Override
				public void actionPerformed(ActionEvent arg0) {
					// TODO:
				}
			});
			pnlBtns.add(mNewBtn);

			styleButtons(mDeleteBtn);
			mDeleteBtn.getModel().addChangeListener(e -> {
				ButtonModel model = (ButtonModel) e.getSource();
				mDeleteBtn.setBorderPainted(model.isRollover());
			});

			// code to support creation of a new army list
			mDeleteBtn.addActionListener(new ActionListener(){
				@Override
				public void actionPerformed(ActionEvent arg0) {
					int row = mIndexTable.getSelectedRow();
					log.info("Reqest to delete row {}", row);
					if (row >= 0) {
						row = mIndexTable.convertRowIndexToModel(row);
						String armyId = (String)mIndexTableModel.getHiddenValue(row);
						log.info("Army ID to delete is {}", armyId);
						int tabIndex = getTab(armyId);
						if (tabIndex >= 0) {
							errorMessage("Cannot delete as open in another tab");
							return;
						}
						log.info("About to delete army {}", armyId);
						
						String path = App.getArmyListPath(armyId);
						try {
							Files.delete(Paths.get(path));
						}
						catch (IOException e) {
							log.warn("File {} deletion failed", path, e);
						}
						mArmyListIndex.delete(armyId);
						try {
							saveIndex();
							mIndexTableModel.removeRow(row);
						}
						catch (IOException | XMLStreamException e) {
							log.warn("Could not save index", e);
							errorMessage("Could not save index");
						}
					}
				}
			});
			pnlBtns.add(mDeleteBtn);

			styleButtons(mCopyBtn);
			mCopyBtn.getModel().addChangeListener(e -> {
				ButtonModel model = (ButtonModel) e.getSource();
				mCopyBtn.setBorderPainted(model.isRollover());
			});

			// code to support creation of a new army list
			mCopyBtn.addActionListener(new ActionListener(){
				@Override
				public void actionPerformed(ActionEvent arg0) {
					// TODO:
				}
			});
			pnlBtns.add(mCopyBtn);
			

			pnlMain.add(pnlBtns,BorderLayout.NORTH);

			// set up index tab
			mIndexTableModel.setColumnIdentifiers(TABLE_COLUMNS);
			mIndexTable.setAutoCreateRowSorter(true);
			mIndexTable.addMouseListener(new MouseAdapter() {
				@Override
				public void mouseClicked(MouseEvent me) {
					if (me.getClickCount() == 2) {
						JTable target = (JTable)me.getSource();
						int row = target.getSelectedRow();
						row = target.convertRowIndexToModel(row);
						String armyId = (String)mIndexTableModel.getHiddenValue(row);
						int tabIndex = getTab(armyId);
						if (tabIndex == -1) {
							//ArmyListDBMEditorSwing ed = new ArmyListDBMEditorSwing(m_army_list_controller,armyId);
							//mTabPane.add(ed);
							int tabCount = mTabPane.getTabCount();
							mTabPane.setSelectedIndex(tabCount-1);
						}	// if - we need to open a new tab
						else {
							mTabPane.setSelectedIndex(tabIndex);
						}	// else - just switch to that tab
					}	// if - double click
				}	// mouseClicked
			});	// MouseAdapter

			JScrollPane indexSp = new JScrollPane(mIndexTable);
			pnlMain.setName("Index");
			mIndexTable.setFillsViewportHeight(true);
			pnlMain.add(indexSp,BorderLayout.CENTER);
			mTabPane.add(pnlMain);

			// The WindowListener we are adding need to happen first because the WFrame version does a system.Exit.
			// remove any current, add this one here, add back previous one (just the WFrame one).
			WindowListener wls[] = mFrame.getWindowListeners();
			for (WindowListener wl : wls) {
				mFrame.removeWindowListener(wl);
			}	// for - remove them all
			mFrame.addWindowListener(new WindowAdapter() {
				@Override
				public void windowClosing(WindowEvent we) {
					int tabCount = mTabPane.getTabCount();
					for (int ii=0; ii<tabCount; ii++) {
						Component comp = mTabPane.getComponentAt(ii);
						String panelName = comp.getName();
						if (panelName.equals("army")) {
							
						}
						/*
						ArmyListDBMModel mdl = ed.getModel();
						try {
							mdl.saveArmy(ii);
						}
						catch (Exception e) {
							log.warn("Error", e);
							errorMessage("Error: "+e);
						}
						*/
					}	// for - each tab
					try {
						saveIndex();
					}
					catch (Exception e) {
						log.warn("Error when saving index", e);
						errorMessage("Error when saving index: "+e);
					}
				}	// windowClosing
			});
			for (WindowListener wl : wls) {
				mFrame.addWindowListener(wl);
			}	// for - remove them all

			try {
				loadIndex();
			}
			catch (Exception e) {
				errorMessage("Unable to start app because of: " + e.getMessage());
			}
			mFrame.setVisible(true);
		}	// run

		//--------------------------------------------------------------------------
		private void saveIndex() throws IOException, XMLStreamException {
			String dataDir = App.getDataDirectory();
			File ff = new File(dataDir);
			if (!ff.exists()) {
				log.info("About to create {}", dataDir);
				boolean ok = ff.mkdirs();
				log.info("Create {} successful? {}", dataDir, ok);
			}
			String path = dataDir + File.separator + "index.xml";
			String xml = mArmyListIndex.getAsXML();
			try (FileWriter fr = new FileWriter(path); BufferedWriter br = new BufferedWriter(fr)) {
				br.write(xml);
			}
			log.info("Saving index XML complete");
		}

		//--------------------------------------------------------------------------
		private void styleButtons(JButton btn) {
			btn.setBorderPainted(false);
			btn.setContentAreaFilled(false);
			LineBorder bdr = new LineBorder(Color.DARK_GRAY,1,true);
			Border margin = new EmptyBorder(4,4,4,4);
			btn.setBorder(new CompoundBorder(bdr, margin));
		}

		//--------------------------------------------------------------------------
		private void errorMessage(String msg) {
			JOptionPane.showMessageDialog(mFrame.getContentPane(),msg);
		}

		//--------------------------------------------------------------------------
		private int getTab(String id) {
			int tabCount = mTabPane.getTabCount();
			for (int ii=0; ii<tabCount; ii++) {
				Component comp = mTabPane.getComponentAt(ii);
				String panelName = comp.getName();
				if (panelName.equals(id)) {
					return(ii);
				}
			}
			return(-1);
		}

		//--------------------------------------------------------------------------
		private void loadIndex() {
			mArmyListIndex.loadFromFile();
			String[] ids = mArmyListIndex.getEntryIDs();
			for (int ii=0; ii< ids.length; ii++) {
				String id = ids[ii];
				String book = mArmyListIndex.getBook(id);
				String name = mArmyListIndex.getName(id);
				String points = mArmyListIndex.getPoints(id);
				String year = mArmyListIndex.getYear(id);
				mIndexTableModel.addRow(new Object[] {name, book, year, points});
				mIndexTableModel.addHiddenValue(id);
			}
		}
	}

	//--------------------------------------------------------------------------
	private void writeStatus(String msg) {
		mStatusLine.setText(msg);
	}

	//--------------------------------------------------------------------------
	private void popupMessage(String msg) {
		JOptionPane.showConfirmDialog(mFrame, msg);
	}

	//--------------------------------------------------------------------------
	private class HiddenColumnTableModel extends DefaultTableModel {
		private static final long serialVersionUID = 1L;
		private final transient List<Object> mHidden = new ArrayList<>();

	    public Object getHiddenValue(int rowIndex) {
	    	int rowCount = mHidden.size();
	    	if (rowIndex >= rowCount) {
	    		return(null);
	    	}	// if
	    	Object obj = mHidden.get(rowIndex);
	    	return(obj);
	    }

	    public void addHiddenValue(Object value) {
	    	mHidden.add(value);
	    }

	    public void setHiddenValue(int rowIndex, Object value) {
	    	mHidden.set(rowIndex,value);
	    }

	    @Override
	    public boolean isCellEditable(int row, int column) {
	       return false;	// all cells false
	    }
	}
}
