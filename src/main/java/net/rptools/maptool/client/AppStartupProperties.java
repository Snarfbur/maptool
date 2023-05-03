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

import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.*;
import net.rptools.maptool.util.StringUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * To access application properties, please use AppProperties! This is a helper class for
 * AppProperties and should never be used directly. It handles the startup properties file and the
 * values in this file.
 */
public class AppStartupProperties {
  private static Logger log;
  private static IOException catchedIOException;
  private static final String KEY_NOT_FOUND = "<key not found>";

  private static final Set<AppProperties.PROP> allowedProps =
      EnumSet.of(
          AppProperties.PROP.DATA_DIR_NAME,
          AppProperties.PROP.LOG_DIR_NAME,
          AppProperties.PROP.LOAD_SERVER_FLAG,
          AppProperties.PROP.LOAD_SERVER_DELAY,
          AppProperties.PROP.LOAD_CAMPAIGN_NAME,
          AppProperties.PROP.LOAD_AUTOSAVE_FILE,
          AppProperties.PROP.LOCALE_LANGUAGE,
          AppProperties.PROP.LOCALE_REGION,
          AppProperties.PROP.FULLSCREEN_FLAG,
          AppProperties.PROP.SKIP_AUTO_UPDATE_FLAG);
  private static HashSet<String> allowedKeys = new HashSet<>();
  private static Properties startupProps;

  public static void initialize() {
    if (startupProps == null) loadStartupProperties();
  }

  public static void initializeLogger() {
    if (log != null) return; // Initialize only once!

    log = LogManager.getLogger(AppStartupProperties.class);

    log.info("Start up properties file: {}", AppProperties.getStartupPropsFileName());
    if (catchedIOException != null)
      if (catchedIOException instanceof FileNotFoundException
          || catchedIOException instanceof NoSuchFileException)
        log.info(
            "{} not found. This can be ok, if it really does not exists.",
            AppProperties.getStartupPropsFileName());
      else
        MapTool.showError(
            "Unexpected IOException during load of startup properties.", catchedIOException);

    // log allowed & found values
    String key, value;
    for (AppProperties.PROP prop : allowedProps) {
      key = prop.getKey();
      allowedKeys.add(key);
      value = startupProps.getProperty(key);
      if (value == null) value = KEY_NOT_FOUND;
      // Show value as in file as a String
      log.info("Usable keys in startup.properties: {}, value found: {}", key, value);
      if (prop == AppProperties.PROP.DATA_DIR_NAME
          && !KEY_NOT_FOUND.equals(value)
          && !AppProperties.isStartupPropsFilePropValueAbsolute()) {
        log.info(
            "Start up properties file definition '{}' was set relative to the MAPTOOL_DATADIR, so redefinition of the MAPTOOL_DATADIR will be ignored!",
            AppProperties.getStartupPropsFileNameOriginal());
      }
    }
  }

  public static void loadStartupProperties() {
    startupProps = new Properties();
    try {
      startupProps.load((Files.newInputStream(Path.of(AppProperties.getStartupPropsFileName()))));
    } catch (FileNotFoundException | NoSuchFileException e) {
      if (log == null) catchedIOException = e;
      else
        log.info(
            "{} not found. This can be ok, if it really does not exists.",
            AppProperties.getStartupPropsFileName());
    } catch (IOException e) {
      if (log == null) catchedIOException = e;
      else MapTool.showError("Unexpected IOException during load of startup properties.", e);
    }
  }

  public static void storeStartupProperties() {
    try {
      startupProps.store(new FileWriter(AppProperties.getStartupPropsFileName()), null);
    } catch (IOException e) {
      log.error("Unexpected IOException during store of startup properties.", e);
      throw new RuntimeException(e);
    }
    log.info(
        "File startup.properties stored in {} with following value pairs:",
        AppProperties.getStartupPropsFileName());
    for (String key : allowedKeys) {
      String value = startupProps.getProperty(key);
      if (value != null) log.info("Key: {}, Value: {}", key, value);
    }
  }

  /**
   * @param key The Key to the property
   * @param defaultValue A default value to return if key is not found
   * @return property value to the key as a String, or defaultValue if not found
   */
  public static String getStartupProperty(String key, String defaultValue) {
    return startupProps.getProperty(key, defaultValue);
  }

  /**
   * @param key The Key to the property
   * @return A boolean value of true if key can be found in the properties or false if not found
   */
  public static boolean getStartupProperty(String key) {
    return startupProps.containsKey(key);
  }

  /**
   * @param key The Key to the property
   * @param defaultValue A default value to return if key is not found
   * @return Int value of the matching option parameter if found or default value if not found or
   *     invalid
   */
  public static int getStartupProperty(String key, int defaultValue) {
    return StringUtil.parseInteger(startupProps.getProperty(key), defaultValue);
  }

  public static void setStartupProperty(String key, String value) {
    startupProps.setProperty(key, value);
  }

  public static void setStartupProperty(String key, boolean value) {
    startupProps.setProperty(key, Boolean.toString(value));
  }

  public static void setStartupProperty(String key, int value) {
    startupProps.setProperty(key, Integer.toString(value));
  }
}
