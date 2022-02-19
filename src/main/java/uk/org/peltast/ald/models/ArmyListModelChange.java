package uk.org.peltast.ald.models;

import java.util.List;
import java.util.Map;

public interface ArmyListModelChange {
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
}
