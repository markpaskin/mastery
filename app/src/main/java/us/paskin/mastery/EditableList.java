package us.paskin.mastery;

import android.view.LayoutInflater;
import android.view.View;
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
     * Adds an item to the list.
     *
     * @param text     the displayed text
     * @param onClick    called if the item is tapped
     * @param onRemove called if the item is removed
     */
    public void addItem(String text, View.OnClickListener onClick, final Runnable onRemove) {
        LayoutInflater inflater = LayoutInflater.from(tableLayout.getContext());
        final TableRow row = (TableRow) inflater.inflate(R.layout.editable_list_item, tableLayout, false);
        TextView textView = (TextView) row.findViewById(R.id.item_text);
        textView.setText(text);
        textView.setOnClickListener(onClick);
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
