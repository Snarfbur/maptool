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

import com.google.common.eventbus.Subscribe;
import java.util.concurrent.TimeUnit;
import javax.swing.*;
import net.rptools.maptool.client.events.CampaignActivated;
import net.rptools.maptool.events.MapToolEventBus;
import net.rptools.maptool.model.Campaign;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * The class used the event CampaignActivated to synchronize the load server action with the load
 * campaign action. Current server must be a personal server and the campaign can not be the default
 * one.
 *
 * <p>Design Pattern: Singleton
 */
public class LoadServerOnStartup {

  public static LoadServerOnStartup INSTANCE = new LoadServerOnStartup();

  private static final Logger log = LogManager.getLogger(LoadServerOnStartup.class);

  private LoadServerOnStartup() {}

  int counter = 0;

  /**
   * Register the instance to the event CampaignActivated. When the event is fired, the instance
   * will load the server und unregister itself.
   */
  public void delayLoadServerUntilCampaignActivated() {
    new MapToolEventBus().getMainEventBus().register(this);
  }

  @Subscribe
  private void doLoadServer(CampaignActivated campaignActivated) {
    Campaign campaign = campaignActivated.campaign();
    if (!campaign.isDefaultCampaign()) {
      log.info("Start Server Begin for Campaign: {}", campaign.getName());
      MapTool.startServerAndConnectFromPreferences();
      log.info("Start Server End for Campaign: {}", campaign.getName());
      try {
        log.info("LOAD_SERVER_DELAY {} start", AppProperties.getLoadServerDelay());
        TimeUnit.SECONDS.sleep(AppProperties.getLoadServerDelay());
        log.info("LOAD_SERVER_DELAY end");
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
      new MapToolEventBus().getMainEventBus().unregister(this);
    }
  }
}
