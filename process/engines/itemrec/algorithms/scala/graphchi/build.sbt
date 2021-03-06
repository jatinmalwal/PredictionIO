import xerial.sbt.Pack._

name := "predictionio-process-itemrec-algorithms-scala-graphchi"

libraryDependencies ++= Seq(
  "ch.qos.logback" % "logback-classic" % "1.1.1",
  "com.twitter" %% "scalding-args" % "0.8.11",
  "org.clapper" %% "grizzled-slf4j" % "1.0.1",
  "org.scalanlp" %% "breeze" % "0.6.1")

packSettings

packJarNameConvention := "full"

packExpandedClasspath := true

packGenerateWindowsBatFile := false

packMain := Map(
  "itemrec.graphchi.dataprep" -> "io.prediction.commons.graphchi.itemrec.GraphChiDataPreparator",
  "itemrec.graphchi.modelcon" -> "io.prediction.commons.graphchi.itemrec.GraphChiModelConstructor")

packJvmOpts := Map(
  "itemrec.graphchi.dataprep" -> Common.packCommonJvmOpts,
  "itemrec.graphchi.modelcon" -> Common.packCommonJvmOpts)
