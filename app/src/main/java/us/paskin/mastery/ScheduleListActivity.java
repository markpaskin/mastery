package us.paskin.mastery;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.protobuf.InvalidProtocolBufferException;

import java.util.concurrent.TimeUnit;

/**
 * An activity representing a list of Schedules. This activity
 * has different presentations for handset and tablet-size devices. On
 * handsets, the activity presents a list of items, which when touched,
 * lead to a {@link ScheduleDetailActivity} representing
 * item details. On tablets, the activity presents the list of items and
 * item details side-by-side using two vertical panes.
 */
public class ScheduleListActivity extends DrawerActivity {

    /**
     * These are intent request types, used for interpreting results from child intents.
     */
    private static final int REQ_EDIT_SCHEDULE = 1;
    private static final int REQ_ADD_SCHEDULE = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_schedule_list);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        // Override the title, which was set to the app title in the manifest so it looks right in the launcher.
        toolbar.setTitle(getResources().getString(R.string.title_schedule_list));
        super.onCreateDrawer();

        View recyclerView = findViewById(R.id.schedule_list);
        assert recyclerView != null;
        setupRecyclerView((RecyclerView) recyclerView);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.schedule_list, menu);
        return super.onCreateOptionsMenu(menu);
    }

    /**
     * This is invoked if an option is select, e.g., the left arrow to return.
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.add_schedule) {
            handleAddSchedule();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void handleAddSchedule() {
        // Start the detail activity with no ARG_ITEM_ID to create a new one.
        Intent intent = new Intent(this, ScheduleDetailActivity.class);
        intent.removeExtra(ScheduleDetailActivity.ARG_SCHEDULE_POSITION);
        intent.removeExtra(ScheduleDetailActivity.ARG_SCHEDULE_ID);
        ScheduleListActivity.this.startActivityForResult(intent, REQ_ADD_SCHEDULE);
    }

    private void setupRecyclerView(@NonNull RecyclerView recyclerView) {
        recyclerView.setAdapter(new SimpleItemRecyclerViewAdapter(this));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != Activity.RESULT_OK) return;
        final int scheduleIndex = data.getIntExtra(ScheduleDetailActivity.ARG_SCHEDULE_POSITION, -1);
        final long scheduleId = data.getLongExtra(ScheduleDetailActivity.ARG_SCHEDULE_ID, -1);
        final boolean deleted = scheduleId == -1;
        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.schedule_list);
        SimpleItemRecyclerViewAdapter adaptor =
                (SimpleItemRecyclerViewAdapter) recyclerView.getAdapter();
        adaptor.refreshView();
        adaptor.notifyDataSetChanged();
/*
        switch (requestCode) {
            case ScheduleDetailActivity.REQ_EDIT_SCHEDULE:
                System.out.println("notify for position " + scheduleIndex);
                if (deleted) adaptor.notifyItemRemoved(scheduleIndex);
                else adaptor.notifyItemChanged(scheduleIndex);
                break;
            case ScheduleDetailActivity.REQ_ADD_SCHEDULE:
                adaptor.notifyDataSetChanged();
                break;
        }
        */
    }

    public class SimpleItemRecyclerViewAdapter
            extends RecyclerView.Adapter<SimpleItemRecyclerViewAdapter.ViewHolder> {

        private final Model data;
        private Cursor cursor;

        public SimpleItemRecyclerViewAdapter(Context context) {
            setHasStableIds(true);
            data = Model.getInstance(context);
            refreshView();
        }

        public void refreshView() {
            if (cursor != null) cursor.close();
            cursor = data.getScheduleList();
        }

        @Override
        public long getItemId(int position) {
            cursor.moveToPosition(position);
            return cursor.getLong(0);
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.schedule_list_item, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(final ViewHolder holder, final int position) {
            cursor.moveToPosition(position);
            final long id = cursor.getLong(0);
            try {
                final Proto.Schedule schedule = Proto.Schedule.parseFrom(cursor.getBlob(1));
                holder.setData(schedule);
            } catch (InvalidProtocolBufferException x) {
                throw new InternalError("failed to parse protocol buffer");
            }
            holder.view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Context context = v.getContext();
                    Intent intent = new Intent(context, ScheduleDetailActivity.class);
                    intent.putExtra(ScheduleDetailActivity.ARG_SCHEDULE_POSITION, position);
                    intent.putExtra(ScheduleDetailActivity.ARG_SCHEDULE_ID, id);
                    ScheduleListActivity.this.startActivityForResult(intent, REQ_EDIT_SCHEDULE);
                }
            });
        }

        @Override
        public int getItemCount() {
            return cursor.getCount();
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            public final View view;
            public final TextView nameTextView;
            public final TextView durationTextView;

            public ViewHolder(View view) {
                super(view);
                this.view = view;
                nameTextView = (TextView) view.findViewById(R.id.schedule_name);
                durationTextView = (TextView) view.findViewById(R.id.schedule_duration);
            }

            public void setData(Proto.Schedule s) {
                nameTextView.setText(s.getName());
                int totalDurationSecs = 0;
                for (Proto.Schedule.Slot slot : s.getSlotList()) {
                    totalDurationSecs += slot.getDurationInSecs();
                }
                int durationInMins = (int) TimeUnit.SECONDS.toMinutes(totalDurationSecs);
                durationTextView.setText(getResources().getQuantityString(R.plurals.num_min, durationInMins, durationInMins));
            }

            @Override
            public String toString() {
                return super.toString() + " '" + nameTextView.getText() + "'";
            }
        }
    }
}
