package uk.org.peltast.ald.models;

public interface ArmyListModelChange {
	/** Add a row. */
	public void addRow();

	/** Insert a row after the given row.
	 * @param afterRow the row to insert after. To insert before the first row use a value of -1. */
	public void insertRow(int afterRow);

	/** Delete the given row.
	 * @param row the zero based row number. */
	public void deleteRow(int row);

	/** Clear the whole army list. */
	public void clear();

	/** Sets a value in a row
	 * @param field The field to change.
	 * @param value The new value. */
	public void setField(ArmyListConstants field, String value);

	/** Sets a value in a row
	 * @param field The field to change.
	 * @param row the zero based row number the field is in.
	 * @param value The new value. */
	public void setRowField(ArmyListConstants field, int row, String value);
}
