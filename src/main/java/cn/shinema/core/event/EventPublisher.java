package cn.shinema.core.event;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import cn.shinema.core.notification.NotificationGateway;

@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface EventPublisher {

	/**
	 * 事件接收者，默认为DomainEvent子类名
	 * 
	 * @return string
	 */
	String name() default "";

	/**
	 * 设置一次获取的事件数
	 * 
	 * @return int
	 */
	int quantity() default 10;

	/**
	 * 自定义发布目标消息网关
	 * 
	 * @return NotifyType[]
	 */
	NotificationGateway[] notifyTypes() default NotificationGateway.ALL;
}
