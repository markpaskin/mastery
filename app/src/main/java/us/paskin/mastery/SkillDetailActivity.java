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

import com.google.protobuf.InvalidProtocolBufferException;

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
     * If supplied and true, then the skill cannot be deleted.
     */
    public static final String ARG_DISABLE_DELETE = "disable_delete";

    /**
     * This intent type is used to identify results from child intents.
     */
    public static final int SELECT_SKILL_GROUP_TO_ADD = 1;

    /**
     * True if we're adding a new skill; false if we're editing one.
     */
    private boolean addingSkill;

    /**
     * True if ARG_DELETE_DISABLED was part of the launching intent.
     */
    private boolean deleteDisabled;

    /**
     * If we're not adding a new skill, this is the previous skill model.
     */
    private Model model;

    /**
     * The index of the skill being edited (or -1 if it's being added).
     */
    private int skillPosition = -1;

    /**
     * The ID of the skill being edited (or -1 if it's being added).
     */
    private long skillId = -1;

    /**
     * The builder that is used to update the skill.  Its fields reflect the model entered by the user.
     */
    private Proto.Skill.Builder skillBuilder;
    private static final String STATE_skillBuilder = "skillBuilder";

    /**
     * This is true if there have been changes that weren't committed.
     */
    private boolean unsavedChanges = false;
    private static final String STATE_unsavedChanges = "unsavedChanges";

    /**
     * This is true if there were changes saved.
     */
    private boolean savedChanges = false;
    private static final String STATE_savedChanges = "savedChanges";

    /**
     * This is the table used to render the skill groups this skill is in.
     */
    EditableList skillGroupList;

    MenuItem revertMenuItem;
    
    /**
     * Called to save the activity's state.
     */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putByteArray(STATE_skillBuilder, skillBuilder.build().toByteArray());
        outState.putBoolean(STATE_unsavedChanges, unsavedChanges);
        outState.putBoolean(STATE_savedChanges, savedChanges);
    }

    /**
     * Sets up the activity.
     *
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        model = Model.getInstance(this);

        // Initialize this object from the intent arguments.
        addingSkill = !getIntent().hasExtra(ARG_SKILL_ID);
        if (!addingSkill) {
            skillId = getIntent().getLongExtra(ARG_SKILL_ID, -1);
            skillPosition = getIntent().getIntExtra(ARG_SKILL_POSITION, -1);
        }

        deleteDisabled = getIntent().getBooleanExtra(ARG_DISABLE_DELETE, false);

        // Initialize the state.
        Proto.Skill skill = null;  // null if addingSkill
        if (savedInstanceState != null) {
            unsavedChanges = savedInstanceState.getBoolean(STATE_unsavedChanges);
            savedChanges = savedInstanceState.getBoolean(STATE_savedChanges);
            try {
                skillBuilder = Proto.Skill.parseFrom(savedInstanceState.getByteArray(STATE_skillBuilder)).toBuilder();
            } catch (InvalidProtocolBufferException x) {
                throw new InternalError("cannot parse protocol buffer");
            }
            skill = skillBuilder.build();
        } else if (addingSkill) {
            skillBuilder = Proto.Skill.newBuilder();
        } else {
            skill = model.getSkillById(skillId);
            skillBuilder = skill.toBuilder();
        }

        setContentView(R.layout.activity_skill_detail);
        Toolbar toolbar = (Toolbar) findViewById(R.id.detail_toolbar);
        setSupportActionBar(toolbar);

        // Show the Up button in the action bar.
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        EditText nameEditText = ((EditText) findViewById(R.id.skill_name_edit_text));
        TextView lastPracticedText = ((TextView) findViewById(R.id.last_practiced));
        TextView durationPracticedText = ((TextView) findViewById(R.id.duration_practiced));

        // Hide the last practiced text if we're adding a new skill.
        if (addingSkill) {
            lastPracticedText.setVisibility(View.GONE);
            durationPracticedText.setVisibility(View.GONE);
        }

        // Set up the priority picker
        NumberPicker practicePriorityPicker = ((NumberPicker) findViewById(R.id.priority_picker));
        practicePriorityPicker.setMinValue(Model.MIN_PRIORITY);
        practicePriorityPicker.setMaxValue(Model.MAX_PRIORITY);

        // Initialize the controls with pre-existing values or defaults.
        if (!addingSkill) {
            updateTitle(skillBuilder.getName());
            nameEditText.setText(skillBuilder.getName());
            lastPracticedText.setText(Model.getLastPracticedText(skill, getResources()));
            String durationPracticed = Model.getDurationPracticedText(skill, getResources());
            if (durationPracticed != null) {
                durationPracticedText.setText(durationPracticed);
            } else {
                durationPracticedText.setVisibility(View.GONE);
            }
            practicePriorityPicker.setValue(skillBuilder.hasPriority() ? skillBuilder.getPriority() : Model.MAX_PRIORITY);
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
                noteUnsavedChanges();
                String name = editable.toString();
                skillBuilder.setName(name);
                updateTitle(name);
            }
        });
        practicePriorityPicker.setOnValueChangedListener(
                new NumberPicker.OnValueChangeListener() {
                    @Override
                    public void onValueChange(NumberPicker numberPicker, int oldVal, int newVal) {
                        noteUnsavedChanges();
                        skillBuilder.setPriority(newVal);
                    }
                }
        );
        if (addingSkill) {
            practicePriorityPicker.setValue(Model.DEFAULT_PRIORITY);
        } else {
            practicePriorityPicker.setValue(skillBuilder.getPriority());
        }

        skillGroupList = new EditableList(
                (TableLayout) findViewById(R.id.parent_group_list),
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Intent intent = new Intent(view.getContext(), SkillGroupListActivity.class);
                        intent.putExtra(SkillGroupListActivity.ARG_MODE_SELECT, true);
                        SkillDetailActivity.this.startActivityForResult(intent, SELECT_SKILL_GROUP_TO_ADD);
                    }
                },
                new EditableList.OnItemRemovedListener() {
                    @Override
                    public void onItemRemoved(int index) {
                        removeFromSkillGroup(skillBuilder.getGroupId(index));
                    }
                });

        if (!addingSkill) {
            for (final long groupId : skillBuilder.getGroupIdList()) {
                addParentGroupToTable(groupId);
            }
        }
    }

    void noteUnsavedChanges() {
        unsavedChanges = true;
        if (revertMenuItem != null) revertMenuItem.setVisible(true);
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
                noteUnsavedChanges();
                addParentGroupToTable(skillGroupId);
                Toast.makeText(getApplicationContext(), R.string.added_skill_group, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getApplicationContext(), R.string.skill_group_already_present, Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void removeFromSkillGroup(long skillGroupId) {
        noteUnsavedChanges();
        LinkedList<Long> groupIds = new LinkedList<>(skillBuilder.getGroupIdList());
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
        Proto.SkillGroup skillGroup = model.getSkillGroupById(skillGroupId);
        skillGroupList.addTextItem(
                skillGroup.getName(),
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Intent intent = new Intent(view.getContext(), SkillGroupDetailActivity.class);
                        intent.putExtra(SkillGroupDetailActivity.ARG_SKILL_GROUP_ID, skillGroupId);
                        SkillDetailActivity.this.startActivity(intent);
                    }
                });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.skill_detail, menu);
        if (addingSkill || deleteDisabled) menu.findItem(R.id.delete_skill).setVisible(false);
        revertMenuItem = menu.findItem(R.id.revert_changes);
        revertMenuItem.setVisible(unsavedChanges);
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
        if (!addingSkill) model.deleteSkill(skillId);
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
    void updateTitle(String title) {
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
                    .setPositiveButton(R.string.ok, null)
                    .show();
            return false;
        }
        if (skillBuilder.getGroupIdList().isEmpty()) {
            new AlertDialog.Builder(this)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setTitle(R.string.no_skill_groups_title)
                    .setMessage(R.string.no_skill_groups_detail)
                    .setPositiveButton(R.string.ok, null)
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
            model.updateSkill(skillId, skillBuilder.build());
            Toast.makeText(getApplicationContext(), R.string.saved_skill, Toast.LENGTH_SHORT).show();
        } else {
            if (model.hasSkillWithName(skillBuilder.getName())) {
                new AlertDialog.Builder(this)
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setTitle(R.string.duplicate_name_title)
                        .setMessage(R.string.duplicate_name_detail)
                        .setPositiveButton(R.string.yes, null)
                        .show();
                return false;
            }
            skillId = model.addSkill(skillBuilder.build());
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
            intent.putExtra(ARG_SKILL_POSITION, skillPosition);
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
