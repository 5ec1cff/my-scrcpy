package five.ec1cff.scrcpy

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class ScrcpyOption(
    val sendFrameMeta: Boolean,
    val bitRate: Int,
    val maxFps: Int,
    val codecOptions: Map<String, String>?,
    val encoderName: String
    ): Parcelable
