package openmods.calc;

import com.google.common.collect.Lists;
import java.util.Arrays;
import openmods.calc.parsing.Token;
import openmods.calc.parsing.TokenType;
import org.junit.Assert;

public class CalcTestUtils {

	public static class ValueParserHelper<E> {
		public final IValueParser<E> parser;

		public ValueParserHelper(IValueParser<E> parser) {
			this.parser = parser;
		}

		private E parse(TokenType type, String value) {
			return parser.parseToken(new Token(type, value));
		}

		public E bin(String value) {
			return parse(TokenType.BIN_NUMBER, value);
		}

		public void testBin(E expected, String input) {
			Assert.assertEquals(expected, bin(input));
		}

		public E oct(String value) {
			return parse(TokenType.OCT_NUMBER, value);
		}

		public void testOct(E expected, String input) {
			Assert.assertEquals(expected, oct(input));
		}

		public E dec(String value) {
			return parse(TokenType.DEC_NUMBER, value);
		}

		public void testDec(E expected, String input) {
			Assert.assertEquals(expected, dec(input));
		}

		public E hex(String value) {
			return parse(TokenType.HEX_NUMBER, value);
		}

		public void testHex(E expected, String input) {
			Assert.assertEquals(expected, hex(input));
		}

		public E quoted(String value) {
			return parse(TokenType.QUOTED_NUMBER, value);
		}

		public void testQuoted(E expected, String input) {
			Assert.assertEquals(expected, quoted(input));
		}
	}

	public static Token t(TokenType type, String value) {
		return new Token(type, value);
	}

	public static Token dec(String value) {
		return t(TokenType.DEC_NUMBER, value);
	}

	public static Token oct(String value) {
		return t(TokenType.OCT_NUMBER, value);
	}

	public static Token hex(String value) {
		return t(TokenType.HEX_NUMBER, value);
	}

	public static Token bin(String value) {
		return t(TokenType.BIN_NUMBER, value);
	}

	public static Token quoted(String value) {
		return t(TokenType.QUOTED_NUMBER, value);
	}

	public static Token symbol(String value) {
		return t(TokenType.SYMBOL, value);
	}

	public static Token symbol_args(String value) {
		return t(TokenType.SYMBOL_WITH_ARGS, value);
	}

	public static Token op(String value) {
		return t(TokenType.OPERATOR, value);
	}

	public static final Token COMMA = t(TokenType.SEPARATOR, ",");
	public static final Token RIGHT_BRACKET = t(TokenType.RIGHT_BRACKET, ")");
	public static final Token LEFT_BRACKET = t(TokenType.LEFT_BRACKET, "(");

	public static class DummyBinaryOperator<E> extends BinaryOperator<E> {

		public DummyBinaryOperator(int precendence, String id) {
			super(id, precendence);
		}

		@Override
		protected E execute(E left, E right) {
			return null;
		}
	}

	public static class DummyUnaryOperator<E> extends UnaryOperator<E> {
		public DummyUnaryOperator(String id) {
			super(id);
		}

		@Override
		protected E execute(E value) {
			return null;
		}
	}

	public static final BinaryOperator<String> PLUS = new DummyBinaryOperator<String>(1, "+");

	public static final Token OP_PLUS = op("+");

	public static final UnaryOperator<String> UNARY_PLUS = new DummyUnaryOperator<String>("+");

	public static final BinaryOperator<String> MINUS = new DummyBinaryOperator<String>(1, "-");

	public static final Token OP_MINUS = op("-");

	public static final UnaryOperator<String> UNARY_MINUS = new DummyUnaryOperator<String>("-");

	public static final UnaryOperator<String> UNARY_NEG = new DummyUnaryOperator<String>("!");

	public static final Token OP_NEG = op("!");

	public static final BinaryOperator<String> MULTIPLY = new DummyBinaryOperator<String>(2, "*");

	public static final Token OP_MULTIPLY = op("*");

	public static IExecutable<String> c(String value) {
		return new Value<String>(value);
	}

	public static SymbolReference<String> s(String value) {
		return new SymbolReference<String>(value);
	}

	public static SymbolReference<String> s(String value, int args) {
		return new SymbolReference<String>(value, args, 1);
	}

	public static SymbolReference<String> s(String value, int args, int rets) {
		return new SymbolReference<String>(value, args, rets);
	}

	public static final IValueParser<String> VALUE_PARSER = new IValueParser<String>() {
		@Override
		public String parseToken(Token token) {
			return token.value;
		}
	};

	public static class CalcCheck<E> {
		private final Calculator<E> sut;

		private final IExecutable<E> expr;

		public CalcCheck(Calculator<E> sut, IExecutable<E> expr) {
			this.expr = expr;
			this.sut = sut;
		}

		public CalcCheck<E> expectResult(E value) {
			Assert.assertEquals(value, sut.executeAndPop(expr));
			return this;
		}

		public CalcCheck<E> expectEmptyStack() {
			Assert.assertTrue(Lists.newArrayList(sut.getStack()).isEmpty());
			return this;
		}

		public CalcCheck<E> expectStack(E... values) {
			Assert.assertEquals(Arrays.asList(values), Lists.newArrayList(sut.getStack()));
			return this;
		}

		public CalcCheck<E> execute() {
			sut.execute(expr);
			return this;
		}
	}
}
