package tae.mobilelivebroadcast.util;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.jpardogo.android.flabbylistview.lib.FlabbyLayout;

import java.util.List;
import java.util.Random;

import butterknife.ButterKnife;
import butterknife.InjectView;
import tae.mobilelivebroadcast.R;

/**
 * Created by Tae on 2016-04-25.
 */
public class ListRoomAdapter extends ArrayAdapter<String> {
    private Context mContext;
    private List<String> mMasters;
    private List<String> mTitles;
    private List<String> mCounts;
    private Random mRandomizer = new Random();

    public ListRoomAdapter(Context context, List<String> masters, List<String> titles, List<String> counts) {
        super(context,0,masters);
        mContext = context;
        mMasters = masters;
        mTitles = titles;
        mCounts = counts;
    }

    @Override
    public int getCount() {
        return mMasters.size();
    }

    @Override
    public String getItem(int position) {
        return mMasters.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = LayoutInflater.from(mContext).inflate(R.layout.item_list, parent, false);
            holder = new ViewHolder(convertView);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        int color = Color.argb(255, mRandomizer.nextInt(256), mRandomizer.nextInt(256), mRandomizer.nextInt(256));
        ((FlabbyLayout)convertView).setFlabbyColor(color);
        holder.broadMaster.setText(mMasters.get(position));
        holder.broadTitle.setText(mTitles.get(position));
        holder.broadCountUser.setText(mCounts.get(position));
        //여기에 viewholder에 입력해놓은 아이템들 값만 넣어주면 됨
        return convertView;
    }

    static class ViewHolder {
        @InjectView(R.id.broadID)
        TextView broadMaster;
        @InjectView(R.id.broadTitle)
        TextView broadTitle;
        @InjectView(R.id.broadCountUser)
        TextView broadCountUser;
        @InjectView(R.id.broadThumbnail)
        ImageView boradThumbnail;

        public ViewHolder(View view) {
            ButterKnife.inject(this, view);
        }
    }
}
