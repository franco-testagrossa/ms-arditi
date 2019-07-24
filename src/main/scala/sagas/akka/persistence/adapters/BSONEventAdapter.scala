package sagas.akka.persistence.adapters

import akka.persistence.journal.{EventAdapter, EventSeq, Tagged}
import reactivemongo.bson.{BSONDocument, BSONDocumentHandler}

import scala.reflect.ClassTag

// Event Adapter for AkkaPersistence with Mongo and BSON
abstract class BSONEventAdapter[E: ClassTag] extends EventAdapter {
  import scala.reflect.classTag

  val eventType: Class[_] = implicitly[ClassTag[E]].runtimeClass

  def defaultManifest: String = classTag[E].runtimeClass.getSimpleName

  // Delegate
  val _tags: Option[Set[String]]
  val handler: BSONDocumentHandler[E]
  private def bsonRepr(event: E):BSONDocument = handler.write(event)
  private def typeRepr(bson: BSONDocument): E = handler.read(bson)

  def fromBson(bson: BSONDocument): E = typeRepr(bson)
  def toBson(event: E): Either[BSONDocument, Tagged] = _tags match {
    case Some(tags) => Right(Tagged(bsonRepr(event), tags))
    case None => Left(bsonRepr(event))
  }

  // Override this method to support multi-version manifest
  def entries: Seq[(String, BSONEventAdapter[E])] = Seq(defaultManifest -> this)

  // EventAdapter
  override def manifest(event: Any): String = defaultManifest

  override def toJournal(event: Any): Any = event match {
    case e: E => toBson(e) match {
      case Left(b) => b
      case Right(t) => t
    }
    case _ => event
  }

  override def fromJournal(event: Any, manifest: String): EventSeq = event match {
    case bson: BSONDocument => EventSeq(fromBson(bson))
    case _ => EventSeq(event)
  }
}
