package cn.shinema.core.port.adapter.publisher;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import javax.transaction.Transactional;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;

import cn.shinema.core.domain.DomainEvent;
import cn.shinema.core.event.StoredEvent;
import cn.shinema.core.event.StoredEventRepository;
import cn.shinema.core.notification.NotificationGatewayService;
import cn.shinema.core.notification.Notification;
import cn.shinema.core.notification.NotificationPublisher;
import cn.shinema.core.notification.NotificationGateway;
import cn.shinema.core.notification.PublishedNotificationTracker;
import cn.shinema.core.notification.PublishedNotificationTrackerRepository;

@ConditionalOnBean(NotificationGatewayService.class)
public class GenericNotificationPublisher implements NotificationPublisher {
	private static Logger LOGGER = LoggerFactory.getLogger(GenericNotificationPublisher.class);

	@Autowired
	private StoredEventRepository storedEventRepository;

	@Autowired
	private List<NotificationGatewayService> notificationGatewayServices;

	@Autowired
	private PublishedNotificationTrackerRepository publishedNotificationTrackerRepository;

	public Collection<Long> publishNotifications(String aTrackerName, int quantity) throws Exception {
		return publishNotifications(aTrackerName, quantity, new NotificationGateway[] { NotificationGateway.ALL });
	}

	public Collection<Long> publishNotifications(String aTrackerName, int quantity, NotificationGateway[] messageGatewayTypes) throws Exception {
		PublishedNotificationTracker publishedNotificationTracker = publishedNotificationTrackerRepository.findByTrackerName(aTrackerName);

		List<StoredEvent> storedEvents = null;
		Pageable pageable = PageRequest.of(0, quantity, Sort.by(Direction.ASC, "eventId"));

		if (publishedNotificationTracker == null) {
			publishedNotificationTracker = new PublishedNotificationTracker(aTrackerName);
		}

		storedEvents = storedEventRepository.findByTrackerNameAndSendStatus(aTrackerName, 0, pageable);

		return publishAndRefresh(publishedNotificationTracker, messageGatewayTypes, storedEvents);
	}

	@Transactional(rollbackOn = { Throwable.class })
	public Collection<Long> publishNotifications(String aTrackerName, List<Long> eventIds, NotificationGateway[] messageGatewayTypes) throws Exception {

		Collection<Long> publishIds = new ArrayList<Long>();
		Collection<Long> storedIds = new ArrayList<Long>();
		PublishedNotificationTracker publishedNotificationTracker = publishedNotificationTrackerRepository.findByTrackerName(aTrackerName);

		if (publishedNotificationTracker == null) {
			publishedNotificationTracker = new PublishedNotificationTracker(aTrackerName);
		}

		Pageable pageable = PageRequest.of(0, eventIds.size(), Sort.by(Direction.ASC, "eventId"));
		List<StoredEvent> storedEvents = storedEventRepository.findByEventIdIn(eventIds, pageable);
		storedEvents.forEach(v -> {
			storedIds.add(v.getEventId());
			if (v.sendStatus() == 1) {
				publishIds.add(v.getEventId());
			}
		});

		Collection<Long> unstoredIds = eventIds.stream().filter(id -> !storedIds.contains(id)).collect(Collectors.toList());

		publishIds.addAll(unstoredIds);

		List<StoredEvent> publishStoredEvents = storedEvents.stream().filter(s -> s.sendStatus() == 0).collect(Collectors.toList());

		List<Long> _publishIds = publishAndRefresh(publishedNotificationTracker, messageGatewayTypes, publishStoredEvents);
		if (null != _publishIds) {
			publishIds.addAll(_publishIds);
		}

		return publishIds;
	}

	private void trackMostRecentPublishedNotification(PublishedNotificationTracker aPublishedNotificationTracker, long mostRecentId) {
		aPublishedNotificationTracker.setMostRecentPublishedEventId(mostRecentId);
		publishedNotificationTrackerRepository.save(aPublishedNotificationTracker);
	}

	private List<Long> publishAndRefresh(PublishedNotificationTracker publishedNotificationTracker, NotificationGateway[] messageGatewayTypes, List<StoredEvent> storedEvents)
			throws Exception {
		if (null == storedEvents || storedEvents.isEmpty()) {
			return new ArrayList<Long>(0);
		}

		List<Long> sendNotifyIds = null;
		List<Notification> notifications = this.notificationsFrom(storedEvents);

		try {
			sendNotifyIds = this.fitlerPublishGateway(notifications, messageGatewayTypes);

			this.updateNotifyStatus(sendNotifyIds);

			Long maxNotifyId = Collections.max(sendNotifyIds);
			if (maxNotifyId > publishedNotificationTracker.mostRecentPublishedEventId()) {
//				LOGGER.info("Update Most Notification Id => {}", maxNotifyId);
				trackMostRecentPublishedNotification(publishedNotificationTracker, maxNotifyId);
			}

//			sendNotifyIds.stream().map(Object::toString).collect(Collectors.toList()).stream().collect(Collectors.joining(","));

			String sendNotifyIdsStr = StringUtils.join(sendNotifyIds, ",");
			LOGGER.info("notification send success. tracker name --> {} ,notification ids --> {}", publishedNotificationTracker.trackerName(), sendNotifyIdsStr);

			return sendNotifyIds;
		} catch (Exception e) {
			throw e;
		}
	}

	private List<Notification> notificationsFrom(List<StoredEvent> aStoredEvents) {
		List<Notification> notifications = new ArrayList<Notification>(aStoredEvents.size());

		for (StoredEvent storedEvent : aStoredEvents) {
			DomainEvent domainEvent = storedEvent.toDomainEvent();
			Notification notification = new Notification(storedEvent.getEventId(), storedEvent.trackerName(), storedEvent.occurredClass(), domainEvent);
			notifications.add(notification);
		}

		return notifications;
	}

	private List<Long> fitlerPublishGateway(List<Notification> notifications, NotificationGateway[] messageGatewayTypes) {
		List<Long> sendNotifyIds = new ArrayList<Long>();

		if (supportNotifyType(NotificationGateway.ALL, messageGatewayTypes)) {
			for (NotificationGatewayService service : notificationGatewayServices) {
				sendNotifyIds = bachPublish(service, notifications);
			}
		} else {
			for (NotificationGatewayService service : notificationGatewayServices) {
				if (supportNotifyType(service.setGatewayType(), messageGatewayTypes)) {
					sendNotifyIds = bachPublish(service, notifications);
				}
			}
		}

		return sendNotifyIds;
	}

	private boolean supportNotifyType(NotificationGateway _messageGatewayType, NotificationGateway[] messageGatewayTypes) {
		for (NotificationGateway messageGatewayType : messageGatewayTypes) {
			if (messageGatewayType == _messageGatewayType) {
				return true;
			}
		}
		return false;
	}

	private List<Long> bachPublish(NotificationGatewayService service, List<Notification> notifications) {
		return service.sendMessage(notifications);
	}

	private void updateNotifyStatus(List<Long> notifyIds) throws Exception {
		storedEventRepository.batchUpdateByIds(notifyIds);
	}

}
