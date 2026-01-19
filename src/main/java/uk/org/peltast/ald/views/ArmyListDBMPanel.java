package uk.org.peltast.ald.views;

import java.awt.BorderLayout;
import java.awt.Component;

import javax.swing.JOptionPane;
import javax.swing.JPanel;

/** Utility class for the outer application panel
*
* @author MA Wheadon
* @copyright MA Wheadon, 2026.
* @licence MIT License.
*/
public class ArmyListDBMPanel extends JPanel {
	private final ArmyListDBMEditorSwing mEditor;
	enum Choice {YES, NO, CANCEL}

	//--------------------------------------------------------------------------
	public ArmyListDBMPanel(ArmyListDBMEditorSwing editor) {
		mEditor = editor;
		JPanel pnl = mEditor.getJPanel();
		setLayout(new BorderLayout());
		add(pnl, BorderLayout.CENTER);
	}

	//--------------------------------------------------------------------------
	ArmyListDBMEditorSwing getEditor() {
		return(mEditor);
	}

	//--------------------------------------------------------------------------
	static Choice confirmMessage(Component parent, String msg) {
		int result = JOptionPane.showConfirmDialog(parent, msg);
		if (result == JOptionPane.YES_OPTION) {
			return(Choice.YES);
		}
		if (result == JOptionPane.NO_OPTION) {
			return(Choice.NO);
		}
		return(Choice.CANCEL);
	}

	//--------------------------------------------------------------------------
	static void errorMessage(Component parent, String msg) {
		JOptionPane.showMessageDialog(parent, msg);
	}
}
