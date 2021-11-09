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

package cn.shinema.core.event.process;

import java.lang.reflect.Method;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.transaction.annotation.Transactional;

import cn.shinema.core.domain.DomainEvent;
import cn.shinema.core.domain.DomainEventPublisher;
import cn.shinema.core.domain.DomainEventSubscriber;
import cn.shinema.core.event.EventListener;
import cn.shinema.core.event.EventPublisher;
import cn.shinema.core.event.EventSerializer;
import cn.shinema.core.event.StoredEvent;
import cn.shinema.core.event.StoredEventRepository;

@Aspect
public class EventListenerProcessor implements ApplicationContextAware {
	private static Logger LOGGER = LoggerFactory.getLogger(EventListenerProcessor.class);

	@Autowired
	private StoredEventRepository storedEventRepository;

	private ApplicationContext applicationContext;

	public EventListenerProcessor() {
		super();
	}

	@Pointcut("@annotation(cn.shinema.core.event.EventListener)")
	public void listener() {

	}

	@Before("listener()")
	public void listen(JoinPoint joinPoint) {
		MethodSignature signature = (MethodSignature) joinPoint.getSignature();
		Method method = signature.getMethod();
		String className = method.getClass().getName();
		EventListener eventListener = method.getAnnotation(EventListener.class);

		LOGGER.debug("Class#{} ,event listener start ...", className);

		if (eventListener != null) {
			if (eventListener.enable()) {
				handleEvent();
			} else {
				LOGGER.debug("Class#{},event listener  enable() is false...", className);
			}
		}
	}

	public void handleEvent() {
		DomainEventPublisher.instance().reset();
		DomainEventPublisher.instance().subscribe(new DomainEventSubscriber<DomainEvent>() {
			public void handleEvent(DomainEvent aDomainEvent) {
				eventStore(aDomainEvent);
			}

			public Class<DomainEvent> subscribedToEventType() {
				return DomainEvent.class;
			}
		});
	}

	@Transactional(rollbackFor = Throwable.class)
	private void eventStore(DomainEvent aDomainEvent) {
		String eventSerialization = EventSerializer.instance().serialize(aDomainEvent);

		try {
			String _trackerName = trackerName(aDomainEvent);
			StoredEvent _newStoredEvent = new StoredEvent(_trackerName, aDomainEvent.getClass().getName(), eventSerialization);
			StoredEvent storedEvent = storedEventRepository.save(_newStoredEvent);
			if (storedEvent.eventId() > 0) {
				EventStoreSuccess eventStoreSuccess = new EventStoreSuccess(this, aDomainEvent.getClass().getName(), storedEvent.eventId());
				this.applicationContext.publishEvent(eventStoreSuccess);
			}
		} catch (Exception e) {
			LOGGER.error("store event fail...{}", e.getMessage());
			throw new RuntimeException(e.getMessage());
		}

	}

	private String trackerName(DomainEvent aDomainEvent) throws ClassNotFoundException {
		String trackerName = null;
		Class<?> clazz = Class.forName(aDomainEvent.getClass().getName());
		EventPublisher eventPublisher = clazz.getAnnotation(EventPublisher.class);

		if (null != eventPublisher) {
			if (null != eventPublisher.name() && !"".equals(eventPublisher.name())) {
				trackerName = eventPublisher.name();
			}
		}

		if (null == trackerName) {
			trackerName = aDomainEvent.getClass().getName();
		}

		return trackerName;

	}

	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}

}
