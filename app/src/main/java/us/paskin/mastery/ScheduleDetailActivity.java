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

import java.util.LinkedList;

/**
 * An activity representing a single Schedule detail screen. This
 * activity is only used narrow width devices. On tablet-size devices,
 * item details are presented side-by-side with a list of items
 * in a {@link ScheduleListActivity}.
 */
public class ScheduleDetailActivity extends AppCompatActivity {
    /**
     * The intent arguments representing the index and ID of the schedule being edited/added.
     * They are both an input and output argument.  If ID is missing, then the request is
     * to add a new schedule.  POSITION is an optional argument used by callers for whom it
     * is convenient to get the position returned as a result.  If ID is -1 on exit and was
     * not -1 on entry, then the schedule has been deleted.
     */
    public static final String ARG_SCHEDULE_ID = "schedule_id";
    public static final String ARG_SCHEDULE_POSITION = "schedule_pos";

    /**
     * This intent type is used to identify results from child intents.
     */
    public static final int ADD_SLOT = 1;

    /**
     * True if we're adding a new schedule; false if we're editing one.
     */
    private boolean addingSchedule;

    /**
     * The application model.
     */
    private Model model;

    /**
     * The index of the schedule being edited (or -1 if it's being added).
     */
    private int scheduleIndex = -1;

    /**
     * The ID of the schedule being edited (or -1 if it's being added).
     */
    private long scheduleId = -1;

    /**
     * The schedule before any updates.  This is null if a new schedule is being added.
     */
    private Proto.Schedule schedule;

    /**
     * The builder that is used to update the schedule.  Its fields reflect the model entered by the user.
     */
    private Proto.Schedule.Builder scheduleBuilder;

    /**
     * This is true if there have been changes that weren't committed.
     */
    private boolean unsavedChanges = false;

    /**
     * This is true if there were changes saved.
     */
    private boolean savedChanges = false;

    /**
     * This is the table used to render the schedule groups this schedule is in.
     */
    EditableList slotList;

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
        addingSchedule = !getIntent().hasExtra(ARG_SCHEDULE_POSITION);
        if (!addingSchedule) {
            scheduleIndex = getIntent().getIntExtra(ScheduleDetailActivity.ARG_SCHEDULE_POSITION, -1);
            scheduleId = getIntent().getLongExtra(ScheduleDetailActivity.ARG_SCHEDULE_ID, -1);
            schedule = model.getScheduleById(scheduleId);
            scheduleBuilder = schedule.toBuilder();
        } else {
            scheduleBuilder = Proto.Schedule.newBuilder();
        }

        setContentView(R.layout.activity_schedule_detail);
        Toolbar toolbar = (Toolbar) findViewById(R.id.detail_toolbar);
        setSupportActionBar(toolbar);

        // Show the Up button in the action bar.
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        EditText nameEditText = ((EditText) findViewById(R.id.schedule_name));

        // Initialize the controls with pre-existing values or defaults.
        if (schedule != null) {
            updateTitle(schedule.getName());
            nameEditText.setText(schedule.getName());
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
                scheduleBuilder.setName(editable.toString());
                updateTitle(editable);
            }
        });

        slotList = new EditableList(
                (TableLayout) findViewById(R.id.parent_group_list),
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        // TODO: select
                    }
                });

        if (schedule != null) {
            for (Proto.Schedule.Slot slot : schedule.getSlotList()) {
                addSlotToTable(slot);
            }
        }
    }

    /**
     * Handles results from intents launched by this activity.
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != Activity.RESULT_OK) return;
        if (requestCode == ADD_SLOT) {
            //TODO
        }
    }

    /**
     * Adds an entry to the parent schedule group table.
     */
    private void addSlotToTable(Proto.Schedule.Slot slot) {
        slotList.addItem(
                model.getSkillGroupById(slot.getGroupId()).getName(),
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        // TODO: select group again
                    }
                },
                new Runnable() {
                    @Override
                    public void run() {
                        // TODO remove slot
                    }
                });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.schedule_detail, menu);
        if (addingSchedule) menu.findItem(R.id.delete_schedule).setVisible(false);
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
        } else if (id == R.id.delete_schedule) {
            handleDeleteSchedule();
            return true;
        } else if (id == R.id.revert_changes) {
            handleRevertChanges();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Called if the user requests to delete this schedule.
     */
    void handleDeleteSchedule() {
        new AlertDialog.Builder(this)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle(R.string.delete_schedule_confirm_title)
                .setMessage(R.string.cannot_undo_detail)
                .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        ScheduleDetailActivity.this.deleteAndFinish();
                    }
                })
                .setNegativeButton(R.string.no, null)
                .show();
    }

    void deleteAndFinish() {
        if (!addingSchedule) model.deleteSchedule(scheduleId);
        scheduleId = -1;
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
                        ScheduleDetailActivity.this.finish();
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
        if (!scheduleBuilder.hasName() || scheduleBuilder.getName().isEmpty()) {
            new AlertDialog.Builder(this)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setTitle(R.string.missing_schedule_name_title)
                    .setMessage(R.string.missing_schedule_name_detail)
                    .setNeutralButton(R.string.ok, null)
                    .show();
            return false;
        }
        if (scheduleBuilder.getSlotList().isEmpty()) {
            new AlertDialog.Builder(this)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setTitle(R.string.no_schedule_slots_title)
                    .setMessage(R.string.no_schedule_slots_detail)
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
    boolean saveSchedule() {
        if (!unsavedChanges) return true;
        if (!validate()) return false;
        if (!addingSchedule) {
            model.updateSchedule(scheduleId, scheduleBuilder.build());
            Toast.makeText(getApplicationContext(), R.string.saved_schedule, Toast.LENGTH_SHORT).show();
        } else {
            scheduleId = model.addSchedule(scheduleBuilder.build());
            Toast.makeText(getApplicationContext(), R.string.added_schedule, Toast.LENGTH_SHORT).show();
        }
        unsavedChanges = false;
        savedChanges = true;
        return true;
    }

    /**
     * Finishes if any pending changes can be saved.
     */
    private void maybeFinish() {
        if (saveSchedule()) finish();
    }

    /**
     * Exits this activity, populating the intent results.
     */
    @Override
    public void finish() {
        Intent intent = new Intent();
        if (savedChanges) {
            intent.putExtra(ARG_SCHEDULE_POSITION, scheduleIndex);
            intent.putExtra(ARG_SCHEDULE_ID, scheduleId);
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
