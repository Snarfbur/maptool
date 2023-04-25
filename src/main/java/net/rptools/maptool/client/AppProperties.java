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
import java.util.Locale;
import net.rptools.maptool.language.I18N;
import net.rptools.maptool.util.StringUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Properties for the application MapTool. Some of them are OS depending and not changable. Others
 * can be change in one or more places with a clear priority from (low -> high) Default_Value ->
 * System_Properties -> StartUp_Properties -> CmdLineOptions
 */
public class AppProperties {

  public enum PROPS {
    OS_NAME("os.name", null, null),
    USER_HOME("user.home", null, null),
    DATA_DIR_NAME("MAPTOOL_DATADIR", "datadir", ".maptool"),
    LOG_DIR_NAME("MAPTOOL_LOGDIR", null, "logs"),
    CONFIG_SUBDIR_NAME(null, null, "config"),
    TEMP_SUBDIR_NAME(null, null, "tmp"),
    CMD_OPTIONS_HELP(null, "help", "false"),
    DEBUG_FLAG(null, "debug", "false"),
    RESET_FLAG(null, "reset", "false"),
    LIST_MACROS_FLAG(null, "macros", "false"),
    VERSION_OVERWRITE(null, "version", null),
    FULLSCREEN_FLAG("", "fullscreen", "false"),
    MONITOR_TO_USE(null, "monitor", "-1"),
    WINDOW_WIDTH(null, "width", "-1"),
    WINDOW_HEIGHT(null, "height", "-1"),
    WINDOW_XPOS(null, "xpos", "-1"),
    WINDOW_YPOS(null, "ypos", "-1"),
    LOAD_SERVER_FLAG("", "server", "false"),
    LOAD_CAMPAIGN_NAME("", "campaign", null),
    @Deprecated
    DEPRECATED_LOAD_CAMPAIGN_NAME(null, "file", null),
    STARTUP_PROPS_FILE_NAME("MAPTOOL_STARTUP_FILE", "props-file", "startup.properties"),
    SKIP_AUTO_UPDATE_FLAG("", null, "false"),
    LOCALE_LANGUAGE("user.language", null, Locale.getDefault().getLanguage()),
    LOCALE_REGION("user.region", null, Locale.getDefault().getCountry());
    private final String key, cmdLongOpt, defaultValue;

    PROPS(String key, String cmdLongOpt, String defaultValue) {
      this.key = key;
      this.cmdLongOpt = cmdLongOpt;
      this.defaultValue = defaultValue;
    }

    /**
     * @return the key or the property name if key is "" or null if only changeable over
     *     cmdLineOption;
     */
    public String getKey() {
      return (key != null && key.isEmpty()) ? name() : key;
    }

    /** @return the long option name or null if there is no command option */
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
  private static String startupPropsFileName, startupPropsFileNameOriginal;
  private static boolean startupPropsFilePropValueAbsolute = false;
  private static Logger log;

  public static void initialize() {
    initializeStartupPropsCaches();
  }

  private static void initializeStartupPropsCaches() {
    if (startupPropsFileName != null) return;
    startupPropsFileNameOriginal =
        System.getProperty(
            PROPS.STARTUP_PROPS_FILE_NAME.getKey(),
            PROPS.STARTUP_PROPS_FILE_NAME.getDefaultValue());
    try {
      startupPropsFileNameOriginal =
          AppCmdLineProperties.getCommandLineOption(
              PROPS.STARTUP_PROPS_FILE_NAME.getCmdLongOpt(), startupPropsFileNameOriginal);
    } catch (NullPointerException e) {
      // CmdLineProperties has problems, which will be handled during logger initialisation, so we
      // ignore it.
    }

    if (startupPropsFileNameOriginal.contains(File.separator)
        || startupPropsFileNameOriginal.contains("/")
        || startupPropsFileNameOriginal.contains("\\")) {
      // this is an absolute path
      startupPropsFilePropValueAbsolute = true;
      startupPropsFileName = startupPropsFileNameOriginal;
    } else {
      // this is a relative path, so we use the dataDir + /config/ to produce an absolute path
      // setting the dataDir in the start-up properties will be ignored
      startupPropsFileName =
          getDataDirName()
              + File.separator
              + getConfigSubDirName()
              + File.separator
              + startupPropsFileNameOriginal;
    }

    startupPropsFileName = new File(startupPropsFileName).getAbsolutePath();

    // Now we need to check for characters that are known to cause problems in
    // path names. We want to allow the local platform to make this decision, but
    // the built-in "jar://" URL uses the "!" as a separator between the archive name
    // and the archive member. :( Right now we're only checking for that one character
    // but the list may need to be expanded in the future.
    if (startupPropsFileName.matches("!")) {
      throw new RuntimeException(I18N.getText("msg.error.unusableDataDir", startupPropsFileName));
    }
  }

  /** Initialize the logger, the needed log environment is now set. */
  public static void initializeLogger() {
    if (log != null) return; // Initialise only once!
    log = LogManager.getLogger(AppProperties.class);
  }

  public static void initializeDefaultLocale() {
    Locale.setDefault(
        new Locale(
            AppStartupProperties.getStartupProperty(
                PROPS.LOCALE_LANGUAGE.getKey(), PROPS.LOCALE_LANGUAGE.getDefaultValue()),
            AppStartupProperties.getStartupProperty(
                PROPS.LOCALE_REGION.getKey(), PROPS.LOCALE_REGION.getDefaultValue())));
    log.info("Default locale initialized to: {}", Locale.getDefault());
  }

  public static String getOsName() {
    return System.getProperty(PROPS.OS_NAME.getKey());
  }

  /** Returns true if currently running on a Windows based operating system. */
  public static boolean isWindowsOs() {
    return getOsName().toLowerCase().startsWith("windows");
  }

  /** Returns true if currently running on a Mac OS X based operating system. */
  public static boolean isMacOs() {
    return getOsName().toLowerCase().startsWith("mac os x");
  }

  /** Returns true if currently running on Unix/Unix like system, e.g. Linux. */
  public static boolean isUnixOs() {
    String osName = getOsName();
    return (osName.contains("nix")
        || osName.contains("nux")
        || osName.contains("aix")
        || osName.contains("sunos"));
  }

  public static String getUserHomeName() {
    return System.getProperty(PROPS.USER_HOME.getKey());
  }

  /**
   * Absolute data directory path name
   *
   * <p>The default value is the user home directory + /.maptool
   *
   * <p>If you want to change the dir for data storage you can set the system property
   * MAPTOOL_DATADIR or or the same name in the start up properties or the cmd option data-dir. If
   * the used value of the property has any file separator characters in it, it will assume you are
   * using an absolute path. If the path does not include a file separator it will use it as a
   * subdirectory in the user's home directory.
   *
   * @return the absolute directory path name to store data files
   */
  public static String getDataDirName() {
    if (dataDirPathName != null) return dataDirPathName;
    dataDirPathName =
        System.getProperty(PROPS.DATA_DIR_NAME.getKey(), PROPS.DATA_DIR_NAME.getDefaultValue());
    if (startupPropsFilePropValueAbsolute) {
      dataDirPathName =
          AppStartupProperties.getStartupProperty(PROPS.DATA_DIR_NAME.getKey(), dataDirPathName);
    }
    dataDirPathName =
        AppCmdLineProperties.getCommandLineOption(
            PROPS.DATA_DIR_NAME.getCmdLongOpt(), dataDirPathName);

    if (!(dataDirPathName.contains(File.separator)
        || dataDirPathName.contains("/")
        || dataDirPathName.contains("\\"))) {
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
   * <p>Initialize the LogDirProperty in the system properties at the first time. This will not
   * check, if the value in the system properties is a valid value! Default value will be the
   * absolute path of the DataDir + /logs.
   *
   * <p>If you want to change the dir for log storage you can set the system property MAPTOOL_LOGDIR
   * or the same name in the start up properties. If the used value of the property has any file
   * separator characters in it, it will assume you are using an absolute path. If the path does not
   * include a file separator it will use it as a subdirectory in the data directory.
   *
   * @return the absolute directory path name to store log files
   */
  public static String getLogDirName() {
    if (logDirPathName != null) return logDirPathName;
    logDirPathName =
        System.getProperty(PROPS.LOG_DIR_NAME.getKey(), PROPS.LOG_DIR_NAME.getDefaultValue());
    logDirPathName =
        AppStartupProperties.getStartupProperty(PROPS.LOG_DIR_NAME.getKey(), logDirPathName);

    if (!(logDirPathName.contains(File.separator)
        || logDirPathName.contains("/")
        || logDirPathName.contains("\\"))) {
      // this is a relative path, so we use the dataDir to produce an absolute path
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

  public static boolean isStartupPropsFilePropValueAbsolute() {
    if (startupPropsFileName == null) initializeStartupPropsCaches(); // Initialize value
    return startupPropsFilePropValueAbsolute;
  }

  public static String getStartupPropsFileNameOriginal() {
    if (startupPropsFileName == null) initializeStartupPropsCaches(); // Initialize value
    return startupPropsFileNameOriginal;
  }

  /**
   * Absolute path & file name for the start up properties of MapTool
   *
   * <p>Default value will be the absolute path & file name of the DataDir +
   * /config/startup.properties.
   *
   * <p>If you want to change the file and path for the startup.properties file you can set the
   * system property MAPTOOL_STARTUPPROPS_FILE or the cmdOption startup-file. If the used value has
   * any file separator characters in it, it will assume you are using an absolute path. If the path
   * does not include a file separator it will use it as a file name in the subdirectory 'config' of
   * the data directory.
   *
   * @return the absolute directory path name to the start up properties file
   */
  public static String getStartupPropsFileName() {
    if (startupPropsFileName == null) initializeStartupPropsCaches(); // Initialize value
    return startupPropsFileName;
  }

  /** @return Name of the subdir for temporary files. */
  public static String getTmpSubDirName() {
    return PROPS.TEMP_SUBDIR_NAME.getDefaultValue();
  }

  /** @return Name of the subdir for configuration files. */
  public static String getConfigSubDirName() {
    return PROPS.CONFIG_SUBDIR_NAME.getDefaultValue();
  }

  public static boolean getDebugFlag() {
    return AppCmdLineProperties.getCommandLineOption(PROPS.DEBUG_FLAG.getCmdLongOpt());
  }

  /** @return A Version string, which overwrites the normal version default null. */
  public static String getVersionOverwrite() {
    return AppCmdLineProperties.getCommandLineOption(
        PROPS.VERSION_OVERWRITE.getCmdLongOpt(), PROPS.VERSION_OVERWRITE.getDefaultValue());
  }

  public static boolean getFullscreenFlag() {
    return AppStartupProperties.getStartupProperty(PROPS.FULLSCREEN_FLAG.getKey())
        || AppCmdLineProperties.getCommandLineOption(PROPS.FULLSCREEN_FLAG.getCmdLongOpt());
  }

  public static int getMonitorToUse() {
    return AppCmdLineProperties.getCommandLineOption(
        PROPS.MONITOR_TO_USE.getCmdLongOpt(), PROPS.MONITOR_TO_USE.getDefaultIntValue());
  }

  public static int getWindowWidth() {
    return AppCmdLineProperties.getCommandLineOption(
        PROPS.WINDOW_WIDTH.getCmdLongOpt(), PROPS.WINDOW_WIDTH.getDefaultIntValue());
  }

  public static int getWindowHeight() {
    return AppCmdLineProperties.getCommandLineOption(
        PROPS.WINDOW_HEIGHT.getCmdLongOpt(), PROPS.WINDOW_HEIGHT.getDefaultIntValue());
  }

  public static int getWindowXpos() {
    return AppCmdLineProperties.getCommandLineOption(
        PROPS.WINDOW_XPOS.getCmdLongOpt(), PROPS.WINDOW_XPOS.getDefaultIntValue());
  }

  public static int getWindowYpos() {
    return AppCmdLineProperties.getCommandLineOption(
        PROPS.WINDOW_YPOS.getCmdLongOpt(), PROPS.WINDOW_YPOS.getDefaultIntValue());
  }

  public static boolean getListMacrosFlag() {
    return AppCmdLineProperties.getCommandLineOption(PROPS.LIST_MACROS_FLAG.getCmdLongOpt());
  }

  public static boolean getResetFlag() {
    return AppCmdLineProperties.getCommandLineOption(PROPS.RESET_FLAG.getCmdLongOpt());
  }

  /** @return Name of the campaign, with should be load at startup, default null. */
  public static String getLoadCampaignName() {
    return AppCmdLineProperties.getCommandLineOption(
        PROPS.LOAD_CAMPAIGN_NAME.getCmdLongOpt(),
        AppCmdLineProperties.getCommandLineOption(
            PROPS.DEPRECATED_LOAD_CAMPAIGN_NAME.getCmdLongOpt(),
            AppStartupProperties.getStartupProperty(
                PROPS.LOAD_CAMPAIGN_NAME.getKey(), PROPS.LOAD_CAMPAIGN_NAME.getDefaultValue())));
  }

  public static boolean getLoadServerFlag() {
    return AppStartupProperties.getStartupProperty(PROPS.LOAD_SERVER_FLAG.getKey())
        || AppCmdLineProperties.getCommandLineOption(PROPS.LOAD_SERVER_FLAG.getCmdLongOpt());
  }

  public static boolean getSkipAutoUpdateFlag() {
    return AppStartupProperties.getStartupProperty(PROPS.SKIP_AUTO_UPDATE_FLAG.getKey());
  }
}
