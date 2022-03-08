package microscenery.network

import java.nio.ByteBuffer

data class VolumeFragment(val id: UInt, val data: ByteBuffer){
    companion object{
        fun fromBuffer(id: UInt, data: ByteBuffer, size: Int): VolumeFragment {
            val buffer = data.duplicate()
            buffer.mark()
            buffer.limit(buffer.position()+size)
            return VolumeFragment(id,buffer)
        }
    }
}