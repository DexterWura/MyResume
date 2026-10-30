package com.myresume.app.data

import java.util.UUID

object ResumeFactory {
    fun newId(): String = UUID.randomUUID().toString()

    fun defaultSections(): List<ResumeSection> = listOf(
        ResumeSection(
            id = newId(),
            type = SectionType.EXPERIENCE,
            title = "Experience",
            experience = listOf(ExperienceItem(id = newId(), bullets = listOf("")))
        ),
        ResumeSection(
            id = newId(),
            type = SectionType.EDUCATION,
            title = "Education",
            education = listOf(EducationItem(id = newId()))
        ),
        ResumeSection(
            id = newId(),
            type = SectionType.SKILLS,
            title = "Skills",
            skills = listOf(SkillItem(id = newId()))
        ),
        ResumeSection(
            id = newId(),
            type = SectionType.PROJECTS,
            title = "Projects",
            projects = listOf(ProjectItem(id = newId(), bullets = listOf("")))
        ),
        ResumeSection(
            id = newId(),
            type = SectionType.CERTIFICATIONS,
            title = "Certifications",
            certifications = listOf(CertificationItem(id = newId()))
        ),
        ResumeSection(
            id = newId(),
            type = SectionType.LANGUAGES,
            title = "Languages",
            languages = listOf(LanguageItem(id = newId()))
        )
    )

    fun blankResume(title: String = "Untitled resume"): ResumeEntity {
        val now = System.currentTimeMillis()
        return ResumeEntity(
            id = newId(),
            title = title,
            sections = defaultSections(),
            createdAt = now,
            updatedAt = now
        )
    }

    fun duplicate(source: ResumeEntity, newTitle: String): ResumeEntity {
        val now = System.currentTimeMillis()
        return source.copy(
            id = newId(),
            title = newTitle,
            sections = source.sections.map { section ->
                section.copy(
                    id = newId(),
                    experience = section.experience.map { it.copy(id = newId()) },
                    education = section.education.map { it.copy(id = newId()) },
                    skills = section.skills.map { it.copy(id = newId()) },
                    projects = section.projects.map { it.copy(id = newId()) },
                    certifications = section.certifications.map { it.copy(id = newId()) },
                    languages = section.languages.map { it.copy(id = newId()) },
                    custom = section.custom.map { it.copy(id = newId()) }
                )
            },
            createdAt = now,
            updatedAt = now
        )
    }

    fun sectionForType(type: SectionType, customTitle: String = "Custom"): ResumeSection {
        val id = newId()
        return when (type) {
            SectionType.EXPERIENCE -> ResumeSection(
                id = id,
                type = type,
                title = "Experience",
                experience = listOf(ExperienceItem(id = newId(), bullets = listOf("")))
            )
            SectionType.EDUCATION -> ResumeSection(
                id = id,
                type = type,
                title = "Education",
                education = listOf(EducationItem(id = newId()))
            )
            SectionType.SKILLS -> ResumeSection(
                id = id,
                type = type,
                title = "Skills",
                skills = listOf(SkillItem(id = newId()))
            )
            SectionType.PROJECTS -> ResumeSection(
                id = id,
                type = type,
                title = "Projects",
                projects = listOf(ProjectItem(id = newId(), bullets = listOf("")))
            )
            SectionType.CERTIFICATIONS -> ResumeSection(
                id = id,
                type = type,
                title = "Certifications",
                certifications = listOf(CertificationItem(id = newId()))
            )
            SectionType.LANGUAGES -> ResumeSection(
                id = id,
                type = type,
                title = "Languages",
                languages = listOf(LanguageItem(id = newId()))
            )
            SectionType.CUSTOM -> ResumeSection(
                id = id,
                type = type,
                title = customTitle,
                custom = listOf(CustomItem(id = newId()))
            )
        }
    }
}
