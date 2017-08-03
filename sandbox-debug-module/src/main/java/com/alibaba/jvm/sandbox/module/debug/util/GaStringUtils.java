package com.alibaba.jvm.sandbox.module.debug.util;

import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.util.*;

import static java.lang.String.format;

/**
 * Greys字符串工具类
 * Created by luanjia@taobao.com on 2017/2/24.
 */
public class GaStringUtils {

    private final static String TAB = "    ";

    private final static Map<Byte, String> ASCII_MAP = new HashMap<Byte, String>();

    static {
        ASCII_MAP.put((byte) 0, "NUL");
        ASCII_MAP.put((byte) 1, "SOH");
        ASCII_MAP.put((byte) 2, "STX");
        ASCII_MAP.put((byte) 3, "ETX");
        ASCII_MAP.put((byte) 4, "EOT");
        ASCII_MAP.put((byte) 5, "ENQ");
        ASCII_MAP.put((byte) 6, "ACK");
        ASCII_MAP.put((byte) 7, "BEL");
        ASCII_MAP.put((byte) 8, "BS");
        ASCII_MAP.put((byte) 9, "HT");
        ASCII_MAP.put((byte) 10, "LF");
        ASCII_MAP.put((byte) 11, "VT");
        ASCII_MAP.put((byte) 12, "FF");
        ASCII_MAP.put((byte) 13, "CR");
        ASCII_MAP.put((byte) 14, "SO");
        ASCII_MAP.put((byte) 15, "SI");
        ASCII_MAP.put((byte) 16, "DLE");
        ASCII_MAP.put((byte) 17, "DC1");
        ASCII_MAP.put((byte) 18, "DC2");
        ASCII_MAP.put((byte) 19, "DC3");
        ASCII_MAP.put((byte) 20, "DC4");
        ASCII_MAP.put((byte) 21, "NAK");
        ASCII_MAP.put((byte) 22, "SYN");
        ASCII_MAP.put((byte) 23, "ETB");
        ASCII_MAP.put((byte) 24, "CAN");
        ASCII_MAP.put((byte) 25, "EM");
        ASCII_MAP.put((byte) 26, "SUB");
        ASCII_MAP.put((byte) 27, "ESC");
        ASCII_MAP.put((byte) 28, "FS");
        ASCII_MAP.put((byte) 29, "GS");
        ASCII_MAP.put((byte) 30, "RS");
        ASCII_MAP.put((byte) 31, "US");
        ASCII_MAP.put((byte) 127, "DEL");
    }

    public static String toString(Object obj, int expand) {
        return toString(obj, 0, expand);
    }

    private static String toString(Object obj, int deep, int expand) {

        final StringBuilder buf = new StringBuilder();
        if (null == obj) {
            buf.append("null");
        } else {

            final Class<?> clazz = obj.getClass();
            final String className = clazz.getSimpleName();

            // 7种基础类型,直接输出@类型[值]
            if (Integer.class.isInstance(obj)
                    || Long.class.isInstance(obj)
                    || Float.class.isInstance(obj)
                    || Double.class.isInstance(obj)
//                    || Character.class.isInstance(obj)
                    || Short.class.isInstance(obj)
                    || Byte.class.isInstance(obj)
                    || Boolean.class.isInstance(obj)) {
                buf.append(format("@%s[%s]", className, obj));
            }

            // Char要特殊处理,因为有不可见字符的因素
            else if (Character.class.isInstance(obj)) {

                final Character c = (Character) obj;

                // ASCII的可见字符
                if (c >= 32
                        && c <= 126) {
                    buf.append(format("@%s[%s]", className, c));
                }

                // ASCII的控制字符
                else if (ASCII_MAP.containsKey((byte) c.charValue())) {
                    buf.append(format("@%s[%s]", className, ASCII_MAP.get((byte) c.charValue())));
                }

                // 超过ASCII的编码范围
                else {
                    buf.append(format("@%s[%s]", className, c));
                }

            }

            // 字符串类型单独处理
            else if (String.class.isInstance(obj)) {
                final StringBuilder bufOfString = new StringBuilder();
                for (Character c : ((String) obj).toCharArray()) {
                    switch (c) {
                        case '\n':
                            bufOfString.append("\\n");
                            break;
                        case '\r':
                            bufOfString.append("\\r");
                            break;
                        default:
                            bufOfString.append(c);
                    }//switch
                }//for
                buf.append(format("@%s[%s]", className, bufOfString));
            }

            // 集合类输出
            else if (Collection.class.isInstance(obj)) {

                @SuppressWarnings("unchecked") final Collection<Object> collection = (Collection<Object>) obj;

                // 非根节点或空集合只展示摘要信息
                if (!isExpand(deep, expand)
                        || collection.isEmpty()) {

                    buf.append(format("@%s[isTop=%s;size=%d]",
                            className,
                            collection.isEmpty(),
                            collection.size()));

                }

                // 展开展示
                else {

                    final StringBuilder bufOfCollection = new StringBuilder();
                    bufOfCollection.append(format("@%s[", className));
                    for (Object e : collection) {
                        bufOfCollection.append("\n").append(toString(e, deep + 1, expand)).append(",\n");
                    }
                    bufOfCollection.append("]");
                    buf.append(bufOfCollection);
                }

            }


            // Map类输出
            else if (Map.class.isInstance(obj)) {

                @SuppressWarnings("unchecked") final Map<Object, Object> map = (Map<Object, Object>) obj;

                // 非根节点或空集合只展示摘要信息
                if (!isExpand(deep, expand)
                        || map.isEmpty()) {

                    buf.append(format("@%s[isTop=%s;size=%d]",
                            className,
                            map.isEmpty(),
                            map.size()));

                } else {

                    final StringBuilder bufOfMap = new StringBuilder();
                    bufOfMap.append(format("@%s[", className));
                    for (Map.Entry<Object, Object> entry : map.entrySet()) {
                        bufOfMap.append("\n").append(toString(entry.getKey(), deep + 1, expand))
                                .append(":")
                                .append(toString(entry.getValue(), deep + 1, expand).trim())
                                .append(",\n");
                    }
                    bufOfMap.append("]");
                    buf.append(bufOfMap);
                }

            }


            // 数组类输出
            else if (obj.getClass().isArray()) {


                final String typeName = obj.getClass().getSimpleName();

                // int[]
                if (typeName.equals("int[]")) {

                    final int[] arrays = (int[]) obj;
                    // 非根节点或空集合只展示摘要信息
                    if (!isExpand(deep, expand)
                            || arrays.length == 0) {

                        buf.append(format("@%s[isTop=%s;size=%d]",
                                typeName,
                                arrays.length == 0,
                                arrays.length));

                    }

                    // 展开展示
                    else {

                        final StringBuilder bufOfArray = new StringBuilder();
                        bufOfArray.append(format("@%s[", className));
                        for (Object e : arrays) {
                            bufOfArray.append("\n").append(toString(e, deep + 1, expand)).append(",");
                        }
                        bufOfArray.append("\n]");
                        buf.append(bufOfArray);
                    }

                }

                // long[]
                else if (typeName.equals("long[]")) {

                    final long[] arrays = (long[]) obj;
                    // 非根节点或空集合只展示摘要信息
                    if (!isExpand(deep, expand)
                            || arrays.length == 0) {

                        buf.append(format("@%s[isTop=%s;size=%d]",
                                typeName,
                                arrays.length == 0,
                                arrays.length));

                    }

                    // 展开展示
                    else {

                        final StringBuilder bufOfArray = new StringBuilder();
                        bufOfArray.append(format("@%s[", className));
                        for (Object e : arrays) {
                            bufOfArray.append("\n").append(toString(e, deep + 1, expand)).append(",");
                        }
                        bufOfArray.append("\n]");
                        buf.append(bufOfArray);
                    }

                }

                // short[]
                else if (typeName.equals("short[]")) {

                    final short[] arrays = (short[]) obj;
                    // 非根节点或空集合只展示摘要信息
                    if (!isExpand(deep, expand)
                            || arrays.length == 0) {

                        buf.append(format("@%s[isTop=%s;size=%d]",
                                typeName,
                                arrays.length == 0,
                                arrays.length));

                    }

                    // 展开展示
                    else {

                        final StringBuilder bufOfArray = new StringBuilder();
                        bufOfArray.append(format("@%s[", className));
                        for (Object e : arrays) {
                            bufOfArray.append("\n").append(toString(e, deep + 1, expand)).append(",");
                        }
                        bufOfArray.append("\n]");
                        buf.append(bufOfArray);
                    }

                }

                // float[]
                else if (typeName.equals("float[]")) {

                    final float[] arrays = (float[]) obj;
                    // 非根节点或空集合只展示摘要信息
                    if (!isExpand(deep, expand)
                            || arrays.length == 0) {

                        buf.append(format("@%s[isTop=%s;size=%d]",
                                typeName,
                                arrays.length == 0,
                                arrays.length));

                    }

                    // 展开展示
                    else {

                        final StringBuilder bufOfArray = new StringBuilder();
                        bufOfArray.append(format("@%s[", className));
                        for (Object e : arrays) {
                            bufOfArray.append("\n").append(toString(e, deep + 1, expand)).append(",");
                        }
                        bufOfArray.append("\n]");
                        buf.append(bufOfArray);
                    }

                }

                // double[]
                else if (typeName.equals("double[]")) {

                    final double[] arrays = (double[]) obj;
                    // 非根节点或空集合只展示摘要信息
                    if (!isExpand(deep, expand)
                            || arrays.length == 0) {

                        buf.append(format("@%s[isTop=%s;size=%d]",
                                typeName,
                                arrays.length == 0,
                                arrays.length));

                    }

                    // 展开展示
                    else {

                        final StringBuilder bufOfArray = new StringBuilder();
                        bufOfArray.append(format("@%s[", className));
                        for (Object e : arrays) {
                            bufOfArray.append("\n").append(toString(e, deep + 1, expand)).append(",");
                        }
                        bufOfArray.append("\n]");
                        buf.append(bufOfArray);
                    }

                }

                // boolean[]
                else if (typeName.equals("boolean[]")) {

                    final boolean[] arrays = (boolean[]) obj;
                    // 非根节点或空集合只展示摘要信息
                    if (!isExpand(deep, expand)
                            || arrays.length == 0) {

                        buf.append(format("@%s[isTop=%s;size=%d]",
                                typeName,
                                arrays.length == 0,
                                arrays.length));

                    }

                    // 展开展示
                    else {

                        final StringBuilder bufOfArray = new StringBuilder();
                        bufOfArray.append(format("@%s[", className));
                        for (Object e : arrays) {
                            bufOfArray.append("\n").append(toString(e, deep + 1, expand)).append(",");
                        }
                        bufOfArray.append("\n]");
                        buf.append(bufOfArray);
                    }

                }

                // char[]
                else if (typeName.equals("char[]")) {

                    final char[] arrays = (char[]) obj;
                    // 非根节点或空集合只展示摘要信息
                    if (!isExpand(deep, expand)
                            || arrays.length == 0) {

                        buf.append(format("@%s[isTop=%s;size=%d]",
                                typeName,
                                arrays.length == 0,
                                arrays.length));

                    }

                    // 展开展示
                    else {

                        final StringBuilder bufOfArray = new StringBuilder();
                        bufOfArray.append(format("@%s[", className));
                        for (Object e : arrays) {
                            bufOfArray.append("\n").append(toString(e, deep + 1, expand)).append(",");
                        }
                        bufOfArray.append("\n]");
                        buf.append(bufOfArray);
                    }

                }

                // byte[]
                else if (typeName.equals("byte[]")) {

                    final byte[] arrays = (byte[]) obj;
                    // 非根节点或空集合只展示摘要信息
                    if (!isExpand(deep, expand)
                            || arrays.length == 0) {

                        buf.append(format("@%s[isTop=%s;size=%d]",
                                typeName,
                                arrays.length == 0,
                                arrays.length));

                    }

                    // 展开展示
                    else {

                        final StringBuilder bufOfArray = new StringBuilder();
                        bufOfArray.append(format("@%s[", className));
                        for (Object e : arrays) {
                            bufOfArray.append("\n").append(toString(e, deep + 1, expand)).append(",");
                        }
                        bufOfArray.append("\n]");
                        buf.append(bufOfArray);
                    }

                }

                // Object[]
                else {
                    final Object[] arrays = (Object[]) obj;
                    // 非根节点或空集合只展示摘要信息
                    if (!isExpand(deep, expand)
                            || arrays.length == 0) {

                        buf.append(format("@%s[isTop=%s;size=%d]",
                                typeName,
                                arrays.length == 0,
                                arrays.length));

                    }

                    // 展开展示
                    else {

                        final StringBuilder bufOfArray = new StringBuilder();
                        bufOfArray.append(format("@%s[", className));
                        for (Object e : arrays) {
                            bufOfArray.append("\n").append(toString(e, deep + 1, expand)).append(",");
                        }
                        bufOfArray.append("\n]");
                        buf.append(bufOfArray);
                    }
                }

            }


            // Throwable输出
            else if (Throwable.class.isInstance(obj)) {

                if (!isExpand(deep, expand)) {
                    buf.append(format("@%s[%s]", className, obj));
                } else {

                    final Throwable throwable = (Throwable) obj;
                    final StringWriter sw = new StringWriter();
                    final PrintWriter pw = new PrintWriter(sw);
                    throwable.printStackTrace(pw);

//                    buf.append(format("@%s[\n",className));
                    buf.append(sw.toString());
//                    buf.append("]");

                }

            }

            // Date输出
            else if (Date.class.isInstance(obj)) {
                buf.append(format("@%s[%s]", className, SimpleDateFormatHolder.getInstance().format((Date) obj)));
            }

            // 普通Object输出
            else {

                if (!isExpand(deep, expand)) {
                    buf.append(format("@%s[%s]", className, obj));
                } else {

                    final StringBuilder bufOfObject = new StringBuilder();
                    bufOfObject.append(format("@%s[", className));
                    final Field[] fields = obj.getClass().getDeclaredFields();
                    if (null != fields) {
                        for (Field field : fields) {

                            if (!field.isAccessible()) {
                                field.setAccessible(true);
                            }

                            try {

                                final Object value = field.get(obj);

                                bufOfObject.append("\n").append(TAB).append(field.getName())
                                        .append("=")
                                        .append(toString(value, deep + 1, expand).trim())
                                        .append(",");

                            } catch (Throwable t) {
                                //
                            }

                        }//for
                        bufOfObject.append("\n");
                    }//if
                    bufOfObject.append("]");
                    buf.append(bufOfObject);

                }

            }

        }

        // TAB偏移
        final StringBuilder tabBuf = new StringBuilder();
        Scanner scanner = null;
        try {
            scanner = new Scanner(new StringReader(buf.toString()));
            while (scanner.hasNextLine()) {
                final String line = scanner.nextLine();
                if (!isRoot(deep)) {
                    tabBuf.append(TAB);
                }
                tabBuf.append(line).append("\n");
            }
        } finally {
            if (null != scanner) {
                scanner.close();
            }
        }
        if (tabBuf.length() > 0) {
            tabBuf.deleteCharAt(tabBuf.length() - 1);
        }

        return tabBuf.toString();

    }

    /**
     * 是否根节点
     *
     * @param deep 深度
     * @return true:根节点 / false:非根节点
     */
    private static boolean isRoot(int deep) {
        return deep == 0;
    }


    /**
     * 是否展开当前深度的节点
     *
     * @param deep   当前节点的深度
     * @param expand 展开极限
     * @return true:当前节点需要展开 / false:当前节点不需要展开
     */
    private static boolean isExpand(int deep, int expand) {
        return deep < expand;
    }

}
