## ![BANNER](https://github.com/alibaba/jvm-sandbox/wiki/img/BANNER.png)
> JVM沙箱容器，一种JVM的非侵入式运行期AOP解决方案<br/>
> Real - time non-invasive AOP framework container based on JVM

##### 2014年[GREYS](https://github.com/oldmanpushcart/greys-anatomy)第一版正式发布，一路看着他从无到有，并不断优化强大，感慨羡慕之余，也在想GREYS是不是只能做问题定位。

##### 2015年开始根据GREYS的底层代码完成了人生的第一个字节码增强工具——动态日志。之后又萌生了将其拆解成*录制回放*、*故障模拟*等工具的想法。扪心自问，我是想以一人一个团队的力量建立大而全的工具平台，还是做一个底层中台，让每一位技术人员都可以在它的基础上快速的实现业务功能。我选择了后者。

## 相关文档

- [README（English）](#for-english)
- [WIKI](https://github.com/alibaba/jvm-sandbox/wiki/Home)
- [用户手册](https://github.com/alibaba/jvm-sandbox/wiki/USER-GUIDE)
- [模块研发手册](https://github.com/alibaba/jvm-sandbox/wiki/MODULE-DEVELOPER-GUIDE)
- [模块研发例子](https://github.com/alibaba/jvm-sandbox/wiki/FOR-EXAMPLE)
- [发布日志](https://github.com/alibaba/jvm-sandbox/wiki/RELEASE-NOTES)


## 项目简介

### JVM-SANDBOX的核心功能是什么？

#### 实时无侵入AOP框架

在常见的AOP框架实现方案中，有静态编织和动态编织两种。

1. **静态编织**  
  静态编织发生在字节码生成时根据一定框架的规则提前将AOP字节码插入到目标类和方法中，实现AOP；

1. **动态编织**  
  动态编织则允许在JVM运行过程中完成指定方法的AOP字节码增强.常见的动态编织方案大多采用重命名原有方法，再新建一个同签名的方法来做代理的工作模式来完成AOP的功能(常见的实现方案如CgLib)，但这种方式存在一些应用边界：

   - **侵入性**  
     对被代理的目标类需要进行侵入式改造。比如：在Spring中必须是托管于Spring容器中的Bean

   - **固化性**  
     目标代理方法在启动之后即固化，无法重新对一个已有方法进行AOP增强

#### 热部署特性

还有一些实现AOP的方式是通过类似热部署的方式完成，但现有的热部署实现方案也存在一些应用边界：

1. 性能折损巨大
1. 对JVM存在侵入性
1. 必须启动时显式开启

基于此我通过JDK6所提供的Instrumentation-API实现了利用HotSwap技术在不重启JVM的情况下实现对任意方法的AOP增强。而且性能开销还在可以接受的范围之内。

#### 动态可插拔容器

为了实现沙箱模块的动态热插拔，容器客户端和沙箱动态可插拔容器采用HTTP协议进行通讯，底层用Jetty8作为HTTP服务器。

### JVM-SANDBOX能做什么？

在JVM沙箱（以下简称沙箱）的世界观中，任何一个Java方法的调用都可以分解为`BEFORE`、`RETURN`和`THROWS`三个环节，由此在三个环节上引申出对应环节的事件探测和流程控制机制。

```java
// BEFORE
try {

   /*
    * do something...
    */

    // RETURN
    return;

} catch (Throwable cause) {
    // THROWS
}
```

基于`BEFORE`、`RETURN`和`THROWS`三个环节事件，可以完成很多类AOP的操作。

1. 可以感知和改变方法调用的入参
2. 可以感知和改变方法调用返回值和抛出的异常
3. 可以改变方法执行的流程

    - 在方法体执行之前直接返回自定义结果对象，原有方法代码将不会被执行
    - 在方法体返回之前重新构造新的结果对象，甚至可以改变为抛出异常
    - 在方法体抛出异常之后重新抛出新的异常，甚至可以改变为正常返回

### JVM沙箱都有哪些可能的应用场景

- 线上故障定位
- 线上系统流控
- 线上故障模拟
- 方法请求录制和结果回放
- 动态日志打印
- 安全信息监测和脱敏

JVM沙箱还能帮助你做很多很多，取决于你的脑洞有多大了。

# FOR ENGLISH

# JVM-SANDBOX
> Real - time non-invasive AOP framework container based on JVM


## Foreword

### What is the core function of the JVM-SANDBOX？

#### Real-time non-invasive AOP framework
In the common AOP framework to achieve the program, there are two kinds of static weaving and dynamic weaving.

##### Static weaving

Static weaving occurs in the bytecode generation according to a certain framework of the rules in advance AOP byte code into the target class and method to achieve AOP.

##### Dynamic weaving

Dynamic weaving allows AOP bytecode enhancement of the specified method to be completed during the execution of the JVM.

Common dynamic weaving programs are mostly used to rename the original method, and then create a new signature method to do the agent's work mode to complete the AOP function (common implementation such as CgLib), but there are some application boundaries.

- Invasive

    The target class of the agent needs to be invaded.

- Curable

    The target agent method is solidified after startup and can not re-validate an existing method
    
##### Hot deployment
There are some ways to implement AOP is done through a similar hot deployment, but there are some application boundaries for existing hot deployment implementations:

- Performance damage huge
- There is an intrusion into the JVM
- Must be started when explicitly opened

Based on this I am through the JDK6 provided Instrumentation-API implementation of the use of HotSwap technology without restarting the JVM in the case of any method to achieve AOP enhancements. And performance overhead is still within acceptable limits


#### Provides a plug-and-play module management container
In order to realize the dynamic hot-swapping of the sandbox module, the container client and the sandbox dynamic pluggable container communicate with the HTTP protocol. The bottom layer uses Jetty8 as the HTTP server.

### What can the JVM-SANDBOX do？

In the JVM-SANDBOX (hereinafter referred to as the sandbox) world view, any one of the Java method calls can be broken down into `BEFORE`,` RETURN` and `THROWS` three links, which in three links on the corresponding link Event detection and process control mechanisms.

```java
// BEFORE-EVENT
try {

   /*
    * do something...
    */

    // RETURN-EVENT
    return;

} catch (Throwable cause) {
    // THROWS-EVENT
}
```

Based on the `BEFORE`,` RETURN` and `THROWS` three events, you can do a lot of AOP operation.

1. You can perceive and change the method call
2. You can sense and change the method call return value and throw the exception
3. You can change the flow of method execution

    - The custom result object is returned directly before the method body is executed, and the original method code will not be executed
    - Re-construct a new result object before the method body returns, and can even change to throw an exception
    - Throws a new exception after throwing an exception in the method body, and can even change to a normal return


### What are the possible scenarios for the JVM-SANDBOX?

- Online fault location
- Online system flow control
- Online fault simulation
- Method to request recording and result playback
- Dynamic log printing
- Safety information monitoring and desensitization

The JVM sandbox can also help you do a lot, depending on how big your brain is.
