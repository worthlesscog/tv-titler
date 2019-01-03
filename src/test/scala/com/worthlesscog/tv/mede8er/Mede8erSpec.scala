package com.worthlesscog.tv.mede8er

import com.worthlesscog.tv.data.TvEpisode
import org.scalatest._

class Mede8erSpec extends FreeSpec with Matchers {

    val subj = new Mede8er with UnimplementedHttpOps

    def episode(n: String, i: Int, r: Option[Double] = None, v: Option[Int] = None) =
        TvEpisode(name = Some(n), number = Some(i), rating = r, votes = v)

    val (a2, a3, a4) = (episode("A2", 2), episode("A3", 3), episode("A4", 4))
    val (b1, b2, b4, b5) = (episode("B1", 1), episode("B2", 2, Some(5.2)), episode("B4", 4), episode("B5", 5, Some(6.3), Some(3)))
    val (c3, c4, c5, c6) = (episode("C3", 3, Some(7.6), Some(5)), episode("C4", 4), episode("C5", 5, Some(8.2), Some(4)), episode("C6", 6))
    val s1 = Seq(a2, a3, a4)
    val s2 = Seq(b1, b2, b4, b5)
    val s3 = Seq(c3, c4, c5, c6)

    "averageRating" - {
        "should calculate the average rating treating missing vote counts as a single vote" in {
            subj.averageRating(s1) should be(0)
            subj.averageRating(s2) should be(10 * (6.3 * 3 + 5.2 * 1) / 4 toInt)
            subj.averageRating(s3) should be(10 * (7.6 * 5 + 8.2 * 4) / 9 toInt)
        }
    }

    "numbered" - {
        "should collect items with the correct number" in {
            subj.numbered(Seq(s1, s2, s3), 1) should be(Seq(b1))
            subj.numbered(Seq(s1, s2, s3), 2) should be(Seq(a2, b2))
            subj.numbered(Seq(s1, s2, s3), 3) should be(Seq(a3, c3))
            subj.numbered(Seq(s1, s2, s3), 6) should be(Seq(c6))
            subj.numbered(Seq(s1, s2, s3), 7) should be(Seq())
        }
    }

    "numbers" - {
        "should collect item numbers" in {
            subj.numbers(Seq(s1)) should be(Seq(2, 3, 4))
            subj.numbers(Seq(s2)) should be(Seq(1, 2, 4, 5))
            subj.numbers(Seq(s3)) should be(Seq(3, 4, 5, 6))
            subj.numbers(Seq(s2, s3)) should be(Seq(1, 2, 3, 4, 5, 6))
        }
    }

}
