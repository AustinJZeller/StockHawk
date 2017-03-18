package com.udacity.stockhawk.ui.widgets;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Binder;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.udacity.stockhawk.R;
import com.udacity.stockhawk.data.Record;
import com.udacity.stockhawk.data.ChoiceUtility;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;

public class StockGizmoOutlook extends RemoteViewsService {
	private static final String LOG_TAG = StockGizmoOutlook.class.getSimpleName();
	public StockGizmoOutlook() {
	}

	@Override
	public RemoteViewsFactory onGetViewFactory(Intent intent) {
		return new WidgetItemRemoteView(this.getApplicationContext(),intent);
	}

	private class WidgetItemRemoteView implements RemoteViewsFactory{
		Context mContext;
		Cursor mCursor;
		Intent mIntent;

		WidgetItemRemoteView(Context mContext, Intent mIntent) {
			this.mContext = mContext;
			this.mIntent = mIntent;
		}

		@Override
		public void onCreate() {
			//Nothing
		}

		@Override
		public int getCount() {
			return mCursor != null ? mCursor.getCount() : 0;
		}

		@Override
		public void onDataSetChanged() {
			if (mCursor!=null)
				mCursor.close();

			final long pId = Binder.clearCallingIdentity();
			mCursor = getContentResolver().query(
					Record.Quote.uri,
					null,
					null,
					null,
					null
			);

			Binder.restoreCallingIdentity(pId);
		}

		@Override
		public int getViewTypeCount() {
			return 1;
		}

		@Override
		public RemoteViews getViewAt(int position) {
			try{
				mCursor.moveToPosition(position);
				int priceChangeColorId;

				// get Stock Quote information
				String stockSymbol = mCursor.getString(mCursor.getColumnIndex(Record.Quote.COLUMN_SYMBOL));
				String stockBidPrice = mCursor.getString(mCursor.getColumnIndex(Record.Quote.COLUMN_PRICE));

				// create List Item for Widget ListView
				RemoteViews listItemRemoteView = new RemoteViews(mContext.getPackageName(), R.layout.list_feature_gizmo);
				listItemRemoteView.setTextViewText(R.id.stock_symbol,stockSymbol);
				listItemRemoteView.setTextViewText(R.id.bid_price,stockBidPrice);



				float rawAbsoluteChange = mCursor.getFloat(Record.Quote.POSITION_ABSOLUTE_CHANGE);
				if (rawAbsoluteChange > 0) {
					priceChangeColorId=(R.drawable.bonus_green);
				} else {
					priceChangeColorId=(R.drawable.bonus_red);
				}

			DecimalFormat	dollarFormatWithPlus = (DecimalFormat) NumberFormat.getCurrencyInstance(Locale.US);
				dollarFormatWithPlus.setPositivePrefix("+$");
				String change = dollarFormatWithPlus.format(rawAbsoluteChange);

				if (ChoiceUtility.getDisplayMode(getApplicationContext())
						.equals(getApplicationContext().getString(R.string.pref_display_mode_absolute_key))) {
					listItemRemoteView.setTextViewText(R.id.change,change);
				} else {
					listItemRemoteView.setTextViewText(R.id.change,change);
				}
				listItemRemoteView.setInt(R.id.change,"setBackgroundResource",priceChangeColorId);

				return listItemRemoteView;
			}catch (Exception e){
				e.printStackTrace();
			}
			return null;
		}

		@Override
		public long getItemId(int position) {
			mCursor.moveToPosition(position);
			return mCursor.getLong(mCursor.getColumnIndex(Record.Quote._ID));
		}

		@Override
		public void onDestroy() {
			if (mCursor!=null)
				mCursor.close();
		}

		@Override
		public RemoteViews getLoadingView() {
			return null;
		}

		@Override
		public boolean hasStableIds() {
			return true;
		}
	}
}
