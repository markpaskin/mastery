package us.paskin.mastery;

import android.app.ProgressDialog;
import android.database.Cursor;

import com.google.protobuf.InvalidProtocolBufferException;

import java.lang.reflect.Array;
import java.sql.Time;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * Represents a concrete practice session.
 */
public class Session {
    private Proto.Schedule schedule;
    private Model model;
    private ArrayList<Slot> slots;

    /**
     * Generates a new session for the provided schedule.  Note that a schedule_slot may not be filled
     * if no skills were in its associated group.  Also, the same skill may be assigned to multiple
     * slots.
     */
    public static Session sampleSession(Proto.Schedule schedule,
                                        Model model,
                                        float stalenessWeight) {
        Session session = new Session(schedule, model);
        HashSet<Long> sampledSkillIds = new HashSet<Long>();
        // Scan through the skills, sampling as we go.
        Cursor cursor = model.getSkillList();
        final int numSlots = session.slots.size();
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
                Slot slot = session.slots.get(slotIndex);
                if (!slot.canBeFilledBy(skill)) continue;
                if (!slot.filled() || random.nextFloat() < (weight / sumWeight[slotIndex])) {
                    slot.fillWith(skillId, skill);
                }
            }
        }
        return session;
    }

    /**
     * @return the schedule from which this session was sampled
     */
    public Proto.Schedule getSchedule() {
        return schedule;
    }

    /**
     * @return a list of slots, parallel to those in the schedule
     */
    public ArrayList<Slot> getSlotList() {
        return slots;
    }

    /**
     * Reloads the skills from the model.  Use this if the skills may have been updated.
     */
    public void refill() {
        for (Slot slot : slots) {
            if (slot.filled())
                slot.fillWith(slot.getSkillId(), model.getSkillById(slot.getSkillId()));
        }
    }

    /**
     * Represents an instantiated schedule_slot in a session.
     */
    public class Slot {
        private long skillId = -1;
        private Proto.Skill skill = null;
        final Proto.Schedule.Slot scheduleSlot;

        public Slot(Proto.Schedule.Slot scheduleSlot) {
            this.scheduleSlot = scheduleSlot;
        }

        /**
         * Fills this schedule_slot with the supplied skill.
         */
        public void fillWith(long skillId, Proto.Skill skill) {
            this.skillId = skillId;
            this.skill = skill;
        }

        /**
         * @return true if this schedule_slot is filled.
         */
        public boolean filled() {
            return skill != null;
        }

        public Proto.Schedule.Slot getScheduleSlot() {
            return scheduleSlot;
        }

        public long getSkillId() {
            return skillId;
        }

        public Proto.Skill getSkill() {
            return skill;
        }

        /**
         * Returns true if the supplied skill can be placed in this schedule_slot.
         */
        public boolean canBeFilledBy(Proto.Skill skill) {
            for (long skillGroupId : skill.getGroupIdList()) {
                if ((this.scheduleSlot.getGroupId() == skillGroupId) ||
                        model.isAncestorOf(scheduleSlot.getGroupId(), skillGroupId)) {
                    return true;
                }
            }
            return false;
        }
    }

    private Session(Proto.Schedule schedule,
                    Model model) {
        this.schedule = schedule;
        this.model = model;
        slots = new ArrayList<Slot>();
        for (Proto.Schedule.Slot slot : schedule.getSlotList()) {
            slots.add(new Slot(slot));
        }
    }

    /**
     * Returns the weight of this skill.  The probability the skill is sampled is proportional to weight.
     */
    private static float weight(Proto.Skill skill, float stalenessWeight) {
        // Scale the priority to [0, 1].
        float priorityZeroOne = ((float) (skill.getPriority()) / ((float) (Model.MAX_PRIORITY)));

        // Map the time from last practice to [0, 1].
        final long curDateSecs = TimeUnit.MILLISECONDS.toSeconds(new Date().getTime());
        final long secondsSincePracticed = Math.max(0L, curDateSecs - skill.getDateLastPracticed());
        final long halfPoint = TimeUnit.DAYS.toSeconds(100);
        final double stalenessZeroOne = ((double) secondsSincePracticed / (double) (secondsSincePracticed + halfPoint));
        final float priorityWeight = 1.0f - stalenessWeight;

        return stalenessWeight * (float) stalenessZeroOne + priorityWeight * priorityZeroOne;
    }
}
