package java.com.alibaba.jvm.sandbox.spy;

/**
 * 间谍处理器
 *
 * @since {@code sandbox-spy:1.3.0}
 */
public interface SpyHandler {

    /**
     * 处理调用方法:执行之前
     * <p>CALL-BEFORE</p>
     *
     * @param listenerId 事件监听器ID
     * @param lineNumber 发生调用方法的代码行号
     * @param owner      调用方法的声明类
     * @param name       调用方法的方法名
     * @param desc       调用方法的方法描述
     * @throws Throwable 处理${调用方法:执行之前}失败
     */
    void handleOnCallBefore(int listenerId, int lineNumber, String owner, String name, String desc) throws Throwable;

    /**
     * 处理调用方法:正常返回
     * <p>CALL-RETURN</p>
     *
     * @param listenerId 事件监听器ID
     * @throws Throwable 处理{调用方法:正常返回}失败
     */
    void handleOnCallReturn(int listenerId) throws Throwable;

    /**
     * 处理调用方法:异常返回
     * <p>CALL-THROWS</p>
     *
     * @param listenerId     事件监听器ID
     * @param throwException 异常返回的异常类型
     * @throws Throwable 处理{调用方法:异常返回}失败
     */
    void handleOnCallThrows(int listenerId, String throwException) throws Throwable;

    /**
     * 处理执行代码执行
     * <p>LINE</p>
     *
     * @param listenerId 事件监听器ID
     * @param lineNumber 代码执行行号
     * @throws Throwable 处理代码执行行失败
     */
    void handleOnLine(int listenerId, int lineNumber) throws Throwable;

    /**
     * 处理方法调用:调用之前
     * <p>BEFORE</p>
     *
     * @param listenerId                事件监听器ID
     * @param targetClassLoaderObjectID 类所在ClassLoader
     * @param argumentArray             参数数组
     * @param javaClassName             类名
     * @param javaMethodName            方法名
     * @param javaMethodDesc            方法签名
     * @param target                    目标对象实例
     * @return Spy流程控制结果
     * @throws Throwable 处理{方法调用:调用之前}失败
     */
    Spy.Ret handleOnBefore(int listenerId, int targetClassLoaderObjectID, Object[] argumentArray, String javaClassName, String javaMethodName, String javaMethodDesc, Object target) throws Throwable;

    /**
     * 处理方法调用:异常返回
     *
     * @param listenerId 事件监听器ID
     * @param throwable  异常返回的异常实例
     * @return Spy流程控制结果
     * @throws Throwable 处理{方法调用:异常返回}失败
     */
    Spy.Ret handleOnThrows(int listenerId, Throwable throwable) throws Throwable;

    /**
     * 处理方法调用:正常返回
     *
     * @param listenerId 事件监听器ID
     * @param object     正常返回的对象实例
     * @return Spy流程控制结果
     * @throws Throwable 处理{方法调用:正常返回}失败
     */
    Spy.Ret handleOnReturn(int listenerId, Object object) throws Throwable;

}