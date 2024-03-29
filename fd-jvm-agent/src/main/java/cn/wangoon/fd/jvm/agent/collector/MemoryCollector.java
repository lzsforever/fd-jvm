package cn.wangoon.fd.jvm.agent.collector;


import cn.wangoon.fd.jvm.core.AgentArgs;
import cn.wangoon.fd.jvm.core.KafkaClientSender;
import cn.wangoon.fd.jvm.core.conf.Config;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import shade.cn.wangoon.fd.common.tracking.Metric;
import shade.cn.wangoon.fd.common.tracking.TraceLogging;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryUsage;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <B>主类名称：</B>MemoryCollector<BR>
 * <B>概要说明：</B>通过线程的MXBean获取jvm内存信息，主要采集四个指标：堆的最大值和已用值、非堆的最大值和已用值<BR>
 * @author Scott.Bai
 * @since 2022年4月27日 下午2:56:55
 */
public class MemoryCollector extends JVMCollect {

    public static final MemoryCollector INSTANCE = new MemoryCollector();

    private final MemoryMXBean memoryMXBean;

    public MemoryCollector() {
        this.memoryMXBean = ManagementFactory.getMemoryMXBean();
    }
    
	@Override
	public void collect() {
		
        MemoryUsage heapMemoryUsage = memoryMXBean.getHeapMemoryUsage();
        MemoryUsage nonHeapMemoryUsage = memoryMXBean.getNonHeapMemoryUsage();
        
        long heapUsed = heapMemoryUsage.getUsed();
        long heapMax = heapMemoryUsage.getMax();
        double heapUsedRate = (heapUsed + 0.0) / heapMax * 100;
        long nonHeapUsed = nonHeapMemoryUsage.getUsed();
		long nonHeapMax = nonHeapMemoryUsage.getMax();
		
		long edenGenUsed = 0;
		long edenGenMax = 0;
		final MemoryPoolMXBean edenMemoryPool = getEdenSpacePool();
        if (edenMemoryPool != null) {
            final MemoryUsage usage = edenMemoryPool.getUsage();
            edenGenUsed = usage.getUsed();
            edenGenMax = usage.getMax();
        }
        
		long oldGenUsed = 0; 
		long oldGenMax = 0;
        final MemoryPoolMXBean oldGenMemoryPool = getOldGenMemoryPool();
        if (oldGenMemoryPool != null) {
            final MemoryUsage usage = oldGenMemoryPool.getUsage();
            oldGenUsed = usage.getUsed();
            oldGenMax = usage.getMax();
        }
		
		long survivorGenUsed = 0;
		long survivorGenMax = 0;
        final MemoryPoolMXBean survivorGenMemoryPool = getSurvivorSpaceMemoryPool();
        if (survivorGenMemoryPool != null) {
            final MemoryUsage usage = survivorGenMemoryPool.getUsage();
            survivorGenUsed = usage.getUsed();
            survivorGenMax = usage.getMax();
        }
        
		long permGenUsed = 0;
		long permGenMax = 0;
        final MemoryPoolMXBean permGenMemoryPool = getPermGenMemoryPool();
        if (permGenMemoryPool != null) {
            final MemoryUsage usage = permGenMemoryPool.getUsage();
            permGenUsed = usage.getUsed();
            permGenMax = usage.getMax();
        }

        long currentTime = System.currentTimeMillis();
        Map<String, String> tags = createTags();

        List<Metric> metrics = Arrays.asList(
                Metric.create("heap_used", heapUsed, currentTime, tags, Config.Rolling.METRICS_JVM_TOPIC),
                Metric.create("heap_max", heapMax, currentTime, tags, Config.Rolling.METRICS_JVM_TOPIC),
                Metric.create("heap_used_rate", heapUsedRate, currentTime, tags, Config.Rolling.METRICS_JVM_TOPIC),
                Metric.create("non_heap_used", nonHeapUsed, currentTime, tags, Config.Rolling.METRICS_JVM_TOPIC),
                Metric.create("non_heap_max", nonHeapMax, currentTime, tags, Config.Rolling.METRICS_JVM_TOPIC),
                Metric.create("eden_gen_used", edenGenUsed, currentTime, tags, Config.Rolling.METRICS_JVM_TOPIC),
                Metric.create("eden_gen_max", edenGenMax, currentTime, tags, Config.Rolling.METRICS_JVM_TOPIC),
                Metric.create("survivor_gen_used", survivorGenUsed, currentTime, tags, Config.Rolling.METRICS_JVM_TOPIC),
                Metric.create("survivor_gen_max", survivorGenMax, currentTime, tags, Config.Rolling.METRICS_JVM_TOPIC),
                Metric.create("old_gen_used", oldGenUsed, currentTime, tags, Config.Rolling.METRICS_JVM_TOPIC),
                Metric.create("old_gen_max", oldGenMax, currentTime, tags, Config.Rolling.METRICS_JVM_TOPIC),
                Metric.create("perm_gen_used", permGenUsed, currentTime, tags, Config.Rolling.METRICS_JVM_TOPIC),
                Metric.create("perm_gen_max", permGenMax, currentTime, tags, Config.Rolling.METRICS_JVM_TOPIC)
        );

        KafkaClientSender kafkaClientSender = KafkaClientSender.getInstance();
        kafkaClientSender.sendBatch(metrics);
	}
	
	/**
	 * <B>方法名称：</B>getSurvivorSpaceMemoryPool<BR>
	 * <B>概要说明：</B>幸存区<BR>
	 * @author Scott.Bai
	 * @since 2022年4月27日 下午3:09:54
	 * @return MemoryPoolMXBean
	 */
    private MemoryPoolMXBean getSurvivorSpaceMemoryPool() {
        for (final MemoryPoolMXBean memoryPool : ManagementFactory.getMemoryPoolMXBeans()) {
            if (memoryPool.getName().endsWith("Survivor Space") || memoryPool.getName().endsWith("G1 Survivor")) {
                return memoryPool;
            }
        }
        return null;
    }

    /**
     * <B>方法名称：</B>getEdenSpacePool<BR>
     * <B>概要说明：</B>新生代<BR>
     * @author Scott.Bai
     * @since 2022年4月27日 下午3:10:06
     * @return MemoryPoolMXBean
     */
    private MemoryPoolMXBean getEdenSpacePool() {
        for (final MemoryPoolMXBean memoryPool : ManagementFactory.getMemoryPoolMXBeans()) {
            if (memoryPool.getName().endsWith("Eden Space") || memoryPool.getName().endsWith("G1 Eden")) {
                return memoryPool;
            }
        }
        return null;
    }

    /**
     * <B>方法名称：</B>getOldGenMemoryPool<BR>
     * <B>概要说明：</B>老年代<BR>
     * @author Scott.Bai
     * @since 2022年4月27日 下午3:10:29
     * @return MemoryPoolMXBean
     */
    private MemoryPoolMXBean getOldGenMemoryPool() {
        for (final MemoryPoolMXBean memoryPool : ManagementFactory.getMemoryPoolMXBeans()) {
            if (memoryPool.getName().endsWith("Old Gen")) {
                return memoryPool;
            }
        }
        return null;
    }

    /**
     * <B>方法名称：</B>getPermGenMemoryPool<BR>
     * <B>概要说明：</B>永远代<BR>
     * @author Scott.Bai
     * @since 2022年4月27日 下午3:10:41
     * @return MemoryPoolMXBean
     */
    private MemoryPoolMXBean getPermGenMemoryPool() {
        for (final MemoryPoolMXBean memoryPool : ManagementFactory.getMemoryPoolMXBeans()) {
            if (memoryPool.getName().endsWith("Perm Gen")) {
                return memoryPool;
            }
        }
        return null;
    }
	
	@Data
	@NoArgsConstructor
	@AllArgsConstructor
	public class MemoryMetric {

		//	已使用的堆大小
		private long heap_used;

		//	堆的最大值
		private long heap_max;
		
		//	堆使用率
		private double heap_used_rate;

		//	非堆已使用值
		private long non_heap_used;

		//	非堆的最大值
		private long non_heap_max;

		//	eden区使用
		private long eden_gen_used;

		//	eden区最大使用
		private long eden_gen_max;

		//	幸存区使用
		private long survivor_gen_used;

		//	幸存区最大使用
		private long survivor_gen_max;
		
		//	老年代使用
		private long old_gen_used;
		
		//	老年代最大使用
		private long old_gen_max;
		
		//	永久区使用
		private long perm_gen_used;
		
		//	永久区最大使用
		private long perm_gen_max;

	}
	
}
