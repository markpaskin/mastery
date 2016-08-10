package us.paskin.mastery;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.View;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.ActionBar;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.NumberPicker;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.LinkedList;

/**
 * An activity representing a single Skill detail screen. This
 * activity is only used narrow width devices. On tablet-size devices,
 * item details are presented side-by-side with a list of items
 * in a {@link SkillListActivity}.
 */
public class SkillDetailActivity extends AppCompatActivity {
    /**
     * The intent arguments representing the index and ID of the skill being edited/added.
     * They are both an input and output argument.  If ID is missing, then the request is
     * to add a new skill.  POSITION is an optional argument used by callers for whom it
     * is convenient to get the position returned as a result.  If ID is -1 on exit and was
     * not -1 on entry, then the skill has been deleted.
     */
    public static final String ARG_SKILL_ID = "skill_id";
    public static final String ARG_SKILL_POSITION = "skill_pos";

    /**
     * This intent type is used to identify results from child intents.
     */
    public static final int SELECT_SKILL_GROUP_TO_ADD = 1;

    /**
     * True if we're adding a new skill; false if we're editing one.
     */
    private boolean addingSkill;

    /**
     * If we're not adding a new skill, this is the previous skill data.
     */
    private Model data;

    /**
     * The index of the skill being edited (or -1 if it's being added).
     */
    private int skillIndex = -1;

    /**
     * The ID of the skill being edited (or -1 if it's being added).
     */
    private long skillId = -1;

    /**
     * The skill before any updates.  This is null if a new skill is being added.
     */
    private Proto.Skill skill;

    /**
     * The builder that is used to update the skill.  Its fields reflect the data entered by the user.
     */
    private Proto.Skill.Builder skillBuilder;

    /**
     * This is true if there have been changes that weren't committed.
     */
    private boolean unsavedChanges = false;

    /**
     * This is true if there were changes saved.
     */
    private boolean savedChanges = false;

    /**
     * This is the table used to render the skill groups this skill is in.
     */
    EditableList skillGroupList;

    /**
     * Sets up the activity.
     *
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        data = Model.getInstance(this);

        // Initialize this object from the intent arguments.
        addingSkill = !getIntent().hasExtra(ARG_SKILL_POSITION);
        if (!addingSkill) {
            skillIndex = getIntent().getIntExtra(SkillDetailActivity.ARG_SKILL_POSITION, -1);
            skillId = getIntent().getLongExtra(SkillDetailActivity.ARG_SKILL_ID, -1);
            skill = data.getSkillById(skillId);
            skillBuilder = skill.toBuilder();
        } else {
            skillBuilder = Proto.Skill.newBuilder();
        }

        setContentView(R.layout.activity_skill_detail);
        Toolbar toolbar = (Toolbar) findViewById(R.id.detail_toolbar);
        setSupportActionBar(toolbar);

        // Show the Up button in the action bar.
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        EditText nameEditText = ((EditText) findViewById(R.id.skill_name));
        TextView lastPracticedText = ((TextView) findViewById(R.id.last_practiced));

        // Hide the last practiced text if we're adding a new skill.
        if (addingSkill) lastPracticedText.setVisibility(View.GONE);

        // Set up the priority picker
        NumberPicker practicePriorityPicker = ((NumberPicker) findViewById(R.id.priority_picker));
        practicePriorityPicker.setMinValue(Model.MIN_PRIORITY);
        practicePriorityPicker.setMaxValue(Model.MAX_PRIORITY);

        // Initialize the controls with pre-existing values or defaults.
        if (skill != null) {
            updateTitle(skill.getName());
            nameEditText.setText(skill.getName());
            lastPracticedText.setText(Model.getLastPracticedText(skill, getResources()));
            practicePriorityPicker.setValue(skill.hasPriority() ? skill.getPriority() : Model.MAX_PRIORITY);
        } else {
            skillBuilder.setPriority(Model.MAX_PRIORITY);
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
                skillBuilder.setName(editable.toString());
                updateTitle(editable);
            }
        });
        practicePriorityPicker.setOnValueChangedListener(
                new NumberPicker.OnValueChangeListener() {
                    @Override
                    public void onValueChange(NumberPicker numberPicker, int oldVal, int newVal) {
                        unsavedChanges = true;
                        skillBuilder.setPriority(newVal);
                    }
                }
        );
        practicePriorityPicker.setValue(Model.DEFAULT_PRIORITY);

        skillGroupList = new EditableList(
                (TableLayout) findViewById(R.id.parent_group_list),
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Intent intent = new Intent(view.getContext(), SkillGroupListActivity.class);
                        intent.putExtra(SkillGroupListActivity.ARG_MODE_SELECT, true);
                        SkillDetailActivity.this.startActivityForResult(intent, SELECT_SKILL_GROUP_TO_ADD);
                    }
                });

        if (skill != null) {
            for (final long groupId : skill.getGroupIdList()) {
                addParentGroupToTable(groupId);
            }
        }
    }

    /**
     * Handles results from intents launched by this activity.
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != Activity.RESULT_OK) return;
        if (requestCode == SELECT_SKILL_GROUP_TO_ADD) {
            final long skillGroupId = data.getLongExtra(SkillGroupListActivity.ARG_SELECTED_SKILL_GROUP_ID, -1);
            if (!skillBuilder.getGroupIdList().contains(skillGroupId)) {
                skillBuilder.addGroupId(skillGroupId);
                unsavedChanges = true;
                addParentGroupToTable(skillGroupId);
                Toast.makeText(getApplicationContext(), R.string.added_skill_group, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getApplicationContext(), R.string.skill_group_already_present, Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void removeFromSkillGroup(long skillGroupId) {
        unsavedChanges = true;
        LinkedList<Long> groupIds = new LinkedList<Long>(skillBuilder.getGroupIdList());
        if (!groupIds.remove(skillGroupId)) {
            throw new InternalError("Could not remove " + skillGroupId);
        }
        skillBuilder.clearGroupId().addAllGroupId(groupIds);
        Toast.makeText(getApplicationContext(), R.string.skill_group_removed, Toast.LENGTH_SHORT).show();
    }

    /**
     * Adds an entry to the parent skill group table.
     */
    private void addParentGroupToTable(final long skillGroupId) {
        Proto.SkillGroup skillGroup = data.getSkillGroupById(skillGroupId);
        skillGroupList.addTextItem(
                skillGroup.getName(),
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Intent intent = new Intent(view.getContext(), SkillGroupDetailActivity.class);
                        intent.putExtra(SkillGroupDetailActivity.ARG_SKILL_GROUP_ID, skillGroupId);
                        SkillDetailActivity.this.startActivity(intent);
                    }
                },
                new Runnable() {
            @Override
            public void run() {
                removeFromSkillGroup(skillGroupId);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.skill_detail, menu);
        if (addingSkill) menu.findItem(R.id.delete_skill).setVisible(false);
        return super.onCreateOptionsMenu(menu);
    }

    /**
     * This is invoked if an option is select, e.g., the left arrow to return.
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
        } else if (id == R.id.delete_skill) {
            handleDeleteSkill();
            return true;
        } else if (id == R.id.revert_changes) {
            handleRevertChanges();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Called if the user requests to delete this skill.
     */
    void handleDeleteSkill() {
        new AlertDialog.Builder(this)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle(R.string.delete_skill_confirm_title)
                .setMessage(R.string.cannot_undo_detail)
                .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        SkillDetailActivity.this.deleteAndFinish();
                    }
                })
                .setNegativeButton(R.string.no, null)
                .show();
    }

    void deleteAndFinish() {
        if (!addingSkill) data.deleteSkill(skillId);
        skillId = -1;
        unsavedChanges = false;
        savedChanges = true;
        finish();
    }

    /**
     * Called if the user requests to revert changes.
     */
    void handleRevertChanges() {
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
                        SkillDetailActivity.this.finish();
                    }
                })
                .setNegativeButton(R.string.no, null)
                .show();
    }

    /**
     * Updates the title.
     */
    void updateTitle(CharSequence title) {
        CollapsingToolbarLayout appBarLayout = (CollapsingToolbarLayout) findViewById(R.id.toolbar_layout);
        if (appBarLayout != null) appBarLayout.setTitle(title);
    }

    /**
     * Validates the current input and shows error dialogs if needed.
     */
    boolean validate() {
        if (!skillBuilder.hasName() || skillBuilder.getName().isEmpty()) {
            new AlertDialog.Builder(this)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setTitle(R.string.missing_skill_name_title)
                    .setMessage(R.string.missing_skill_name_detail)
                    .setNeutralButton(R.string.ok, null)
                    .show();
            return false;
        }
        if (skillBuilder.getGroupIdList().isEmpty()) {
            new AlertDialog.Builder(this)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setTitle(R.string.no_skill_groups_title)
                    .setMessage(R.string.no_skill_groups_detail)
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
        if (!addingSkill) {
            data.updateSkill(skillId, skillBuilder.build());
            Toast.makeText(getApplicationContext(), R.string.saved_skill, Toast.LENGTH_SHORT).show();
        } else {
            skillId = data.addSkill(skillBuilder.build());
            Toast.makeText(getApplicationContext(), R.string.added_skill, Toast.LENGTH_SHORT).show();
        }
        unsavedChanges = false;
        savedChanges = true;
        return true;
    }

    /**
     * Finishes if any pending changes can be saved.
     */
    private void maybeFinish() {
        if (saveSkill()) finish();
    }

    /**
     * Exits this activity, populating the intent results.
     */
    @Override
    public void finish() {
        Intent intent = new Intent();
        if (savedChanges) {
            intent.putExtra(ARG_SKILL_POSITION, skillIndex);
            intent.putExtra(ARG_SKILL_ID, skillId);
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
