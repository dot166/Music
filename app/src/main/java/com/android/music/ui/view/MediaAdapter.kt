package com.android.music.ui.view

import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.media3.common.MediaItem
import androidx.recyclerview.widget.RecyclerView
import com.android.music.R

class MediaAdapter(val musicList: MutableList<MediaItem>, val action: (item: MediaItem) -> Unit) :
RecyclerView.Adapter<MediaAdapter.ViewHolder>() {
    override fun onCreateViewHolder(viewGroup: ViewGroup, i: Int): ViewHolder {
        val v = LayoutInflater.from(viewGroup.context).inflate(R.layout.media_item, viewGroup, false)
        return ViewHolder(v)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        val currentSong = musicList[position]

        viewHolder.title.text = currentSong.mediaMetadata.title

        viewHolder.artist.text = currentSong.mediaMetadata.artist

        val artBytes = currentSong.mediaMetadata.artworkData
        if (artBytes != null) {
            val bitmap = BitmapFactory.decodeByteArray(artBytes, 0, artBytes.size)
            viewHolder.albumArt.setImageBitmap(bitmap)
        } else {
            viewHolder.albumArt.setImageResource(R.drawable.def_art)
        }

        viewHolder.title.isSelected = true
        viewHolder.artist.isSelected = true

        viewHolder.itemView.setOnClickListener { view: View? ->
            this.action(currentSong)
        }
    }

    override fun getItemCount(): Int {
        return musicList.size
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var title: TextView = itemView.findViewById(R.id.song_name)
        var artist: TextView = itemView.findViewById(R.id.song_info)
        var albumArt: ImageView = itemView.findViewById(R.id.song_album_cover)
    }
}