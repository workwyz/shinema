package cn.shinema.core.notification;

import java.util.List;

public interface NotificationGatewayService {

	public List<Long> sendMessage(List<Notification> notifications);

	public NotificationGateway setGatewayType();
}
