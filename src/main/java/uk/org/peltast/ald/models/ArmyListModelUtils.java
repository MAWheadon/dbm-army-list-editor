/*-------------------------------------------------------------------------------
01/05/2021 MAW Created.
13/01/2026 MAW Added getLogPath(), improved getDataPath().
-------------------------------------------------------------------------------*/

package uk.org.peltast.ald.models;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.harawata.appdirs.AppDirs;
import net.harawata.appdirs.AppDirsFactory;

/** Various utilities.
* @author MA Wheadon
* @copyright MA Wheadon, 2021,2026.
* @licence MIT License.
*/
public class ArmyListModelUtils {
	private static final Logger log = LoggerFactory.getLogger(ArmyListModelUtils.class);

	//--------------------------------------------------------------------------
	private ArmyListModelUtils() {
		throw new IllegalStateException("Utility class");
	}

	//--------------------------------------------------------------------------
	public static String getDataPath() {
		AppDirs appDirs = AppDirsFactory.getInstance();
		String dataDir = appDirs.getUserDataDir("ArmyListDesigner", null, "peltast.org.uk");	// don't want to include the version
		log.info("Data path is {}", dataDir);
		return(dataDir);
	}

	//--------------------------------------------------------------------------
	public static String getLogPath() {
		AppDirs appDirs = AppDirsFactory.getInstance();
		String logPath = appDirs.getUserLogDir("ArmyListDesigner", null, "peltast.org.uk");	// don't want to include the version
		return(logPath);
	}
}
