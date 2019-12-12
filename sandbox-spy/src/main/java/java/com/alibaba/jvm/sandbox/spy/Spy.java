package java.com.alibaba.jvm.sandbox.spy;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 间谍类，藏匿在各个ClassLoader中
 * <p>
 * 从{@code 0.0.0.v}版本之后,因为要考虑能在alipay的CloudEngine环境中使用,这个环境只能向上查找java.开头的包路径.
 * 所以这里只好把Spy的包路径前缀中增加了java.开头
 * </p>
 * <p>
 * 从{@code 1.1.0}版本之后，修复了命名空间在Spy中不支持的问题
 * </p>
 *
 * @author luanjia@taobao.com
 */
public class Spy {

    /**
     * 控制Spy是否在发生异常时主动对外抛出
     * T:主动对外抛出，会中断方法
     * F:不对外抛出，只将异常信息打印出来
     */
    public static volatile boolean isSpyThrowException = false;

    private static final ConcurrentHashMap<String, SpyHandler> namespaceSpyHandlerMap
            = new ConcurrentHashMap<String, SpyHandler>();

    /**
     * 判断间谍类是否已经完成初始化
     *
     * @param namespace 命名空间
     * @return TRUE:已完成初始化;FALSE:未完成初始化;
     */
    public static boolean isInit(final String namespace) {
        return namespaceSpyHandlerMap.containsKey(namespace);
    }

    /**
     * 初始化间谍
     *
     * @param namespace  命名空间
     * @param spyHandler 间谍处理器
     * @since {@code sandbox-spy:1.3.0}
     */
    public static void init(final String namespace,
                            final SpyHandler spyHandler) {
        namespaceSpyHandlerMap.putIfAbsent(namespace, spyHandler);
    }

    /**
     * 清理间谍钩子方法
     *
     * @param namespace 命名空间
     */
    public synchronized static void clean(final String namespace) {
        namespaceSpyHandlerMap.remove(namespace);
        // 如果是最后的一个命名空间，则需要重新清理Node中所持有的Thread
        if (namespaceSpyHandlerMap.isEmpty()) {
            selfCallBarrier.cleanAndInit();
        }
    }


    // 全局序列
    private static final AtomicInteger sequenceRef = new AtomicInteger(1000);

    /**
     * 生成全局唯一序列，
     * 在JVM-SANDBOX中允许多个命名空间的存在，不同的命名空间下listenerId/objectId将会被植入到同一份字节码中，
     * 此时需要用全局的ID生成策略规避不同的命名空间
     *
     * @return 全局自增序列
     */
    public static int nextSequence() {
        return sequenceRef.getAndIncrement();
    }


    private static void handleException(Throwable cause) throws Throwable {
        if (isSpyThrowException) {
            throw cause;
        } else {
            cause.printStackTrace();
        }
    }

    private static final SelfCallBarrier selfCallBarrier = new SelfCallBarrier();

    public static void spyMethodOnCallBefore(final int lineNumber,
                                             final String owner,
                                             final String name,
                                             final String desc,
                                             final String namespace,
                                             final int listenerId) throws Throwable {
        try {
            final SpyHandler spyHandler = namespaceSpyHandlerMap.get(namespace);
            if (null != spyHandler) {
                spyHandler.handleOnCallBefore(listenerId, lineNumber, owner, name, desc);
            }
        } catch (Throwable cause) {
            handleException(cause);
        }
    }

    public static void spyMethodOnCallReturn(final String namespace,
                                             final int listenerId) throws Throwable {
        try {
            final SpyHandler spyHandler = namespaceSpyHandlerMap.get(namespace);
            if (null != spyHandler) {
                spyHandler.handleOnCallReturn(listenerId);
            }
        } catch (Throwable cause) {
            handleException(cause);
        }
    }

    public static void spyMethodOnCallThrows(final String throwException,
                                             final String namespace,
                                             final int listenerId) throws Throwable {
        try {
            final SpyHandler spyHandler = namespaceSpyHandlerMap.get(namespace);
            if (null != spyHandler) {
                spyHandler.handleOnCallThrows(listenerId, throwException);
            }
        } catch (Throwable cause) {
            handleException(cause);
        }
    }

    public static void spyMethodOnLine(final int lineNumber,
                                       final String namespace,
                                       final int listenerId) throws Throwable {
        try {
            final SpyHandler spyHandler = namespaceSpyHandlerMap.get(namespace);
            if (null != spyHandler) {
                spyHandler.handleOnLine(listenerId, lineNumber);
            }
        } catch (Throwable cause) {
            handleException(cause);
        }
    }

    public static Ret spyMethodOnBefore(final Object[] argumentArray,
                                        final String namespace,
                                        final int listenerId,
                                        final int targetClassLoaderObjectID,
                                        final String javaClassName,
                                        final String javaMethodName,
                                        final String javaMethodDesc,
                                        final Object target) throws Throwable {
        final Thread thread = Thread.currentThread();
        if (selfCallBarrier.isEnter(thread)) {
            return Ret.RET_NONE;
        }
        final SelfCallBarrier.Node node = selfCallBarrier.enter(thread);
        try {
            final SpyHandler spyHandler = namespaceSpyHandlerMap.get(namespace);
            if (null == spyHandler) {
                return Ret.RET_NONE;
            }
            return spyHandler.handleOnBefore(
                    listenerId, targetClassLoaderObjectID, argumentArray,
                    javaClassName,
                    javaMethodName,
                    javaMethodDesc,
                    target
            );
        } catch (Throwable cause) {
            handleException(cause);
            return Ret.RET_NONE;
        } finally {
            selfCallBarrier.exit(thread, node);
        }
    }

    public static Ret spyMethodOnReturn(final Object object,
                                        final String namespace,
                                        final int listenerId) throws Throwable {
        final Thread thread = Thread.currentThread();
        if (selfCallBarrier.isEnter(thread)) {
            return Ret.RET_NONE;
        }
        final SelfCallBarrier.Node node = selfCallBarrier.enter(thread);
        try {
            final SpyHandler spyHandler = namespaceSpyHandlerMap.get(namespace);
            if (null == spyHandler) {
                return Ret.RET_NONE;
            }
            return spyHandler.handleOnReturn(listenerId, object);
        } catch (Throwable cause) {
            handleException(cause);
            return Ret.RET_NONE;
        } finally {
            selfCallBarrier.exit(thread, node);
        }
    }

    public static Ret spyMethodOnThrows(final Throwable throwable,
                                        final String namespace,
                                        final int listenerId) throws Throwable {
        final Thread thread = Thread.currentThread();
        if (selfCallBarrier.isEnter(thread)) {
            return Ret.RET_NONE;
        }
        final SelfCallBarrier.Node node = selfCallBarrier.enter(thread);
        try {
            final SpyHandler spyHandler = namespaceSpyHandlerMap.get(namespace);
            if (null == spyHandler) {
                return Ret.RET_NONE;
            }
            return spyHandler.handleOnThrows(listenerId, throwable);
        } catch (Throwable cause) {
            handleException(cause);
            return Ret.RET_NONE;
        } finally {
            selfCallBarrier.exit(thread, node);
        }
    }

    /**
     * 返回结果
     */
    public static class Ret {

        public static final int RET_STATE_NONE = 0;
        public static final int RET_STATE_RETURN = 1;
        public static final int RET_STATE_THROWS = 2;
        private static final Ret RET_NONE = new Ret(RET_STATE_NONE, null);
        /**
         * 返回状态(0:NONE;1:RETURN;2:THROWS)
         */
        public final int state;
        /**
         * 应答对象
         */
        public final Object respond;

        /**
         * 构造返回结果
         *
         * @param state   返回状态
         * @param respond 应答对象
         */
        private Ret(int state, Object respond) {
            this.state = state;
            this.respond = respond;
        }

        public static Ret newInstanceForNone() {
            return RET_NONE;
        }

        public static Ret newInstanceForReturn(Object object) {
            return new Ret(RET_STATE_RETURN, object);
        }

        public static Ret newInstanceForThrows(Throwable throwable) {
            return new Ret(RET_STATE_THROWS, throwable);
        }

    }

    /**
     * 本地线程
     */
    public static class SelfCallBarrier {

        public static class Node {
            private final Thread thread;
            private final ReentrantLock lock;
            private Node pre;
            private Node next;

//            Node() {
//                this(null);
//            }

            Node(final Thread thread) {
                this(thread, null);
            }

            Node(final Thread thread, final ReentrantLock lock) {
                this.thread = thread;
                this.lock = lock;
            }

        }

        // 删除节点
        void delete(final Node node) {
            node.pre.next = node.next;
            if (null != node.next) {
                node.next.pre = node.pre;
            }
            // help gc
            node.pre = (node.next = null);
        }

        // 插入节点
        void insert(final Node top, final Node node) {
            if (null != top.next) {
                top.next.pre = node;
            }
            node.next = top.next;
            node.pre = top;
            top.next = node;
        }

        static final int THREAD_LOCAL_ARRAY_LENGTH = 512;

        final Node[] nodeArray = new Node[THREAD_LOCAL_ARRAY_LENGTH];

        SelfCallBarrier() {
            cleanAndInit();
        }

        Node createTopNode() {
            return new Node(null, new ReentrantLock());
        }

        void cleanAndInit() {
            for (int i = 0; i < THREAD_LOCAL_ARRAY_LENGTH; i++) {
                nodeArray[i] = createTopNode();
            }
        }

        int abs(int val) {
            return val < 0
                    ? val * -1
                    : val;
        }

        boolean isEnter(Thread thread) {
            final Node top = nodeArray[abs(thread.hashCode()) % THREAD_LOCAL_ARRAY_LENGTH];
            Node node = top;
            try {
                // spin for lock
                while (!top.lock.tryLock()) ;
                while (null != node.next) {
                    node = node.next;
                    if (thread == node.thread) {
                        return true;
                    }
                }
                return false;
            } finally {
                top.lock.unlock();
            }
        }

        Node enter(Thread thread) {
            final Node top = nodeArray[abs(thread.hashCode()) % THREAD_LOCAL_ARRAY_LENGTH];
            final Node node = new Node(thread);
            try {
                while (!top.lock.tryLock()) ;
                insert(top, node);
            } finally {
                top.lock.unlock();
            }
            return node;
        }

        void exit(Thread thread, Node node) {
            final Node top = nodeArray[abs(thread.hashCode()) % THREAD_LOCAL_ARRAY_LENGTH];
            try {
                while (!top.lock.tryLock()) ;
                delete(node);
            } finally {
                top.lock.unlock();
            }
        }

    }

}
