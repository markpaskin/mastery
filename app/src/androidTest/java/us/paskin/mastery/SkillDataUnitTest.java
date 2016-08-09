package us.paskin.mastery;

import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.app.Activity;
import android.content.Context;
import android.test.ActivityInstrumentationTestCase2;
import android.test.ActivityTestCase;
import android.test.ActivityUnitTestCase;

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

    SkillDetailActivity activity;
    SkillData skillData;

    public SkillDataUnitTest() {
        super(SkillDetailActivity.class);
    }

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        injectInstrumentation(InstrumentationRegistry.getInstrumentation());
        activity = getActivity();
        Context context = activity.getApplicationContext();
        context.deleteDatabase(TEST_DATABASE_NAME);
        skillData = new SkillData(context, TEST_DATABASE_NAME);
    }

    @After
    @Override
    public void tearDown() throws Exception {
        super.tearDown();
    }

    @Test
    public void testAddSkill() {
        Proto.Skill skill = Proto.Skill.newBuilder().setName("A").setPriority(5).addGroupId(1).build();
        final long id = skillData.addSkill(skill);
        Proto.Skill retrieved = skillData.getSkillById(id);
        assertEquals(skill, retrieved);
    }
}