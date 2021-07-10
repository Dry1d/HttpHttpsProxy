/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package HttpHttpsProxy;

import java.io.File;
import org.codehaus.plexus.archiver.tar.TarGZipUnArchiver;
import org.codehaus.plexus.logging.console.ConsoleLoggerManager;
import static HttpHttpsProxy.HttpHttpsProxy.fs;

/**
 *
 * @author Dry1d
 */
public class Archive {
    public static void untar(File tarbal) {
        final TarGZipUnArchiver ua = new TarGZipUnArchiver();
// Logging - as @Akom noted, logging is mandatory in newer versions, so you can use a code like this to configure it:
        ConsoleLoggerManager manager = new ConsoleLoggerManager();
        manager.initialize();
        ua.enableLogging(manager.getLoggerForComponent("bla"));
// -- end of logging part
        ua.setSourceFile(tarbal);

        File destDir = new File(tarbal.getParentFile().getAbsolutePath() + fs + "tarball");

        if (destDir.exists()) {
            destDir.delete();
        }
        destDir.mkdirs();
        ua.setDestDirectory(destDir);
        ua.extract();
    }
}
