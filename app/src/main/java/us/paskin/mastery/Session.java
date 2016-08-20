package us.paskin.mastery;

import android.database.Cursor;

import com.google.protobuf.InvalidProtocolBufferException;

import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * Represents a concrete practice session.
 */
public class Session {

    /**
     * Generates a new session for the provided schedule.  Note that a schedule_slot may not be filled
     * if no skills were in its associated group.  Also, the same skill may be assigned to multiple
     * slots.
     */
    public static long[] sampleSession(Proto.Schedule schedule,
                                       Model model,
                                       float stalenessWeight) {
        final int numSlots = schedule.getSlotCount();
        long session[] = new long[numSlots];
        for (int i = 0; i < numSlots; ++i) session[i] = -1;
        // Scan through the skills, sampling as we go.
        Cursor cursor = model.getSkillList();
        float sumWeight[] = new float[numSlots];
        for (int i = 0; i < numSlots; ++i) {
            sumWeight[i] = 0.0f;
        }
        Random random = new Random();
        for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
            final long skillId = cursor.getLong(0);
            Proto.Skill skill;
            try {
                skill = Proto.Skill.parseFrom(cursor.getBlob(1));
            } catch (InvalidProtocolBufferException x) {
                throw new InternalError("cannot parse protocol buffer");
            }
            final float weight = weight(skill, stalenessWeight);
            for (int slotIndex = 0; slotIndex < numSlots; ++slotIndex) {
                if (!slotCanBeFilledBy(schedule.getSlot(slotIndex), skill, model)) continue;
                boolean selected = false;
                if (session[slotIndex] == -1 || random.nextFloat() < (weight / sumWeight[slotIndex])) {
                    session[slotIndex] = skillId;
                    selected = true;
                }
                sumWeight[slotIndex] += weight;
                if (selected) break;  // don't allow the same skill in two slots
            }
        }
        return session;
    }

    /**
     * Returns true if the supplied skill can be placed in this schedule_slot.
     */
    public static boolean slotCanBeFilledBy(Proto.Schedule.Slot slot, Proto.Skill skill,
                                            Model model) {
        if (!slot.hasGroupId()) return true;
        for (long skillGroupId : skill.getGroupIdList()) {
            if ((slot.getGroupId() == skillGroupId) ||
                    model.isAncestorOf(slot.getGroupId(), skillGroupId)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns the weight of this skill.  The probability the skill is sampled is proportional to weight.
     */
    private static float weight(Proto.Skill skill, float stalenessWeight) {
        // Scale the priority to [0, 1].
        float priorityZeroOne = ((float) (skill.getPriority()) / ((float) (Model.MAX_PRIORITY)));
        // Map the estimated amount of practice time in the past 100 days to a staleness value in [0, 1].
        final long estHoursPracticed = TimeUnit.SECONDS.toHours(skill.getEstSecondsPracticed100Days());
        final long halfPoint = 5;
        final double stalenessZeroOne = 1.0 - ((double) estHoursPracticed / (double) (estHoursPracticed + halfPoint));
        // Compute a weighted sum.
        final float priorityWeight = 1.0f - stalenessWeight;
        return stalenessWeight * (float) stalenessZeroOne + priorityWeight * priorityZeroOne;
    }
}
