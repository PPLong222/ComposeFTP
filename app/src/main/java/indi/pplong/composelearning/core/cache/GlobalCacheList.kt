package indi.pplong.composelearning.core.cache

import java.util.concurrent.ConcurrentHashMap

/**
 * Description:
 * @author PPLong
 * @date 11/7/24 3:51 PM
 */
object GlobalCacheList {
    val map = ConcurrentHashMap<String, String>()
}