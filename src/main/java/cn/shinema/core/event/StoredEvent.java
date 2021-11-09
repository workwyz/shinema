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

package cn.shinema.core.event;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Lob;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import javax.persistence.Version;

import org.hibernate.annotations.GenericGenerator;

import cn.shinema.core.AssertionConcern;
import cn.shinema.core.domain.DomainEvent;

@Entity
@Table(name = "t_stored_event", indexes = { @Index(columnList = "tracker_name, send_status"), @Index(columnList = "send_status,occurred_on") })
public class StoredEvent extends AssertionConcern implements Serializable {
	private static final long serialVersionUID = 5672635743895958664L;

	@Id
	@GeneratedValue(generator = "id_generator")
	@GenericGenerator(name = "id_generator", strategy = "identity")
	@Column(name = "event_id", nullable = false, unique = true, length = 20)
	private Long eventId;

	@Column(name = "tracker_name", nullable = false, length = 255)
	private String trackerName;

	@Column(name = "occurred_class", nullable = false, length = 255)
	private String occurredClass;

	@Column(name = "send_status", nullable = false, length = 1)
	private Integer sendStatus = 0;

	@Version
	@Column(name = "concurrency_version", nullable = false, length = 11)
	private int concurrencyVersion;

	@Lob
	@Column(name = "payload", nullable = true)
	private String payload;

	@Column(name = "occurred_on")
	private Long occurredOn;

	@Column(name = "last_occurred_on")
	private Long lastOccurredOn;

	public StoredEvent() {
		super();
	}

	public StoredEvent(String trackerName, String occurredClass, String payload) {
		super();
		this.trackerName = trackerName;
		this.occurredClass = occurredClass;
		this.payload = payload;
		this.occurredOn = System.currentTimeMillis();
	}

	@SuppressWarnings("unchecked")
	public <T extends DomainEvent> T toDomainEvent() {
		Class<? extends T> domainEventClass = null;

		try {
			domainEventClass = (Class<? extends T>) Class.forName(this.occurredClass);
		} catch (Exception e) {
			throw new IllegalStateException("Class load error, because: " + e.getMessage());
		}

		T domainEvent = EventSerializer.instance().deserialize(this.payload(), domainEventClass);

		return domainEvent;
	}

	protected void setEventBody(String payload) {
		this.assertArgumentNotEmpty(payload, "The event body is required.");
		this.assertArgumentLength(payload, 1, 65000, "The event body must be 65000 characters or less.");
		this.payload = payload;
	}

	protected void setEventId(long anEventId) {
		this.eventId = anEventId;
	}

	public boolean equals(Object anObject) {
		boolean equalObjects = false;

		if (anObject != null && this.getClass() == anObject.getClass()) {
			StoredEvent typedObject = (StoredEvent) anObject;
			equalObjects = this.eventId() == typedObject.eventId();
		}

		return equalObjects;
	}

	public Long getEventId() {
		return eventId;
	}

	public void setEventId(Long eventId) {
		this.eventId = eventId;
	}

	public String trackerName() {
		return trackerName;
	}

	protected void setTrackerName(String trackerName) {
		this.assertArgumentNotEmpty(trackerName, "The event tracker name is required.");
		this.assertArgumentLength(trackerName, 1, 150, "The event tracker name must be 150 characters or less.");
		this.trackerName = trackerName;
	}

	public String occurredClass() {
		return occurredClass;
	}

	protected void setOccurredClass(String occurredClass) {
		this.assertArgumentNotEmpty(trackerName, "The event occurred class is required.");
		this.assertArgumentLength(trackerName, 1, 150, "The event occurred class must be 150 characters or less.");
		this.occurredClass = occurredClass;
	}

	public String payload() {
		return this.payload;
	}

	public long eventId() {
		return this.eventId;
	}

	public Integer sendStatus() {
		return sendStatus;
	}

	public void setSendStatus(Integer sendStatus) {
		this.sendStatus = sendStatus;
	}

	@PrePersist
	void createdAt() {
		this.occurredOn = System.currentTimeMillis();
	}

	@PreUpdate
	void updatedAt() {
		this.lastOccurredOn = System.currentTimeMillis();
	}

	public String toString() {
		return "StoredEvent [eventBody=" + payload + ", eventId=" + eventId + ", occurredOn=" + occurredOn + ", trackerName=" + trackerName + ", occurredClass=" + occurredClass
				+ "]";
	}

}
