package us.paskin.mastery;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.support.design.widget.FloatingActionButton;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

/**
 * An activity representing a list of Skills. This activity
 * has different presentations for handset and tablet-size devices. On
 * handsets, the activity presents a list of items, which when touched,
 * lead to a {@link SkillDetailActivity} representing
 * item details. On tablets, the activity presents the list of items and
 * item details side-by-side using two vertical panes.
 */
public class SkillListActivity extends DrawerActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_skill_list);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setTitle(getTitle());
        super.onCreateDrawer();

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Start the detail activity with no ARG_ITEM_ID to create a new one.
                Context context = view.getContext();
                Intent intent = new Intent(context, SkillDetailActivity.class);
                intent.removeExtra(SkillDetailActivity.ARG_SKILL_INDEX);
                context.startActivity(intent);
            }
        });

        View recyclerView = findViewById(R.id.skill_list);
        assert recyclerView != null;
        setupRecyclerView((RecyclerView) recyclerView);
    }

    private void setupRecyclerView(@NonNull RecyclerView recyclerView) {
        recyclerView.setAdapter(new SimpleItemRecyclerViewAdapter());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == SkillDetailActivity.SKILL_EDITED_RESULT &&
                resultCode == RESULT_OK &&
                data.hasExtra(SkillDetailActivity.RESULT_EDITED_SKILL_INDEX)) {
            final int editedSkillIndex = data.getIntExtra(SkillDetailActivity.RESULT_EDITED_SKILL_INDEX, -1);
            RecyclerView recyclerView = (RecyclerView) findViewById(R.id.skill_list);
            recyclerView.getAdapter().notifyItemChanged(editedSkillIndex);
        }
    }

    public class SimpleItemRecyclerViewAdapter
            extends RecyclerView.Adapter<SimpleItemRecyclerViewAdapter.ViewHolder> {

        private final SkillData data;

        public SimpleItemRecyclerViewAdapter() {
            data = SkillData.getInstance();
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.skill_list_item, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(final ViewHolder holder, final int position) {
            Proto.Skill skill = data.getSkillByIndex(position);
            holder.setData(skill);
            holder.mView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                        Context context = v.getContext();
                        Intent intent = new Intent(context, SkillDetailActivity.class);
                    intent.putExtra(SkillDetailActivity.ARG_SKILL_INDEX, position);
                    SkillListActivity.this.startActivityForResult(intent, SkillDetailActivity.SKILL_EDITED_RESULT);
                }
            });
        }

        @Override
        public int getItemCount() {
            return data.numSkills();
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            public final View mView;
            public final TextView mIdView;
            public final TextView mContentView;
            public Proto.Skill mItem;

            public ViewHolder(View view) {
                super(view);
                mView = view;
                mIdView = (TextView) view.findViewById(R.id.id);
                mContentView = (TextView) view.findViewById(R.id.content);
            }

            public void setData(Proto.Skill skill) {
                mItem = skill;
                mContentView.setText(skill.getName());
            }

            @Override
            public String toString() {
                return super.toString() + " '" + mContentView.getText() + "'";
            }
        }
    }
}
