package us.paskin.mastery;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TableLayout;
import android.widget.Toast;

import com.google.protobuf.InvalidProtocolBufferException;

import java.util.LinkedList;

/**
 * An activity to view/edit a skill group.
 */
public class SkillGroupDetailActivity extends AppCompatActivity {
    /**
     * The intent arguments representing the index and ID of the skill group being edited/added.
     * They are both an input and output argument.  If ID is missing, then the request is
     * to add a new group.  POSITION is an optional argument used by callers for whom it
     * is convenient to get the position returned as a result.
     */
    public static final String ARG_SKILL_GROUP_ID = "skill_group_id";
    public static final String ARG_SKILL_GROUP_POSITION = "skill_group_pos";

    /**
     * This intent type is used to identify results from child intents.
     */
    public static final int SELECT_PARENT_GROUP_TO_ADD = 1;

    /**
     * A handle on the model model.
     */
    private Model model;

    /**
     * True if we're adding a new skill group; false if we're editing one.
     */
    private boolean addingSkillGroup;

    /**
     * The ID of the skill being edited (or -1 if one is being created).
     */
    private long skillGroupId = -1;

    /**
     * The index of the skill group being edited (or -1 if none was supplied).
     */
    private int skillGroupPosition = -1;

    /**
     * The builder that is used to update the skill group.
     */
    private Proto.SkillGroup.Builder skillGroupBuilder;
    private static final String STATE_skillGroupBuilder = "skillGroupBuilder";

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
     * This is the table of parent groups this group is in.
     */
    EditableList parentGroupList;

    MenuItem revertMenuItem;

    /**
     * Called to save the activity's state.
     */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putByteArray(STATE_skillGroupBuilder, skillGroupBuilder.build().toByteArray());
        outState.putBoolean(STATE_unsavedChanges, unsavedChanges);
        outState.putBoolean(STATE_savedChanges, savedChanges);
    }

    /**
     * Sets up the activity.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        model = Model.getInstance(this);

        // Initialize this object from the intent arguments.
        addingSkillGroup = !getIntent().hasExtra(ARG_SKILL_GROUP_ID);
        if (!addingSkillGroup) {
            skillGroupPosition = getIntent().getIntExtra(SkillGroupDetailActivity.ARG_SKILL_GROUP_POSITION, -1);
            skillGroupId = getIntent().getLongExtra(SkillGroupDetailActivity.ARG_SKILL_GROUP_ID, -1);
        }
        // Initialize the state variables.
        if (savedInstanceState != null) {
            unsavedChanges = savedInstanceState.getBoolean(STATE_unsavedChanges);
            savedChanges = savedInstanceState.getBoolean(STATE_savedChanges);
            try {
                skillGroupBuilder = Proto.SkillGroup.parseFrom(
                        savedInstanceState.getByteArray(STATE_skillGroupBuilder)).toBuilder();
            } catch (InvalidProtocolBufferException x) {
                throw new InternalError("cannot parse protocol buffer");
            }
        } else if (addingSkillGroup) {
            skillGroupBuilder = Proto.SkillGroup.newBuilder();
        } else {
            skillGroupBuilder = model.getSkillGroupById(skillGroupId).toBuilder();
        }

        setContentView(R.layout.activity_skill_group_detail);
        Toolbar toolbar = (Toolbar) findViewById(R.id.detail_toolbar);
        setSupportActionBar(toolbar);

        // Show the Up button in the action bar.
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        EditText nameEditText = ((EditText) findViewById(R.id.skill_group_name));

        // Initialize the controls with pre-existing values or defaults.
        if (!addingSkillGroup) {
            updateTitle(skillGroupBuilder.getName());
            nameEditText.setText(skillGroupBuilder.getName());
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
                skillGroupBuilder.setName(editable.toString());
                updateTitle(editable.toString());
            }
        });

        parentGroupList = new EditableList(
                (TableLayout) findViewById(R.id.parent_group_list),
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Intent intent = new Intent(view.getContext(), SkillGroupListActivity.class);
                        intent.putExtra(SkillGroupListActivity.ARG_MODE_SELECT, true);
                        SkillGroupDetailActivity.this.startActivityForResult(intent, SELECT_PARENT_GROUP_TO_ADD);
                    }
                },
                new EditableList.OnItemRemovedListener() {
                    @Override
                    public void onItemRemoved(int index) {
                        removeParentGroup(skillGroupBuilder.getParentId(index));
                    }
                });

        if (!addingSkillGroup) {
            for (final long groupId : skillGroupBuilder.getParentIdList()) {
                addParentGroupToTable(groupId);
            }
        }
    }

    void noteUnsavedChanges() {
        unsavedChanges = true;
        if (revertMenuItem != null) revertMenuItem.setVisible(true);
    }
    
    private void tryAddParentGroup(long parentGroupId) {
        if (!addingSkillGroup && parentGroupId == skillGroupId) {
            Toast.makeText(getApplicationContext(), R.string.skill_self_parent, Toast.LENGTH_SHORT).show();
            return;
        }
        if (skillGroupBuilder.getParentIdList().contains(parentGroupId)) {
            Toast.makeText(getApplicationContext(), R.string.skill_group_already_present, Toast.LENGTH_SHORT).show();
            return;
        }
        if (!addingSkillGroup && model.isAncestorOf(skillGroupId, parentGroupId)) {
            new AlertDialog.Builder(this)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setTitle(R.string.skill_group_cycle_title)
                    .setMessage(R.string.skill_group_cycle_detail)
                    .setPositiveButton(R.string.ok, null)
                    .show();
            return;
        }
        skillGroupBuilder.addParentId(parentGroupId);
        noteUnsavedChanges();
        addParentGroupToTable(parentGroupId);
        Toast.makeText(getApplicationContext(), R.string.added_skill_group, Toast.LENGTH_SHORT).show();
    }

    /**
     * Handles results from intents launched by this activity.
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != Activity.RESULT_OK) return;
        if (requestCode == SELECT_PARENT_GROUP_TO_ADD) {
            final long parentGroupId = data.getLongExtra(SkillGroupListActivity.ARG_SELECTED_SKILL_GROUP_ID, -1);
            tryAddParentGroup(parentGroupId);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.skill_group_detail, menu);
        revertMenuItem = menu.findItem(R.id.revert_changes);
        revertMenuItem.setVisible(unsavedChanges);
        return super.onCreateOptionsMenu(menu);
    }

    private void removeParentGroup(long skillGroupId) {
        noteUnsavedChanges();
        LinkedList<Long> groupIds = new LinkedList<Long>(skillGroupBuilder.getParentIdList());
        if (!groupIds.remove(skillGroupId)) {
            throw new InternalError("Could not remove " + skillGroupId);
        }
        skillGroupBuilder.clearParentId().addAllParentId(groupIds);
        Toast.makeText(getApplicationContext(), R.string.skill_group_removed, Toast.LENGTH_SHORT).show();
    }

    /**
     * Adds an entry to the parent skill group table.
     */
    private void addParentGroupToTable(final long skillGroupId) {
        Proto.SkillGroup skillGroup = model.getSkillGroupById(skillGroupId);
        parentGroupList.addTextItem(
                skillGroup.getName(),
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Intent intent = new Intent(view.getContext(), SkillGroupDetailActivity.class);
                        intent.putExtra(SkillGroupDetailActivity.ARG_SKILL_GROUP_ID, skillGroupId);
                        SkillGroupDetailActivity.this.startActivity(intent);
                    }
                });
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
        } else if (id == R.id.revert_changes) {
            handleRevertChanges();
            return true;
        }
        return super.onOptionsItemSelected(item);
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
                        SkillGroupDetailActivity.this.finish();
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
        if (!skillGroupBuilder.hasName() || skillGroupBuilder.getName().isEmpty()) {
            new AlertDialog.Builder(this)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setTitle(R.string.missing_skill_name_title)
                    .setMessage(R.string.missing_skill_name_detail)
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
        if (!addingSkillGroup) {
            model.updateSkillGroup(skillGroupBuilder.build());
            Toast.makeText(getApplicationContext(), R.string.saved_skill_group, Toast.LENGTH_SHORT).show();
        } else {
            if (model.hasSkillGroupWithName(skillGroupBuilder.getName())) {
                new AlertDialog.Builder(this)
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setTitle(R.string.duplicate_name_title)
                        .setMessage(R.string.duplicate_name_detail)
                        .setPositiveButton(R.string.yes, null)
                        .show();
                return false;
            }
            skillGroupBuilder.setId(Fingerprint.forString(skillGroupBuilder.getName()));
            skillGroupId = skillGroupBuilder.getId();
            model.addSkillGroup(skillGroupBuilder.build());
            Toast.makeText(getApplicationContext(), R.string.added_skill_group, Toast.LENGTH_SHORT).show();
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
            intent.putExtra(ARG_SKILL_GROUP_POSITION, skillGroupPosition);
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
