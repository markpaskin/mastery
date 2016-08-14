package us.paskin.mastery;

import android.app.Activity;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.NotificationCompat;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.format.DateUtils;
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

import java.util.Date;
import java.util.LinkedList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

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
     * The ID of the notification posted by this activity to show the current skill, or when the
     * user should move to the next slot or stop practicing.
     */
    public static int STATUS_NOTIFICATION_ID = 1;

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
     * The keys for the notification preferences.
     */
    public static String PREF_ENABLE_NOTIFICATIONS = "enable_practice_notifications";

    /**
     * Cached preferences.
     */
    private boolean enableNotifications;

    /**
     * The current mode.
     */
    private static final int PAUSE = 1;
    private static final int PLAY = 2;

    private int mode = PAUSE;
    private static final String STATE_mode = "mode";

    /**
     * If the mode is PLAY, then this is when we entered that mode.
     */
    private Date practicingSince;
    private static final String STATE_practicingSince = "practicingSince";

    /**
     * The total number of seconds the skill has been practiced, excluding the current play interval.
     */
    private long prevPracticeSecs;

    /**
     * Used to update the display while practicing.
     */
    private Timer durationDisplayUpdateTimer;

    /**
     * This is the table used to render the skill groups this skill is in.
     */
    EditableList skillGroupList;

    MenuItem revertMenuItem;

    FloatingActionButton playPauseButton;

    TextView lastPracticedText;
    TextView durationPracticedText;

    /**
     * Called to save the activity's state.
     */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putByteArray(STATE_skillBuilder, skillBuilder.build().toByteArray());
        outState.putBoolean(STATE_unsavedChanges, unsavedChanges);
        outState.putBoolean(STATE_savedChanges, savedChanges);
        outState.putInt(STATE_mode, mode);
        if (mode == PLAY) {
            outState.putLong(STATE_practicingSince, practicingSince.getTime());
        }
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

        // Cache the preferences.
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        enableNotifications = sharedPreferences.getBoolean(PREF_ENABLE_NOTIFICATIONS, true);

        // Initialize the state.
        Proto.Skill skill = null;  // null if addingSkill
        mode = PAUSE;
        if (savedInstanceState != null) {
            unsavedChanges = savedInstanceState.getBoolean(STATE_unsavedChanges);
            savedChanges = savedInstanceState.getBoolean(STATE_savedChanges);
            try {
                skillBuilder = Proto.Skill.parseFrom(savedInstanceState.getByteArray(STATE_skillBuilder)).toBuilder();
            } catch (InvalidProtocolBufferException x) {
                throw new InternalError("cannot parse protocol buffer");
            }
            skill = skillBuilder.build();
            mode = savedInstanceState.getInt(STATE_mode);
            if (mode == PLAY) {
                practicingSince = new Date(savedInstanceState.getLong(STATE_practicingSince));
            }
        } else if (addingSkill) {
            skillBuilder = Proto.Skill.newBuilder();
        } else {
            skill = model.getSkillById(skillId);
            skillBuilder = skill.toBuilder();
        }
        prevPracticeSecs = skillBuilder.getSecondsPracticed();

        setContentView(R.layout.activity_skill_detail);
        Toolbar toolbar = (Toolbar) findViewById(R.id.detail_toolbar);
        setSupportActionBar(toolbar);

        // Show the Up button in the action bar.
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        EditText nameEditText = ((EditText) findViewById(R.id.skill_name_edit_text));
        lastPracticedText = ((TextView) findViewById(R.id.last_practiced));
        durationPracticedText = ((TextView) findViewById(R.id.duration_practiced));

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

        // Initialize the play/pause button.
        playPauseButton = (FloatingActionButton) findViewById(R.id.fab);
        playPauseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                handlePlayPauseButtonClick();
            }
        });
        if (mode == PLAY) {
            playPauseButton.setImageResource(R.drawable.pause);
        } else {
            playPauseButton.setImageResource(R.drawable.play);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        if (mode == PLAY) startDurationUpdates();
        clearCurrentNotifications();
    }

    synchronized void handlePlayPauseButtonClick() {
        switch (mode) {
            case PLAY:
                pause();
                break;
            case PAUSE:
                play();
                break;
        }
    }

    /**
     * Called to start PLAY mode.
     */
    void play() {
        if (!saveSkill()) return;
        mode = PLAY;
        practicingSince = new Date();
        playPauseButton.setImageResource(R.drawable.pause);
        startDurationUpdates();
        lastPracticedText.setVisibility(View.VISIBLE);
        durationPracticedText.setVisibility(View.VISIBLE);
        lastPracticedText.setVisibility(View.GONE);
    }

    void startDurationUpdates() {
        durationDisplayUpdateTimer = new Timer();
        final long msInSec = TimeUnit.SECONDS.toMillis(1);
        durationDisplayUpdateTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        SkillDetailActivity.this.updateDuration();
                    }
                });
            }
        }, msInSec, msInSec);
    }

    void stopDurationUpdates() {
        if (durationDisplayUpdateTimer == null) return;
        durationDisplayUpdateTimer.cancel();
        durationDisplayUpdateTimer.purge();
    }

    /**
     * Returns the number of seconds that the skill has been practiced.
     */
    long getTotalSecondsPracticed() {
        int secondsPracticed = 0;
        if (practicingSince != null) {
            Date now = new Date();
            final long millisPracticed = now.getTime() - practicingSince.getTime();
            secondsPracticed = (int) TimeUnit.MILLISECONDS.toSeconds(millisPracticed);
        }
        return secondsPracticed + prevPracticeSecs;
    }

    synchronized void updateDuration() {
        if (practicingSince == null) return;
        durationPracticedText.setTypeface(null, Typeface.BOLD);
        durationPracticedText.setText(String.format(getResources().getString(R.string.duration_practiced_text),
                DateUtils.formatElapsedTime(getTotalSecondsPracticed())));
    }

    /**
     * Cancels any existing or scheduled notifications.
     */
    void clearCurrentNotifications() {
        NotificationManager mNotifyMgr =
                (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        mNotifyMgr.cancel(STATUS_NOTIFICATION_ID);
    }

    /**
     * Generates a notification that shows that a practice is ongoing.
     */
    private void startPracticingNotification() {
        if (!enableNotifications) return;
        Intent notificationIntent = new Intent(this, SkillDetailActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent intent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
        NotificationCompat.Builder mBuilder =
                (NotificationCompat.Builder) new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.play)
                        .setContentTitle(getResources().getString(R.string.status_notification_title))
                        .setContentText(skillBuilder.getName())
                        .setContentIntent(intent)
                        .setAutoCancel(false);
        NotificationManager mNotifyMgr =
                (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        mNotifyMgr.notify(STATUS_NOTIFICATION_ID, mBuilder.build());
    }

    /**
     * Called to start PAUSE mode.  (This has nothing to do with onPause.)
     */
    synchronized void pause() {
        mode = PAUSE;
        accumulatePracticeTime();
        playPauseButton.setImageResource(R.drawable.play);
        durationPracticedText.setTypeface(null, Typeface.NORMAL);
        stopDurationUpdates();
        clearCurrentNotifications();
    }

    synchronized void accumulatePracticeTime() {
        if (practicingSince != null) {
            Date now = new Date();
            final long millisPracticed = now.getTime() - practicingSince.getTime();
            final int secondsPracticed = (int) TimeUnit.MILLISECONDS.toSeconds(millisPracticed);
            prevPracticeSecs += secondsPracticed;
            model.addPracticeSecondsToSkill(secondsPracticed, skillId);
            saveSkill();
        }
        practicingSince = null;
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
     *
     */
    @Override
    public void onStop() {
        super.onStop();
        stopDurationUpdates();
        if (mode == PLAY) startPracticingNotification();
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
     * Finishes if any pending practice is ended and any pending changes can be saved.
     */
    private void maybeFinish() {
        if (mode == PLAY) {
            new AlertDialog.Builder(this)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setTitle(R.string.cancel_session_title)
                    .setMessage(R.string.cancel_session_detail)
                    .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            pause();
                            if (saveSkill()) finish();
                        }
                    })
                    .setNegativeButton(R.string.no, null)
                    .show();
        } else {
            if (saveSkill()) finish();
        }
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
