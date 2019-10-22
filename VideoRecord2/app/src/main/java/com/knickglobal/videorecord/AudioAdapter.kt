package com.knickglobal.videorecord

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.item_audio.view.*

class AudioAdapter(private val mainActivity: MainActivity,private val list: List<AudioModel>
                   , private val mListener: OnItemClickListener?) :
    RecyclerView.Adapter<AudioAdapter.AudioViewHolder>() {

    interface OnItemClickListener {
        fun onItemClick(pathAudio: String, nameAudio: String)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AudioViewHolder {
        return AudioViewHolder(LayoutInflater.from(mainActivity)
            .inflate(R.layout.item_audio, parent, false))
    }

    override fun getItemCount(): Int {
        return list.size
    }

    override fun onBindViewHolder(holder: AudioViewHolder, position: Int) {
        val sb = StringBuilder()
        holder.nameAndArtist.text = sb.append(list[position].aName).append("\n").append(list[position].aArtist)
        holder.nameAndArtist.setOnClickListener {
            mListener?.onItemClick(list[position].aPath, list[position].aName)
        }
    }

    class AudioViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val nameAndArtist: TextView = view.nameAndArtist
    }

}