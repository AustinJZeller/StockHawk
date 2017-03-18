package com.udacity.stockhawk.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.udacity.stockhawk.R;
import com.udacity.stockhawk.sync.IntentCited;
import com.udacity.stockhawk.sync.SyncedAndCite;
import com.udacity.stockhawk.utility.Utilitys;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import yahoofinance.Stock;
import yahoofinance.histquotes.HistoricalQuote;


public class StockDetails extends TopActivity {

    public static Stock stock;
    private static final String TAG = StockDetails.class.getSimpleName();

    @BindView(R.id.lineChart_activity_line_graph)
    LineChart lineChart;

    @BindView(R.id.toolbar_activity_line_graph)
    Toolbar toolbar;

    @BindView(R.id.ll_activity_line_graph)
    LinearLayout linearLayout;

    @BindView(R.id.currency)
    TextView cu;

    @BindView(R.id.stocksymbol)
    TextView ss;

    @BindView(R.id.yearhigh)
    TextView yh;

    @BindView(R.id.yearlow)
    TextView yl;

    @BindView(R.id.daylow)
    TextView dl;

    @BindView(R.id.dayhigh)
    TextView dh;

    @BindView(R.id.stock_name)
    TextView sn;

    @BindView(R.id.last_trade_date)
    TextView ltd;

    String symbol = "";
    ActionBar actionBar;
    private StockHistoryBroadcast stockHistoryBroadcast;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_boundary_chart);
        stock = null;
        ButterKnife.bind(this);

        symbol = getIntent().getStringExtra("symbol_name");

        setSupportActionBar(toolbar);
        actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowHomeEnabled(true);
        }

        getHistoricalData();


    }

    private void getHistoricalData() {
        stockHistoryBroadcast = new StockHistoryBroadcast();
        LocalBroadcastManager.getInstance(this).registerReceiver(stockHistoryBroadcast,
                new IntentFilter(SyncedAndCite.ACTION_STOCK_HISTORY));
        Intent nowIntent = new Intent(this, IntentCited.class);
        nowIntent.putExtra("Symbol", symbol);
        nowIntent.putExtra("IsGetHistory", true);
        startService(nowIntent);
        showLoading(getString(R.string.grabbing_history));
    }

    public class StockHistoryBroadcast extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent != null && intent.getAction().equalsIgnoreCase(SyncedAndCite.ACTION_STOCK_HISTORY)) {
                hideLoading();
                LocalBroadcastManager.getInstance(StockDetails.this).unregisterReceiver(stockHistoryBroadcast);
                showHistory();
            }
        }
    }

    private void showHistory() {
        if (stock != null) {

            ss.setText(stock.getSymbol());
            sn.setText(stock.getName());
            cu.setText(stock.getCurrency());
            ltd.setText(stock.getQuote().getLastTradeDateStr());
            dl.setText(stock.getQuote().getDayLow() + "");
            dh.setText(stock.getQuote().getDayHigh() + "");
            yl.setText(stock.getQuote().getYearLow() + "");
            yh.setText(stock.getQuote().getYearHigh() + "");

            ArrayList<Entry> entries = new ArrayList<>();
            ArrayList<String> xvalues = new ArrayList<>();
            try {
                List<HistoricalQuote> history = stock.getHistory();
                for (int i = 0; i < history.size(); i += 2) {
                    HistoricalQuote historicalQuote = history.get(i);
                    BigDecimal yValue = historicalQuote.getClose();
                    xvalues.add(Utilitys.getFormatedDate(historicalQuote.getDate().getTimeInMillis(), "dd/MM/yy"));
                    entries.add(new Entry(yValue.floatValue(), i));
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            XAxis xAxis = lineChart.getXAxis();
            xAxis.setLabelsToSkip(5);
            xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
            xAxis.setTextSize(12f);
            xAxis.setTextColor(Color.rgb(182, 182, 182));

            YAxis left = lineChart.getAxisLeft();
            left.setEnabled(true);
            left.setLabelCount(10, true);
            left.setTextColor(Color.rgb(182, 182, 182));

            lineChart.getAxisRight().setEnabled(false);
            lineChart.getLegend().setTextSize(16f);
            lineChart.setDrawGridBackground(true);
            lineChart.setGridBackgroundColor(Color.rgb(25, 118, 210));
            lineChart.setDescriptionColor(Color.WHITE);
            lineChart.setDescription(getResources().getString(R.string.last_twelve_month_stock));

            String name = getResources().getString(R.string.stock);
            LineDataSet dataSet = new LineDataSet(entries, name);
            LineData lineData = new LineData(xvalues, dataSet);

            lineChart.animateX(2500);
            lineChart.setData(lineData);

        } else {

            final Snackbar snackbar = Snackbar
                    .make(linearLayout, getString(R.string.no_information_show), Snackbar.LENGTH_INDEFINITE)
                    .setAction(R.string.redo, new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            getHistoricalData();
                        }
                    })
                    .setActionTextColor(Color.GREEN);

            View subview = snackbar.getView();
            TextView tv = (TextView) subview.findViewById(android.support.design.R.id.snackbar_text);
            tv.setTextColor(Color.RED);
            snackbar.show();
        }
    }


}