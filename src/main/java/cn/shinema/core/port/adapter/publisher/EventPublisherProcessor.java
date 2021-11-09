package cn.shinema.core.port.adapter.publisher;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.apache.commons.lang.StringUtils;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.core.annotation.Order;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import cn.shinema.core.domain.DomainEvent;
import cn.shinema.core.event.EventPublisher;
import cn.shinema.core.event.StoredEventRepository;
import cn.shinema.core.event.process.EventScanRegistrar;
import cn.shinema.core.event.process.EventStoreSuccess;
import cn.shinema.core.lock.ZkLock;
import cn.shinema.core.notification.NotificationPublisher;
import cn.shinema.core.notification.NotificationGateway;

@Order(1)
@ConditionalOnBean({ NotificationPublisher.class, ZkLock.class })
public class EventPublisherProcessor implements ApplicationRunner {
	private static Logger LOGGER = LoggerFactory.getLogger(EventPublisherProcessor.class);

	@Autowired
	private ZkLock lock;

	@Autowired
	private StoredEventRepository storedEventRepository;

	@Autowired
	private NotificationPublisher notificationPublisher;

	private final Map<String, EventPublisherDefine> domainEvents = new TreeMap<String, EventPublisherDefine>();

	private Future<?> scheduledFuture = null;

	private ScheduledExecutorService scheduledExecutorService = null;

	@Value("${event.interval:500}")
	private int eventInterval;

	@PostConstruct
	public void postConstruct() {
		LOGGER.info("=====================PostConstruct========================");
		this.loaderDomainEvents();

		ThreadFactoryBuilder factoryBuilder = new ThreadFactoryBuilder();
		factoryBuilder.setNameFormat("NotificationScheduled-%d");
		scheduledExecutorService = Executors.newSingleThreadScheduledExecutor(factoryBuilder.build());

	}

	public void run(ApplicationArguments args) throws Exception {
		LOGGER.info("=====================Runner========================");

		for (Map.Entry<String, EventPublisherDefine> event : domainEvents.entrySet()) {
			String trackerName = event.getKey();
			EventPublisherDefine publisherDef = event.getValue();

			Collection<Long> eventIds = storedEventRepository.listUnpublishedEventIds(trackerName);

			if (!eventIds.isEmpty()) {
				LOGGER.info("Unpublish Message:: TrackerName => {} Count => {}", trackerName, eventIds.size());
				eventIds.stream().forEach(v -> {
					publisherDef.unPublishQueue().add(v);
				});
			}

		}
		scheduleInitialized(5000);
	}

	@PreDestroy
	public void preDestroy() {
		LOGGER.info("=====================preDestroy========================");

		if (scheduledFuture != null) {
			scheduledFuture.cancel(false);
		}

		domainEvents.forEach((k, v) -> v.shutdown());

		if (!scheduledExecutorService.isShutdown()) {
			scheduledExecutorService.shutdown();
		}

	}

	private void loaderDomainEvents() {
		Set<Class<? extends DomainEvent>> classes = new HashSet<Class<? extends DomainEvent>>();
		String eventScanPackagesStr = System.getProperty(EventScanRegistrar.EventScanPackages, "");

		String[] eventScanPackages = null;
		if (!StringUtils.isEmpty(eventScanPackagesStr)) {
			LOGGER.info("Found Config @DomainEventScan== {}", eventScanPackagesStr);
			eventScanPackages = StringUtils.split(eventScanPackagesStr, ',');
		} else {
			LOGGER.warn("@DomainEventScan No Setting...");
			return;
		}

		if (null != eventScanPackages) {
			for (String packages : eventScanPackages) {
				Reflections reflections = new Reflections(packages);
				classes.addAll(reflections.getSubTypesOf(DomainEvent.class));
			}
		}

		for (Class<? extends DomainEvent> clazz : classes) {
			String className = clazz.getName();

			LOGGER.info("Found Domain Event Class => {}", className);

			EventPublisherDefine pubdefine = new EventPublisherDefine(className, clazz);

			EventPublisher eventPublisher = clazz.getAnnotation(EventPublisher.class);
			if (null != eventPublisher) {
				String trackerName = eventPublisher.name();
				if (null != trackerName && !"".equals(trackerName)) {
					pubdefine.setTrackerName(trackerName);
				} else {
					pubdefine.setTrackerName(className);
				}
				pubdefine.setOccurredClass(className);
				pubdefine.setQuantity(eventPublisher.quantity());
				pubdefine.setNotifyTypes(eventPublisher.notifyTypes());
			} else {
				pubdefine.setTrackerName(className);
				pubdefine.setOccurredClass(className);
				pubdefine.setQuantity(10);
				pubdefine.setNotifyTypes(new NotificationGateway[] { NotificationGateway.KAFKA });
			}

			Class<DomainEvent> eventClazz = DomainEvent.class;
			if (eventClazz.isAssignableFrom(clazz)) {
				if (!eventClazz.equals(clazz)) {
					domainEvents.put(className, pubdefine);
				}
			}
		}
	}

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void onEventSuccess(EventStoreSuccess event) {
		String trackerName = event.trackerName();
		Long eventId = event.eventId();

		LOGGER.info("Event Store Listener :: TrackerName =>{} , EventId => {}", trackerName, eventId);

		EventPublisherDefine publisherDef = this.domainEvents.get(trackerName);

		if (null != publisherDef) {
			publisherDef.unPublishQueue().add(eventId);
		} else {
			LOGGER.error("Tracker Name [{}] , No Found !", trackerName);
		}
	}

	class InnerScheduleThread implements Runnable {
		public void run() {
			domainEvents.forEach((k, v) -> {
				int unpublishCount = v.unPublishQueue().size();
//				LOGGER.info("Publish Schedule::Event Id =>{} , unpublishCount => {} ", k, unpublishCount);
				if (unpublishCount > 0 && !v.isPublishing()) {
					LOGGER.info("UnPublish Message:: TrackerName:{},Count:{}", v.trackerName(), unpublishCount);
					v.execute(new EventPublisherJob(lock, v, notificationPublisher));
				}
			});
		}
	}

	private void scheduleInitialized(long delay) {
		scheduledFuture = scheduledExecutorService.scheduleWithFixedDelay(new InnerScheduleThread(), delay, eventInterval, TimeUnit.MILLISECONDS);
	}

	public Map<String, EventPublisherDefine> domainEvents() {
		return domainEvents;
	}

}
