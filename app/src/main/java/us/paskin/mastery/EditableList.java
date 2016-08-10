package us.paskin.mastery;

import android.content.Context;
import android.support.annotation.Nullable;
import android.text.Layout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

/**
 * Created by baq on 8/8/16.
 */
public class EditableList {
    final private TableLayout tableLayout;

    private OnItemRemovedListener onItemRemovedListener;

    /**
     * @param tableLayout a handle to a TableLayout structured via editable_table_layout.xml.
     * @param onAdd       called when the user wishes to add an item
     */
    public EditableList(TableLayout tableLayout, View.OnClickListener onAdd,
                        OnItemRemovedListener onRemoved) {
        this.tableLayout = tableLayout;
        this.onItemRemovedListener = onRemoved;
        tableLayout.findViewById(R.id.add_button).setOnClickListener(onAdd);
        LayoutInflater inflater = LayoutInflater.from(tableLayout.getContext());
    }

    public interface OnItemRemovedListener {
        void onItemRemoved(int index);
    }

    /**
     * Returns the root view of this list.  The caller can use this to inflate views that will be added via addItem.
     */
    public TableLayout getRoot() {
        return tableLayout;
    }

    /**
     * Adds an string item to the list.
     *
     * @param text     the displayed text
     * @param onClick    called if the item is tapped
     * @return the index of the newly added item
     */
    public int addTextItem(String text, @Nullable View.OnClickListener onClick) {
        TextView textView = new TextView(tableLayout.getContext());
        textView.setText(text);
        // Update the text view to fill its parent, so that it receives all click events.
        textView.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        if (onClick != null) textView.setOnClickListener(onClick);
        return addItem(textView);
    }

    /**
     * Adds an string item to the list.
     *
     * @param view     the displayed item
     * @return the index of the newly added item
     */
    public int addItem(View view) {
        LayoutInflater inflater = LayoutInflater.from(tableLayout.getContext());
        final TableRow row = (TableRow) inflater.inflate(R.layout.editable_list_item, tableLayout, false);
        LinearLayout itemLayout = (LinearLayout) row.findViewById(R.id.item);
        itemLayout.addView(view);
        // Add this as the penultimate row so the "add" row is last.
        int numRows = tableLayout.getChildCount();
        final int index = numRows - 1;
        tableLayout.addView(row, index);
        View removeButton = row.findViewById(R.id.remove_button);
        removeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (onItemRemovedListener != null) {
                    // Compute the index associated with this button.  It may have changed due to previous edits.
                    int index = tableLayout.indexOfChild(row);
                    onItemRemovedListener.onItemRemoved(index);
                }
                tableLayout.removeView(row);
            }
        });
        return index;
    }
}
