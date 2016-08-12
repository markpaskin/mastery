package us.paskin.mastery;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.v4.view.ViewCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.NotificationCompat;
import android.support.v7.widget.Toolbar;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

public class SessionActivity extends AppCompatActivity {
    /**
     * This activity requires its intent to have the session ID set.
     */
    public static String ARG_SESSION_ID = "session_id";

    /**
     * The ID of the notification posted by this activity when the user should move to the next
     * slot or stop practicing.
     */
    public static int UPDATE_NOTIFICATION_ID = 1;

    /**
     * The ID of the notification that is triggered during practice when the user leaves the activity.
     */
    public static int PRACTICING_NOTIFICATION_ID = 2;

    /**
     * The key to a float preference giving a [0, 1] weight for staleness.
     */
    public static String PREF_STALENESS_IMPORTANCE = "pref_staleness_importance";

    /**
     * The keys for the notification preferences.
     */
    public static String PREF_ENABLE_NOTIFICATIONS = "enable_practice_notifications";
    public static String PREF_NOTIFICATION_RINGTONE = "practice_notification_ringtone";
    public static String PREF_NOTIFICATION_VIBRATE = "practice_notification_vibrate";

    /**
     * The application model.
     */
    private Model model;

    /**
     * The schedule from which the session is built.
     */
    private Proto.Schedule schedule;

    /**
     * The sampled session.
     */
    private Session session = null;

    /**
     * Contains the control button.
     */
    private View controlsContainer;

    /**
     * The play/pause button.
     */
    private ImageButton playPauseButton;
    private ImageButton prevButton;
    private ImageButton nextButton;

    /**
     * The current mode.
     */
    enum Mode {
        PAUSE, PLAY
    }

    private Mode mode = Mode.PAUSE;

    /**
     * The current slot that is being practiced.
     */
    private int curSlotIndex = 0;

    /**
     * If the mode is PLAY, then this is when we entered that mode.
     */
    private Date practicingSince;

    /**
     * These are the amount of practiced seconds that have been stored for each slot.  We keep this
     * so that if the user returns to a slot the display doesn't show them starting from zero.
     */
    private int storedDurations[];

    /**
     * The duration TextViews for each slot.
     */
    private ArrayList<TextView> slotDurationTextViewList;

    /**
     * The views for each slot.
     */
    private ArrayList<View> slotViewList;

    /**
     * Used to format durations.
     */
    final DateUtils dateUtils = new DateUtils();

    /**
     * Used to update the display while practicing.
     */
    private Timer durationDisplayUpdateTimer;

    /**
     * Used to notify the user it's time to move on.
     */
    private Timer nextNotificationTimer;

    /**
     * The cached value of PREF_STALENESS_WEIGHT.
     */
    private float stalenessWeight;

    private boolean enableNotifications;
    private boolean notificationsVibrate;
    private String notificationRingtoneUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // On the first creation, this is null.  If it's not null, the activity is being re-created.
        if (savedInstanceState != null) return;

        model = Model.getInstance(this);
        schedule = model.getScheduleById(getIntent().getLongExtra(ARG_SESSION_ID, -1));
        setTitle(R.string.session_activity_title);

        setContentView(R.layout.activity_session);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        controlsContainer = findViewById(R.id.controls_container);
        controlsContainer.setVisibility(View.INVISIBLE);

        playPauseButton = (ImageButton) findViewById(R.id.play_pause_button);
        playPauseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                handlePlayPauseButtonClick(view);
            }
        });
        prevButton = (ImageButton) findViewById(R.id.prev_button);
        prevButton.setEnabled(false);
        prevButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                handlePrevButtonClick(view);
            }
        });
        nextButton = (ImageButton) findViewById(R.id.next_button);
        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                handleNextButtonClick(view);
            }
        });

        storedDurations = new int[schedule.getSlotList().size()];
    }

    synchronized void handlePlayPauseButtonClick(View view) {
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
        mode = Mode.PLAY;
        practicingSince = new Date();
        playPauseButton.setImageResource(R.drawable.pause);
        startDurationUpdates();
        if (getSlotTotalSecondsPracticed(curSlotIndex) < schedule.getSlotList().get(curSlotIndex).getDurationInSecs()) {
            scheduleNextNotification();
        }
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
                        SessionActivity.this.updateDuration();
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
     * Returns the number of seconds (during this activity) that the slot has been practiced.
     */
    int getSlotTotalSecondsPracticed(int slotIndex) {
        int secondsPracticed = 0;
        if (practicingSince != null && slotIndex == curSlotIndex) {
            Date now = new Date();
            final long millisPracticed = now.getTime() - practicingSince.getTime();
            secondsPracticed = (int) TimeUnit.MILLISECONDS.toSeconds(millisPracticed);
        }
        final int secondsPreviouslyPracticed = storedDurations[slotIndex];
        return secondsPracticed + secondsPreviouslyPracticed;
    }

    synchronized void updateDuration() {
        if (practicingSince == null) return;
        TextView textView = slotDurationTextViewList.get(curSlotIndex);
        textView.setTypeface(null, Typeface.BOLD);
        textView.setText(DateUtils.formatElapsedTime(getSlotTotalSecondsPracticed(curSlotIndex)));
    }

    /**
     * Generates a notification that it's time to move on to the next slot.
     */
    private void generateNextNotification() {
        if (!enableNotifications) return;
        Intent notificationIntent = new Intent(this, SessionActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent intent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
        final boolean atLastSlot = curSlotIndex == schedule.getSlotCount() - 1;
        NotificationCompat.Builder mBuilder =
                (NotificationCompat.Builder) new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.play)
                        .setContentTitle(getResources().getString(R.string.next_notification_title))
                        .setContentText(getResources().getString(
                                atLastSlot ? R.string.practice_complete_detail : R.string.next_notification_text))
                        .setContentIntent(intent)
                        .setAutoCancel(true)
                        .setSound(Uri.parse(notificationRingtoneUri));
        if (notificationsVibrate) mBuilder.setDefaults(Notification.DEFAULT_VIBRATE);
        NotificationManager mNotifyMgr =
                (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        mNotifyMgr.notify(UPDATE_NOTIFICATION_ID, mBuilder.build());
    }

    /**
     * Schedules a "next" notification to be generated when the time for the current slot is up.
     */
    void scheduleNextNotification() {
        if (!enableNotifications) return;
        if (mode != Mode.PLAY) return;
        final int secondsPracticed = getSlotTotalSecondsPracticed(curSlotIndex);
        final int secondsToPractice = schedule.getSlot(curSlotIndex).getDurationInSecs();
        final int secondsLeftToPractice = secondsToPractice - secondsPracticed;
        nextNotificationTimer = new Timer();
        nextNotificationTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        SessionActivity.this.generateNextNotification();
                    }
                });
            }
        }, TimeUnit.SECONDS.toMillis(secondsLeftToPractice));
    }

    /**
     * Cancels any existing or scheduled "next" notifications.
     */
    void cancelNextNotification() {
        if (nextNotificationTimer != null) {
            nextNotificationTimer.cancel();
            nextNotificationTimer.purge();
        }
        NotificationManager mNotifyMgr =
                (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        mNotifyMgr.cancel(UPDATE_NOTIFICATION_ID);
    }

    /**
     * Generates a notification that shows that a practice is ongoing.
     */
    private void startPracticingNotification() {
        if (!enableNotifications) return;
        Intent notificationIntent = new Intent(this, SessionActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent intent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
        NotificationCompat.Builder mBuilder =
                (NotificationCompat.Builder) new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.play)
                        .setContentTitle(getResources().getString(R.string.status_notification_title))
                        .setContentText(session.getSlotList().get(curSlotIndex).getSkill().getName())
                        .setContentIntent(intent)
                        .setAutoCancel(false);
        NotificationManager mNotifyMgr =
                (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        mNotifyMgr.notify(PRACTICING_NOTIFICATION_ID, mBuilder.build());
    }

    /**
     * Cancels the notification created by startPracticingNotification.
     */
    void stopPracticingNotification() {
        if (!enableNotifications) return;
        NotificationManager mNotifyMgr =
                (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        mNotifyMgr.cancel(PRACTICING_NOTIFICATION_ID);
    }


    /**
     * Called to start PAUSE mode.  (This has nothing to do with onPause.)
     */
    synchronized void pause() {
        mode = Mode.PAUSE;
        accumulatePracticeTime();
        playPauseButton.setImageResource(R.drawable.play);
        slotDurationTextViewList.get(curSlotIndex).setTypeface(null, Typeface.NORMAL);
        stopDurationUpdates();
        cancelNextNotification();
    }

    synchronized void accumulatePracticeTime() {
        Session.Slot slot = session.getSlotList().get(curSlotIndex);
        if (slot.filled() && practicingSince != null) {
            Date now = new Date();
            final long millisPracticed = now.getTime() - practicingSince.getTime();
            final int secondsPracticed = (int) TimeUnit.MILLISECONDS.toSeconds(millisPracticed);
            long skillId = session.getSlotList().get(curSlotIndex).getSkillId();
            model.addPracticeSecondsToSkill(secondsPracticed, skillId);
            storedDurations[curSlotIndex] += secondsPracticed;
        }
        practicingSince = null;
    }

    /**
     * Visually alters the appearance of a slot to be (not) emphasized.
     *
     * @param slotIndex
     * @param emphasize
     */
    private void setSlotEmphasis(int slotIndex, boolean emphasize) {
        View view = slotViewList.get(slotIndex);
        ViewCompat.setElevation(view, emphasize ? 25.0f : 5.0f);
    }

    synchronized void handlePrevButtonClick(View view) {
        if (curSlotIndex < 1) throw new InternalError("prev from first");
        pause();
        setSlotEmphasis(curSlotIndex, false);
        curSlotIndex -= 1;
        setSlotEmphasis(curSlotIndex, true);
        if (curSlotIndex == 0) prevButton.setEnabled(false);
        nextButton.setEnabled(true);
    }

    synchronized void handleNextButtonClick(View view) {
        int numSlots = session.getSlotList().size();
        if (curSlotIndex >= numSlots) throw new InternalError("next from last");
        pause();
        setSlotEmphasis(curSlotIndex, false);
        curSlotIndex += 1;
        setSlotEmphasis(curSlotIndex, true);
        if (curSlotIndex == numSlots - 1) nextButton.setEnabled(false);
        prevButton.setEnabled(true);
    }

    /**
     * Inefficient way to make sure skill names are updated: redraw the page.
     */
    @Override
    public void onStart() {
        super.onStart();
        // Make sure we have a rendered session.
        if (session == null) {
            // We don't have a session yet.  Launch a thread to sample one and then render it.
            new Thread(new Runnable() {
                @Override
                public void run() {
                    session = Session.sampleSession(schedule, model, stalenessWeight);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            layoutSession();
                        }
                    });
                }
            }).start();
        } else {
            // Update the session in case the skills were updated by the previous activity.
            session.refill();
            layoutSession();
        }
        if (mode == Mode.PLAY) startDurationUpdates();
        stopPracticingNotification();

        // Get the staleness importance preference.  It should be in [0, 1].
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        stalenessWeight = sharedPreferences.getFloat(PREF_STALENESS_IMPORTANCE, 0.5f);
        stalenessWeight = Math.max(0.0f, stalenessWeight);
        stalenessWeight = Math.min(1.0f, stalenessWeight);

        enableNotifications = sharedPreferences.getBoolean(PREF_ENABLE_NOTIFICATIONS, true);
        notificationsVibrate = sharedPreferences.getBoolean(PREF_NOTIFICATION_VIBRATE, true);
        notificationRingtoneUri = sharedPreferences.getString(PREF_NOTIFICATION_RINGTONE,
                Settings.System.DEFAULT_NOTIFICATION_URI.toString());
    }

    boolean startedPractice() {
        if (practicingSince != null) return true;
        for (int secs : storedDurations) if (secs > 0) return true;
        return false;
    }

    boolean finishedPractice() {
        final int lastSlotIndex = schedule.getSlotCount() - 1;
        return getSlotTotalSecondsPracticed(lastSlotIndex) >= schedule.getSlot(lastSlotIndex).getDurationInSecs();
    }

    /**
     * Called if the user requests to revert changes.
     */
    void confirmExit() {
        if (!startedPractice() || finishedPractice()) {
            finish();
            return;
        }
        new AlertDialog.Builder(this)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle(R.string.cancel_session_title)
                .setMessage(R.string.cancel_session_detail)
                .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        SessionActivity.this.finish();
                    }
                })
                .setNegativeButton(R.string.no, null)
                .show();
    }

    // Confirms if it is safe to exit before doing so.
    @Override
    public void onBackPressed() {
        confirmExit();
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
            confirmExit();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void finish() {
        super.finish();
        pause();
    }

    /**
     *
     */
    @Override
    public void onStop() {
        super.onStop();
        stopDurationUpdates();
        if (mode == Mode.PLAY) startPracticingNotification();
    }

    /**
     *
     */
    @Override
    public void onDestroy() {
        super.onDestroy();
        stopDurationUpdates();
        cancelNextNotification();
        stopPracticingNotification();
    }

    /**
     * Called when the session is available to be rendered.
     */
    private void layoutSession() {
        controlsContainer.setVisibility(View.VISIBLE);
        LinearLayout container = (LinearLayout) findViewById(R.id.session_slots_container);
        container.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(container.getContext());
        int totalDurationInSecs = 0;
        slotDurationTextViewList = new ArrayList<TextView>();
        slotViewList = new ArrayList<View>();
        int slotIndex = 0;
        for (final Session.Slot slot : session.getSlotList()) {
            View slotView = inflater.inflate(R.layout.session_slot, container, false);
            slotViewList.add(slotView);
            TextView skillNameTextView = (TextView) slotView.findViewById(R.id.skill_name);
            TextView duration = (TextView) slotView.findViewById(R.id.slot_duration);
            slotDurationTextViewList.add(duration);
            if (slot.filled()) {
                Proto.Skill skill = slot.getSkill();
                skillNameTextView.setText(skill.getName());
                final int durationInMinutes = (int) TimeUnit.SECONDS.toMinutes(slot.getScheduleSlot().getDurationInSecs());
                duration.setText(getResources().getQuantityString(R.plurals.num_min, durationInMinutes, durationInMinutes));
                totalDurationInSecs += slot.getScheduleSlot().getDurationInSecs();
                slotView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Intent intent = new Intent(SessionActivity.this, SkillDetailActivity.class);
                        intent.putExtra(SkillDetailActivity.ARG_SKILL_ID, slot.getSkillId());
                        intent.putExtra(SkillDetailActivity.ARG_DISABLE_DELETE, true);
                        SessionActivity.this.startActivity(intent);
                    }
                });
            } else {
                skillNameTextView.setText(R.string.slot_not_filled);
                skillNameTextView.setTypeface(null, Typeface.ITALIC);
                skillNameTextView.setBackgroundColor(getResources().getColor(R.color.error_background));
            }
            TextView groupNameTextView = (TextView) slotView.findViewById(R.id.group_name);
            groupNameTextView.setText(model.getSkillGroupById(slot.getScheduleSlot().getGroupId()).getName());
            container.addView(slotView);
            setSlotEmphasis(slotIndex, slotIndex == curSlotIndex);
            ++slotIndex;
        }

        TextView totalDurationTextView = (TextView) findViewById(R.id.total_duration);

        final int totalDurationInMinutes = (int) TimeUnit.SECONDS.toMinutes(totalDurationInSecs);
        totalDurationTextView.setText(getResources().getQuantityString(
                R.plurals.num_min, totalDurationInMinutes, totalDurationInMinutes));

        updateDuration();
    }
}
