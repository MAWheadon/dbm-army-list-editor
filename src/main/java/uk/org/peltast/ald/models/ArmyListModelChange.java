package uk.org.peltast.ald.models;

public interface ArmyListModelChange {
	public enum ChangeType {ADD, CHANGE, DELETE}
	public enum ChangeObject {CELL, ROW}
	public enum ChangeItem {BOOK, COSTFILE, DESCRIPTION, YEAR,
		QTY, DESC, DRILL, TYPE, GRADE, ADJ, COST, TOTAL, CMD1QTY, CMD2QTY, CMD3QTY, CMD4QTY,UNUSED,
		EL_COUNT, EL_EQUIV, HALF_ARMY, ARMY_TOTAL,
		COST_CMD1, COST_CMD2, COST_CMD3, COST_CMD4, 
		EL_COUNT_CMD1, EL_COUNT_CMD2, EL_COUNT_CMD3, EL_COUNT_CMD4,
		EQUIV_CMD1, EQUIV_CMD2, EQUIV_CMD3, EQUIV_CMD4,
		BP_CMD1, BP_CMD2, BP_CMD3, BP_CMD4}
	public void change(ChangeType type, ChangeObject obj, ChangeItem item, String value);
	public void change(ChangeType type, ChangeObject obj, ChangeItem item, String[] values);
}
