package cn.shinema.core.restful;

public enum ResponseCode {
	SUCCESS(200, "请求成功"), ERROR(500, "内部服务器错误");

	private final int code;
	private final String desc;

	ResponseCode(int code, String desc) {
		this.code = code;
		this.desc = desc;
	}

	public int getCode() {
		return code;
	}

	public String getDesc() {
		return desc;
	}

}
