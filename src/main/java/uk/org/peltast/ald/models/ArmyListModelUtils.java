/*-------------------------------------------------------------------------------
01/05/2021 MAW Created.
-------------------------------------------------------------------------------*/

package uk.org.peltast.ald.models;

import net.harawata.appdirs.AppDirs;
import net.harawata.appdirs.AppDirsFactory;

public class ArmyListModelUtils {

	//--------------------------------------------------------------------------
	private ArmyListModelUtils() {
		throw new IllegalStateException("Utility class");
	}

	//--------------------------------------------------------------------------
	public static String getDataPath() {
		AppDirs appDirs = AppDirsFactory.getInstance();
		String dataDir = appDirs.getUserDataDir("DBMArmyListDesigner", "1.0", "peltast.org.uk");
		return(dataDir);
	}
}
