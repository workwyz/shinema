package cn.shinema.core.port.adapter.util;

public class TypeConvertUtils {
    /**
     * int到byte[] 由高位到低位
     *
     * @param value 需要转换为byte数组的整行值。
     * @return byte数组
     */
    public static byte[] intToByteArray(int value) {
        byte[] result = new byte[4];
        result[0] = (byte) ((value >> 24) & 0xFF);
        result[1] = (byte) ((value >> 16) & 0xFF);
        result[2] = (byte) ((value >> 8) & 0xFF);
        result[3] = (byte) (value & 0xFF);
        return result;
    }

    /**
     * byte[]转int
     *
     * @param bytes 需要转换成int的数组
     * @return int值
     */
    public static int byteArrayToInt(byte[] bytes) {
        int value = 0;
        for (int i = 0; i < 4; i++) {
            int shift = (3 - i) * 8;
            value += (bytes[i] & 0xFF) << shift;
        }
        return value;
    }

    public static void main(String[] args) {
        System.out.println(intToByteArray(1));
    }
}
