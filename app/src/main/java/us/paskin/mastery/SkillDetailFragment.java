package us.paskin.mastery;

import android.app.Activity;
import android.support.design.widget.CollapsingToolbarLayout;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import us.paskin.mastery.dummy.DummyContent;

/**
 * A fragment representing a single Skill detail screen.
 * This fragment is either contained in a {@link SkillListActivity}
 * in two-pane mode (on tablets) or a {@link SkillDetailActivity}
 * on handsets.
 */
public class SkillDetailFragment extends Fragment {
    /**
     * The fragment argument representing the item ID that this fragment
     * represents.
     */
    public static final String ARG_ITEM_ID = "item_id";

    /**
     * The dummy content this fragment is presenting.
     */
    private DummyContent.DummyItem mItem;

    /**
     * This is true if there have been changes that weren't committed.
     */
    private boolean changed = false;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public SkillDetailFragment() {
    }

    /**
     * @return true if there have been changes made
     */
    public boolean hasChanges() {
        return changed;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments().containsKey(ARG_ITEM_ID)) {
            // Load the dummy content specified by the fragment
            // arguments. In a real-world scenario, use a Loader
            // to load content from a content provider.
            mItem = DummyContent.ITEM_MAP.get(getArguments().getString(ARG_ITEM_ID));
        }
    }

    void updateTitle(CharSequence title) {
        Activity activity = this.getActivity();
        CollapsingToolbarLayout appBarLayout = (CollapsingToolbarLayout) activity.findViewById(R.id.toolbar_layout);
        if (appBarLayout != null) {
            appBarLayout.setTitle(title);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.skill_detail, container, false);

        EditText nameEditText = ((EditText) rootView.findViewById(R.id.skill_name));
        // Show the dummy content as text in a TextView.
        if (mItem != null) {
            updateTitle(mItem.content);
            nameEditText.setText(mItem.details);
        }

        nameEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
                SkillDetailFragment.this.changed = true;
                updateTitle(editable);
            }
        });

        return rootView;
    }
}
