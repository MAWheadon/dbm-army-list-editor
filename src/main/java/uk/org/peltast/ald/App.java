package uk.org.peltast.ald;

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.org.peltast.ald.views.ArmyListDBMSwing;

/**
 * Hello world!
 *
 */
public class App {
	private static final Logger log = LoggerFactory.getLogger(ArmyListDBMSwing.class);

    //--------------------------------------------------------------------------
	public static void main( String[] args )
    {
        log.info("About to start ALD");
        ArmyListDBMSwing app = new ArmyListDBMSwing();
        app.start();
        log.info("Done ALD");
    }

    //--------------------------------------------------------------------------
    public static String getDataDirectory() {
    	String osName = System.getProperty("os.name").toLowerCase();
    	log.info("OS name is {}", (osName));
    	String parent = null;
    	if (osName.startsWith("win")) {
    		parent = System.getenv("APPDATA");
    	}
    	else if (osName.contains("nix") || osName.contains("nux") || osName.contains("aix")) {
    		parent = System.getenv("XDG_DATA_HOME");
    	}
    	final String dir;
    	if (parent == null) {
    		parent = System.getProperty("user.home");
        	dir = parent + File.separator + ".ald";
    	}
    	else {
        	dir = parent + File.separator + "ald";
    	}
    	log.info("Data directory is {}", dir);
    	return(dir);
    }

    //--------------------------------------------------------------------------
    public static String getArmyListPath(String armyId) {
		String path = getDataDirectory() + File.separator + "army_list_" + armyId + ".xml";
		log.info("Path is {}" ,path);
		return(path);
    }
}
