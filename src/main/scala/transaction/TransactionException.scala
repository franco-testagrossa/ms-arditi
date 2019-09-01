package transaction

trait TransactionException extends Exception {
  def sinkTopic: String

  override def getMessage: String = super.getMessage
}
