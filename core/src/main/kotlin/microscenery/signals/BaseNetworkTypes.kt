package microscenery.signals

import org.joml.Vector2i
import org.joml.Vector3f
import org.joml.Vector3i
import org.withXR.network.v3.Vector2Int
import org.withXR.network.v3.Vector3Float
import org.withXR.network.v3.Vector3Int

fun Vector2i.toProto() = Vector2Int.newBuilder().setX(this.x).setY(this.y).build()
fun Vector2Int.toPoko() = Vector2i(this.x, this.y)

fun Vector3i.toProto() = Vector3Int.newBuilder().setX(this.x).setY(this.y).setZ(this.z).build()
fun Vector3Int.toPoko() = Vector3i(this.x, this.y, this.z)

fun Vector3f.toProto() = Vector3Float.newBuilder().setX(this.x).setY(this.y).setZ(this.z).build()
fun Vector3Float.toPoko() = Vector3f(this.x, this.y, this.z)
