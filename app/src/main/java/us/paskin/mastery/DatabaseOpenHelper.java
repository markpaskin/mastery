package us.paskin.mastery;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by baq on 8/7/16.
 */
public class DatabaseOpenHelper extends SQLiteOpenHelper {
    // If you change the database schema, you must increment the database version.
    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "Mastery.db";

    // Generic constants
    private static final String TEXT_TYPE = " TEXT";
    private static final String BLOB_TYPE = " BLOB";
    private static final String UNIQUE = " NOT NULL UNIQUE";
    private static final String COMMA_SEP = ",";

    // Skills DB statements
    private static final String SQL_CREATE_SKILLS =
            "CREATE TABLE " + DatabaseContract.SkillEntry.TABLE_NAME + " (" +
                    DatabaseContract.SkillEntry._ID + " INTEGER PRIMARY KEY" +
                    COMMA_SEP + DatabaseContract.SkillEntry.COLUMN_NAME_NAME + TEXT_TYPE + UNIQUE +
                    COMMA_SEP + DatabaseContract.SkillEntry.COLUMN_NAME_PROTO + BLOB_TYPE +
                    " )";
    private static final String SQL_DROP_SKILLS =
            "DROP TABLE IF EXISTS " + DatabaseContract.SkillEntry.TABLE_NAME;


    // Skill groups DB statements
    private static final String SQL_CREATE_SKILL_GROUPS =
            "CREATE TABLE " + DatabaseContract.SkillGroupEntry.TABLE_NAME + " (" +
                    DatabaseContract.SkillGroupEntry._ID + " INTEGER PRIMARY KEY" +
                    COMMA_SEP + DatabaseContract.SkillGroupEntry.COLUMN_NAME_NAME + TEXT_TYPE + UNIQUE +
                    COMMA_SEP + DatabaseContract.SkillGroupEntry.COLUMN_NAME_PROTO + BLOB_TYPE +
                    " )";
    private static final String SQL_DROP_SKILL_GROUPS =
            "DROP TABLE IF EXISTS " + DatabaseContract.SkillGroupEntry.TABLE_NAME;

    // Schedule DB statements
    private static final String SQL_CREATE_SCHEDULES =
            "CREATE TABLE " + DatabaseContract.ScheduleEntry.TABLE_NAME + " (" +
                    DatabaseContract.ScheduleEntry._ID + " INTEGER PRIMARY KEY" +
                    COMMA_SEP + DatabaseContract.SkillGroupEntry.COLUMN_NAME_NAME + TEXT_TYPE + UNIQUE +
                    COMMA_SEP + DatabaseContract.ScheduleEntry.COLUMN_NAME_PROTO + BLOB_TYPE +
                    " )";
    private static final String SQL_DROP_SCHEDULES =
            "DROP TABLE IF EXISTS " + DatabaseContract.ScheduleEntry.TABLE_NAME;

    /**
     * Main constructor.
     *
     * @param context
     */
    public DatabaseOpenHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    /**
     * Opens a database with a provided name.  Used for testing.
     *
     * @param context
     * @param databaseName
     */
    public DatabaseOpenHelper(Context context, String databaseName) {
        super(context, databaseName, null, DATABASE_VERSION);
    }

    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_SKILLS);
        db.execSQL(SQL_CREATE_SKILL_GROUPS);
        db.execSQL(SQL_CREATE_SCHEDULES);
    }

    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Right now there is only one version.
        onCreate(db);
    }

    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Right now there is only one version.
        onUpgrade(db, oldVersion, newVersion);
    }
}

