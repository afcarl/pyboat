package pyboat

import pyboat.game._

import scala.collection.mutable.ListBuffer
import java.sql.DriverManager
import java.sql.Connection
import scala.io.Source
import java.io.IOException

abstract class DriverConfig
case class LocalHost() extends DriverConfig {
  val url = "jdbc:mysql://localhost/diplomacy?autoReconnect=true&useSSL=false"
  val username = "root"
  val password = ""
}
case class Synapse() extends DriverConfig {
  val url = "jdbc:mysql://10.1.58.68/diplomacy?autoReconnect=true&useSSL=false"
  val username = "maxst"
  val password = Database.readPasswordFromFile()
}

object Database {
  val driver = "com.mysql.jdbc.Driver"
  val driverConfig = Synapse()
  val url = driverConfig.url
  val username = driverConfig.username
  val password = driverConfig.password

  def getTotalNumExamplesMoveOrHold() : Int = {
    // The number of labels of this type in a game = (n - 1),
    // where n is the number of turns
    // so the number of labels in the database is N - g
    // where g is the number of games and N is the total number
    // of Fall/Winter/Spring Orders phases
    val turnsStatement = "SELECT COUNT(turn_num) AS total FROM turns WHERE phase=\"Orders\""
    val gamesStatement = "SELECT COUNT(id) AS total FROM games"
    var (rs, connection) = query(turnsStatement)

    rs.next()
    val totalTurns = rs.getInt("total")
    connection.close()

    var (rs1, connection1) = query(gamesStatement)

    rs1.next()
    val totalGames = rs1.getInt("total")
    connection1.close()

    return totalTurns - totalGames
  }

  def readPasswordFromFile() : String = {
    for (line <- Source.fromFile("password.secrets").getLines()) {
      if (line != "") {
        return line
      }
    }
    throw new IOException("Could not get the password from the password file.")
  }

  def getAllGameIds() : List[Int] = {
    val sqlStatement = "SELECT id FROM games"
    var (rs, connection) = query(sqlStatement)
    var lb = new ListBuffer[Int]()
    while (rs.next()) {
      lb += rs.getInt("id")
    }
    connection.close()
    return lb.toList
  }

  /**
   * Gets a DipUnit from the Database whose unitId and gameId are known.
   *
   * !! Note: The returned unit will have a non-sensical (likely null) location !!
   */
  def getUnit(gameId: Int, unitId: Int) : DipUnit = {
    val sqlStatement = "SELECT * FROM units WHERE game_id=" + gameId + " AND unit_id=" + unitId
    var (rs, connection) = query(sqlStatement)

    rs.next()
    val u = breakOutUnit(rs, connection, gameId)
    connection.close()

    return u
  }

  def breakOutUnit(rs: java.sql.ResultSet, connection: java.sql.Connection, gameId: Int) : DipUnit = {
    val id = rs.getInt("game_id")
    val country : CountryType = rs.getString("country").charAt(0) match {
      case 'A' => Austria()
      case 'E' => England()
      case 'F' => France()
      case 'G' => Germany()
      case 'I' => Italy()
      case 'R' => Russia()
      case 'T' => Turkey()
    }
    val unitType : UnitType = rs.getString("type").charAt(0) match {
      case 'A' => Army()
      case 'F' => Fleet()
    }
    val startTurn = rs.getInt("start_turn")
    val endTurn = rs.getInt("end_turn")
    val unitId = rs.getInt("unit_id")

    // Figure out where the unit is located by asking for its first order and retrieving location from that
    // If this isn't a starting unit, this won't work and we will just let its location be null
    val locationStat = "SELECT * FROM orders WHERE game_id=" + gameId + " AND unit_id=" + unitId + " AND turn_num=1"
    val lrs = connection.createStatement().executeQuery(locationStat)
    val location = if (lrs.next()) lrs.getString("location") else null

    val u = new DipUnit(gameId, country, unitType, startTurn, endTurn, unitId, location)
    return u
  }

  def getStartingUnits(gameId: Int, country: CountryType) : List[DipUnit] = {
    val sqlStatement = "SELECT * FROM units WHERE game_id=" + gameId + " AND country=\'" + country.code + "\' and start_turn=0"
    var (rs, connection) = query(sqlStatement)

    val buf = new ListBuffer[DipUnit]
    while (rs.next()) {
      val u = breakOutUnit(rs, connection, gameId)
      buf += u
    }
    connection.close()

    return buf.toList
  }

  /**
   * @return Tuple3 of the form (gameId, numTurns, numPlayers)
   */
  def getGame(gameId: Int) : (Int, Int, Int) = {
    val sqlStatement = "SELECT * FROM games WHERE id=" + gameId
    var (rs, connection) = query(sqlStatement)
    //implicitly assuming that there is only one row in the resultSet (there is no good way to assert this)

    rs.next()
    val id = rs.getInt("id")
    val numTurns = rs.getInt("num_turns")
    val numPlayers = rs.getInt("num_players")
    connection.close()

    require(gameId == id)
    return (id, numTurns, numPlayers)
  }

  def getOrdersForTurn(gameId: Int, turnNum: Int) : List[Order] = {
    val sqlStatement = "SELECT * FROM orders WHERE game_id=" + gameId + " AND turn_num=" + turnNum
    var (rs, connection) = query(sqlStatement)

    val buf = new ListBuffer[Order]
    while(rs.next()) {
      val id = rs.getInt("game_id")
      val unitId = rs.getInt("unit_id")
      val unitOrder = rs.getString("unit_order") match {
        case "MOVE" => Move()
        case "HOLD" => Hold()
        case "CONVOY" => Convoy()
        case "SUPPORT" => Support()
        case "BUILD" => Build()
        case "RETREAT" => Retreat()
        case "DESTROY" => Destroy()
      }
      val location = rs.getString("location")
      val tmpTarget = rs.getString("target")
      val target = if (tmpTarget != null && tmpTarget.contains("VIA CONVOY")) tmpTarget.dropRight(" VIA CONVOY".length) else tmpTarget
      val targetDest = rs.getString("target_dest")
      val success = rs.getInt("success") == 1
      val reason = rs.getString("reason")
      val num = rs.getInt("turn_num")
      val order = new Order(gameId, unitId, unitOrder, location, target, targetDest, success, reason, turnNum)
      buf += order
    }
    connection.close()

    return buf.toList
  }

  def turnExists(gameId: Int, turnNum: Int) : Boolean = {
    val sqlStatement = "SELECT * FROM turns WHERE game_id=" + gameId + " AND turn_num=" + turnNum
    var (rs, connection) = query(sqlStatement)
    val exists = rs.next()
    connection.close()
    return exists
  }

  def getTurn(gameId: Int, turnNum: Int) : Turn = {
    val sqlStatement = "SELECT * FROM turns WHERE game_id=" + gameId + " AND turn_num=" + turnNum
    var (rs, connection) = query(sqlStatement)
    //implicitly assuming that there is only one row in the resultSet (there is no good way to assert this)

    rs.next()
    val rsGameId = rs.getInt("game_id")
    val rsTurnNum = rs.getInt("turn_num")
    val year = rs.getInt("year")
    val season : SeasonType = rs.getString("season") match {
      case "Winter" => Winter()
      case "Spring" => Spring()
      case "Fall" => Fall()
    }
    val phase : PhaseType = rs.getString("phase") match {
      case "Winter" => WinterPhase()
      case "Orders" => OrdersPhase()
      case "Build" => BuildPhase()
      case "Retreat" => RetreatPhase()
    }
    connection.close()

    require(gameId == rsGameId)
    require(turnNum == rsTurnNum)

    return new Turn(gameId, turnNum, phase, year, season)
  }

  /**
   * Don't forget to close the connection!
   */
  def query(sqlStatement: String) : (java.sql.ResultSet, Connection) = {
    var connection : Connection = null
    Class.forName(driver)
    connection = DriverManager.getConnection(url, username, password)
    val stat = connection.createStatement()
    val rs = stat.executeQuery(sqlStatement)
    return (rs, connection)
  }
}
