package us.paskin.mastery;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_skill_group_list);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setTitle(getTitle());
        super.onCreateDrawer();

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Start the detail activity with no ARG_SKILL_GROUP_ITEM_ID to create a new one.
                Context context = view.getContext();
                Intent intent = new Intent(context, SkillGroupDetailActivity.class);
                intent.removeExtra(SkillGroupDetailActivity.ARG_SKILL_GROUP_INDEX);
                intent.removeExtra(SkillGroupDetailActivity.ARG_SKILL_GROUP_ID);
                SkillGroupListActivity.this.startActivityForResult(intent, SkillGroupDetailActivity.REQ_ADD_SKILL_GROUP);
            }
        });

        View recyclerView = findViewById(R.id.skill_group_list);
        assert recyclerView != null;
        setupRecyclerView((RecyclerView) recyclerView);
    }

    private void setupRecyclerView(@NonNull RecyclerView recyclerView) {
        recyclerView.setAdapter(new SimpleItemRecyclerViewAdapter(this));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != Activity.RESULT_OK) return;
        final int skillGroupIndex = data.getIntExtra(SkillGroupDetailActivity.ARG_SKILL_GROUP_INDEX, -1);
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

    public class SimpleItemRecyclerViewAdapter
            extends RecyclerView.Adapter<SimpleItemRecyclerViewAdapter.ViewHolder> {

        private final SkillData data;
        private Cursor cursor;

        public SimpleItemRecyclerViewAdapter(Context context) {
            setHasStableIds(true);
            data = SkillData.getInstance(context);
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
                    Context context = v.getContext();
                    Intent intent = new Intent(context, SkillGroupDetailActivity.class);
                    intent.putExtra(SkillGroupDetailActivity.ARG_SKILL_GROUP_INDEX, position);
                    intent.putExtra(SkillGroupDetailActivity.ARG_SKILL_GROUP_ID, id);
                    SkillGroupListActivity.this.startActivityForResult(intent, SkillGroupDetailActivity.REQ_EDIT_SKILL_GROUP);
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
