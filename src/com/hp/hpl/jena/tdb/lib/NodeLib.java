/*
 * (c) Copyright 2008, 2009 Hewlett-Packard Development Company, LP
 * All rights reserved.
 * [See end of file]
 */

package com.hp.hpl.jena.tdb.lib;

import static com.hp.hpl.jena.tdb.sys.SystemTDB.LenNodeHash ;

import java.io.UnsupportedEncodingException ;
import java.nio.ByteBuffer ;
import java.security.DigestException ;
import java.security.MessageDigest ;
import java.security.NoSuchAlgorithmException ;
import java.util.Iterator ;

import atlas.iterator.Iter ;
import atlas.iterator.Transform ;
import atlas.lib.Bytes ;

import com.hp.hpl.jena.graph.Node ;
import com.hp.hpl.jena.tdb.TDBException ;
import com.hp.hpl.jena.tdb.base.objectfile.ObjectFile ;
import com.hp.hpl.jena.tdb.base.objectfile.StringFile ;
import com.hp.hpl.jena.tdb.base.record.Record ;
import com.hp.hpl.jena.tdb.nodetable.NodeTable ;
import com.hp.hpl.jena.tdb.nodetable.Nodec ;
import com.hp.hpl.jena.tdb.nodetable.NodecSSE ;
import com.hp.hpl.jena.tdb.store.Hash ;
import com.hp.hpl.jena.tdb.store.NodeId ;
import com.hp.hpl.jena.tdb.store.NodeType ;

public class NodeLib
{
    private static Nodec nodec = new NodecSSE() ;
    
    // Characters in IRIs that are illegal and cause SSE problems, but we wish to keep.
    final private static char MarkerChar = '_' ;
    final private static char[] invalidIRIChars = { MarkerChar , ' ' } ; 
    
    // Used.
    
    public static long encodeStore(Node node, StringFile file)
    {
//        String s = encode(node) ;
//        long x = file.write(s) ;
//        return x ;
        return encodeStore(node, file.getByteBufferFile()) ;
    }

    public static Node fetchDecode(long id, StringFile file)
    {
//        String s = file.read(id) ;
//        Node n = decode(s) ;
//        return n ;
        return fetchDecode(id, file.getByteBufferFile()) ;
    }

    public static long encodeStore(Node node, ObjectFile file)
    {
        // Could use a pool of nodec each with one (large!) buffer.
        // c.f. encoding strings.
        ByteBuffer bb = nodec.alloc(node) ;
        int len = nodec.encode(node, bb, null) ;
        long x = file.write(bb) ;
        nodec.release(bb) ;
        return x ;
    }

    public static Node fetchDecode(long id, ObjectFile file)
    {
        ByteBuffer bb = file.read(id) ;
        bb.position(0) ;
        Node n = nodec.decode(bb, null) ;
        return n ;
    }

    // OLD CODE
//    // Use nodec's
//    private static String encode(Node node) { return encode(node, null) ; }
//
//    private static String encode(Node node, PrefixMapping pmap)
//    {
//        if ( node.isBlank() )
//            return "_:"+node.getBlankNodeLabel() ;
//        if ( node.isURI() ) 
//        {
//            // Pesky spaces
//            //throw new TDBException("Space found in URI: "+node) ;
//            String x = StrUtils.encode(node.getURI(), '_', invalidIRIChars) ;
//            if ( x != node.getURI() )
//                node = Node.createURI(x) ; 
//        }
//        
//        return FmtUtils.stringForNode(node, pmap) ;
//    }
//
//    private static Node decode(String s)     { return decode(s, null) ; }
//    
//    private static Node decode(String s, PrefixMapping pmap)
//    {
//        if ( s.startsWith("_:") )   
//        {
//            s = s.substring(2) ;
//            return Node.createAnon(new AnonId(s)) ;
//        }
//
//        if ( s.startsWith("<") )
//        {
//            s = s.substring(1,s.length()-1) ;
//            s = StrUtils.decode(s, MarkerChar) ;
//            return Node.createURI(s) ;
//        }
//        
//        try {
//            // SSE invocation is expensive (??).
//            Node n = SSE.parseNode(s, pmap) ;
//            return n ;
//        } catch (SSEParseException ex)
//        {
//            ALog.fatal(NodeLib.class, "decode: Failed to parse: "+s) ;
//            throw ex ;
//        }
//    }

    public static Hash hash(Node n)
    { 
        Hash h = new Hash(LenNodeHash) ;
        setHash(h, n) ;
        return h ;
    }
    
    public static void setHash(Hash h, Node n) 
    {
        NodeType nt = NodeType.lookup(n) ;
        switch(nt) 
        {
            case URI:
                hash(h, n.getURI(), null, null, nt) ;
                return ;
            case BNODE:
                hash(h, n.getBlankNodeLabel(), null, null, nt) ;
                return ;
            case LITERAL:
                hash(h,
                     n.getLiteralLexicalForm(),
                     n.getLiteralLanguage(),
                     n.getLiteralDatatypeURI(), nt) ;
                return  ;
            case OTHER:
                throw new TDBException("Attempt to hash something strange: "+n) ; 
        }
        throw new TDBException("NodeType broken: "+n) ; 
    }
    
    private static void hash(Hash h, String lex, String lang, String datatype, NodeType nodeType)
    {
        if ( datatype == null )
            datatype = "" ;
        if ( lang == null )
            lang = "" ;
        String toHash = lex + "|" + lang + "|" + datatype+"|"+nodeType.getName() ;
        MessageDigest digest;
        try
        {
            digest = MessageDigest.getInstance("MD5");
            digest.update(toHash.getBytes("UTF8"));
            if ( h.getLen() == 16 )
                // MD5 is 16 bytes.
                digest.digest(h.getBytes(), 0, 16) ;
            else
            {
                byte b[] = digest.digest(); // 16 bytes.
                // Avoid the copy? If length is 16.  digest.digest(bytes, 0, length) needs 16 bytes
                System.arraycopy(b, 0, h.getBytes(), 0, h.getLen()) ;
            }
            return ;
        }
        catch (NoSuchAlgorithmException e)
        { e.printStackTrace(); }
        catch (UnsupportedEncodingException e)
        { e.printStackTrace(); } 
        catch (DigestException ex)
        { ex.printStackTrace(); }
        return ;
    }
    
    public static NodeId getNodeId(Record r, int idx)
    {
        return NodeId.create(Bytes.getLong(r.getKey(), idx)) ;
    }
    
    public static Node termOrAny(Node node)
    {
        if ( node == null || node.isVariable() )
            return Node.ANY ;
        return node ;
    }
    
    public static Iterator<Node> nodes(final NodeTable nodeTable, Iterator<NodeId> iter)
    {
        return Iter.map(iter, new Transform<NodeId, Node>(){
            //@Override
            public Node convert(NodeId item)
            {
                return nodeTable.getNodeForNodeId(item) ;
            }
        }) ;
    }

}

/*
 * (c) Copyright 2008, 2009 Hewlett-Packard Development Company, LP
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. The name of the author may not be used to endorse or promote products
 *    derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */