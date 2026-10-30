package com.myresume.app

import com.myresume.app.data.ExperienceItem
import com.myresume.app.data.ResumeFactory
import com.myresume.app.data.SectionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ResumeFactoryTest {
    @Test
    fun duplicateKeepsContentAndChangesIdentity() {
        val original = ResumeFactory.blankResume("Backend engineer")
        val withContent = original.copy(
            targetJobTitle = "Android developer",
            summary = "Built apps",
            sections = original.sections.map { section ->
                if (section.type == SectionType.EXPERIENCE) {
                    section.copy(
                        experience = listOf(
                            ExperienceItem(
                                id = section.experience.first().id,
                                title = "Engineer",
                                company = "Acme",
                                bullets = listOf("Shipped features")
                            )
                        )
                    )
                } else section
            }
        )
        val copy = ResumeFactory.duplicate(withContent, "Android role")

        assertNotEquals(withContent.id, copy.id)
        assertEquals("Android role", copy.title)
        assertEquals(withContent.targetJobTitle, copy.targetJobTitle)
        assertEquals(withContent.summary, copy.summary)
        assertEquals(withContent.sections.size, copy.sections.size)

        val originalExp = withContent.sections.first { it.type == SectionType.EXPERIENCE }
        val copyExp = copy.sections.first { it.type == SectionType.EXPERIENCE }
        assertNotEquals(originalExp.id, copyExp.id)
        assertEquals(originalExp.experience.first().title, copyExp.experience.first().title)
        assertEquals(originalExp.experience.first().company, copyExp.experience.first().company)
        assertNotEquals(originalExp.experience.first().id, copyExp.experience.first().id)
    }

    @Test
    fun blankResumeHasDefaultSections() {
        val resume = ResumeFactory.blankResume()
        assertTrue(resume.sections.any { it.type == SectionType.EXPERIENCE })
        assertTrue(resume.sections.any { it.type == SectionType.EDUCATION })
        assertTrue(resume.sections.any { it.type == SectionType.SKILLS })
    }
}
