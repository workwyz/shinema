package cn.shinema.core.port.adapter.messaging;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.Header;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.AcknowledgingMessageListener;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.ContainerProperties.AckMode;
import org.springframework.kafka.listener.KafkaMessageListenerContainer;
import org.springframework.kafka.listener.MessageListenerContainer;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.util.ReflectionUtils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

import cn.shinema.core.event.ConsumedEvent;
import cn.shinema.core.event.ConsumedEventRepository;
import cn.shinema.core.event.DeadLetter;
import cn.shinema.core.event.DeadLetterRepository;
import cn.shinema.core.event.KafkaConsumer;
import cn.shinema.core.notification.NotificationReader;
import cn.shinema.core.port.adapter.util.TypeConvertUtils;

@Order(5)
@ConditionalOnBean(ConsumerFactory.class)
public class KafkaConsumerProcessor implements ApplicationRunner, ApplicationContextAware {
	private static final Logger LOGGER = LoggerFactory.getLogger(KafkaConsumerProcessor.class);

	public static final String ORIGINAL_TOPIC_HEADER_KEY = "original_topic";
	public static final String RETRY_COUNT_HEADER_KEY = "retry_count";
//    public static final String DLQ_TOPIC = "dlq-topic";

	@Resource
	private Environment environment;

	@Autowired
	private ConsumedEventRepository consumedEventRepository;

	@Autowired
	private DeadLetterRepository deadLetterRepository;

	@Autowired
	private ConsumerFactory<String, String> consumerFactory;

	@Autowired
	private KafkaTemplate<String, String> kafkaTemplate;

	private ApplicationContext applicationContext = null;

	private Map<String, MessageListenerContainer> listenerContainers;

	@PostConstruct
	public void postConstruct() {
		LOGGER.info("=========KafkaConsumerProcessor PostConstruct=========");

		listenerContainers = new HashMap<String, MessageListenerContainer>();
		String[] beanNames = applicationContext.getBeanDefinitionNames();
		for (String beanName : beanNames) {
			Object bean = applicationContext.getBean(beanName);
			postProcessAfterInitialization(bean, beanName);
		}

	}

	public void run(ApplicationArguments args) throws Exception {
		LOGGER.info("=====================KafkaConsumerProcessor Runner========================");

		listenerContainers.forEach((k, v) -> {
			LOGGER.info("Kafka消息监听：{} 开始...", k);
			v.start();
		});
	}

	@PreDestroy
	public void preDestroy() throws Exception {
		LOGGER.info("========KafkaConsumerProcessor preDestroy==========");
		listenerContainers.forEach((k, v) -> {
			LOGGER.info("Kafka消息监听：{} 关闭...", k);
			v.stop();
		});
	}

	public void postProcessAfterInitialization(final Object bean, final String beanName) {
		ReflectionUtils.doWithLocalMethods(bean.getClass(), method -> {

			KafkaConsumer annotation = AnnotationUtils.findAnnotation(method, KafkaConsumer.class);
			if (annotation == null) {
				return;
			}

			if (method.getParameterCount() == 0) {
				LOGGER.warn("@KafkaConsumer无配置参数 {}#{}  忽略！", beanName, method.getName());
				return;
			}

			String topic = annotation.topic();
			String groupId = annotation.groupId();

			LOGGER.info("注册消息处理：Bean Name=>{},method=>{}", beanName, method.getName());
			LOGGER.info("消息监听参数：Topic=>{}, GroupId=>{}", topic, groupId);
			LOGGER.info("处理参数列表：{}", Stream.of(method.getParameters()).map(Parameter::getName).collect(Collectors.joining(",")));

			ContainerProperties containerProperties = new ContainerProperties(topic);
			containerProperties.setGroupId(groupId);
			containerProperties.setAckMode(AckMode.MANUAL_IMMEDIATE);
			containerProperties.setMessageListener(new AcknowledgingMessageListener<String, String>() {
				public void onMessage(ConsumerRecord<String, String> record, Acknowledgment acknowledgment) {
					handleMessage(bean, method, record, acknowledgment);
				}
			});

			KafkaMessageListenerContainer<String, String> messageListenerContainer = new KafkaMessageListenerContainer<String, String>(consumerFactory, containerProperties);

			String containerName = String.format("%s%s%s", beanName, "#", method.getName());
			listenerContainers.put(containerName, messageListenerContainer);

		});
	}

	private void handleMessage(final Object bean, final Method method, final ConsumerRecord<String, String> record, Acknowledgment acknowledgment) {

		try {
			filteredDispatch(bean, method, record);
		} catch (Exception e) {
			LOGGER.error("处理消息异常：", e);
			try {
				failStrategy(method, record);
			} catch (Exception e1) {
				LOGGER.error("处理死信消息异常：", e1);
			}
		} finally {
			// 无论是否消费成功，都确认此消息
			acknowledgment.acknowledge();
		}

	}

	private void filteredDispatch(final Object bean, final Method method, ConsumerRecord<String, String> record) throws Exception {
		String topic = record.topic();
		String key = record.key();
		int partition = record.partition();
		long timestamp = record.timestamp();
		long offset = record.offset();

		LOGGER.debug("New Kafka Message: topic={},key={},partition={},timestamp={},offset={}", topic, key, partition, timestamp, offset);

		String msgValue = record.value();

		if (null != msgValue && !"".equals(msgValue)) {
			JsonElement jsonElement = null;
			try {
				jsonElement = JsonParser.parseString(msgValue);

				if (jsonElement.isJsonObject()) {
					NotificationReader notificationReader = new NotificationReader(msgValue);

					if (notificationReader.isEventBody()) {
						long notificationId = notificationReader.notificationId();
						String trackerName = notificationReader.trackerName();
						String eventBody = notificationReader.eventAsString();
						String receiveName = method.getDeclaringClass().getName();

						LOGGER.info("Unpack Message：TrackerName=>{} , NotificationId=>{} , ReceiveName=>{}", trackerName, notificationId, receiveName);

						int spentStatus = consumedEventRepository.countByTrackerNameAndReceiveNameAndEventId(trackerName, receiveName, notificationId);

						if (!(spentStatus > 0)) {

							handleEventMsg(bean, method, record, notificationId, eventBody);

							ConsumedEvent consumedEvent = new ConsumedEvent(notificationId, trackerName, receiveName);
							consumedEventRepository.save(consumedEvent);

						} else {
							LOGGER.warn("重复消费，忽略！  EventId => {} ", notificationId);
						}
					} else {
						handleMsg(bean, method, record, msgValue);
					}
				} else {
					handleMsg(bean, method, record, msgValue);
				}
			} catch (JsonSyntaxException ex) {
				LOGGER.warn("非JSON消息格式，直接子类处理。Message => {} ", msgValue);
				handleMsg(bean, method, record, msgValue);
			}
		} else {
			LOGGER.info("消息内容为空，忽略！");
		}
	}

	private void handleMsg(final Object bean, final Method method, ConsumerRecord<String, String> record, String msgValue) throws Exception {
		try {
			Object event = null;
			int paramCount = method.getParameterCount();
			String receiveName = method.getDeclaringClass().getName();

			if (paramCount == 1) {
				Class<?> targetType = method.getParameterTypes()[0];
				if (String.class.isAssignableFrom(targetType)) {
					event = msgValue;
				} else if (ConsumerRecord.class.isAssignableFrom(targetType)) {
					event = record;
				} else {
					Gson gson = new GsonBuilder().serializeNulls().create();
					event = gson.fromJson(msgValue, targetType);
				}
			} else {
				throw new RuntimeException("消费子类参数设置错误；仅支持1个参数的输入。。。");
			}

			long starttime = System.currentTimeMillis();
			LOGGER.info("开始消费子类调用：started=>{},subclass name=>{}", starttime, receiveName);

			method.invoke(bean, event);

			long endtime = System.currentTimeMillis();
			LOGGER.info("消费子类调用耗时：completed=>{},elapsed=>{}", endtime, (endtime - starttime));

		} catch (Exception e) {
			LOGGER.error("消息处理失败====", e);
			throw e;
		}
	}

	private void handleEventMsg(final Object bean, final Method method, ConsumerRecord<String, String> record, long notificationId, String eventBody) throws Exception {
		try {
			Object event = null;
			int paramCount = method.getParameterCount();
			String receiveName = method.getDeclaringClass().getName();

			long starttime = System.currentTimeMillis();
			LOGGER.info("开始消费子类调用：notification id=>{},started=>{},subclass name=>{}", notificationId, starttime, receiveName);

			if (paramCount == 1) {
				Class<?> targetType = method.getParameterTypes()[0];
				if (String.class.isAssignableFrom(targetType)) {
					event = eventBody;
				} else if (ConsumerRecord.class.isAssignableFrom(targetType)) {
					event = record;
				} else {
					Gson gson = new GsonBuilder().serializeNulls().create();
					event = gson.fromJson(eventBody, targetType);
				}
				method.invoke(bean, event);
			} else if (paramCount == 2) {
				Class<?> targetType = method.getParameterTypes()[1];
				if (String.class.isAssignableFrom(targetType)) {
					event = eventBody;
				} else if (ConsumerRecord.class.isAssignableFrom(targetType)) {
					event = record;
				} else {
					Gson gson = new GsonBuilder().serializeNulls().create();
					event = gson.fromJson(eventBody, targetType);
				}
				method.invoke(bean, notificationId, eventBody);
			} else {
				throw new RuntimeException("消费子类参数设置错误；仅支持1-2个参数的输入。。。");
			}

			long endtime = System.currentTimeMillis();
			LOGGER.info("消费子类调用耗时：notification id=>{},completed=>{},elapsed=>{}", notificationId, endtime, (endtime - starttime));

		} catch (Exception e) {
			LOGGER.error("消息处理失败====", e);
			throw e;
		}
	}

	private void failStrategy(final Method method, final ConsumerRecord<String, String> record) {
		KafkaConsumer annotation = AnnotationUtils.findAnnotation(method, KafkaConsumer.class);
		boolean enableDlqDef = annotation.enableDLQ();
		int retryCountDef = annotation.retryCount();

		if (enableDlqDef) {
			int retryCount = 0;
			Header retryCountHeader = record.headers().lastHeader(RETRY_COUNT_HEADER_KEY);

			if (null != retryCountHeader) {
				retryCount = TypeConvertUtils.byteArrayToInt(retryCountHeader.value());
			}

			if (retryCount < retryCountDef) {
				ProducerRecord<String, String> reSendRecord = new ProducerRecord<>(record.topic(), record.value());
				reSendRecord.headers().add(ORIGINAL_TOPIC_HEADER_KEY, record.topic().getBytes(StandardCharsets.UTF_8));
				reSendRecord.headers().add(RETRY_COUNT_HEADER_KEY, TypeConvertUtils.intToByteArray(++retryCount));
				kafkaTemplate.send(reSendRecord);
				LOGGER.warn("Retry [Count:{}] Message Publish; Topic =>{} ; Data => {}", retryCount, record.topic(), record.value());
			} else {
				failConduct(method, record);
				LOGGER.error("Message Consumer Fail Timeout ; Retry Count {} ; Save To DeadLetter ; requires doing with at once ...", retryCount);
			}
		} else {
			failConduct(method, record);
		}

	}

	private void failConduct(final Method method, final ConsumerRecord<String, String> record) {
		final String msgValue = record.value();
		final String receiveName = method.getDeclaringClass().getName();

		if (null != msgValue && !"".equals(msgValue)) {
			NotificationReader notificationReader = new NotificationReader(msgValue);
			String trackerName = notificationReader.trackerName();
			long notificationId = notificationReader.notificationId();
			String eventBody = notificationReader.eventAsString();

			DeadLetter deadLetter = new DeadLetter(notificationId, trackerName, receiveName, eventBody);
			boolean exist = deadLetterRepository.existsByEventIdAndTrackerNameAndReceiveName(deadLetter.notificationId(), trackerName, receiveName);
			if (!exist) {
				deadLetterRepository.save(deadLetter);
			}
		}
	}

	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;

	}

}
