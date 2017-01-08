package fr.guiet.automationserver.dataaccess;

import java.sql.DriverManager;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.PreparedStatement;
import java.util.Calendar;
import java.sql.Timestamp;
import java.util.List;
import java.util.Date;
import java.util.ArrayList;
import java.sql.ResultSet;
import org.apache.log4j.Logger;
import org.influxdb.*;
import org.influxdb.dto.*;
import java.util.concurrent.TimeUnit;

import fr.guiet.automationserver.dto.*;

public class DbManager {

	// Logger
	private static Logger _logger = Logger.getLogger(DbManager.class);

	private static String _host = "jdbc:postgresql://127.0.0.1:5432/automation";
	private static String _userName = "automation_p";
	private static String _password = "brgm";

	private static String _hostInfluxDB = "http://192.168.1.25:8086";
	private static String _userNameInfluxDB = "user_automation";
	private static String _passwordInfluxDB = "raspberry";
	private InfluxDB _influxDB = null;

	public DbManager() {
		try {
			Class.forName("org.postgresql.Driver");
		} catch (ClassNotFoundException e) {
			_logger.error("Impossible de trouver le driver PostgreSQL JDBC, il faut l'inclure dans le library path", e);
			_logger.info("Trame non enregistree en base");
		}
	}

	public void SaveSensorInfoInfluxDB(String sensorName, Float actualTemp, Float wantedTemp, float humidity) {

		// Pas de sauvegarde de valeur null dans influxdb
		// if (actualTemp == null || wantedTemp == null) return;

		try {

			// _logger.info("InfluxDB connecting..");
			_influxDB = InfluxDBFactory.connect(_hostInfluxDB, _userNameInfluxDB, _passwordInfluxDB);
			// _logger.info("InfluxDB Connected");

			BatchPoints batchPoints = BatchPoints.database("automation").tag("async", "true").retentionPolicy("default")
					// .consistency(ConsistencyLevel.ALL)
					.build();

			Point point1 = Point.measurement(sensorName).time(System.currentTimeMillis(), TimeUnit.MILLISECONDS)
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

	public void SaveSensorInfo(long idSensor, Float actualTemp, Float wantedTemp, float humidity) {

		Connection connection = null;
		PreparedStatement pst = null;

		try {

			connection = DriverManager.getConnection(_host, _userName, _password);

			String stm = "INSERT INTO teleinfo.sensor_entry("
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

	public ArrayList<HeaterDto> GetHeatersByRoomId(long roomId) {

		Connection connection = null;
		PreparedStatement pst = null;
		ResultSet rs = null;
		ArrayList<HeaterDto> dtoList = new ArrayList<HeaterDto>();

		try {
			connection = DriverManager.getConnection(_host, _userName, _password);

			String query = "select b.name, b.id_heater, current_consumption, phase, raspberry_pin from teleinfo.room_heater a, teleinfo.heater b "
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

	public SensorDto GetSensorByRoomId(long sensorId) {

		Connection connection = null;
		PreparedStatement pst = null;
		ResultSet rs = null;
		SensorDto dto = null;

		try {
			connection = DriverManager.getConnection(_host, _userName, _password);

			String query = "SELECT c.id_sensor, c.sensor_address FROM teleinfo.sensor c " + "where c.id_sensor = ? ";

			pst = connection.prepareStatement(query);
			pst.setLong(1, sensorId);

			rs = pst.executeQuery();
			rs.next();

			dto = new SensorDto();
			dto.sensorId = rs.getLong("id_sensor");
			dto.sensorAddress = rs.getString("sensor_address");

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

			connection = DriverManager.getConnection(_host, _userName, _password);

			String query = "select temp from teleinfo.temp_schedule where id_room=? and day_of_week=date_part('dow',now())+1 and (date_trunc('second', now()::timestamp))::time without time zone between hour_begin and hour_end";

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

			connection = DriverManager.getConnection(_host, _userName, _password);

			String query = "select priority from teleinfo.priority_schedule where id_heater=1 and day_of_week=date_part('dow',now())+1 and (date_trunc('second', now()::timestamp))::time without time zone between hour_begin and hour_end";

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
			connection = DriverManager.getConnection(_host, _userName, _password);

			String query = "SELECT a.id_room, a.name, a.id_sensor FROM teleinfo.room a ";

			pst = connection.prepareStatement(query);

			rs = pst.executeQuery();

			roomList = new ArrayList<RoomDto>();
			while (rs.next()) {
				RoomDto r = new RoomDto();
				r.id = rs.getLong("id_room");
				r.name = rs.getString("name");
				r.idSensor = rs.getLong("id_sensor");

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
			connection = DriverManager.getConnection(_host, _userName, _password);

			if (dayOfWeek == dow) {

				query = "select temp, hour_begin, hour_end, day_of_week " + "from teleinfo.temp_schedule "
						+ "where id_room=? and day_of_week=date_part('dow',now())+1 " + "and "
						+ "(temp!=(select temp from teleinfo.temp_schedule where id_room=? and day_of_week=date_part('dow',now())+1 and (date_trunc('second', now()::timestamp))::time without time zone between hour_begin and hour_end)) "
						+ "and "
						+ "(day_of_week=(select day_of_week from teleinfo.temp_schedule where id_room=? and day_of_week=date_part('dow',now())+1 and (date_trunc('second', now()::timestamp))::time without time zone between hour_begin and hour_end)) "
						+ "and "
						+ "(hour_begin > (select hour_end from teleinfo.temp_schedule where id_room=? and day_of_week=date_part('dow',now())+1 and (date_trunc('second', now()::timestamp))::time without time zone between hour_begin and hour_end)) "
						+ "order by day_of_week,hour_begin asc " + "limit 1 ";

				pst = connection.prepareStatement(query);
				pst.setLong(1, roomId);
				pst.setLong(2, roomId);
				pst.setLong(3, roomId);
				pst.setLong(4, roomId);
			} else {
				query = "select temp, hour_begin, hour_end, day_of_week " + "from teleinfo.temp_schedule "
						+ "where id_room=? and day_of_week=? " + "and "
						+ "(temp!=(select temp from teleinfo.temp_schedule where id_room=? and day_of_week=date_part('dow',now())+1 and (date_trunc('second', now()::timestamp))::time without time zone between hour_begin and hour_end)) "
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

	public void SaveTeleInfoTrameToInfluxDb(TeleInfoTrameDto teleInfoTrameDto) {
		try {

			// _logger.info("InfluxDB connecting..");
			_influxDB = InfluxDBFactory.connect(_hostInfluxDB, _userNameInfluxDB, _passwordInfluxDB);
			// _logger.info("InfluxDB Connected");

			BatchPoints batchPoints = BatchPoints.database("automation").tag("async", "true").retentionPolicy("default")
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

			/*
			 * Serie serie = new
			 * Serie.Builder("automation").columns("actual_temp", "wanted_temp",
			 * "humidity").values( actualTemp, wantedTemp, humidity).build();
			 */

			// _logger.info("InfluxDB writing...");
			_influxDB.write(batchPoints);

			_influxDB.close();
			// influxDB.write(sensorName, TimeUnit.MILLISECONDS, serie);
			// _logger.info("InfluxDB written...");
		} catch (Exception e) {
			_logger.error("Erreur lors de l'écriture dans InfluxDB", e);
		}

	}

	/***
	 * /* /* Sauvegarde de la trame en bdd /*
	 ***/
	public void SaveTeleInfoTrame(TeleInfoTrameDto teleInfoTrameDto) {

		Connection connection = null;
		PreparedStatement pst = null;

		try {

			connection = DriverManager.getConnection(_host, _userName, _password);

			String stm = "INSERT INTO teleinfo.trame_teleinfo("
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
