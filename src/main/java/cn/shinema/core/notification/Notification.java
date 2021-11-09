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
import java.util.Date;

import cn.shinema.core.AssertionConcern;
import cn.shinema.core.domain.DomainEvent;

public class Notification extends AssertionConcern implements Comparable<Notification>, Serializable {

	private static final long serialVersionUID = 1L;

	private long eventId;
	private String occurredClass;
	private String trackerName;
	private DomainEvent event;
	private Date occurredOn;

	protected Notification() {
		super();
	}

	public Notification(long aEventId, String trackerName, String aOccurredClass, DomainEvent anEvent) {
		this();
		this.setEventId(aEventId);
		this.setTrackerName(trackerName);
		this.setOccurredClass(aOccurredClass);
		this.setEvent(anEvent);
		this.setOccurredOn(anEvent.occurredOn());
	}

	@SuppressWarnings("unchecked")
	public <T extends DomainEvent> T event() {
		return (T) this.event;
	}

	public long notificationId() {
		return this.eventId;
	}

	public Date occurredOn() {
		return this.occurredOn;
	}

	@Override
	public boolean equals(Object anObject) {
		boolean equalObjects = false;

		if (anObject != null && this.getClass() == anObject.getClass()) {
			Notification typedObject = (Notification) anObject;
			equalObjects = this.notificationId() == typedObject.notificationId();
		}

		return equalObjects;
	}

	@Override
	public int hashCode() {
		int hashCodeValue = +(3017 * 197) + (int) this.notificationId();

		return hashCodeValue;
	}

	@Override
	public String toString() {
		return "Notification [event=" + event + ", eventId=" + eventId + ", trackerName=" + trackerName
				+ ", occurredClass=" + occurredClass + ", occurredOn=" + occurredOn + "]";
	}

	protected void setEvent(DomainEvent anEvent) {
		this.assertArgumentNotNull(anEvent, "The event is required.");

		this.event = anEvent;
	}

	protected void setEventId(long aEventId) {
		this.eventId = aEventId;
	}

	public String getOccurredClass() {
		return occurredClass;
	}

	public void setOccurredClass(String occurredClass) {
		this.occurredClass = occurredClass;
	}

	protected void setOccurredOn(Date anOccurredOn) {
		this.occurredOn = anOccurredOn;
	}

	public String trackerName() {
		return trackerName;
	}

	protected void setTrackerName(String aTrackerName) {
		this.assertArgumentNotEmpty(aTrackerName, "The type name is required.");
		this.assertArgumentLength(aTrackerName, 100, "The type name must be 100 characters or less.");
		this.trackerName = aTrackerName;
	}

	public int compareTo(Notification o) {
		return new Long(this.eventId).compareTo(o.eventId);
	}

}
