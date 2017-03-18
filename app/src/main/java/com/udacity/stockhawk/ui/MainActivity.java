package com.udacity.stockhawk.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.udacity.stockhawk.R;
import com.udacity.stockhawk.data.Record;
import com.udacity.stockhawk.data.ChoiceUtility;
import com.udacity.stockhawk.sync.SyncedAndCite;

import butterknife.BindView;
import butterknife.ButterKnife;
import timber.log.Timber;

public class MainActivity extends TopActivity implements LoaderManager.LoaderCallbacks<Cursor>,
        SwipeRefreshLayout.OnRefreshListener,
        StockAdapter.StockAdapterOnClickHandler {

    private static final int STOCK_LOADER = 0;
    @BindView(R.id.recycler_view)
    RecyclerView recyclerView;
    @BindView(R.id.fab)
    FloatingActionButton fab;
    @BindView(R.id.swipe_refresh)
    SwipeRefreshLayout swipeRefreshLayout;
    @BindView(R.id.error)
    TextView error;

    @BindView(R.id.toolbar_activity_my_stocks)
    Toolbar toolbar;

    private StockAdapter adapter;
    private BroadcastReceiver stockCheckBroadcast;

    @Override
    public void onClick(Cursor cursor, int position) {
        if (cursor.moveToPosition(position)) {
            Intent intent = new Intent(MainActivity.this, StockDetails.class);
            intent.putExtra("symbol_name", adapter.getSymbolAtPosition(position));
            startActivity(intent);
        }


    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        if (!networkUp()) {
            error.setText(getString(R.string.error_no_network));
            error.setVisibility(View.VISIBLE);
        }
        setSupportActionBar(toolbar);
        adapter = new StockAdapter(this, this);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        swipeRefreshLayout.setOnRefreshListener(this);
        swipeRefreshLayout.setRefreshing(true);
        onRefresh();

        SyncedAndCite.initialize(this);
        getSupportLoaderManager().initLoader(STOCK_LOADER, null, this);

        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
                String symbol = adapter.getSymbolAtPosition(viewHolder.getAdapterPosition());
                ChoiceUtility.removeStock(MainActivity.this, symbol);
                getContentResolver().delete(Record.Quote.makeUriForStock(symbol), null, null);
            }
        }).attachToRecyclerView(recyclerView);


    }

    private boolean networkUp() {
        ConnectivityManager cm =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = cm.getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isConnectedOrConnecting();
    }

    @Override
    public void onRefresh() {

        SyncedAndCite.syncImmediately(this);

        if (!networkUp() && adapter.getItemCount() == 0) {
            swipeRefreshLayout.setRefreshing(false);
            error.setText(getString(R.string.error_no_network));
            error.setVisibility(View.VISIBLE);
        } else if (!networkUp()) {
            swipeRefreshLayout.setRefreshing(false);
            Toast.makeText(this, R.string.toast_no_connectivity, Toast.LENGTH_LONG).show();
        } else if (ChoiceUtility.getStocks(this).size() == 0) {
            Timber.d("WHYAREWEHERE");
            swipeRefreshLayout.setRefreshing(false);
            error.setText(getString(R.string.error_no_stocks));
            error.setVisibility(View.VISIBLE);
        } else {
            error.setVisibility(View.GONE);
        }
    }

    public void button(View view) {
        new AddStockDialog().show(getFragmentManager(), "StockDialogFragment");
    }

    void addStock(String symbol) {
        if (symbol != null && !symbol.trim().isEmpty()) {

            if (networkUp()) {
                stockCheckBroadcast = new StockCheckBroadcast();
                LocalBroadcastManager.getInstance(this).registerReceiver(stockCheckBroadcast,
                        new IntentFilter(SyncedAndCite.ACTION_STOCK_EXIST));
                SyncedAndCite.checkStockSymbolExistence(symbol.trim(), MainActivity.this);
                showLoading(getString(R.string.checking_stock));

            } else {
                String message = getString(R.string.toast_stock_added_no_connectivity, symbol);
                Toast.makeText(this, message, Toast.LENGTH_LONG).show();
            }
        }
    }

    public void addAndSyncData(String symbol) {
        if (networkUp()) {
            swipeRefreshLayout.setRefreshing(true);
        } else {
            String message = getString(R.string.toast_stock_added_no_connectivity, symbol);
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        }

        ChoiceUtility.addStock(this, symbol);
        SyncedAndCite.syncImmediately(this);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new CursorLoader(this,
                Record.Quote.uri,
                Record.Quote.QUOTE_COLUMNS,
                null, null, Record.Quote.COLUMN_SYMBOL);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        swipeRefreshLayout.setRefreshing(false);

        if (data.getCount() != 0) {
            error.setVisibility(View.GONE);
        }
        adapter.setCursor(data);
    }


    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        swipeRefreshLayout.setRefreshing(false);
        adapter.setCursor(null);
    }


    private void setDisplayModeMenuItemIcon(MenuItem item) {
        if (ChoiceUtility.getDisplayMode(this)
                .equals(getString(R.string.pref_display_mode_absolute_key))) {
            item.setIcon(R.drawable.ic_ratio);
        } else {
            item.setIcon(R.drawable.ic_currency);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        MenuItem item = menu.findItem(R.id.action_change_units);
        setDisplayModeMenuItemIcon(item);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_change_units) {
            ChoiceUtility.toggleDisplayMode(this);
            setDisplayModeMenuItemIcon(item);
            adapter.notifyDataSetChanged();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public class StockCheckBroadcast extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent != null && intent.getAction().equalsIgnoreCase(SyncedAndCite.ACTION_STOCK_EXIST)) {
                hideLoading();
                LocalBroadcastManager.getInstance(MainActivity.this).unregisterReceiver(stockCheckBroadcast);
                if (intent.getBooleanExtra("IS_STOCK_EXIST", false)) {
                    String symbol = intent.getStringExtra("SYMBOL");
                    addAndSyncData(symbol);
                } else {
                    showDialog(new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            alertDialog.dismiss();
                        }
                    }, null, getString(R.string.ok), "", getString(R.string.no_stock_found), getString(R.string.stock_entered_not_available));
                }
            }
        }
    }


}
