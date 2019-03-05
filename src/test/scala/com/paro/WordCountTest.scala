package com.paro

import com.spotify.scio.io.TextIO
import com.spotify.scio.testing._
import org.scalatest.FlatSpec

class WordCountTest extends FlatSpec {

    "Random test" should "test" in {
        val inData = Seq("a b c d e", "a b a b")
        val expected = Seq("a: 3", "b: 3", "c: 1", "d: 1", "e: 1")

        println("UNIT TEST")
    }
}
