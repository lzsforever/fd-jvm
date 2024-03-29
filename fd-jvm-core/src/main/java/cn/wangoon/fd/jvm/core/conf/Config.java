package cn.wangoon.fd.jvm.core.conf;


import shade.cn.wangoon.fd.common.util.NetUtil;
import shade.com.alibaba.fastjson.JSON;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <B>主类名称：</B>Config<BR>
 * <B>概要说明：</B>This is the core config in Tracking<BR>
 *
 * @author Scott.Bai
 * @since 2022年6月8日 下午4:04:07
 */
public class Config {

    public static class Agent {

        /**
         * ApplicationName must set in agent start.
         * <p>
         * Go to the Actual app service Launcher the applicationName;
         * <p>
         * However When If IsUsedUnSafeNamed = true, It Must be a component set up in advance
         * { @link Context.UnSafeNamed#SettingGlobalName }
         * Otherwise used Tracking Config, 'agent.config' with Config.Agent.APPLICATION_NAME.
         * <p>
         * Most of the time, the Tracing logger appended after the 'fd-log-core' component is loaded;
         * If only if the GettingApplicationName is faild, please submit the Bug contact me;
         * <p>
         * Fixed By Scott.Bai
         */
        public static String APPLICATION_NAME = "";

        /**
         * InstanceName, The identifier of the instance; Usually it is the IP and started port Number.
         */
        public static String INSTANCE_NAME = NetUtil.getLocalIp();

        /**
         * Namespace isolates headers in cross process propagation. The HEADER name will be `HeaderName:Namespace`.
         */
        public static String NAMESPACE = "";

        /**
         * Ip:
         */
        public static String IP = NetUtil.getLocalIp();

        /**
         * HostName
         */
        public static String HOSTNAME = NetUtil.getLocalHostName();

        /**
         * ENV default offline, online
         */
        public static String ENV = "offline";

        /**
         * Initialized JSON
         */
        public static String INITIALIZE_JSON_ASMSerializerFactory = JSON.toJSONString(new Object());

        /**
         * If the operation name of the first span is included in this set, this segment should be ignored. Multiple values should be separated by `,`.
         */
        public static String IGNORE_SUFFIX = ".jpg,.jpeg,.js,.css,.png,.bmp,.gif,.ico,.mp3,.mp4,.html,.svg";

        /**
         * The max number of SegmentRef in a single span to keep memory cost estimatable.
         */
        public static int TRACE_SEGMENT_REF_LIMIT_PER_SPAN = 500;

        /**
         * The max number of spans in a single segment. Through this config item, keep your application memory cost estimated.
         */
        public static int SPAN_LIMIT_PER_SEGMENT = 300;

        /**
         * Config bytebuddy open debugging
         */
        public static boolean IS_OPEN_DEBUGGING_CLASS = false;

        /**
         * If true, Tracking agent will cache all instrumented classes to memory or disk files (decided by class cache
         * mode), allow other javaagent to enhance those classes that enhanced by agent.
         */
        public static boolean IS_CACHE_CLASS = false;

        /**
         * The instrumented classes cache mode: MEMORY or FILE MEMORY: cache class bytes to memory, if instrumented
         * classes is too many or too large, it may take up more memory FILE: cache class bytes in `/class-cache`
         * folder, automatically clean up cached class files when the application exits
         */

        /**
         * How depth the agent goes, when log cause exceptions.
         */
        public static int CAUSE_EXCEPTION_DEPTH = 5;

        /**
         * Limit the length of the operationName to prevent the overlength issue in the storage.
         */
        public static int OPERATION_NAME_THRESHOLD = 150;

        /**
         * 	Sampler strategy default Random
         */

        /**
         * Sampler rate between 0.0 and 1.0 (100%)
         */
        public volatile static double SAMPLER_RATE = 1.0;

        /**
         * Limit the length of the operationName to prevent the overlength issue in the storage.
         */
        public static Map<String, String> INSTANCE_PROPERTIES = new HashMap<>();

    }

    public static class Plugin {

        /**
         * Exclude activated plugins
         */
        public static String EXCLUDE_PLUGINS = "";

        /**
         * Mount the folders of the plugins. The folder path is relative to agent.jar.
         */
        public static List<String> MOUNT = Arrays.asList("plugins", "activations");
    }

    public static class Logging {

        /**
         * Log file name.
         */
        public static String FILE_NAME = "tracking.log";

        /**
         * Log files directory. Default is blank string, means, use "{AgentJarDir}/logs  " to output logs.
         */
        public static String DIR = "logs";

        /**
         * The max size of log file. If the size is bigger than this, archive the current file, and write into a new file.
         */
        public static int MAX_FILE_SIZE = 500 * 1024 * 1024;


        /**
         * The log patten. Default is "%level %timestamp %thread %class : %msg %throwable".
         */
        public static String PATTERN = "%level %timestamp %thread %class : %msg %throwable";

        /**
         * The log console print
         */
        public static boolean IS_OPEN_CONSOLE_PRINT = false;
    }


    public static class Rolling {

        /**
         * Kafka Address, Setting this That means enables RollingNumber
         */
        public static String KAFKA_ADDRESS = "kafka_address";

        /**
         * The max size of RollingNumber
         */
        public static int ROLLINGNUMBER_MAX_SIZE = 1024;

        /**
         * default topic name: metriclog-counter
         */
        public static String COUNTER_TOPIC = "metriclog-counter";
        public static String METRICS_JVM_TOPIC = "metrics_jvm_test";


    }

}
