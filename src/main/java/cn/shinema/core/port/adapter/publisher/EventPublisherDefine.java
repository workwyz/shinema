package cn.shinema.core.port.adapter.publisher;

import java.util.Queue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import cn.shinema.core.domain.DomainEvent;
import cn.shinema.core.notification.NotificationGateway;

public class EventPublisherDefine {

	private Class<?> clazz;
	private int quantity = 10;
	private String trackerName;
	private String occurredClass;
	private NotificationGateway[] messageGatewayTypes;

	private ExecutorService executorService = null;

	private AtomicBoolean publishing = new AtomicBoolean(false);

	private Queue<Long> unPublishQueue = new LinkedBlockingQueue<Long>(1000 * 100 * 10);

	public EventPublisherDefine(String trackerName, Class<?> clazz) {
		super();
		this.trackerName = trackerName;
		this.clazz = clazz;
		initThreadExecutor(clazz.getSimpleName());
	}

	private void initThreadExecutor(String classSimpleName) {
		String threadName = String.format("%s%s%s%s", "NotificationPublisher", "-", classSimpleName, "-%d");
		ThreadFactoryBuilder factoryBuilder = new ThreadFactoryBuilder();
		factoryBuilder.setNameFormat(threadName);
		this.executorService = Executors.newSingleThreadExecutor(factoryBuilder.build());
	}

	public String trackerName() {
		return trackerName;
	}

	public void setTrackerName(String trackerName) {
		this.trackerName = trackerName;
	}

	public String getOccurredClass() {
		return occurredClass;
	}

	public void setOccurredClass(String occurredClass) {
		this.occurredClass = occurredClass;
	}

	public int quantity() {
		return quantity;
	}

	public void setQuantity(int quantity) {
		this.quantity = quantity;
	}

	public Class<?> getClazz() {
		return clazz;
	}

	public void setClazz(Class<? extends DomainEvent> clazz) {
		this.clazz = clazz;
	}

	public NotificationGateway[] notifyTypes() {
		return messageGatewayTypes;
	}

	public void setNotifyTypes(NotificationGateway[] messageGatewayTypes) {
		this.messageGatewayTypes = messageGatewayTypes;
	}

	public Queue<Long> unPublishQueue() {
		return unPublishQueue;
	}

	public boolean isPublishing() {
		return this.publishing.get();
	}

	public void publishing() {
		this.publishing.compareAndSet(false, true);
	}

	public void published() {
		this.publishing.compareAndSet(true, false);
	}

	public void execute(Runnable command) {
		executorService.execute(command);
	}

	public void shutdown() {
		if (!executorService.isShutdown()) {
			executorService.shutdown();
		}
	}

}
