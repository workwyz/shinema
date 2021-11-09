package cn.shinema.core.port.adapter.publisher;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;
import org.apache.curator.framework.recipes.locks.InterProcessLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.shinema.core.lock.ZkLock;
import cn.shinema.core.notification.NotificationPublisher;
import cn.shinema.core.notification.NotificationGateway;

public class EventPublisherJob implements Runnable {
	private static Logger LOGGER = LoggerFactory.getLogger(EventPublisherJob.class);

	private ZkLock lock;
	private EventPublisherDefine publisherDef;
	private NotificationPublisher notificationPublisher;

	private String trackerName = null;
	private Integer quantity = null;
	private Queue<Long> unPublishQueue = null;
	private NotificationGateway[] messageGatewayTypes = null;

	public EventPublisherJob(ZkLock lock, EventPublisherDefine publisherDef, NotificationPublisher notificationPublisher) {
		this.lock = lock;
		this.publisherDef = publisherDef;
		this.notificationPublisher = notificationPublisher;
	}

	public void run() {
		trackerName = publisherDef.trackerName();
		quantity = publisherDef.quantity();
		unPublishQueue = publisherDef.unPublishQueue();
		messageGatewayTypes = publisherDef.notifyTypes();

		InterProcessLock process = null;

		try {
			publisherDef.publishing();

			process = lock.tryLock(trackerName);
			if (process.acquire(ZkLock.waitTimeOut, TimeUnit.SECONDS)) {
//				LOGGER.info("事件发布： ## OccurredClass=>{},TrackerName=>{}, #Quantity=>{}", occurredClass, TrackerName, quantity);
				postMessage();
			}
		} catch (Exception e) {
			LOGGER.warn("Lock Get Fail :{}", e.getMessage());
		} finally {
			if (null != process) {
				try {
					process.release();
				} catch (Exception e) {
					LOGGER.error("Lock Release Error :{}", e.getMessage());
				}
			}
			publisherDef.published();
		}

	}

	private void postMessage() {
		List<Long> eventIds = new ArrayList<Long>();

		try {
			if (!unPublishQueue.isEmpty()) {
				for (int i = 0; i < quantity; i++) {
					Long eventId = unPublishQueue.poll();
					if (null != eventId) {
						eventIds.add(eventId);
					}
				}

				Collection<Long> publishedIds = notificationPublisher.publishNotifications(trackerName, eventIds, messageGatewayTypes);

				if (null == publishedIds || publishedIds.isEmpty()) {
					LOGGER.error("发布出现异常，归队重发；Event Id => {}", StringUtils.join(eventIds, ","));
					eventIds.stream().forEach(v -> {
						unPublishQueue.add(v);
					});
				} else {
					Collection<Long> unpubishIds = eventIds.stream().filter(id -> !publishedIds.contains(id)).collect(Collectors.toList());
					if (null != unpubishIds && !unpubishIds.isEmpty()) {
						LOGGER.error("发布出现异常，归队重发；Event Id => {}", StringUtils.join(unpubishIds, ","));
						unpubishIds.stream().forEach(v -> {
							unPublishQueue.add(v);
						});
					}
				}
			}
		} catch (Exception e) {
			LOGGER.error("Publish Notification Error :", e.getMessage());
			LOGGER.error("发布出现异常，归队重发；Event Id => {}", StringUtils.join(eventIds, ","));
			eventIds.stream().forEach(v -> {
				unPublishQueue.add(v);
			});
		}
	}
}
