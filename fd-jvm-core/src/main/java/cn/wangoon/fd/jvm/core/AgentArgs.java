package cn.wangoon.fd.jvm.core;

import shade.cn.wangoon.fd.common.tracking.TraceLogging;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @version 1.0.0
 * @author: dolan
 * @title
 * @description: args处理类
 * @create: 2024/03/27 17:47
 **/
public class AgentArgs {
    public static Map<String, List<String>> jvmArgsMap = new HashMap<>();


    /**
     * 解析args参数
     *
     * @param args agent参数
     */

    public static void parseArgs(String args) {
        // 如果参数为null，则返回
        if (args == null) {
            return;
        }

        // 去除参数两端的空格
        args = args.trim();
        // 如果去除空格后参数为空，则返回
        if (args.isEmpty()) {
            return;
        }

        // 遍历参数字符串中的每个参数键值对
        for (String argPair : args.split(",")) {
            // 将参数键值对按等号分割为键和值
            String[] strs = argPair.split("=");
            // 如果分割后的数组长度不为2，说明参数格式不正确，抛出异常
            if (strs.length != 2) {
                throw new IllegalArgumentException("Arguments for the agent should be like: key1=value1,key2=value2");
            }

            // 获取参数键，并去除两端空格
            String key = strs[0].trim();
            // 如果参数键为空，则抛出异常
            if (key.isEmpty()) {
                throw new IllegalArgumentException("Argument key should not be empty");
            }

            // 获取参数值，并去除两端空格
            List<String> list = jvmArgsMap.computeIfAbsent(key, k -> new ArrayList<>());
            // 将参数值添加到对应的键的列表中
            list.add(strs[1].trim());
        }

    }

    /**
     * 获取指定value str
     *
     * @param key
     * @return
     */
    public static String getValue(String key) {
        List<String> list = jvmArgsMap.get(key);
        if (list == null || list.size() <= 0) {
            return "";
        }
        return String.join(",", list);
    }
}
