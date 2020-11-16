package mainargs

import mainargs.Result.Error.MismatchedArguments
import utest._



object CoreBase{
  case object MyException extends Exception
  @main
  def foo() = 1
  @main
  def bar(i: Int) = i

  @main(doc = "Qux is a function that does stuff")
  def qux(i: Int,
          @arg(doc = "Pass in a custom `s` to override it")
          s: String  = "lols") = s * i
  @main
  def ex() = throw MyException

  def notExported(nonParseable: java.io.InputStream) = ???

  val alsoNotExported = "bazzz"
}

object CorePositionalEnabledTests extends CoreTests(true)
object CorePositionalDisabledTests extends CoreTests(false)

class CoreTests(allowPositional: Boolean) extends TestSuite{
  val check = new Checker(CoreBase, allowPositional = allowPositional)

  val tests = Tests {
    test("formatMainMethods"){
      Renderer.formatMainMethods(CoreBase, check.mains.value, 95)
    }
    test("basicModelling") {
      val names = check.mains.value.map(_.name)

      assert(
        names ==
        List("foo", "bar", "qux", "ex")
      )
      val evaledArgs = check.mains.value.map(_.argSigs.map{
        case ArgSig(name, s, tpe, docs, None, _, _) => (name, tpe, docs, None)
        case ArgSig(name, s, tpe, docs, Some(default), _, _) =>
          (name, tpe, docs, Some(default(CoreBase)))
      })

      assert(
        evaledArgs == List(
          List(),
          List(("i", "int", None, None)),
          List(
            ("i", "int", None, None),
            ("s", "str", Some("Pass in a custom `s` to override it"), Some("lols"))
          ),
          List()
        )
      )
    }

    test("invoke"){
      test - check(
        List("foo"), Result.Success(1)
      )
      test - check(
        List("bar", "--i", "2"), Result.Success(2)
      )
      test - check(
        List("qux", "--i", "2"), Result.Success("lolslols")
      )
      test - check(
        List("qux", "--i", "3", "--s", "x"), Result.Success("xxx")
      )
    }

    test("failures"){
      test("missingParams"){
        test - assertMatch(check.parseInvoke(List("bar"))){
          case Result.Error.MismatchedArguments(
            List(ArgSig("i", _, _, _, _, false, _)),
            Nil,
            Nil,
            None
          ) =>
        }
        test - assertMatch(check.parseInvoke(List("qux", "--s", "omg"))){
          case Result.Error.MismatchedArguments(
          List(ArgSig("i", _, _, _, _, false, _)),
            Nil,
            Nil,
            None
          ) =>
        }
      }

      test("tooManyParams") - check(
        List("foo", "1", "2"),
        Result.Error.MismatchedArguments(Nil, List("1", "2"), Nil, None)
      )

      test("failing") - check(
        List("ex"),
        Result.Error.Exception(CoreBase.MyException)
      )
    }
  }
}


object CorePositionalDisabledOnlyTests extends TestSuite{
  val check = new Checker(CoreBase, allowPositional = false)

  val tests = Tests {
    test("invoke"){
      test - check(
        List("bar", "2"),
        MismatchedArguments(
          missing = List(ArgSig("i",None,"int",None,None,false,false)),
          unknown = List("2")
        )
      )
      test - check(
        List("qux", "2"),
        MismatchedArguments(
          missing = List(ArgSig("i",None,"int",None,None,false,false)),
          unknown = List("2")
        )
      )
      test - check(
        List("qux", "3", "x"),
        MismatchedArguments(
          missing = List(ArgSig("i",None,"int",None,None,false,false)),
          unknown = List("3", "x")
        )
      )
      test - check(
        List("qux", "--i", "3", "x"),
        MismatchedArguments(List(),List("x"),List(),None)
      )
    }

    test("failures"){
      test("invalidParams") - check(
        List("bar", "lol"),
        MismatchedArguments(
          missing = List(ArgSig("i",None,"int",None,None,false,false)),
          unknown = List("lol"),
        )
      )
    }

    test("redundantParams") - check(
      List("qux", "1", "--i", "2"),
      MismatchedArguments(
        missing = List(ArgSig("i", None,"int",None,None,false,false)),
        unknown = List("1", "--i", "2"),
      )
    )
  }
}


object CorePositionalEnabledOnlyTests extends TestSuite{
  val check = new Checker(CoreBase, allowPositional = true)

  val tests = Tests {
    test("invoke"){
      test - check(List("bar", "2"), Result.Success(2))
      test - check(List("qux", "2"), Result.Success("lolslols"))
      test - check(List("qux", "3", "x"), Result.Success("xxx"))
      test - check(
        List("qux", "--i", "3", "x"), Result.Success("xxx")
      )
    }

    test("failures"){
      test("invalidParams") - assertMatch(
        check.parseInvoke(List("bar", "lol"))
      ){
        case Result.Error.InvalidArguments(
        List(Result.ParamError.Failed(ArgSig("i", _, _, _, _, _, _), "lol", _))
        ) =>
      }

      test("redundantParams"){
        val parsed = check.parseInvoke(List("qux", "1", "--i", "2"))
        assertMatch(parsed){
          case Result.Error.MismatchedArguments(
          Nil, Nil, Seq((ArgSig("i", _, _, _, _, false, _), Seq("1", "2"))), None
          ) =>
        }
      }
    }
  }
}