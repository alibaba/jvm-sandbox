package com.alibaba.jvm.sandbox.qatest.core.util;

import com.alibaba.jvm.sandbox.core.util.SandboxProtector;
import org.junit.Assert;
import org.junit.Test;

public class SandboxProtectorTestCase {

    private final SandboxProtector protector = new SandboxProtector();

    public interface Person {

        Person evolution();

    }

    class MyFather implements Person {

        private final Person son;

        MyFather(final Person son) {
            this.son = protector.protectProxy(Person.class, son);
        }

        @Override
        public Person evolution() {
            return son.evolution();
        }

    }

    class Me implements Person {

        @Override
        public Person evolution() {
            return null;
        }

    }


    @Test
    public void test$protectProxy() {

        final Person myFather = protector.protectProxy(Person.class, new MyFather(new Me() {
            @Override
            public Person evolution() {
                Assert.assertTrue(protector.isInProtecting());
                return super.evolution();
            }
        }));
        for (int i = 0; i < 10; i++) {
            Assert.assertNull(myFather.evolution());
        }

        Assert.assertFalse(protector.isInProtecting());

    }

}
