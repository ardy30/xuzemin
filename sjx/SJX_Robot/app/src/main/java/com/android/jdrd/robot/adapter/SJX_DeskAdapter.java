package com.android.jdrd.robot.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.android.jdrd.robot.R;
import com.android.jdrd.robot.activity.SJX_MainActivity;
import com.android.jdrd.robot.util.Constant;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 作者: jiayi.zhang
 * 时间: 2017/8/9
 * 描述: 编辑桌子适配器
 */

public class SJX_DeskAdapter extends BaseAdapter {
    Context context;
    List<Map<String, Object>> list;
    private boolean isState = false;

    public SJX_DeskAdapter(Context _context, List<Map<String, Object>> _list) {
        this.list = _list;
        this.context = _context;
    }


    public void setIsState(boolean isState) {
        this.isState = isState;
        notifyDataSetChanged();
    }


    /**
     * 在此适配器中所代表的数据集中的条目数
     *
     * @return size
     */
    @Override
    public int getCount() {
        return list.size();
    }

    /**
     * 获取数据集中与指定索引对应的数据项
     *
     * @param position 获取下标
     * @return
     */
    @Override
    public Object getItem(int position) {
        return list.get(position);
    }

    /**
     * 获取在列表中与指定索引对应的行id
     *
     * @param position
     * @return
     */
    @Override
    public long getItemId(int position) {
        return position;
    }

    /**
     * 获取一个在数据集中指定索引的视图来显示数据
     *
     * @param position
     * @param convertView
     * @param parent
     * @return
     */
    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        final ViewHolder viewHolder;
        ArrayList<Boolean> list_cb = ((SJX_MainActivity) context).getSelectItems();
        // 如果缓存convertView为空，则需要创建View
        if (convertView == null) {
            // 根据context上下文加载布局，这里的是AreaAdapter本身，即this
            final LayoutInflater inflater = (LayoutInflater) context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.sjx_desk_item, null);
            viewHolder = new ViewHolder();
            // 根据自定义的Item布局加载布局
            viewHolder.text = (TextView) convertView.findViewById(R.id.name);
            viewHolder.image = (ImageView) convertView.findViewById(R.id.image);
            viewHolder.bjzt = (ImageView) convertView.findViewById(R.id.bjzt);
            // 复选框
            viewHolder.cb = (CheckBox) convertView.findViewById(R.id.ck_select);
            // 将设置好的布局保存到缓存中，并将其设置在Tag里，以便后面方便取出Tag
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }
        // 是否显示复选框
        viewHolder.cb.setVisibility(isState ? View.VISIBLE : View.GONE);
        // 设置复选框
        if (list_cb.size() != 0)
            viewHolder.cb.setChecked(list_cb.get(position));

        // 隐藏
        viewHolder.bjzt.setVisibility(View.GONE);
        if (list.get(position).get("image") != null) {
            viewHolder.image.setVisibility(View.VISIBLE);
            viewHolder.text.setVisibility(View.GONE);
            if (position == 0) {

                viewHolder.image.setImageResource(R.animator.btn_add_desk_selector);
            } else {
                if (SJX_MainActivity.DeskIsEdit) {
                    viewHolder.bjzt.setVisibility(View.VISIBLE);
                }
                viewHolder.image.setImageResource(R.animator.btn_add_selector);
            }
        } else {
            if (SJX_MainActivity.DeskIsEdit) {
                viewHolder.bjzt.setVisibility(View.VISIBLE);
            }
            viewHolder.text.setVisibility(View.VISIBLE);
            viewHolder.image.setVisibility(View.GONE);
            String str = list.get(position).get("name").toString().trim();
            if (str.length() > 3) {
                StringBuilder sb = new StringBuilder(str);
                sb.insert(3, "\n");
                Constant.debugLog("str" + str + " sb" + sb);
                viewHolder.text.setText(sb);
            } else {
                Constant.debugLog("str" + str);
                viewHolder.text.setText(str);
            }
        }
        return convertView;
    }


    // ViewHolder静态类
    public static class ViewHolder {
        public TextView text;
        public ImageView image;
        public ImageView bjzt;
        public CheckBox cb;
        RelativeLayout back;
    }

}

