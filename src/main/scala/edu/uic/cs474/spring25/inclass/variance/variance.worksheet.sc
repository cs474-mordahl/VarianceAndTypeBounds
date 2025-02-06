/** Let's try a different motivation for variance, which is, simply, making the
  * compiler happy.
  *
  * By default, the compiler assumes that higher-kinded types (i.e., generic
  * types) are *invariant,* i.e., that A <= B (we use <= to mean "is a subtype
  * of") does not imply that T[A] <= T[B] for some T.
  *
  * This gets us into trouble when we're trying to compile common types.
  *
  * The code below might be our first attempt to implement a list in Scala.
  */

sealed trait InvariantList[T]
case class NonEmptyInvariantList[T](head: T, tail: InvariantList[T])
    extends InvariantList[T]
case object EmptyInvariantList extends InvariantList[Nothing]

/** So far, so good, right? But we get into trouble when we try to actually use
  * this type, as you can see if you uncomment the line of code below:
  */

// format: off
/*
val x: InvariantList[String] =
  NonEmptyInvariantList(
    "Hello",
    NonEmptyInvariantList("World", EmptyInvariantList)
  )
*/
// format: on

/** You should see a compiler error under EmptyInvariantList, saying that we
  * expected InvariantList[String], but got EmptyInvariantList.type (which is a
  * subtype of InvariantList[Nothing]).
  *
  * But wait, I thought that A <= B means that A can be used anywhere a B is
  * expected? So then, shouldn't InvariantList[Nothing] <=
  * InvariantList[String]?
  *
  * Why does this happen? It's because of type invariance: even though Nothing
  * <= String (remember, Nothing is subtype of everything in Scala),
  * InvariantList[Nothing] </= InvariantList[String] because generic types are,
  * by default, invariant.
  *
  * So, how can we fix this? Well, we simply declare that our custom list type
  * is covariant in its type parameter.
  *
  * --- COVARIANCE ---
  *
  * A type A[T] is covariant in T if X <= Y => (implies) A[X] <= A[Y]. We denote
  * covariance with a + before the type parameter.
  */

sealed trait VariantList[+T]
case class NonEmptyVariantList[T](head: T, tail: VariantList[T])
    extends VariantList[T]
case object EmptyVariantList extends VariantList[Nothing]

// Now this works, because EmptyVariantList is a subtype of VariantList[String]
val x: VariantList[String] =
  NonEmptyVariantList("Hello", NonEmptyVariantList("World", EmptyVariantList))

/** Okay, let's switch gears to an encoder -- this trait describes something
  * that knows how to encode an object, T, into an string (useful for storing
  * objects)
  */
sealed trait Encoder[T]:
  def encode(t: T): String

/** Now, let's assume we have a simple class hierarchy as follows: */
class Animal
class Cat    extends Animal
class Dog    extends Animal
class Poodle extends Dog

/** And we want to provide a method to encode Animals */
extension [T <: Animal](animal: T)
  //       ^^^^^^^^^^^ this is a type bound, it just means any T <= Animal
  def encode(using e: Encoder[T]) = e.encode(animal)

// Now, we should be able to get to work, right?
given Encoder[Cat]:
  def encode(t: Cat): String = "Cat"

val x = Cat().encode()
x
