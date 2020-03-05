package cetus.unittest;
import cetus.analysis.RangeDomain;
import cetus.analysis.Section;
import cetus.exec.Driver;
import cetus.hir.*;
import org.junit.Test;
import org.junit.Ignore;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class TestTripletCases {
	@Test
	public void testSetAndGetStride() {
		long strideVal = 2;
		long lb = 1;
		long ub = 10;
		
		Expression strideExpr = new IntegerLiteral(strideVal);
		RangeExpression rangeExpr = new RangeExpression(new IntegerLiteral(lb), new IntegerLiteral(ub));
		rangeExpr.setStride(strideExpr);
		assertEquals(strideExpr, rangeExpr.getStride());
	}

	@Test
	public void testGetStride() {
		long strideVal = 2;
		long lb = 1;
		long ub = 10;

		Expression strideExpr = new IntegerLiteral(strideVal);
		RangeExpression rangeExpr = new RangeExpression(new IntegerLiteral(lb), new IntegerLiteral(ub), new IntegerLiteral(strideVal));
		rangeExpr.setStride(strideExpr);
		assertEquals(strideExpr, rangeExpr.getStride());
	}

	@Test
	public void testDefaultStride() {
		long lb = 1;
		long ub = 10;

		RangeExpression rangeExpr = new RangeExpression(new IntegerLiteral(lb), new IntegerLiteral(ub));
		Expression stride = rangeExpr.getStride();
		assertTrue(stride instanceof IntegerLiteral);
		IntegerLiteral strideVal = (IntegerLiteral)stride;
		assertEquals(strideVal.getValue(), 1);
	}

    @Test
    public void testUnionSectionWithStride1() {
        Driver.setOptionValue("verbosity", "1");
        Section s1 = new Section(1);
        s1.add(new Section.ELEMENT(new ArrayAccess(new NameID("A"),new RangeExpression(new IntegerLiteral(1), new IntegerLiteral(10)))));
        Section s2 = new Section(1);
        s2.add(new Section.ELEMENT(new ArrayAccess(new NameID("A"),new RangeExpression(new IntegerLiteral(5), new IntegerLiteral(15)))));
        RangeDomain r = new RangeDomain();

        Section s3 = s1.unionWith(s2, r);
        Section.ELEMENT e = s3.get(0);
        RangeExpression result = (RangeExpression)e.get(0);

        assertEquals(result.getLB(), new IntegerLiteral(1));
        assertEquals(result.getUB(), new IntegerLiteral(15));
        assertEquals(result.getStride(), new IntegerLiteral(1));
    }

    @Test
    public void testIntersectSectionWithStride1() {
        Driver.setOptionValue("verbosity", "1");
        Section s1 = new Section(1);
        s1.add(new Section.ELEMENT(new ArrayAccess(new NameID("A"),new RangeExpression(new IntegerLiteral(1), new IntegerLiteral(10)))));
        Section s2 = new Section(1);
        s2.add(new Section.ELEMENT(new ArrayAccess(new NameID("A"),new RangeExpression(new IntegerLiteral(5), new IntegerLiteral(15)))));
        RangeDomain r = new RangeDomain();

        Section s3 = s1.intersectWith(s2, r);
        Section.ELEMENT e = s3.get(0);
        RangeExpression result = (RangeExpression)e.get(0);

        assertEquals(result.getLB(), new IntegerLiteral(5));
        assertEquals(result.getUB(), new IntegerLiteral(10));
        assertEquals(result.getStride(), new IntegerLiteral(1));
    }

    @Test
    public void testUnionSectionWithStride2() {
        Driver.setOptionValue("verbosity", "1");
        Section s1 = new Section(1);
        s1.add(new Section.ELEMENT(new ArrayAccess(new NameID("A"),new RangeExpression(new IntegerLiteral(1), new IntegerLiteral(10), new IntegerLiteral(2)))));
        Section s2 = new Section(1);
        s2.add(new Section.ELEMENT(new ArrayAccess(new NameID("A"),new RangeExpression(new IntegerLiteral(5), new IntegerLiteral(15), new IntegerLiteral(2)))));
        RangeDomain r = new RangeDomain();

        Section s3 = s1.unionWith(s2, r);
        Section.ELEMENT e = s3.get(0);
        RangeExpression result = (RangeExpression)e.get(0);

        assertEquals(result.getLB(), new IntegerLiteral(1));
        assertEquals(result.getUB(), new IntegerLiteral(15));
        assertEquals(result.getStride(), new IntegerLiteral(1));
    }

    @Test
    public void testIntersectSectionWithStride2() {
        Driver.setOptionValue("verbosity", "1");
        Section s1 = new Section(1);
        s1.add(new Section.ELEMENT(new ArrayAccess(new NameID("A"),new RangeExpression(new IntegerLiteral(1), new IntegerLiteral(10), new IntegerLiteral(2)))));
        Section s2 = new Section(1);
        s2.add(new Section.ELEMENT(new ArrayAccess(new NameID("A"),new RangeExpression(new IntegerLiteral(5), new IntegerLiteral(15), new IntegerLiteral(2)))));
        RangeDomain r = new RangeDomain();

        Section s3 = s1.intersectWith(s2, r);
        Section.ELEMENT e = s3.get(0);
        RangeExpression result = (RangeExpression)e.get(0);

        assertEquals(result.getLB(), new IntegerLiteral(5));
        assertEquals(result.getUB(), new IntegerLiteral(9));
        assertEquals(result.getStride(), new IntegerLiteral(2));
    }

    @Test
    public void testUnionSectionWithStrideExpr() {
        Driver.setOptionValue("verbosity", "1");
        Section s1 = new Section(1);
        BinaryExpression stride = new BinaryExpression(new NameID("i"), BinaryOperator.ADD, new IntegerLiteral(1));
        s1.add(new Section.ELEMENT(new ArrayAccess(new NameID("A"),new RangeExpression(new IntegerLiteral(1), new IntegerLiteral(10), stride))));
        Section s2 = new Section(1);
        s2.add(new Section.ELEMENT(new ArrayAccess(new NameID("A"),new RangeExpression(new IntegerLiteral(5), new IntegerLiteral(15), new IntegerLiteral(1)))));
        RangeDomain r = new RangeDomain();

        Section s3 = s1.unionWith(s2, r);
        Section.ELEMENT e = s3.get(0);
        RangeExpression result = (RangeExpression)e.get(0);

        assertEquals(result.getLB(), new IntegerLiteral(1));
        assertEquals(result.getUB(), new IntegerLiteral(15));
        assertEquals(result.getStride(), new IntegerLiteral(1));
    }

    @Test
    public void testIntersectSectionWithStrideExpr() {
        Driver.setOptionValue("verbosity", "1");
        Section s1 = new Section(1);
        BinaryExpression stride = new BinaryExpression(new NameID("i"), BinaryOperator.ADD, new IntegerLiteral(1));
        s1.add(new Section.ELEMENT(new ArrayAccess(new NameID("A"),new RangeExpression(new IntegerLiteral(1), new IntegerLiteral(10), stride))));
        Section s2 = new Section(1);
        s2.add(new Section.ELEMENT(new ArrayAccess(new NameID("A"),new RangeExpression(new IntegerLiteral(5), new IntegerLiteral(15), new IntegerLiteral(1)))));
        RangeDomain r = new RangeDomain();

        Section s3 = s1.intersectWith(s2, r);
        Section.ELEMENT e = s3.get(0);
        RangeExpression result = (RangeExpression)e.get(0);

        assertEquals(result.getLB(), new IntegerLiteral(5));
        assertEquals(result.getUB(), new IntegerLiteral(10));
        assertEquals(result.getStride(), new IntegerLiteral(1));
    }

    @Test
    public void testIntersectSectionWithStride() {
        Driver.setOptionValue("verbosity", "1");
        Section s1 = new Section(1);
        BinaryExpression stride = new BinaryExpression(new NameID("i"), BinaryOperator.ADD, new IntegerLiteral(1));
        s1.add(new Section.ELEMENT(new ArrayAccess(new NameID("A"),new RangeExpression(new IntegerLiteral(1), new IntegerLiteral(10), new IntegerLiteral(2)))));
        Section s2 = new Section(1);
        s2.add(new Section.ELEMENT(new ArrayAccess(new NameID("A"),new RangeExpression(new IntegerLiteral(3), new IntegerLiteral(15), new IntegerLiteral(3)))));
        RangeDomain r = new RangeDomain();

        Section s3 = s1.intersectWith(s2, r);
        Section.ELEMENT e = s3.get(0);
        RangeExpression result = (RangeExpression)e.get(0);

        assertEquals(result.getLB(), new IntegerLiteral(3));
        assertEquals(result.getUB(), new IntegerLiteral(9));
        assertEquals(result.getStride(), new IntegerLiteral(6));
    }

    @Test
    public void testIntersectSectionWithStrideEmpty() {
        Driver.setOptionValue("verbosity", "1");
        Section s1 = new Section(1);
        BinaryExpression stride = new BinaryExpression(new NameID("i"), BinaryOperator.ADD, new IntegerLiteral(1));
        s1.add(new Section.ELEMENT(new ArrayAccess(new NameID("A"),new RangeExpression(new IntegerLiteral(1), new IntegerLiteral(10), new IntegerLiteral(2)))));
        Section s2 = new Section(1);
        s2.add(new Section.ELEMENT(new ArrayAccess(new NameID("A"),new RangeExpression(new IntegerLiteral(0), new IntegerLiteral(15), new IntegerLiteral(2)))));
        RangeDomain r = new RangeDomain();

        Section s3 = s1.intersectWith(s2, r);
        assertTrue(s3.isEmpty());
    }
}
