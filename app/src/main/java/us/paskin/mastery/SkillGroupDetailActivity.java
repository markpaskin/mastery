package us.paskin.mastery;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.Toast;

/**
 * An activity to view/edit a skill group.
 */
public class SkillGroupDetailActivity extends AppCompatActivity {
    /**
     * The intent arguments representing the index and ID of the skill being edited/added.
     * They are both an input and output argument.
     */
    public static final String ARG_SKILL_GROUP_INDEX = "skill_group_index";
    public static final String ARG_SKILL_GROUP_ID = "skill_group_id";

    /**
     * These are intent request types.
     */
    public static final int REQ_EDIT_SKILL_GROUP = 1;
    public static final int REQ_ADD_SKILL_GROUP = 2;

    /**
     * A handle on the data model.
     */
    private SkillData data;

    /**
     * True if we're adding a new skill group; false if we're editing one.
     */
    private boolean addingSkillGroup;

    /**
     * The index of the skill group being edited.  If this is -1, it's a new skill group.  After saving,
     * the ID will be set for a new skill group.
     */
    private int skillGroupIndex;

    /**
     * The ID of the skill being edited.
     */
    private long skillGroupId;

    /**
     * The skill group before any updates.  This is null if a new group is being created.
     */
    private Proto.SkillGroup skillGroup;

    /**
     * The builder that is used to update the skill group.
     */
    private Proto.SkillGroup.Builder skillGroupBuilder;

    /**
     * This is true if there have been changes that weren't committed.
     */
    private boolean unsavedChanges = false;

    /**
     * This is true if there were changes saved.
     */
    private boolean savedChanges = false;

    /**
     * This is true if a new skill group was added.
     */
    private boolean addedSkillGroup = false;

    /**
     * Sets up the activity.
     *
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        data = SkillData.getInstance(this);

        // Initialize this object from the intent arguments.
        addingSkillGroup = !getIntent().hasExtra(ARG_SKILL_GROUP_INDEX);
        if (!addingSkillGroup) {
            skillGroupIndex = getIntent().getIntExtra(SkillGroupDetailActivity.ARG_SKILL_GROUP_INDEX, -1);
            skillGroupId = getIntent().getLongExtra(SkillGroupDetailActivity.ARG_SKILL_GROUP_ID, -1);
            skillGroup = data.getSkillGroupById(skillGroupId);
            skillGroupBuilder = skillGroup.toBuilder();
        } else {
            skillGroupBuilder = Proto.SkillGroup.newBuilder();
        }

        setContentView(R.layout.activity_skill_group_detail);
        Toolbar toolbar = (Toolbar) findViewById(R.id.detail_toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (saveSkill()) finish();
            }
        });

        // Show the Up button in the action bar.
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        EditText nameEditText = ((EditText) findViewById(R.id.skill_group_name));

        // Initialize the controls with pre-existing values or defaults.
        if (skillGroup != null) {
            updateTitle(skillGroup.getName());
            nameEditText.setText(skillGroup.getName());
        }

        // Set up listeners (after setting initial values, so we don't get events for those).
        nameEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
                unsavedChanges = true;
                skillGroupBuilder.setName(editable.toString());
                updateTitle(editable);
            }
        });
    }

    /**
     * This is invoked if an option is select, e.g., the left arrow to return.
     *
     * @param item
     * @return
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            // This ID represents the Home or Up button. In the case of this
            // activity, the Up button is shown. Use NavUtils to allow users
            // to navigate up one level in the application structure. For
            // more details, see the Navigation pattern on Android Design:
            //
            // http://developer.android.com/design/patterns/navigation.html#up-vs-back
            //
            maybeFinish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Updates the title.
     *
     * @param title
     */
    void updateTitle(CharSequence title) {
        CollapsingToolbarLayout appBarLayout = (CollapsingToolbarLayout) findViewById(R.id.toolbar_layout);
        if (appBarLayout != null) appBarLayout.setTitle(title);
    }

    /**
     * Validates the current input and shows error dialogs if needed.
     */
    boolean validate() {
        if (!skillGroupBuilder.hasName() || skillGroupBuilder.getName().isEmpty()) {
            new AlertDialog.Builder(this)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setTitle(R.string.missing_skill_name_title)
                    .setMessage(R.string.missing_skill_name_detail)
                    .setNeutralButton(R.string.ok, null)
                    .show();
            return false;
        }
        return true;
    }

    /**
     * If there are unsaved changes, they are saved.
     *
     * @return true if no failures occur
     */
    boolean saveSkill() {
        if (!unsavedChanges) return true;
        if (!validate()) return false;
        if (!addingSkillGroup) {
            data.updateSkillGroup(skillGroupBuilder.build());
            Toast.makeText(getApplicationContext(), R.string.saved_skill_group, Toast.LENGTH_SHORT).show();
        } else {
            skillGroupBuilder.setId(Fingerprint.forString(skillGroupBuilder.getName()));
            skillGroupId = skillGroupBuilder.getId();
            data.addSkillGroup(skillGroupBuilder.build());
            addedSkillGroup = true;
            Toast.makeText(getApplicationContext(), R.string.added_skill_group, Toast.LENGTH_SHORT).show();
        }
        unsavedChanges = false;
        savedChanges = true;
        return true;
    }

    /**
     * Finishes if there are no unsaved changes (or the user discards them).
     */
    private void maybeFinish() {
        if (!unsavedChanges) {
            finish();
            return;
        }
        new AlertDialog.Builder(this)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle(R.string.discard_edits_title)
                .setMessage(R.string.discard_edits_question)
                .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        SkillGroupDetailActivity.this.finish();
                    }
                })
                .setNegativeButton(R.string.no, null)
                .show();
    }

    /**
     * Exits this activity, populating the intent results.
     */
    @Override
    public void finish() {
        Intent intent = new Intent();
        if (savedChanges) {
            intent.putExtra(ARG_SKILL_GROUP_INDEX, skillGroupIndex);
            intent.putExtra(ARG_SKILL_GROUP_ID, skillGroupId);
            setResult(Activity.RESULT_OK, intent);
        } else {
            setResult(Activity.RESULT_CANCELED, intent);
        }
        super.finish();
    }

    // Confirms if it is safe to exit before doing so.
    @Override
    public void onBackPressed() {
        maybeFinish();
    }
}
