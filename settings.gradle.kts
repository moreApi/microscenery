rootProject.name = "microscenery"

include("core")
include("frontend")

val withZenSysConCon: String? by extra
if (withZenSysConCon?.toBoolean() == true) {
    include("zenSysConCon")
}
