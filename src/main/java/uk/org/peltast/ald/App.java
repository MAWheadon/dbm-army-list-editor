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
}
