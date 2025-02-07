import java.lang.reflect.AnnotatedArrayType;

class Arrays {
  /** 
   * This method will throw a runtime exception 
   * (java.lang.ArrayStoreException), but Java's compiler cannot 
   * catch this bug, while Scala's would not admit analogous code.
   */
  public static void main(String[] args) {
    Dog[] dogArray = {new Dog(), new Dog()};
    Animal[] animalArray = dogArray;
    animalArray[1] = new Cat();
    System.out.println(dogArray);
  }
}
