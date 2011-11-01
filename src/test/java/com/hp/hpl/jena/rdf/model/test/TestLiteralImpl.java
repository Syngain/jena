/*
  (c) Copyright 2003, 2004, 2005, 2006, 2007, 2008, 2009 Hewlett-Packard Development Company, LP, all rights reserved.
  [See end of file]
  $Id: TestLiteralImpl.java,v 1.4 2010-11-12 16:02:21 chris-dollin Exp $
*/

package com.hp.hpl.jena.rdf.model.test;

import com.hp.hpl.jena.datatypes.TypeMapper;
import com.hp.hpl.jena.graph.impl.AdhocDatatype;
import com.hp.hpl.jena.rdf.model.*;

import junit.framework.*;

/**
	TestLiteralImpl - minimal, this is the first time an extra test has been needed above
    the regression testing.

	@author kers
*/
public class TestLiteralImpl extends ModelTestBase 
    {
    public TestLiteralImpl( String name )
        { super( name ); }
        
    public static TestSuite suite()
        { return new TestSuite( TestLiteralImpl.class ); }

    /**
        Test that a non-literal node cannot be as'ed into a literal
    */
    public void testCannotAsNonLiteral()
        { Model m = ModelFactory.createDefaultModel(); 
        try
            { resource( m, "plumPie" ).as( Literal.class ); 
            fail( "non-literal cannot be converted to literal" ); }
        catch (LiteralRequiredException l)
            { pass(); } }
    
    /**
        Test that a literal node can be as'ed into a literal.
    */    
    public void testAsLiteral()
        { Model m = ModelFactory.createDefaultModel();  
        literal( m, "17" ).as( Literal.class );  }
    
    public void testLiteralHasModel()
        {
        Model m = ModelFactory.createDefaultModel();
        testLiteralHasModel( m, m.createLiteral( "hello, world" ) );
        testLiteralHasModel( m, m.createLiteral( "hello, world", "en-UK" ) );
        testLiteralHasModel( m, m.createLiteral( "hello, world", true ) );
        testLiteralHasModel( m, m.createTypedLiteral( "hello, world" ) );
        testLiteralHasModel( m, m.createTypedLiteral( false ) );
        testLiteralHasModel( m, m.createTypedLiteral( 17 ) );
        testLiteralHasModel( m, m.createTypedLiteral( 'x' ) );
        }

    private void testLiteralHasModel( Model m, Literal lit )
        { assertSame( m, lit.getModel() ); }
    
    public void testInModel()
        {
        Model m1 = ModelFactory.createDefaultModel();
        Model m2 = ModelFactory.createDefaultModel();
        Literal l1 = m1.createLiteral( "17" );
        Literal l2 = l1.inModel( m2 );
        assertEquals( l1, l2 );
        assertSame( m2, l2.getModel() );
        }    
    
    public void testSameAdhocClassUS()
    	{
        Model m = ModelFactory.createDefaultModel();
        Resource ra = m.createResource( "eh:/rhubarb" );
        Resource rb = m.createResource( "eh:/cottage" );
        assertNull( "not expecting ResourceImpl to have RDF Datatype get", TypeMapper.getInstance().getTypeByValue( ra ) );
        Literal la = m.createTypedLiteral( ra ); 
        Literal lb = m.createTypedLiteral( rb );
        assertInstanceOf( AdhocDatatype.class, la.getDatatype() );
        assertSame( la.getDatatype(), lb.getDatatype() );
    	}
    
    public void testTypedLiteralTypesAndValues()
        {
        Model m = ModelFactory.createDefaultModel();
        Resource r = m.createResource( "eh:/rhubarb" );
        Literal typed = m.createTypedLiteral( r ); 
        Literal string = m.createLiteral( r.getURI() );
        assertEquals( string.getLexicalForm(), typed.getLexicalForm() );
        assertEquals( string.getLanguage(), typed.getLanguage() );
        assertDiffer( string.getDatatypeURI(), typed.getDatatypeURI() );
        assertNotNull( "a datatype should have been invented for Resource[Impl]", typed.getDatatype() );
        assertDiffer( typed, string );
        assertDiffer( typed.getValue(), string.getValue() );
        assertEquals( r, typed.getValue() );
        assertDiffer( typed.hashCode(), string.hashCode() );
        }
    }

/*
    (c) Copyright 2003, 2004, 2005, 2006, 2007, 2008, 2009 Hewlett-Packard Development Company, LP
    All rights reserved.

    Redistribution and use in source and binary forms, with or without
    modification, are permitted provided that the following conditions
    are met:

    1. Redistributions of source code must retain the above copyright
       notice, this list of conditions and the following disclaimer.

    2. Redistributions in binary form must reproduce the above copyright
       notice, this list of conditions and the following disclaimer in the
       documentation and/or other materials provided with the distribution.

    3. The name of the author may not be used to endorse or promote products
       derived from this software without specific prior written permission.

    THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
    IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
    OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
    IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
    INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
    NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
    DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
    THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
    (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
    THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/