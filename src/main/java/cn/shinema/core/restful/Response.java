package cn.shinema.core.restful;

public class Response<T> {
	private int code;
	private String msg;
	private T data;

	public Response() {
	}

	public Response(int status, T data) {
		this.code = status;
		this.data = data;
	}

	public Response(int status, String msg, T data) {
		this.code = status;
		this.msg = msg;
		this.data = data;
	}

	public Response(int status, String msg) {
		this.code = status;
		this.msg = msg;
	}

	public int getCode() {
		return code;
	}

	public T getData() {
		return data;
	}

	public String getMsg() {
		return msg;
	}

	public static <T> Response<T> success() {
		return new Response<T>(ResponseCode.SUCCESS.getCode(), ResponseCode.SUCCESS.getDesc());
	}

	public static <T> Response<T> success(String msg) {
		return new Response<T>(ResponseCode.SUCCESS.getCode(), msg);
	}

	public static <T> Response<T> success(T data) {
		return new Response<T>(ResponseCode.SUCCESS.getCode(), data);
	}

	public static <T> Response<T> success(String msg, T data) {
		return new Response<T>(ResponseCode.SUCCESS.getCode(), msg, data);
	}

	public static <T> Response<T> error() {
		return new Response<T>(ResponseCode.ERROR.getCode(), ResponseCode.ERROR.getDesc());
	}

	public static <T> Response<T> error(int code, String msg) {
		return new Response<T>(code, msg);
	}

	public static <T> Response<T> error(int code, String msg, T data) {
		return new Response<T>(code, msg, data);
	}

}
