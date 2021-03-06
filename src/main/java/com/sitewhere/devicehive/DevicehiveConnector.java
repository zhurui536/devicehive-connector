/**
 * This file was automatically generated by the Mule Development Kit
 */
package com.sitewhere.devicehive;

import org.apache.log4j.Logger;
import org.mule.api.DefaultMuleException;
import org.mule.api.MuleException;
import org.mule.api.annotations.Configurable;
import org.mule.api.annotations.Connector;
import org.mule.api.annotations.Source;
import org.mule.api.annotations.display.FriendlyName;
import org.mule.api.annotations.lifecycle.Start;
import org.mule.api.annotations.param.Default;
import org.mule.api.annotations.param.Optional;
import org.mule.api.callback.SourceCallback;

import com.devicehive.model.DeviceCommand;
import com.devicehive.model.DeviceNotification;
import com.hazelcast.client.HazelcastClient;
import com.hazelcast.client.config.ClientConfig;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.ITopic;
import com.hazelcast.core.LifecycleEvent;
import com.hazelcast.core.LifecycleEvent.LifecycleState;
import com.hazelcast.core.LifecycleListener;

/**
 * Uses Hazelcast to pull DeviceHive commands and notifications into Mule.
 * 
 * @author Derek Adams
 */
@Connector(name = "devicehive", schemaVersion = "1.0", friendlyName = "DeviceHive")
public class DevicehiveConnector implements LifecycleListener {

	/** Static logger instance */
	private static Logger LOGGER = Logger.getLogger(DevicehiveConnector.class);

	/** Hazelcast client for DeviceHive */
	private HazelcastInstance hazelcast;

	/** Indicates whether connected to Hazelcast */
	private boolean connected = false;

	/**
	 * DeviceHive Hazelcast username.
	 */
	@Optional
	@Configurable
	@Default("DeviceHive")
	@FriendlyName("DeviceHive Hazelcast Username")
	private String hazelcastUsername;

	/**
	 * DeviceHive Hazelcast password.
	 */
	@Optional
	@Configurable
	@Default("DeviceHive")
	@FriendlyName("DeviceHive Hazelcast Password")
	private String hazelcastPassword;

	/**
	 * DeviceHive Hazelcast address.
	 */
	@Optional
	@Configurable
	@Default("localhost:54701")
	@FriendlyName("DeviceHive Hazelcast Address")
	private String hazelcastAddress;

	/**
	 * Called when the connector starts.
	 * 
	 * @throws MuleException
	 */
	@Start
	public void doStart() throws MuleException {
		if (!isConnected()) {
			connect();
		}
	}

	/**
	 * Connect to Hazelcast.
	 * 
	 * @throws MuleException
	 */
	protected void connect() throws MuleException {
		ClientConfig clientConfig = new ClientConfig();
		clientConfig.getGroupConfig().setName(getHazelcastUsername());
		clientConfig.getGroupConfig().setPassword(getHazelcastPassword());
		clientConfig.addAddress(getHazelcastAddress());
		clientConfig.setSmartRouting(true);

		try {
			this.hazelcast = HazelcastClient.newHazelcastClient(clientConfig);
			this.hazelcast.getLifecycleService().addLifecycleListener(this);
			this.connected = true;
			LOGGER.info("Connected to DeviceHive Hazelcast cluster.");
		} catch (Exception e) {
			this.connected = false;
			throw new DefaultMuleException("Unable to connect to DeviceHive Hazelcast cluster.", e);
		}
	}

	/**
	 * Subscribes to Hazelcast messages from DeviceHive for processing in Mule.
	 * 
	 * {@sample.xml ../../../doc/DeviceHive-connector.xml.sample devicehive:subscribe}
	 * 
	 * @param callback
	 *            needed to generate new Mule messages
	 * @throws MuleException
	 *             if not able to connect to Hazelcast.
	 */
	@Source
	public void subscribe(final SourceCallback callback) throws MuleException {
		if (!isConnected()) {
			connect();
		}

		// Subscribe to commands.
		ITopic<DeviceCommand> commands = hazelcast.getTopic("DEVICE_COMMAND");
		commands.addMessageListener(new DeviceHiveCommandProcessor(callback));
		LOGGER.info("Registered for device commands from DeviceHive.");

		// Subscribe to notifications.
		ITopic<DeviceNotification> notifications = hazelcast.getTopic("DEVICE_NOTIFICATION");
		notifications.addMessageListener(new DeviceHiveNotificationProcessor(callback));
		LOGGER.info("Registered for device notifications from DeviceHive.");
	}

	/**
	 * Called when Hazelcast lifecycle changes.
	 * 
	 * @param event
	 */
	@Override
	public void stateChanged(LifecycleEvent event) {
		LOGGER.info("Hazelcast lifecycle changed to '" + event.getState().name() + "'.");
		if (event.getState() == LifecycleState.SHUTDOWN) {
			this.connected = false;
		}
	}

	public String getHazelcastUsername() {
		return hazelcastUsername;
	}

	public void setHazelcastUsername(String hazelcastUsername) {
		this.hazelcastUsername = hazelcastUsername;
	}

	public String getHazelcastPassword() {
		return hazelcastPassword;
	}

	public void setHazelcastPassword(String hazelcastPassword) {
		this.hazelcastPassword = hazelcastPassword;
	}

	public String getHazelcastAddress() {
		return hazelcastAddress;
	}

	public void setHazelcastAddress(String hazelcastAddress) {
		this.hazelcastAddress = hazelcastAddress;
	}

	protected boolean isConnected() {
		return connected;
	}

	protected void setConnected(boolean connected) {
		this.connected = connected;
	}
}