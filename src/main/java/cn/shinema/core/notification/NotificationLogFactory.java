//   Copyright 2012,2013 Vaughn Vernon
//
//   Licensed under the Apache License, Version 2.0 (the "License");
//   you may not use this file except in compliance with the License.
//   You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
//   Unless required by applicable law or agreed to in writing, software
//   distributed under the License is distributed on an "AS IS" BASIS,
//   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//   See the License for the specific language governing permissions and
//   limitations under the License.

package cn.shinema.core.notification;

import java.util.ArrayList;
import java.util.List;

import cn.shinema.core.domain.DomainEvent;
import cn.shinema.core.event.StoredEvent;
import cn.shinema.core.event.StoredEventRepository;

public class NotificationLogFactory {

	// this could be a configuration
	private static final int NOTIFICATIONS_PER_LOG = 20;

	private StoredEventRepository storedEventRepository;

	public static int notificationsPerLog() {
		return NOTIFICATIONS_PER_LOG;
	}

	public NotificationLogFactory() {
		super();

	}

	public NotificationLog createCurrentNotificationLog() {
		return this.createNotificationLog(this.calculateCurrentNotificationLogId());
	}

	public NotificationLog createNotificationLog(NotificationLogId aNotificationLogId) {

		long count = storedEventRepository.count();

		NotificationLogInfo info = new NotificationLogInfo(aNotificationLogId, count);

		return this.createNotificationLog(info);
	}

	private NotificationLogInfo calculateCurrentNotificationLogId() {

		long count = storedEventRepository.count();

		long remainder = count % NOTIFICATIONS_PER_LOG;

		if (remainder == 0 && count > 0) {
			remainder = NOTIFICATIONS_PER_LOG;
		}

		long low = count - remainder + 1;

		// ensures a minted id value even though there may
		// not be a full set of notifications at present
		long high = low + NOTIFICATIONS_PER_LOG - 1;

		return new NotificationLogInfo(new NotificationLogId(low, high), count);
	}

	private NotificationLog createNotificationLog(NotificationLogInfo aNotificationLogInfo) {

		List<StoredEvent> storedEvents = storedEventRepository.findByEventIdBetween(aNotificationLogInfo.notificationLogId().low(),
				aNotificationLogInfo.notificationLogId().high());

		boolean archivedIndicator = aNotificationLogInfo.notificationLogId().high() < aNotificationLogInfo.totalLogged();

		NotificationLogId next = archivedIndicator ? aNotificationLogInfo.notificationLogId().next(NOTIFICATIONS_PER_LOG) : null;

		NotificationLogId previous = aNotificationLogInfo.notificationLogId().previous(NOTIFICATIONS_PER_LOG);

		NotificationLog notificationLog = new NotificationLog(aNotificationLogInfo.notificationLogId().encoded(), NotificationLogId.encoded(next),
				NotificationLogId.encoded(previous), this.notificationsFrom(storedEvents), archivedIndicator);

		return notificationLog;
	}

	private List<Notification> notificationsFrom(List<StoredEvent> aStoredEvents) {
		List<Notification> notifications = new ArrayList<Notification>(aStoredEvents.size());

		for (StoredEvent storedEvent : aStoredEvents) {
			DomainEvent domainEvent = storedEvent.toDomainEvent();

			Notification notification = new Notification(storedEvent.eventId(), storedEvent.trackerName(), storedEvent.trackerName(), domainEvent);

			notifications.add(notification);
		}

		return notifications;
	}

}
