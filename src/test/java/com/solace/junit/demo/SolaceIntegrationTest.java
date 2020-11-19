package com.solace.junit.demo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Logger;

import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.solace.junit.AbstractPubSubPlusTestCase;
import com.solacesystems.jcsmp.BytesXMLMessage;
import com.solacesystems.jcsmp.InvalidPropertiesException;
import com.solacesystems.jcsmp.JCSMPException;
import com.solacesystems.jcsmp.JCSMPFactory;
import com.solacesystems.jcsmp.JCSMPProperties;
import com.solacesystems.jcsmp.JCSMPSession;
import com.solacesystems.jcsmp.JCSMPStreamingPublishEventHandler;
import com.solacesystems.jcsmp.TextMessage;
import com.solacesystems.jcsmp.Topic;
import com.solacesystems.jcsmp.XMLMessageConsumer;
import com.solacesystems.jcsmp.XMLMessageListener;
import com.solacesystems.jcsmp.XMLMessageProducer;

public class SolaceIntegrationTest extends AbstractPubSubPlusTestCase {

	private static final Logger LOGGER = java.util.logging.Logger
			.getLogger(SolaceIntegrationTest.class.getCanonicalName());
	private static JCSMPSession session;

	@BeforeClass
	public static void connectSMF() {
		final JCSMPProperties properties = new JCSMPProperties();
		properties.setProperty(JCSMPProperties.HOST, String.format("%s:%s", getHost(), getSMFPort())); // host:port
		properties.setProperty(JCSMPProperties.USERNAME, "default"); // client-username
		properties.setProperty(JCSMPProperties.VPN_NAME, "default"); // message-vpn
		try {
			session = JCSMPFactory.onlyInstance().createSession(properties);
		} catch (InvalidPropertiesException e) {
			throw new RuntimeException(e);
		}

		try {
			session.connect();
		} catch (JCSMPException e) {
			throw new RuntimeException(e);
		}
	}

	@AfterClass
	public static void tearDown() {
		if (session != null && !session.isClosed()) {
			session.closeSession();
		}
	}

	@Test
	public void publishToTopicSMF() {
		final Topic topic = JCSMPFactory.onlyInstance().createTopic("tutorial/topic");
		try {
			/** Anonymous inner-class for handling publishing events */
			XMLMessageProducer prod = session.getMessageProducer(new JCSMPStreamingPublishEventHandler() {
				@Override
				public void responseReceived(String messageID) {
					LOGGER.info("Producer received response for msg: " + messageID);
				}

				@Override
				public void handleError(String messageID, JCSMPException e, long timestamp) {
					LOGGER.info(
							String.format("Producer received error for msg: %s@%s - %s%n", messageID, timestamp, e));
				}
			});
			// Publish-only session is now hooked up and running!

			TextMessage msg = JCSMPFactory.onlyInstance().createMessage(TextMessage.class);
			final String text = "Hello world!";
			msg.setText(text);
			LOGGER.info(
					String.format("Connected. About to send message '%s' to topic '%s'...%n", text, topic.getName()));
			prod.send(msg, topic);
		} catch (JCSMPException e1) {
			e1.printStackTrace();
			fail(e1.getMessage());
		}
		LOGGER.info("Message sent. Exiting.");
	}

	@Test
	public void publishSubscribeSMF() {
		final Topic topic = JCSMPFactory.onlyInstance().createTopic("fire/forget");
		try {
			final CountDownLatch latch = new CountDownLatch(1); // used for
			// synchronizing b/w threads
			/**
			 * Anonymous inner-class for MessageListener This demonstrates the async
			 * threaded message callback
			 */
			final XMLMessageConsumer cons = session.getMessageConsumer(new XMLMessageListener() {
				@Override
				public void onReceive(BytesXMLMessage msg) {
					if (msg instanceof TextMessage) {
						LOGGER.info(String.format("TextMessage received: '%s'%n", ((TextMessage) msg).getText()));
						assertEquals("Hello subscriber!", ((TextMessage) msg).getText());
					} else {
						LOGGER.info("Message received.");
						LOGGER.info(String.format("Message Dump:%n%s%n", msg.dump()));
					}
					latch.countDown(); // unblock main thread
				}

				@Override
				public void onException(JCSMPException e) {
					LOGGER.info(String.format("Consumer received exception: %s%n", e));
					latch.countDown(); // unblock main thread
				}
			});
			session.addSubscription(topic);
			LOGGER.info("Connected. Awaiting message...");
			cons.start();
			// Consume-only session is now hooked up and running!

			/** Anonymous inner-class for handling publishing events */
			XMLMessageProducer prod = session.getMessageProducer(new JCSMPStreamingPublishEventHandler() {
				@Override
				public void responseReceived(String messageID) {
					LOGGER.info("Producer received response for msg: " + messageID);
				}

				@Override
				public void handleError(String messageID, JCSMPException e, long timestamp) {
					LOGGER.info(
							String.format("Producer received error for msg: %s@%s - %s%n", messageID, timestamp, e));
				}
			});
			// Publish-only session is now hooked up and running!

			TextMessage msg = JCSMPFactory.onlyInstance().createMessage(TextMessage.class);
			final String text = "Hello subscriber!";
			msg.setText(text);
			LOGGER.info(
					String.format("Connected. About to send message '%s' to topic '%s'...%n", text, topic.getName()));
			prod.send(msg, topic);
			try {
				latch.await(); // block here until message received, and latch will flip
			} catch (InterruptedException e) {
				LOGGER.info("I was awoken while waiting");
			}
			// Close consumer
			cons.close();
			// now send a message
		} catch (JCSMPException e1) {
			e1.printStackTrace();
			fail(e1.getMessage());
		}
	}

	@Test
	public void httpSend() throws IOException {
		final Topic topic = JCSMPFactory.onlyInstance().createTopic("http/topic");
		try {
			final CountDownLatch latch = new CountDownLatch(1); // used for
			// synchronizing b/w threads
			/**
			 * Anonymous inner-class for MessageListener This demonstrates the async
			 * threaded message callback
			 */
			final XMLMessageConsumer cons = session.getMessageConsumer(new XMLMessageListener() {
				@Override
				public void onReceive(BytesXMLMessage msg) {
					if (msg instanceof TextMessage) {
						LOGGER.info(String.format("TextMessage received: '%s'%n", ((TextMessage) msg).getText()));
						assertEquals("{\"hello\": \"world\"}", ((TextMessage) msg).getText());
					} else {
						LOGGER.info("Message received.");
						LOGGER.info(String.format("Message Dump:%n%s%n", msg.dump()));
					}
					
					latch.countDown(); // unblock main thread
				}

				@Override
				public void onException(JCSMPException e) {
					LOGGER.info(String.format("Consumer received exception: %s%n", e));
					latch.countDown(); // unblock main thread
				}
			});
			session.addSubscription(topic);
			LOGGER.info("Connected. Awaiting message...");
			cons.start();
			// Consume-only session is now hooked up and running!

			String postEndpoint = String.format("http://%s:%s/http/topic", getHost(), getRESTPort());

			CloseableHttpClient httpclient = HttpClients.createDefault();

			HttpPost httpPost = new HttpPost(postEndpoint);

			httpPost.setHeader("Accept", "application/json");
			httpPost.setHeader("Content-type", "application/json");

			String inputJson = "{\"hello\": \"world\"}";

			StringEntity stringEntity = new StringEntity(inputJson);
			httpPost.setEntity(stringEntity);

			LOGGER.info("Executing request " + httpPost.getRequestLine());

			CloseableHttpResponse response = httpclient.execute(httpPost);

			// Throw runtime exception if status code isn't 200
			if (response.getStatusLine().getStatusCode() != 200) {
				fail("Could not send message via http");
			}
			try {
				latch.await(); // block here until message received, and latch will flip
			} catch (InterruptedException e) {
				LOGGER.info("I was awoken while waiting");
			}
			// Close consumer
			cons.close();
			// now send a message
		} catch (JCSMPException e1) {
			e1.printStackTrace();
			fail(e1.getMessage());
		}
	}

	@Test
	public void sempRESTCall() throws ClientProtocolException, IOException {
		String getEndpoint = String.format("http://%s:%s/SEMP/v2/config/msgVpns", getHost(), getSEMPPort());
		CredentialsProvider provider = new BasicCredentialsProvider();
		provider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(getAdminUser(), getAdminPassword()));
		CloseableHttpClient httpclient = HttpClientBuilder.create().setDefaultCredentialsProvider(provider).build();

		HttpGet httpGet = new HttpGet(getEndpoint);
		CloseableHttpResponse response = httpclient.execute(httpGet);
		assertTrue(response.getStatusLine().getStatusCode() == 200);
		String msgVpns = EntityUtils.toString(response.getEntity()); 
		assertTrue(msgVpns.contains("default"));
		LOGGER.info(msgVpns.substring(0, 256));
	}
}
