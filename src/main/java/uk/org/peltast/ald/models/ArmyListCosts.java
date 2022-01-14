package uk.org.peltast.ald.models;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/** Reads DBM cost files (proprietary format, though in XML) of the form 
 * "costs_dbm_x_y.yaml" where x and y are DBM rules versions (3.3 probably 
 * being the last).
 * Acknowledgements: De Bellis Multitudinis (DBM) is a set of wargames rules 
 * for ancient and medieval battles written by Phil Barker and Richard Bodley 
 * Scott (much appreciated to them both).
 * 
 * @author Mark Andrew Wheadon
 * @date 6th November 2012.
 * @copyright Mark Andrew Wheadon, 2012,2020.
 * @licence MIT License.
 */
public class ArmyListCosts {
	private static final Logger log = LoggerFactory.getLogger(ArmyListCosts.class);
	private enum NodeNames {costs, books, book, drill, adjustments, adjustment, types, type, troop};
	private enum AttributeNames {rules, version, name, text, cost, grade, ap, eq, adjs, description};
	private List<String> mBooks = new ArrayList<>();
	private Costs mCosts;

	//--------------------------------------------------------------------------
	/** Internal representation of a costs file. */
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
		/** Return the drill by drill name. Not all drills have all types.
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
	/** Combining grade, cost, element equivalents and any adjustments. Entries
	 * of Troop make up a specific army. */
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
	/** Kn, Cv, El, etc. */
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
	/** Various adjustments might be allowed for a specific troop type such as
	 * being a general, or mounted. Only one adjustment is allowed for a troop
	 * entry. Troop types might be allowed more than one but here they are 
	 * combined so that only one entry is needed, for example a foot ally 
	 * general who is mounted is 'Mounted Ally General'. */
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
	/** Regular, Irregular or Fort. */
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
				throw new IllegalArgumentException("Unknown adjustment mnemonic "+ adjustmentMnemonic);
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

		@Override
		public void startElement(String uri, String localName,String qName, Attributes attributes) throws SAXException {
			NodeNames nodeName = NodeNames.valueOf(qName);
			switch (nodeName) {
				case costs :
					String rules = getAttributeValueAsString(AttributeNames.rules, attributes);
					String version = getAttributeValueAsString(AttributeNames.version, attributes);
					mCosts = new Costs(rules,version);
					break;
				case book :
					String book = getAttributeValueAsString(AttributeNames.description, attributes);
					mBooks.add(book);
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
	ArmyListCosts(ArmyListVersion ver) throws ParserConfigurationException, SAXException, IOException {
		String version = MessageFormat.format("costs_dbm_{0}_{1}.xml", ver.getMajorVerison(), ver.getMinorVerison());
		loadCostFile(version);
	}

	//--------------------------------------------------------------------------
	private void loadCostFile(String costFileName) throws ParserConfigurationException, SAXException, IOException {
		log.info("About to load costs file {}", costFileName);
		SAXParserFactory factory = SAXParserFactory.newInstance();
		SAXParser saxParser = factory.newSAXParser();
		XMLReader xmlReader = new XMLReader();
		try (InputStream is = ArmyListCosts.class.getClassLoader().getResourceAsStream(costFileName)) {
			saxParser.parse(is, xmlReader);
		}
		log.info("Completed loading costs file {}", costFileName);
	}

	//--------------------------------------------------------------------------
	public String[] getBooks() {
		return(mBooks.toArray(new String[0]));
	}

	//--------------------------------------------------------------------------
	static List<ArmyListVersion> listAvailableVersions() {
		String regex1 = "^costs_dbm_\\d_\\d.xml$";
		String regex2 = "\\d";
		Pattern pattern1 = Pattern.compile(regex1);
		Pattern pattern2 = Pattern.compile(regex2);
		TreeSet<ArmyListVersion> vers = new TreeSet<>();
		File dir = new File("src/main/resources");
		String[] names = dir.list();
		log.info("Version files are {}", Arrays.asList(names));
		for (String name : names) {
			Matcher matcher1 = pattern1.matcher(name);
			boolean matches = matcher1.matches();
			if (matches) {
				Matcher matcher2 = pattern2.matcher(name);
				boolean found = matcher2.find();
				if (found) {
					String major = matcher2.group();
					found = matcher2.find();
					if (found) {
						String minor = matcher2.group();
						ArmyListVersion ver = new ArmyListVersion(Integer.parseInt(major), Integer.parseInt(minor));
						vers.add(ver);
					}
				}
			}
		}
		log.info("Available version are {}", vers);
		return(new ArrayList<>(vers));
	}

	//--------------------------------------------------------------------------
	public List<String> getDrillList() {
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
	/** Gets the name/text of the adjustment, e.g. General, Chariots
	 * @param drillName Reg, Irr, Fort
	 * @param adjustmentMnemonic E.g ch, gen.
	 * @return The adjustment mnemonic text, e.g. Chariots. */
	String getAdjustmentText(String drillName, String adjustmentMnemonic) {
		if (adjustmentMnemonic == null || adjustmentMnemonic.isEmpty()) {
			return("");
		}
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
		if (adjustmentMnemonic == null || adjustmentMnemonic.isEmpty()) {
			return(0f);
		}
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
		try {
			Drill drill = mCosts.getDrill(drillName);
			Type type = drill.getType(typeName);
			Troop troop = type.getTroop(gradeName);
			float el = troop.getEquivalents();
			return(el);
		} catch (IllegalArgumentException iae) {
			log.info("Value not set: {}", iae.getMessage());	// some value is not set
			return(0);
		}
	}

	//--------------------------------------------------------------------------
	/** A helper method to calculate the cost of some troops.
	 * @param drillName The drill, e.g. Reg, Irr, Fort.
	 * @param typeName The type e.g. Kn, Cv ...
	 * @param gradeName The grade e.g. S, O, F, I, X.
	 * @param AdjustmenName The adjustment mnemonic, ch, gen, ally
	 * @param count The number of those troops.
	 * @return The total cost, which will be 0 if the line is invalid. */
	float getLineCost(String drillName, String typeName, String gradeName, String adjustmentMnemonic, int nbr) {
		try {
			Drill drill = mCosts.getDrill(drillName);
			Type type = drill.getType(typeName);
			Troop troop = type.getTroop(gradeName);
			float costEach = troop.getArmyPoints();
			float adjCost = getAdjustmentCost(drillName, adjustmentMnemonic);
			float troopCost = costEach + adjCost;
			float totalCost = troopCost * nbr;
			return(totalCost);
		}
		catch (IllegalArgumentException iae) {
			log.info("Value not set: {}", iae.getMessage());	// some value is not set
			return(0);
		}
	}

	//--------------------------------------------------------------------------
	String getRules() {
		return(mCosts.mRules);
	}

	//--------------------------------------------------------------------------
	public String getVersion() {
		return(mCosts.mVersion);
	}
}
