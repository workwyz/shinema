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

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import javax.persistence.Version;

import cn.shinema.core.AssertionConcern;

@Entity
@Table(name = "t_published_notification_tracker")
public class PublishedNotificationTracker extends AssertionConcern implements Serializable {
	private static final long serialVersionUID = 6705348900173926053L;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "published_notification_tracker_id", nullable = false, unique = true, length = 20)
	private long publishedNotificationTrackerId;

	@Column(name = "tracker_name", nullable = true, unique = true, length = 150)
	private String trackerName;

	@Column(name = "most_recent_published_event_id", nullable = false, length = 20)
	private long mostRecentPublishedEventId;

	@Version
	@Column(name = "concurrency_version", nullable = false, length = 11)
	private int concurrencyVersion;

	@Column(name = "occurred_on")
	private Long occurredOn;

	@Column(name = "last_occurred_on")
	private Long lastOccurredOn;

	protected PublishedNotificationTracker() {
		super();
		this.occurredOn = System.currentTimeMillis();
	}

	public PublishedNotificationTracker(String trackerName) {
		super();
		this.trackerName = trackerName;
		this.occurredOn = System.currentTimeMillis();
	}

	public String trackerName() {
		return trackerName;
	}

	public void setTrackerName(String trackerName) {
		this.assertArgumentNotEmpty(trackerName, "The tracker type name is required.");
		this.assertArgumentLength(trackerName, 100, "The tracker type name must be 100 characters or less.");
		this.trackerName = trackerName;
	}

	public long publishedNotificationTrackerId() {
		return publishedNotificationTrackerId;
	}

	public long mostRecentPublishedEventId() {
		return mostRecentPublishedEventId;
	}

	public void setMostRecentPublishedEventId(long mostRecentPublishedEventId) {
		this.mostRecentPublishedEventId = mostRecentPublishedEventId;
	}

	public int concurrencyVersion() {
		return concurrencyVersion;
	}

	@PrePersist
	void createdAt() {
		this.occurredOn = System.currentTimeMillis();
	}

	@PreUpdate
	void updatedAt() {
		this.lastOccurredOn = System.currentTimeMillis();
	}

	public void failWhenConcurrencyViolation(int aVersion) {
		this.assertStateTrue(aVersion == this.concurrencyVersion(), "Concurrency Violation: Stale data detected. Entity was already modified.");
	}

}
