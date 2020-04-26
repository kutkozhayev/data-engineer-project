import java.text.SimpleDateFormat
import java.util.{Calendar, Properties}

import edu.stanford.nlp.ling.CoreAnnotations
import edu.stanford.nlp.neural.rnn.RNNCoreAnnotations
import edu.stanford.nlp.pipeline.StanfordCoreNLP
import edu.stanford.nlp.sentiment.SentimentCoreAnnotations
import edu.stanford.nlp.util.CoreMap

import scala.collection.JavaConverters._
import org.apache.spark.sql.SparkSession


object TwitterDataAnalysis extends App {

  val dateformat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss")
  val currentdate = dateformat.format(Calendar.getInstance().getTime)

  val spark  = SparkSession.builder()
    .appName("TwitterSentimentalAnalysis")
    .master("local[*]")
    .getOrCreate()

  spark.sparkContext.setLogLevel("WARN")
  import spark.implicits._
  import org.apache.spark.sql.functions._

  val kafkaTwitterStream = spark.readStream
    .format("kafka")
    .option("kafka.bootstrap.servers", "localhost:9093")
    .option("subscribe", "twitter")
    .option("startingOffsets", "earliest")
    .load()

  val twitterDS =
    kafkaTwitterStream.selectExpr("split(value,',')[0] as id"
      ,"split(value,',')[1] as created_at"
      ,"split(value,',')[2] as user"
      ,"split(value,',')[3] as text"
    )

  val sentiment = udf((tweets: String) => {
    if(tweets != null) {
      var mainSentiment = 0
      var longest = 0
      val sentimentText = Array("Very Negative", "Negative", "Neutral", "Positive", "Very Positive")
      val props = new Properties()
      props.setProperty("annotators", "tokenize, ssplit, parse, sentiment")
      new StanfordCoreNLP(props).process(tweets)
        .get(classOf[CoreAnnotations.SentencesAnnotation])
        .asScala.foreach((sentence: CoreMap) => {
        val sentiment = RNNCoreAnnotations
          .getPredictedClass(sentence.get(classOf[SentimentCoreAnnotations.AnnotatedTree]))
        val partText = sentence.toString()

        if (partText.length() > longest) {
          mainSentiment = sentiment
          longest = partText.length()
        }
      })
      sentimentText(mainSentiment)
    }
    else {
      "empty"
    }
  })

  val analysedTwitterDS = twitterDS
    .select($"id",
      $"created_at",
      $"user",
      $"text")
    .withColumn("date", typedLit(currentdate))
    .withColumn("sentiment", sentiment($"text"))//calculate the sentiment using UDF
    .select($"id",
      $"created_at",
      $"user",
      $"text",
      $"date",
      $"sentiment")

  analysedTwitterDS.writeStream
    .outputMode("append")
    .format("es")
    .option("es.nodes","localhost")
    .option("es.port","9200")
    .option("checkpointLocation","/tmp/")
    .start("twitter/doc")
    .awaitTermination()
}
