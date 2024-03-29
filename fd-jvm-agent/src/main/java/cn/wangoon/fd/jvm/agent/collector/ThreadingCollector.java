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

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <B>主类名称：</B>ThreadingCollector<BR>
 * <B>概要说明：</B>通过线程的MXBean获取线程信息，主要获取两个核心指标：当前jvm中存活的线程数量、当前jvm 的历史存活线程数量的峰值<BR>
 * 
 * @author Scott.Bai
 * @since 2022年4月27日 下午1:52:45
 */
@Slf4j
public class ThreadingCollector extends JVMCollect {

	private final ThreadMXBean threadMXBean;

	public ThreadingCollector() {
		this.threadMXBean = ManagementFactory.getThreadMXBean();
	}

	@Override
	public void collect() {
		ThreadInfo[] threads = threadMXBean.getThreadInfo(threadMXBean.getAllThreadIds());
		
		int threadCount = 0, peakThreadCount = 0, threadDeadLock = 0,
				threadBlocked = 0,  threadFresh = 0, threadRunnable = 0,
				threadTerminated = 0, threadTimeWaiting = 0, threadWaiting = 0;
		
		threadCount = threadMXBean.getThreadCount();
		peakThreadCount = threadMXBean.getPeakThreadCount();
		
		long[] deadThread = threadMXBean.findDeadlockedThreads();
		threadDeadLock = deadThread == null ? 0 : deadThread.length;
		
		
		for (ThreadInfo info : threads) {
			if (info == null) {
				continue;
			}
			switch (info.getThreadState()) {
				case BLOCKED:
					threadBlocked++;
					break;
				case NEW:
					threadFresh++;
					break;
				case RUNNABLE:
					threadRunnable++;
					break;
				case TERMINATED:
					threadTerminated++;
					break;
				case TIMED_WAITING:
					threadTimeWaiting++;
					break;
				case WAITING:
					threadWaiting++;
					break;
				default:
					log.warn(" Thread State [{}] doesn't exist.", info.getThreadState());
			}
		}
		long currentTime = System.currentTimeMillis();

		Map<String, String> tags = createTags();

		List<Metric> metrics = Arrays.asList(
				Metric.create("thread_count", threadCount, currentTime, tags, Config.Rolling.METRICS_JVM_TOPIC),
				Metric.create("peak_thread_count", peakThreadCount, currentTime, tags, Config.Rolling.METRICS_JVM_TOPIC),
				Metric.create("thread_dead_lock", threadDeadLock, currentTime, tags, Config.Rolling.METRICS_JVM_TOPIC),
				Metric.create("thread_blocked", threadBlocked, currentTime, tags, Config.Rolling.METRICS_JVM_TOPIC),
				Metric.create("thread_fresh", threadFresh, currentTime, tags, Config.Rolling.METRICS_JVM_TOPIC),
				Metric.create("thread_runnable", threadRunnable, currentTime, tags, Config.Rolling.METRICS_JVM_TOPIC),
				Metric.create("thread_terminated", threadTerminated, currentTime, tags, Config.Rolling.METRICS_JVM_TOPIC),
				Metric.create("thread_time_waiting", threadTimeWaiting, currentTime, tags, Config.Rolling.METRICS_JVM_TOPIC),
				Metric.create("thread_waiting", threadWaiting, currentTime, tags, Config.Rolling.METRICS_JVM_TOPIC)
		);
		KafkaClientSender kafkaClientSender = KafkaClientSender.getInstance();
		kafkaClientSender.sendBatch(metrics);
	}

	@Data
	@NoArgsConstructor
	@AllArgsConstructor
	public class ThreadingMetric {

		// 	线程总数
		private int thread_count;

		// 	线程个数峰值
		private int peak_thread_count;
		
		// 	死锁线程
		private int thread_dead_lock;

		// 	阻塞线程
		private int thread_blocked;

		// 	新创建的线程
		private int thread_fresh;

		// 	可运行状态的线程
		private int thread_runnable;

		// 	已终结的线程
		private int thread_terminated;

		// 	限时等待的线程
		private int thread_time_waiting;

		// 	非限时等待的线程
		private int thread_waiting;

	}

}
