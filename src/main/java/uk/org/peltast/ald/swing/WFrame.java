/*------------------------------------------------------------------------------
13/01/2026 MAW Saves and restores the window position. Now uses OS dependent data and log paths.
------------------------------------------------------------------------------*/
package uk.org.peltast.ald.swing;

import java.awt.Component;
import java.awt.Frame;
import java.awt.Rectangle;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Properties;

import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JSeparator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.org.peltast.ald.models.ArmyListModelUtils;

/** A helper Swing class that creates a JFrame and remembers where it is on 
 * the desktop.
 * 
 * @author MA Wheadon
 * @date 16th October 2013.
 * @copyright MA Wheadon, 2013,2025.
 * @licence MIT License.
 */
public class WFrame extends JFrame {
	private static final Logger log = LoggerFactory.getLogger(WFrame.class);
	private static final long serialVersionUID = 1L;
	private boolean mSettingsFileExists;
	private File mDirFile;
	private File mSettingsFile;
	private JMenuBar mMenuBar;

	//----------------------------------------------------------------------
	public WFrame(String title) {
		super(title);
		final String dir = ArmyListModelUtils.getDataPath();
		final String path = dir + File.separator + "settings.properties";
		log.info("Settings file is {}", path);
		mSettingsFile = new File(path);
		mDirFile = mSettingsFile.getParentFile();
		mDirFile.mkdirs();
		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);	// Important
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent we) {
				//saveFramePosition();
			}
		});

		Properties props = new Properties();
		final boolean exists = mSettingsFile.exists();
		if (exists) {
			try (FileInputStream fis = new FileInputStream(mSettingsFile)) {
				mSettingsFileExists = mSettingsFile.exists();
				if (mSettingsFileExists) {
					props.load(fis);
					int left = Integer.parseInt(props.getProperty(WindowState.WINDOW_STATE_LEFT.name()));
					int top = Integer.parseInt(props.getProperty(WindowState.WINDOW_STATE_TOP.name()));
					int width = Integer.parseInt(props.getProperty(WindowState.WINDOW_STATE_WIDTH.name()));
					int height = Integer.parseInt(props.getProperty(WindowState.WINDOW_STATE_HEIGHT.name()));
					boolean max = Boolean.parseBoolean(props.getProperty(WindowState.WINDOW_STATE_MAXIMISED.name()));
					setBounds(left,top,width,height);
					int windowState = getExtendedState();
					if (max) {
						windowState |= Frame.MAXIMIZED_BOTH;
					}
					setExtendedState(windowState);
				}
			}
			catch (Exception e) {
				log.warn("Error", e);
			}
		} else {
			log.info("Settings file does not exist: {}", path);
		}
	}

	//----------------------------------------------------------------------
	public void saveFramePosition() {
		Rectangle rect = getBounds();
		log.info("Saving window position {}, rect");
		Properties props = new Properties();
		props.setProperty(WindowState.WINDOW_STATE_LEFT.name(),String.valueOf(rect.x));
		props.setProperty(WindowState.WINDOW_STATE_TOP.name(),String.valueOf(rect.y));
		props.setProperty(WindowState.WINDOW_STATE_WIDTH.name(),String.valueOf(rect.width));
		props.setProperty(WindowState.WINDOW_STATE_HEIGHT.name(),String.valueOf(rect.height));
		int windowState = getExtendedState();
		if ((windowState & Frame.MAXIMIZED_BOTH) == Frame.MAXIMIZED_BOTH) {
			props.setProperty(WindowState.WINDOW_STATE_MAXIMISED.name(),"true");
		}
		else {
			props.setProperty(WindowState.WINDOW_STATE_MAXIMISED.name(),"false");
		}
		try (FileOutputStream fos = new FileOutputStream(mSettingsFile)) {
			props.store(fos,"");
			System.exit(0);
		}
		catch (Exception e) {
			log.warn("Error", e);
		}
	}

	//----------------------------------------------------------------------
	@Override
	public void setVisible(boolean b) {
		if (!mSettingsFileExists) {
			pack();
		}
		super.setVisible(b);
	}

	//--------------------------------------------------------------------------
	public void addMenu(String name, ActionListener actionListener, String ... items) {
		if (mMenuBar == null) {
			mMenuBar = new JMenuBar();
			setJMenuBar(mMenuBar);
		}
		JMenu menu = new JMenu(name);
		for (String item : items) {
			if (item.equals("-")) {
				menu.add(new JSeparator());
			}
			else {
				JMenuItem mi = new JMenuItem(item);
				menu.add(mi);
				mi.addActionListener(actionListener);
			}
		}
		mMenuBar.add(menu);
	}

	//--------------------------------------------------------------------------
	public void enableMenuItem(String txt, boolean sw) {
		int nbrMenus = mMenuBar.getComponentCount();
		for (int mm=0; mm<nbrMenus; mm++) {
			JMenu menu = (JMenu)mMenuBar.getComponent(mm);
			int nbrMenuItems = menu.getMenuComponentCount();
			for (int mi=0; mi<nbrMenuItems; mi++) {
				Component comp = menu.getMenuComponent(mi);
				if (!(comp instanceof JMenuItem)) {
					continue;
				}	// if - a separator or something
				JMenuItem menuItem = (JMenuItem)comp;
				String text = menuItem.getText();
				if (text.equals(txt)) {
					menuItem.setEnabled(sw);
					return;
				}	// if - the menu item we are looking for
			}	// for - each menu item
		}	// for - each menu
	}

	//--------------------------------------------------------------------------
	private enum WindowState {
		WINDOW_STATE_LEFT,
		WINDOW_STATE_TOP,
		WINDOW_STATE_WIDTH,
		WINDOW_STATE_HEIGHT,
		WINDOW_STATE_MAXIMISED;
	}	// WindowState
}	// WFrame
