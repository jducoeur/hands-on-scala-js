package scalaparser
package syntax
import acyclic.file
import org.parboiled2._

trait Literals { self: Parser with Basic with Identifiers =>
  object Literals{
    import Basic._
    def FloatingPointLiteral = rule {
      capture(
        "." ~ oneOrMore(Digit) ~ optional(ExponentPart) ~ optional(FloatType) |
          oneOrMore(Digit) ~ (
            "." ~ oneOrMore(Digit) ~ optional(ExponentPart) ~ optional(FloatType) |
            ExponentPart ~ optional(FloatType) |
            optional(ExponentPart) ~ FloatType))
    }

    def IntegerLiteral = rule { capture((DecimalNumeral | HexNumeral) ~ optional(anyOf("Ll"))) }

    def BooleanLiteral = rule { capture(Key.W("true") | Key.W("false"))  }

    def MultilineComment: Rule0 = rule { "/*" ~ zeroOrMore(MultilineComment | !"*/" ~ ANY) ~ "*/" }
    def Comment: Rule0 = rule {
      MultilineComment |
        "//" ~ zeroOrMore(!Basic.Newline ~ ANY) ~ &(Basic.Newline | EOI)
    }

    def Literal = rule {
      (capture(optional("-")) ~ (FloatingPointLiteral | IntegerLiteral) ~> ((sign: String, number) => sign + number)) |
      BooleanLiteral |
      CharacterLiteral |
      StringLiteral |
      SymbolLiteral |
      capture(Key.W("null") ~ !(Basic.Letter | Basic.Digit))
    }


    def EscapedChars = rule { '\\' ~ anyOf("rnt\\\"") }

    // Note that symbols can take on the same values as keywords!
    def SymbolLiteral = rule { ''' ~ capture(Identifiers.PlainId | Identifiers.Keywords) }

    def CharacterLiteral = rule { ''' ~ capture(UnicodeExcape | EscapedChars | !'\\' ~ CharPredicate.from(isPrintableChar)) ~ ''' }

    def MultiLineChars = rule { zeroOrMore(optional('"') ~ optional('"') ~ noneOf("\"")) }
    def StringLiteral = rule {
      (optional(Identifiers.Id) ~ "\"\"\"" ~ capture(MultiLineChars) ~ capture("\"\"\"" ~ zeroOrMore('"')) ~> ((multilineChars: String, quotes) => multilineChars + quotes.dropRight(3))) |
      (optional(Identifiers.Id) ~ '"' ~ capture(zeroOrMore("\\\"" | noneOf("\n\""))) ~ '"')
    }

    def isPrintableChar(c: Char): Boolean = {
      val block = Character.UnicodeBlock.of(c)
      !Character.isISOControl(c) && !Character.isSurrogate(c) && block != null && block != Character.UnicodeBlock.SPECIALS
    }
  }
}

