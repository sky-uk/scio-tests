package com.paro

import org.apache.avro.Schema
import org.apache.avro.generic.GenericRecord
import org.apache.beam.sdk.io.DefaultFilenamePolicy.Params
import org.apache.beam.sdk.io.{DefaultFilenamePolicy, DynamicAvroDestinations, FileBasedSink, FileSystems}

class TestRandomTypeDynamicAvroDestinations(filePrefix:String) extends DynamicAvroDestinations[TestType, String, GenericRecord] {
  override def getSchema(
                          destination: String): Schema =
    TestType.schema

  override def formatRecord(
                             record: TestType): GenericRecord =
    TestType.getRecord(record)

  override def getDestination(
                               element: TestType): String = element.destination

  override def getDefaultDestination: String = "tmp/multiavro/"

  override def getFilenamePolicy(
                                  destination: String): FileBasedSink.FilenamePolicy =
    DefaultFilenamePolicy.fromParams(
      new Params()
        .withBaseFilename(FileSystems.matchNewResource(filePrefix, false))
        .withShardTemplate(DefaultFilenamePolicy.DEFAULT_WINDOWED_SHARD_TEMPLATE)
        .withWindowedWrites()
        .withSuffix(".avro"))
}