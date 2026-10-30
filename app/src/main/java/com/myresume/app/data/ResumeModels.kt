package com.myresume.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import org.json.JSONArray
import org.json.JSONObject

enum class SectionType {
    EXPERIENCE,
    EDUCATION,
    SKILLS,
    PROJECTS,
    CERTIFICATIONS,
    LANGUAGES,
    CUSTOM
}

data class ExperienceItem(
    val id: String,
    val title: String = "",
    val company: String = "",
    val location: String = "",
    val startDate: String = "",
    val endDate: String = "",
    val bullets: List<String> = emptyList()
)

data class EducationItem(
    val id: String,
    val degree: String = "",
    val school: String = "",
    val location: String = "",
    val startDate: String = "",
    val endDate: String = "",
    val details: String = ""
)

data class SkillItem(
    val id: String,
    val name: String = "",
    val level: String = ""
)

data class ProjectItem(
    val id: String,
    val name: String = "",
    val link: String = "",
    val description: String = "",
    val bullets: List<String> = emptyList()
)

data class CertificationItem(
    val id: String,
    val name: String = "",
    val issuer: String = "",
    val date: String = ""
)

data class LanguageItem(
    val id: String,
    val name: String = "",
    val proficiency: String = ""
)

data class CustomItem(
    val id: String,
    val title: String = "",
    val body: String = ""
)

data class ResumeSection(
    val id: String,
    val type: SectionType,
    val title: String,
    val visible: Boolean = true,
    val experience: List<ExperienceItem> = emptyList(),
    val education: List<EducationItem> = emptyList(),
    val skills: List<SkillItem> = emptyList(),
    val projects: List<ProjectItem> = emptyList(),
    val certifications: List<CertificationItem> = emptyList(),
    val languages: List<LanguageItem> = emptyList(),
    val custom: List<CustomItem> = emptyList()
) {
    fun isEmpty(): Boolean = when (type) {
        SectionType.EXPERIENCE -> experience.isEmpty() || experience.all {
            it.title.isBlank() && it.company.isBlank() && it.bullets.all(String::isBlank)
        }
        SectionType.EDUCATION -> education.isEmpty() || education.all {
            it.degree.isBlank() && it.school.isBlank()
        }
        SectionType.SKILLS -> skills.isEmpty() || skills.all { it.name.isBlank() }
        SectionType.PROJECTS -> projects.isEmpty() || projects.all {
            it.name.isBlank() && it.description.isBlank()
        }
        SectionType.CERTIFICATIONS -> certifications.isEmpty() || certifications.all {
            it.name.isBlank()
        }
        SectionType.LANGUAGES -> languages.isEmpty() || languages.all { it.name.isBlank() }
        SectionType.CUSTOM -> custom.isEmpty() || custom.all {
            it.title.isBlank() && it.body.isBlank()
        }
    }
}

data class ContactInfo(
    val fullName: String = "",
    val email: String = "",
    val phone: String = "",
    val location: String = "",
    val website: String = "",
    val linkedIn: String = ""
)

@Entity(tableName = "resumes")
@TypeConverters(ResumeConverters::class)
data class ResumeEntity(
    @PrimaryKey val id: String,
    val title: String,
    val targetJobTitle: String = "",
    val summary: String = "",
    val contact: ContactInfo = ContactInfo(),
    val sections: List<ResumeSection> = emptyList(),
    val templateId: String = "classic_ats_navy",
    val photoPath: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

class ResumeConverters {
    @TypeConverter
    fun contactToString(value: ContactInfo): String = JSONObject().apply {
        put("fullName", value.fullName)
        put("email", value.email)
        put("phone", value.phone)
        put("location", value.location)
        put("website", value.website)
        put("linkedIn", value.linkedIn)
    }.toString()

    @TypeConverter
    fun contactFromString(value: String): ContactInfo {
        val o = JSONObject(value)
        return ContactInfo(
            fullName = o.optString("fullName"),
            email = o.optString("email"),
            phone = o.optString("phone"),
            location = o.optString("location"),
            website = o.optString("website"),
            linkedIn = o.optString("linkedIn")
        )
    }

    @TypeConverter
    fun sectionsToString(value: List<ResumeSection>): String {
        val array = JSONArray()
        value.forEach { section ->
            array.put(sectionToJson(section))
        }
        return array.toString()
    }

    @TypeConverter
    fun sectionsFromString(value: String): List<ResumeSection> {
        val array = JSONArray(value)
        return buildList {
            for (i in 0 until array.length()) {
                add(sectionFromJson(array.getJSONObject(i)))
            }
        }
    }

    private fun sectionToJson(section: ResumeSection): JSONObject = JSONObject().apply {
        put("id", section.id)
        put("type", section.type.name)
        put("title", section.title)
        put("visible", section.visible)
        put("experience", JSONArray().apply {
            section.experience.forEach { item ->
                put(JSONObject().apply {
                    put("id", item.id)
                    put("title", item.title)
                    put("company", item.company)
                    put("location", item.location)
                    put("startDate", item.startDate)
                    put("endDate", item.endDate)
                    put("bullets", JSONArray(item.bullets))
                })
            }
        })
        put("education", JSONArray().apply {
            section.education.forEach { item ->
                put(JSONObject().apply {
                    put("id", item.id)
                    put("degree", item.degree)
                    put("school", item.school)
                    put("location", item.location)
                    put("startDate", item.startDate)
                    put("endDate", item.endDate)
                    put("details", item.details)
                })
            }
        })
        put("skills", JSONArray().apply {
            section.skills.forEach { item ->
                put(JSONObject().apply {
                    put("id", item.id)
                    put("name", item.name)
                    put("level", item.level)
                })
            }
        })
        put("projects", JSONArray().apply {
            section.projects.forEach { item ->
                put(JSONObject().apply {
                    put("id", item.id)
                    put("name", item.name)
                    put("link", item.link)
                    put("description", item.description)
                    put("bullets", JSONArray(item.bullets))
                })
            }
        })
        put("certifications", JSONArray().apply {
            section.certifications.forEach { item ->
                put(JSONObject().apply {
                    put("id", item.id)
                    put("name", item.name)
                    put("issuer", item.issuer)
                    put("date", item.date)
                })
            }
        })
        put("languages", JSONArray().apply {
            section.languages.forEach { item ->
                put(JSONObject().apply {
                    put("id", item.id)
                    put("name", item.name)
                    put("proficiency", item.proficiency)
                })
            }
        })
        put("custom", JSONArray().apply {
            section.custom.forEach { item ->
                put(JSONObject().apply {
                    put("id", item.id)
                    put("title", item.title)
                    put("body", item.body)
                })
            }
        })
    }

    private fun sectionFromJson(o: JSONObject): ResumeSection {
        val type = SectionType.valueOf(o.getString("type"))
        return ResumeSection(
            id = o.getString("id"),
            type = type,
            title = o.optString("title"),
            visible = o.optBoolean("visible", true),
            experience = o.optJSONArray("experience")?.let { arr ->
                buildList {
                    for (i in 0 until arr.length()) {
                        val item = arr.getJSONObject(i)
                        add(
                            ExperienceItem(
                                id = item.getString("id"),
                                title = item.optString("title"),
                                company = item.optString("company"),
                                location = item.optString("location"),
                                startDate = item.optString("startDate"),
                                endDate = item.optString("endDate"),
                                bullets = stringList(item.optJSONArray("bullets"))
                            )
                        )
                    }
                }
            } ?: emptyList(),
            education = o.optJSONArray("education")?.let { arr ->
                buildList {
                    for (i in 0 until arr.length()) {
                        val item = arr.getJSONObject(i)
                        add(
                            EducationItem(
                                id = item.getString("id"),
                                degree = item.optString("degree"),
                                school = item.optString("school"),
                                location = item.optString("location"),
                                startDate = item.optString("startDate"),
                                endDate = item.optString("endDate"),
                                details = item.optString("details")
                            )
                        )
                    }
                }
            } ?: emptyList(),
            skills = o.optJSONArray("skills")?.let { arr ->
                buildList {
                    for (i in 0 until arr.length()) {
                        val item = arr.getJSONObject(i)
                        add(
                            SkillItem(
                                id = item.getString("id"),
                                name = item.optString("name"),
                                level = item.optString("level")
                            )
                        )
                    }
                }
            } ?: emptyList(),
            projects = o.optJSONArray("projects")?.let { arr ->
                buildList {
                    for (i in 0 until arr.length()) {
                        val item = arr.getJSONObject(i)
                        add(
                            ProjectItem(
                                id = item.getString("id"),
                                name = item.optString("name"),
                                link = item.optString("link"),
                                description = item.optString("description"),
                                bullets = stringList(item.optJSONArray("bullets"))
                            )
                        )
                    }
                }
            } ?: emptyList(),
            certifications = o.optJSONArray("certifications")?.let { arr ->
                buildList {
                    for (i in 0 until arr.length()) {
                        val item = arr.getJSONObject(i)
                        add(
                            CertificationItem(
                                id = item.getString("id"),
                                name = item.optString("name"),
                                issuer = item.optString("issuer"),
                                date = item.optString("date")
                            )
                        )
                    }
                }
            } ?: emptyList(),
            languages = o.optJSONArray("languages")?.let { arr ->
                buildList {
                    for (i in 0 until arr.length()) {
                        val item = arr.getJSONObject(i)
                        add(
                            LanguageItem(
                                id = item.getString("id"),
                                name = item.optString("name"),
                                proficiency = item.optString("proficiency")
                            )
                        )
                    }
                }
            } ?: emptyList(),
            custom = o.optJSONArray("custom")?.let { arr ->
                buildList {
                    for (i in 0 until arr.length()) {
                        val item = arr.getJSONObject(i)
                        add(
                            CustomItem(
                                id = item.getString("id"),
                                title = item.optString("title"),
                                body = item.optString("body")
                            )
                        )
                    }
                }
            } ?: emptyList()
        )
    }

    private fun stringList(array: JSONArray?): List<String> {
        if (array == null) return emptyList()
        return buildList {
            for (i in 0 until array.length()) {
                add(array.getString(i))
            }
        }
    }
}
