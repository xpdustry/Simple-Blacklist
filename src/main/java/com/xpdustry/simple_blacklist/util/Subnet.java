/*
 * Copyright (c) 2015 CableLabs and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package com.xpdustry.simple_blacklist.util;

import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;


/**
 * @author c3oe.de, based on snippets from Scott Plante, John Kugelmann
 * 
 * Taken from this StackOverflow post: https://stackoverflow.com/a/25165891
 * and modified for us use.
 */
public class Subnet
{
    final public int bytesSubnetCount;
    final public int bitsMaskCount;
    final public BigInteger bigMask;
    final public BigInteger bigSubnetMasked;
    final public String address;
    
    /** For use via format "192.168.0.0/24" or "2001:db8:85a3:880:0:0:0:0/57" */
    public Subnet( final InetAddress subnetAddress, final int bits )
    {
        this.bytesSubnetCount = subnetAddress.getAddress().length; // 4 or 16
        this.bitsMaskCount = bits;
        this.bigMask = BigInteger.valueOf( -1 ).shiftLeft( this.bytesSubnetCount*8 - bits ); // mask = -1 << 32 - bits
        this.bigSubnetMasked = new BigInteger( subnetAddress.getAddress() ).and( this.bigMask );
        this.address = this.bytesSubnetCount == 4 ? toIPv4String( this.bigSubnetMasked ) :
                                                    toIPv6String( this.bigSubnetMasked );
    }

    /** For use via format "192.168.0.0/255.255.255.0" or single address */
    public Subnet( final InetAddress subnetAddress, final InetAddress mask )
    {
        this.bytesSubnetCount = subnetAddress.getAddress().length;
        if ( null == mask ) this.bitsMaskCount = this.bytesSubnetCount*8;
        else {
          int bits = 0;
          for ( byte b : mask.getAddress() ) 
            bits += Integer.bitCount( b );
          this.bitsMaskCount = bits;
        }
        this.bigMask = null == mask ? BigInteger.valueOf( -1 ) : new BigInteger( mask.getAddress() ); // no mask given case is handled here.
        this.bigSubnetMasked = new BigInteger( subnetAddress.getAddress() ).and( this.bigMask );
        this.address = 4 == this.bytesSubnetCount ? toIPv4String( this.bigSubnetMasked ) :
                                                    toIPv6String( this.bigSubnetMasked );
    }

    /**
     * Subnet factory method.
     * @param subnetAndMask format: "192.168.0.0/24" or "192.168.0.0/255.255.255.0"
     *        or single address or "2001:db8:85a3:880:0:0:0:0/57"
     *      
     * @apiNote if the provided subnet doesn't have a mask, a single host mask is used
     *  
     * @return a new instance or null if the IP is not valid
     */
    public static Subnet createInstance( final String subnetAndMask )
    {
        try {
            final String[] stringArr = subnetAndMask.split("[/%]");
            if ( 2 > stringArr.length )
                 return new Subnet( InetAddress.getByName( stringArr[ 0 ] ), null );
            else if ( stringArr[ 1 ].contains(".") || stringArr[ 1 ].contains(":") )
                 return new Subnet( InetAddress.getByName( stringArr[ 0 ] ), InetAddress.getByName( stringArr[ 1 ] ) );
            else return new Subnet( InetAddress.getByName( stringArr[ 0 ] ), Integer.parseInt( stringArr[ 1 ] ) );     
        } catch ( final Exception e ) {
            return null;
        }
    }

    public boolean isInNet( final String address ) { 
      try {
          return isInNet( InetAddress.getByName( address ) );
      } catch (UnknownHostException e) {
          return false;
      } 
    }
    
    public boolean isInNet( final InetAddress address )
    {
        final byte[] bytesAddress = address.getAddress();
        if ( this.bytesSubnetCount != bytesAddress.length )
            return false;
        return new BigInteger( bytesAddress ).and( this.bigMask ).equals( this.bigSubnetMasked );
    }
    
    public boolean isInNet( final Subnet subnet ) {
        //if ( this.bytesSubnetCount != subnet.bytesSubnetCount )
        //    return false;
        return subnet.bigSubnetMasked.and( this.bigMask ).equals( this.bigSubnetMasked );
    }
    
    public boolean partialEquals( final String other ) {
      return toString().equals( other );
    }    
    
    public boolean partialEquals( final Subnet other ) {
      return this.bigSubnetMasked.equals( other.bigSubnetMasked );
    }
    
    @Override
    final public boolean equals( Object obj )
    {
        if ( ! (obj instanceof Subnet) )
            return false;
        final Subnet other = (Subnet)obj;
        return  this.bigSubnetMasked.equals( other.bigSubnetMasked ) &&
                this.bigMask.equals( other.bigMask ) &&
                this.bytesSubnetCount == other.bytesSubnetCount;
    }

    @Override
    final public int hashCode()
    {
        return this.bigSubnetMasked.hashCode();
    }

    @Override
    public String toString() {
      return toString(false);
    }
    
    public String toString(boolean alwaysWithMask) {
      if (alwaysWithMask) return address + "/" + this.bitsMaskCount;
      else {
        String result = address;
        if (this.bitsMaskCount != this.bytesSubnetCount*8) result += "/" + this.bitsMaskCount;
        return result;        
      }
    }

    
    public static String toIPv4String( final BigInteger bigInteger ) {
        String ip = "";
        
        for ( int i = 0; i < 4; i++ ) {
            if ( 0 < i ) ip += '.';
            ip += bigInteger.shiftRight((3 - i) * 8).intValue() & 0xff;
        }
        
        return ip;
    }
    
    /**
     * Taken from <a href="https://github.com/jgonian/commons-ip-math/
     * blob/master/commons-ip-math/src/main/java/com/github/jgonian/ipmath/Ipv6.java">
     * jgonian's commons-ip-math repo</a>
     */
    public static String toIPv6String( final BigInteger bigInteger ) {
        int[] parts = new int[8];

        // Find longest sequence of zeroes. Use the first one if there are
        // multiple sequences of zeroes with the same length.
        int currentZeroPartsLength = 0;
        int currentZeroPartsStart = 0;
        int maxZeroPartsLength = 0;
        int maxZeroPartsStart = 0;

        for (int i = 0; i < parts.length; ++i) {
            parts[i] = bigInteger.shiftRight((7 - i) * 16).intValue() & 0xffff;
            if (parts[i] == 0) {
                if (currentZeroPartsLength == 0)
                    currentZeroPartsStart = i;
                
                if (++currentZeroPartsLength > maxZeroPartsLength) {
                    maxZeroPartsLength = currentZeroPartsLength;
                    maxZeroPartsStart = currentZeroPartsStart;
                }
            } else currentZeroPartsLength = 0;
        }

        
        String ip = "";

        for (int i = 0; i < parts.length; ++i) {
            if (i == maxZeroPartsStart && maxZeroPartsLength > 1) {
                i += maxZeroPartsLength;
                ip += ':';
            }
            if (i > 0) ip += ':';
            if (i <= 7) ip += Integer.toHexString(parts[i]);
            else break;
        }

        return ip;
    }

}