package cn.shinema.core.port.adapter.util;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class JsonUtils {
	private static final Logger LOGGER = LoggerFactory.getLogger(JsonUtils.class);

	public static final ObjectMapper mapper = new ObjectMapper();

	public static String toStr(Object value) {
		if (null == value) {
			if (LOGGER.isWarnEnabled()) {
				LOGGER.warn("=======待转换对象为空，请检查！");
			}
			return null;
		}

		String result = null;

		try {
			result = mapper.writeValueAsString(value);
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		}

		return result;
	}

	public static boolean canRead(String content) {
		boolean isJson = false;

		try {
//			JsonNode jsonNode = mapper.readTree(content);
			mapper.readTree(content);
			isJson = true;
		} catch (IOException e) {
			// e.printStackTrace();
		}

		return isJson;
	}

	public static JsonNode toTree(String content) {
		JsonNode jsonNode = null;

		try {
			jsonNode = mapper.readTree(content);
		} catch (IOException e) {
			e.printStackTrace();
		}

		return jsonNode;
	}

	public static <T> T toBean(String content, Class<T> clazz) {
		if (null == content || "".equals(content)) {
			if (LOGGER.isWarnEnabled()) {
				LOGGER.warn("=======待转换对象为空，请检查！");
			}
			return null;
		}

		T result = null;

		try {
			result = mapper.readValue(content, clazz);
		} catch (IOException e) {
			e.printStackTrace();
		}

		return result;
	}

	public static <T> List<T> toList(String content, Class<T> beanType) {
		JavaType javaType = mapper.getTypeFactory().constructParametricType(List.class, beanType);

		try {
			List<T> resultList = mapper.readValue(content, javaType);
			return resultList;
		} catch (Exception e) {
			e.printStackTrace();
		}

		return null;
	}

	public static <K, V> Map<K, V> toMap(String content, Class<K> keyType, Class<V> valueType) {
		JavaType javaType = mapper.getTypeFactory().constructMapType(Map.class, keyType, valueType);

		try {
			Map<K, V> resultMap = mapper.readValue(content, javaType);
			return resultMap;
		} catch (Exception e) {
			e.printStackTrace();
		}

		return null;
	}

	public static void main(String[] args) {

	}

}
