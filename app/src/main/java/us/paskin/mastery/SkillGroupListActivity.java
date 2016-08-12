package us.paskin.mastery;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.protobuf.InvalidProtocolBufferException;

/**
 * An activity representing a list of Skills. This activity
 * has different presentations for handset and tablet-size devices. On
 * handsets, the activity presents a list of items, which when touched,
 * lead to a {@link SkillDetailActivity} representing
 * item details. On tablets, the activity presents the list of items and
 * item details side-by-side using two vertical panes.
 */
public class SkillGroupListActivity extends DrawerActivity {

    /**
     * If this boolean Intent argument is set to true, then tapping on a skill group
     * returns the ID of that group as a result to the calling activity via the RES_SKILL_GROUP_ID
     * extra.  This activity also permits adding new skills in this mode, in which case the added
     * group is selected.
     */
    public static final String ARG_MODE_SELECT = "select";

    /**
     * When run in "select" mode, this activity will return the selected group's ID with this name.
     */
    public static final String ARG_SELECTED_SKILL_GROUP_ID = "selected_group_id";

    /**
     * If supplied, then this int extra will be passed through from input to output.
     */
    public static final String ARG_POSITION = "position";

    /**
     * These are intent request types.  They are used to process results from child intents.
     */
    private static final int REQ_EDIT_SKILL_GROUP = 1;
    private static final int REQ_ADD_SKILL_GROUP = 2;

    /**
     * Indicates if this activity was launched in "select" mode.
     */
    private boolean selectMode = false;

    /**
     * This is the value of ARG_POSITION if it is supplied or -1 otherwise.
     */
    private int position;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_skill_group_list);

        selectMode = getIntent().getBooleanExtra(ARG_MODE_SELECT, false);
        position = getIntent().getIntExtra(ARG_POSITION, -1);

        if (selectMode) setTitle(R.string.select_skill_group_title);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setTitle(getTitle());
        if (selectMode) {
            // Show the Up button in the action bar.
            ActionBar actionBar = getSupportActionBar();
            if (actionBar != null) {
                actionBar.setDisplayHomeAsUpEnabled(true);
            }
        } else {
            super.onCreateDrawer();
        }

        View recyclerView = findViewById(R.id.skill_group_list);
        assert recyclerView != null;
        setupRecyclerView((RecyclerView) recyclerView);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.skill_group_list, menu);
        return super.onCreateOptionsMenu(menu);
    }

    /**
     * This is invoked if an option is select, e.g., the left arrow to return.
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            finish();
            return true;
        } else if (id == R.id.add_skill_group) {
            handleAddSkillGroup();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void handleAddSkillGroup() {
        Intent intent = new Intent(this, SkillGroupDetailActivity.class);
        intent.removeExtra(SkillGroupDetailActivity.ARG_SKILL_GROUP_POSITION);
        intent.removeExtra(SkillGroupDetailActivity.ARG_SKILL_GROUP_ID);
        SkillGroupListActivity.this.startActivityForResult(intent, REQ_ADD_SKILL_GROUP);
    }

    private void setupRecyclerView(@NonNull RecyclerView recyclerView) {
        recyclerView.setAdapter(new SimpleItemRecyclerViewAdapter(this));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != Activity.RESULT_OK) return;

        // If this activity is run in "select" mode and we've just added a new group, auto-select it.
        if (requestCode == REQ_ADD_SKILL_GROUP && selectMode) {
            final long skillGroupId = data.getLongExtra(SkillGroupDetailActivity.ARG_SKILL_GROUP_ID, -1);
            returnSkillGroupId(skillGroupId);
            return;
        }

        final int skillGroupIndex = data.getIntExtra(SkillGroupDetailActivity.ARG_SKILL_GROUP_POSITION, -1);
        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.skill_group_list);
        SimpleItemRecyclerViewAdapter adaptor =
                (SimpleItemRecyclerViewAdapter) recyclerView.getAdapter();
        adaptor.refreshView();
        adaptor.notifyDataSetChanged();
/*
        switch (requestCode) {
            case SkillGroupDetailActivity.REQ_EDIT_SKILL_GROUP:
                adaptor.notifyItemChanged(skillGroupIndex);
                break;
            case SkillGroupDetailActivity.REQ_ADD_SKILL_GROUP:
                adaptor.notifyDataSetChanged();
                break;
        }
        */
    }

    /**
     * Finishes this activity, returning the selected group ID.
     *
     * @param id
     */
    private void returnSkillGroupId(long id) {
        Intent intent = new Intent();
        intent.putExtra(ARG_SELECTED_SKILL_GROUP_ID, id);
        intent.putExtra(ARG_POSITION, position);
        setResult(Activity.RESULT_OK, intent);
        finish();
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
            cursor = data.getSkillGroupList();
        }

        @Override
        public long getItemId(int position) {
            cursor.moveToPosition(position);
            return cursor.getLong(0);
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.skill_group_list_item, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(final ViewHolder holder, final int position) {
            cursor.moveToPosition(position);
            final long id = cursor.getLong(0);
            try {
                final Proto.SkillGroup skillGroup = Proto.SkillGroup.parseFrom(cursor.getBlob(1));
                holder.setData(skillGroup);
            } catch (InvalidProtocolBufferException x) {
                throw new InternalError("failed to parse protocol buffer");
            }
            holder.view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (selectMode) {
                        returnSkillGroupId(id);
                        return;
                    }
                    Context context = v.getContext();
                    Intent intent = new Intent(context, SkillGroupDetailActivity.class);
                    intent.putExtra(SkillGroupDetailActivity.ARG_SKILL_GROUP_POSITION, position);
                    intent.putExtra(SkillGroupDetailActivity.ARG_SKILL_GROUP_ID, id);
                    SkillGroupListActivity.this.startActivityForResult(intent, REQ_EDIT_SKILL_GROUP);
                }
            });
        }

        @Override
        public int getItemCount() {
            return cursor.getCount();
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            public final View view;
            public final TextView text;

            public ViewHolder(View view) {
                super(view);
                this.view = view;
                text = (TextView) view.findViewById(R.id.skill_group_item_text);
            }

            public void setData(Proto.SkillGroup g) {
                text.setText(g.getName());
            }

            @Override
            public String toString() {
                return super.toString() + " '" + text.getText() + "'";
            }
        }
    }
}
