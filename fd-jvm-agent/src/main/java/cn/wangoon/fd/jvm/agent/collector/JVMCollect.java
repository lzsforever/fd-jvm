package cn.wangoon.fd.jvm.agent.collector;


import cn.wangoon.fd.jvm.core.AgentArgs;
import shade.cn.wangoon.fd.common.tracking.MetricCollect;
import shade.cn.wangoon.fd.common.tracking.TraceLogging;

import java.util.HashMap;
import java.util.Map;

/**
 * <B>主类名称：</B>JVMCollect<BR>
 * <B>概要说明：</B>JVM Collect SPI Extension<BR>
 * @author Scott.Bai
 * @since 2022年4月27日 下午1:41:00
 */
public abstract class JVMCollect implements MetricCollect {
    public  Map<String, String> createTags() {
        Map<String, String> tags = new HashMap<>();
        tags.put("type", "JVM");
        tags.put(TraceLogging.Keys.APPLICATIONNAME, AgentArgs.getValue(TraceLogging.Keys.APPLICATIONNAME));
        return tags;
    }
}
