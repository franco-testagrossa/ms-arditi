package sagas.mappings.bson_event_adapter

import reactivemongo.bson.{BSONDocumentHandler, Macros}
import sagas.account.AccountActor.AmountIncreased
import sagas.akka.persistence.adapters.BSONEventAdapter

object AmountIncreasedEventAdapter extends BSONEventAdapter[AmountIncreased] {

  val handler: BSONDocumentHandler[AmountIncreased] =
    Macros.handler[AmountIncreased]

  override val _tags: Option[Set[String]] = Some(Set("AmountIncreasedTag"))
}
