/*-------------------------------------------------------------------------------
13/01/2026 MAW Tidy up.
-------------------------------------------------------------------------------*/
package uk.org.peltast.ald.models;

import java.util.List;

/** A view class implements this class to reflect changes to the model in the 
 * view. When a user adds or changes a row or value that may cause several 
 * updates in the model (such as totals) and they need to be reflected in 
 * the view. The view does not do any working out, the model does this so the
 * model needs to tell the view all the things that were updated.
 * 
 * @author MA Wheadon
 * @copyright MA Wheadon, 2019,2026.
 * @licence MIT License.
 */
public interface ArmyListModelChange {

	/** changed status */
	public void changed(boolean changed);

	/** Add a row. */
	public void addRow();

	/** Delete the given row.
	 * @param row the zero based row number. */
	public void deleteRow(int row);

	/** Move the given row down one.
	 * @param row the zero based row number. */
	public void moveRowDown(int row);

	/** Move the given row up one.
	 * @param row the zero based row number. */
	public void moveRowUp(int row);

	/** Clear the whole army list. */
	public void clear();

	/** Sets a value in a (non row) field.
	 * @param field The field to change.
	 * @param value The new value. */
	public void setField(ArmyListConstants field, String value);

	/** Sets a value in a row field.
	 * @param field The field to change.
	 * @param row the zero based row number the field is in.
	 * @param value The new value. */
	public void setRowField(ArmyListConstants field, int row, String value);

	/** Sets a value in a row field.
	 * @param field The field to change.
	 * @param row the zero based row number the field is in.
	 * @param nbr The value as a number. */
	public void setRowField(ArmyListConstants field, int row, int nbr);

	/** Sets a value in a row field.
	 * @param field The field to change.
	 * @param row the zero based row number the field is in.
	 * @param nbr The value as a number. */
	public void setRowField(ArmyListConstants field, int row, float nbr);

	/** Sets a list of possible values in a row field.
	 * @param <E>
	 * @param field The field to change.
	 * @param row the zero based row number the field is in.
	 * @param values The new list of possible values.
	 * @param selectedValue The value currently selected. May be null if no value is selected. */
	public <E> void setRowFieldList(ArmyListConstants field, int row, List<E> values, String selectedValue);

	/** Indicates (or not) that the given row applies to the whole army rather than a specific command, e.g. fortifications.
	 * @param row the zero based row number the field is in.
	 * @param armyLevel Does this row apply to the army as a whole or individual commands. */
	public void setArmyLevelRow(int row, boolean armyLevel);
}
