--
-- PostgreSQL database dump
--

-- Dumped from database version 9.4.10
-- Dumped by pg_dump version 9.5.5

SET statement_timeout = 0;
SET lock_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SET check_function_bodies = false;
SET client_min_messages = warning;
SET row_security = off;

--
-- Name: automation; Type: SCHEMA; Schema: -; Owner: automation_p
--

CREATE SCHEMA automation;


ALTER SCHEMA automation OWNER TO automation_p;

--
-- Name: plpgsql; Type: EXTENSION; Schema: -; Owner: 
--

CREATE EXTENSION IF NOT EXISTS plpgsql WITH SCHEMA pg_catalog;


--
-- Name: EXTENSION plpgsql; Type: COMMENT; Schema: -; Owner: 
--

COMMENT ON EXTENSION plpgsql IS 'PL/pgSQL procedural language';


SET search_path = automation, pg_catalog;

SET default_tablespace = '';

SET default_with_oids = false;

--
-- Name: heater; Type: TABLE; Schema: automation; Owner: automation_p
--

CREATE TABLE heater (
    id_heater integer NOT NULL,
    current_consumption smallint NOT NULL,
    phase smallint NOT NULL,
    raspberry_pin smallint NOT NULL,
    name character varying(30) NOT NULL
);


ALTER TABLE heater OWNER TO automation_p;

--
-- Name: TABLE heater; Type: COMMENT; Schema: automation; Owner: automation_p
--

COMMENT ON TABLE heater IS 'Chauffage';


--
-- Name: COLUMN heater.id_heater; Type: COMMENT; Schema: automation; Owner: automation_p
--

COMMENT ON COLUMN heater.id_heater IS 'Identifiant du chauffage';


--
-- Name: COLUMN heater.current_consumption; Type: COMMENT; Schema: automation; Owner: automation_p
--

COMMENT ON COLUMN heater.current_consumption IS 'consommation du chauffage en Ampère';


--
-- Name: COLUMN heater.phase; Type: COMMENT; Schema: automation; Owner: automation_p
--

COMMENT ON COLUMN heater.phase IS 'Sur quelle phase est localisé le chauffage';


--
-- Name: COLUMN heater.raspberry_pin; Type: COMMENT; Schema: automation; Owner: automation_p
--

COMMENT ON COLUMN heater.raspberry_pin IS 'Correspond à la pin p4j (http://pi4j.com/pins/model-b-plus.html) sur lequel est branché le fil pilote';


--
-- Name: COLUMN heater.name; Type: COMMENT; Schema: automation; Owner: automation_p
--

COMMENT ON COLUMN heater.name IS 'Nom du chauffage';


--
-- Name: heater_id_seq; Type: SEQUENCE; Schema: automation; Owner: automation_p
--

CREATE SEQUENCE heater_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE heater_id_seq OWNER TO automation_p;

--
-- Name: heater_id_seq; Type: SEQUENCE OWNED BY; Schema: automation; Owner: automation_p
--

ALTER SEQUENCE heater_id_seq OWNED BY heater.id_heater;


--
-- Name: priority_schedule; Type: TABLE; Schema: automation; Owner: automation_p
--

CREATE TABLE priority_schedule (
    id_priority_schedule integer NOT NULL,
    priority smallint NOT NULL,
    id_heater bigint NOT NULL,
    day_of_week smallint NOT NULL,
    hour_begin time without time zone NOT NULL,
    hour_end time without time zone NOT NULL
);


ALTER TABLE priority_schedule OWNER TO automation_p;

--
-- Name: TABLE priority_schedule; Type: COMMENT; Schema: automation; Owner: automation_p
--

COMMENT ON TABLE priority_schedule IS 'Priorité des chauffages par tranche horaire';


--
-- Name: COLUMN priority_schedule.priority; Type: COMMENT; Schema: automation; Owner: automation_p
--

COMMENT ON COLUMN priority_schedule.priority IS 'Priorité définie pour cette tranche horaire';


--
-- Name: COLUMN priority_schedule.id_heater; Type: COMMENT; Schema: automation; Owner: automation_p
--

COMMENT ON COLUMN priority_schedule.id_heater IS 'Chauffage concerné par cette tranche horaire';


--
-- Name: COLUMN priority_schedule.day_of_week; Type: COMMENT; Schema: automation; Owner: automation_p
--

COMMENT ON COLUMN priority_schedule.day_of_week IS 'Jour de la semaine';


--
-- Name: priority_schedule_id_seq; Type: SEQUENCE; Schema: automation; Owner: automation_p
--

CREATE SEQUENCE priority_schedule_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE priority_schedule_id_seq OWNER TO automation_p;

--
-- Name: priority_schedule_id_seq; Type: SEQUENCE OWNED BY; Schema: automation; Owner: automation_p
--

ALTER SEQUENCE priority_schedule_id_seq OWNED BY priority_schedule.id_priority_schedule;


--
-- Name: room; Type: TABLE; Schema: automation; Owner: automation_p
--

CREATE TABLE room (
    id_room integer NOT NULL,
    name character varying(30) NOT NULL,
    id_sensor bigint NOT NULL,
    mqtt_topic character varying(255) NOT NULL,
    influxdb_measurement character varying(50) NOT NULL
);


ALTER TABLE room OWNER TO automation_p;

--
-- Name: TABLE room; Type: COMMENT; Schema: automation; Owner: automation_p
--

COMMENT ON TABLE room IS 'Piece de la maison';


--
-- Name: COLUMN room.id_room; Type: COMMENT; Schema: automation; Owner: automation_p
--

COMMENT ON COLUMN room.id_room IS 'Identifiant de la pièce';


--
-- Name: COLUMN room.name; Type: COMMENT; Schema: automation; Owner: automation_p
--

COMMENT ON COLUMN room.name IS 'Nom de la piece';


--
-- Name: COLUMN room.mqtt_topic; Type: COMMENT; Schema: automation; Owner: automation_p
--

COMMENT ON COLUMN room.mqtt_topic IS 'Mqtt topic (to publish room data)';


--
-- Name: COLUMN room.influxdb_measurement; Type: COMMENT; Schema: automation; Owner: automation_p
--

COMMENT ON COLUMN room.influxdb_measurement IS 'nom de la table InfluxDB';


--
-- Name: room_heater; Type: TABLE; Schema: automation; Owner: automation_p
--

CREATE TABLE room_heater (
    id_room bigint NOT NULL,
    id_heater bigint NOT NULL
);


ALTER TABLE room_heater OWNER TO automation_p;

--
-- Name: room_id_seq; Type: SEQUENCE; Schema: automation; Owner: automation_p
--

CREATE SEQUENCE room_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE room_id_seq OWNER TO automation_p;

--
-- Name: room_id_seq; Type: SEQUENCE OWNED BY; Schema: automation; Owner: automation_p
--

ALTER SEQUENCE room_id_seq OWNED BY room.id_room;


--
-- Name: sensor; Type: TABLE; Schema: automation; Owner: automation_p
--

CREATE TABLE sensor (
    id_sensor integer NOT NULL,
    sensor_address character varying(23),
    name character varying(50) NOT NULL,
    tempshift numeric(4,2) NOT NULL
);


ALTER TABLE sensor OWNER TO automation_p;

--
-- Name: TABLE sensor; Type: COMMENT; Schema: automation; Owner: automation_p
--

COMMENT ON TABLE sensor IS 'Table qui contient la liste des capteurs';


--
-- Name: COLUMN sensor.id_sensor; Type: COMMENT; Schema: automation; Owner: automation_p
--

COMMENT ON COLUMN sensor.id_sensor IS 'Identifiant du capteur';


--
-- Name: COLUMN sensor.sensor_address; Type: COMMENT; Schema: automation; Owner: automation_p
--

COMMENT ON COLUMN sensor.sensor_address IS 'Adresse du capteur';


--
-- Name: COLUMN sensor.tempshift; Type: COMMENT; Schema: automation; Owner: automation_p
--

COMMENT ON COLUMN sensor.tempshift IS 'Permet de gérer le décalage entre la température lue par le capteur (celui peut être positionné en bas et ne pas refléter la bonne température)';


--
-- Name: sensor_entry; Type: TABLE; Schema: automation; Owner: automation_p
--

CREATE TABLE sensor_entry (
    id_entry integer NOT NULL,
    id_sensor bigint NOT NULL,
    actual_temp numeric(4,2),
    wanted_temp numeric(4,2),
    humidity numeric(4,2),
    received_date timestamp without time zone NOT NULL
);


ALTER TABLE sensor_entry OWNER TO automation_p;

--
-- Name: sensor_entry_id_entry_seq; Type: SEQUENCE; Schema: automation; Owner: automation_p
--

CREATE SEQUENCE sensor_entry_id_entry_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE sensor_entry_id_entry_seq OWNER TO automation_p;

--
-- Name: sensor_entry_id_entry_seq; Type: SEQUENCE OWNED BY; Schema: automation; Owner: automation_p
--

ALTER SEQUENCE sensor_entry_id_entry_seq OWNED BY sensor_entry.id_entry;


--
-- Name: sensor_id_sensor_seq; Type: SEQUENCE; Schema: automation; Owner: automation_p
--

CREATE SEQUENCE sensor_id_sensor_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE sensor_id_sensor_seq OWNER TO automation_p;

--
-- Name: sensor_id_sensor_seq; Type: SEQUENCE OWNED BY; Schema: automation; Owner: automation_p
--

ALTER SEQUENCE sensor_id_sensor_seq OWNED BY sensor.id_sensor;


--
-- Name: temp_schedule; Type: TABLE; Schema: automation; Owner: automation_p
--

CREATE TABLE temp_schedule (
    id_temp_schedule integer NOT NULL,
    temp numeric(4,2) NOT NULL,
    id_room bigint NOT NULL,
    day_of_week smallint NOT NULL,
    hour_begin time without time zone NOT NULL,
    hour_end time without time zone NOT NULL
);


ALTER TABLE temp_schedule OWNER TO automation_p;

--
-- Name: TABLE temp_schedule; Type: COMMENT; Schema: automation; Owner: automation_p
--

COMMENT ON TABLE temp_schedule IS 'Temp. des chauffages par tranche horaire';


--
-- Name: COLUMN temp_schedule.temp; Type: COMMENT; Schema: automation; Owner: automation_p
--

COMMENT ON COLUMN temp_schedule.temp IS 'Priorité définie pour cette tranche horaire';


--
-- Name: COLUMN temp_schedule.id_room; Type: COMMENT; Schema: automation; Owner: automation_p
--

COMMENT ON COLUMN temp_schedule.id_room IS 'Temp de la piece à contrôler pour cette tranche horaire';


--
-- Name: COLUMN temp_schedule.day_of_week; Type: COMMENT; Schema: automation; Owner: automation_p
--

COMMENT ON COLUMN temp_schedule.day_of_week IS 'Jour de la  semaine (2=lundi, Saturday=7, dimanche=1)';


--
-- Name: temp_schedule_id_seq; Type: SEQUENCE; Schema: automation; Owner: automation_p
--

CREATE SEQUENCE temp_schedule_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE temp_schedule_id_seq OWNER TO automation_p;

--
-- Name: temp_schedule_id_seq; Type: SEQUENCE OWNED BY; Schema: automation; Owner: automation_p
--

ALTER SEQUENCE temp_schedule_id_seq OWNED BY temp_schedule.id_temp_schedule;


--
-- Name: trame_teleinfo; Type: TABLE; Schema: automation; Owner: automation_p
--

CREATE TABLE trame_teleinfo (
    date_reception timestamp without time zone NOT NULL,
    adco character varying(12) NOT NULL,
    optarif character varying(4) NOT NULL,
    isousc smallint NOT NULL,
    hchc integer NOT NULL,
    hchp integer NOT NULL,
    ptec character varying(4) NOT NULL,
    iinst1 smallint NOT NULL,
    iinst2 smallint NOT NULL,
    iinst3 smallint NOT NULL,
    imax1 smallint NOT NULL,
    imax2 smallint NOT NULL,
    imax3 smallint NOT NULL,
    pmax integer NOT NULL,
    papp integer NOT NULL,
    hhphc character varying(1) NOT NULL,
    motdetat character varying(6) NOT NULL,
    ppot character varying(2) NOT NULL
);


ALTER TABLE trame_teleinfo OWNER TO automation_p;

--
-- Name: TABLE trame_teleinfo; Type: COMMENT; Schema: automation; Owner: automation_p
--

COMMENT ON TABLE trame_teleinfo IS 'Stockage des trames de teleinfo en provenance du compteur EDF';


--
-- Name: COLUMN trame_teleinfo.date_reception; Type: COMMENT; Schema: automation; Owner: automation_p
--

COMMENT ON COLUMN trame_teleinfo.date_reception IS 'Timestamp de réception de la trame Teleinfo';


--
-- Name: COLUMN trame_teleinfo.adco; Type: COMMENT; Schema: automation; Owner: automation_p
--

COMMENT ON COLUMN trame_teleinfo.adco IS 'Adresse du concentrateur de téléreport';


--
-- Name: COLUMN trame_teleinfo.optarif; Type: COMMENT; Schema: automation; Owner: automation_p
--

COMMENT ON COLUMN trame_teleinfo.optarif IS 'Option tarifaire choisie';


--
-- Name: COLUMN trame_teleinfo.isousc; Type: COMMENT; Schema: automation; Owner: automation_p
--

COMMENT ON COLUMN trame_teleinfo.isousc IS 'Intensité souscrite en Ampère';


--
-- Name: id_heater; Type: DEFAULT; Schema: automation; Owner: automation_p
--

ALTER TABLE ONLY heater ALTER COLUMN id_heater SET DEFAULT nextval('heater_id_seq'::regclass);


--
-- Name: id_priority_schedule; Type: DEFAULT; Schema: automation; Owner: automation_p
--

ALTER TABLE ONLY priority_schedule ALTER COLUMN id_priority_schedule SET DEFAULT nextval('priority_schedule_id_seq'::regclass);


--
-- Name: id_room; Type: DEFAULT; Schema: automation; Owner: automation_p
--

ALTER TABLE ONLY room ALTER COLUMN id_room SET DEFAULT nextval('room_id_seq'::regclass);


--
-- Name: id_sensor; Type: DEFAULT; Schema: automation; Owner: automation_p
--

ALTER TABLE ONLY sensor ALTER COLUMN id_sensor SET DEFAULT nextval('sensor_id_sensor_seq'::regclass);


--
-- Name: id_entry; Type: DEFAULT; Schema: automation; Owner: automation_p
--

ALTER TABLE ONLY sensor_entry ALTER COLUMN id_entry SET DEFAULT nextval('sensor_entry_id_entry_seq'::regclass);


--
-- Name: id_temp_schedule; Type: DEFAULT; Schema: automation; Owner: automation_p
--

ALTER TABLE ONLY temp_schedule ALTER COLUMN id_temp_schedule SET DEFAULT nextval('temp_schedule_id_seq'::regclass);


--
-- Name: id_priority_schedule; Type: CONSTRAINT; Schema: automation; Owner: automation_p
--

ALTER TABLE ONLY priority_schedule
    ADD CONSTRAINT id_priority_schedule PRIMARY KEY (id_priority_schedule);


--
-- Name: id_temp_schedule; Type: CONSTRAINT; Schema: automation; Owner: automation_p
--

ALTER TABLE ONLY temp_schedule
    ADD CONSTRAINT id_temp_schedule PRIMARY KEY (id_temp_schedule);


--
-- Name: idx_id_heater_day_of_week_hour_begin_hour_end; Type: CONSTRAINT; Schema: automation; Owner: automation_p
--

ALTER TABLE ONLY priority_schedule
    ADD CONSTRAINT idx_id_heater_day_of_week_hour_begin_hour_end UNIQUE (id_heater, day_of_week, hour_begin, hour_end);


--
-- Name: idx_id_room_day_of_week_hour_begin_hour_end; Type: CONSTRAINT; Schema: automation; Owner: automation_p
--

ALTER TABLE ONLY temp_schedule
    ADD CONSTRAINT idx_id_room_day_of_week_hour_begin_hour_end UNIQUE (id_room, day_of_week, hour_begin, hour_end);


--
-- Name: pk_heater_id; Type: CONSTRAINT; Schema: automation; Owner: automation_p
--

ALTER TABLE ONLY heater
    ADD CONSTRAINT pk_heater_id PRIMARY KEY (id_heater);


--
-- Name: pk_id_entry; Type: CONSTRAINT; Schema: automation; Owner: automation_p
--

ALTER TABLE ONLY sensor_entry
    ADD CONSTRAINT pk_id_entry PRIMARY KEY (id_entry);


--
-- Name: pk_id_sensor; Type: CONSTRAINT; Schema: automation; Owner: automation_p
--

ALTER TABLE ONLY sensor
    ADD CONSTRAINT pk_id_sensor PRIMARY KEY (id_sensor);


--
-- Name: pk_room_heater; Type: CONSTRAINT; Schema: automation; Owner: automation_p
--

ALTER TABLE ONLY room_heater
    ADD CONSTRAINT pk_room_heater PRIMARY KEY (id_room, id_heater);


--
-- Name: pk_room_id; Type: CONSTRAINT; Schema: automation; Owner: automation_p
--

ALTER TABLE ONLY room
    ADD CONSTRAINT pk_room_id PRIMARY KEY (id_room);


--
-- Name: pk_trame_teleinfo; Type: CONSTRAINT; Schema: automation; Owner: automation_p
--

ALTER TABLE ONLY trame_teleinfo
    ADD CONSTRAINT pk_trame_teleinfo PRIMARY KEY (date_reception);


--
-- Name: idx_dater_reception; Type: INDEX; Schema: automation; Owner: automation_p
--

CREATE INDEX idx_dater_reception ON trame_teleinfo USING btree (date_reception);


--
-- Name: fk_heater_priority_schedule; Type: FK CONSTRAINT; Schema: automation; Owner: automation_p
--

ALTER TABLE ONLY priority_schedule
    ADD CONSTRAINT fk_heater_priority_schedule FOREIGN KEY (id_heater) REFERENCES heater(id_heater);


--
-- Name: fk_heater_room_heater; Type: FK CONSTRAINT; Schema: automation; Owner: automation_p
--

ALTER TABLE ONLY room_heater
    ADD CONSTRAINT fk_heater_room_heater FOREIGN KEY (id_heater) REFERENCES heater(id_heater);


--
-- Name: fk_room_room_heater; Type: FK CONSTRAINT; Schema: automation; Owner: automation_p
--

ALTER TABLE ONLY room_heater
    ADD CONSTRAINT fk_room_room_heater FOREIGN KEY (id_room) REFERENCES room(id_room);


--
-- Name: fk_room_sensor; Type: FK CONSTRAINT; Schema: automation; Owner: automation_p
--

ALTER TABLE ONLY room
    ADD CONSTRAINT fk_room_sensor FOREIGN KEY (id_sensor) REFERENCES sensor(id_sensor);


--
-- Name: fk_room_temp_schedule; Type: FK CONSTRAINT; Schema: automation; Owner: automation_p
--

ALTER TABLE ONLY temp_schedule
    ADD CONSTRAINT fk_room_temp_schedule FOREIGN KEY (id_room) REFERENCES room(id_room);


--
-- Name: fk_sensor_sensor_entry; Type: FK CONSTRAINT; Schema: automation; Owner: automation_p
--

ALTER TABLE ONLY sensor_entry
    ADD CONSTRAINT fk_sensor_sensor_entry FOREIGN KEY (id_sensor) REFERENCES sensor(id_sensor);


--
-- Name: public; Type: ACL; Schema: -; Owner: postgres
--

REVOKE ALL ON SCHEMA public FROM PUBLIC;
REVOKE ALL ON SCHEMA public FROM postgres;
GRANT ALL ON SCHEMA public TO postgres;
GRANT ALL ON SCHEMA public TO PUBLIC;


--
-- PostgreSQL database dump complete
--

