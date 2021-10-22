package edu.harvard.drs.verify.utility;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * Key utility tests.
 */
public class KeyUtilityTest {

    @Test
    public void testVerifyIngest() {
        assertEquals(
            "2222/1111/11112222/inventory.json",
            KeyUtility.buildKey(11112222L, "inventory.json")
        );

        assertEquals(
            "2222/1110/1112222/inventory.json",
            KeyUtility.buildKey(1112222L, "inventory.json")
        );

        assertEquals(
            "1000/0000/1/inventory.json",
            KeyUtility.buildKey(1L, "inventory.json")
        );
    }

}
