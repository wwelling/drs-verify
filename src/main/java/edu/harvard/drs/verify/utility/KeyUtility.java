package edu.harvard.drs.verify.utility;

import static java.lang.String.format;
import static java.lang.String.valueOf;
import static org.apache.commons.lang3.StringUtils.leftPad;
import static org.apache.commons.lang3.StringUtils.reverse;

/**
 * Key utility.
 */
public final class KeyUtility {

    /**
     * Private key utility constructor.
     */
    private KeyUtility() { }

    /**
     * Return key with reverse URN NSS path appended to key.
     *
     * @param id   nss id
     * @param path remaining path
     * @return full s3 key
     */
    public static String buildKey(Long id, String path) {
        String reversedNss = reverse(leftPad(valueOf(id), 8, "0"));

        return format(
            "%s/%s/%s/%s",
            reversedNss.substring(0, 4),
            reversedNss.substring(4, 8),
            id,
            path
        );
    }

}
