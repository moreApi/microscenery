rootProject.name = "anon"

include("core")
include("frontend")

val withSysCon: String? by extra
if (withSysCon?.toBoolean() == true) {
    include("zenSysConCon")
}
