package in.thetechguru.walle.remote.abremotewallpaperchanger.activity_fragments;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import java.util.List;
import java.util.concurrent.Executors;

import butterknife.BindView;
import butterknife.ButterKnife;
import in.thetechguru.walle.remote.abremotewallpaperchanger.R;
import in.thetechguru.walle.remote.abremotewallpaperchanger.history.HistoryItem;
import in.thetechguru.walle.remote.abremotewallpaperchanger.history.HistoryRepo;

/**
 * Created by abami on 04-Feb-18.
 */

public class ActivityHistory extends AppCompatActivity {

    @BindView(R.id.toolbar)
    Toolbar toolbar;

    @BindView(R.id.recycler_view)
    RecyclerView recyclerView;

    @BindView(R.id.progress_bar)
    ProgressBar progressBar;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);
        ButterKnife.bind(this);
        setSupportActionBar(toolbar);

        // add back arrow to toolbar
        if (getSupportActionBar() != null){
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        Glide.with(this)
                .load("https://i2.wp.com/theprehabguys.com/wp-content/uploads/2016/12/black-background.jpg?ssl=1")
                //.centerCrop()
                //.crossFade(500)
                .into((ImageView)findViewById(R.id.full_background));

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter( new HistoryAdapter());

        setTitle("History");
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                overridePendingTransition(android.R.anim.slide_in_left,android.R.anim.slide_out_right);
                finish();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    class HistoryAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>{

        private List<HistoryItem> historyItems;
        private Handler handler;

        private static final int VIEW_TYPE_HISTORY_SELF = 1;
        private static final int VIEW_TYPE_HISTORY_ELSE = 2;

        HistoryAdapter(){
            handler = new Handler(Looper.getMainLooper());
            Executors.newSingleThreadExecutor().execute(new Runnable() {
                @Override
                public void run() {
                    historyItems = HistoryRepo.getInstance().getHistory();
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            progressBar.setVisibility(View.INVISIBLE);
                            recyclerView.setVisibility(View.VISIBLE);
                            notifyDataSetChanged();
                        }
                    });
                }
            });
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view;

            if (viewType == VIEW_TYPE_HISTORY_SELF) {
                view = LayoutInflater.from(getApplicationContext())
                        .inflate(R.layout.item_history_changd_self, parent, false);
                return new HistorySelfChanged(view);
            } else if (viewType == VIEW_TYPE_HISTORY_ELSE) {
                view = LayoutInflater.from(getApplicationContext())
                        .inflate(R.layout.item_history_else_changed, parent, false);
                return new HistoryElseChanged(view);
            }

            return null;
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int i) {
            CharSequence ago =
                    DateUtils.getRelativeTimeSpanString(historyItems.get(i).timestamp);
            Log.d("HistoryAdapter", "onBindViewHolder: " + ago);

            switch (holder.getItemViewType()) {
                case VIEW_TYPE_HISTORY_SELF:
                    ((HistorySelfChanged)holder).textView.setText(getString(R.string.you_changed, historyItems.get(i).changedOf));
                    Glide.with(getApplicationContext()).load(Uri.parse(historyItems.get(i).path))
                            .override(200,200)
                            .centerCrop()
                            .placeholder(R.drawable.ic_error_outline_black_24dp)
                            .into(((HistorySelfChanged)holder).imageView);
                    ((HistorySelfChanged)holder).timestamp.setText(ago);
                    if(historyItems.get(i).status==HistoryItem.STATUS.SUCCESS){
                        ((HistorySelfChanged) holder).changeStatus.setImageDrawable(getResources().getDrawable(R.drawable.ic_check_black_24dp));
                    }
                    break;

                case VIEW_TYPE_HISTORY_ELSE:
                    ((HistoryElseChanged)holder).textView.setText(getString(R.string.changed_by, historyItems.get(i).changedBy));
                    Glide.with(getApplicationContext()).load(Uri.parse(historyItems.get(i).path))
                            .override(200,200)
                            .centerCrop()
                            .placeholder(R.drawable.ic_error_outline_black_24dp)
                            .into(((HistoryElseChanged)holder).imageView);
                    ((HistoryElseChanged)holder).timestamp.setText(ago);
            }
        }

        @Override
        public int getItemCount() {
            return historyItems.size();
        }

        @Override
        public int getItemViewType(int position) {
            String changedBy = historyItems.get(position).changedBy;

            if (changedBy.equals("self")) {
                // Changed by self
                return VIEW_TYPE_HISTORY_SELF;
            } else {
                // Changed by else
                return VIEW_TYPE_HISTORY_ELSE;
            }
        }

        class HistorySelfChanged extends RecyclerView.ViewHolder {

            @BindView(R.id.changed_by_username)
            TextView textView;
            @BindView(R.id.changed_wallpaper)
            ImageView imageView;
            @BindView(R.id.timestamp) TextView timestamp;
            @BindView(R.id.change_status) ImageView changeStatus;

            HistorySelfChanged(View itemView) {
                super(itemView);
                ButterKnife.bind(this,itemView);
            }
        }

        class HistoryElseChanged extends RecyclerView.ViewHolder {

            @BindView(R.id.changed_by_username)
            TextView textView;
            @BindView(R.id.changed_wallpaper)
            ImageView imageView;
            @BindView(R.id.timestamp) TextView timestamp;

            HistoryElseChanged(View itemView) {
                super(itemView);
                ButterKnife.bind(this,itemView);
            }
        }

    }

}
