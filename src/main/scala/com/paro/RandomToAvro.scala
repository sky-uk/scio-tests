package com.paro

import java.time.{LocalDateTime, ZoneId}

import com.spotify.scio.ContextAndArgs
import org.apache.beam.sdk.io._
import org.apache.beam.sdk.transforms.windowing.{FixedWindows, Window}
import org.joda.time.Duration

import scala.util.Random

object RandomToAvro {

  /**
    * Run with:
    * run --project=sky-italia-bigdata --runner=DirectRunner --gcpTempLocation=gs://sky-ita-data-analytics-dev/dataflow/temp
    * --stagingLocation=gs://sky-ita-data-analytics-dev/dataflow/staging --tempLocation=gs://sky-ita-data-analytics-dev/dataflow/temp --region=europe-west1
    *
    * Message Example: 1,user1,user1_email,test_file
    */
  def main(cargs: Array[String]): Unit = {

    val (sc, args) = ContextAndArgs(cargs)

    sc.customInput("randomNumbers", GenerateSequence.from(0))
      .map { s =>
        TestType(
          id = s.toInt,
          name = s"user $s",
          email = s"$s@gmail.com",
          timestamp=LocalDateTime.of(2019, 1, Random.nextInt(20)+1, 0,0,0)
            .atZone(ZoneId.of("Europe/Rome")).toInstant.toEpochMilli,
          destination = (s % 5).toString
        )
      }
      .applyTransform(Window
        .into[TestType](FixedWindows.of(Duration.standardSeconds(1)))
      )
      .saveAsCustomOutput(
        "Custom avro IO",
        AvroIO
          .writeCustomTypeToGenericRecords[TestType]()
          .withNumShards(1)
          .withWindowedWrites()
          .withTempDirectory(FileSystems.matchNewResource(
            "tmp/multiavro/", true))
          .to(new TestRandomTypeDynamicAvroDestinations("numbers-"))
      )

    sc.close().waitUntilFinish()
  }

}

