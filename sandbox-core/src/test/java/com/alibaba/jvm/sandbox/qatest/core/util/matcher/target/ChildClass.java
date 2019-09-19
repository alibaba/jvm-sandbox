package com.alibaba.jvm.sandbox.qatest.core.util.matcher.target;

public class ChildClass extends ParentClass {

    @Override
    public void methodOfSumIntArray(int... intArray) {

    }

    public interface PublicInterface {
    }

    protected interface ProtectedInterface {

    }

    public static class PublicStaticClass {

        PublicStaticClass(int... array) {

        }

        PublicStaticClass(String... array) {

        }

    }

    protected static class ProtectedStaticClass {

    }

    class InnerClass {

    }

    public enum PublicEnum {

    }

    protected enum ProtectedEnum {

    }

    public PublicInterface methodOfReturnPublicInterface() {
        return null;
    }

    public ProtectedInterface methodOfReturnProtectedInterface() {
        return null;
    }

    public PublicStaticClass methodOfReturnPublicStaticClass() {
        return null;
    }

    public ProtectedStaticClass methodOfReturnProtectedStaticClass() {
        return null;
    }

    public PublicEnum methodOfReturnPublicEnum() {
        return null;
    }

    public InnerClass methodOfReturnInnerClass() {
        return null;
    }

    public ProtectedEnum methodOfReturnProtectedEnum() {
        return null;
    }

    public void methodOfSingleArguments(
            final PublicInterface publicInterface,
            final ProtectedInterface protectedInterface,
            final PublicStaticClass publicStaticClass,
            final ProtectedStaticClass protectedStaticClass,
            final InnerClass innerClass,
            final PublicEnum publicEnum,
            final ProtectedEnum protectedEnum
    ) {

    }

    public void methodOfArrayArguments(
            final PublicInterface[] publicInterface,
            final ProtectedInterface[] protectedInterface,
            final PublicStaticClass[] publicStaticClass,
            final ProtectedStaticClass[] protectedStaticClass,
            final InnerClass[] innerClass,
            final PublicEnum[] publicEnum,
            final ProtectedEnum[] protectedEnum
    ) {

    }


    @InheritedAnnotation
    public void methodOfChildClassWithAnnotation() throws RuntimeException {

    }

    private static void methodOfPrivateStatic() {

    }

    private native void methodOfPrivateNative();

    @Override
    void methodOfParentIsAbstract() {

    }

    @Override
    public void methodOfParentInterfaceFirstFirstWithAnnotation() {

    }

}
