package cn.shinema.core.config;

import cn.shinema.core.event.process.EventListenerProcessor;
import cn.shinema.core.notification.NotificationGatewayService;
import cn.shinema.core.notification.NotificationPublisher;
import cn.shinema.core.port.adapter.messaging.KafkaConsumerProcessor;
import cn.shinema.core.port.adapter.publisher.EventPublisherProcessor;
import cn.shinema.core.port.adapter.publisher.GenericNotificationPublisher;
import cn.shinema.core.port.adapter.publisher.KafkaNotificationGatewayService;

import org.springframework.context.annotation.Bean;

//@Configuration
public class EventStoredConfig {
    @Bean
    public EventListenerProcessor eventListenerProcessor() {
        return new EventListenerProcessor();
    }

    @Bean
    public NotificationGatewayService kafkaMessageGatewayService() {
        return new KafkaNotificationGatewayService();
    }

    @Bean
    public NotificationPublisher multiNotificationPublisher() {
        return new GenericNotificationPublisher();
    }

    @Bean
    public EventPublisherProcessor instantiationTracingProcessor() {
        return new EventPublisherProcessor();
    }

    @Bean
    public KafkaConsumerProcessor kafkaConsumerProcessor() {
        return new KafkaConsumerProcessor();
    }


}
