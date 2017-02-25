package fr.guiet.automationserver.dataaccess;

import java.sql.DriverManager;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.PreparedStatement;
import java.util.Calendar;
import java.sql.Timestamp;
import java.util.List;
import java.util.Properties;
import java.util.Date;
import java.util.ArrayList;
import java.sql.ResultSet;
import org.apache.log4j.Logger;
import org.influxdb.*;
import org.influxdb.dto.*;
import java.util.concurrent.TimeUnit;

import fr.guiet.automationserver.dto.*;

/**
 * Handles database management
 * 
 * @author guiet
 *
 */
public class DbManager {

	// Logger
	private static Logger _logger = Logger.getLogger(DbManager.class);

	private static String _postgresqlConnectionString = "jdbc:postgresql://%s:%s/%s";
	private static String _userName = null;
	private static String _password = null;
	private static String _postgresqlEnable = null;

	private static String _influxdbConnectionString = "http://%s:%s";
	private static String _databaseInfluxDB = null; // "user_automation";
	private static String _userNameInfluxDB = null; // "user_automation";
	private static String _passwordInfluxDB = null; // "raspberry";
	private static String _influxdbEnable = null;
	private static String _retentionPolicy = null;

	private InfluxDB _influxDB = null;

	/**
	 * Constructor
	 */
	public DbManager() {
		try {
			Class.forName("org.postgresql.Driver");
		} catch (ClassNotFoundException e) {
			_logger.error("Impossible de trouver le driver PostgreSQL JDBC, il faut l'inclure dans le library path", e);
		}

		InputStream is = null;
		try {
			String configPath = System.getProperty("automationserver.config.path");
			is = new FileInputStream(configPath);

			Properties prop = new Properties();
			prop.load(is);

			// PostgreSQL
			String host = prop.getProperty("postgresql.host");
			String port = prop.getProperty("postgresql.port");
			String database = prop.getProperty("postgresql.database");

			// TODO : Checker les valeurs null
			_postgresqlConnectionString = String.format(_postgresqlConnectionString, host, port, database);
			_userName = prop.getProperty("postgresql.username");
			_password = prop.getProperty("postgresql.password");
			_postgresqlEnable = prop.getProperty("postgresql.enable");

			// InfluxDB
			host = prop.getProperty("influxdb.host");
			port = prop.getProperty("influxdb.port");
			_influxdbConnectionString = String.format(_influxdbConnectionString, host, port);
			_databaseInfluxDB = prop.getProperty("influxdb.database");
			_userNameInfluxDB = prop.getProperty("influxdb.username");
			_passwordInfluxDB = prop.getProperty("influxdb.password");
			_retentionPolicy = prop.getProperty("influxdb.retentionpolicy");
			_influxdbEnable = prop.getProperty("influxdb.enable");

		} catch (FileNotFoundException e) {
			_logger.error(
					"Impossible de trouver le fichier de configuration classpath_folder/config/automationserver.properties",
					e);
		} catch (IOException e) {
			_logger.error(
					"Impossible de trouver le fichier de configuration classpath_folder/config/automationserver.properties",
					e);
		}
	}
	
	public void SaveOutsideSensorsInfo(float outsideTemp, float garageTemp, int pressure, float altitude) {
		try {
			if (!_influxdbEnable.equals("true"))
				return;

			_logger.info("Saving outside info to InfluxDB");

			// _logger.info("InfluxDB connecting..");
			_influxDB = InfluxDBFactory.connect(_influxdbConnectionString, _userNameInfluxDB, _passwordInfluxDB);
			// _logger.info("InfluxDB Connected");

			BatchPoints batchPoints = BatchPoints.database(_databaseInfluxDB).retentionPolicy(_retentionPolicy)
					// .consistency(ConsistencyLevel.ALL)
					.build();

			Point point1 = Point.measurement("outside").time(System.currentTimeMillis(), TimeUnit.MILLISECONDS)
					.addField("outside_temp", outsideTemp)
					.addField("garage_temp", garageTemp)
					.addField("pressure", pressure)
					.addField("altitude", altitude)
					.build();

			batchPoints.point(point1);

			// _logger.info("InfluxDB writing...");
			_influxDB.write(batchPoints);
			// influxDB.write(sensorName, TimeUnit.MILLISECONDS, serie);
			// _logger.info("InfluxDB written...");
			_influxDB.close();

		} catch (Exception e) {
			_logger.error("Erreur lors de l'écriture dans InfluxDB", e);
		}
		
	}

	public void SaveMailboxSensorInfoInfluxDB(float vcc) {
		try {
			if (!_influxdbEnable.equals("true"))
				return;

			_logger.info("Saving mailbox info to InfluxDB");

			// _logger.info("InfluxDB connecting..");
			_influxDB = InfluxDBFactory.connect(_influxdbConnectionString, _userNameInfluxDB, _passwordInfluxDB);
			// _logger.info("InfluxDB Connected");

			BatchPoints batchPoints = BatchPoints.database(_databaseInfluxDB).retentionPolicy(_retentionPolicy)
					// .consistency(ConsistencyLevel.ALL)
					.build();

			Point point1 = Point.measurement("mailbox").time(System.currentTimeMillis(), TimeUnit.MILLISECONDS)
					.addField("vcc", vcc).build();

			batchPoints.point(point1);

			// _logger.info("InfluxDB writing...");
			_influxDB.write(batchPoints);
			// influxDB.write(sensorName, TimeUnit.MILLISECONDS, serie);
			// _logger.info("InfluxDB written...");
			_influxDB.close();

		} catch (Exception e) {
			_logger.error("Erreur lors de l'écriture dans InfluxDB", e);
		}
	}

	/**
	 * Saves sensor information into InfluxDB
	 * 
	 * @param sensorName
	 * @param actualTemp
	 * @param wantedTemp
	 * @param humidity
	 */
	public void SaveSensorInfoInfluxDB(String roomName, Float actualTemp, Float wantedTemp, float humidity) {

		try {
			if (!_influxdbEnable.equals("true"))
				return;

			_logger.info("Saving sensor info to InfluxDB");

			// _logger.info("InfluxDB connecting..");
			_influxDB = InfluxDBFactory.connect(_influxdbConnectionString, _userNameInfluxDB, _passwordInfluxDB);
			// _logger.info("InfluxDB Connected");

			BatchPoints batchPoints = BatchPoints.database(_databaseInfluxDB).retentionPolicy(_retentionPolicy)
					// .consistency(ConsistencyLevel.ALL)
					.build();

			Point point1 = Point.measurement(roomName).time(System.currentTimeMillis(), TimeUnit.MILLISECONDS)
					.addField("actual_temp", actualTemp).addField("wanted_temp", wantedTemp)
					.addField("humidity", humidity).build();

			batchPoints.point(point1);

			// _logger.info("InfluxDB writing...");
			_influxDB.write(batchPoints);
			// influxDB.write(sensorName, TimeUnit.MILLISECONDS, serie);
			// _logger.info("InfluxDB written...");
			_influxDB.close();

		} catch (Exception e) {
			_logger.error("Erreur lors de l'écriture dans InfluxDB", e);
		}
	}

	public void SaveCaveInfoToInfluxDb(Float temp, Float humidity, String extractorState) {

		try {
			if (!_influxdbEnable.equals("true"))
				return;

			_logger.info("Saving cave info to InfluxDB");

			// _logger.info("InfluxDB connecting..");
			_influxDB = InfluxDBFactory.connect(_influxdbConnectionString, _userNameInfluxDB, _passwordInfluxDB);
			// _logger.info("InfluxDB Connected");

			BatchPoints batchPoints = BatchPoints.database(_databaseInfluxDB).retentionPolicy(_retentionPolicy)
					// .consistency(ConsistencyLevel.ALL)
					.build();

			Point point1 = Point.measurement("basement").time(System.currentTimeMillis(), TimeUnit.MILLISECONDS)
					.addField("temperature", temp).addField("humidity", humidity)
					.addField("extractor_state", extractorState).build();

			batchPoints.point(point1);

			// _logger.info("InfluxDB writing...");
			_influxDB.write(batchPoints);
			// influxDB.write(sensorName, TimeUnit.MILLISECONDS, serie);
			// _logger.info("InfluxDB written...");
			_influxDB.close();

		} catch (Exception e) {
			_logger.error("Erreur lors de l'écriture dans InfluxDB", e);
		}
	}

	/**
	 * Saves teleinfo framework into InfluxDB
	 * 
	 * @param teleInfoTrameDto
	 */
	public void SaveTeleInfoTrameToInfluxDb(TeleInfoTrameDto teleInfoTrameDto) {

		try {

			if (!_influxdbEnable.equals("true"))
				return;

			// _logger.info("InfluxDB connecting..");
			_influxDB = InfluxDBFactory.connect(_influxdbConnectionString, _userNameInfluxDB, _passwordInfluxDB);
			// _logger.info("InfluxDB Connected");

			BatchPoints batchPoints = BatchPoints.database(_databaseInfluxDB).retentionPolicy(_retentionPolicy)
					// .consistency(ConsistencyLevel.ALL)
					.build();
			Point point1 = Point.measurement("teleinfo").time(System.currentTimeMillis(), TimeUnit.MILLISECONDS)
					.addField("ADCO", teleInfoTrameDto.ADCO).addField("OPTARIF", teleInfoTrameDto.OPTARIF)
					.addField("ISOUSC", teleInfoTrameDto.ISOUSC).addField("HCHC", teleInfoTrameDto.HCHC)
					.addField("HCHP", teleInfoTrameDto.HCHP).addField("PTEC", teleInfoTrameDto.PTEC)
					.addField("IINST1", teleInfoTrameDto.IINST1).addField("IINST2", teleInfoTrameDto.IINST2)
					.addField("IINST3", teleInfoTrameDto.IINST3).addField("IMAX1", teleInfoTrameDto.IMAX1)
					.addField("IMAX2", teleInfoTrameDto.IMAX2).addField("IMAX3", teleInfoTrameDto.IMAX3)
					.addField("PMAX", teleInfoTrameDto.PMAX).addField("PAPP", teleInfoTrameDto.PAPP)
					.addField("HHPHC", teleInfoTrameDto.HHPHC).addField("MOTETAT", teleInfoTrameDto.MOTDETAT)
					.addField("PPOT", teleInfoTrameDto.PPOT).build();

			batchPoints.point(point1);

			// _logger.info("InfluxDB writing...");
			_influxDB.write(batchPoints);

			_influxDB.close();
			// influxDB.write(sensorName, TimeUnit.MILLISECONDS, serie);
			// _logger.info("InfluxDB written...");
		} catch (Exception e) {
			_logger.error("Erreur lors de l'écriture dans InfluxDB", e);
		}

	}

	/**
	 * Saves sensor information into PostgreSQL
	 * 
	 * @param idSensor
	 * @param actualTemp
	 * @param wantedTemp
	 * @param humidity
	 */
	public void SaveSensorInfo(long idSensor, Float actualTemp, Float wantedTemp, float humidity) {

		// TODO : revoir le modèle de données de la base Postgres

		Connection connection = null;
		PreparedStatement pst = null;

		try {

			if (!_postgresqlEnable.equals("true"))
				return;

			connection = DriverManager.getConnection(_postgresqlConnectionString, _userName, _password);

			String stm = "INSERT INTO automation.sensor_entry("
					+ "id_sensor, actual_temp, wanted_temp, humidity, received_date)" + "VALUES (?, ?, ?, ?, ?);";

			pst = connection.prepareStatement(stm);

			pst.setLong(1, idSensor);
			pst.setFloat(2, actualTemp);
			if (wantedTemp == null)
				pst.setNull(3, java.sql.Types.FLOAT);
			else
				pst.setFloat(3, wantedTemp);
			pst.setFloat(4, humidity);
			Calendar cal = Calendar.getInstance();
			Timestamp timestamp = new Timestamp(cal.getTimeInMillis());
			pst.setTimestamp(5, timestamp);

			pst.executeUpdate();

		} catch (SQLException e) {
			_logger.error("Erreur lors de l'enregistrement en base de donnees", e);
		} finally {

			try {
				if (pst != null) {
					pst.close();
				}
				if (connection != null) {
					connection.close();
				}

			} catch (SQLException ex) {
				_logger.error("Erreur lors de la fermeture de la base de donnees", ex);
			}
		}

	}

	/**
	 * Gets list of heaters dto associated with a room
	 * 
	 * @param roomId
	 * @return Returns list of Dto objects representing heaters
	 */
	public ArrayList<HeaterDto> GetHeatersByRoomId(long roomId) {

		Connection connection = null;
		PreparedStatement pst = null;
		ResultSet rs = null;
		ArrayList<HeaterDto> dtoList = new ArrayList<HeaterDto>();

		try {
			connection = DriverManager.getConnection(_postgresqlConnectionString, _userName, _password);

			String query = "select b.name, b.id_heater, current_consumption, phase, raspberry_pin from automation.room_heater a, automation.heater b "
					+ "where a.id_heater = b.id_heater and a.id_room=?";

			pst = connection.prepareStatement(query);
			pst.setLong(1, roomId);

			rs = pst.executeQuery();

			while (rs.next()) {
				HeaterDto dto = new HeaterDto();
				dto.heaterId = rs.getLong("id_heater");
				dto.currentConsumption = rs.getInt("current_consumption");
				dto.phase = rs.getInt("phase");
				dto.raspberryPin = rs.getInt("raspberry_pin");
				dto.name = rs.getString("name");

				dtoList.add(dto);
			}

		} catch (SQLException e) {
			_logger.error("Erreur lors de la récupération des radiateurs dans la base de données", e);
		} finally {

			try {
				if (pst != null) {
					pst.close();
				}
				if (connection != null) {
					connection.close();
				}

			} catch (SQLException ex) {
				_logger.error("Erreur lors de la fermeture de la base de donnees", ex);
			}
		}

		return dtoList;
	}

	/**
	 * Returns sensor dto associated with a room
	 * 
	 * @param sensorId
	 * @return
	 */
	public SensorDto GetSensorByRoomId(long sensorId) {

		Connection connection = null;
		PreparedStatement pst = null;
		ResultSet rs = null;
		SensorDto dto = null;

		try {
			connection = DriverManager.getConnection(_postgresqlConnectionString, _userName, _password);

			String query = "SELECT c.id_sensor, c.sensor_address, c.name FROM automation.sensor c "
					+ "where c.id_sensor = ? ";

			pst = connection.prepareStatement(query);
			pst.setLong(1, sensorId);

			rs = pst.executeQuery();
			rs.next();

			dto = new SensorDto();
			dto.sensorId = rs.getLong("id_sensor");
			dto.sensorAddress = rs.getString("sensor_address");
			dto.name = rs.getString("name");

		} catch (SQLException e) {
			_logger.error("Erreur lors de la récupération du capteur dans la base de données", e);
		} finally {

			try {
				if (pst != null) {
					pst.close();
				}
				if (connection != null) {
					connection.close();
				}

			} catch (SQLException ex) {
				_logger.error("Erreur lors de la fermeture de la base de donnees", ex);
			}
		}

		return dto;
	}

	// *** Obtient la temperature par defaut à instaurer de la piece pour un
	// jour de la semaine et une heure
	public Float GetDefaultTempByRoom(long roomId) {

		Connection connection = null;
		PreparedStatement pst = null;
		ResultSet rs = null;
		Float defaultTemp = null;

		try {

			connection = DriverManager.getConnection(_postgresqlConnectionString, _userName, _password);

			String query = "select temp from automation.temp_schedule where id_room=? and day_of_week=date_part('dow',now())+1 and (date_trunc('second', now()::timestamp))::time without time zone between hour_begin and hour_end";

			pst = connection.prepareStatement(query);
			pst.setLong(1, roomId);

			rs = pst.executeQuery();
			while (rs.next()) {
				defaultTemp = rs.getFloat("temp");
			}
		} catch (SQLException e) {
			_logger.error("Erreur lors de la lecture en base de donnees (getDefaultTempByRoom)", e);
		} finally {

			try {
				if (pst != null) {

					pst.close();
				}
				if (rs != null) {
					rs.close();
				}
				if (connection != null) {
					connection.close();
				}

			} catch (SQLException ex) {

				_logger.error("Erreur lors de la fermeture de la base de donnees", ex);
			}
		}

		return defaultTemp;
	}

	// Obtient la priorite du radiateur en fonction du jour de la semaine et de
	// l'heure
	public Integer GetCurrentPriorityByHeaterId(long heaterId) {

		Connection connection = null;
		PreparedStatement pst = null;
		ResultSet rs = null;
		Integer currentPriority = null;

		try {

			connection = DriverManager.getConnection(_postgresqlConnectionString, _userName, _password);

			String query = "select priority from automation.priority_schedule where id_heater=1 and day_of_week=date_part('dow',now())+1 and (date_trunc('second', now()::timestamp))::time without time zone between hour_begin and hour_end";

			pst = connection.prepareStatement(query);
			// pst.setLong(1, heaterId);

			rs = pst.executeQuery();
			while (rs.next()) {
				currentPriority = rs.getInt("priority");
			}
		} catch (SQLException e) {
			_logger.error("Erreur lors de la lecture en base de donnees (GetCurrentPriorityByHeaterId)", e);
		} finally {

			try {
				if (pst != null) {

					pst.close();
				}
				if (rs != null) {
					rs.close();
				}
				if (connection != null) {
					connection.close();
				}

			} catch (SQLException ex) {

				_logger.error("Erreur lors de la fermeture de la base de donnees", ex);
			}
		}

		return currentPriority;
	}

	/***
	 * /* /* Récupère la liste des pièces /*
	 ***/
	public List<RoomDto> GetRooms() {

		List<RoomDto> roomList = null;
		Connection connection = null;
		PreparedStatement pst = null;
		// PreparedStatement pst1 = null;
		ResultSet rs = null;

		try {
			connection = DriverManager.getConnection(_postgresqlConnectionString, _userName, _password);

			String query = "SELECT a.id_room, a.name, a.id_sensor, a.mqtt_topic, a.influxdb_measurement FROM automation.room a ";

			pst = connection.prepareStatement(query);

			rs = pst.executeQuery();

			roomList = new ArrayList<RoomDto>();
			while (rs.next()) {
				RoomDto r = new RoomDto();
				r.id = rs.getLong("id_room");
				r.name = rs.getString("name");
				r.idSensor = rs.getLong("id_sensor");
				r.mqttTopic = rs.getString("mqtt_topic");
				r.influxdbMeasurement = rs.getString("influxdb_measurement");

				roomList.add(r);
			}
		} catch (SQLException e) {
			_logger.error("Erreur lors de la lecture des pièces dans la base de données", e);
		} finally {
			try {
				if (pst != null) {
					pst.close();
				}
				if (rs != null) {
					rs.close();
				}
				if (connection != null) {
					connection.close();
				}

			} catch (SQLException ex) {
				_logger.error("Erreur lors de la fermeture de la base de donnees", ex);
			}
		}

		return roomList;
	}

	public TempScheduleDto GetNextDefaultTempByRoom(long roomId, int dayOfWeek) {

		Connection connection = null;
		PreparedStatement pst = null;
		ResultSet rs = null;
		// Float defaultTemp = null;
		// Date du jour
		Calendar calendar = Calendar.getInstance();
		// Jour de la semaine
		int dow = calendar.get(Calendar.DAY_OF_WEEK);
		String query;
		TempScheduleDto ts = null;

		try {
			connection = DriverManager.getConnection(_postgresqlConnectionString, _userName, _password);

			if (dayOfWeek == dow) {

				query = "select temp, hour_begin, hour_end, day_of_week " + "from automation.temp_schedule "
						+ "where id_room=? and day_of_week=date_part('dow',now())+1 " + "and "
						+ "(temp!=(select temp from automation.temp_schedule where id_room=? and day_of_week=date_part('dow',now())+1 and (date_trunc('second', now()::timestamp))::time without time zone between hour_begin and hour_end)) "
						+ "and "
						+ "(day_of_week=(select day_of_week from automation.temp_schedule where id_room=? and day_of_week=date_part('dow',now())+1 and (date_trunc('second', now()::timestamp))::time without time zone between hour_begin and hour_end)) "
						+ "and "
						+ "(hour_begin > (select hour_end from automation.temp_schedule where id_room=? and day_of_week=date_part('dow',now())+1 and (date_trunc('second', now()::timestamp))::time without time zone between hour_begin and hour_end)) "
						+ "order by day_of_week,hour_begin asc " + "limit 1 ";

				pst = connection.prepareStatement(query);
				pst.setLong(1, roomId);
				pst.setLong(2, roomId);
				pst.setLong(3, roomId);
				pst.setLong(4, roomId);
			} else {
				query = "select temp, hour_begin, hour_end, day_of_week " + "from automation.temp_schedule "
						+ "where id_room=? and day_of_week=? " + "and "
						+ "(temp!=(select temp from automation.temp_schedule where id_room=? and day_of_week=date_part('dow',now())+1 and (date_trunc('second', now()::timestamp))::time without time zone between hour_begin and hour_end)) "
						+ "order by hour_begin asc " + "limit 1 ";

				pst = connection.prepareStatement(query);
				pst.setLong(1, roomId);
				pst.setInt(2, dayOfWeek);
				pst.setLong(3, roomId);
			}

			rs = pst.executeQuery();
			while (rs.next()) {
				Timestamp hourBegin = rs.getTimestamp("hour_begin");
				Timestamp hourEnd = rs.getTimestamp("hour_end");
				ts = new TempScheduleDto(new Date(hourBegin.getTime()), new Date(hourEnd.getTime()),
						rs.getFloat("temp"), rs.getInt("day_of_week"));
			}
		} catch (SQLException e) {
			_logger.error("Erreur lors de la lecture en base de donnees (getNextDefaultTempByRoom)", e);
		} finally {

			try {
				if (pst != null) {
					pst.close();
				}
				if (rs != null) {
					rs.close();
				}
				if (connection != null) {
					connection.close();
				}

			} catch (SQLException ex) {
				_logger.error("Erreur lors de la fermeture de la base de donnees", ex);
			}
		}

		return ts;

	}

	/***
	 * /* /* Sauvegarde de la trame en bdd /*
	 ***/
	public void SaveTeleInfoTrame(TeleInfoTrameDto teleInfoTrameDto) {

		Connection connection = null;
		PreparedStatement pst = null;

		try {

			if (!_postgresqlEnable.equals("true"))
				return;

			connection = DriverManager.getConnection(_postgresqlConnectionString, _userName, _password);

			String stm = "INSERT INTO automation.trame_teleinfo("
					+ "date_reception, adco, optarif, isousc, hchc, hchp, ptec, iinst1, "
					+ "iinst2, iinst3, imax1, imax2, imax3, pmax, papp, hhphc, motdetat, " + "ppot)"
					+ "VALUES (?, ?, ?, ?, ?, ?, ?, ?,  ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);";

			pst = connection.prepareStatement(stm);

			Calendar cal = Calendar.getInstance();
			Timestamp timestamp = new Timestamp(cal.getTimeInMillis());
			pst.setTimestamp(1, timestamp);

			pst.setString(2, teleInfoTrameDto.ADCO);
			pst.setString(3, teleInfoTrameDto.OPTARIF);
			pst.setShort(4, teleInfoTrameDto.ISOUSC);
			pst.setInt(5, teleInfoTrameDto.HCHC);
			pst.setInt(6, teleInfoTrameDto.HCHP);
			// System.out.println("PTEC : "+teleInfoTrameDto.PTEC);
			pst.setString(7, teleInfoTrameDto.PTEC);
			pst.setShort(8, teleInfoTrameDto.IINST1);
			pst.setShort(9, teleInfoTrameDto.IINST2);
			pst.setShort(10, teleInfoTrameDto.IINST3);
			pst.setShort(11, teleInfoTrameDto.IMAX1);
			pst.setShort(12, teleInfoTrameDto.IMAX2);
			pst.setShort(13, teleInfoTrameDto.IMAX3);
			pst.setInt(14, teleInfoTrameDto.PMAX);
			pst.setInt(15, teleInfoTrameDto.PAPP);
			pst.setString(16, teleInfoTrameDto.HHPHC);
			pst.setString(17, teleInfoTrameDto.MOTDETAT);
			// System.out.println("PPOT : "+teleInfoTrameDto.PPOT);
			pst.setString(18, teleInfoTrameDto.PPOT);

			pst.executeUpdate();

		} catch (SQLException e) {
			_logger.error("Erreur lors de l'enregistrement de la trame teleinfo en base de données", e);
		} finally {

			try {
				if (pst != null) {
					pst.close();
				}
				if (connection != null) {
					connection.close();
				}

			} catch (SQLException ex) {
				_logger.error("Erreur lors de la fermeture de la base de donnees", ex);
			}
		}
	}
}
