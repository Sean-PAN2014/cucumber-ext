package cn.nextop.test.assertj.asserts;

public class AssertjAssert<A> extends AssertjAbstractAssert<AssertjAssert<A>, A> {
	
	public AssertjAssert(A actual) {
		super(actual, AssertjAssert.class);
	}

}
