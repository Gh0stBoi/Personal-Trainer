package com.personaltrainer.data

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.personaltrainer.data.entities.*

/**
 * Room TypeConverters for all custom types that Room can't persist natively.
 */
class Converters {

    private val gson = Gson()

    // ── Enum converters ────────────────────────────────────────────────────

    @TypeConverter fun fromSex(v: Sex) = v.name
    @TypeConverter fun toSex(v: String) = Sex.valueOf(v)

    @TypeConverter fun fromGoal(v: Goal) = v.name
    @TypeConverter fun toGoal(v: String) = Goal.valueOf(v)

    @TypeConverter fun fromExperience(v: ExperienceLevel) = v.name
    @TypeConverter fun toExperience(v: String) = ExperienceLevel.valueOf(v)

    @TypeConverter fun fromStrictness(v: Strictness) = v.name
    @TypeConverter fun toStrictness(v: String) = Strictness.valueOf(v)

    @TypeConverter fun fromMuscleGroup(v: MuscleGroup) = v.name
    @TypeConverter fun toMuscleGroup(v: String) = MuscleGroup.valueOf(v)

    @TypeConverter fun fromMovementPattern(v: MovementPattern) = v.name
    @TypeConverter fun toMovementPattern(v: String) = MovementPattern.valueOf(v)

    @TypeConverter fun fromSessionType(v: SessionType) = v.name
    @TypeConverter fun toSessionType(v: String) = SessionType.valueOf(v)

    @TypeConverter fun fromSessionStatus(v: SessionStatus) = v.name
    @TypeConverter fun toSessionStatus(v: String) = SessionStatus.valueOf(v)

    @TypeConverter fun fromSessionPriority(v: SessionPriority) = v.name
    @TypeConverter fun toSessionPriority(v: String) = SessionPriority.valueOf(v)

    @TypeConverter fun fromSplitType(v: SplitType) = v.name
    @TypeConverter fun toSplitType(v: String) = SplitType.valueOf(v)

    @TypeConverter fun fromProgramStatus(v: ProgramStatus) = v.name
    @TypeConverter fun toProgramStatus(v: String) = ProgramStatus.valueOf(v)

    @TypeConverter fun fromCheckinKind(v: CheckinKind) = v.name
    @TypeConverter fun toCheckinKind(v: String) = CheckinKind.valueOf(v)

    @TypeConverter fun fromCheckinSource(v: CheckinSource) = v.name
    @TypeConverter fun toCheckinSource(v: String) = CheckinSource.valueOf(v)

    @TypeConverter fun fromMealSlot(v: MealSlot) = v.name
    @TypeConverter fun toMealSlot(v: String) = MealSlot.valueOf(v)

    @TypeConverter fun fromReminderKind(v: ReminderKind) = v.name
    @TypeConverter fun toReminderKind(v: String) = ReminderKind.valueOf(v)

    @TypeConverter fun fromReminderState(v: ReminderState) = v.name
    @TypeConverter fun toReminderState(v: String) = ReminderState.valueOf(v)

    // ── List<Enum> converters ──────────────────────────────────────────────

    @TypeConverter
    fun fromEquipmentList(value: List<Equipment>): String =
        gson.toJson(value.map { it.name })

    @TypeConverter
    fun toEquipmentList(value: String): List<Equipment> {
        val type = object : TypeToken<List<String>>() {}.type
        return gson.fromJson<List<String>>(value, type).map { Equipment.valueOf(it) }
    }

    @TypeConverter
    fun fromMuscleGroupList(value: List<MuscleGroup>): String =
        gson.toJson(value.map { it.name })

    @TypeConverter
    fun toMuscleGroupList(value: String): List<MuscleGroup> {
        val type = object : TypeToken<List<String>>() {}.type
        return gson.fromJson<List<String>>(value, type).map { MuscleGroup.valueOf(it) }
    }

    @TypeConverter
    fun fromDietPreferenceList(value: List<DietPreference>): String =
        gson.toJson(value.map { it.name })

    @TypeConverter
    fun toDietPreferenceList(value: String): List<DietPreference> {
        val type = object : TypeToken<List<String>>() {}.type
        return gson.fromJson<List<String>>(value, type).map { DietPreference.valueOf(it) }
    }

    // ── List<String> ──────────────────────────────────────────────────────

    @TypeConverter
    fun fromStringList(value: List<String>): String = gson.toJson(value)

    @TypeConverter
    fun toStringList(value: String): List<String> {
        val type = object : TypeToken<List<String>>() {}.type
        return gson.fromJson(value, type)
    }
}
