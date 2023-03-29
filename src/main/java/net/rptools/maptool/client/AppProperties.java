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
import net.rptools.maptool.language.I18N;
import org.apache.logging.log4j.LogManager;import org.apache.logging.log4j.Logger;
import net.rptools.maptool.util.StringUtil;

/** Properties for the application MapTool.
    Some of them are OS depending and not changable.
    Others can be change in one or more places with a clear priority from (low -> high)
    Default_Value -> System_Properties -> StartUp_Properties -> CmdLineOptions
 */
public class AppProperties {

  public enum PROPS {
    OS_NAME ("os.name", null, null),
    USER_HOME ("user.home", null, null),
    DATA_DIR_NAME ("MAPTOOL_DATADIR", "datadir", ".maptool"),
    LOG_DIR_NAME("MAPTOOL_LOGDIR", null, "logs"),
    CONFIG_SUBDIR_NAME (null, null, "config"),
    TEMP_SUBDIR_NAME (null, null, "tmp"),
    CMD_OPTIONS_HELP (null, "help", "false"),
    DEBUG_FLAG (null, "debug", "false"),
    RESET_FLAG (null, "reset", "false"),
    LIST_MACROS_FLAG ( null,"macros", "false"),
    VERSION_OVERWRITE (null, "version", null),
    FULLSCREEN (null,"fullscreen", "false"),
    MONITOR_TO_USE (null, "monitor", "-1"),
    WINDOW_WIDTH (null, "width", "-1"),
    WINDOW_HEIGHT (null, "height", "-1"),
    WINDOW_XPOS (null, "xpos", "-1"),
    WINDOW_YPOS (null, "ypos", "-1"),
    LOAD_SERVER (null, "server", "false"),
    LOAD_CAMPAIGN_NAME (null, "campaign", null),
    @Deprecated
    DEPRECATED_LOAD_CAMPAIGN_NAME (null, "file", null);

    private final String key, cmdLongOpt, defaultValue;

    PROPS(String key, String cmdLongOpt, String defaultValue) {
      this.key = key;
      this.cmdLongOpt = cmdLongOpt;
      this.defaultValue = defaultValue;
    }

    /**
     *
     * @return the key or the property name if key is "" or null if only changeable over cmdLineOption;
     */
    public String getKey() {
      return (key != null && key.isEmpty()) ? name() : key;
    }

    /**
     *
     * @return the long option name or null if there is no command option
     */
    public String getCmdLongOpt() {
      return cmdLongOpt;
    }

    public String getDefaultValue() {
      return defaultValue;
    }

    public int getDefaultIntValue() {
      return StringUtil.parseInteger(defaultValue, -1);
    }
  }

  // Caching
  private static String dataDirPathName;
  private static String logDirPathName;
  private static Logger log;

  public static void initialize(String[] cmdLineArgs){
    AppCmdLineProperties.initialize(cmdLineArgs);
  }

  public static String getOsName () {
    return System.getProperty(PROPS.OS_NAME.getKey());
  }

  /** Returns true if currently running on a Windows based operating system. */
  public static boolean isWindowsOs () {return getOsName().toLowerCase().startsWith("windows"); }

  /** Returns true if currently running on a Mac OS X based operating system. */
  public static boolean isMacOs () {return getOsName().toLowerCase().startsWith("mac os x"); }

  /** Returns true if currently running on Unix/Unix like system, e.g. Linux. */
  public static boolean isUnixOs () {
    String osName = getOsName();
    return (   osName.contains("nix")
            || osName.contains("nux")
            || osName.contains("aix")
            || osName.contains("sunos"));
  }

  public static String getUserHomeName () {
    return System.getProperty(PROPS.USER_HOME.getKey());
  }

  /**
   * Absolute data directory path name
   *
   * <p>If you want to change the dir for data storage you can set the system property
   * MAPTOOL_DATADIR. If the value of the MAPTOOL_DATADIR has any file separator characters in it,
   * it will assume you are using an absolute path. If the path does not include a file separator it
   * will use it as a subdirectory in the user's home directory.
   *
   * @return the absolute directory path name to store data files
   */
  public static String getDataDirName() {
    if (dataDirPathName != null) return dataDirPathName;
    dataDirPathName = AppCmdLineProperties.getCommandLineOption(PROPS.DATA_DIR_NAME.getCmdLongOpt(), PROPS.DATA_DIR_NAME.getDefaultValue());
    dataDirPathName = System.getProperty(PROPS.DATA_DIR_NAME.getKey(), dataDirPathName);

    if (!(dataDirPathName.contains(File.separator) || dataDirPathName.contains("/") || dataDirPathName.contains("\\"))) {
      // this is a relative path, so we use the user home to produce an absolute path
      dataDirPathName = getUserHomeName() + File.separator + dataDirPathName;
    }
    dataDirPathName = new File(dataDirPathName).getAbsolutePath();

    // Now we need to check for characters that are known to cause problems in
    // path names. We want to allow the local platform to make this decision, but
    // the built-in "jar://" URL uses the "!" as a separator between the archive name
    // and the archive member. :( Right now we're only checking for that one character
    // but the list may need to be expanded in the future.
    if (dataDirPathName.matches("!")) {
      throw new RuntimeException(I18N.getText("msg.error.unusableDataDir", dataDirPathName));
    }
    return dataDirPathName;
  }

  /**
   * Absolute logs directory path name
   *
   * Initialize the LogDirProperty in the system properties at the first time.
   * This will not check, if the value in the system properties is a valid value!
   * Default value will be the absolute path of the DataDir + /logs.
   *
   *  <p>If you want to change the dir for log storage you can set the system property
   *  MAPTOOL_LOGDIR. If the value of the MAPTOOL_LOGDIR has any file separator characters in it,
   *  it will assume you are using an absolute path. If the path does not include a file separator it
   *  will use it as a subdirectory in the data directory.
   *
   * @return the absolute directory path name to store log files
   */
  public static String getLogDirName() {
    if (logDirPathName != null) return logDirPathName;

    logDirPathName =
        System.getProperty(PROPS.LOG_DIR_NAME.getKey(), PROPS.LOG_DIR_NAME.getDefaultValue());
    if (!(logDirPathName.contains(File.separator)
        || logDirPathName.contains("/")
        || logDirPathName.contains("\\"))) {
      // this is a relative path, so we use the user home to produce an absolute path
      logDirPathName = getDataDirName() + File.separator + logDirPathName;
    }

    logDirPathName = new File(logDirPathName).getAbsolutePath();

    // Now we need to check for characters that are known to cause problems in
    // path names. We want to allow the local platform to make this decision, but
    // the built-in "jar://" URL uses the "!" as a separator between the archive name
    // and the archive member. :( Right now we're only checking for that one character
    // but the list may need to be expanded in the future.
    if (logDirPathName.matches("!")) {
      throw new RuntimeException(I18N.getText("msg.error.unusableDataDir", logDirPathName));
    }

    System.setProperty(PROPS.LOG_DIR_NAME.getKey(), logDirPathName);

    return logDirPathName;
  }

  /**
   * @return Name of the subdir for temporary files.
   */
  public static String getTmpSubDirName() {
    return PROPS.TEMP_SUBDIR_NAME.getDefaultValue();
  }

  /**
   * @return Name of the subdir for configuration files.
   */
  public static String getConfigSubDirName() {
    return PROPS.CONFIG_SUBDIR_NAME.getDefaultValue();
  }

  /**
   * Initialize the Loggers for the different Property classes.
   * The needed log environment is now set.
   */
  public static void initializeLoggers() {
    if (log != null) return; // Initialise only once!

    log = LogManager.getLogger(AppProperties.class);
    AppCmdLineProperties.initializeLogger();
  }

  public static boolean getDebugFlag () {
    return AppCmdLineProperties.getCommandLineOption(PROPS.DEBUG_FLAG.getCmdLongOpt());
  }

  /**
   *
   * @return A Version string, which overwrites the normal version default null.
   */
  public static String getVersionOverwrite () {
    return AppCmdLineProperties.getCommandLineOption(
        PROPS.VERSION_OVERWRITE.getCmdLongOpt(), PROPS.VERSION_OVERWRITE.getDefaultValue());
  }

  public static boolean getFullscreenFlag () {
    return AppCmdLineProperties.getCommandLineOption(PROPS.FULLSCREEN.getCmdLongOpt());
  }

  public static int getMonitorToUse () {
    return AppCmdLineProperties.getCommandLineOption(
        PROPS.MONITOR_TO_USE.getCmdLongOpt(), PROPS.MONITOR_TO_USE.getDefaultIntValue());
  }

  public static int getWindowWidth () {
    return AppCmdLineProperties.getCommandLineOption(
        PROPS.WINDOW_WIDTH.getCmdLongOpt(), PROPS.WINDOW_WIDTH.getDefaultIntValue());
  }

  public static int getWindowHeight () {
    return AppCmdLineProperties.getCommandLineOption(
        PROPS.WINDOW_HEIGHT.getCmdLongOpt(), PROPS.WINDOW_HEIGHT.getDefaultIntValue());
  }

  public static int getWindowXpos () {
    return AppCmdLineProperties.getCommandLineOption(
        PROPS.WINDOW_XPOS.getCmdLongOpt(), PROPS.WINDOW_XPOS.getDefaultIntValue());
  }

  public static int getWindowYpos () {
    return AppCmdLineProperties.getCommandLineOption(
        PROPS.WINDOW_YPOS.getCmdLongOpt(), PROPS.WINDOW_YPOS.getDefaultIntValue());
  }

  public static boolean getListMacrosFlag () {
    return AppCmdLineProperties.getCommandLineOption(PROPS.LIST_MACROS_FLAG.getCmdLongOpt());
  }
  public static boolean getResetFlag () {
    return AppCmdLineProperties.getCommandLineOption(PROPS.RESET_FLAG.getCmdLongOpt());
  }

  /**
   *
   * @return Name of the campaign, with should be load at startup, default null.
   */
  public static String getLoadCompaignName () {
    return AppCmdLineProperties.getCommandLineOption(
        PROPS.LOAD_CAMPAIGN_NAME.getCmdLongOpt(), AppCmdLineProperties.getCommandLineOption(PROPS.DEPRECATED_LOAD_CAMPAIGN_NAME.getCmdLongOpt(), PROPS.LOAD_CAMPAIGN_NAME.getDefaultValue()));
  }

}
