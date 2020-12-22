package com.alibaba.jvm.sandbox.core.util.matcher.structure;


import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

/**
 * 类结构工厂类
 * <p>
 * 根据构造方式的不同，返回的实现方式也不一样。但无论哪一种实现方式都尽可能符合接口约定。
 * </p>
 *
 * @author luanjia@taobao.com
 */
public class ClassStructureFactory {

    private static final Logger logger = LoggerFactory.getLogger(ClassStructureFactory.class);

    // cache stat: hit ratio: 1024 - 41% ; 2048 - 56%
    // 最佳的性能应该是Caffeine，但是jdk版本不够
    private final static LoadingCache<Class<?>, ClassStructure> CLASS_STRUCTURE_CACHE
            = CacheBuilder.newBuilder().expireAfterWrite(15, TimeUnit.MINUTES)
            .maximumSize(2048).build(new CacheLoader<Class<?>, ClassStructure>() {
                @Override
                public ClassStructure load(Class<?> key) {
                    return new ClassStructureImplByJDK(key);
                }
            });

    /**
     * 通过Class类来构造类结构
     *
     * @param clazz 目标Class类
     * @return JDK实现的类结构
     */
    public static ClassStructure createClassStructure(final Class<?> clazz) {
        return CLASS_STRUCTURE_CACHE.getUnchecked(clazz);
    }

    /**
     * 通过Class类字节流来构造类结构
     *
     * @param classInputStream Class类字节流
     * @param loader           即将装载Class的ClassLoader
     * @return ASM实现的类结构
     */
    public static ClassStructure createClassStructure(final InputStream classInputStream,
                                                      final ClassLoader loader) {
        try {
            return new ClassStructureImplByAsm(classInputStream, loader);
        } catch (IOException cause) {
            logger.warn("create class structure failed by using ASM, return null. loader={};", loader, cause);
            return null;
        }
    }

    /**
     * 通过Class类字节数组来构造类结构
     *
     * @param classByteArray Class类字节数组
     * @param loader         即将装载Class的ClassLoader
     * @return ASM实现的类结构
     */
    public static ClassStructure createClassStructure(final byte[] classByteArray,
                                                      final ClassLoader loader) {
        return new ClassStructureImplByAsm(classByteArray, loader);
    }

}
