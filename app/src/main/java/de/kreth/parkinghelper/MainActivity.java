package de.kreth.parkinghelper;

import android.location.Location;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.zip.Inflater;

public class MainActivity extends AppCompatActivity {

    private int count = 0;
    private PositionAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                storeCurrentPosition();
            }
        });

        ListView listView = (ListView) findViewById(R.id.list_view_position);
        adapter = new PositionAdapter();
        listView.setAdapter(adapter);
    }

    private void storeCurrentPosition() {
        count++;
        Log.d(getClass().getName(), "Fetching and storing Location " + count);

        Location pos = new Location("test");
        pos.setLatitude(13.12325);
        pos.setLongitude(5.32113);
        pos.setTime(new Date().getTime());
        PositionItem item = PositionItem.create("Item " + count, pos);
        adapter.add(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private class PositionAdapter extends BaseAdapter {

        private final List<PositionItem> data = new ArrayList<>();

        public void add (PositionItem item) {
            data.add(item);
            Log.d(getClass().getName(), "Added item " + item);
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return data.size();
        }

        @Override
        public PositionItem getItem(int position) {
            return data.get(position);
        }

        @Override
        public long getItemId(int position) {
            return getItem(position).getId();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            LayoutHolder holder;
            if(convertView == null) {
                convertView = getLayoutInflater().inflate(R.layout.position_item_layout, parent);
                holder = new LayoutHolder();
                holder.caption = convertView.findViewById(R.id.textViewCaption);
                holder.description = convertView.findViewById(R.id.textViewDescription);
                convertView.setTag(holder);
            } else {
                holder = (LayoutHolder) convertView.getTag();
            }
            holder.update(getItem(position));
            return convertView;
        }

        private class LayoutHolder {
            TextView caption;
            TextView description;
            void update(PositionItem item) {
                caption.setText(item.getName());
                StringBuilder desc = new StringBuilder();
                desc
                        .append(item.getLocation().getLongitude())
                        .append("\n")
                        .append(item.getLocation().getLatitude());
                description.setText(desc);
            }
        }
    }
}
