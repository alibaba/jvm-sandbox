package com.alibaba.jvm.sandbox.module.debug;

import com.alibaba.jvm.sandbox.api.Information;
import com.alibaba.jvm.sandbox.api.LoadCompleted;
import com.alibaba.jvm.sandbox.api.Module;
import com.alibaba.jvm.sandbox.api.listener.Sentry;
import com.alibaba.jvm.sandbox.api.listener.ext.Advice;
import com.alibaba.jvm.sandbox.api.listener.ext.AdviceListener;
import com.alibaba.jvm.sandbox.api.listener.ext.EventWatchBuilder;
import com.alibaba.jvm.sandbox.api.resource.ModuleEventWatcher;
import org.kohsuke.MetaInfServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;

import static com.alibaba.jvm.sandbox.module.debug.JdbcLoggerModule.MonitorJavaSqlPreparedStatementStep.*;

/**
 * 基于JDBC的SQL日志
 *
 * @author luanjia@taobao.com
 */
@MetaInfServices(Module.class)
@Information(id = "debug-jdbc-logger", version = "0.0.1", author = "luanjia@taobao.com")
public class JdbcLoggerModule implements Module, LoadCompleted {

    private final Logger smLogger = LoggerFactory.getLogger("DEBUG-JDBC-LOGGER");

    @Resource
    private ModuleEventWatcher moduleEventWatcher;

    @Override
    public void loadCompleted() {
        monitorJavaSqlStatement();
        monitorJavaSqlPreparedStatement();
    }

    // 监控java.sql.Statement的所有实现类
    private void monitorJavaSqlStatement() {
        new EventWatchBuilder(moduleEventWatcher)
                .onClass(Statement.class).includeSubClasses()
                .onBehavior("execute*")
                /**/.withParameterTypes(String.class)
                /**/.withParameterTypes(String.class, int.class)
                /**/.withParameterTypes(String.class, int[].class)
                /**/.withParameterTypes(String.class, String[].class)
                .onWatch(new AdviceListener() {

                    private final String MARK_STATEMENT_EXECUTE = "MARK_STATEMENT_EXECUTE";
                    private final String PREFIX = "STMT";

                    @Override
                    public void before(Advice advice) {
                        advice.attach(System.currentTimeMillis(), MARK_STATEMENT_EXECUTE);
                    }

                    @Override
                    public void afterReturning(Advice advice) {
                        if (advice.hasMark(MARK_STATEMENT_EXECUTE)) {
                            final long costMs = System.currentTimeMillis() - (Long) advice.attachment();
                            final String sql = advice.getParameterArray()[0].toString();
                            logSql(PREFIX, sql, costMs, true, null);
                        }
                    }

                    @Override
                    public void afterThrowing(Advice advice) {
                        if (advice.hasMark(MARK_STATEMENT_EXECUTE)) {
                            final long costMs = System.currentTimeMillis() - (Long) advice.attachment();
                            final String sql = advice.getParameterArray()[0].toString();
                            logSql(PREFIX, sql, costMs, false, advice.getThrowable());
                        }
                    }

                });
    }

    enum MonitorJavaSqlPreparedStatementStep {
        waiting_Connection_prepareStatement,
        waiting_PreparedStatement_execute,
        waiting_PreparedStatement_execute_finish,
    }

    private void monitorJavaSqlPreparedStatement() {

        new EventWatchBuilder(moduleEventWatcher)
                .onClass(Connection.class)
                .includeSubClasses()
                .onBehavior("prepareStatement")
                .onClass(PreparedStatement.class)
                .includeSubClasses()
                .onBehavior("execute*")
                .onWatch(new AdviceListener() {

                    private final String MARK_PREPARED_STATEMENT_EXECUTE = "MARK_PREPARED_STATEMENT_EXECUTE";
                    private final String PREFIX = "PSTMT";


                    private final Sentry<MonitorJavaSqlPreparedStatementStep> sentry
                            = new Sentry<MonitorJavaSqlPreparedStatementStep>(waiting_Connection_prepareStatement);

                    @Override
                    public void before(Advice advice) {

                        // Connection.prepareStatement()
                        if (advice.getTarget() instanceof Connection
                                && sentry.next(waiting_Connection_prepareStatement, waiting_PreparedStatement_execute)) {
                            sentry.attach(advice.getParameterArray()[0].toString());
                        }

                        // PreparedStatement.execute*()
                        if (advice.getTarget() instanceof PreparedStatement
                                && sentry.next(waiting_PreparedStatement_execute, waiting_PreparedStatement_execute_finish)) {
                            advice.attach(System.currentTimeMillis(), MARK_PREPARED_STATEMENT_EXECUTE);
                        }

                    }

                    @Override
                    public void afterReturning(Advice advice) {
                        if (finishing(sentry, advice)) {
                            final long costMs = System.currentTimeMillis() - (Long) advice.attachment();
                            final String sql = sentry.attachment();
                            logSql(PREFIX, sql, costMs, true, null);
                        }
                    }

                    @Override
                    public void afterThrowing(Advice advice) {
                        if (finishing(sentry, advice)) {
                            final long costMs = System.currentTimeMillis() - (Long) advice.attachment();
                            final String sql = sentry.attachment();
                            logSql(PREFIX, sql, costMs, false, advice.getThrowable());
                        }
                    }

                    private boolean finishing(final Sentry<MonitorJavaSqlPreparedStatementStep> sentry,
                                              final Advice advice) {
                        return advice.hasMark(MARK_PREPARED_STATEMENT_EXECUTE)
                                && sentry.next(waiting_PreparedStatement_execute_finish, waiting_Connection_prepareStatement);
                    }

                });
    }

    // SQL日志输出
    private void logSql(final String prefix,
                        final String sql,
                        final long costMs,
                        final boolean isSuccess,
                        final Throwable cause) {
        smLogger.info("{};cost:{}ms;{};sql:{}",
                prefix,
                costMs,
                isSuccess
                        ? "success"
                        : "failed",
                sql,
                cause
        );
    }

}
