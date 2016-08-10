package us.paskin.mastery;

import android.content.Context;
import android.text.Layout;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

/**
 * Created by baq on 8/8/16.
 */
public class EditableList {
    final private TableLayout tableLayout;

    /**
     * @param tableLayout a handle to a TableLayout structured via editable_table_layout.xml.
     * @param onAdd       called when the user wishes to add an item
     */
    public EditableList(TableLayout tableLayout, View.OnClickListener onAdd) {
        this.tableLayout = tableLayout;
        tableLayout.findViewById(R.id.add_button).setOnClickListener(onAdd);
        LayoutInflater inflater = LayoutInflater.from(tableLayout.getContext());
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
     * @param onRemove called if the item is removed
     */
    public void addTextItem(String text, View.OnClickListener onClick, final Runnable onRemove) {
        TextView textView = new TextView(tableLayout.getContext());
        textView.setText(text);
        addItem(textView, onClick, onRemove);
    }

    /**
     * Adds an string item to the list.
     *
     * @param view     the displayed item
     * @param onClick  called if the item is tapped
     * @param onRemove called if the item is removed
     */
    public void addItem(View view, View.OnClickListener onClick, final Runnable onRemove) {
        LayoutInflater inflater = LayoutInflater.from(tableLayout.getContext());
        final TableRow row = (TableRow) inflater.inflate(R.layout.editable_list_item, tableLayout, false);
        LinearLayout itemLayout = (LinearLayout) row.findViewById(R.id.item);
        itemLayout.setOnClickListener(onClick);
        itemLayout.addView(view);
        tableLayout.addView(row);
        View removeButton = row.findViewById(R.id.remove_button);
        removeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onRemove.run();
                tableLayout.removeView(row);
            }
        });
    }
}
