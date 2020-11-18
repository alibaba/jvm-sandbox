package com.alibaba.jvm.sandbox.core.util;


import com.alibaba.jvm.sandbox.core.util.matcher.structure.ClassStructure;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Utilize for {@link ClassStructure}
 *
 * @author <a href="mailto:renyi.cry@alibaba-inc.com">renyi.cry<a/>
 * @date 2020/11/9 1:19 上午
 */
public abstract class ClassStructureUtils {

    public static String[] toJavaClassNameArray(final Collection<ClassStructure> classStructures) {
        if (null == classStructures) {
            return null;
        }
        final List<String> javaClassNames = new ArrayList<String>();
        for (final ClassStructure classStructure : classStructures) {
            javaClassNames.add(classStructure.getJavaClassName());
        }
        return javaClassNames.toArray(new String[0]);
    }

}
