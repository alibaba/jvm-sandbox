package com.alibaba.jvm.sandbox.core.util.matcher.inner;

import com.alibaba.jvm.sandbox.api.filter.ExtFilter;
import com.alibaba.jvm.sandbox.core.util.matcher.structure.ClassStructure;

import java.util.ArrayList;
import java.util.Collection;

import static com.alibaba.jvm.sandbox.core.util.ClassStructureUtils.toJavaClassNameArray;

/**
 * A {@link ClassMatcher} with {@link ExtFilter}
 *
 * @author <a href="mailto:renyi.cry@alibaba-inc.com">renyi.cry<a/>
 * @date 2020/11/9 12:58 上午
 */
public class ClassExtFilterMatcher implements ClassMatcher {

    private final ExtFilter extFilter;

    public ClassExtFilterMatcher(ExtFilter extFilter) {
        this.extFilter = extFilter;
    }

    @Override
    public boolean matches(ClassStructure classStructure) {

        // 如果不开启加载Bootstrap的类，遇到就过滤掉
        if (!extFilter.isIncludeBootstrap() && classStructure.getClassLoader() == null) {
            return false;
        }

        // 匹配ClassStructure
        for (final ClassStructure wmCs : getWaitingMatchClassStructures(classStructure)) {

            ClassStructure superClassStructure = wmCs.getSuperClassStructure();
            String[] interfaceTypeJavaClassNameArray = extFilter.hasInterfaceTypes() ?
                    toJavaClassNameArray(wmCs.getFamilyInterfaceClassStructures()) : new String[]{};
            String[] annotationTypeJavaClassNameArray = extFilter.hasAnnotationTypes() ?
                    toJavaClassNameArray(wmCs.getFamilyAnnotationTypeClassStructures()) : new String[]{};

            // 匹配类结构
            if (extFilter.doClassFilter(
                    wmCs.getAccess().getAccessCode(),
                    wmCs.getJavaClassName(),
                    null == superClassStructure ? null : superClassStructure.getJavaClassName(),
                    interfaceTypeJavaClassNameArray,
                    annotationTypeJavaClassNameArray)) {

                return true;

            }
        }

        return false;

    }

    @Override
    public boolean hasClassIdentity() {
        return extFilter.hasClassIdentity();
    }

    @Override
    public String getClassIdentity() {
        return extFilter.getClassIdentity();
    }

    // 获取需要匹配的类结构
    // 如果要匹配子类就需要将这个类的所有家族成员找出
    private Collection<ClassStructure> getWaitingMatchClassStructures(final ClassStructure classStructure) {
        final Collection<ClassStructure> waitingMatchClassStructures = new ArrayList<ClassStructure>();
        waitingMatchClassStructures.add(classStructure);
        if (extFilter.isIncludeSubClasses()) {
            waitingMatchClassStructures.addAll(classStructure.getFamilyTypeClassStructures());
        }
        return waitingMatchClassStructures;
    }

}
