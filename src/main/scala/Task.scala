object Task {
  sealed trait Customer extends Product {
    def id: String

    def resignProbability: Double
  }

  case class Vip(id: String, resignProbability: Double, phoneNumber: String, email: String) extends Customer

  case class Exclusive(id: String, resignProbability: Double, email: String) extends Customer

  case class Standard(id: String, resignProbability: Double, phoneNumber: String) extends Customer

  val customers: Seq[Customer] = Seq(
    Standard("id-1", 0.8, "123456789"),
    Exclusive("id-5", 0.22, "exclusiveCustomer@test.pl"),
    Vip("id-11", 0.22, "111111111", "vip11Customer@test.pl"),
    Vip("id-7", 0.75, "222222222", "vip7Customer@test.pl"),
    Vip("id-32", 0.78, "", "vip32Customer@test.pl"),
    Standard("id-44", 0.49, "0987654321")
  )

  // START CODE HERE
  def getChurns(customerList: Seq[Customer]): Seq[String] = {
    val priority: Map[Class[_], Int] = Map(classOf[Vip] -> 1, classOf[Exclusive] -> 2, classOf[Standard] -> 3)
    customerList
      .filter(_.resignProbability > 0.5)
      .sortBy(c => (priority(c.getClass), -c.resignProbability))
      .map {
        case Vip(id, _, phoneNumber, email) => Seq(id, phoneNumber, email)
        case Exclusive(id, _, email) => Seq(id, email)
        case Standard(id, _, phoneNumber) => Seq(id, phoneNumber)
      }.map(_.filter(_.nonEmpty).mkString(" : "))
  }
  // END CODE HERE


  def main(args: Array[String]): Unit = {
    getChurns(customers).foreach(println)
  }
}