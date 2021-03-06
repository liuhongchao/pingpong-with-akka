import com.typesafe.sbt.packager.docker
import com.typesafe.sbt.packager.docker._

dockerExposedPorts in Docker := Seq(9000, 2551, 2552, 3001)

dockerRepository := Some("com.example")

dockerBaseImage := "java"

dockerEntrypoint := Seq(
  "bin/akka-ping-pong"
)

enablePlugins(JavaAppPackaging)
