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

/**
 * An activity representing a list of Skills. This activity
 * has different presentations for handset and tablet-size devices. On
 * handsets, the activity presents a list of items, which when touched,
 * lead to a {@link SkillDetailActivity} representing
 * item details. On tablets, the activity presents the list of items and
 * item details side-by-side using two vertical panes.
 */
public class SkillListActivity extends DrawerActivity {

    /**
     * These are intent request types, used for interpreting results from child intents.
     */
    private static final int REQ_EDIT_SKILL = 1;
    private static final int REQ_ADD_SKILL = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_skill_list);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setTitle(getTitle());
        super.onCreateDrawer();

        View recyclerView = findViewById(R.id.skill_list);
        assert recyclerView != null;
        setupRecyclerView((RecyclerView) recyclerView);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.skill_list, menu);
        return super.onCreateOptionsMenu(menu);
    }

    /**
     * This is invoked if an option is select, e.g., the left arrow to return.
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.add_skill) {
            handleAddSkill();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void handleAddSkill() {
        // Start the detail activity with no ARG_ITEM_ID to create a new one.
        Intent intent = new Intent(this, SkillDetailActivity.class);
        intent.removeExtra(SkillDetailActivity.ARG_SKILL_POSITION);
        intent.removeExtra(SkillDetailActivity.ARG_SKILL_ID);
        SkillListActivity.this.startActivityForResult(intent, REQ_ADD_SKILL);
    }

    private void setupRecyclerView(@NonNull RecyclerView recyclerView) {
        recyclerView.setAdapter(new SimpleItemRecyclerViewAdapter(this));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != Activity.RESULT_OK) return;
        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.skill_list);
        SimpleItemRecyclerViewAdapter adaptor =
                (SimpleItemRecyclerViewAdapter) recyclerView.getAdapter();
        adaptor.refreshView();
        adaptor.notifyDataSetChanged();
/*
        switch (requestCode) {
            case SkillDetailActivity.REQ_EDIT_SKILL:
                final int skillIndex = data.getIntExtra(SkillDetailActivity.ARG_SKILL_POSITION, -1);
                final long skillId = data.getLongExtra(SkillDetailActivity.ARG_SKILL_ID, -1);
                final boolean deleted = skillId == -1;
                System.out.println("notify for position " + skillIndex);
                if (deleted) adaptor.notifyItemRemoved(skillIndex);
                else adaptor.notifyItemChanged(skillIndex);
                break;
            case SkillDetailActivity.REQ_ADD_SKILL:
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
            cursor = data.getSkillList();
        }

        @Override
        public long getItemId(int position) {
            cursor.moveToPosition(position);
            return cursor.getLong(0);
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.skill_list_item, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(final ViewHolder holder, int position) {
            cursor.moveToPosition(position);
            final long id = cursor.getLong(0);
            try {
                final Proto.Skill skill = Proto.Skill.parseFrom(cursor.getBlob(1));
                holder.setData(skill);
            } catch (InvalidProtocolBufferException x) {
                throw new InternalError("failed to parse protocol buffer");
            }
            holder.view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Context context = v.getContext();
                    Intent intent = new Intent(context, SkillDetailActivity.class);
                    intent.putExtra(SkillDetailActivity.ARG_SKILL_POSITION, holder.getAdapterPosition());
                    intent.putExtra(SkillDetailActivity.ARG_SKILL_ID, id);
                    SkillListActivity.this.startActivityForResult(intent, REQ_EDIT_SKILL);
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
                text = (TextView) view.findViewById(R.id.skill_item_text);
            }

            public void setData(Proto.Skill s) {
                text.setText(s.getName());
            }

            @Override
            public String toString() {
                return super.toString() + " '" + text.getText() + "'";
            }
        }
    }
}
