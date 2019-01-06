package com.alibaba.jvm.sandbox.qatest.core.util;

import org.apache.commons.lang3.StringUtils;

import static org.apache.commons.lang3.ArrayUtils.getLength;
import static org.junit.Assert.assertEquals;

public class AssertUtils {

    public static <E> void assertArrayEquals(E[] exceptArray, E[] actualArray) {
        assertEquals(
                String.format(
                        "except size not matched!\n\texcept:%s\n\tactual:%s",
                        StringUtils.join(exceptArray, ","),
                        StringUtils.join(actualArray, ",")
                ),
                getLength(exceptArray),
                getLength(actualArray)
        );
        for (int index = 0; index < exceptArray.length; index++) {
            assertEquals("[" + index + "] not matched", exceptArray[index], actualArray[index]);
        }
    }

}
