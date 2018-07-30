package com.beta1.memories.beta4musicplayer.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;

import com.beta1.memories.beta4musicplayer.R;
import com.beta1.memories.beta4musicplayer.Song;

import java.util.ArrayList;
import java.util.List;

public class BaseSongAdapter extends BaseAdapter implements Filterable {

    private Context context;
    private List<Song> arSong;
    private List<Song> arSongFull;

    public BaseSongAdapter(Context context, List<Song> arSong) {
        this.context = context;
        this.arSong = arSong;
        this.arSongFull = new ArrayList<>(arSong);
    }

    @Override
    public int getCount() {
        return arSong.size();
    }

    @Override
    public Object getItem(int position) {
        return arSong.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        View view = convertView;
        if (view == null) {
            view = LayoutInflater.from(context).inflate(R.layout.list_song, parent, false);
        }

        Song currSong = arSong.get(position);

        TextView tvTitle = view.findViewById(R.id.tvTitle);
        TextView tvArtist = view.findViewById(R.id.tvArtist);
        ImageView ivSong = view.findViewById(R.id.ivSong);

        view.setTag(position);

        tvTitle.setText(currSong.getTitle());
        tvArtist.setText(currSong.getArtist());
        ivSong.setImageBitmap(currSong.getAlbumB());

        return view;
    }

    //filter
    @Override
    public Filter getFilter() {
        return musicListFilter;
    }

    private Filter musicListFilter = new Filter() {
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            final List<Song> filteredList = new ArrayList<>();
            if (constraint == null || constraint.length() == 0) {
                filteredList.addAll(arSongFull);
            } else {
                String filterPattern = constraint.toString().toLowerCase().trim();
                for (Song song : arSongFull) {
                    if (song.getTitle().toLowerCase().contains(filterPattern)) {
                        filteredList.add(song);
                    }
                }
            }

            FilterResults results = new FilterResults();
            results.values = filteredList;

            return results;
        }

        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            arSong.clear();
            arSong.addAll((List) results.values);
            notifyDataSetChanged();
        }
    };
}
