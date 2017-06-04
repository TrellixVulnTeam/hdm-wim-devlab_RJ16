package de.hdm.wim.Helper;

import com.google.api.gax.grpc.ExecutorProvider;
import com.google.api.gax.grpc.InstantiatingExecutorProvider;
import com.google.cloud.ServiceOptions;
import com.google.cloud.pubsub.spi.v1.MessageReceiver;
import com.google.cloud.pubsub.spi.v1.PagedResponseWrappers;
import com.google.cloud.pubsub.spi.v1.Subscriber;
import com.google.cloud.pubsub.spi.v1.SubscriptionAdminClient;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.pubsub.v1.ListSubscriptionsRequest;
import com.google.pubsub.v1.ProjectName;
import com.google.pubsub.v1.PushConfig;
import com.google.pubsub.v1.Subscription;
import com.google.pubsub.v1.SubscriptionName;
import com.google.pubsub.v1.TopicName;
import de.hdm.wim.sharedLib.Constants.Config;
import org.apache.log4j.Logger;

/**
 * Created by ben on 04/06/2017.
 */
public class SubscriptionHelper {

	private static final Logger _logger = Logger.getLogger(SubscriptionHelper.class);
	private final String _projectId;
	private String _pushEndpoint;

	/**
	 * Instantiates a new TopicHelper.
	 */
	public SubscriptionHelper() {

		this._projectId = ServiceOptions.getDefaultProjectId();
	}

	/**
	 * Instantiates a new SubscriptionHelper.
	 *
	 * @param projectId unique project identifier, eg. "my-project-id"
	 */
	public SubscriptionHelper(String projectId){
		this._projectId = projectId;
	}

	/**
	 * Gets project id.
	 *
	 * @return the project id
	 */
	public String getProjectId() {
		return _projectId;
	}

	/**
	 * Create a pull subscription to a specif topic.
	 *
	 * @param topicName the topic name
	 * @param subscriptionId the subscription id, eg. "my-test-subscription"
	 * @return the subscription
	 * @throws Exception the exception
	 */
	public Subscription createPullSubscriptionIfNotExists(TopicName topicName, String subscriptionId) throws Exception{

		try (SubscriptionAdminClient subscriptionAdminClient = SubscriptionAdminClient.create()) {

			Iterable<Subscription> subscriptions 	= getSubscriptions();

			for(Subscription subscription : subscriptions){
				if(subscription.getNameAsSubscriptionName().getSubscription().equals(subscriptionId))
				{
					_logger.info("Subscription already exists: " + subscriptionId);
					return subscription;
				}
			}

			SubscriptionName subscriptionName =	SubscriptionName.create(_projectId, subscriptionId);

			// create a pull subscription with default acknowledgement deadline
			Subscription subscription = subscriptionAdminClient.createSubscription(
				subscriptionName,
				topicName,
				PushConfig.getDefaultInstance(),
				10
			);

			/*PushConfig pushConfig = PushConfig.newBuilder().setPushEndpoint("").build();
			pushConfig.*/


			_logger.info("Successfully created subscription: " + subscriptionId);

			return subscription;
		}
	}


	/**
	 * Create a push subscription to a specif topic.
	 *
	 * @param topicName the topic name
	 * @param subscriptionId the subscription id, eg. "my-test-subscription"
	 * @return the subscription
	 * @throws Exception the exception
	 */
	public Subscription createPushSubscriptionIfNotExists(TopicName topicName, String subscriptionId) throws Exception{

		try (SubscriptionAdminClient subscriptionAdminClient = SubscriptionAdminClient.create()) {

			Iterable<Subscription> subscriptions = getSubscriptions();

			for(Subscription subscription : subscriptions){
				if(subscription.getNameAsSubscriptionName().getSubscription().equals(subscriptionId))
				{
					_logger.info("Subscription already exists: " + subscriptionId);
					return subscription;
				}
			}

			_pushEndpoint 						= String.format(
													"https://%s.appspot.com/pubsub/push?token=%s&topic=%s",
													_projectId,
													Config.secretToken,
													topicName.getTopic()
			                                      );
			SubscriptionName subscriptionName 	= SubscriptionName.create(_projectId, subscriptionId);
			PushConfig pushConfig 				= PushConfig.newBuilder().setPushEndpoint(_pushEndpoint).build();

			// create a push subscription with default acknowledgement deadline
			Subscription subscription = subscriptionAdminClient.createSubscription(
				subscriptionName,
				topicName,
				pushConfig,
				10
			);

			_logger.info("Successfully created push subscription: " + subscriptionId);

			return subscription;
		}
	}

	/**
	 * Gets subscriptions.
	 *
	 * @return the subscriptions
	 * @throws Exception the exception
	 */
	public Iterable<Subscription> getSubscriptions() throws Exception {

		try (SubscriptionAdminClient subscriptionAdminClient = SubscriptionAdminClient.create()) {

			ListSubscriptionsRequest listSubscriptionsRequest =
				ListSubscriptionsRequest.newBuilder()
					.setProjectWithProjectName(ProjectName.create(_projectId))
					.build();

			PagedResponseWrappers.ListSubscriptionsPagedResponse response = subscriptionAdminClient
				.listSubscriptions(listSubscriptionsRequest);
			Iterable<Subscription> subscriptions = response.iterateAll();
			for (Subscription subscription : subscriptions) {
				_logger.info(subscription.getName());
			}
			return subscriptions;
		}
	}

	/**
	 * Create subscriber.
	 *
	 * @param subscriptionId the subscription id
	 * @throws Exception the exception
	 */
	public void createSubscriber(String subscriptionId) throws Exception{

		SubscriptionName subscriptionName = SubscriptionName.create(_projectId, subscriptionId);

		ExecutorProvider executorProvider =	InstantiatingExecutorProvider.newBuilder()
			.setExecutorThreadCount(1)
			.build();

		// Instantiate an asynchronous message receiver
		MessageReceiver receiver = (message, consumer) ->{
			// handle incoming message, then ack/nack the received message
			_logger.info("Id : " + message.getMessageId());
			_logger.info("Data : " + message.getData().toStringUtf8());
			consumer.ack();
		};

		Subscriber subscriber = null;
		try {
			// Create a subscriber bound to the message receiver
			subscriber = Subscriber
				.defaultBuilder(subscriptionName, receiver)
				.setExecutorProvider(executorProvider)
				.build();

			subscriber.addListener(
				new Subscriber.Listener() {
					@Override
					public void failed(Subscriber.State from, Throwable failure) {
						// Handle failure. This is called when the Subscriber encountered a fatal error and is shutting down.
						_logger.error(failure);
					}
				},
				MoreExecutors.directExecutor()
			);

			subscriber.startAsync().awaitRunning();

			Thread.sleep(30000); //5 Minutes = 300000
		} catch (InterruptedException e){
			e.printStackTrace();
		} finally {
			// stop receiving messages
			if (subscriber != null) {
				subscriber.stopAsync();
			}
		}
	}

	public String get_pushEndpoint() {
		return _pushEndpoint;
	}
}
