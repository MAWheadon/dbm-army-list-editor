package uk.org.peltast.ald.models;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/** Reads DBM cost files (proprietary format, though in XML) of the form 
 * "costs_dbm_x_y.yaml" where x and y are DBM rules versions (3.2 probably 
 * being the last).
 * Acknowledgements: De Bellis Multitudinis (DBM) is a set of wargames rules 
 * for ancient and medieval battles written by Phil Barker and Richard Bodley 
 * Scott (much appreciated to them both).
 * 
 * @author Mark Andrew Wheadon
 * @date 6th November 2012.
 * @copyright Mark Andrew Wheadon, 2012,2019.
 * @licence MIT License.
 */
class ArmyListCosts {
	private enum NodeNames {costs,drill,adjustments, adjustment,types,type,troop};
	private enum AttributeNames {rules,version,name,text,cost,grade,ap,eq,adjs};
	private Costs mCosts;
	private List<String> mBooks = new ArrayList<>();	// the 4 DBM army list books

	//--------------------------------------------------------------------------
	private class Costs {
		private final String mRules;
		private final String mVersion;
		private final Map<String,Drill> mDrills = new LinkedHashMap<>();	// the whole costs file keyed by drill
		
		Costs(String rules, String version) {
			mRules = rules;
			mVersion = version;
		}
		
		void addDrill(String drillName, Drill drill) {
			mDrills.put(drillName, drill);
		}
		
		//--------------------------------------------------------------------------
		/** Return the drill by drill name. 
		 * Not all drills have all types.
		 * @param drillName Reg, Irr or Fort.
		 * @return A Drill. */
		private Drill getDrill(String drillName) {
			Drill drill = mDrills.get(drillName);
			if (drill == null) {
				throw new IllegalArgumentException("Drill "+drillName+ " not known");
			}
			return(drill);
		}

		//--------------------------------------------------------------------------
		List<String> getDrillList() {
			List<String> list = new ArrayList<>();
			mDrills.forEach((k,v) -> list.add(k));
			return(list);
		}
	}

	//--------------------------------------------------------------------------
	private class Troop {
		private final String mGrade;
		private final float mArmyPoints;
		private final float mEquivalents;
		private final String[] mAdjustments;
		
		Troop(String grade, float armyPoints, float equivalents, String[] adjustments) {
			mGrade = grade;
			mArmyPoints = armyPoints;
			mEquivalents = equivalents;
			mAdjustments = adjustments;
		}

		String getGrade() {
			return(mGrade);
		}
		
		float getArmyPoints() {
			return(mArmyPoints);
		}
		
		float getEquivalents() {
			return(mEquivalents);
		}
		
		String[] getAdjustments() {
			return(mAdjustments);
		}
	}

	//--------------------------------------------------------------------------
	private class Type {
		private final String mName;
		private final List<Troop> mTroops = new ArrayList<>();
		
		Type(String name) {
			mName = name;
		}
		
		void addTroop(Troop troop) {
			mTroops.add(troop);
		}
		
		Troop getTroop(String gradeName) {
			for (Troop troop : mTroops) {
				if (troop.mGrade.equals(gradeName)) {
					return(troop);
				}
			}
			throw new IllegalArgumentException("Unknown grade " + gradeName);
		}
	}

	//--------------------------------------------------------------------------
	private class Adjustment {
		private final String mName;
		private final String mText;
		private final float mCost;
		
		Adjustment(String name, String text, float cost) {
			mName = name;
			mText = text;
			mCost = cost;
		}
		
		String getName() {
			return(mName);
		}
		
		String getText() {
			return(mText);
		}
		
		float getCost() {
			return(mCost);
		}
	}

	//--------------------------------------------------------------------------
	private class Drill {
		private final String mName;
		private final Map<String,Adjustment> mAdjustments = new LinkedHashMap<>();
		private final Map<String,Type> mTypes = new LinkedHashMap<>();
		
		Drill(String name) {
			mName = name;
		}
		
		void addAdjustment(String name, Adjustment adjustment) {
			mAdjustments.put(name, adjustment);
		}
		
		void addType(String name, Type type) {
			mTypes.put(name, type);
		}

		Adjustment getAdjustment(String adjustmentMnemonic) {
			Adjustment adjustment = mAdjustments.get(adjustmentMnemonic);
			if (adjustment == null) {
				throw new IllegalArgumentException("Unknown adjustmenr mnemonic "+ adjustmentMnemonic);
			}
			return(adjustment);
		}

		String getAdjustmentText(String adjustmentMnemonic) {
			Adjustment adjustment = getAdjustment(adjustmentMnemonic);
			String text = adjustment.getText();
			return(text);
		}

		float getAdjustmentCost(String adjustmentMnemonic) {
			Adjustment adjustment = getAdjustment(adjustmentMnemonic);
			float cost = adjustment.getCost();
			return(cost);
		}
		
		Type getType(String typeName) {
			Type type = mTypes.get(typeName);
			if (type == null) {
				throw new IllegalArgumentException("Unknown adjustmenr mnemonic "+ typeName);
			}
			return(type);
		}
	}

	//--------------------------------------------------------------------------
	private class XMLReader extends DefaultHandler {
		private Drill mDrill;
		private Type mType;

		public void startElement(String uri, String localName,String qName, Attributes attributes) throws SAXException {
			System.out.println("Start Element :" + qName);
			NodeNames nodeName = NodeNames.valueOf(qName);
			switch (nodeName) {
				case costs :
					String rules = getAttributeValueAsString(AttributeNames.rules, attributes);
					String version = getAttributeValueAsString(AttributeNames.version, attributes);
					mCosts = new Costs(rules,version);
					break;
				case drill :
					String name = getAttributeValueAsString(AttributeNames.name, attributes);
					mDrill = new Drill(name);
					mCosts.addDrill(name, mDrill);
					break;
				case adjustment :
					name = getAttributeValueAsString(AttributeNames.name, attributes);
					String text = getAttributeValueAsString(AttributeNames.text, attributes);
					float cost = getAttributeValueAsFloat(AttributeNames.cost, attributes);
					Adjustment adjustment = new Adjustment(name, text, cost);
					mDrill.addAdjustment(name, adjustment);
					break;
				case type :
					name = getAttributeValueAsString(AttributeNames.name, attributes);
					mType = new Type(name);
					mDrill.addType(name, mType);
					break;
				case troop :
					String grade = getAttributeValueAsString(AttributeNames.grade, attributes);
					Float ap = getAttributeValueAsFloat(AttributeNames.ap, attributes);
					Float eq = getAttributeValueAsFloat(AttributeNames.eq, attributes);
					String adjsCsv = getAttributeValueAsString(AttributeNames.adjs, attributes);
					String[] adjs = new String[] {};
					if (adjsCsv != null) {
						adjs = adjsCsv.split("\\s*,\\s*");
					}
					Troop troop = new Troop(grade, ap, eq, adjs);
					mType.addTroop(troop);
					break;
			}
		}

		private String getAttributeValueAsString(AttributeNames aName, Attributes attributes) {
			String name = aName.toString();
			String value = attributes.getValue(name);
			return(value);
		}

		private Float getAttributeValueAsFloat(AttributeNames aName, Attributes attributes) {
			String name = aName.toString();
			String value = attributes.getValue(name);
			Float flt = Float.parseFloat(value);
			return(flt);
		}
	}

	//--------------------------------------------------------------------------
	ArmyListCosts(String costFileName) throws IOException {
		loadCostFile(costFileName);
	}

	//--------------------------------------------------------------------------
	void loadCostFile(String costFileName) {
		try {
			SAXParserFactory factory = SAXParserFactory.newInstance();
			SAXParser saxParser = factory.newSAXParser();
			XMLReader xmlReader = new XMLReader();
			File inputFile = new File(costFileName);
			saxParser.parse(costFileName, xmlReader);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	//--------------------------------------------------------------------------
	List<String> getBooks() {
		return(mBooks);
	}

	//--------------------------------------------------------------------------
	List<String> getDrillList() {
		List<String> list = mCosts.getDrillList();
		return(list);
	}

	//--------------------------------------------------------------------------
	/** Return the types (e.g. Kn, Lh etc.) for the given drill (Reg, Irr, Fort). 
	 * Not all drills have all types.
	 * @param drillName Reg, Irr or Fort.
	 * @return A list of the available types. */
	String[] getTypes(String drillName) {
		Drill drill = mCosts.getDrill(drillName);
		Set<String> typeNames = drill.mTypes.keySet();
		String[] typeNamesArray = typeNames.toArray(new String[0]);
		Arrays.sort(typeNamesArray);
		return(typeNamesArray);
	}

	//--------------------------------------------------------------------------
	/** Return the grades (e.g. S, O, I, F, X) for the given drill (Reg, Irr, 
	 * Fort) and type (e.g. kn, LH etc.). Not all types have all grades.
	 * @param drillName Reg, Irr or Fort.
	 * @param typeName Kn, Lh, etc.
	 * @return A list of the available grades. */
	String[] getTroopGradeList(String drillName, String typeName) {
		Drill drill = mCosts.getDrill(drillName);
		Type type = drill.getType(typeName);
		List<Troop> troops = type.mTroops;
		String[] gradeList = new String[troops.size()];
		int ii = 0;
		for (Troop troop : troops) {
			String grade = troop.mGrade;
			gradeList[ii++] = grade;
		}
		return(gradeList);
	}

	//--------------------------------------------------------------------------
	/** Gets the list of adjustments as mnemonics for a specific troop type.
	 * @param drillName Reg, Irr, Fort
	 * @param typeName
	 * @param gradeName
	 * @return The list of mnemonics. */
	private String[] getTroopAdjustmentList(String drillName, String typeName, String gradeName) {
		Drill drill = mCosts.getDrill(drillName);
		Type type = drill.getType(typeName);
		Troop troop = type.getTroop(gradeName);
		String[] list = troop.getAdjustments();
		return(list);
	}

	//--------------------------------------------------------------------------
	/** Gets the name/text of the adjustment, e.g. General, Chariots
	 * @param drillName Reg, Irr, Fort
	 * @param adjustmentMnemonic E.g ch, gen.
	 * @return The adjustment mnemonic text, e.g. Chariots. */
	String getAdjustmentText(String drillName, String adjustmentMnemonic) {
		Drill drill = mCosts.getDrill(drillName);
		String text = drill.getAdjustmentText(adjustmentMnemonic);
		return(text);
	}

	//--------------------------------------------------------------------------
	/** Gets the cost adjustment.
	 * @param drillName Reg, Irr, Fort
	 * @param adjustmentMnemonic E.g ch, gen.
	 * @return The adjustment cost. */
	float getAdjustmentCost(String drillName, String adjustmentMnemonic) {
		Drill drill = mCosts.getDrill(drillName);
		float cost = drill.getAdjustmentCost(adjustmentMnemonic);
		return(cost);
	}

	//--------------------------------------------------------------------------
	float getTroopCost(String drillName, String typeName, String gradeName) {
		Drill drill = mCosts.getDrill(drillName);
		Type type = drill.getType(typeName);
		Troop troop = type.getTroop(gradeName);
		float ap = troop.getArmyPoints();
		return(ap);
	}

	//--------------------------------------------------------------------------
	float getTroopEquivalents(String drillName, String typeName, String gradeName) {
		Drill drill = mCosts.getDrill(drillName);
		Type type = drill.getType(typeName);
		Troop troop = type.getTroop(gradeName);
		float el = troop.getEquivalents();
		return(el);
	}

	//--------------------------------------------------------------------------
	/** A helper method to calculate the cost of some troops.
	 * @param drillName The drill, e.g. Reg, Irr, Fort.
	 * @param typeName The type e.g. Kn, Cv ...
	 * @param gradeName The grade e.g. S, O, F, I, X.
	 * @param AdjustmenName The adjustment mnemonic, ch, gen, ally
	 * @param count The number of those troops.
	 * @return The total cost. */
	float getLineCost(String drillName, String typeName, String gradeName, String adjustmentMnemonic, int nbr) {
		Drill drill = mCosts.getDrill(drillName);
		Type type = drill.getType(typeName);
		Troop troop = type.getTroop(gradeName);
		float costEach = troop.getArmyPoints();
		float adjCost = getAdjustmentCost(drillName, adjustmentMnemonic);
		float troopCost = costEach + adjCost;
		float totalCost = troopCost + nbr;
		return(totalCost);
	}

	//--------------------------------------------------------------------------
	public static void main(String[] args) {
		// some basic tests
		String file_name = "resources/costs/costs_dbm_3_2.xml";
		ArmyListCosts alc = null;
		try {
			alc = new ArmyListCosts(file_name);
		}	// try
		catch (Exception e) {
			System.out.println("Something terrible happened: "+e.toString());
		}	// catch
		List<String> books = alc.getBooks();
		System.out.println("Books: "+books.toString());
		List<String> drills = alc.getDrillList();
		StringBuilder sb = new StringBuilder();
		for (String drill : drills) {	sb.append(drill);	sb.append(", ");	}
		System.out.println("Drills: "+sb.toString());

		String[] types = alc.getTypes("Reg");
		sb.setLength(0);
		for (String type : types) {	sb.append(type);	sb.append(", ");	}
		System.out.println("Types for Reg: "+sb.toString());

		types = alc.getTypes("Irr");
		sb.setLength(0);
		for (String type : types) {	sb.append(type);	sb.append(", ");	}
		System.out.println("Types for Irr: "+sb.toString());

		types = alc.getTypes("Fort");
		sb.setLength(0);
		for (String type : types) {	sb.append(type);	sb.append(", ");	}
		System.out.println("Types for Fort: "+sb.toString());

		String[] grades = alc.getTroopGradeList("Reg","Ax");
		sb.setLength(0);
		for (String grade : grades) {	sb.append(grade);	sb.append(", ");	}
		System.out.println("Grades for Reg Ax: "+sb.toString());

		String[] adjs = alc.getTroopAdjustmentList("Irr","Ax","I");
		sb.setLength(0);
		for (String adj : adjs) {	sb.append(adj);	sb.append(", ");	}
		System.out.println("Adjustments for Irr Ax I: "+sb.toString());

		float el_adj = alc.getAdjustmentCost("Reg", "gen");
		System.out.println("Element adjustments for Reg Kn F general is: "+el_adj);

		float el_cost = alc.getTroopCost("Reg", "Kn", "F");
		System.out.println("Element cost for Reg Kn F is: "+el_cost);

		float el_equiv = alc.getTroopEquivalents("Reg", "Ps", "O");
		System.out.println("Element equivalents for Reg Ps O is: "+el_equiv);
	}
}
