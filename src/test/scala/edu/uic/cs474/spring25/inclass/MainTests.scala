package edu.uic.cs474.spring25.inclass.encapsulation

// For more information on writing tests, see
// https://scalameta.org/munit/docs/getting-started.html
class MySuite extends munit.FunSuite:
  test("example test that succeeds"):
    val obtained = 42
    val expected = 42
    assertEquals(obtained, expected)
end MySuite
