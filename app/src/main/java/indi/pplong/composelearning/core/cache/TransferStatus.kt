package indi.pplong.composelearning.core.cache

/**
 * Description:
 * @author PPLong
 * @date 10/24/24 5:25 PM
 */
sealed class TransferStatus {
    data object Initial : TransferStatus()
    data class Transferring(val value: Float, val speed: Long) : TransferStatus()
    data object Failed : TransferStatus()
    data object Successful : TransferStatus()
    data object Loading : TransferStatus()
    data class Paused(val size: Long) : TransferStatus()
}