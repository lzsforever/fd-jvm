package cn.wangoon.fd.jvm.agent;


import cn.wangoon.fd.jvm.agent.collector.JVMCollect;
import cn.wangoon.fd.jvm.core.AgentArgs;
import lombok.extern.slf4j.Slf4j;

import java.lang.instrument.Instrumentation;


/**
 * <B>主类名称：</B>JvmAgent<BR>
 * <B>概要说明：</B>The JvmAgent-APM Agent premain Entry.<BR>
 *
 * @author dolan
 * @since 2024年3月18日 下午2:22:29
 */
@Slf4j
public class JvmAgent {

    /**
     * Main entrance. Use byte-buddy transform to enhance all classes, which define in plugins.
     * -javaagent:/path/to/agent.jar=agentArgs
     * -javaagent:/path/to/agent.jar=k1=v1,k2=v2...
     * 等号之后都是参数,也就是入参agentArgs
     * -javaagent参数必须在-jar之前
     */
    public static void premain(String agentArgs, Instrumentation instrumentation) {
        log.info("Java jvm Agent start premain args: " + agentArgs);
        AgentArgs.parseArgs(agentArgs);
        MetricSchedule.getInstance().scheduleAtFixedRate(1, 10, JVMCollect.class);
    }


}
