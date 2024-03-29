package cn.wangoon.fd.jvm.agent;


import lombok.extern.slf4j.Slf4j;
import shade.cn.wangoon.fd.common.loader.ServiceLoader;
import shade.cn.wangoon.fd.common.tracking.MetricCollect;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * <B>主类名称：</B>MetricSchedule<BR>
 * <B>概要说明：</B>单线程通用指标扫描器实现类<BR>
 * @author Scott.Bai
 * @since 2022年4月27日 下午12:13:13
 */
@Slf4j
public class MetricSchedule {
	
    private static class SingletonHolder {
        private static final MetricSchedule INSTANCE = new MetricSchedule();
    }

    private MetricSchedule() {
    }

    public static MetricSchedule getInstance() {
        return SingletonHolder.INSTANCE;
    }

    private final ScheduledExecutorService SCHEDULED_EXECUTOR_SERVICE = Executors.newScheduledThreadPool(1);
    
    private final ConcurrentHashMap<Class<? extends MetricCollect>, ScheduleTask> taskMap = new ConcurrentHashMap<>();
    

    public void scheduleAtFixedRate(long initialDelay, long period, Class<? extends MetricCollect> clazz) {
    	SCHEDULED_EXECUTOR_SERVICE.scheduleAtFixedRate(new ScheduleTask(clazz), initialDelay, period, TimeUnit.SECONDS);
		log.info("MetricSchedule start, MetricCollect TypeClass: {}, initialDelay: {}, period: {}", clazz, initialDelay, period);
    }
    

    
    /**
     * <B>主类名称：</B>ScheduleTask<BR>
     * <B>概要说明：</B>执行任务实现类<BR>
     * @author Scott.Bai
     * @since 2022年4月27日 下午12:41:09
     */
    class ScheduleTask implements Runnable {
    	
    	private ServiceLoader<? extends MetricCollect> serviceLoader;
    	
    	private volatile boolean isRunning = false;
    	
    	public ScheduleTask(Class<? extends MetricCollect> clazz) {
			this.serviceLoader = ServiceLoader.load(clazz);
			this.isRunning = true;
			taskMap.put(clazz, this);
    	}
    	
		@Override
		public void run() {
			if(isRunning) {
				for(MetricCollect collector : serviceLoader) {
					collector.collect();
				}				
			}
		}
		
    }
    
}
