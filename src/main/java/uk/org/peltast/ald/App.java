/*------------------------------------------------------------------------------
13/01/2026 MAW Improved logging.
------------------------------------------------------------------------------*/
package uk.org.peltast.ald;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.harawata.appdirs.AppDirs;
import net.harawata.appdirs.AppDirsFactory;
import uk.org.peltast.ald.views.ArmyListDBMSwing;

/**
 * The entry point for the Army List Designer.
 * @author MA Wheadon
 * @date 26th June 2012.
 * @copyright MA Wheadon, 2019,2026.
 * @licence MIT License.
 */
public class App {

    //--------------------------------------------------------------------------
	public static void main( String[] args ) {
		// We MUST set the system property for the log directory BEFORE any logging, even getting the logger
		AppDirs appDirs = AppDirsFactory.getInstance();
		String logPath = appDirs.getUserLogDir("ArmyListDesigner", null, "peltast.org.uk");	// don't want to include the version
		System.setProperty("logs_path", logPath);
		// no logging before here
		final Logger log = LoggerFactory.getLogger(App.class);
		log.info("About to start ALD. The log path is {}", logPath);
		ArmyListDBMSwing app = new ArmyListDBMSwing();
        app.start();
        log.info("ALD started ...");
    }
}
