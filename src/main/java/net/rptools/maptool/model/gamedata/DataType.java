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
package net.rptools.maptool.model.gamedata;

public enum DataType {
  LONG,
  DOUBLE,
  STRING,
  BOOLEAN,
  LIST,
  MAP,
  UNDEFINED;

  public DataValue convert(DataValue dataValue, DataType to) {
    return switch (to) {
      case LONG -> new LongDataValue(dataValue.getName(), dataValue.asLong());
      case DOUBLE -> new DoubleDataValue(dataValue.getName(),dataValue.asDouble());
      case BOOLEAN -> new BooleanDataValue(dataValue.getName(), dataValue.asBoolean());
      case STRING -> new StringDataValue(dataValue.getName(), dataValue.asString());
      case LIST -> new ListDataValue(dataValue.getName(), dataValue.asList());
      case MAP -> new MapDataValue(dataValue.getName(), dataValue.asMap());
      case UNDEFINED -> throw InvalidDataOperation.createInvalidConversion(dataValue.getDataType(),
          to);
    };
  }
}
