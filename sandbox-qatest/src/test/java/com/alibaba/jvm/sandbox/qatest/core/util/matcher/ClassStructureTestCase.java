package com.alibaba.jvm.sandbox.qatest.core.util.matcher;

import com.alibaba.jvm.sandbox.core.util.matcher.structure.Access;
import com.alibaba.jvm.sandbox.core.util.matcher.structure.BehaviorStructure;
import com.alibaba.jvm.sandbox.core.util.matcher.structure.ClassStructure;
import com.alibaba.jvm.sandbox.qatest.core.enhance.target.Calculator;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;

import static com.alibaba.jvm.sandbox.core.util.matcher.structure.ClassStructureFactory.createClassStructure;
import static com.alibaba.jvm.sandbox.qatest.util.QaClassUtils.toByteArray;
import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;

/**
 * 类结构测试用例
 */
@RunWith(Parameterized.class)
public class ClassStructureTestCase {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Parameterized.Parameters
    public static Collection<Object[]> getData() {
        return asList(new Object[][]{
                new Object[]{PublicUser.class},
                new Object[]{Calculator.class}
        });
    }

    private final Class<?> clazz;

    public ClassStructureTestCase(Class<?> clazz) {
        this.clazz = clazz;
    }


    private ClassStructure genByAsm() throws IOException {
        return createClassStructure(toByteArray(clazz), clazz.getClassLoader());
    }

    private ClassStructure genByJdk() {
        return createClassStructure(clazz);
    }

    private void assertAccessEquals(Access acAsm, Access acJdk) {
        assertEquals("isPublic", acAsm.isPublic(), acJdk.isPublic());
        assertEquals("isPrivate", acAsm.isPrivate(), acJdk.isPrivate());
        assertEquals("isProtected",acAsm.isProtected(), acJdk.isProtected());
        assertEquals("isStatic", acAsm.isStatic(), acJdk.isStatic());
        assertEquals("isFinal", acAsm.isFinal(), acJdk.isFinal());
        assertEquals("isInterface", acAsm.isInterface(), acJdk.isInterface());
        assertEquals("isNative", acAsm.isNative(), acJdk.isNative());
        assertEquals("isAbstract", acAsm.isAbstract(), acJdk.isAbstract());
        assertEquals("isEnum", acAsm.isEnum(), acJdk.isEnum());
        assertEquals("isAnnotation", acAsm.isAnnotation(), acJdk.isAnnotation());
    }

    private void assertClassStructureListEquals(Collection<ClassStructure> csAsmCollection, Collection<ClassStructure> csJdkCollection) {
        assertEquals(csAsmCollection.size(), csJdkCollection.size());
        final List<ClassStructure> csAsmList = new ArrayList<ClassStructure>(csAsmCollection);
        final List<ClassStructure> csJdkList = new ArrayList<ClassStructure>(csJdkCollection);
        for (int index = 0; index < csAsmCollection.size(); index++) {
            assertClassStructureEquals(csAsmList.get(index), csJdkList.get(index));
        }
    }

    private void assertBehaviorStructureListEquals(List<BehaviorStructure> bsAsmList, List<BehaviorStructure> bsJdkList) {
        assertEquals(String.format("asm:[%s];jdk:[%s]", bsAsmList, bsJdkList), bsAsmList.size(), bsJdkList.size());
        for (int index = 0; index < bsAsmList.size(); index++) {
            assertBehaviorStructureEquals(bsAsmList.get(index), bsJdkList.get(index));
        }
    }

    private void assertBehaviorStructureEquals(BehaviorStructure bsAsm, BehaviorStructure bsJdk) {
        assertEquals(bsAsm.getName(), bsJdk.getName());
        logger.info("assert {}#{}", bsAsm.getDeclaringClassStructure().getJavaClassName(), bsAsm.getName());

        assertClassStructureEquals(bsAsm.getDeclaringClassStructure(), bsJdk.getDeclaringClassStructure());
        assertAccessEquals(bsAsm.getAccess(), bsJdk.getAccess());
        assertClassStructureEquals(bsAsm.getReturnTypeClassStructure(), bsJdk.getReturnTypeClassStructure());

        assertClassStructureListEquals(bsAsm.getParameterTypeClassStructures(), bsJdk.getParameterTypeClassStructures());
    }

    private Set<String> uniqueClassStructureCheck = new HashSet<String>();

    private void assertClassStructureEquals(ClassStructure csAsm, ClassStructure csJdk) {
        if (csAsm == null
                || csJdk == null) {
            assertEquals(csAsm, csJdk);
            return;
        }

        assertEquals(csAsm.getJavaClassName(), csJdk.getJavaClassName());

        if (uniqueClassStructureCheck.contains(csAsm.getJavaClassName())) {
            return;
        } else {
            uniqueClassStructureCheck.add(csAsm.getJavaClassName());
        }

        logger.info("assert CS:{}", csAsm.getJavaClassName());

        assertAccessEquals(csAsm.getAccess(), csJdk.getAccess());
        assertEquals(csAsm.getClassLoader(), csJdk.getClassLoader());
        assertClassStructureEquals(csAsm.getSuperClassStructure(), csJdk.getSuperClassStructure());
        assertClassStructureListEquals(csAsm.getInterfaceClassStructures(), csJdk.getInterfaceClassStructures());
        assertClassStructureListEquals(csAsm.getFamilySuperClassStructures(), csJdk.getFamilySuperClassStructures());
        assertClassStructureListEquals(csAsm.getFamilyInterfaceClassStructures(), csJdk.getFamilyInterfaceClassStructures());
        assertClassStructureListEquals(csAsm.getFamilyTypeClassStructures(), csJdk.getFamilyTypeClassStructures());
        assertClassStructureListEquals(csAsm.getAnnotationTypeClassStructures(), csJdk.getAnnotationTypeClassStructures());
        assertClassStructureListEquals(csAsm.getFamilyAnnotationTypeClassStructures(), csJdk.getFamilyAnnotationTypeClassStructures());
        assertBehaviorStructureListEquals(sortBehaviorStructureCollectionByName(csAsm.getBehaviorStructures()), sortBehaviorStructureCollectionByName(csJdk.getBehaviorStructures()));

    }

    private List<BehaviorStructure> sortBehaviorStructureCollectionByName(Collection<BehaviorStructure> bsCollection) {
        final List<BehaviorStructure> sortedBsList;
        Collections.sort(sortedBsList = new ArrayList<BehaviorStructure>(bsCollection), new Comparator<BehaviorStructure>() {
            @Override
            public int compare(BehaviorStructure o1, BehaviorStructure o2) {
                return o1.getName().compareTo(o2.getName());
            }
        });
        return sortedBsList;
    }


    @Test
    public void assertTheSame_Asm_Jdk() throws IOException {

        final ClassStructure csByAsm = genByAsm();
        final ClassStructure csByJdk = genByJdk();

        assertClassStructureEquals(csByAsm, csByJdk);

    }

}
