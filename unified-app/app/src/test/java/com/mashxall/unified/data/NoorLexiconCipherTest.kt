package com.mashxall.unified.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NoorLexiconCipherTest {
    @Test
    fun decryptsAnActualOriginalNoorDictionaryEntryWithTheRecoveredKey() {
        // Unmodified Base64 source value for word_list.id=6 (آتَوْا) in db_words_lexicon.db.
        val encryptedAtwa = listOf(
            "DnCyx2N7BfB/hbIoPWCaghYpEtfX+aRsRYH4ATON4M4shSN22RA3GFSr2S81SnzrJcVpazZ9+eOm9FH1GikL9LMZubCxGcl+lB8qo4L1H05YJDquvg5iWKW3Ww6wP456keeeaddtc2BxiXJmjU0105sK0AKIJGs+/",
            "o+VWzGsjNcTggGJb1NE2alu46qUAMIm7lVCtury62WqS+/rq5pFfaydNe0VARTATnJiWak9Y3g4U5c6NhAQSKKQJ2keEkwf2ak1CID27ZLbKVUm6l+4ppY1PwN5WUeZbtYzyzxq/aQHjsF92IUo9pPLBo6wjeP2znoRK08ABBnklrCzRGlzXVPG0CJ49hZaCTYvFUjrOA2dm1BqoCBfREwZr5n9z6KRfYEY0btgERcLOOZc4Yyt2W6ALShJPzW1DLRjCXmHMIcXfAszLnRrQJRvYZukWoM7FcZM4qAy2FcCls9htKWApmIy5DHz4EaLbTfisYlc3EDZs4j2J3Ptz53u9YkMdSfdiDxC38xYW1JDKdk40Bp31Z7d+/",
            "LaA97mEY8s6p29IJDaHV/TLmbc7E+g4/fxgEUQKVBzaN9/UnH+WfIBzFPfSFZBonzW3JzMKm22Kc8rnGJUeAsyhv6Wg77pPtd2gAqc/vRFHJGj1G0RGD4+RyNc9dLPo7VriWuYizWw8oBNkJeXknWbYcyfdmQltMDgD4qL7jyUoaGMLKsKIQMV77pw1rTGgrpSJiGyFyhWLu8//b8KBf3fCOkWkoHzwGBBTYX8eeMW09bL0qdcViayfkkyT35hbxfpZakUleEWTk/uc0I="
        ).joinToString("")

        val decoded = NoorLexiconCipher.decrypt(encryptedAtwa)

        assertTrue(decoded.contains("آتَوْا"))
        assertTrue(decoded.contains("ئەویش لە (إيتاء)ە"))
        assertTrue(decoded.contains("زەكاتیش بیدەن"))
        assertFalse(isLikelyEncryptedLexiconPayload(decoded))
    }
}
