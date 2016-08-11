package us.paskin.mastery;

import android.graphics.Typeface;
import android.os.Bundle;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

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
     * The FAB that starts the session.
     */
    private FloatingActionButton fab;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        model = Model.getInstance(this);
        schedule = model.getScheduleById(getIntent().getLongExtra(ARG_SESSION_ID, -1));
        setTitle(R.string.session_activity_title);

        setContentView(R.layout.activity_session);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        fab.setVisibility(View.INVISIBLE);

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
    }

    /**
     * Called when the session is available to be rendered.
     */
    private void layoutSession() {
        fab.setVisibility(View.VISIBLE);
        TableLayout container = (TableLayout) findViewById(R.id.session_slots_container);
        LayoutInflater inflater = LayoutInflater.from(container.getContext());
        int totalDurationInSecs = 0;
        for (Session.Slot slot : session.getSlotList()) {
            View slotView = inflater.inflate(R.layout.session_slot, container, false);
            TextView skillNameTextView = (TextView) slotView.findViewById(R.id.skill_name);
            if (slot.filled()) {
                skillNameTextView.setText(slot.getSkill().getName());
                TextView duration = (TextView) slotView.findViewById(R.id.slot_duration);
                final int durationInMinutes = (int) TimeUnit.SECONDS.toMinutes(slot.getScheduleSlot().getDurationInSecs());
                duration.setText(getResources().getQuantityString(R.plurals.num_min, durationInMinutes, durationInMinutes));
                totalDurationInSecs += slot.getScheduleSlot().getDurationInSecs();
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
