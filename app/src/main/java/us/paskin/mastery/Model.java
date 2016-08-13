package us.paskin.mastery;

import android.content.ContentValues;
import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.Nullable;
import android.text.format.DateUtils;

import com.google.protobuf.InvalidProtocolBufferException;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;

import us.paskin.mastery.Proto.Skill;

/**
 * Represents the data underlying the Mastery app.
 */
public class Model {
    // Constants for Skill.priority.
    public static final int MIN_PRIORITY = 1;
    public static final int MAX_PRIORITY = 10;
    public static final int DEFAULT_PRIORITY = MAX_PRIORITY;

    // Constants for Schedule.Slot.duration_in_secs.
    public static final int MIN_SLOT_DURATION_IN_SECS = (int) TimeUnit.MINUTES.toSeconds(1);
    public static final int MAX_SLOT_DURATION_IN_SECS = (int) TimeUnit.MINUTES.toSeconds(60);
    public static final int DEFAULT_SLOT_DURATION_IN_SECS = (int) TimeUnit.MINUTES.toSeconds(10);

    private SQLiteDatabase db = null;

    /**
     * Maps a group ID to the IDs of its parents.  If there are no parents, the value is null.
     */
    private HashMap<Long, Set<Long>> parentGroups = new HashMap<>();

    private static Model singleton;

    public static synchronized Model getInstance(Context context) {
        if (singleton == null) {
            singleton = new Model(context);
        }
        return singleton;
    }

    private Model(Context context) {
        init(new DatabaseOpenHelper(context));
    }

    /**
     * Constructor used for testing.
     */
    Model(Context context, String testDatabaseName) {
        init(new DatabaseOpenHelper(context, testDatabaseName));
    }

    /**
     * Initializes this object.
     *
     * @param openHelper
     */
    synchronized void init(DatabaseOpenHelper openHelper) {
        db = openHelper.getWritableDatabase();
        initCaches();
    }

    /**
     * Initializes in-memory caches from the database.
     */
    private synchronized void initCaches() {
        Cursor cursor = getSkillGroupList();
        Proto.SkillGroup skillGroup;
        for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
            try {
                skillGroup = Proto.SkillGroup.parseFrom(cursor.getBlob(1));
            } catch (InvalidProtocolBufferException x) {
                throw new InternalError("cannot parse protocol buffer");
            }
            if (skillGroup.getParentIdCount() > 0) {
                parentGroups.put(skillGroup.getId(), new TreeSet<>(skillGroup.getParentIdList()));
            } else {
                parentGroups.put(skillGroup.getId(), null);
            }
        }
        cursor.close();
    }

    synchronized boolean isValidSkillGroupId(long id) {
        return parentGroups.containsKey(id);
    }

    // Returns a cursor with two columns: SkillEntry._ID and SkillEntry.COLUMN_NAME_PROTO.
    public synchronized Cursor getSkillList() {
        String[] projection = {
                DatabaseContract.SkillEntry._ID,
                DatabaseContract.SkillEntry.COLUMN_NAME_PROTO};
        String sortOrder =
                DatabaseContract.SkillEntry.COLUMN_NAME_NAME + " ASC";
        return db.query(
                DatabaseContract.SkillEntry.TABLE_NAME,   // The table to query
                projection,                               // The columns to return
                null,                                     // The columns for the WHERE clause
                null,                                     // The values for the WHERE clause
                null,                                     // don't group the rows
                null,                                     // don't filter by row groups
                sortOrder
        );
    }

    public synchronized Skill getSkillById(long id) {
        String[] projection = {DatabaseContract.SkillEntry.COLUMN_NAME_PROTO};
        String selection = DatabaseContract.SkillEntry._ID + " = " + id;
        Cursor c = db.query(
                DatabaseContract.SkillEntry.TABLE_NAME,  // The table to query
                projection,                               // The columns to return
                selection,                                // The columns for the WHERE clause
                null,                                     // The values for the WHERE clause
                null,                                     // don't group the rows
                null,                                     // don't filter by row groups
                null                                      // The sort order
        );
        if (c.getCount() != 1) {
            throw new InternalError("Expected one row, got " + c.getCount());
        }
        if (c.getColumnCount() != 1) {
            throw new InternalError("Expected one column, got " + c.getColumnCount());
        }
        c.moveToFirst();
        try {
            Skill skill = Skill.parseFrom(c.getBlob(0));
            c.close();
            return skill;
        } catch (InvalidProtocolBufferException x) {
            throw new InternalError("failed to parse protocol message");
        }
    }

    private synchronized void validateSkill(Proto.Skill skill) {
        if (!skill.hasName() || skill.getName().isEmpty()) {
            throw new IllegalArgumentException("Skill missing name");
        }
        if (!skill.hasPriority()) {
            throw new IllegalArgumentException("Skill missing priority");
        }
        if (skill.getPriority() > MAX_PRIORITY || skill.getPriority() < MIN_PRIORITY) {
            throw new IllegalArgumentException("Skill priority out of range: " + skill.getPriority());
        }
        for (long skillGroupId : skill.getGroupIdList()) {
            if (!isValidSkillGroupId(skillGroupId)) {
                throw new IllegalArgumentException("Skill in invalid group");
            }
        }
        if (skill.hasDateLastPracticed() && skill.getDateLastPracticed() < 0) {
            throw new IllegalArgumentException("date last practiced is negative");
        }
    }

    /**
     * Returns true if there is a skill with the supplied name in the database.
     * No two skills are permitted to have the same name.
     */
    public synchronized boolean hasSkillWithName(String name) {
        String[] projection = {DatabaseContract.SkillEntry._ID};
        String selection = DatabaseContract.SkillEntry.COLUMN_NAME_NAME + " = ?";
        String selectionArgs[] = {name};
        Cursor c = db.query(
                DatabaseContract.SkillEntry.TABLE_NAME,  // The table to query
                projection,                               // The columns to return
                selection,                                // The columns for the WHERE clause
                selectionArgs,                            // The values for the WHERE clause
                null,                                     // don't group the rows
                null,                                     // don't filter by row groups
                null                                      // The sort order
        );
        int count = c.getCount();
        c.close();
        return count > 0;
    }

    // The ID of the skill is returned.
    public synchronized long addSkill(Skill skill) {
        validateSkill(skill);
        ContentValues values = new ContentValues();
        values.put(DatabaseContract.SkillEntry.COLUMN_NAME_NAME, skill.getName());
        values.put(DatabaseContract.SkillEntry.COLUMN_NAME_PROTO, skill.toByteArray());
        return db.insert(DatabaseContract.SkillEntry.TABLE_NAME, null, values);
    }

    /**
     * Updates a skill in the database.
     *
     * @param id    the ID of the skill to update.  Throws IllegalArgumentException if this is invalid.
     * @param skill the new skill data.
     */
    public synchronized void updateSkill(long id, Skill skill) throws IllegalArgumentException {
        validateSkill(skill);
        ContentValues values = new ContentValues();
        values.put(DatabaseContract.SkillEntry.COLUMN_NAME_NAME, skill.getName());
        values.put(DatabaseContract.SkillEntry.COLUMN_NAME_PROTO, skill.toByteArray());
        String selection = DatabaseContract.SkillEntry._ID + " = " + id;
        final int numUpdated = db.update(DatabaseContract.SkillEntry.TABLE_NAME,
                values, selection, null);
        switch (numUpdated) {
            case 1:
                return;
            case 0:
                throw new IllegalArgumentException("invalid id: " + id);
            default:
                throw new InternalError("id has multiple records");
        }
    }

    /**
     * Updates the model to reflect that the skill has been practiced for an additional amount of time.
     */
    public synchronized void addPracticeSecondsToSkill(int seconds, long skillId) {
        Skill.Builder skillBuilder = getSkillById(skillId).toBuilder();
        final long curDateInSecs = TimeUnit.MILLISECONDS.toSeconds(new Date().getTime());
        skillBuilder.setSecondsPracticed(skillBuilder.getSecondsPracticed() + seconds)
                .setDateLastPracticed(curDateInSecs);
        // Update est_seconds_practiced_100_days.  If it's been less than 100 days since the last
        // practice, then we assume the practice time was distributed uniformly across the 100 day
        // period leading up to the last practice.
        if (skillBuilder.hasDateLastPracticed()) {
            final long prevEst = skillBuilder.getEstSecondsPracticed100Days();
            final long secsIn100Days = TimeUnit.DAYS.toSeconds(100);
            final long secsSinceLastPractice = curDateInSecs - skillBuilder.getDateLastPracticed();
            final double forgetFrac = Math.max(1.0, (double) secsSinceLastPractice / (double) secsIn100Days);
            skillBuilder.setEstSecondsPracticed100Days(seconds + (long) ((1.0 - forgetFrac) * prevEst));
        } else {
            skillBuilder.setEstSecondsPracticed100Days(seconds);
        }
        updateSkill(skillId, skillBuilder.build());
    }

    /**
     * Deletes a skill from the database.
     *
     * @param id the ID of the skill to delete.  Throws IllegalArgumentException if this is invalid.
     */
    public synchronized void deleteSkill(long id) throws IllegalArgumentException {
        String selection = DatabaseContract.SkillEntry._ID + " = " + id;
        final int numDeleted = db.delete(DatabaseContract.SkillEntry.TABLE_NAME, selection, null);
        switch (numDeleted) {
            case 1:
                return;
            case 0:
                throw new IllegalArgumentException("invalid id: " + id);
            default:
                throw new InternalError("id had multiple records");
        }
    }

    /**
     * Returns a localized string like "Last practiced 4 days ago".
     *
     * @param skill
     * @param resources
     * @return
     */
    public static String getLastPracticedText(Skill skill, Resources resources) {
        if (!skill.hasDateLastPracticed()) {
            return resources.getString(R.string.last_practiced_never);
        }
        final long millis = new Date().getTime()
                - new Date(TimeUnit.SECONDS.toMillis(skill.getDateLastPracticed())).getTime();
        final long diffInDays = TimeUnit.MILLISECONDS.toDays(millis);
        if (diffInDays == 0) {
            return resources.getString(R.string.last_practiced_recently);
        }
        final long diffInWeeks = diffInDays / 7;
        if (diffInWeeks < 14) {
            return resources.getQuantityString(R.plurals.last_practiced_days, (int) diffInDays, (int) diffInDays);
        }
        final long diffInMonths = diffInDays / 30;
        if (diffInMonths < 3) {
            return resources.getQuantityString(R.plurals.last_practiced_weeks, (int) diffInWeeks, (int) diffInWeeks);
        }
        return resources.getQuantityString(R.plurals.last_practiced_months, (int) diffInMonths, (int) diffInMonths);
    }

    /**
     * Returns a localized string like "Practiced for 4 hours" or null if it has not been practiced.
     *
     * @param skill
     * @param resources
     * @return
     */
    public static
    @Nullable
    String getDurationPracticedText(Skill skill, Resources resources) {
        if (!skill.hasSecondsPracticed()) {
            return null;
        } else {
            return String.format(resources.getString(R.string.duration_practiced_text),
                    DateUtils.formatElapsedTime(skill.getSecondsPracticed()));
        }
    }

    // Returns a cursor with two columns: SkillGroupEntry._ID and SkillGroupEntry.COLUMN_NAME_PROTO.
    public synchronized Cursor getSkillGroupList() {
        String[] projection = {
                DatabaseContract.SkillGroupEntry._ID,
                DatabaseContract.SkillGroupEntry.COLUMN_NAME_PROTO};
        String sortOrder =
                DatabaseContract.SkillGroupEntry.COLUMN_NAME_NAME + " ASC";
        return db.query(
                DatabaseContract.SkillGroupEntry.TABLE_NAME,   // The table to query
                projection,                               // The columns to return
                null,                                     // The columns for the WHERE clause
                null,                                     // The values for the WHERE clause
                null,                                     // don't group the rows
                null,                                     // don't filter by row groups
                sortOrder
        );
    }

    public synchronized Proto.SkillGroup getSkillGroupById(long id) {
        String[] projection = {DatabaseContract.SkillGroupEntry.COLUMN_NAME_PROTO};
        String selection = DatabaseContract.SkillGroupEntry._ID + " = " + id;
        Cursor c = db.query(
                DatabaseContract.SkillGroupEntry.TABLE_NAME,  // The table to query
                projection,                               // The columns to return
                selection,                                // The columns for the WHERE clause
                null,                                     // The values for the WHERE clause
                null,                                     // don't group the rows
                null,                                     // don't filter by row groups
                null                                      // The sort order
        );
        if (c.getCount() != 1) {
            throw new InternalError("Expected one row, got " + c.getCount());
        }
        if (c.getColumnCount() != 1) {
            throw new InternalError("Expected one column, got " + c.getColumnCount());
        }
        c.moveToFirst();
        try {
            Proto.SkillGroup skillGroup = Proto.SkillGroup.parseFrom(c.getBlob(0));
            c.close();
            return skillGroup;
        } catch (InvalidProtocolBufferException x) {
            throw new InternalError("failed to parse protocol message");
        }
    }

    private synchronized void validateSkillGroup(Proto.SkillGroup skillGroup) {
        if (!skillGroup.hasId()) {
            throw new IllegalArgumentException("Skill group missing ID");
        }
        if (!skillGroup.hasName() || skillGroup.getName().isEmpty()) {
            throw new IllegalArgumentException("Skill group missing name");
        }
        for (long skillGroupId : skillGroup.getParentIdList()) {
            if (!isValidSkillGroupId(skillGroupId)) {
                throw new IllegalArgumentException("Skill group has invalid parent");
            }
        }
    }

    private synchronized void updateSkillGroupParents(Proto.SkillGroup skillGroup) {
        if (skillGroup.getParentIdCount() > 0)
            parentGroups.put(skillGroup.getId(), new TreeSet<>(skillGroup.getParentIdList()));
        else
            parentGroups.put(skillGroup.getId(), null);
    }

    /**
     * Returns true if there is a skill group with the supplied name in the database.
     * No two skill groups are permitted to have the same name.
     */
    public synchronized boolean hasSkillGroupWithName(String name) {
        String[] projection = {DatabaseContract.SkillGroupEntry._ID};
        String selection = DatabaseContract.SkillGroupEntry.COLUMN_NAME_NAME + " = ?";
        String selectionArgs[] = {name};
        Cursor c = db.query(
                DatabaseContract.SkillGroupEntry.TABLE_NAME,  // The table to query
                projection,                               // The columns to return
                selection,                                // The columns for the WHERE clause
                selectionArgs,                            // The values for the WHERE clause
                null,                                     // don't group the rows
                null,                                     // don't filter by row groups
                null                                      // The sort order
        );
        int count = c.getCount();
        c.close();
        return count > 0;
    }

    public synchronized void addSkillGroup(Proto.SkillGroup skillGroup) {
        validateSkillGroup(skillGroup);
        ContentValues values = new ContentValues();
        values.put(DatabaseContract.SkillGroupEntry._ID, skillGroup.getId());
        values.put(DatabaseContract.SkillGroupEntry.COLUMN_NAME_NAME, skillGroup.getName());
        values.put(DatabaseContract.SkillGroupEntry.COLUMN_NAME_PROTO, skillGroup.toByteArray());
        final long id = db.insert(DatabaseContract.SkillGroupEntry.TABLE_NAME, null, values);
        if (id != skillGroup.getId()) {
            throw new InternalError("ID mismatch");
        }
        updateSkillGroupParents(skillGroup);
    }


    /**
     * Updates a skill group in the database.
     *
     * @param skillGroup the new skill data.
     */
    public synchronized void updateSkillGroup(Proto.SkillGroup skillGroup) {
        validateSkillGroup(skillGroup);
        ContentValues values = new ContentValues();
        values.put(DatabaseContract.SkillGroupEntry.COLUMN_NAME_NAME, skillGroup.getName());
        values.put(DatabaseContract.SkillGroupEntry.COLUMN_NAME_PROTO, skillGroup.toByteArray());
        String selection = DatabaseContract.SkillEntry._ID + " = " + skillGroup.getId();
        final int numUpdated = db.update(DatabaseContract.SkillGroupEntry.TABLE_NAME,
                values, selection, null);
        switch (numUpdated) {
            case 1:
                updateSkillGroupParents(skillGroup);
                return;
            case 0:
                throw new IllegalArgumentException("invalid id: " + skillGroup.getId());
            default:
                throw new InternalError("id has multiple records");
        }
    }

    /**
     * All references to the group ID prevId are replaced by references to newId.  If newId is -1,
     * then references to prevId are removed.  Throws IllegalArgumentException if
     * either ID is invalid (except for newId == -1).
     * prevId may not be an ancestor of newId.
     */
    public synchronized void replaceSkillGroup(final long prevId, final long newId) throws IllegalArgumentException {
        if (!isValidSkillGroupId(prevId)) {
            throw new IllegalArgumentException("invalid skill group ID");
        }
        if (newId != -1) {
            if (prevId == newId) return;
            if (!isValidSkillGroupId(newId)) {
                throw new IllegalArgumentException("invalid skill group ID");
            }
            if (isAncestorOf(prevId, newId)) {
                throw new IllegalArgumentException("cannot replace skill group with a descendant");
            }
        }
        try {
            // Go through the skill table, performing the replacement in parent groups.
            Cursor cursor = getSkillList();
            for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                final long skillId = cursor.getLong(0);
                Skill.Builder skillBuilder = Skill.parseFrom(cursor.getBlob(1)).toBuilder();
                HashSet<Long> groups = new HashSet<>(skillBuilder.getGroupIdList());
                if (groups.remove(prevId) && newId != -1) {
                    groups.add(newId);
                }
                skillBuilder.clearGroupId().addAllGroupId(groups);
                updateSkill(skillId, skillBuilder.build());
            }
            // Go through the skill group table, performing the replacement in parent groups.
            cursor = getSkillGroupList();
            for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                Proto.SkillGroup.Builder skillGroupBuilder = Proto.SkillGroup.parseFrom(cursor.getBlob(1)).toBuilder();
                HashSet<Long> parents = new HashSet<>(skillGroupBuilder.getParentIdList());
                if (parents.remove(prevId) && newId != -1) {
                    parents.add(newId);
                }
                skillGroupBuilder.clearParentId().addAllParentId(parents);
                updateSkillGroup(skillGroupBuilder.build());
            }
            // Go through the schedule table, replacing the group in slots.
            cursor = getScheduleList();
            for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                final long scheduleId = cursor.getLong(0);
                Proto.Schedule schedule = Proto.Schedule.parseFrom(cursor.getBlob(1));
                List<Proto.Schedule.Slot> slots = schedule.getSlotList();
                List<Proto.Schedule.Slot> updatedSlots = new LinkedList<>();
                for (Proto.Schedule.Slot slot : slots) {
                    Proto.Schedule.Slot.Builder slotBuilder = slot.toBuilder();
                    if (slotBuilder.getGroupId() == prevId) {
                        if (newId != -1)
                            slotBuilder.setGroupId(newId);
                        else
                            slotBuilder.clearGroupId();
                    }
                    updatedSlots.add(slotBuilder.build());
                }
                Proto.Schedule.Builder scheduleBuilder = schedule.toBuilder().clearSlot().addAllSlot(updatedSlots);
                updateSchedule(scheduleId, scheduleBuilder.build());
            }
        } catch (InvalidProtocolBufferException x) {
            throw new InternalError("cannot parse protocol buffer");
        }
    }

    /**
     * Deletes a skill group from the database.  Throws IllegalArgumentException if the ID is invalid.
     */

    public synchronized void deleteSkillGroup(long idToDelete) throws IllegalArgumentException {
        replaceSkillGroup(idToDelete, -1);
        // Delete the old ID.
        String selection = DatabaseContract.SkillGroupEntry._ID + " = " + idToDelete;
        final int numDeleted = db.delete(DatabaseContract.SkillGroupEntry.TABLE_NAME, selection, null);
        switch (numDeleted) {
            case 1:
                return;
            case 0:
                throw new IllegalArgumentException("invalid id: " + idToDelete);
            default:
                throw new InternalError("id had multiple records");
        }
    }

    /**
     * Returns a set of ancestor group IDs, or null if there are none.
     *
     * @param groupId
     * @return null if there are no ancestors
     */
    public synchronized
    @Nullable
    Set<Long> getAncestorGroups(long groupId) {
        Set<Long> parents = parentGroups.get(groupId);
        if (parents == null) return null;
        LinkedList<Long> toProcess = new LinkedList<>(parents);
        Set<Long> ancestors = new HashSet<>();
        while (!toProcess.isEmpty()) {
            final long ancestor = toProcess.removeFirst();
            if (ancestor == groupId) throw new InternalError("parent cycle");
            if (ancestors.contains(ancestor)) continue;
            ancestors.add(ancestor);
            Set<Long> ancestorParents = parentGroups.get(ancestor);
            if (ancestorParents != null) toProcess.addAll(ancestorParents);
        }
        return ancestors;
    }

    /**
     * Returns true if ancestorGroupId is an ancestor of groupId.
     */
    public synchronized boolean isAncestorOf(long ancestorGroupId, long groupId) {
        Set<Long> parents = parentGroups.get(groupId);
        if (parents == null) return false;
        LinkedList<Long> toProcess = new LinkedList<>(parents);
        Set<Long> ancestors = new HashSet<>();
        while (!toProcess.isEmpty()) {
            final long ancestor = toProcess.removeFirst();
            if (ancestor == groupId) throw new InternalError("parent cycle");
            if (ancestor == ancestorGroupId) return true;
            if (ancestors.contains(ancestor)) continue;
            ancestors.add(ancestor);
            Set<Long> ancestorParents = parentGroups.get(ancestor);
            if (ancestorParents != null) toProcess.addAll(ancestorParents);
        }
        return false;
    }

    /**
     * Returns a list of the IDs of all skill groups this skill is in, directly or indirectly.
     */
    public synchronized Set<Long> getAllSkillGroupIds(Skill skill) {
        Set<Long> result = new TreeSet<>();
        for (long directGroupId : skill.getGroupIdList()) {
            result.add(directGroupId);
            Set<Long> ancestors = getAncestorGroups(directGroupId);
            if (ancestors != null) {
                Set<Long> ancestorGroups = getAncestorGroups(directGroupId);
                if (ancestorGroups != null) {
                    result.addAll(ancestorGroups);
                }
            }
        }
        return result;
    }

    /**
     * Removes all data, returning it to a newly-initialized state.
     */
    public synchronized void clearAllData() {
        db.delete(DatabaseContract.SkillEntry.TABLE_NAME, null, null);
        db.delete(DatabaseContract.SkillGroupEntry.TABLE_NAME, null, null);
        db.delete(DatabaseContract.ScheduleEntry.TABLE_NAME, null, null);
    }

    /**
     * Removes all existing data and re-initializes with fake data.
     */
    public synchronized void initWithFakeData() {
        clearAllData();

        // Add skill groups.  Note these must be done in reverse dependency order.
        addSkillGroup(Proto.SkillGroup.newBuilder()
                .setId(0)
                .setName("Warm-ups")
                .build());
        addSkillGroup(Proto.SkillGroup.newBuilder()
                .setId(-1)
                .setName("Scales")
                .addParentId(0)
                .build());
        addSkillGroup(Proto.SkillGroup.newBuilder()
                .setId(1)
                .setName("Etudes")
                .build());

        // Add skills
        final long dateInSecs = TimeUnit.MILLISECONDS.toSeconds(new Date().getTime());
        addSkill(Skill.newBuilder()
                .setName("Carcassi Op. 60 No. 7")
                .setDateLastPracticed(dateInSecs - TimeUnit.DAYS.toSeconds(2))
                .setPriority(6)
                .addGroupId(1)
                .setSecondsPracticed(345)
                .build());
        addSkill(Skill.newBuilder()
                .setName("Shearer Scale p. 253")
                .setDateLastPracticed(dateInSecs - TimeUnit.HOURS.toSeconds(7))
                .setPriority(2)
                .addGroupId(-1)
                .setSecondsPracticed(34)
                .build());

        // Add a schedule
        addSchedule(Proto.Schedule.newBuilder()
                .setName("Weekday")
                .addSlot(Proto.Schedule.Slot.newBuilder().setGroupId(0).setDurationInSecs(300))
                .addSlot(Proto.Schedule.Slot.newBuilder().setGroupId(1).setDurationInSecs(1200))
                .build());
    }

    // Returns a cursor with two columns: ScheduleEntry._ID and ScheduleEntry.COLUMN_NAME_PROTO.
    public synchronized Cursor getScheduleList() {
        String[] projection = {
                DatabaseContract.ScheduleEntry._ID,
                DatabaseContract.ScheduleEntry.COLUMN_NAME_PROTO};
        String sortOrder =
                DatabaseContract.ScheduleEntry.COLUMN_NAME_NAME + " ASC";
        return db.query(
                DatabaseContract.ScheduleEntry.TABLE_NAME,   // The table to query
                projection,                               // The columns to return
                null,                                     // The columns for the WHERE clause
                null,                                     // The values for the WHERE clause
                null,                                     // don't group the rows
                null,                                     // don't filter by row groups
                sortOrder
        );
    }

    public synchronized Proto.Schedule getScheduleById(long id) {
        String[] projection = {DatabaseContract.ScheduleEntry.COLUMN_NAME_PROTO};
        String selection = DatabaseContract.ScheduleEntry._ID + " = " + id;
        Cursor c = db.query(
                DatabaseContract.ScheduleEntry.TABLE_NAME,  // The table to query
                projection,                               // The columns to return
                selection,                                // The columns for the WHERE clause
                null,                                     // The values for the WHERE clause
                null,                                     // don't group the rows
                null,                                     // don't filter by row groups
                null                                      // The sort order
        );
        if (c.getCount() != 1) {
            throw new InternalError("Expected one row, got " + c.getCount());
        }
        if (c.getColumnCount() != 1) {
            throw new InternalError("Expected one column, got " + c.getColumnCount());
        }
        c.moveToFirst();
        try {
            Proto.Schedule schedule = Proto.Schedule.parseFrom(c.getBlob(0));
            c.close();
            return schedule;
        } catch (InvalidProtocolBufferException x) {
            throw new InternalError("failed to parse protocol message");
        }
    }

    private synchronized void validateSchedule(Proto.Schedule schedule) {
        if (!schedule.hasName() || schedule.getName().isEmpty()) {
            throw new IllegalArgumentException("Schedule missing name");
        }
        if (schedule.getSlotCount() == 0) {
            throw new IllegalArgumentException("empty schedule");
        }
        for (Proto.Schedule.Slot slot : schedule.getSlotList()) {
            if (!slot.hasDurationInSecs() || slot.getDurationInSecs() <= 0) {
                throw new IllegalArgumentException("non-positive schedule_slot duration");
            }
            if (slot.hasGroupId() && !isValidSkillGroupId(slot.getGroupId())) {
                throw new IllegalArgumentException("schedule_slot has invalid group id");
            }
        }
    }

    /**
     * Returns true if there is a schedule with the supplied name in the database.
     * No two schedules are permitted to have the same name.
     */
    public synchronized boolean hasScheduleWithName(String name) {
        String[] projection = {DatabaseContract.ScheduleEntry._ID};
        String selection = DatabaseContract.ScheduleEntry.COLUMN_NAME_NAME + " = ?";
        String selectionArgs[] = {name};
        Cursor c = db.query(
                DatabaseContract.ScheduleEntry.TABLE_NAME,  // The table to query
                projection,                               // The columns to return
                selection,                                // The columns for the WHERE clause
                selectionArgs,                            // The values for the WHERE clause
                null,                                     // don't group the rows
                null,                                     // don't filter by row groups
                null                                      // The sort order
        );
        int count = c.getCount();
        c.close();
        return count > 0;
    }

    // The ID of the skill is returned.
    public synchronized long addSchedule(Proto.Schedule schedule) {
        validateSchedule(schedule);
        ContentValues values = new ContentValues();
        values.put(DatabaseContract.ScheduleEntry.COLUMN_NAME_NAME, schedule.getName());
        values.put(DatabaseContract.ScheduleEntry.COLUMN_NAME_PROTO, schedule.toByteArray());
        return db.insert(DatabaseContract.ScheduleEntry.TABLE_NAME, null, values);
    }

    /**
     * Updates a schedule in the database.
     *
     * @param id       the ID of the schedule to update.  Throws IllegalArgumentException if this is invalid.
     * @param schedule the new schedule data.
     */
    public synchronized void updateSchedule(long id, Proto.Schedule schedule) throws IllegalArgumentException {
        validateSchedule(schedule);
        ContentValues values = new ContentValues();
        values.put(DatabaseContract.ScheduleEntry.COLUMN_NAME_NAME, schedule.getName());
        values.put(DatabaseContract.ScheduleEntry.COLUMN_NAME_PROTO, schedule.toByteArray());
        String selection = DatabaseContract.ScheduleEntry._ID + " = " + id;
        final int numUpdated = db.update(DatabaseContract.ScheduleEntry.TABLE_NAME,
                values, selection, null);
        switch (numUpdated) {
            case 1:
                return;
            case 0:
                throw new IllegalArgumentException("invalid id: " + id);
            default:
                throw new InternalError("id has multiple records");
        }
    }

    /**
     * Deletes a schedule from the database.
     *
     * @param id the ID of the schedule to delete.  Throws IllegalArgumentException if this is invalid.
     */
    public synchronized void deleteSchedule(long id) throws IllegalArgumentException {
        String selection = DatabaseContract.ScheduleEntry._ID + " = " + id;
        final int numDeleted = db.delete(DatabaseContract.ScheduleEntry.TABLE_NAME, selection, null);
        switch (numDeleted) {
            case 1:
                return;
            case 0:
                throw new IllegalArgumentException("invalid id: " + id);
            default:
                throw new InternalError("id had multiple records");
        }
    }
}
