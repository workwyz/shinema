package cn.shinema.core.port.adapter.publisher;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.util.concurrent.ListenableFuture;

import cn.shinema.core.notification.NotificationGatewayService;
import cn.shinema.core.notification.Notification;
import cn.shinema.core.notification.NotificationSerializer;
import cn.shinema.core.port.adapter.messaging.MessageException;
import cn.shinema.core.notification.NotificationGateway;

public class KafkaNotificationGatewayService implements NotificationGatewayService {
	private static Logger LOGGER = LoggerFactory.getLogger(KafkaNotificationGatewayService.class);

	private final int waitTimeOut = 60;

	@Autowired
	private KafkaTemplate<String, String> kafkaTemplate;

	public NotificationGateway setGatewayType() {
		return NotificationGateway.KAFKA;
	}

	public List<Long> sendMessage(List<Notification> notifications) {
		List<Long> notifyIds = new ArrayList<Long>();
		Map<Long, ListenableFuture<SendResult<String, String>>> resutMap = new HashMap<Long, ListenableFuture<SendResult<String, String>>>();

		try {
			if (null != notifications) {
				for (Notification notify : notifications) {
					resutMap.put(notify.notificationId(), sendMessage(notify));
				}

				resutMap.forEach((k, v) -> {
					if (success(v)) {
						notifyIds.add(k);
					}
				});

//				String succssIds = notifyIds.stream().map(Object::toString).collect(Collectors.toList()).stream().collect(Collectors.joining(","));
//				LOGGER.info("Message Send Success=>{}", succssIds);

			}
		} catch (MessageException e) {
			LOGGER.error("消息发送失败：{}", e.getMessage());
		}

		return notifyIds;
	}

	private boolean success(ListenableFuture<SendResult<String, String>> listenableFuture) {
		if (!listenableFuture.isDone()) {
			try {
				listenableFuture.get(waitTimeOut, TimeUnit.SECONDS);
			} catch (InterruptedException | ExecutionException | TimeoutException e) {
				LOGGER.error("消息发送失败：{}", e.getMessage());
				return false;
			}
		}
		return true;
	}

	private ListenableFuture<SendResult<String, String>> sendMessage(Notification notification) {
		String data = NotificationSerializer.instance().serialize(notification);

		return kafkaTemplate.send(notification.trackerName(), null, notification.occurredOn().getTime(),
				notification.trackerName(), data);
	}
}
