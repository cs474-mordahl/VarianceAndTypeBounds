trait List[+A]:
  def prepend(elem: A): NonEmptyList[A] = NonEmptyList(elem, this)

case class NonEmptyList[+A](head: A, tail: List[A]) extends List[A]

object Nil extends List[Nothing]
