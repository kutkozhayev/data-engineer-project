name := "TwitterDataAnalysis"

version := "0.1"

scalaVersion := "2.11.12"

libraryDependencies += "org.apache.spark" %% "spark-sql" % "2.4.4"

libraryDependencies += "org.apache.spark" %% "spark-sql-kafka-0-10" % "2.4.4"

libraryDependencies += "org.apache.spark" %% "spark-streaming" % "2.4.4"

libraryDependencies += "org.elasticsearch" % "elasticsearch-hadoop" % "6.8.8"

libraryDependencies += "edu.stanford.nlp" % "stanford-corenlp" % "3.5.1"

libraryDependencies += "edu.stanford.nlp" % "stanford-corenlp" % "3.5.1" classifier "models"