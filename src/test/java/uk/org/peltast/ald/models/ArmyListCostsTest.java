package uk.org.peltast.ald.models;

import static org.junit.jupiter.api.Assertions.*;

import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class ArmyListCostsTest {
	private static final Logger log = LoggerFactory.getLogger(ArmyListCostsTest.class);
	private static ArmyListCosts costs;

	//--------------------------------------------------------------------------
	@BeforeAll
	static void setUpBeforeClass() throws Exception {
		try {
			costs = new ArmyListCosts(new ArmyListVersion(3, 3));
		}
		catch (Exception e) {
			log.warn("Error", e);
		}
	}

	//--------------------------------------------------------------------------
	@Test
	final void testGetDrillList() {
		List<String> drills = costs.getDrillList();
		assertEquals(3, drills.size(), "There should be 3 drills");
		log.info("Drills: {}", drills);
	}

	//--------------------------------------------------------------------------
	@Test
	final void testGetTypes() {
		List<String> regTypes = costs.getTypes("Reg");
		assertEquals(13, regTypes.size(), "There should be 13 Reg types");
		log.info("Reg types: {}", Arrays.asList(regTypes));

		List<String> irrTypes = costs.getTypes("Irr");
		assertEquals(17, irrTypes.size(), "There should be 17 Irr types");
		log.info("Irr types: {}", Arrays.asList(irrTypes));

		List<String> fortTypes = costs.getTypes("Fort");
		assertEquals(2, fortTypes.size(), "There should be 2 Fort types");
		log.info("Fort types: {}", Arrays.asList(fortTypes));
	}

	//--------------------------------------------------------------------------
	@Test
	final void testGetTroopGradeList() {
		// Just a few examples
		List<String> regKnGrades = costs.getTroopGradeList("Reg", "Kn");
		assertEquals(5, regKnGrades.size(), "There should be 5 grades of Reg Kn");
		log.info("Reg Kn grades: {}", Arrays.asList(regKnGrades));

		List<String> irrAxGrades = costs.getTroopGradeList("Irr", "Ax");
		assertEquals(4, irrAxGrades.size(), "There should be 4 grades of Irr Ax");
		log.info("Irr Ax grades: {}", Arrays.asList(irrAxGrades));

		List<String> fortPFGrades = costs.getTroopGradeList("Fort", "PF");
		assertEquals(1, fortPFGrades.size(), "There should be 1 grade of Fort PF");
		log.info("Fort PF grades: {}", Arrays.asList(fortPFGrades));
	}

	//--------------------------------------------------------------------------
	@Test
	final void testGetAdjustmentText() {
		// A few examples
		String txt = costs.getAdjustmentText("Reg", "rr");
		assertNotNull(txt);
		log.info("Reg rr adjustment text is {}", txt);

		txt = costs.getAdjustmentText("Reg", "mtdgen1");
		assertNotNull(txt);
		log.info("Mounted general sole adjustment text is {}", txt);

		txt = costs.getAdjustmentText("Irr", "chally");
		assertNotNull(txt);
		log.info("Irr ally general adjustment text is {}", txt);

		txt = costs.getAdjustmentText("Irr", null);
		assertEquals("", txt);
		log.info("Irr without adjustment text is {}", txt);
	}

	//--------------------------------------------------------------------------
	@Test
	final void testGetAdjustmentCost() {
		// A few examples
		float adj = costs.getAdjustmentCost("Reg", "chgen");
		assertNotEquals(0, adj);
		log.info("Reg chariot general adjustment cost is {}", adj);

		adj = costs.getAdjustmentCost("Irr", "mtdally");
		assertNotEquals(0, adj);
		log.info("Irr mounted ally general adjustment cost is {}", adj);

		adj = costs.getAdjustmentCost("Fort", "camp");
		assertNotEquals(0, adj);
		log.info("Fort camp adjustment cost is {}", adj);

		adj = costs.getAdjustmentCost("Reg", "");
		assertEquals(0, adj);
		log.info("Reg without adjustment is {}", adj);
	}

	//--------------------------------------------------------------------------
	@Test
	final void testGetTroopCost() {
		// A few examples
		float cost = costs.getTroopCost("Reg", "Bd", "O");
		assertTrue(cost >= 0.5);
		log.info("Reg blade O cost is {}", cost);

		cost = costs.getTroopCost("Irr", "Exp", "O");
		assertTrue(cost > 5);
		log.info("Irr Exp O cost is {}", cost);

		cost = costs.getTroopCost("Fort", "TF", "n/a");
		assertTrue(cost >= 0.5);
		log.info("Fort TF cost is {}", cost);
	}

	//--------------------------------------------------------------------------
	@Test
	final void testGetTroopEquivalents() {
		// A few examples
		float eq = costs.getTroopEquivalents("Reg", "Cv", "I");
		assertTrue(eq >= 0.5);
		log.info("Reg Cv I element equivalents is {}", eq);

		eq = costs.getTroopEquivalents("Irr", "Ps", "S");
		assertTrue(eq >= 0.5);
		log.info("Irr Ps S element equivalents is {}", eq);

		eq = costs.getTroopEquivalents("Fort", "PF", "n/a");
		assertTrue(eq >= 0);
		log.info("Fort PF element equivalents is {}", eq);
	}

	//--------------------------------------------------------------------------
	@Test
	final void testGetLineCost() {
		// A few examples
		float cost = costs.getLineCost("Reg", "Pk", "I", "", 24);
		assertEquals(72, cost);
		log.info("Reg Pk I x 24 costs {} pts", cost);

		cost = costs.getLineCost("Irr", "Hd", "I", "", 7);
		assertEquals(3.5f, cost);
		log.info("Irr Hd I x 7 costs {} pts", cost);

		cost = costs.getLineCost("Fort", "PF", "n/a", "gtwy", 1);
		assertEquals(4, cost);
		log.info("Fort gateway x 1 costs {} pts", cost);
	}

	//--------------------------------------------------------------------------
	@Test
	final void testlistAvailableVersions() throws URISyntaxException {
		List<ArmyListVersion> vers = costs.listAvailableVersions();
		assertTrue(vers.size() > 0);
		log.info("The available costs versions are {}", vers);
	}

}
