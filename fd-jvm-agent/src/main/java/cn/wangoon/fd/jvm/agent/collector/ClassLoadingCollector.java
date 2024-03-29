package cn.wangoon.fd.jvm.agent.collector;

import cn.wangoon.fd.jvm.core.AgentArgs;
import cn.wangoon.fd.jvm.core.KafkaClientSender;
import cn.wangoon.fd.jvm.core.conf.Config;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import shade.cn.wangoon.fd.common.tracking.Metric;
import shade.cn.wangoon.fd.common.tracking.TraceLogging;

import java.lang.management.ClassLoadingMXBean;
import java.lang.management.ManagementFactory;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <B>主类名称：</B>ClassLoadingCollector<BR>
 * <B>概要说明：</B>获取装载类的MXBean, 通过MXBean采集类信息<BR>
 *
 * @author Scott.Bai
 * @since 2022年4月27日 下午12:16:10
 */
public class ClassLoadingCollector extends JVMCollect {

    public static final ClassLoadingCollector INSTANCE = new ClassLoadingCollector();

    private final ClassLoadingMXBean classLoadingMXBean;

    public ClassLoadingCollector() {
        this.classLoadingMXBean = ManagementFactory.getClassLoadingMXBean();
    }

    /**
     * <B>方法名称：</B>collect<BR>
     * <B>概要说明：</B>指标收集<BR>
     *
     * @author Scott.Bai
     * @since 2022年4月27日 下午12:30:33
     */
    public void collect() {
        long currentTime = System.currentTimeMillis();
        Map<String, String> tags = createTags();

        List<Metric> metrics = Arrays.asList(
                Metric.create("loaded_class_count", classLoadingMXBean.getLoadedClassCount(), currentTime, tags, Config.Rolling.METRICS_JVM_TOPIC),
                Metric.create("total_loaded_class_count", classLoadingMXBean.getTotalLoadedClassCount(), currentTime, tags, Config.Rolling.METRICS_JVM_TOPIC),
                Metric.create("unloaded_class_count", classLoadingMXBean.getUnloadedClassCount(), currentTime, tags, Config.Rolling.METRICS_JVM_TOPIC)

        );
        KafkaClientSender kafkaClientSender = KafkaClientSender.getInstance();
        kafkaClientSender.sendBatch(metrics);
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public class ClassLoadingMetric {

        //	获取 jvm 已加载的类的数量
        private int loaded_class_count;

        //	获取 jvm 加载过的类的总量
        private long total_loaded_class_count;

        //	获取 jvm 已卸载的类的数量
        private long unloaded_class_count;

    }

}
