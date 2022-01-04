/*-------------------------------------------------------------------------------
01/05/2021 MAW Created.
-------------------------------------------------------------------------------*/

package uk.org.peltast.ald.models;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.harawata.appdirs.AppDirs;
import net.harawata.appdirs.AppDirsFactory;
import uk.org.peltast.ald.views.ArmyListDBMSwing;

public class ArmyListModelUtils {
	private static final Logger log = LoggerFactory.getLogger(ArmyListModelUtils.class);

	//--------------------------------------------------------------------------
	private ArmyListModelUtils() {
		throw new IllegalStateException("Utility class");
	}

	//--------------------------------------------------------------------------
	public static String getDataPath() {
		AppDirs appDirs = AppDirsFactory.getInstance();
		String dataDir = appDirs.getUserDataDir("DBMArmyListDesigner", "1.0", "peltast.org.uk");
		log.info("Data path is {}", dataDir);
		return(dataDir);
	}
}
