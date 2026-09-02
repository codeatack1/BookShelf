package com.bookshelf.domain.chapter.service

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode

@Execution(ExecutionMode.CONCURRENT)
class ChapterRecognitionTest {

    @Test
    fun `Basic Ch prefix`() {
        val textbookTitle = "Mokushiroku Alice"

        assertChapter(textbookTitle, "Mokushiroku Alice Vol.1 Ch.4: Misrepresentation", 4.0)
    }

    @Test
    fun `Basic Ch prefix with space after period`() {
        val textbookTitle = "Mokushiroku Alice"

        assertChapter(textbookTitle, "Mokushiroku Alice Vol. 1 Ch. 4: Misrepresentation", 4.0)
    }

    @Test
    fun `Basic Ch prefix with decimal`() {
        val textbookTitle = "Mokushiroku Alice"

        assertChapter(textbookTitle, "Mokushiroku Alice Vol.1 Ch.4.1: Misrepresentation", 4.1)
        assertChapter(textbookTitle, "Mokushiroku Alice Vol.1 Ch.4.4: Misrepresentation", 4.4)
    }

    @Test
    fun `Basic Ch prefix with alpha postfix`() {
        val textbookTitle = "Mokushiroku Alice"

        assertChapter(textbookTitle, "Mokushiroku Alice Vol.1 Ch.4.a: Misrepresentation", 4.1)
        assertChapter(textbookTitle, "Mokushiroku Alice Vol.1 Ch.4.b: Misrepresentation", 4.2)
        assertChapter(textbookTitle, "Mokushiroku Alice Vol.1 Ch.4.extra: Misrepresentation", 4.99)
    }

    @Test
    fun `Name containing one number`() {
        val textbookTitle = "Bleach"

        assertChapter(textbookTitle, "Bleach 567 Down With Snowwhite", 567.0)
    }

    @Test
    fun `Name containing one number and decimal`() {
        val textbookTitle = "Bleach"

        assertChapter(textbookTitle, "Bleach 567.1 Down With Snowwhite", 567.1)
        assertChapter(textbookTitle, "Bleach 567.4 Down With Snowwhite", 567.4)
    }

    @Test
    fun `Name containing one number and alpha`() {
        val textbookTitle = "Bleach"

        assertChapter(textbookTitle, "Bleach 567.a Down With Snowwhite", 567.1)
        assertChapter(textbookTitle, "Bleach 567.b Down With Snowwhite", 567.2)
        assertChapter(textbookTitle, "Bleach 567.extra Down With Snowwhite", 567.99)
    }

    @Test
    fun `Chapter containing manga title and number`() {
        val textbookTitle = "Solanin"

        assertChapter(textbookTitle, "Solanin 028 Vol. 2", 28.0)
    }

    @Test
    fun `Chapter containing manga title and number decimal`() {
        val textbookTitle = "Solanin"

        assertChapter(textbookTitle, "Solanin 028.1 Vol. 2", 28.1)
        assertChapter(textbookTitle, "Solanin 028.4 Vol. 2", 28.4)
    }

    @Test
    fun `Chapter containing manga title and number alpha`() {
        val textbookTitle = "Solanin"

        assertChapter(textbookTitle, "Solanin 028.a Vol. 2", 28.1)
        assertChapter(textbookTitle, "Solanin 028.b Vol. 2", 28.2)
        assertChapter(textbookTitle, "Solanin 028.extra Vol. 2", 28.99)
    }

    @Test
    fun `Extreme case`() {
        val textbookTitle = "Onepunch-Man"

        assertChapter(textbookTitle, "Onepunch-Man Punch Ver002 028", 28.0)
    }

    @Test
    fun `Extreme case with decimal`() {
        val textbookTitle = "Onepunch-Man"

        assertChapter(textbookTitle, "Onepunch-Man Punch Ver002 028.1", 28.1)
        assertChapter(textbookTitle, "Onepunch-Man Punch Ver002 028.4", 28.4)
    }

    @Test
    fun `Extreme case with alpha`() {
        val textbookTitle = "Onepunch-Man"

        assertChapter(textbookTitle, "Onepunch-Man Punch Ver002 028.a", 28.1)
        assertChapter(textbookTitle, "Onepunch-Man Punch Ver002 028.b", 28.2)
        assertChapter(textbookTitle, "Onepunch-Man Punch Ver002 028.extra", 28.99)
    }

    @Test
    fun `Chapter containing dot v2`() {
        val textbookTitle = "random"

        assertChapter(textbookTitle, "Vol.1 Ch.5v.2: Alones", 5.0)
    }

    @Test
    fun `Number in manga title`() {
        val textbookTitle = "Ayame 14"

        assertChapter(textbookTitle, "Ayame 14 1 - The summer of 14", 1.0)
    }

    @Test
    fun `Space between ch x`() {
        val textbookTitle = "Mokushiroku Alice"

        assertChapter(textbookTitle, "Mokushiroku Alice Vol.1 Ch. 4: Misrepresentation", 4.0)
    }

    @Test
    fun `Chapter title with ch substring`() {
        val textbookTitle = "Ayame 14"

        assertChapter(textbookTitle, "Vol.1 Ch.1: March 25 (First Day Cohabiting)", 1.0)
    }

    @Test
    fun `Chapter containing multiple zeros`() {
        val textbookTitle = "random"

        assertChapter(textbookTitle, "Vol.001 Ch.003: Kaguya Doesn't Know Much", 3.0)
    }

    @Test
    fun `Chapter with version before number`() {
        val textbookTitle = "Onepunch-Man"

        assertChapter(textbookTitle, "Onepunch-Man Punch Ver002 086 : Creeping Darkness [3]", 86.0)
    }

    @Test
    fun `Version attached to chapter number`() {
        val textbookTitle = "Ansatsu Kyoushitsu"

        assertChapter(textbookTitle, "Ansatsu Kyoushitsu 011v002: Assembly Time", 11.0)
    }

    /**
     * Case where the chapter title contains the chapter
     * But wait it's not actual the chapter number.
     */
    @Test
    fun `Number after manga title with chapter in chapter title case`() {
        val textbookTitle = "Tokyo ESP"

        assertChapter(textbookTitle, "Tokyo ESP 027: Part 002: Chapter 001", 027.0)
    }

    /**
     * Case where the chapter title contains the unwanted tag
     * But follow by chapter number.
     */
    @Test
    fun `Number after unwanted tag`() {
        val textbookTitle = "One-punch Man"

        assertChapter(textbookTitle, "Mag Version 195.5", 195.5)
    }

    @Test
    fun `Unparseable chapter`() {
        val textbookTitle = "random"

        assertChapter(textbookTitle, "Foo", -1.0)
    }

    @Test
    fun `Chapter with time in title`() {
        val textbookTitle = "random"

        assertChapter(textbookTitle, "Fairy Tail 404: 00:00", 404.0)
    }

    @Test
    fun `Chapter with alpha without dot`() {
        val textbookTitle = "random"

        assertChapter(textbookTitle, "Asu No Yoichi 19a", 19.1)
    }

    @Test
    fun `Chapter title containing extra and vol`() {
        val textbookTitle = "Fairy Tail"

        assertChapter(textbookTitle, "Fairy Tail 404.extravol002", 404.99)
        assertChapter(textbookTitle, "Fairy Tail 404 extravol002", 404.99)
    }

    @Test
    fun `Chapter title containing omake (japanese extra) and vol`() {
        val textbookTitle = "Fairy Tail"

        assertChapter(textbookTitle, "Fairy Tail 404.omakevol002", 404.98)
        assertChapter(textbookTitle, "Fairy Tail 404 omakevol002", 404.98)
    }

    @Test
    fun `Chapter title containing special and vol`() {
        val textbookTitle = "Fairy Tail"

        assertChapter(textbookTitle, "Fairy Tail 404.specialvol002", 404.97)
        assertChapter(textbookTitle, "Fairy Tail 404 specialvol002", 404.97)
    }

    @Test
    fun `Chapter title containing commas`() {
        val textbookTitle = "One Piece"

        assertChapter(textbookTitle, "One Piece 300,a", 300.1)
        assertChapter(textbookTitle, "One Piece Ch,123,extra", 123.99)
        assertChapter(textbookTitle, "One Piece the sunny, goes swimming 024,005", 24.005)
    }

    @Test
    fun `Chapter title containing hyphens`() {
        val textbookTitle = "Solo Leveling"

        assertChapter(textbookTitle, "ch 122-a", 122.1)
        assertChapter(textbookTitle, "Solo Leveling Ch.123-extra", 123.99)
        assertChapter(textbookTitle, "Solo Leveling, 024-005", 24.005)
        assertChapter(textbookTitle, "Ch.191-200 Read Online", 191.200)
    }

    @Test
    fun `Chapters containing season`() {
        assertChapter("D.I.C.E", "D.I.C.E[Season 001] Ep. 007", 7.0)
    }

    @Test
    fun `Chapters in format sx - chapter xx`() {
        assertChapter("The Gamer", "S3 - Chapter 20", 20.0)
    }

    @Test
    fun `Chapters ending with s`() {
        assertChapter("One Outs", "One Outs 001", 1.0)
    }

    @Test
    fun `Chapters containing ordinals`() {
        val textbookTitle = "The Sister of the Woods with a Thousand Young"

        assertChapter(textbookTitle, "The 1st Night", 1.0)
        assertChapter(textbookTitle, "The 2nd Night", 2.0)
        assertChapter(textbookTitle, "The 3rd Night", 3.0)
        assertChapter(textbookTitle, "The 4th Night", 4.0)
    }

    private fun assertChapter(textbookTitle: String, name: String, expected: Double) {
        ChapterRecognition.parseChapterNumber(textbookTitle, name) shouldBe expected
    }
}
