package us.paskin.mastery;

import android.provider.BaseColumns;

/**
 *
 */
public class DatabaseContract {
    DatabaseContract() {
    }

    public static abstract class SkillEntry implements BaseColumns {
        public static final String TABLE_NAME = "skills";
        public static final String COLUMN_NAME_NAME = "name";
        public static final String COLUMN_NAME_PROTO = "proto";
    }

    public static abstract class SkillGroupEntry implements BaseColumns {
        public static final String TABLE_NAME = "skill_groups";
        public static final String COLUMN_NAME_NAME = "name";
        public static final String COLUMN_NAME_PROTO = "proto";
    }

    public static abstract class ScheduleEntry implements BaseColumns {
        public static final String TABLE_NAME = "schedules";
        public static final String COLUMN_NAME_NAME = "name";
        public static final String COLUMN_NAME_PROTO = "proto";
    }
}
