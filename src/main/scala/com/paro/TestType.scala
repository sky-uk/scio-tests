package com.paro

import com.sksamuel.avro4s.{AvroSchema, RecordFormat}
import org.apache.avro.Schema
import org.apache.avro.generic.GenericRecord
case class TestType(id: Int, name: String, email: String,
                    timestamp: Long,
                    destination: String = "default_destination") {
  self =>

  def toRecord(): GenericRecord = TestType.getRecord(self)
}

object TestType {
  val schema: Schema = AvroSchema[TestType]
  lazy val format: RecordFormat[TestType] = RecordFormat[TestType]

  def getRecord(c: TestType): GenericRecord = format.to(c)
}