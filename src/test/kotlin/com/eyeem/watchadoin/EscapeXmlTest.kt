package com.eyeem.watchadoin

import org.amshove.kluent.`should be equal to`
import org.junit.Test

class EscapeXmlTest {
    @Test
    fun test() {
        """oneone!!1 '"<>& """.escapeXml() `should be equal to` """oneone!!1 &apos;&quot;&lt;&gt;&amp; """
    }
}