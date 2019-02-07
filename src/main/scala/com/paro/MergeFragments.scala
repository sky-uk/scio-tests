package com.paro

import java.time.format.DateTimeFormatter
import java.time.{Instant, LocalDateTime, ZoneId}

import com.spotify.scio.ContextAndArgs
import com.spotify.scio.avro._
import org.apache.avro.Schema
import org.apache.avro.generic.GenericRecord
import org.apache.beam.sdk.io.DefaultFilenamePolicy.Params
import org.apache.beam.sdk.io._
import org.apache.beam.sdk.transforms.windowing.{FixedWindows, Window}
import org.joda.time.Duration


object MergeFragments {

  /**
    * Run with:
    * run --project=sky-italia-bigdata --runner=DirectRunner --gcpTempLocation=gs://sky-ita-data-analytics-dev/dataflow/temp
    * --stagingLocation=gs://sky-ita-data-analytics-dev/dataflow/staging --tempLocation=gs://sky-ita-data-analytics-dev/dataflow/temp --region=europe-west1
    *
    * Message Example: 1,user1,user1_email,test_file
    */
  def main(cargs: Array[String]): Unit = {
//    val schema:Schema=TestType.schema

    val (sc, args) = ContextAndArgs(cargs)
    val timestampField:String=args.getOrElse("timestamp-field", "timestamp")
    val schema:Schema=args.getOrElse("schema", "") match {
      case s:String if s.isEmpty =>
        import org.apache.avro.Schema
        new Schema.Parser().parse(TestType.schema.toString)
//      case s:String if s.endsWith(".avro") =>
//        import org.apache.avro.file.DataFileReader
//        import org.apache.avro.generic.GenericDatumReader
//        import org.apache.avro.generic.GenericRecord
//        import org.apache.avro.io.DatumReader
//        val datumReader = new GenericDatumReader[GenericRecord]
//        val dataFileReader = new DataFileReader[_](new Nothing("file.avro"), datumReader)
//        dataFileReader.getSchema
      case s:String  =>
        new Schema.Parser().parse(s)
    }



    //    sc.avroFile()
//      .customInput("input", org.apache.beam.sdk.io.AvroIO.parseAllGenericRecords())

    sc.avroFile[GenericRecord]("*.avro", schema)
      .timestampBy({element => new org.joda.time.Instant(element.get(timestampField).asInstanceOf[Long])} )
      .applyTransform(Window
        .into[GenericRecord](FixedWindows.of(Duration.standardDays(1)))
      )
      .saveAsCustomOutput(
        "Custom avro IO",
        org.apache.beam.sdk.io.AvroIO
          .writeCustomTypeToGenericRecords[GenericRecord]()
          .withNumShards(1)
          .withWindowedWrites()
          .withTempDirectory(FileSystems.matchNewResource(
            "tmp/multiavro/", true))
          .to(new DateTimeDynamicAvroDestinations("date-", timestampField, schema))
      )

    sc.close().waitUntilFinish()
  }


  class DateTimeDynamicAvroDestinations(filePrefix: String, timestampField:String, schema:Schema) extends DynamicAvroDestinations[GenericRecord, String, GenericRecord] {

    override def getSchema(
                            destination: String): Schema =schema

    override def formatRecord(
                               record: GenericRecord): GenericRecord = record

    override def getDestination(
                                 element: GenericRecord): String ={
      val value=element.get(timestampField).asInstanceOf[Long]
      LocalDateTime.ofInstant(
        Instant.ofEpochMilli(value), ZoneId.of("Europe/Rome")).format(DateTimeFormatter.ISO_DATE)
        .replace("-", "")

    }

    override def getDefaultDestination: String = "unknown"

    override def getFilenamePolicy(
                                    destination: String): FileBasedSink.FilenamePolicy =
      DefaultFilenamePolicy.fromParams(
        new Params()
          .withBaseFilename(FileSystems.matchNewResource(filePrefix+destination, false))
          .withShardTemplate("")
          .withSuffix(".avro"))
  }

}

