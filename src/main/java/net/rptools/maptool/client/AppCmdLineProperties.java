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

import java.util.ArrayList;
import java.util.Arrays;
import net.rptools.maptool.util.StringUtil;
import org.apache.commons.cli.*;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * To access application properties, please use AppProperties! This is a helper class for
 * AppProperties and should never be used directly. It handles the values set as arguments (command
 * line options) to the application.
 */
public class AppCmdLineProperties {

  private static Logger log;

  private static Options cmdOptions;
  private static CommandLine cmd;
  private static String[] cmdLineArgs;

  public static void initialize(String[] args) {
    cmdLineArgs = args;
    cmdOptions = new Options();
    cmdOptions.addOption(
        "?",
        AppProperties.PROP.CMD_OPTIONS_HELP.getCmdLongOpt(),
        false,
        "list of options incl. description to log and information frame, then exit the application");
    cmdOptions.addOption(
        "d",
        AppProperties.PROP.DEBUG_FLAG.getCmdLongOpt(),
        false,
        "turn on System.out enhanced debug output");
    cmdOptions.addOption(
        "v",
        AppProperties.PROP.VERSION_OVERWRITE.getCmdLongOpt(),
        true,
        "override MapTool version. Some MapTool functions will break if the version is not set correct!");
    cmdOptions.addOption(
        "f", AppProperties.PROP.FULLSCREEN_FLAG.getCmdLongOpt(), false, "set to maximize window");
    cmdOptions.addOption(
        "g",
        AppProperties.PROP.MONITOR_TO_USE.getCmdLongOpt(),
        true,
        "sets which monitor (graphical device) to use");
    cmdOptions.addOption(
        "w",
        AppProperties.PROP.WINDOW_WIDTH.getCmdLongOpt(),
        true,
        "override MapTool window width. Only usable together with monitor and height");
    cmdOptions.addOption(
        "h",
        AppProperties.PROP.WINDOW_HEIGHT.getCmdLongOpt(),
        true,
        "override MapTool window height. Only usable together with monitor and width");
    cmdOptions.addOption(
        "x",
        AppProperties.PROP.WINDOW_XPOS.getCmdLongOpt(),
        true,
        "override MapTool window starting x coordinate. Only usable together with monitor and ypos");
    cmdOptions.addOption(
        "y",
        AppProperties.PROP.WINDOW_YPOS.getCmdLongOpt(),
        true,
        "override MapTool window starting y coordinate. Only usable together with monitor and xpos");
    cmdOptions.addOption(
        "m",
        AppProperties.PROP.LIST_MACROS_FLAG.getCmdLongOpt(),
        false,
        "display defined list of macro functions");
    cmdOptions.addOption(
        "r",
        AppProperties.PROP.RESET_FLAG.getCmdLongOpt(),
        false,
        "reset startup options to defaults");
    cmdOptions.addOption(
        "F",
        AppProperties.PROP.DEPRECATED_LOAD_CAMPAIGN_NAME.getCmdLongOpt(),
        true,
        "load campaign on startup. Deprecated: Please use C or campaign");
    cmdOptions.addOption(
        "C",
        AppProperties.PROP.LOAD_CAMPAIGN_NAME.getCmdLongOpt(),
        true,
        "load campaign on startup. Arg.: Full file path and name of the campaign");
    cmdOptions.addOption(
        "S",
        AppProperties.PROP.LOAD_SERVER_FLAG.getCmdLongOpt(),
        false,
        "start server on startup. Using server parameters from user preferences");
    cmdOptions.addOption(
        "s",
        AppProperties.PROP.LOAD_SERVER_DELAY.getCmdLongOpt(),
        false,
        "delay the start of the server for x seconds. e.g. network needs more time to be ready");
    cmdOptions.addOption(
        "A",
        AppProperties.PROP.LOAD_AUTOSAVE_FILE.getCmdLongOpt(),
        true,
        "if there is a newer autosave file then the campaign to load, should the app load it (yes), not load it (no) or ask (ask) for the decision (default is ask)");
    cmdOptions.addOption(
        "D",
        AppProperties.PROP.DATA_DIR_NAME.getCmdLongOpt(),
        true,
        "override MapTool data dictionary");
    cmdOptions.addOption(
        "P",
        AppProperties.PROP.STARTUP_PROPS_FILE_NAME.getCmdLongOpt(),
        true,
        "override name (& path) of the startup properties file");

    parse();
  }

  private static void parse() {
    CommandLineParser cmdParser = new DefaultParser();
    cmd = null;

    ArrayList<String> correctCmdLineArgs = new ArrayList<>(Arrays.asList(cmdLineArgs));
    while (cmd == null) {
      try {
        cmd =
            cmdParser.parse(
                cmdOptions, correctCmdLineArgs.toArray(new String[correctCmdLineArgs.size()]));
      } catch (UnrecognizedOptionException e) {
        if (log != null) MapTool.showWarning("Error parsing the command line", e);
        correctCmdLineArgs.remove(e.getOption());
      } catch (ParseException e) {
        if (log != null) MapTool.showWarning("Error parsing the command line", e);
        correctCmdLineArgs.clear(); // Unknown parse exception so clean the list to initialize cmd
      }
    }

    if (log != null) {
      if (correctCmdLineArgs.isEmpty()) log.info("no argument passed via command line");
      else
        for (String arg : correctCmdLineArgs) log.info("argument passed via command line: " + arg);
    }
  }

  public static void initializeLogger() {
    if (log != null) return; // Initialize only once!

    log = LogManager.getLogger(AppCmdLineProperties.class);
    parse(); // parse can be done twice, same result, almost no time consumed and now we can show
    // warn & write log
    if (cmd.hasOption(AppProperties.PROP.CMD_OPTIONS_HELP.getCmdLongOpt())) helpInfo();
  }

  /**
   * Search for command line arguments for options. Expecting arguments specified as
   * -parameter=value pair and returns a string.
   *
   * <p>Examples: -version=1.4.0.1 -user=Jamz
   *
   * @param searchValue Option string to search for, ie -version
   * @param defaultValue A default value to return if option is not found
   * @return Option value found as a String, or defaultValue if not found
   */
  public static String getCommandLineOption(String searchValue, String defaultValue) {
    return cmd.hasOption(searchValue) ? cmd.getOptionValue(searchValue) : defaultValue;

    /* Version NO, Campaign YES ???
    if (cmd.getArgs().length != 0) {
      log.info("Overriding -F option with extra argument");
      loadCampaignOnStartPath = cmd.getArgs()[0];
    }
    if (!loadCampaignOnStartPath.isEmpty()) {
      log.info("Loading initial campaign: " + loadCampaignOnStartPath);
    }
    */

  }

  /**
   * Search for command line arguments for options. Expecting arguments formatted as a switch
   *
   * <p>Examples: -x or -fullscreen
   *
   * @param searchValue Option string to search for, ie -version
   * @return A boolean value of true if option parameter found or false if not found
   */
  public static boolean getCommandLineOption(String searchValue) {
    return cmd.hasOption(searchValue);
  }

  /**
   * Search for command line arguments for options. Expecting arguments specified as
   * -parameter=value pair and returns a string.
   *
   * <p>Examples: -monitor=1 -x=0 -y=0 -w=1200 -h=960
   *
   * @param searchValue Option string to search for, ie -version
   * @param defaultValue A default value to return if option is not found
   * @return Int value of the matching option parameter if found or default value if not found
   */
  public static int getCommandLineOption(String searchValue, int defaultValue) {
    return StringUtil.parseInteger(cmd.getOptionValue(searchValue), defaultValue);
  }

  /** A typical help information for the supported command line options. */
  public static void helpInfo() {
    String longOption;
    String longOptionTitle = "Long Option";
    String startInfoLine = "List of available command line options:";
    String endInfoLine = "Application will stop now!";
    String lineSeparator = System.lineSeparator();
    String lineBreak = "<br>";

    int maxLongOptionLength = longOptionTitle.length();
    int longOptionLength = 0;
    for (Option option : cmdOptions.getOptions()) {
      longOptionLength = option.getLongOpt().length();
      if (longOptionLength > maxLongOptionLength) maxLongOptionLength = longOptionLength;
    }

    StringBuilder messageBuilder = new StringBuilder();
    String message;

    messageBuilder.append(startInfoLine).append(lineSeparator);
    message =
        String.format(
            "X | %s | Description",
            new StringBuilder(longOptionTitle)
                .append(" ".repeat(maxLongOptionLength - longOptionTitle.length()))
                .toString());
    messageBuilder.append(message).append(lineSeparator);
    for (Option option : cmdOptions.getOptions()) {
      longOptionLength = option.getLongOpt().length();
      longOption =
          new StringBuilder(option.getLongOpt())
              .append(" ".repeat(maxLongOptionLength - longOptionLength))
              .toString();
      message = String.format("%s | %s | %s", option.getOpt(), longOption, option.getDescription());
      messageBuilder.append(message).append(lineSeparator);
    }
    messageBuilder.append(endInfoLine).append(lineSeparator);
    log.info(messageBuilder.toString());
    String infoMessage =
        StringUtils.replaceEach(
            messageBuilder.toString(), new String[] {lineSeparator}, new String[] {lineBreak});
    MapTool.showInformation(infoMessage);

    System.exit(0);
  }
}
