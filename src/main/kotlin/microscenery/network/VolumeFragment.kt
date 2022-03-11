package microscenery.network

import java.nio.ByteBuffer

data class VolumeFragment(val id: Int, val data: ByteBuffer){
    companion object{
        fun fromBuffer(id: Int, data: ByteBuffer, size: Int): VolumeFragment {
            val buffer = data.duplicate()
            buffer.mark()
            buffer.limit(buffer.position()+size)
            return VolumeFragment(id,buffer)
        }
    }
}