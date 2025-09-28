package com.github.kyrobbins.common.utility;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RsaUtilityTest {

    @ValueSource(
            strings = {
                    "-----BEGIN PRIVATE KEY-----abc-----END PRIVATE KEY-----",
                    "-----BEGIN PUBLIC KEY-----abc-----END PUBLIC KEY-----",
                    "-----BEGIN KEY-----abc-----END KEY-----",
                    "-----BEGIN-----\nabc  \n-----END----- \n",
                    "-----BEGIN-----\r\nabc  -----\r\nEND----- \r\n",
                    "-----BEGIN-----\na  \nb  \nc  \n-----END----- \n",
                    "-----BEGIN-----\r\na \r\nb  \r\nc  \r\n  -----END----- \r\n",
                    "-----BEGIN-----\n\ra  \n\rb  \n\rc  \n\r-----END-----  \n\r",
                    "abc",
                    "a \nb \nc \n",
                    "a  \r\nb  \r\nc  \r\n",
                    "a  \n\rb  \n\rc  \n\r"
            })
    @ParameterizedTest
    void normalizeKeyString(String input) throws Exception {
        String expected = "abc";

        RsaKeyUtility utility = new RsaKeyUtility();

        assertEquals(expected, utility.normalizeKeyString(input));
    }

}
