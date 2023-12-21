import org.apache.spark.sql.{Row, SparkSession}
import org.apache.spark.sql.expressions.Window
import org.apache.spark.sql.functions.{col, collect_list, explode, from_json, row_number, struct}
import org.apache.spark.sql.types.{BooleanType, DoubleType, IntegerType, StringType, StructType}
import org.apache.spark.sql.functions._
import org.json4s.JsonAST.JObject

import scala.collection.mutable
import org.json4s.jackson.{JsonMethods, Serialization}
import org.json4s.NoTypeHints
import org.json4s.jackson.JsonMethods.{compact, render}
import java.io.{File, PrintWriter}


object Main {

  def main(args: Array[String]): Unit = {
    val spark = SparkSession.builder
      .appName("Data Reader")
      .master("local[*]")
      .getOrCreate()

    spark.sparkContext.setLogLevel("WARN")

    val dataPath = "dane.csv" // path to file
    val inputData = spark.read
      .option("header", "true")
      .option("escape", "\"")
      .csv(dataPath)

    val deviceSchema = new StructType()
      .add("browser", StringType)
      .add("browserVersion", StringType)
      .add("browserSize", StringType)
      .add("operatingSystem", StringType)
      .add("operatingSystemVersion", StringType)
      .add("isMobile", BooleanType)
      .add("mobileDeviceBranding", StringType)
      .add("mobileDeviceModel", StringType)
      .add("mobileInputSelector", StringType)
      .add("mobileDeviceInfo", StringType)
      .add("mobileDeviceMarketingName", StringType)
      .add("flashVersion", StringType)
      .add("language", StringType)
      .add("screenColors", StringType)
      .add("screenResolution", StringType)
      .add("deviceCategory", StringType)

    val geoNetworkSchema = new StructType()
      .add("continent", StringType)
      .add("subContinent", StringType)
      .add("country", StringType)
      .add("region", StringType)
      .add("metro", StringType)
      .add("city", StringType)
      .add("cityId", IntegerType)
      .add("networkDomain", StringType)
      .add("latitude", DoubleType)
      .add("longitude", DoubleType)
      .add("networkLocation", StringType)

    val parsedDf = inputData
      .withColumn("deviceParsed", from_json(col("device"), deviceSchema))
      .withColumn("geoNetworkParsed", from_json(col("geoNetwork"), geoNetworkSchema))

    val df = parsedDf.select(
      col("_c0").as("ordinal"),
      col("date"),
      col("deviceParsed.*"),
      col("fullVisitorId"),
      col("geoNetworkParsed.*")
    )

    val windowSpec = Window.partitionBy("country").orderBy(col("date").desc)

    val dfWithRowNumber = df.withColumn("row_number", row_number().over(windowSpec))

    val top5PerCountry = dfWithRowNumber.filter(col("row_number") <= 5).drop("row_number")

    val resultDf = top5PerCountry
      .groupBy("country")
      .agg(
        collect_list(struct(col("fullVisitorId"), col("date"), col("browser"))).alias("visitor_info")
      )
      .orderBy("country")

    val explodedDf = resultDf.withColumn("visitor_info_exploded", explode(col("visitor_info")))

    val combinedDf = explodedDf
      .select(
        col("country"),
        col("visitor_info_exploded.browser").as("browser"),
        concat_ws(" : ",
          col("visitor_info_exploded.fullVisitorId"),
          col("visitor_info_exploded.date")
        ).as("fullVisitorIdAndDate")
      )
      .groupBy("country", "browser")
      .agg(
        collect_list("fullVisitorIdAndDate").as("fullVisitorIdAndDate")
      )
      .groupBy("country")
      .agg(
        collect_list(struct(col("browser"), col("fullVisitorIdAndDate"))).alias("browserInfo")
      )

    val collectedData: Array[Row] = combinedDf.collect()

    val countryBrowserMap: Map[String, Map[String, mutable.ArraySeq[Map[String, String]]]] = collectedData.map {
      case Row(country: String, browsers: mutable.ArraySeq[Row]) =>
        country -> browsers.map {
          case Row(browser: String, visitorData: mutable.ArraySeq[String]) =>
            browser -> visitorData.map(visitorIdAndDate => {
              val parts = visitorIdAndDate.split(":")
              Map("id" -> parts(0), "date" -> parts(1))
            })
        }.toMap
    }.toMap

    implicit val formats = Serialization.formats(NoTypeHints)

    val parsedJson = JsonMethods.parse(Serialization.write(countryBrowserMap))

    parsedJson match {
      case JObject(fields) =>
        val sortedJson = JObject(fields.sortBy(_._1))
        val sortedJsonString = compact(render(sortedJson))
        println(sortedJsonString)

        val filePath = "./output.json"

        val file = new File(filePath)
        val printWriter = new PrintWriter(file)
        printWriter.write(sortedJsonString)
        printWriter.close()

      case _ => println("no JSON")
    }

    spark.stop
  }
}
