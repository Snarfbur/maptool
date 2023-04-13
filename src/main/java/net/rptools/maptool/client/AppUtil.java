/*
 * This software Copyright by the RPTools.net development team, and
 * licensed under the Affero GPL Version 3 or, at your option, any later
 * version.
 *
 * MapTool Source Code is distributed in the hope that it will be
 * useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * You should have received a copy of the GNU Affero General Public
 * License * along with this source Code.  If not, please visit
 * <http://www.gnu.org/licenses/> and specifically the Affero license
 * text at <http://www.gnu.org/licenses/agpl.html>.
 */
package net.rptools.maptool.client;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.CodeSource;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.prefs.Preferences;
import net.rptools.maptool.client.ui.zone.PlayerView;
import net.rptools.maptool.language.I18N;
import net.rptools.maptool.model.Token;
import net.rptools.maptool.model.Zone;
import net.rptools.maptool.model.player.Player;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.appender.FileAppender;
import org.apache.logging.log4j.core.appender.RollingFileAppender;

/** This class provides utility functions for maptool client. */
public class AppUtil {

  private static final String CLIENT_ID_FILE = "client-id";
  private static final String APP_HOME_CONFIG_FILENAME = "maptool.cfg";

  public static final ScheduledExecutorService fileCheckExecutor =
      new ScheduledThreadPoolExecutor(1);

  private static Logger log;

  /**
   * Returns true if currently running on a Windows based operating system.
   *
   * @deprecated Its a property for the app, so please use AppProperties.isWindowsOs() direct.
   */
  @Deprecated public static boolean WINDOWS = AppProperties.isWindowsOs();

  /**
   * Returns true if currently running on a Mac OS X based operating system.
   *
   * @deprecated Its a property for the app, so please use AppProperties.isMacOs() direct.
   */
  @Deprecated public static boolean MAC_OS_X = AppProperties.isMacOs();

  /**
   * Returns true if currently running on Linux or other Unix/Unix like system.
   *
   * @deprecated Its a property for the app, so please use AppProperties.isUnixOs() direct.
   */
  @Deprecated public static boolean LINUX_OR_UNIX = AppProperties.isUnixOs();

  /** @deprecated Not used anywhere? If usefull later, please set comment, otherwise remove it */
  @Deprecated
  public static final String LOOK_AND_FEEL_NAME =
      MAC_OS_X
          ? "net.rptools.maptool.client.TinyLookAndFeelMac"
          : "de.muntjak.tinylookandfeel.TinyLookAndFeel";

  private static File appHome;

  private static String packagerCfgFileName =
      getAttributeFromJarManifest("Implementation-Title", AppConstants.APP_NAME) != null
          ? getAttributeFromJarManifest("Implementation-Title", AppConstants.APP_NAME) + ".cfg"
          : null;

  /**
   * Initialize the main directories DataDir & LogDir This is be done only once!
   *
   * <p>As a side-effect the function creates the directories if not exits.
   */
  public static void initializeMainDirs() {
    if (appHome != null) return; // Initialize only once!
    appHome = new File(AppProperties.getDataDirName());
    if (!appHome.exists()) createDir(appHome);
    File logsHome = new File(AppProperties.getLogDirName());
    if (!logsHome.exists()) createDir(logsHome);
  }

  /**
   * Initializes the first loggers. This is be done only once!
   *
   * <p>During startup some classes have to be initialized before logging is possible. This can now
   * establish a logger and maybe log some saved information over the starting phase.
   */
  public static void initializeFirstLoggers() {
    if (log != null) return; // Initialize only once

    log = LogManager.getLogger(AppUtil.class);

    log.info("********************************************************************************");
    log.info("**                                                                            **");
    log.info("**                              MapTool Started!                              **");
    log.info("**                                                                            **");
    log.info("********************************************************************************");
    log.info("Logging to: " + getLoggerFileName());

    AppProperties.initializeLogger();
    AppStartupProperties.initializeLogger();
    AppCmdLineProperties.initializeLogger();
  }

  public static String getLoggerFileName() {
    org.apache.logging.log4j.core.Logger loggerImpl = (org.apache.logging.log4j.core.Logger) log;
    Appender appender = loggerImpl.getAppenders().get("LogFile");

    if (appender != null) {
      if (appender instanceof FileAppender) {
        return ((FileAppender) appender).getFileName();
      } else if (appender instanceof RollingFileAppender) {
        return ((RollingFileAppender) appender).getFileName();
      }
    }

    return "NOT_CONFIGURED";
  }
  
  private static void createDir(File path) {
    if (!path.exists()) {
      path.mkdirs();
      // Now check our work
      if (!path.exists()) {
        RuntimeException re =
            new RuntimeException(
                I18N.getText("msg.error.unableToCreateDataDir", path.getAbsolutePath()));
        if (log != null && log.isInfoEnabled()) {
          log.info("msg.error.unableToCreateDataDir", re);
        }
        throw re;
      }
    }
  }

  /**
   * Returns a File path representing the base directory to store local data. By default this is a
   * ".maptool" directory in the user's home directory.
   *
   * <p>If you want to change the dir for data storage you can set the system property
   * MAPTOOL_DATADIR. If the value of the MAPTOOL_DATADIR has any file separator characters in it,
   * it will assume you are using an absolute path. If the path does not include a file separator it
   * will use it as a subdirectory in the user's home directory
   *
   * @return the maptool data directory
   */
  public static File getAppHome() {
    return appHome;
  }

  /**
   * Returns a {@link File} path that points to the AppHome base directory along with the subpath
   * denoted in the "subdir" argument.
   *
   * <p>For example <code>getAppHome("cache")</code> will return the path <code>{APPHOME}/cache
   * </code>.
   *
   * <p>As a side-effect the function creates the directory pointed to by File.
   *
   * @param subdir of the maptool home directory
   * @return the maptool data directory name subdir
   * @see AppUtil#getAppHome
   */
  public static File getAppHome(String subdir) {
    File path = getAppHome();
    if (!StringUtils.isEmpty(subdir)) path = new File(path.getAbsolutePath(), subdir);
    if (!path.exists()) createDir(path);
    return path;
  }

  /**
   * Set the state back to uninitialized
   *
   * @deprecated Not in use ?
   */
  // Package protected for testing
  @Deprecated
  static void reset() {
    appHome = null;
  }

  /**
   * Returns a File path representing the base directory that the application is running from. e.g.
   * C:\Users\Troll\AppData\Local\MapTool\app
   *
   * @return the maptool install directory
   */
  public static String getAppInstallLocation() {
    String path = "UNKNOWN";

    try {
      CodeSource codeSource = MapTool.class.getProtectionDomain().getCodeSource();
      File jarFile = new File(codeSource.getLocation().toURI().getPath());
      path = jarFile.getParentFile().getPath();
    } catch (URISyntaxException e) {
      log.error("Error retrieving MapTool installation directory: ", e);
      throw new RuntimeException(I18N.getText("msg.error.unknownInstallPath"), e);
    }

    return path;
  }

  /**
   * Returns a File path representing the configuration file in the base directory that the
   * application is running from. e.g. C:\Users\Troll\AppData\Local\MapTool\app
   *
   * @return the configuration file in the maptool install directory
   */
  public static File getAppCfgFile() {
    File cfgFile;

    if (packagerCfgFileName == null) {
      return null;
    }

    try {
      CodeSource codeSource = MapTool.class.getProtectionDomain().getCodeSource();
      File jarFile = new File(codeSource.getLocation().toURI().getPath());
      String cfgFilepath = jarFile.getParentFile().getPath() + File.separator + packagerCfgFileName;

      cfgFile = new File(cfgFilepath);

    } catch (URISyntaxException e) {
      log.error("Error retrieving MapTool cfg file: ", e);
      throw new RuntimeException(I18N.getText("msg.error.retrieveCfgFile"), e);
    }

    return cfgFile;
  }

  /**
   * Returns a File path representing configuration file under the app home directory structure.
   *
   * @return the maptool configuration file under the app home directory structure.
   */
  public static File getDataDirAppCfgFile() {
    // Temp old code:
    return getAppHome(AppProperties.getConfigSubDirName())
        .toPath()
        .resolve(APP_HOME_CONFIG_FILENAME)
        .toFile();
  }

  /**
   * Get the an attribute value from MANIFEST.MF
   *
   * @return the String value or empty string if not found
   */
  public static String getAttributeFromJarManifest(String attributeName, String defaultValue) {
    ClassLoader cl = MapTool.class.getClassLoader();

    try {
      URL url = cl.getResource("META-INF/MANIFEST.MF");
      Manifest manifest = new Manifest(url.openStream());

      Attributes attr = manifest.getMainAttributes();
      return attr.getValue(attributeName);
    } catch (IOException e) {
      log.error("No {} attribute found in MANIFEST.MF...", attributeName, e);
    }

    return defaultValue;
  }

  /**
   * Returns a File object for the maptool tmp directory, or null if the users home directory could
   * not be determined.
   *
   * @return the maptool tmp directory
   */
  public static File getTmpDir() {
    return getAppHome(AppProperties.getTmpSubDirName());
  }

  //
  // To Do: Refactor into 2 classes: The methods above and below this line do not have a common
  // theme, but each of them represents a subject area.
  //

  /**
   * Returns true if the player owns the token, otherwise false. If the player is GM this function
   * always returns true. If strict token management is disabled then this function always returns
   * true.
   *
   * @param token the {@link Token} to check the ownership of.
   * @return {@code true} if the player owns the token, otherwise {@code false}.
   */
  public static boolean playerOwns(Token token) {
    Player player = MapTool.getPlayer();
    if (player.isGM()) {
      return true;
    }
    if (!MapTool.getServerPolicy().useStrictTokenManagement()) {
      return true;
    }
    return token.isOwner(player.getName());
  }

  /**
   * Returns whether the token is owned by a non-gm player.
   *
   * @param token the token
   * @return true if owned by all, or one of the owners is online and not a gm.
   */
  public static boolean ownedByOnePlayer(Token token) {
    if (token.isOwnedByAll()) {
      return true;
    }
    List<String> players = MapTool.getNonGMs();
    for (String owner : token.getOwners()) {
      if (players.contains(owner)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Returns true if the token is visible in the zone. If the view is the GM view then this function
   * always returns true.
   *
   * @param token the {@link Token} to check if the GM owns.
   * @return {@code true} if the GM "owns" the {@link Token}, otherwise {@code false}.
   */
  public static boolean gmOwns(Token token) {
    Player player = MapTool.getPlayer();

    if (!MapTool.getServerPolicy().useStrictTokenManagement()) {
      return true;
    }
    return (token.isOwner(player.getName()) && !token.isOwnedByAll()) || !token.hasOwners();
  }

  /**
   * Returns true if the token is visible in the zone. If the view is the GM view then this function
   * always returns true.
   *
   * @param zone to check for visibility
   * @param token to check for visibility in zone
   * @param view to use when checking visibility
   * @return true if token is visible in zone given the view
   */
  public static boolean tokenIsVisible(Zone zone, Token token, PlayerView view) {
    if (view.isGMView()) {
      return true;
    }
    return zone.isTokenVisible(token);
  }

  /**
   * Returns the disk spaced used in a given directory in a human readable format automatically
   * adjusting to kb/mb/gb etc.
   *
   * @param directory the directory to retrieve the space used for.
   * @return String of disk usage information.
   * @author Jamz
   * @since 1.4.0.1
   */
  public static String getDiskSpaceUsed(File directory) {
    try {
      final var visitor =
          new SimpleFileVisitor<Path>() {
            public long totalSize = 0;

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
              if (!attrs.isSymbolicLink()) {
                totalSize += attrs.size();
              }
              return FileVisitResult.CONTINUE;
            }
          };
      Files.walkFileTree(directory.toPath(), visitor);

      return FileUtils.byteCountToDisplaySize(visitor.totalSize) + " ";
    } catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }

  /**
   * Returns the free disk spaced for a given directory in a human readable format automatically
   * adjusting to kb/mb/gb etc.
   *
   * @param directory the directory to retrieve the free space for.
   * @return String of free disk space
   * @author Jamz
   * @since 1.4.0.
   */
  public static String getFreeDiskSpace(File directory) {
    return FileUtils.byteCountToDisplaySize(directory.getFreeSpace()) + " ";
  }

  public static String readClientId() {
    Path clientFile = Paths.get(getAppHome().getAbsolutePath(), CLIENT_ID_FILE);
    String clientId = "unknown";
    if (!clientFile.toFile().exists()) {
      clientId = UUID.randomUUID().toString();
      try {
        Files.write(clientFile, clientId.getBytes());
      } catch (IOException e) {
        log.info("msg.error.unableToCreateClientIdFile", e);
      }
    } else {
      try {
        clientId = new String(Files.readAllBytes(clientFile));
      } catch (IOException e) {
        log.info("msg.error.unableToReadClientIdFile", e);
      }
    }
    return clientId;
  }

  /**
   * Returns the name of the theme to use for the MapTool UI.
   *
   * @return the name of the theme to use for the MapTool UI.
   */
  public static String getThemeName() {
    Preferences prefs = Preferences.userRoot().node(AppConstants.APP_NAME + "/ui/theme");
    return prefs.get("themeName", AppConstants.DEFAULT_THEME_NAME);
  }

  /**
   * Sets the name of the theme to use for the MapTool UI.
   *
   * @param themeName the name of the theme to use for the MapTool UI.
   */
  public static void setThemeName(String themeName) {
    Preferences prefs = Preferences.userRoot().node(AppConstants.APP_NAME + "/ui/theme");
    prefs.put("themeName", themeName);
  }
}
