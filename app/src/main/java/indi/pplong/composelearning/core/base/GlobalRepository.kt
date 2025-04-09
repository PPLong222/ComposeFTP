package indi.pplong.composelearning.core.base

import indi.pplong.composelearning.ftp.FTPServerPool
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Description:
 * @author PPLong
 * @date 11/3/24 4:20 PM
 */
@Singleton
class GlobalRepository @Inject constructor(
    val pool: FTPServerPool
) {
}