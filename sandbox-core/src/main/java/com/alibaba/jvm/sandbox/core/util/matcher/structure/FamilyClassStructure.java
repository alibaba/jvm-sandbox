package com.alibaba.jvm.sandbox.core.util.matcher.structure;

import com.alibaba.jvm.sandbox.core.util.LazyGet;

import java.lang.annotation.Inherited;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

public abstract class FamilyClassStructure implements ClassStructure {

    private final LazyGet<Set<ClassStructure>> familyInterfaceClassStructuresLazyGet
            = new LazyGet<Set<ClassStructure>>() {
        @Override
        protected Set<ClassStructure> initialValue() throws Throwable {
            final Set<ClassStructure> familyInterfaceClassStructures = new HashSet<ClassStructure>();
            for (final ClassStructure interfaceClassStructure : getInterfaceClassStructures()) {
                // 1. 先加自己声明的接口
                familyInterfaceClassStructures.add(interfaceClassStructure);
                // 2. 再加接口所声明的祖先(接口继承)
                familyInterfaceClassStructures.addAll(interfaceClassStructure.getFamilyInterfaceClassStructures());
            }
            return familyInterfaceClassStructures;
        }
    };

    @Override
    public Set<ClassStructure> getFamilyInterfaceClassStructures() {
        return familyInterfaceClassStructuresLazyGet.get();
    }


    private final LazyGet<Set<ClassStructure>> familyTypeClassStructuresLazyGet
            = new LazyGet<Set<ClassStructure>>() {
        @Override
        protected Set<ClassStructure> initialValue() throws Throwable {
            final Set<ClassStructure> familyClassStructures = new LinkedHashSet<ClassStructure>();

            // 注入家族类&家族类所声明的家族接口
            for (final ClassStructure familySuperClassStructure : getFamilySuperClassStructures()) {
                familyClassStructures.add(familySuperClassStructure);
                familyClassStructures.addAll(familySuperClassStructure.getFamilyInterfaceClassStructures());
            }

            // 注入家族接口
            for (final ClassStructure familyInterfaceClassStructure : getFamilyInterfaceClassStructures()) {
                familyClassStructures.add(familyInterfaceClassStructure);
                familyClassStructures.addAll(familyInterfaceClassStructure.getFamilyInterfaceClassStructures());
            }
            return familyClassStructures;
        }
    };

    @Override
    public Set<ClassStructure> getFamilyTypeClassStructures() {
        return familyTypeClassStructuresLazyGet.get();
    }

    // 当前类结构是否一个可被继承的Annotation类结构
    private static boolean isInheritedAnnotationType(ClassStructure classStructure) {
        if (!classStructure.getAccess().isAnnotation()) {
            return false;
        }
        for (final ClassStructure annotationTypeClassStructure : classStructure.getAnnotationTypeClassStructures()) {
            if (Inherited.class.getName().equals(annotationTypeClassStructure.getJavaClassName())) {
                return true;
            }
        }
        return false;
    }

    // 过滤掉没有@Inherited标注的Annotation，因为他们不能继承
    private Set<ClassStructure> filterInheritedAnnotationTypeClassStructure(final Set<ClassStructure> classStructures) {
        final Iterator<ClassStructure> itCs = new HashSet<ClassStructure>(classStructures).iterator();
        while (itCs.hasNext()) {
            final ClassStructure annotationTypeClassStructure = itCs.next();
            if (!isInheritedAnnotationType(annotationTypeClassStructure)) {
                itCs.remove();
            }
        }
        return classStructures;
    }

    private final LazyGet<Set<ClassStructure>> familyAnnotationTypeClassStructuresLazyGet
            = new LazyGet<Set<ClassStructure>>() {
        @Override
        protected Set<ClassStructure> initialValue() throws Throwable {
            final Set<ClassStructure> familyAnnotationTypeClassStructures = new HashSet<ClassStructure>(getAnnotationTypeClassStructures());
            for (final ClassStructure familyClassStructure : getFamilyTypeClassStructures()) {
                familyAnnotationTypeClassStructures.addAll(
                        filterInheritedAnnotationTypeClassStructure(
                                familyClassStructure.getFamilyAnnotationTypeClassStructures()
                        )
                );//addAll
            }//for
            return familyAnnotationTypeClassStructures;
        }
    };

    @Override
    public Set<ClassStructure> getFamilyAnnotationTypeClassStructures() {
        return familyAnnotationTypeClassStructuresLazyGet.get();
    }

    private final LazyGet<LinkedHashSet<ClassStructure>> familySuperClassStructuresLazyGet
            = new LazyGet<LinkedHashSet<ClassStructure>>() {
        @Override
        protected LinkedHashSet<ClassStructure> initialValue() throws Throwable {
            final LinkedHashSet<ClassStructure> familySuperClassStructures = new LinkedHashSet<ClassStructure>();
            final ClassStructure superClassStructure = getSuperClassStructure();
            if (null != superClassStructure) {
                // 1. 先加自己的父类
                familySuperClassStructures.add(superClassStructure);
                // 2. 再加父类的祖先
                familySuperClassStructures.addAll(superClassStructure.getFamilySuperClassStructures());
            }
            return familySuperClassStructures;
        }
    };

    @Override
    public LinkedHashSet<ClassStructure> getFamilySuperClassStructures() {
        return familySuperClassStructuresLazyGet.get();
    }

    @Override
    public int hashCode() {
        return getJavaClassName().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof ClassStructure
                && getJavaClassName().equals(((ClassStructure) obj).getJavaClassName());
    }

}
