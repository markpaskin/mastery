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
    private static final String COMMA_SEP = ",";

    // Skills DB statements
    private static final String SQL_CREATE_SKILLS =
            "CREATE TABLE " + DatabaseContract.SkillEntry.TABLE_NAME + " (" +
                    DatabaseContract.SkillEntry._ID + " INTEGER PRIMARY KEY" +
                    COMMA_SEP + DatabaseContract.SkillEntry.COLUMN_NAME_NAME + TEXT_TYPE +
                    COMMA_SEP + DatabaseContract.SkillEntry.COLUMN_NAME_PROTO + BLOB_TYPE +
                    " )";
    private static final String SQL_DELETE_SKILLS =
            "DROP TABLE IF EXISTS " + DatabaseContract.SkillEntry.TABLE_NAME;


    // Skill groups DB statements
    private static final String SQL_CREATE_SKILL_GROUPS =
            "CREATE TABLE " + DatabaseContract.SkillGroupEntry.TABLE_NAME + " (" +
                    DatabaseContract.SkillGroupEntry._ID + " INTEGER PRIMARY KEY" +
                    COMMA_SEP + DatabaseContract.SkillGroupEntry.COLUMN_NAME_NAME + TEXT_TYPE +
                    COMMA_SEP + DatabaseContract.SkillGroupEntry.COLUMN_NAME_PROTO + BLOB_TYPE +
                    " )";
    private static final String SQL_DELETE_SKILL_GROUPS =
            "DROP TABLE IF EXISTS " + DatabaseContract.SkillGroupEntry.TABLE_NAME;

    private static DatabaseOpenHelper singleton;

    public static synchronized DatabaseOpenHelper getInstance(Context context) {
        // Use the application context, which will ensure that you
        // don't accidentally leak an Activity's context.
        // See this article for more information: http://bit.ly/6LRzfx
        if (singleton == null) {
            singleton = new DatabaseOpenHelper(context.getApplicationContext());
        }
        return singleton;
    }

    private DatabaseOpenHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        context.deleteDatabase(DATABASE_NAME);  // TODO remove
    }

    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_SKILLS);
        db.execSQL(SQL_CREATE_SKILL_GROUPS);
    }

    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // TODO: upgrade policy is to simply to discard the data and start over
        db.execSQL(SQL_DELETE_SKILLS);
        db.execSQL(SQL_DELETE_SKILL_GROUPS);
        onCreate(db);
    }

    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onUpgrade(db, oldVersion, newVersion);
    }
}

