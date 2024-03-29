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

package cn.shinema.core.domain;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class DomainEventPublisher {

	private static final ThreadLocal<DomainEventPublisher> instance = new ThreadLocal<DomainEventPublisher>() {
		protected DomainEventPublisher initialValue() {
			return new DomainEventPublisher();
		}
	};

	private boolean publishing;

	private List<DomainEventSubscriber<DomainEvent>> subscribers;

	public static DomainEventPublisher instance() {
		return instance.get();
	}

	public void publish(final DomainEvent aDomainEvent) {
		if (!this.isPublishing() && this.hasSubscribers()) {

			try {
				this.setPublishing(true);

				Class<?> eventType = aDomainEvent.getClass();

				List<DomainEventSubscriber<DomainEvent>> allSubscribers = this.subscribers();

				for (DomainEventSubscriber<DomainEvent> subscriber : allSubscribers) {
					Class<?> subscribedToType = subscriber.subscribedToEventType();

					if (eventType == subscribedToType || subscribedToType == DomainEvent.class) {
						subscriber.handleEvent(aDomainEvent);
					}
				}

			} finally {
				this.setPublishing(false);
			}
		}
	}

	public void publishAll(Collection<DomainEvent> aDomainEvents) {
		for (DomainEvent domainEvent : aDomainEvents) {
			this.publish(domainEvent);
		}
	}

	public void reset() {
		if (!this.isPublishing()) {
			this.setSubscribers(null);
		}
	}

	public void subscribe(DomainEventSubscriber<DomainEvent> aSubscriber) {
		if (!this.isPublishing()) {
			this.ensureSubscribersList();

			this.subscribers().add(aSubscriber);
		}
	}

	private DomainEventPublisher() {
		super();

		this.setPublishing(false);
		this.ensureSubscribersList();
	}

	private void ensureSubscribersList() {
		if (!this.hasSubscribers()) {
			this.setSubscribers(new ArrayList<DomainEventSubscriber<DomainEvent>>());
		}
	}

	private boolean isPublishing() {
		return this.publishing;
	}

	private void setPublishing(boolean aFlag) {
		this.publishing = aFlag;
	}

	private boolean hasSubscribers() {
		return this.subscribers() != null;
	}

	private List<DomainEventSubscriber<DomainEvent>> subscribers() {
		return this.subscribers;
	}

	private void setSubscribers(List<DomainEventSubscriber<DomainEvent>> aSubscriberList) {
		this.subscribers = aSubscriberList;
	}
}
