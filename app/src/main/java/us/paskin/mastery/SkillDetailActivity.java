package us.paskin.mastery;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.ActionBar;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.Toast;

/**
 * An activity representing a single Skill detail screen. This
 * activity is only used narrow width devices. On tablet-size devices,
 * item details are presented side-by-side with a list of items
 * in a {@link SkillListActivity}.
 */
public class SkillDetailActivity extends AppCompatActivity {
    /**
     * The intent argument representing the index of the skill being edited/added.
     * It is both an input and output argument.
     */
    public static final String ARG_SKILL_INDEX = "skill_index";


    /**
     * These are intent request types.
     */
    public static final int REQ_EDIT_SKILL = 1;
    public static final int REQ_ADD_SKILL = 2;

    /**
     * The index of the skill being edited.  If this is -1, it's a new skill.
     */
    private int skillIndex = -1;

    /**
     * The skill before any updates.  This is null if a new skill is being created.
     */
    private Proto.Skill skill;

    /**
     * The builder that is used to update the skill.
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
     * This is true if a new skill was added.
     */
    private boolean addedSkill = false;

    /**
     * Sets up the activity.
     *
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize this object from the intent arguments.
        if (getIntent().hasExtra(ARG_SKILL_INDEX)) {
            skillIndex = getIntent().getIntExtra(SkillDetailActivity.ARG_SKILL_INDEX, -1);
            skill = SkillData.getInstance().getSkillByIndex(skillIndex);
            assert skill != null;
            skillBuilder = skill.toBuilder();
        } else {
            skillBuilder = Proto.Skill.newBuilder();
        }

        setContentView(R.layout.activity_skill_detail);
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

        EditText nameEditText = ((EditText) findViewById(R.id.skill_name));
        if (skill != null) {
            updateTitle(skill.getName());
            nameEditText.setText(skill.getName());
        }

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
    }

    /**
     * This is invoked if an option is select, e.g., the left arrow to return.
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
     * @param title
     */
    void updateTitle(CharSequence title) {
        CollapsingToolbarLayout appBarLayout = (CollapsingToolbarLayout) findViewById(R.id.toolbar_layout);
        if (appBarLayout != null) appBarLayout.setTitle(title);
    }

    /**
     * If there are unsaved changes, they are saved.
     *
     * @return true if no failures occur
     */
    boolean saveSkill() {
        if (!unsavedChanges) return true;
        SkillData data = SkillData.getInstance();
        if (skillIndex != -1) {
            data.updateSkill(skillIndex, skillBuilder.build());
            Toast.makeText(getApplicationContext(), R.string.saved_skill, Toast.LENGTH_SHORT).show();
        } else {
            skillIndex = data.addSkill(skillBuilder.build());
            addedSkill = true;
            Toast.makeText(getApplicationContext(), R.string.added_skill, Toast.LENGTH_SHORT).show();
        }
        unsavedChanges = false;
        savedChanges = true;
        return true;
    }

    /**
     *  Finishes if there are no unsaved changes (or the user discards them).
     */
    private void maybeFinish() {
        if (!unsavedChanges) {
            finish();
            return;
        }
        // TODO: i18n
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
     * Exits this activity, populating the intent results.
     */
    @Override
    public void finish() {
        Intent intent = new Intent();
        if (savedChanges) {
            intent.putExtra(ARG_SKILL_INDEX, skillIndex);
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
