/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.xpdustry.simple_blacklist.util;

import java.util.regex.Pattern;

import arc.util.Structs;

/**
 * <p><b>InetAddress</b> validation and conversion routines ({@code java.net.InetAddress}).</p>
 *
 * <p>This class provides methods to validate a candidate IP address.
 *
 * @since 1.4
 */
public class InetAddressValidator {
    public static final int IPV4_MAX_BITS_MASK = 32;
    public static final int IPV4_MAX_OCTET_VALUE = 255;
    public static final int IPV6_MAX_BITS_MASK = 128;
    public static final int IPV6_MAX_HEX_GROUPS = 8;
    public static final int IPV6_MAX_HEX_DIGITS_PER_GROUP = 4;
    private static final int MAX_UNSIGNED_SHORT = 0xffff;

    private static final Pattern DIGITS_PATTERN = Pattern.compile("\\d{1,3}");
    private static final Pattern ID_CHECK_PATTERN = Pattern.compile("[^\\s/%]+");

    /**
     * Checks if the specified string is a valid IPv4 or IPv6 address.
     *
     * @param inetAddress the string to validate
     * @return true if the string validates as an IP address
     */
    public static boolean isValid(final String inetAddress) {
        return isValidInet4Address(inetAddress) || isValidInet6Address(inetAddress);
    }

    /**
     * Validates an IPv4 address. Returns true if valid.
     *
     * @param inet4Address the IPv4 address to validate
     * @return true if the argument contains a valid IPv4 address
     */
    public static boolean isValidInet4Address(String inet4Address) {
        // remove the address prefix (% and /)
        inet4Address = removeAddressPrefix(inet4Address, IPV4_MAX_BITS_MASK);
        if (inet4Address == null) {
            return false;
        }
        
        final String[] parts = inet4Address.split("\\.");
        
        // ip v4 addresses can only have four bytes
        if (parts.length != 4) {
            return false;
        }
        
        // verify that address parts are legal
        for (final String ipSegment : parts) {
            if (ipSegment.isEmpty()) {
                return false;
            }
            int iIpSegment = Strings.parseInt(ipSegment);
            if (iIpSegment < 0 || iIpSegment > IPV4_MAX_OCTET_VALUE) {
                return false;
            }
            if (ipSegment.length() > 1 && ipSegment.startsWith("0")) {
                return false;
            }
        }
        
        return true;
    }

    /**
     * Validates an IPv6 address. Returns true if valid.
     *
     * @param inet6Address the IPv6 address to validate
     * @return true if the argument contains a valid IPv6 address
     *
     * @since 1.4.1
     */
    public static boolean isValidInet6Address(String inet6Address) {
        // remove the address prefix (% and /)
        inet6Address = removeAddressPrefix(inet6Address, IPV6_MAX_BITS_MASK);
        if (inet6Address == null) {
            return false;
        }
        
        final boolean containsCompressedZeroes = inet6Address.contains("::");
        if (containsCompressedZeroes && inet6Address.indexOf("::") != inet6Address.lastIndexOf("::")) {
            return false;
        }
        final boolean startsWithCompressed = inet6Address.startsWith("::");
        final boolean endsWithCompressed = inet6Address.endsWith("::");
        final boolean endsWithSep = inet6Address.endsWith(":");
        if (inet6Address.startsWith(":") && !startsWithCompressed || endsWithSep && !endsWithCompressed) {
            return false;
        }
        String[] octets = inet6Address.split(":");
        if (containsCompressedZeroes) {
            if (endsWithCompressed) {
                // String.split() drops ending empty segments
                octets = Structs.add(octets, "");
            } else if (startsWithCompressed && octets.length > 0) {
                octets = Structs.remove(octets, 0);
            }
        }
        if (octets.length > IPV6_MAX_HEX_GROUPS) {
            return false;
        }
        int validOctets = 0;
        int emptyOctets = 0; // consecutive empty chunks
        for (int index = 0; index < octets.length; index++) {
            final String octet = octets[index];
            if (octet.isEmpty()) {
                emptyOctets++;
                if (emptyOctets > 1) {
                    return false;
                }
            } else {
                emptyOctets = 0;
                // Is last chunk an IPv4 address?
                if (index == octets.length - 1 && octet.contains(".")) {
                    if (!isValidInet4Address(octet)) {
                        return false;
                    }
                    validOctets += 2;
                    continue;
                }
                if (octet.length() > IPV6_MAX_HEX_DIGITS_PER_GROUP) {
                    return false;
                }
                int octetInt = Strings.parseInt(octet, 16, Integer.MIN_VALUE);
                if (octetInt < 0 || octetInt > MAX_UNSIGNED_SHORT) {
                    return false;
                }
            }
            validOctets++;
        }
        if (validOctets > IPV6_MAX_HEX_GROUPS || validOctets < IPV6_MAX_HEX_GROUPS && !containsCompressedZeroes) {
            return false;
        }
        return true;
    }
    
    /**
     * Validate and remove the address prefix.
     *
     * @param address the address to validate
     * @return the address without the prefix or null if it's not valid
     */
    private static String removeAddressPrefix(String address, final int maxBits) {
        String[] parts;
        // remove prefix size. This will appear after the zone id (if any)
        parts = address.split("/", -1);
        if (parts.length > 2) {
            return null; // can only have one prefix specifier
        }
        if (parts.length == 2) {
            if (!DIGITS_PATTERN.matcher(parts[1]).matches()) {
                return null; // not a valid number
            }
            final int bits = Strings.parseInt(parts[1]); // cannot fail because of RE check
            if (bits < 0 || bits > maxBits) {
                return null; // out of range
            }
        }
        // remove zone-id
        parts = parts[0].split("%", -1);
        if (parts.length > 2) {
            return null;
        }
        // The id syntax is implementation independent, but it presumably cannot allow:
        // whitespace, '/' or '%'
        if (parts.length == 2 && !ID_CHECK_PATTERN.matcher(parts[1]).matches()) {
            return null; // invalid id
        }
        
        return parts[0];
    }
}