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
  * We can clearly see a situation where, if the compiler allowed us to use this
  * code, we would run into trouble.
  *
  * I encourage you to stop here before loooking at the code ahead, and think
  * about it for a minute. What bad things could we do if the compiler admitted
  * the code above?
  *
  * If you can't find a solution, scroll down and see what I came up with.
  *
  * Last chance!
  *
  * Here's my example of how, if the compiler did not throw these errors, we
  * would run into trouble.
  */

// We make a list of animals
/* val animalList: BrokenList[Animal] =
 * BrokenNonEmptyList[Cat](Cat(), BrokenNonEmptyList[Cat](Cat(), BrokenNil))
 * animalList.getClass() */
// format: off
/*
val bl: BrokenNonEmptyList[Animal] = BrokenNonEmptyList[Cat](Cat(), BrokenNil)
val cl = bl.prepend(Dog()) // This works, even though we're now putting a dog into a list of cats!
*/
// format: on

/** So, how do we fix this? The solution is to flip the variance of T. We do
  * this by introducing a new type, U, that has T as its subtype.
  */

sealed trait Animal:
  def name: String
case class Dog(name: String) extends Animal
case class Cat(name: String) extends Animal

sealed trait MyList[+T]:
  def prepend[U >: T](elem: U): MyNonEmptyList[U] = MyNonEmptyList(elem, this)
end MyList
case class MyNonEmptyList[+T](head: T, tail: MyList[T]) extends MyList[T]
case object Nil                                         extends MyList[Nothing]

val cl1: MyList[Cat]    = MyNonEmptyList[Cat](Cat("Ace"), Nil)
val al1: MyList[Animal] = cl1
al1.prepend(Dog("Fido"))

/** In this case, T is called a "lower type bound", because it specifies the
  * lowest type that U can be.
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
val x: ObjectArray = "String" :: Integer(1) :: Double.box(1.34) :: EmptyArray
