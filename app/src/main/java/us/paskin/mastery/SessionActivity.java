package us.paskin.mastery;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.NotificationCompat;
import android.support.v7.widget.Toolbar;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TableLayout;
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
    private Timer timer;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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

    void play() {
        mode = Mode.PLAY;
        practicingSince = new Date();
        playPauseButton.setImageResource(R.drawable.pause);
        startDurationUpdates();
    }

    void startDurationUpdates() {
        timer = new Timer();
        final long msInSec = TimeUnit.SECONDS.toMillis(1);
        timer.scheduleAtFixedRate(new TimerTask() {
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
        timer.cancel();
        timer.purge();
    }

    synchronized void updateDuration() {
        if (practicingSince == null) return;
        TextView textView = slotDurationTextViewList.get(curSlotIndex);
        textView.setTypeface(null, Typeface.BOLD);
        Date now = new Date();
        final long millisPracticed = now.getTime() - practicingSince.getTime();
        final int secondsPracticed = (int) TimeUnit.MILLISECONDS.toSeconds(millisPracticed);
        final int secondsPreviouslyPracticed = storedDurations[curSlotIndex];
        final int totalSecondsPracticed = secondsPracticed + secondsPreviouslyPracticed;
        textView.setText(DateUtils.formatElapsedTime(totalSecondsPracticed));
        if (totalSecondsPracticed > schedule.getSlotList().get(curSlotIndex).getDurationInSecs()) {
            generateNotification();
        }
    }

    /**
     * Generates a notification that it's time to move on to the next slot.
     */
    private void generateNotification() {
        Intent notificationIntent = new Intent(this, SessionActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent intent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
        NotificationCompat.Builder mBuilder =
                (NotificationCompat.Builder) new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.play)
                        .setContentTitle(getResources().getString(R.string.next_notification_title))
                        .setContentText(getResources().getString(R.string.next_notification_text))
                        .setContentIntent(intent);
        int mNotificationId = 001;
        NotificationManager mNotifyMgr =
                (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        mNotifyMgr.notify(mNotificationId, mBuilder.build());
    }

    synchronized void pause() {
        mode = Mode.PAUSE;
        accumulatePracticeTime();
        playPauseButton.setImageResource(R.drawable.play);
        slotDurationTextViewList.get(curSlotIndex).setTypeface(null, Typeface.NORMAL);
        stopDurationUpdates();
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

    synchronized void handlePrevButtonClick(View view) {
        if (curSlotIndex < 1) throw new InternalError("prev from first");
        pause();
        curSlotIndex -= 1;
        play();
        if (curSlotIndex == 0) prevButton.setEnabled(false);
        nextButton.setEnabled(true);
    }

    synchronized void handleNextButtonClick(View view) {
        int numSlots = session.getSlotList().size();
        if (curSlotIndex >= numSlots) throw new InternalError("next from last");
        pause();
        curSlotIndex += 1;
        play();
        if (curSlotIndex == numSlots - 1) nextButton.setEnabled(false);
        prevButton.setEnabled(true);
    }

    /**
     * Inefficient way to make sure skill names are updated: redraw the page.
     */
    @Override
    public void onResume() {
        super.onResume();
        if (session == null) {
            // Launch a thread to sample the session.
            new Thread(new Runnable() {
                @Override
                public void run() {
                    session = Session.sampleSession(schedule, model);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            layoutSession();
                        }
                    });
                }
            }).start();
        } else {
            layoutSession();
        }
        if (mode == Mode.PLAY) startDurationUpdates();
    }

    /**
     * Called when the session is available to be rendered.
     */
    private void layoutSession() {
        controlsContainer.setVisibility(View.VISIBLE);
        TableLayout container = (TableLayout) findViewById(R.id.session_slots_container);
        container.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(container.getContext());
        int totalDurationInSecs = 0;
        slotDurationTextViewList = new ArrayList<TextView>();
        slotViewList = new ArrayList<View>();
        for (final Session.Slot slot : session.getSlotList()) {
            View slotView = inflater.inflate(R.layout.session_slot, container, false);
            slotViewList.add(slotView);
            TextView skillNameTextView = (TextView) slotView.findViewById(R.id.skill_name);
            TextView duration = (TextView) slotView.findViewById(R.id.slot_duration);
            slotDurationTextViewList.add(duration);
            if (slot.filled()) {
                Proto.Skill skill = model.getSkillById(slot.getSkillId());
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
        }
        TextView totalDurationTextView = (TextView) findViewById(R.id.total_duration);

        final int totalDurationInMinutes = (int) TimeUnit.SECONDS.toMinutes(totalDurationInSecs);
        totalDurationTextView.setText(getResources().getQuantityString(
                R.plurals.num_min, totalDurationInMinutes, totalDurationInMinutes));

    }
}
