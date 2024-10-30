package indi.pplong.composelearning.core.base.state

/**
 * Description:
 * @author PPLong
 * @date 9/30/24 12:10 PM
 */
enum class LoadingState {
    LOADING,
    FAIL,
    SUCCESS
}

enum class RequestingState {
    REQUEST,
    DONE
}

enum class EditState {
    INITIAL,
    EDITING,
    SAVING,
    SUCCESS,
    FAIL
}

enum class ConfigureState {
    CONNECTING,
    CONFIGURING
}