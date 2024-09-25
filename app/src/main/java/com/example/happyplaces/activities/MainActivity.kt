package com.example.happyplaces.activities

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.happyplaces.R
import com.example.happyplaces.adapters.HappyPlacesAdapter
import com.example.happyplaces.database.DatabaseHandler
import com.example.happyplaces.models.HappyPlaceModel
import com.example.happyplaces.utils.SwipeToDeleteCallback
import com.example.happyplaces.utils.SwipeToEditCallback
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.item_happy_place.view.*

class MainActivity : AppCompatActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        fabAddHappyPlace.setOnClickListener {
            val intent = Intent(this@MainActivity, AddHappyPlaceActivity::class.java)
            startActivityForResult(intent,ADD_PLACE_ACTIVITY_REQUEST_CODE)
        }
        getHappyPlaceListFromLocalDB()
    }

    //db functionality
    private fun getHappyPlaceListFromLocalDB(){
        val dbHandler=DatabaseHandler(this)
        val getHappyPlaceList:ArrayList<HappyPlaceModel> =dbHandler.getHappyPlacesList()

        if(getHappyPlaceList.size>0){
           rv_happy_places_list.visibility= View.VISIBLE
            tv_no_records_available.visibility=View.GONE
            setUpHappyPlacesRecyclerView(getHappyPlaceList)//setting the inner db to adapter indirectly through recycler view
        }else{
            rv_happy_places_list.visibility= View.GONE
            tv_no_records_available.visibility=View.VISIBLE
        }
    }
    //adapter functionality
    private fun setUpHappyPlacesRecyclerView(happyPlaceList:ArrayList<HappyPlaceModel>){
        rv_happy_places_list.layoutManager=LinearLayoutManager(this)

        rv_happy_places_list.setHasFixedSize(true)
        val placesAdapter=HappyPlacesAdapter(this,happyPlaceList)
        rv_happy_places_list.adapter=placesAdapter

        placesAdapter.setOnClickListener(object:HappyPlacesAdapter.OnClickListener{
            override fun onClick(position: Int, model: HappyPlaceModel) {
                val intent=Intent(this@MainActivity,HappyPlaceDetailActivity::class.java)
                intent.putExtra(EXTRA_PLACE_DETAILS,model)
                startActivity(intent)
            }

        }) //clicking on an adapter needs this procedure to be done bruh

        //edit func
val editSwipeHandler=object:SwipeToEditCallback(this){
    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        val adapter=rv_happy_places_list.adapter as HappyPlacesAdapter
        adapter.notifyEditItem(this@MainActivity,viewHolder.adapterPosition,ADD_PLACE_ACTIVITY_REQUEST_CODE)
        //so this send us to addHappyPlaceActivity screen
        }

    }
        val editItemTouchHelper=ItemTouchHelper(editSwipeHandler)
    //utility class provided by the Android Support Library to handle swipe gestures and drag-and-drop functionality
        editItemTouchHelper.attachToRecyclerView(rv_happy_places_list)


        //delete function
        val deleteSwipeHandler=object: SwipeToDeleteCallback(this){
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val adapter=rv_happy_places_list.adapter as HappyPlacesAdapter
                adapter.removeAt(viewHolder.adapterPosition)
                getHappyPlaceListFromLocalDB()
            }
        }
        val deleteItemTouchHelper=ItemTouchHelper(deleteSwipeHandler)
        //utility class provided by the Android Support Library to handle swipe gestures and drag-and-drop functionality
        deleteItemTouchHelper.attachToRecyclerView(rv_happy_places_list)

    }
    companion object{
        var ADD_PLACE_ACTIVITY_REQUEST_CODE=1
        var EXTRA_PLACE_DETAILS="extra_place_details"
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode== ADD_PLACE_ACTIVITY_REQUEST_CODE){
            if(resultCode== RESULT_OK){
                getHappyPlaceListFromLocalDB()
            }else{
                Log.e("Activity","Cancelled or Back Pressed")
            }
        }
    }

}