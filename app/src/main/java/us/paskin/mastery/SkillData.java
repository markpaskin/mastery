package us.paskin.mastery;

import android.content.ContentValues;
import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.google.protobuf.InvalidProtocolBufferException;

import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import us.paskin.mastery.Proto.Skill;

/**
 *
 */
public class SkillData {

    public static final int MIN_PRIORITY = 1;
    public static final int MAX_PRIORITY = 10;

    SQLiteDatabase db = null;

    private static SkillData singleton;

    public static synchronized SkillData getInstance(Context context) {
        if (singleton == null) {
            singleton = new SkillData(context);
        }
        return singleton;
    }

    private SkillData(Context context) {
        DatabaseOpenHelper openHelper = DatabaseOpenHelper.getInstance(context);
        db = openHelper.getWritableDatabase();
        addFakeData();
    }

    // Returns a cursor with two columns: SkillEntry._ID and SkillEntry.COLUMN_NAME_NAME.
    public Cursor getSkillList() {
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

    public Skill getSkillById(long id) {
        System.out.println("getSkillById " + id);
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

    // The ID of the skill is returned.
    public long addSkill(Skill skill) {
        ContentValues values = new ContentValues();
        values.put(DatabaseContract.SkillEntry.COLUMN_NAME_NAME, skill.getName());
        values.put(DatabaseContract.SkillEntry.COLUMN_NAME_PROTO, skill.toByteArray());
        long id = db.insert(DatabaseContract.SkillEntry.TABLE_NAME, null, values);
        System.out.println("added id: " + id);
        return id;
    }

    public boolean updateSkill(long id, Skill skill) {
        ContentValues values = new ContentValues();
        values.put(DatabaseContract.SkillEntry.COLUMN_NAME_NAME, skill.getName());
        values.put(DatabaseContract.SkillEntry.COLUMN_NAME_PROTO, skill.toByteArray());
        String selection = DatabaseContract.SkillEntry._ID + " = " + id;
        final boolean success = 1 == db.update(DatabaseContract.SkillEntry.TABLE_NAME,
                values, selection, null);
        return success;
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
        final long millis = new Date().getTime() - new Date(skill.getDateLastPracticed()).getTime();
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

    public void addFakeData() {
        addSkill(Skill.newBuilder()
                .setName("Carcassi Op. 60 No. 7")
                .setDateLastPracticed(new Date().getTime() - TimeUnit.DAYS.toMillis(2))
                .setPriority(6)
                .build());
        addSkill(Skill.newBuilder()
                .setName("Shearer Scale p. 253")
                .setDateLastPracticed(new Date().getTime() - TimeUnit.HOURS.toMillis(7))
                .setPriority(2)
                .build());
    }
}
