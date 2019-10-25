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

/** A helper Swing class that creates a JFrame and remembers where it is on 
 * the desktop.
 * 
 * @author Mark Andrew Wheadon
 * @date 16th October 2013.
 * @copyright Mark Andrew Wheadon, 2013,2014.
 * @licence MIT License.
 */
public class WFrame extends JFrame {
	private static final long serialVersionUID = 1L;
	private File m_settings_file;
	private boolean m_settings_file_exists;
	private String m_dir;
	private File m_dir_file;
	private JMenuBar m_menu_bar;

	//----------------------------------------------------------------------
	public WFrame(String title) {
		super(title);
		m_dir = title.replace(' ', '_');		
		String path = System.getProperty("user.home");
		path = path + File.separator + m_dir + File.separator + m_dir + ".properties";
		m_settings_file = new File(path);
		m_dir_file = m_settings_file.getParentFile();
		m_dir_file.mkdirs();
		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent we) {
				Rectangle rect = getBounds();
				Properties props = new Properties();
				props.setProperty(WindowState.WINDOW_STATE_LEFT.name(),String.valueOf(rect.x));
				props.setProperty(WindowState.WINDOW_STATE_TOP.name(),String.valueOf(rect.y));
				props.setProperty(WindowState.WINDOW_STATE_WIDTH.name(),String.valueOf(rect.width));
				props.setProperty(WindowState.WINDOW_STATE_HEIGHT.name(),String.valueOf(rect.height));
				int window_state = getExtendedState();
				if ((window_state & Frame.MAXIMIZED_BOTH) == Frame.MAXIMIZED_BOTH) {
					props.setProperty(WindowState.WINDOW_STATE_MAXIMISED.name(),"true");
				}	// if
				else {
					props.setProperty(WindowState.WINDOW_STATE_MAXIMISED.name(),"false");
				}	// else
				try {
					FileOutputStream fos = new FileOutputStream(m_settings_file);
					props.store(fos,"");
					System.exit(0);
				}	// try
				catch (Exception e) {
					System.out.println("Error:" +e);
				}	// catch
			}	// windowClosing
		});

		Properties props = new Properties();
		try {
			FileInputStream fis = new FileInputStream(m_settings_file);
			m_settings_file_exists = m_settings_file.exists();
			if (m_settings_file_exists) {
				props.load(fis);
				int left = Integer.parseInt(props.getProperty(WindowState.WINDOW_STATE_LEFT.name()));
				int top = Integer.parseInt(props.getProperty(WindowState.WINDOW_STATE_TOP.name()));
				int width = Integer.parseInt(props.getProperty(WindowState.WINDOW_STATE_WIDTH.name()));
				int height = Integer.parseInt(props.getProperty(WindowState.WINDOW_STATE_HEIGHT.name()));
				boolean max = Boolean.parseBoolean(props.getProperty(WindowState.WINDOW_STATE_MAXIMISED.name()));
				setBounds(left,top,width,height);
				int window_state = getExtendedState();
				if (max) {
					window_state |= Frame.MAXIMIZED_BOTH;
				}	// if
				setExtendedState(window_state);
			}	// if
		}	// try
		catch (Exception e) {
			System.out.println("Error:" +e);
		}	// catch
	}	// WFrame

	//----------------------------------------------------------------------
	@Override
	public void setVisible(boolean b) {
		if (m_settings_file_exists == false) {
			pack();
		}	// if
		super.setVisible(b);
	}	// setVisible

	//--------------------------------------------------------------------------
	public File getDirectory() {
		return(m_dir_file);
	}	// getDirectory

	//--------------------------------------------------------------------------
	public void addMenu(String name, ActionListener action_listener, String ... items) {
		if (m_menu_bar == null) {
			m_menu_bar = new JMenuBar();
			setJMenuBar(m_menu_bar);
		}	// if
		JMenu menu = new JMenu(name);
		for (String item : items) {
			if (item.equals("-")) {
				menu.add(new JSeparator());
			}	// if
			else {
				JMenuItem mi = new JMenuItem(item);
				menu.add(mi);
				mi.addActionListener(action_listener);
			}	// else
		}	// for
		m_menu_bar.add(menu);
	}	// addMenu

	//--------------------------------------------------------------------------
	public void enableMenuItem(String txt, boolean sw) {
		int nbr_menus = m_menu_bar.getComponentCount();
		for (int mm=0; mm<nbr_menus; mm++) {
			JMenu menu = (JMenu)m_menu_bar.getComponent(mm);
			int nbr_menu_items = menu.getMenuComponentCount();
			for (int mi=0; mi<nbr_menu_items; mi++) {
				Component comp = menu.getMenuComponent(mi);
				if (comp instanceof JMenuItem == false) {
					continue;
				}	// if - a separator or something
				JMenuItem menu_item = (JMenuItem)comp;
				String text = menu_item.getText();
				if (text.equals(txt)) {
					menu_item.setEnabled(sw);
					return;
				}	// if - the menu item we are looking for
			}	// for - each menu item
		}	// for - each menu
	}	// enableMenuItem

	//--------------------------------------------------------------------------
	private enum WindowState {
		WINDOW_STATE_LEFT,
		WINDOW_STATE_TOP,
		WINDOW_STATE_WIDTH,
		WINDOW_STATE_HEIGHT,
		WINDOW_STATE_MAXIMISED;
	}	// WindowState
}	// WFrame
