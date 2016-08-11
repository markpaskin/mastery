package us.paskin.mastery;

import android.app.ProgressDialog;
import android.database.Cursor;

import com.google.protobuf.InvalidProtocolBufferException;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Random;

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
                                        Model model) {
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
            final float weight = weight(skill);
            for (int slotIndex = 0; slotIndex < numSlots; ++slotIndex) {
                Slot slot = session.slots.get(slotIndex);
                if (!slot.canBeFilledBy(skill)) continue;
                if (!slot.filled() || random.nextFloat() < (weight / sumWeight[slotIndex])) {
                    slot.fillWith(skillId);
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
     * Represents an instantiated schedule_slot in a session.
     */
    public class Slot {
        private long skillId = -1;
        private boolean hasSkill = false;
        final Proto.Schedule.Slot scheduleSlot;

        public Slot(Proto.Schedule.Slot scheduleSlot) {
            this.scheduleSlot = scheduleSlot;
        }

        /**
         * Fills this schedule_slot with the supplied skill.
         */
        public void fillWith(long skillId) {
            this.skillId = skillId;
            hasSkill = true;
        }

        /**
         * @return true if this schedule_slot is filled.
         */
        public boolean filled() {
            return hasSkill;
        }

        public Proto.Schedule.Slot getScheduleSlot() {
            return scheduleSlot;
        }

        public long getSkillId() {
            return skillId;
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
    private static float weight(Proto.Skill skill) {
        return (float) skill.getPriority();  // TODO: add in time
    }
}
