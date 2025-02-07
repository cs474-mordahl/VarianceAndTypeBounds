// We're going to be using animals a lot in this worksheet:
sealed trait Animal:
  def name: String
case class Dog(name: String) extends Animal
case class Cat(name: String) extends Animal

/** In the variance worksheet, we talked about contravariance and covariance,
  * and when each is appropriate.
  *
  * However, the puzzle is not complete. To completely work with type
  * hierarchies, we need a way to specify the minimum and maximum bounds of
  * types.
  *
  * This problem arises when we try to add an append method to our covariant
  * list type.
  */

// format: off
/*
sealed trait BrokenList[+T]
  def prepend(elem: T): BrokenNonEmptyList[T] = BrokenNonEmptyList(elem, this)
case class BrokenNonEmptyList[+T](head: T, tail: BrokenList[T]) extends BrokenList[T]
case object BrokenNil extends BrokenList[Nothing]
*/
// format: on

/** If you uncomment the previous code, you'll see that the compiler complains
  * that we try to use an covariant parameter (T) in a contravariant position.
  * Remember, functions are contravariant in their input and covariant in their
  * output.
  *
  * It's not quite clear in this example why this could be a problem, but I
  * encourage you to stop and think about how, if we created a mutable,
  * covariant collection, what kinds of problems we could run into if the
  * compiler did not complain about this.
  *
  * Here's a hint, consider the following line of code (it doesn't compile but
  * assume it does)
  */

// case class MutableBox[+T](var inside: T)

/** Now, what kind of shenanigans could we get into here?
  *
  * I encourage you to stop here before loooking at the code ahead, and think
  * about it for a minute.
  *
  * If you can't find a solution, scroll down and see what I came up with.
  *
  * Last chance!
  *
  * Here's my example of how, if the compiler did not throw these errors, we
  * would run into trouble.
  */

// format: off
/*
val dogBox: MutableBox[Dog] = MutableBox[Dog](Dog("Fido"))
val animalBox: MutableBox[Animal] = dogBox // Legal because MutableBox[Dog] <= MutableBox[Animal]
animalBox.inside = Cat("Ace") // now dogBox, of type MutableBox[Dog], has a cat inside it!
*/
// format: on

/** This is actually a real problem with Java arrays, which are mutable and
  * covariant!
  *
  * Check out Arrays.java to see an example of the Java compiler allowing
  * unsound code (note that you can't reproduce this in Scala, because Scala
  * arrays are declared as invariant).
  */

/** Here's an example of how we can get in trouble even with immutable
  * collections (the code won't compile, so I commented it out)
  */

// format: off
/*
sealed trait BrokenList[+T]:
  def prepend(elem: T): BrokenNonEmptyList[T] = BrokenNonEmptyList(elem, this)
case class BrokenNonEmptyList[+T](head: T, tail: BrokenList[T]) extends BrokenList[T]
case object BrokenNil extends BrokenList[Nothing]

// We make a list of Cats
val catList: BrokenList[Cat] = BrokenNonEmptyList[Cat](Cat("Ace"), BrokenNil)
val animalList: BrokenList[Animal] = catList // Legal because of Liskov
animalList.prepend(Dog("Fido")) // Now we have a Dog in a Cat list!
*/
// format: on

/** So, how do we fix this? The solution is to flip the variance of T. We do
  * this by introducing a new type, U, that has T as its subtype.
  */

sealed trait MyList[+T]:
  def prepend[U >: T](elem: U): MyNonEmptyList[U] =
    MyNonEmptyList(elem, this)
end MyList
case class MyNonEmptyList[+T](head: T, tail: MyList[T]) extends MyList[T]
case object Nil                                         extends MyList[Nothing]

// Create a list of cats
val cl1: MyList[Cat] = MyNonEmptyList[Cat](Cat("Ace"), Nil)
// Valid because of Liskov
val al1: MyList[Animal] = cl1
/* The compiler won't allow this next line, because it's expecting something
 * that is >= Animal */
// al1.prepend[Dog](Dog("Fido"))
/* However, we can do the following, which is safe because the return value is a
 * List of Animals, and cl1 is not actually modified. */
al1.prepend[Animal](Dog("Fido"))

/** If we try to define a mutable list, Scala won't let us make it covariant. */
// format: off
/*
sealed trait MutableList[+T]
/* Scala won't let us compile the following: */
case class NonEmptyMutableList[+T](var head: T, var tail: MutableList[T])
*/
// format: on

/** The only way we can create a mutable, generic collection is by making it
  * invariant.
  */
sealed trait MutableList[T]
case class NonEmptyMutableList[T](var head: T, var tail: MutableList[T])
    extends MutableList[T]
// Note that we can't make this MutableList[Nothing], because it's no longer covariant.
case class EmptyMutableList[T]() extends MutableList[T]

var ml1 = NonEmptyMutableList[Cat](Cat("Ace"), EmptyMutableList[Cat]())
/* And now this assignment won't work: */
// var ml2: MutableList[Animal] = ml1

/** In the case of MyList.prepend, T is called a "lower type bound", because it
  * specifies the lowest type that U can be.
  *
  * Of course, we can specify upper type bounds, too. Upper type bounds are more
  * common, and are usually used to specify certain traits that a type must
  * meet. For example, consider that we had a trait that describes an object
  * that can be Serialized:
  */
trait Serializable:
  def toByteArray(): Array[Byte]

/** Here, we specify that this method takes any type, so long as that type mixes
  * in the Serializable trait.
  */
def serialize[T <: Serializable](t: T): Array[Byte] =
  t.toByteArray()

/** You may be wondering, why don't we just specify that the type of T is
  * serializable, as in the following:
  */
def otherSerialize(t: Serializable): Array[Byte] = t.toByteArray()

/** This is fine as long as all we need from T is the fact that we can call
  * .toByteArray(). However, using type parameters allows us to ensure better
  * type safety.
  *
  * I think this is demonstrated in the following:
  */
sealed trait ObjectArray:
  def ::(head: Object): ObjectArray = NonEmptyObjectArray(head, this)
case class NonEmptyObjectArray(head: Object, tail: ObjectArray)
    extends ObjectArray
case object EmptyArray extends ObjectArray

/** Now, it's easy for us to make an array that has a smorgasbord of different
  * objects.
  */
val x: ObjectArray =
  "String" :: Integer.valueOf(1) :: Double.box(1.34) :: EmptyArray
