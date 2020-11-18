package com.alibaba.jvm.sandbox.api.filter;

/**
 * <p>Define a unique class with a given id. The wrapped class would
 * not be a unique class iff {@link #hasClassIdentity} is false.
 *
 * This class is useful when you decide to enhance the performance
 * when finding appropriate classes.
 *
 * <code>
 *     if (idClazz.hasUniqueIdentity) {
 *         String id = idClazz.getUniqueIdentity();
 *         ...
 *     }
 * </code>
 *
 * @author <a href="mailto:renyi.cry@alibaba-inc.com">renyi.cry<a/>
 * @date 2020/11/9 11:20 上午
 */
public interface ClassIdentifiable {

    /**
     * Does the filter has a unique identity?
     *
     * @return
     */
    boolean hasClassIdentity();

    /**
     * Get the unique identity of the filter.
     *
     * @return
     */
    String getClassIdentity();

}
