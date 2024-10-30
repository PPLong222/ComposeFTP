package indi.pplong.composelearning.core.load.model

import indi.pplong.composelearning.core.cache.TransferStatus
import indi.pplong.composelearning.core.file.model.TransferredFileItem

/**
 * Description:
 * @author PPLong
 * @date 10/25/24 8:35 PM
 */
data class TransferringFile(
    val transferredFileItem: TransferredFileItem = TransferredFileItem(),
    val transferStatus: TransferStatus = TransferStatus.Initial
)
