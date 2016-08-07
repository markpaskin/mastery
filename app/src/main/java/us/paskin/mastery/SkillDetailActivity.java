package us.paskin.mastery;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.ActionBar;
import android.support.v4.app.NavUtils;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import us.paskin.mastery.Proto.Skill;

/**
 * An activity representing a single Skill detail screen. This
 * activity is only used narrow width devices. On tablet-size devices,
 * item details are presented side-by-side with a list of items
 * in a {@link SkillListActivity}.
 */
public class SkillDetailActivity extends AppCompatActivity {
    /**
     * The fragment argument representing the item ID that this fragment
     * represents.
     */
    public static final String ARG_SKILL_INDEX = "skill_index";

    public static final int SKILL_EDITED_RESULT = 1;

    public static final String RESULT_EDITED_SKILL_INDEX = "edited_skill_index";

    /**
     * The dummy content this fragment is presenting.
     */
    private Proto.Skill skill;

    /**
     * This is true if there have been changes that weren't committed.
     */
    private boolean unsavedChanges = false;

    /**
     * This is true if there were changes saved.
     */
    private boolean savedChanges = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getIntent().hasExtra(ARG_SKILL_INDEX)) {
            final int skillIndex = getIntent().getIntExtra(SkillDetailActivity.ARG_SKILL_INDEX, -1);
            skill = SkillData.getInstance().getSkillByIndex(skillIndex);
            assert skill != null;
        }

        setContentView(R.layout.activity_skill_detail);
        Toolbar toolbar = (Toolbar) findViewById(R.id.detail_toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (saveSkill(view)) finish();
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
                updateTitle(editable);
            }
        });
    }

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
            maybeExit();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    void updateTitle(CharSequence title) {
        CollapsingToolbarLayout appBarLayout = (CollapsingToolbarLayout) findViewById(R.id.toolbar_layout);
        if (appBarLayout != null) appBarLayout.setTitle(title);
    }

    // Returns true on success.
    boolean saveSkill(View view) {
        if (!unsavedChanges) return true;
        SkillData data = new SkillData();
        data.addSkill(Skill.newBuilder().setName("foo").build());
        unsavedChanges = false;
        savedChanges = true;
        Toast.makeText(getApplicationContext(), R.string.saved_skill, Toast.LENGTH_SHORT).show();
        return true;
    }

    /**
     * Determines if it is safe to exit.
     */
    private void maybeExit() {
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

    @Override
    public void finish() {
        Intent intent = new Intent();
        if (savedChanges) {
            intent.putExtra(RESULT_EDITED_SKILL_INDEX, 1);  // TODO
            setResult(SKILL_EDITED_RESULT, intent);
        }
        super.finish();
    }

    // Confirms if it is safe to exit before doing so.
    @Override
    public void onBackPressed() {
        maybeExit();
    }
}
