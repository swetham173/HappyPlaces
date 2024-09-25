package com.example.happyplaces.adapters

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.happyplaces.R
import com.example.happyplaces.activities.AddHappyPlaceActivity
import com.example.happyplaces.activities.MainActivity
import com.example.happyplaces.database.DatabaseHandler
import com.example.happyplaces.models.HappyPlaceModel
import kotlinx.android.synthetic.main.item_happy_place.view.*

open class HappyPlacesAdapter(
    private val context: Context,
    private var list: ArrayList<HappyPlaceModel>
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

private var onClickListener:OnClickListener? =null
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {

        return MyViewHolder(
            LayoutInflater.from(context).inflate(
                R.layout.item_happy_place,
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val model = list[position]

        if (holder is MyViewHolder) {
            holder.itemView.iv_place_image.setImageURI(Uri.parse(model.image))//getting the uri from model,image...now setting tht image uri to tht id
           //not as bitmap but the uri of image which we stored in phone
            holder.itemView.tvTitle.text = model.title
            holder.itemView.tvDescription.text = model.description

            holder.itemView.setOnClickListener{
                if(onClickListener!=null){
                    onClickListener!!.onClick(position,model) //overriding the onClick in main act
                }
            }
        }
    }
    //since it is an edit function it defined in the adapter class and is responsible for notifying the adapter that changes need to be made to a specific item in the RecyclerView.
    fun notifyEditItem(activity:Activity,position: Int,requestCode:Int){
        val intent=Intent(context,AddHappyPlaceActivity::class.java)
        intent.putExtra(MainActivity.EXTRA_PLACE_DETAILS,list[position])//we can get to know the actual position through adapter
        //since i cant start intent from adapter itself we are using "activity."
        activity.startActivityForResult(intent,requestCode)//request code is useful when it comes to  whether i want to edit or do something if it satisfies the (if) condition
        //This method notifies the adapter that the item at the specified position has changed
        notifyItemChanged(position)
    }

    fun removeAt(position: Int){
        val dbHandler=DatabaseHandler(context)
        val isDeleted=dbHandler.deleteHappyPlace(list[position])
        if(isDeleted>0){
            list.removeAt(position)//remove from arrayList
            notifyItemRemoved(position)

        }
    }
    fun setOnClickListener(onClickListener: OnClickListener){
        this.onClickListener=onClickListener //my variable(interface type)=my parameter
    }

    override fun getItemCount(): Int {
        return list.size
    }
    //to make a recycler view clickable then we need interface
    interface OnClickListener{
        fun onClick(position: Int,model: HappyPlaceModel)
    }

    private class MyViewHolder(view: View) : RecyclerView.ViewHolder(view)
}
