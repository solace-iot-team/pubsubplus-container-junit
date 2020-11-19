package com.solace.junit;

import java.util.logging.Logger;

import org.testcontainers.containers.GenericContainer;


public abstract class AbstractPubSubPlusTestCase {
	private static final int SEMP_PORT = 8080;
	private static final int REST_PORT = 9000;
	private static final int MQTT_WEB_PORT = 8000;
	private static final int MQTT_PORT = 1883;
	private static final int AMQP_PORT = 5672;
	private static final int WEB_TRANSPORT = 8008;
	private static final int SMF_PORT = 55555;
	private static final GenericContainer<?> solace;
	private static final String ADMIN_USER = "admin";
	private static final String ADMIN_PASSWORD = "admin";
	
	private static final Logger LOGGER = java.util.logging.Logger.getLogger(AbstractPubSubPlusTestCase.class.getCanonicalName());
	
	
	public static final String getHost() {
		return solace.getContainerIpAddress();
	}
	public static final Integer getSMFPort() {
		return solace.getMappedPort(SMF_PORT);
	}
	public static final Integer getWebTransport() {
		return solace.getMappedPort(WEB_TRANSPORT);
	}
	public static final Integer getAMQPPort() {
		return solace.getMappedPort(AMQP_PORT);
	}
	public static final Integer getMQTTPort() {
		return solace.getMappedPort(MQTT_PORT);
	}
	public static final Integer getMQTTWebPort() {
		return solace.getMappedPort(MQTT_WEB_PORT);
	}
	public static final Integer getRESTPort() {
		return solace.getMappedPort(REST_PORT);
	}
	public static final Integer getSEMPPort() {
		return solace.getMappedPort(SEMP_PORT);
	}
	public static final String getAdminUser() {
		return ADMIN_USER;
	}
	public static final String getAdminPassword() {
		return ADMIN_PASSWORD;
	}

	
	
	static {
		LOGGER.info("Starting Solace PubSub+ Docker Container");
		solace = new GenericContainer<>("solace/solace-pubsub-standard:latest").withExposedPorts(SMF_PORT, WEB_TRANSPORT, AMQP_PORT, MQTT_PORT, MQTT_WEB_PORT, REST_PORT, SEMP_PORT)
				.withSharedMemorySize(1000000000L).withEnv("username_admin_globalaccesslevel", ADMIN_USER)
				.withEnv("username_admin_password", ADMIN_PASSWORD).withEnv("system_scaling_maxconnectioncount", "100");
		solace.start();

		LOGGER.info(String.format("Started Solace PubSub+ Docker Container, available on host [%s], SMF port [%d]", solace.getContainerIpAddress(), solace.getMappedPort(SMF_PORT)));
	}

}
