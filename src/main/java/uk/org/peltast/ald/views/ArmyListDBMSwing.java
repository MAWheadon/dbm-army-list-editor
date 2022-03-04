package uk.org.peltast.ald.views;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.NoSuchFileException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JComboBox;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.table.DefaultTableModel;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLStreamException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import uk.org.peltast.ald.models.ArmyIndexModelChange;
import uk.org.peltast.ald.models.ArmyListConstants;
import uk.org.peltast.ald.models.ArmyListDBMModel;
import uk.org.peltast.ald.models.ArmyListIndex;
import uk.org.peltast.ald.models.ArmyListModelUtils;
import uk.org.peltast.ald.swing.WFrame;

/** Shows a list of all your army lists and allows you to edit them.
 * 
 * @author Mark Andrew Wheadon
 * @date 16th June 2012.
 * @copyright Mark Andrew Wheadon, 2012,2021.
 * @licence MIT License.
 */
public class ArmyListDBMSwing implements ArmyIndexModelChange {
	private static final Logger log = LoggerFactory.getLogger(ArmyListDBMSwing.class);
	private static final String[] TABLE_COLUMNS = {"Group", "Name", "Book", "Year", "Points"};

	private boolean mChanged = false;
	private final String mDataDir;
	private WFrame mFrame = new WFrame("Army List DBM");

	private JTabbedPane mTabPane = new JTabbedPane();
	private HiddenColumnTableModel mIndexTableModel = new HiddenColumnTableModel();
	private JTable mIndexTable = new JTable(mIndexTableModel);
	private final JTextField mStatusLine = new JTextField("Welcome ... Use File->New army to create an army, or right click on an army for more options");
	private final ArmyListIndex mArmyListIndex = new ArmyListIndex();

	private JMenuBar mMenuBar = new JMenuBar();
	private JMenu mMenuFile = new JMenu("File");
	private JMenuItem mMenuItemNewArmy = new JMenuItem("New army");
	private JMenuItem mMenuItemExportArmy = new JMenuItem("Export army");
	private JMenuItem mMenuItemImportArmy = new JMenuItem("Import army");
	private JMenuItem mMenuItemEditArmy = new JMenuItem("Edit army");
	private JMenuItem mMenuItemCopyArmy = new JMenuItem("Copy army");
	private JMenuItem mMenuItemChangeGroupOfArmy = new JMenuItem("Change group of army");
	private JMenuItem mMenuItemDeleteArmy = new JMenuItem("Delete army");
	private JMenuItem mMenuItemExit = new JMenuItem("Exit");
	private JPopupMenu mPopupMenu = new JPopupMenu();

	//--------------------------------------------------------------------------
	public ArmyListDBMSwing() {
		mDataDir = ArmyListModelUtils.getDataPath();
		File ff = new File(mDataDir);
		if (!ff.exists()) {
			log.info("About to create {}", mDataDir);
			boolean ok = ff.mkdirs();
			log.info("Create {} successful? {}", mDataDir, ok);
		}
	}

	//--------------------------------------------------------------------------
	public void start() {
		URL url = getClass().getResource("/");
		log.info("Resource URL is {}", url);
		SwingUtilities.invokeLater(new SetupGui());
	}

	//--------------------------------------------------------------------------
	private class SetupGui implements Runnable {
		private int mSelectedRow = -1;
		@Override
		public void run() {
			Container cont = mFrame.getContentPane();
			JPanel pnl = new JPanel(new BorderLayout());
			pnl.add(mTabPane, BorderLayout.CENTER);
			mStatusLine.setEditable(false);
			pnl.add(mStatusLine,BorderLayout.SOUTH);
			cont.add(pnl);

			JPanel pnlMain = new JPanel(new BorderLayout());

			mMenuItemNewArmy.addActionListener(e -> {
				newArmy();
			});

			mMenuItemDeleteArmy.addActionListener(e -> {
				String armyId = getSelectedArmyId();
				if (armyId != null) {
					int tabIndex = getTabIndex(armyId);
					if (tabIndex >= 0) {
						errorMessage("Cannot delete as open in another tab");
						return;
					}
					log.info("About to delete army {}", armyId);
					try {
						ArmyListDBMModel.deleteArmy(mDataDir, armyId);
					}
					catch (IOException ioe) {
						log.warn("Failed to delete army because of", ioe);
					}
					mArmyListIndex.delete(armyId);
					try {
						saveIndex();
						mIndexTableModel.removeRow(mSelectedRow);
					}
					catch (IOException | XMLStreamException ioe) {
						log.warn("Could not save index", ioe);
						errorMessage("Could not save index");
					}
				}
			});

			mMenuItemEditArmy.addActionListener(e -> {
				editArmy();
			});

			mMenuItemCopyArmy.addActionListener(e -> {
			});

			mMenuItemChangeGroupOfArmy.addActionListener(e -> {
				String[] groups = mArmyListIndex.getGroups();
				JComboBox<String> combo = new JComboBox<>(groups);
				combo.setEditable(true);
				JOptionPane.showMessageDialog(mFrame, combo, "Change army group", JOptionPane.QUESTION_MESSAGE);
				String groupNameNew = combo.getSelectedItem().toString();
				String armyId = getSelectedArmyId();
				if (armyId == null) {
					errorMessage("Army not selected");
				}
				else {
					mArmyListIndex.setGroupName(armyId, groupNameNew);
					try {
						saveIndex();
						mIndexTableModel.setValueAt(groupNameNew, mSelectedRow, 0);
					} catch (IOException | XMLStreamException e1) {
						errorMessage("Could not save index");
					}
				}
			});

			mMenuItemExit.addActionListener(e -> {
				// check if there are unsaved changes to any armies
				// else
				int reply = JOptionPane.showConfirmDialog(mFrame, "Are you sure you want to exit?", "Exit?",  JOptionPane.YES_NO_OPTION);
				if (reply == JOptionPane.YES_OPTION) {
					if (mChanged) {
						try {
							saveIndex();
						}
						catch (Exception e1) {
							log.warn("Error when saving index", e1);
							errorMessage("Error when saving index: "+e1);
						}
					}
					mFrame.dispose();
				}
			});
			mFrame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

			pnl.add(mMenuBar,BorderLayout.NORTH);
			mMenuBar.add(mMenuFile);
			mMenuFile.add(mMenuItemNewArmy);
			mMenuFile.add(mMenuItemExit);
			mPopupMenu.add(mMenuItemChangeGroupOfArmy);
			mPopupMenu.add(mMenuItemEditArmy);
			mPopupMenu.add(mMenuItemCopyArmy);
			mPopupMenu.add(mMenuItemDeleteArmy);
			
			// set up index tab
			mIndexTableModel.setColumnIdentifiers(TABLE_COLUMNS);
			mIndexTable.setAutoCreateRowSorter(true);
			mIndexTable.addMouseListener(new MouseAdapter() {
				@Override
				public void mousePressed(MouseEvent me) {
					if (SwingUtilities.isLeftMouseButton(me)) {
						if (me.getClickCount() == 2) {
							editArmy();
						}
					} else if (SwingUtilities.isRightMouseButton(me)) {
						int row = mIndexTable.rowAtPoint(me.getPoint());
						log.info("Index row selected was {}", row);
						if (row >= 0) {
							mIndexTable.setRowSelectionInterval(row, row);
							mPopupMenu.show(mIndexTable, me.getPoint().x, me.getPoint().y);
						}
					}
				}
			});
			JScrollPane indexSp = new JScrollPane(mIndexTable);
			pnlMain.setName("Index");
			mIndexTable.setFillsViewportHeight(true);
			pnlMain.add(indexSp,BorderLayout.CENTER);
			mTabPane.add(pnlMain);

			// The WindowListener we are adding need to happen first because the WFrame version does a system.Exit.
			// remove any current, add this one here, add back previous one (just the WFrame one).
			WindowListener[] wls = mFrame.getWindowListeners();
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
					if (mChanged) {
						try {
							saveIndex();
						}
						catch (Exception e) {
							log.warn("Error when saving index", e);
							errorMessage("Error when saving index: "+e);
						}
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

		//----------------------------------------------------------------------
		private void newArmy() {
			ArmyListDBMModel model = new ArmyListDBMModel();
			ArmyListDBMEditorSwing ed;
			try {
				ed = new ArmyListDBMEditorSwing(model);
			} catch (ParserConfigurationException | SAXException | IOException e1) {
				String txt = "Error when trying to create army editor for a new army";
				log.warn(txt , e1);
				errorMessage(txt);
				return;
			}
			ed.setIndexChanges(ArmyListDBMSwing.this);
			Component tab = mTabPane.add(ed.getJPanel());
			String armyId = model.getArmyId();
			tab.setName(armyId);
			int tabIndex = getTabIndex(armyId);
			mTabPane.setTitleAt(tabIndex, "");
			mIndexTableModel.addRow(new Object[] {"", "", "", "", ""});
			mIndexTableModel.addHiddenValue(armyId);
			log.info("Table model row count is {}, hidden row count is {}", mIndexTableModel.getRowCount(), mIndexTableModel.getHiddenRowCount());
			mArmyListIndex.addEntry(armyId, "", "", "", "", "");
			int tabCount = mTabPane.getTabCount();
			mTabPane.setSelectedIndex(tabCount-1);
		}

		//----------------------------------------------------------------------
		private void editArmy() {
			String armyId = getSelectedArmyId();
			if (armyId == null) {
				errorMessage("Army not selected");
			}
			else {
				ArmyListDBMModel armyList = new ArmyListDBMModel();
				try {
					armyList.loadFromFile(mDataDir, armyId);
				}
				catch (NoSuchFileException nsfe) {
					errorMessage("The army could not be found");
					return;
				}
				catch (IOException e1) {
					errorMessage("Could not load army");
					return;
				}
				int tabIndex = getTabIndex(armyId);
				if (tabIndex == -1) {
					ArmyListDBMEditorSwing ed;
					try {
						ed = new ArmyListDBMEditorSwing(armyList);
					} catch (ParserConfigurationException | SAXException | IOException e1) {
						String txt = "Error when trying to create army editor";
						log.warn(txt , e1);
						errorMessage(txt);
						return;
					}
					ed.setIndexChanges(ArmyListDBMSwing.this);
					Component tab = mTabPane.add(ed.getJPanel());
					tab.setName(armyId);
					int tabCount = mTabPane.getTabCount();
					String armyName = armyList.getArmyName();
					mTabPane.setTitleAt(tabCount-1, armyName);
					mTabPane.setSelectedIndex(tabCount-1);
				}
				else {
					mTabPane.setSelectedIndex(tabIndex);	// else - just switch to that tab
				}
			}
		}

		//--------------------------------------------------------------------------
		private void loadIndex() {
			mArmyListIndex.loadFromFile(mDataDir);
			String[] ids = mArmyListIndex.getEntryIDs();
			for (int ii=0; ii< ids.length; ii++) {
				String id = ids[ii];
				String book = mArmyListIndex.getBook(id);
				String group = mArmyListIndex.getGroupName(id);
				if (group == null) {
					group = "";
				}
				String name = mArmyListIndex.getName(id);
				String points = mArmyListIndex.getPoints(id);
				String year = mArmyListIndex.getYear(id);
				mIndexTableModel.addRow(new Object[] {group, name, book, year, points});
				mIndexTableModel.addHiddenValue(id);
			}
		}

		//--------------------------------------------------------------------------
		/** Get the ID of the army selected, probably used during right mouse 
		 * button processing.
		 * @return the army Id or null if none selected. */
		private String getSelectedArmyId() {
			int row = mIndexTable.getSelectedRow();
			log.info("Selected row on screen is {}", row);
			if (row >= 0) {
				mSelectedRow = mIndexTable.convertRowIndexToModel(row);
				log.info("Selected row in table is {}", mSelectedRow);
				String armyId = (String)mIndexTableModel.getHiddenValue(mSelectedRow);
				log.info("Selected Army ID is {}", armyId);
				return(armyId);
			}
			return(null);
		}
	}

	//--------------------------------------------------------------------------
	private void saveIndex() throws IOException, XMLStreamException {
		mArmyListIndex.saveToFile(mDataDir);
		writeStatus("Index saved");
		mChanged = false;
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

	    public int getHiddenRowCount() {
	    	return(mHidden.size());
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

	//--------------------------------------------------------------------------
	private void writeStatus(String msg) {
		mStatusLine.setText(msg);
	}

	//--------------------------------------------------------------------------
	private void errorMessage(String msg) {
		JOptionPane.showMessageDialog(mFrame.getContentPane(),msg);
		writeStatus(msg);
	}

	//--------------------------------------------------------------------------
	private int getTabIndex(String id) {
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
	private int getIndexRow(String armyId) {
		int rowCount = mIndexTableModel.getRowCount();
		for (int ii = 0; ii < rowCount; ii++) {
			String id = (String)(mIndexTableModel.getHiddenValue(ii));
			if (id.equals(armyId)) {
				return(ii);
			}
		}
		return(-1);
	}

	//--------------------------------------------------------------------------
	@Override
	public void change(String armyId, ArmyListConstants field, String value) {
		int indexRow = getIndexRow(armyId);
		switch (field) {
			case ARMY_NAME :
				mIndexTableModel.setValueAt(value, indexRow, 1);
				int tabIndex = getTabIndex(armyId);
				mTabPane.setTitleAt(tabIndex, value);
				mArmyListIndex.updateEntryName(armyId, value);
				mChanged = true;
				break;
			case ARMY_BOOK :
				mIndexTableModel.setValueAt(value, indexRow, 2);
				mArmyListIndex.updateEntryBook(armyId, value);
				mChanged = true;
				break;
			case ARMY_YEAR :
				mIndexTableModel.setValueAt(value, indexRow, 3);
				mArmyListIndex.updateEntryYear(armyId, value);
				mChanged = true;
				break;
			case ARMY_POINTS :
				mIndexTableModel.setValueAt(value, indexRow, 4);
				mArmyListIndex.updateEntryPoints(armyId, value);
				mChanged = true;
				break;
			case CLOSE :
				tabIndex = getTabIndex(armyId);
				mTabPane.remove(tabIndex);
				break;
			default	:
				log.warn("Unknown index field {}", field);
		}
	}
}
