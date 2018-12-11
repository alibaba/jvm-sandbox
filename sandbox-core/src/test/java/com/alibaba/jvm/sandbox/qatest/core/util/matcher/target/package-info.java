/**
 * 这个包中的类构建和描述一个类结构，用于测试{@link com.alibaba.jvm.sandbox.core.util.matcher.structure.ClassStructureFactory}
 *
 * <li>
 * 类继承结构
 * <pre>
 * {@code
 *
 *   ChildClass
 *            |
 *        <extends>
 *            |
 *            ` ParentClass <implements> IParentInterfaceFirst <extends> IParentInterfaceFirstFirst
 *                        |            |                               |
 *                        |            |                               ` IParentInterfaceFirstSecond
 *                        |            ` IParentInterfaceSecond
 *                        |
 *                    <extends>
 *                        |
 *                        ` GrandpaClass <implements> IGrandpaInterfaceFirst <extends> IGrandpaInterfaceFirstFirst
 *
 * }
 * </pre>
 * </li>
 */
package com.alibaba.jvm.sandbox.qatest.core.util.matcher.target;