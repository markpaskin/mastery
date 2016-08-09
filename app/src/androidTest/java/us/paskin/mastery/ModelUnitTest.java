package us.paskin.mastery;

import android.database.Cursor;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.app.Activity;
import android.content.Context;
import android.test.ActivityInstrumentationTestCase2;

import com.google.protobuf.InvalidProtocolBufferException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * To work on unit tests, switch the Test Artifact in the Build Variants view.
 */
@RunWith(AndroidJUnit4.class)
public class ModelUnitTest extends ActivityInstrumentationTestCase2<SkillDetailActivity> {
    public static final String TEST_DATABASE_NAME = "Test.db";

    protected Context context;
    protected Model model;

    public ModelUnitTest() {
        super(SkillDetailActivity.class);
    }

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        injectInstrumentation(InstrumentationRegistry.getInstrumentation());
        Activity activity = getActivity();
        context = activity.getApplicationContext();
        context.deleteDatabase(TEST_DATABASE_NAME);
        model = new Model(context, TEST_DATABASE_NAME);
    }

    @After
    @Override
    public void tearDown() throws Exception {
        super.tearDown();
    }

    @Test
    public void testGetSkillById() {
        Proto.Skill skill = Proto.Skill.newBuilder().setName("A").setPriority(5).build();
        final long id = model.addSkill(skill);
        Proto.Skill retrieved = model.getSkillById(id);
        assertEquals(skill, retrieved);
    }

    @Test
    public void testGetSkillList() throws InvalidProtocolBufferException {
        Proto.Skill skill = Proto.Skill.newBuilder().setName("B").setPriority(5).build();
        final long id = model.addSkill(skill);
        Proto.Skill skill2 = Proto.Skill.newBuilder().setName("A").setPriority(7).build();
        final long id2 = model.addSkill(skill2);
        Cursor cursor = model.getSkillList();
        assertEquals(2, cursor.getCount());
        assertEquals(2, cursor.getColumnCount());
        cursor.moveToFirst();
        assertEquals(id2, cursor.getLong(0));
        assertEquals(skill2, Proto.Skill.parseFrom(cursor.getBlob(1)));
        cursor.moveToNext();
        assertEquals(id, cursor.getLong(0));
        assertEquals(skill, Proto.Skill.parseFrom(cursor.getBlob(1)));
        cursor.close();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAddInvalidSkillMissingName() {
        model.addSkill(Proto.Skill.newBuilder().setPriority(7).build());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAddInvalidSkillEmptyName() {
        model.addSkill(Proto.Skill.newBuilder().setPriority(7).setName("").build());
    }


    @Test(expected = IllegalArgumentException.class)
    public void testAddInvalidSkillPriorityTooBig() {
        model.addSkill(Proto.Skill.newBuilder().setPriority(Model.MAX_PRIORITY + 1).setName("A").build());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAddInvalidSkillPriorityTooSmall() {
        model.addSkill(Proto.Skill.newBuilder().setPriority(Model.MIN_PRIORITY - 1).setName("A").build());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testUpdateSkillBadId() {
        model.updateSkill(0, Proto.Skill.newBuilder().setName("A").setPriority(2).build());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testUpdateSkillNoName() {
        model.updateSkill(0, Proto.Skill.newBuilder().setPriority(2).build());
    }

    @Test
    public void testUpdateSkill() {
        Proto.Skill skill = Proto.Skill.newBuilder().setName("A").setPriority(5).build();
        final long id = model.addSkill(skill);
        Proto.Skill retrieved = model.getSkillById(id);
        assertEquals(skill, retrieved);

        skill = skill.toBuilder().setName("B").build();
        model.updateSkill(id, skill);
        retrieved = model.getSkillById(id);
        assertEquals(skill, retrieved);
    }

    @Test
    public void testDeleteSkill() {
        Proto.Skill skill = Proto.Skill.newBuilder().setName("A").setPriority(5).build();
        assertEquals(0, model.getSkillList().getCount());
        final long id = model.addSkill(skill);
        assertEquals(1, model.getSkillList().getCount());
        model.deleteSkill(id);
        assertEquals(0, model.getSkillList().getCount());
    }

    @Test
    public void testDeleteSkillBadId() {
        try {
            model.deleteSkill(0);
        } catch (IllegalArgumentException x) {
            assertEquals("invalid id: 0", x.getMessage());
            return;
        }
        fail("did not throw an error");
    }

    @Test
    public void testGetSkillGroupById() {
        final long id = 1;
        Proto.SkillGroup skillGroup = Proto.SkillGroup.newBuilder().setName("A").setId(id).build();
        model.addSkillGroup(skillGroup);
        Proto.SkillGroup retrieved = model.getSkillGroupById(id);
        assertEquals(skillGroup.toBuilder().setId(id).build(), retrieved);
    }

    @Test
    public void testGetSkillGroupList() throws InvalidProtocolBufferException {
        final long id = 1;
        Proto.SkillGroup skillGroup = Proto.SkillGroup.newBuilder().setName("B").setId(id).build();
        model.addSkillGroup(skillGroup);
        final long id2 = 2;
        Proto.SkillGroup skillGroup2 = Proto.SkillGroup.newBuilder().setName("A").setId(id2).build();
        model.addSkillGroup(skillGroup2);
        Cursor cursor = model.getSkillGroupList();
        assertEquals(2, cursor.getCount());
        assertEquals(2, cursor.getColumnCount());
        cursor.moveToFirst();
        assertEquals(id2, cursor.getLong(0));
        assertEquals(skillGroup2, Proto.SkillGroup.parseFrom(cursor.getBlob(1)));
        cursor.moveToNext();
        assertEquals(id, cursor.getLong(0));
        assertEquals(skillGroup, Proto.SkillGroup.parseFrom(cursor.getBlob(1)));
        cursor.close();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAddInvalidSkillGroupMissingName() {
        model.addSkillGroup(Proto.SkillGroup.newBuilder().setId(1).build());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAddInvalidSkillGroupEmptyName() {
        model.addSkillGroup(Proto.SkillGroup.newBuilder().setName("").setId(1).build());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAddInvalidSkillGroupMissingId() {
        model.addSkillGroup(Proto.SkillGroup.newBuilder().setName("A").build());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testUpdateSkillGroupMissingId() {
        model.updateSkillGroup(Proto.SkillGroup.newBuilder().setName("A").build());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testUpdateSkillGroupBadId() {
        model.updateSkillGroup(Proto.SkillGroup.newBuilder().setName("A").setId(1).build());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testUpdateSkillGroupNoName() {
        final long id = 1;
        Proto.SkillGroup skillGroup = Proto.SkillGroup.newBuilder().setName("B").setId(id).build();
        model.addSkillGroup(skillGroup);
        skillGroup = Proto.SkillGroup.newBuilder().setId(id).build();
        model.updateSkillGroup(skillGroup);
    }

    @Test
    public void testUpdateSkillGroup() {
        final long id = 1;
        Proto.SkillGroup skillGroup = Proto.SkillGroup.newBuilder().setName("A").setId(id).build();
        model.addSkillGroup(skillGroup);
        Proto.SkillGroup retrieved = model.getSkillGroupById(id);
        assertEquals(skillGroup, retrieved);

        skillGroup = skillGroup.toBuilder().setName("B").build();
        model.updateSkillGroup(skillGroup);
        retrieved = model.getSkillGroupById(id);
        assertEquals(skillGroup, retrieved);
    }

    private void addFakeData() {
        // Add skill groups.  Note these must be done in reverse dependency order.
        model.addSkillGroup(Proto.SkillGroup.newBuilder()
                .setId(0)
                .setName("Warm-ups")
                .build());
        model.addSkillGroup(Proto.SkillGroup.newBuilder()
                .setId(-1)
                .setName("Scales")
                .addParentId(0)
                .build());
        model.addSkillGroup(Proto.SkillGroup.newBuilder()
                .setId(1)
                .setName("Etudes")
                .build());

        // Add skills
        model.addSkill(Proto.Skill.newBuilder()
                .setName("Carcassi Op. 60 No. 7")
                .setPriority(6)
                .addGroupId(1)
                .build());
        model.addSkill(Proto.Skill.newBuilder()
                .setName("Shearer Scale p. 253")
                .setPriority(2)
                .addGroupId(-1)
                .build());
    }

    ////

    @Test
    public void testGetScheduleById() {
        final long groupId = 1;
        model.addSkillGroup(Proto.SkillGroup.newBuilder().setName("G").setId(groupId).build());
        Proto.Schedule schedule = Proto.Schedule.newBuilder().setName("S").addSlot(
                Proto.Schedule.Slot.newBuilder().setGroupId(groupId).setDurationInSecs(60)).build();
        final long scheduleId = model.addSchedule(schedule);
        Proto.Schedule retrieved = model.getScheduleById(scheduleId);
        assertEquals(schedule, retrieved);
    }

    @Test
    public void testGetScheduleList() throws InvalidProtocolBufferException {
        final long groupId = 1;
        model.addSkillGroup(Proto.SkillGroup.newBuilder().setName("G").setId(groupId).build());
        Proto.Schedule schedule = Proto.Schedule.newBuilder().setName("T").addSlot(
                Proto.Schedule.Slot.newBuilder().setGroupId(groupId).setDurationInSecs(60)).build();
        final long scheduleId = model.addSchedule(schedule);
        Proto.Schedule schedule2 = Proto.Schedule.newBuilder().setName("S").addSlot(
                Proto.Schedule.Slot.newBuilder().setGroupId(groupId).setDurationInSecs(60)).build();
        final long scheduleId2 = model.addSchedule(schedule2);

        Cursor cursor = model.getScheduleList();
        assertEquals(2, cursor.getCount());
        assertEquals(2, cursor.getColumnCount());
        cursor.moveToFirst();
        assertEquals(scheduleId2, cursor.getLong(0));
        assertEquals(schedule2, Proto.Schedule.parseFrom(cursor.getBlob(1)));
        cursor.moveToNext();
        assertEquals(scheduleId, cursor.getLong(0));
        assertEquals(schedule, Proto.Schedule.parseFrom(cursor.getBlob(1)));
        cursor.close();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAddInvalidScheduleMissingName() {
        final long groupId = 1;
        model.addSkillGroup(Proto.SkillGroup.newBuilder().setName("G").setId(groupId).build());
        model.addSchedule(Proto.Schedule.newBuilder().addSlot(
                Proto.Schedule.Slot.newBuilder().setGroupId(groupId).setDurationInSecs(60)).build());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAddInvalidScheduleEmptyName() {
        final long groupId = 1;
        model.addSkillGroup(Proto.SkillGroup.newBuilder().setName("G").setId(groupId).build());
        model.addSchedule(Proto.Schedule.newBuilder().setName("").addSlot(
                Proto.Schedule.Slot.newBuilder().setGroupId(groupId).setDurationInSecs(60)).build());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAddInvalidScheduleNoSlots() {
        model.addSchedule(Proto.Schedule.newBuilder().setName("S").build());
    }

    @Test
    public void testAddInvalidScheduleSlotHasBadGroupId() {
        try {
            model.addSchedule(Proto.Schedule.newBuilder().setName("S").addSlot(
                    Proto.Schedule.Slot.newBuilder().setGroupId(1).setDurationInSecs(60)).build());
        } catch (IllegalArgumentException x) {
            assertEquals("slot has invalid group id", x.getMessage());
            return;
        }
        fail("did not throw an error");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAddInvalidScheduleSlotHasNonPosDuration() {
        final long groupId = 1;
        model.addSkillGroup(Proto.SkillGroup.newBuilder().setName("G").setId(groupId).build());
        model.addSchedule(Proto.Schedule.newBuilder().setName("S").addSlot(
                Proto.Schedule.Slot.newBuilder().setGroupId(groupId).setDurationInSecs(0)).build());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testUpdateScheduleBadId() {
        final long groupId = 1;
        model.addSkillGroup(Proto.SkillGroup.newBuilder().setName("G").setId(groupId).build());
        model.updateSchedule(0, Proto.Schedule.newBuilder().setName("A").addSlot(
                Proto.Schedule.Slot.newBuilder().setGroupId(groupId).setDurationInSecs(60)).build());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testUpdateScheduleNoName() {
        final long groupId = 1;
        model.addSkillGroup(Proto.SkillGroup.newBuilder().setName("G").setId(groupId).build());
        final long scheduleId = model.addSchedule(Proto.Schedule.newBuilder().setName("S").addSlot(
                Proto.Schedule.Slot.newBuilder().setGroupId(groupId).setDurationInSecs(60)).build());
        model.updateSchedule(scheduleId, Proto.Schedule.newBuilder().addSlot(
                Proto.Schedule.Slot.newBuilder().setGroupId(groupId).setDurationInSecs(60)).build());
    }

    @Test
    public void testUpdateSchedule() {
        final long groupId = 1;
        model.addSkillGroup(Proto.SkillGroup.newBuilder().setName("G").setId(groupId).build());
        Proto.Schedule schedule = Proto.Schedule.newBuilder().setName("S").addSlot(
                Proto.Schedule.Slot.newBuilder().setGroupId(groupId).setDurationInSecs(60)).build();
        final long scheduleId = model.addSchedule(schedule);
        Proto.Schedule retrieved = model.getScheduleById(scheduleId);
        assertEquals(schedule, retrieved);

        schedule = schedule.toBuilder().setName("T").build();
        model.updateSchedule(scheduleId, schedule);
        retrieved = model.getScheduleById(scheduleId);
        assertEquals(schedule, retrieved);
    }

    @Test
    public void testDeleteSchedule() {
        final long groupId = 1;
        model.addSkillGroup(Proto.SkillGroup.newBuilder().setName("G").setId(groupId).build());
        Proto.Schedule schedule = Proto.Schedule.newBuilder().setName("S").addSlot(
                Proto.Schedule.Slot.newBuilder().setGroupId(groupId).setDurationInSecs(60)).build();

        assertEquals(0, model.getScheduleList().getCount());
        final long scheduleId = model.addSchedule(schedule);
        assertEquals(1, model.getScheduleList().getCount());
        model.deleteSchedule(scheduleId);
        assertEquals(0, model.getScheduleList().getCount());
    }

    @Test
    public void testDeleteScheduleBadId() {
        try {
            model.deleteSchedule(0);
        } catch (IllegalArgumentException x) {
            assertEquals("invalid id: 0", x.getMessage());
            return;
        }
        fail("did not throw an error");
    }

    @Test
    public void testInitWithData() {
        // Add fake data.
        addFakeData();
        assertEquals(2, model.getSkillList().getCount());
        assertEquals(3, model.getSkillGroupList().getCount());
        assertTrue(model.isAncestorOf(0, -1));
        assertFalse(model.isAncestorOf(-1, 0));

        // Now rebuild the object; this should cause us to reload/reinitialize.
        model = new Model(context, TEST_DATABASE_NAME);
        // Check that everything's in order.
        assertEquals(2, model.getSkillList().getCount());
        assertEquals(3, model.getSkillGroupList().getCount());
        assertTrue(model.isAncestorOf(0, -1));
        assertFalse(model.isAncestorOf(-1, 0));
    }
}