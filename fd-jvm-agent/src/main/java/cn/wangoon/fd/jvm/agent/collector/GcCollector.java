package cn.wangoon.fd.jvm.agent.collector;

import cn.wangoon.fd.jvm.core.AgentArgs;
import cn.wangoon.fd.jvm.core.KafkaClientSender;
import cn.wangoon.fd.jvm.core.conf.Config;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import shade.cn.wangoon.fd.common.tracking.Metric;
import shade.cn.wangoon.fd.common.tracking.TraceLogging;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.util.*;


/**
 * <B>主类名称：</B>GcCollector<BR>
 * <B>概要说明：</B>通过GCMXBean获取gc 次数和 gc 时间的信息，并计算jvm 的吞吐量(throughput = (jvm-running-time - gc-time)/jvm-running-time )<BR>
 *
 * @author Scott.Bai
 * @since 2022年4月27日 上午9:47:45
 */
@Slf4j
public class GcCollector extends JVMCollect {
    public static final GcCollector INSTANCE = new GcCollector();

    private static final long START_TIME = ManagementFactory.getRuntimeMXBean().getStartTime();

    private static final String YOUNG_GEN_MARKER = "Y";
    private static final String OLD_GEN_MARKER = "O";

    private final List<GarbageCollectorMXBean> garbageCollectorMXBeanList;

    private final HashMap<String, Long> lastGcCount = new HashMap<>();

    private final HashMap<String, Long> lastGcTime = new HashMap<>();

    public GcCollector() {
        garbageCollectorMXBeanList = ManagementFactory.getGarbageCollectorMXBeans();
    }

    /**
     * <B>方法名称：</B>collectMetrics<BR>
     * <B>概要说明：</B>获取youngGC、oldGc的次数和时间, jvm的吞吐量<BR>
     *
     * @author Scott.Bai
     * @since 2022年4月27日 上午9:48:40
     */
    public void collect() {

        // 定义垃圾收集统计信息字段
        long youngGcCount = 0;
        long youngGcTime = 0;
        long oldGcCount = 0;
        long oldGcTime = 0;
        double gcThroughput;
        long totalGcTime = 0;
        for (GarbageCollectorMXBean gcBean : garbageCollectorMXBeanList) {

            String gcBeanName = gcBean.getName();

            //	gcTime、 gcNum 均为累计值，需要求增量
            long gcTime = gcBean.getCollectionTime();
            long gcCount = gcBean.getCollectionCount();
            log.debug("gc region: {}, gc-total-count: {}, gc-total-time: {}", gcBeanName, gcCount, gcTime);

            long curGcCount, curGcTime;
            totalGcTime += gcTime;

            GCType gc = GCType.getGcDetails(gcBeanName);

            if (lastGcCount.get(gc.getGcName()) == null) {
                //	第一次采集，更新 lastGcCount、lastGcTime
                lastGcCount.put(gc.getGcName(), gcCount);
                curGcCount = gcCount;
                lastGcTime.put(gc.getGcName(), gcTime);
                curGcTime = gcTime;
            } else {
                //	非第一次采集，需要计算当前时间区间的统计值，并更新 lastGcCount、lastGcTime
                curGcCount = gcCount - lastGcCount.get(gc.getGcName());
                lastGcCount.put(gc.getGcName(), gcCount);

                curGcTime = gcTime - lastGcTime.get(gc.getGcName());
                lastGcTime.put(gc.getGcName(), gcTime);
            }

            log.debug("gc region: {}, current-gc-count: {}, current-time: {}", gcBeanName, curGcCount, curGcTime);

            switch (gc.getGcGenMarker()) {
                case YOUNG_GEN_MARKER:
                    youngGcCount = curGcCount;
                    youngGcTime = curGcTime;

                    break;
                case OLD_GEN_MARKER:
                    oldGcCount = curGcCount;
                    oldGcTime = curGcTime;
                    break;
                default:
            }

        }

        // 	计算 jvm 的吞吐量，避免对 gc 时间的统计，吞吐量 < 98，需要考虑优化程序
        long runningTime = System.currentTimeMillis() - START_TIME;
        gcThroughput = (runningTime - totalGcTime) * 1.0d / runningTime * 100;

        long currentTime = System.currentTimeMillis();
        Map<String, String> tags = createTags();

        List<Metric> metrics = Arrays.asList(
                Metric.create("young_gc_count", youngGcCount, currentTime, tags, Config.Rolling.METRICS_JVM_TOPIC),
                Metric.create("young_gc_time", youngGcTime, currentTime, tags, Config.Rolling.METRICS_JVM_TOPIC),
                Metric.create("old_gc_count", oldGcCount, currentTime, tags, Config.Rolling.METRICS_JVM_TOPIC),
                Metric.create("old_gc_time", oldGcTime, currentTime, tags, Config.Rolling.METRICS_JVM_TOPIC),
                Metric.create("throughput", gcThroughput, currentTime, tags, Config.Rolling.METRICS_JVM_TOPIC)
        );
        KafkaClientSender kafkaClientSender = KafkaClientSender.getInstance();
        kafkaClientSender.sendBatch(metrics);
    }

    /**
     * <B>主类名称：</B>GCType<BR>
     * <B>概要说明：</B>枚举垃圾收集器类型，每种垃圾收集器拥有不同的youngGcCount name和oldGcCount name<BR>
     *
     * @author Scott.Bai
     * @since 2022年4月27日 上午9:44:27
     */
    @Slf4j
    private enum GCType {

        /**
         * Young GC
         */

        // -XX:+UseSerialGC
        SERIAL_YOUNG_GC("Copy", YOUNG_GEN_MARKER),

        // -XX:+UseParNewGC
        PARNEW_YOUNG_GC("ParNew", YOUNG_GEN_MARKER),

        // -XX:+UseParallelGC
        PARALLEL_YOUNG_GC("PS Scavenge", YOUNG_GEN_MARKER),

        // -XX:+UseG1GC
        G1_YOUNG_GC("G1 Young Generation", YOUNG_GEN_MARKER),


        /**
         * Old GC
         */

        // -XX:+UseSerialGC
        SERIAL_OLD_GC("MarkSweepCompact", OLD_GEN_MARKER),

        // -XX:+UseParallelGC
        PARALLEL_OLD_GC("PS MarkSweep", OLD_GEN_MARKER),

        // -XX:+UseConcMarkSweepGC
        CMS_OLD_GC("ConcurrentMarkSweep", OLD_GEN_MARKER),

        // -XX:+UseG1GC
        G1_OLD_GC("G1 Old Generation", OLD_GEN_MARKER),

        UNKNOWN_GC("UnknownGc", OLD_GEN_MARKER);

        private final String gcName;

        //属于区域标记
        private final String gcGenMarker;

        /**
         * <B>构造方法</B>GCType<BR>
         *
         * @param gcName      GC指标名称
         * @param gcGenMarker GC类型：Y/O   Y:年轻代，O:老年代
         */
        GCType(String gcName, String gcGenMarker) {
            this.gcName = gcName;
            this.gcGenMarker = gcGenMarker;
        }

        public static GCType getGcDetails(String gcName) {
            for (GCType ce : GCType.values()) {
                if (ce.getGcName().equals(gcName)) {
                    return ce;
                }
            }
            log.warn("GCType doesn't contain the gc name [{}].", gcName);
            return UNKNOWN_GC;
        }

        public String getGcName() {
            return gcName;
        }

        public String getGcGenMarker() {
            return gcGenMarker;
        }

    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public class GCMetric {

        //	年轻代收集的次数
        private long young_gc_count;

        //	年轻代收集的时间
        private long young_gc_time;

        //	老年代收集的次数
        private long old_gc_count;

        //	老年代收集的时间
        private long old_gc_time;

        //	吞吐量
        private double throughput;

    }

}
