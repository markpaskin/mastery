package us.paskin.mastery;

import android.database.Cursor;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.app.Activity;
import android.content.Context;
import android.test.ActivityInstrumentationTestCase2;
import android.test.ActivityTestCase;
import android.test.ActivityUnitTestCase;

import com.google.protobuf.InvalidProtocolBufferException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * To work on unit tests, switch the Test Artifact in the Build Variants view.
 */
@RunWith(AndroidJUnit4.class)
public class SkillDataUnitTest extends ActivityInstrumentationTestCase2<SkillDetailActivity> {
    public static final String TEST_DATABASE_NAME = "Test.db";

    Context context;
    SkillData skillData;

    public SkillDataUnitTest() {
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
        skillData = new SkillData(context, TEST_DATABASE_NAME);
    }

    @After
    @Override
    public void tearDown() throws Exception {
        super.tearDown();
    }

    @Test
    public void testGetSkillById() {
        Proto.Skill skill = Proto.Skill.newBuilder().setName("A").setPriority(5).build();
        final long id = skillData.addSkill(skill);
        Proto.Skill retrieved = skillData.getSkillById(id);
        assertEquals(skill, retrieved);
    }

    @Test
    public void testGetSkillList() throws InvalidProtocolBufferException {
        Proto.Skill skill = Proto.Skill.newBuilder().setName("B").setPriority(5).build();
        final long id = skillData.addSkill(skill);
        Proto.Skill skill2 = Proto.Skill.newBuilder().setName("A").setPriority(7).build();
        final long id2 = skillData.addSkill(skill2);
        Cursor cursor = skillData.getSkillList();
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
        skillData.addSkill(Proto.Skill.newBuilder().setPriority(7).build());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAddInvalidSkillEmptyName() {
        skillData.addSkill(Proto.Skill.newBuilder().setPriority(7).setName("").build());
    }


    @Test(expected = IllegalArgumentException.class)
    public void testAddInvalidSkillPriorityTooBig() {
        skillData.addSkill(Proto.Skill.newBuilder().setPriority(SkillData.MAX_PRIORITY + 1).setName("A").build());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAddInvalidSkillPriorityTooSmall() {
        skillData.addSkill(Proto.Skill.newBuilder().setPriority(SkillData.MIN_PRIORITY - 1).setName("A").build());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testUpdateSkillBadId() {
        skillData.updateSkill(0, Proto.Skill.newBuilder().setName("A").setPriority(2).build());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testUpdateSkillNoName() {
        skillData.updateSkill(0, Proto.Skill.newBuilder().setPriority(2).build());
    }

    @Test
    public void testUpdateSkill() {
        Proto.Skill skill = Proto.Skill.newBuilder().setName("A").setPriority(5).build();
        final long id = skillData.addSkill(skill);
        Proto.Skill retrieved = skillData.getSkillById(id);
        assertEquals(skill, retrieved);

        skill = skill.toBuilder().setName("B").build();
        skillData.updateSkill(id, skill);
        retrieved = skillData.getSkillById(id);
        assertEquals(skill, retrieved);
    }

    @Test
    public void testGetSkillGroupById() {
        final long id = 1;
        Proto.SkillGroup skillGroup = Proto.SkillGroup.newBuilder().setName("A").setId(id).build();
        skillData.addSkillGroup(skillGroup);
        Proto.SkillGroup retrieved = skillData.getSkillGroupById(id);
        assertEquals(skillGroup.toBuilder().setId(id).build(), retrieved);
    }

    @Test
    public void testGetSkillGroupList() throws InvalidProtocolBufferException {
        final long id = 1;
        Proto.SkillGroup skillGroup = Proto.SkillGroup.newBuilder().setName("B").setId(id).build();
        skillData.addSkillGroup(skillGroup);
        final long id2 = 2;
        Proto.SkillGroup skillGroup2 = Proto.SkillGroup.newBuilder().setName("A").setId(id2).build();
        skillData.addSkillGroup(skillGroup2);
        Cursor cursor = skillData.getSkillGroupList();
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
        skillData.addSkillGroup(Proto.SkillGroup.newBuilder().setId(1).build());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAddInvalidSkillGroupEmptyName() {
        skillData.addSkillGroup(Proto.SkillGroup.newBuilder().setName("").setId(1).build());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAddInvalidSkillGroupMissingId() {
        skillData.addSkillGroup(Proto.SkillGroup.newBuilder().setName("A").build());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testUpdateSkillGroupMissingId() {
        skillData.updateSkillGroup(Proto.SkillGroup.newBuilder().setName("A").build());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testUpdateSkillGroupBadId() {
        skillData.updateSkillGroup(Proto.SkillGroup.newBuilder().setName("A").setId(1).build());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testUpdateSkillGroupNoName() {
        final long id = 1;
        Proto.SkillGroup skillGroup = Proto.SkillGroup.newBuilder().setName("B").setId(id).build();
        skillData.addSkillGroup(skillGroup);
        skillGroup = Proto.SkillGroup.newBuilder().setId(id).build();
        skillData.updateSkillGroup(skillGroup);
    }

    @Test
    public void testUpdateSkillGroup() {
        final long id = 1;
        Proto.SkillGroup skillGroup = Proto.SkillGroup.newBuilder().setName("A").setId(id).build();
        skillData.addSkillGroup(skillGroup);
        Proto.SkillGroup retrieved = skillData.getSkillGroupById(id);
        assertEquals(skillGroup, retrieved);

        skillGroup = skillGroup.toBuilder().setName("B").build();
        skillData.updateSkillGroup(skillGroup);
        retrieved = skillData.getSkillGroupById(id);
        assertEquals(skillGroup, retrieved);
    }

    private void addFakeData() {
        // Add skill groups.  Note these must be done in reverse dependency order.
        skillData.addSkillGroup(Proto.SkillGroup.newBuilder()
                .setId(0)
                .setName("Warm-ups")
                .build());
        skillData.addSkillGroup(Proto.SkillGroup.newBuilder()
                .setId(-1)
                .setName("Scales")
                .addParentId(0)
                .build());
        skillData.addSkillGroup(Proto.SkillGroup.newBuilder()
                .setId(1)
                .setName("Etudes")
                .build());

        // Add skills
        skillData.addSkill(Proto.Skill.newBuilder()
                .setName("Carcassi Op. 60 No. 7")
                .setPriority(6)
                .addGroupId(1)
                .build());
        skillData.addSkill(Proto.Skill.newBuilder()
                .setName("Shearer Scale p. 253")
                .setPriority(2)
                .addGroupId(-1)
                .build());
    }

    @Test
    public void testInitWithData() {
        // Add fake data.
        addFakeData();
        assertEquals(2, skillData.getSkillList().getCount());
        assertEquals(3, skillData.getSkillGroupList().getCount());
        assertTrue(skillData.isAncestorOf(0, -1));
        assertFalse(skillData.isAncestorOf(-1, 0));

        // Now rebuild the object; this should cause us to reload/reinitialize.
        skillData = new SkillData(context, TEST_DATABASE_NAME);
        // Check that everything's in order.
        assertEquals(2, skillData.getSkillList().getCount());
        assertEquals(3, skillData.getSkillGroupList().getCount());
        assertTrue(skillData.isAncestorOf(0, -1));
        assertFalse(skillData.isAncestorOf(-1, 0));
    }
}