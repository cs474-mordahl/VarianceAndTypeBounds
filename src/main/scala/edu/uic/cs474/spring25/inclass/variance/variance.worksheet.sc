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
case class NonEmptyVariantList[+T](head: T, tail: VariantList[T])
    extends VariantList[T]
case object EmptyVariantList extends VariantList[Nothing]

// Now this works, because EmptyVariantList is a subtype of VariantList[String]
val x: VariantList[String] =
  NonEmptyVariantList("Hello", NonEmptyVariantList("World", EmptyVariantList))

/** Okay, let's switch gears to an encoder -- this trait describes something
  * that knows how to encode an object, T, into an string (useful for storing
  * objects)
  */
sealed trait InvariantEncoder[T]:
  def encode(t: T): String

/** Now, let's assume we have a simple class hierarchy as follows: */
sealed trait Animal:
  def name: String
case class Cat(name: String) extends Animal
case class Dog(name: String) extends Animal

// Now, we should be able to get to work, right?
val catEncoder = new InvariantEncoder[Cat]:
  def encode(t: Cat): String = "Cat"

/** What if I don't want to provide an encoder for every class, and instead, I
  * want to provide a general Animal encoder?
  */
val animalEncoder = new InvariantEncoder[Animal]:
  def encode(t: Animal): String = "Animal"

// The following works fine, since cats are animals:
animalEncoder.encode(Cat("Ace"))

/** However, if I try to assign an animal encoder somewhere a cat encoder is
  * expected we get an error:
  */
// val catEncoder: Encoder[Cat] = animalEncoder

/** Again, this is because of default invariance. However, we don't want this
  * type to be covariant, right? Even though Cat <= Animal, we don't want an
  * Encoder[Cat] to be able to be used anywhere an Encoder[Animal] is expected,
  * right? What if we ask the encoder to encode a dog, or a fish?
  *
  * In fact, the relationship here is the exact opposite: since Cat <= Animal,
  * we can use an Encoder[Animal] anywhere an Encoder[Cat] is expected, since an
  * Encoder[Animal] must know how to encode ALL animals.
  *
  * Therefore, by the Liskov Substitution Principle, Encoder[Animal] <=
  * Encoder[Cat]
  *
  * ----- CONTRAVARIANCE ------
  *
  * A type A[T] is contravariant in T if X <= Y ==> A[Y] <= A[X]
  */

sealed trait ContravariantEncoder[-T]:
  //                              ^^ a minus sign indicates contravariance
  def encode(t: T): String

val animalVariantEncoder = new ContravariantEncoder[Animal]:
  def encode(t: Animal): String = t.name

val catVariantEncoder: ContravariantEncoder[Cat] = animalVariantEncoder
catVariantEncoder.encode(Cat("Ace"))

/** Now, let's get to the fun part.
  *
  * Single-argument functions in Scala are subtypes of Function[A, B], where A
  * indicates the input type and B indicates the output type.
  */

trait MyFunction[A, B]:
  def apply(a: A): B

/** Now, let's go back to our list type. We want to be able to map over our
  * list, by providing a function that applies to each element of the list. The
  * function looks like this:
  */
extension [T](elem: T)
  def ::(tail: VariantList[T]) = NonEmptyVariantList(elem, tail)

extension [T](li: VariantList[T])
  def map[B](f: MyFunction[T, B]): VariantList[B] =
    li match
      case NonEmptyVariantList(head, tail) =>
        f(head) :: tail.map(f)
      case EmptyVariantList => EmptyVariantList
end extension

val intList: VariantList[Int] = 1 :: 2 :: 3 :: EmptyVariantList
/* The following provides a new list where each element of intList is
 * incremented. */
intList.map(i => i + 1)

/* Now, obviously, we can apply any function of type MyFunction[Int, B] to this
 * list, for any B. What other functions could we apply?
 *
 * Well, any function that accepts Int or a supertype of Int works, right? For
 * example, the following function should work: */
val objFunction = new MyFunction[Object, Any]:
  def apply(a: Object): String = a.toString()

// However, this won't work.
// intList.map(objFunction)

/** By the above logic, functions should be contravariant in their input, right?
  * If we need a function that takes a type of A, a function that takes any
  * supertype of A should also work, since it has to be able to handle A.
  *
  * But functions are not contravariant in their return type. In fact, functions
  * are COVARIANT in their return type, in that if we expect a function
  * Function[Object, A], we should be able to use any function Function[Object,
  * B] where B <= A, since all B's are subtypes of A, right?
  *
  * In general, when type parameters refer to things we can put in but not take
  * out, those type parameters should be covariant. When a type parameter
  * referes to something we can take out but not put in, the type parameter
  * should be contravariant.
  *
  * Therefore, the correct definition of single-argument functions is as follows
  */
sealed trait MyWorkingFunction[-A, +B]:
  def apply[A, B](a: A): B

// However, if we try to write our map function using this definition, we run into a problem:
// format: off
/*
extension [T](li: VariantList[T])
  def map1[B](f: MyWorkingFunction[T, B]): VariantList[B] =
    li match
      case NonEmptyVariantList(head, tail) =>
        f(head) :: tail.map(f)
      case EmptyVariantList => EmptyVariantList
end extension
*/
// format: on

/** This gets into the problem of providing type bounds, which we'll cover in
  * the typebounds worksheet.
  */
