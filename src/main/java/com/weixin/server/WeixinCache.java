package com.weixin.server;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class WeixinCache {

	private static final ConcurrentHashMap<String, Object> cacheMap = new ConcurrentHashMap<>();

	public static void put(String key, Object value) {
		cacheMap.put(key, value);
	}

	public static Object get(String key) {
		return cacheMap.get(key);
	}

	public static void remove(String key) {
		cacheMap.remove(key);
	}

	public static void put(String key, Object value, Integer expireTime) {
		put(key, value);
		if (expireTime > 0) {
			ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
			executor.scheduleAtFixedRate(() -> {
				remove(key);
			}, 0, expireTime, TimeUnit.SECONDS);
		}
	}
}
