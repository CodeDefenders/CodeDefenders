import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class ClassWithPrivateInnerClass {
	private static class InnerClass {
		int foo;

		protected InnerClass(int x) {
			foo = x;
		}
	}

	public int foo(int x) {
		InnerClass ic = new InnerClass(x);
		return ic.foo;
	}
}
