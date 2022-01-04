package uk.org.peltast.ald.models;

public interface ArmyIndexModelChange {
	/** Change one of the entries. 
	 * @param armyId To uniquely identify the army in the index.
	 * @param field One of name, book, year, points.
	 * @param value The new value. */
	public void change(String armyId, ArmyListConstants field, String value);
}
