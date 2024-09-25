package com.example.happyplaces.activities
import android.Manifest
import android.app.Activity
import android.app.DatePickerDialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.happyplaces.R
import com.example.happyplaces.database.DatabaseHandler
import com.example.happyplaces.models.HappyPlaceModel
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.Autocomplete
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode

import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import kotlinx.android.synthetic.main.activity_add_happy_place.*
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.*
class AddHappyPlaceActivity : AppCompatActivity(), View.OnClickListener {
    private var cal=Calendar.getInstance()
    private lateinit var dateSetListener:DatePickerDialog.OnDateSetListener
    private var saveImageToInternalStorage:Uri?=null
    private var mLatitude:Double=0.0
    private var mLongitude:Double=0.0
    private var mHappyPlaceDetails: HappyPlaceModel? =null



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_happy_place)

        setSupportActionBar(toolbar_add_place)// converting the default toolbar to our customized toolBar
        //when you set a toolbar as the action bar for an activity using setSupportActionBar(toolbar),
        // the toolbar automatically gets certain functionalities associated with it, including a navigation icon.
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar_add_place.setNavigationOnClickListener {
            onBackPressed()
        }
        if (!Places.isInitialized()) {
            Places.initialize(
                this@AddHappyPlaceActivity,
                resources.getString(R.string.google_maps_api_key)
            )
        }

        if(intent.hasExtra(MainActivity.EXTRA_PLACE_DETAILS)){//key to identify if its swiped right
            mHappyPlaceDetails=intent.getSerializableExtra(MainActivity.EXTRA_PLACE_DETAILS) as HappyPlaceModel
        }
        dateSetListener=DatePickerDialog.OnDateSetListener { view, year, month, dayOfMonth ->
            cal.set(Calendar.YEAR,year)
            cal.set(Calendar.MONTH,month)
            cal.set(Calendar.DAY_OF_MONTH,dayOfMonth)
            updateDateInView()
        }
        updateDateInView()//so tht today's date is visible

        //separate for editing
        if(mHappyPlaceDetails!=null){
            supportActionBar!!.title="Edit Happy Place"
            et_title.setText(mHappyPlaceDetails!!.title)
            et_description.setText(mHappyPlaceDetails!!.description)
            et_date.setText(mHappyPlaceDetails!!.date)
            et_location.setText(mHappyPlaceDetails!!.location)
            mLatitude=mHappyPlaceDetails!!.latitude
            mLongitude=mHappyPlaceDetails!!.longitude
            saveImageToInternalStorage=Uri.parse(mHappyPlaceDetails!!.image)
            iv_place_image.setImageURI(saveImageToInternalStorage)
            btn_save.text="UPDATE"
        }
        et_date.setOnClickListener(this)//onClick method defined in the same class will be called.
        tv_add_image.setOnClickListener(this)
        btn_save.setOnClickListener(this)
        et_location.setOnClickListener(this)
    }

    override fun onClick(v: View?) {
        when(v!!.id){
            R.id.et_date ->{
                DatePickerDialog(
                    this@AddHappyPlaceActivity,
                    dateSetListener,
                    cal.get(Calendar.YEAR),
                    cal.get(Calendar.MONTH),
                    cal.get(Calendar.DAY_OF_MONTH)
                ).show()
            }
            R.id.tv_add_image ->{
                val pictureDialog=AlertDialog.Builder(this)
                pictureDialog.setTitle("Select Action")
                val pictureDialogItems=arrayOf("Select photo from Gallery","Capture photo from Camera")
                pictureDialog.setItems(pictureDialogItems){
                   dialog,which-> //interacting with dialog and index

                    when(which){
                        0->choosePhotoFromGallery()
                        1->takePhotoFromGallery()
                    }

                }
                pictureDialog.show()
            }
            R.id.btn_save->{
                when{
                    et_title.text.isNullOrEmpty()->{
                        Toast.makeText(this,"Please enter title",Toast.LENGTH_SHORT).show()
                    }
                    et_description.text.isNullOrEmpty()->{
                        Toast.makeText(this,"Please enter description",Toast.LENGTH_SHORT).show()
                    }
                    et_location.text.isNullOrEmpty()->{
                        Toast.makeText(this,"Please enter a location",Toast.LENGTH_SHORT).show()
                    }
                    saveImageToInternalStorage==null->{
                        Toast.makeText(this,"Please select an image",Toast.LENGTH_SHORT).show()
                    }else->{
                         val happyPlaceModel=HappyPlaceModel(
                             if(mHappyPlaceDetails==null) 0 else mHappyPlaceDetails!!.id, //usually its 0(normal case when no swipe is done)
                             et_title.text.toString(),
                             saveImageToInternalStorage.toString(),
                             et_description.text.toString(),
                             et_date.text.toString(),
                             et_location.text.toString(),
                             mLatitude,
                             mLongitude
                         )
                    val dbHandler= DatabaseHandler(this)
                    if(mHappyPlaceDetails==null) {
                        val addHappyPlace = dbHandler.addHappyPlace(happyPlaceModel)
                        if (addHappyPlace>0){
                            setResult(RESULT_OK)//result is ok only when there is a entry
                            // this will trigger the override function of main activity if the result is okay
                            finish()//close the activity and go to main activity
                        }
                    }else{
                        val updateHappyPlace = dbHandler.updateHappyPlace(happyPlaceModel)
                        if (updateHappyPlace>0){
                            setResult(RESULT_OK)//result is ok only when there is a entry
                            // this will trigger the override function of main activity if the result is okay
                            finish()//close the activity and go to main activity
                        }
                    }

                    }
                }
            }

            R.id.et_location -> {
                try {
                    // These are the list of fields which we required is passed
                    val fields = listOf(
                        Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG,
                        Place.Field.ADDRESS
                    )
                    Log.d("Autocomplete", "Creating autocomplete intent")

                    // Start the autocomplete intent with a unique request code.
                    val intent =
                        Autocomplete.IntentBuilder(AutocompleteActivityMode.FULLSCREEN, fields)
                            .build(this@AddHappyPlaceActivity)
                    startActivityForResult(intent, PLACE_AUTOCOMPLETE_REQUEST_CODE)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

        }

    }
    private fun choosePhotoFromGallery(){
        Dexter.withActivity(this).withPermissions(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        ).withListener(object: MultiplePermissionsListener {

            override fun onPermissionsChecked(report: MultiplePermissionsReport?){ //object containing information about the permissions.

                if (report!!.areAllPermissionsGranted()){//if permission granted
                   val GalleryIntent=Intent(Intent.ACTION_PICK,
                       MediaStore.Images.Media.EXTERNAL_CONTENT_URI) //picking image
                    startActivityForResult(GalleryIntent, GALLERY)//parent activity
                }
            }
            override fun onPermissionRationaleShouldBeShown(
                permissions: MutableList<PermissionRequest>,
                token: PermissionToken )
            {
                showRationalForPermission()
            }
        })
            .onSameThread().check()// asking for permission and displaying the rationale at the same time

    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {//child activity
        super.onActivityResult(requestCode, resultCode, data)

        if(resultCode== RESULT_OK){
            if(requestCode== GALLERY){
                if(data!=null){
                     val contentURI=data.data//this child activity data we cant just say "data " here becaz it might refer to other child activity
                    //but now we have mentioned conditions which makes it look more specific abt child activity
                    try{//get image from gallery
                         val selectedImageBitmap= MediaStore.Images.Media.getBitmap(this.contentResolver,contentURI)
                             //The contentResolver is responsible for accessing content providers and performing operations on content such as images


                        //so basically this log statements are for testing if the image is stored in device since there is no way of testing this directly

                         saveImageToInternalStorage=saveImageToInternalStorage(selectedImageBitmap)
                        Log.e("Saved image: ","Path::$saveImageToInternalStorage")



                    //bitmapping(accessing the image,with the help of location)
                        iv_place_image.setImageBitmap(selectedImageBitmap)
                    }catch (e:IOException){
                        e.printStackTrace()
                        Toast.makeText(this,"Failed to load image from Gallery",Toast.LENGTH_SHORT).show()
                    }
                }
            }else if(requestCode== CAMERA){
                val thumbnail:Bitmap=data!!.extras!!.get("data") as Bitmap//first data word represents the image retrieved from tht function
                //second word "Data" represents the key value name which we give it to extras (thumbnail)
                //now we are mapping resultant data to thumbnail
                //"data" should be in small letter

                //so basically this log statements are for testing if the image is stored in device since there is no way of testing this directly
                 saveImageToInternalStorage=saveImageToInternalStorage(thumbnail)
                Log.e("Saved image: ","Path::$saveImageToInternalStorage")

                iv_place_image.setImageBitmap(thumbnail)
            }
            else if (requestCode == PLACE_AUTOCOMPLETE_REQUEST_CODE) {
Log.d("Sweth","over")
                val place: Place = Autocomplete.getPlaceFromIntent(data!!)

                et_location.setText(place.address)
                mLatitude = place.latLng!!.latitude
                mLongitude = place.latLng!!.longitude
            }
            // END
        }
        else if (resultCode == Activity.RESULT_CANCELED) {
            Log.e("Cancelled", "Cancelled")
        }
    }
    private fun showRationalForPermission(){//reason why should i grant permission
         AlertDialog.Builder(this).setMessage("It looks like you have turned off permissions required for this features." +
                 "It can be enabled under the application settings")
             .setPositiveButton("GO TO SETTINGS"){//if i click on this
                 _,_ ->
                 try{//launching the settings screen for the current application
                     val intent =Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                        val uri= Uri.fromParts("package",packageName,null)
                     intent.data=uri //intent is set to open the settings for the application specified by this URI.
                     startActivity(intent)
                 }catch(e:ActivityNotFoundException){
                        e.printStackTrace()
                 }
             }.setNegativeButton("Cancel"){//if i click on this button
                 Dialog,_->
                 Dialog.dismiss()
             }.show()

    }
    private fun updateDateInView(){
        val myFormat="dd.MM.yyyy"
        val sdf=SimpleDateFormat(myFormat,Locale.getDefault())
        et_date.setText(sdf.format(cal.time).toString())
    }

    private fun takePhotoFromGallery(){
        Dexter.withActivity(this).withPermissions(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.CAMERA
        ).withListener(object: MultiplePermissionsListener {

            override fun onPermissionsChecked(report: MultiplePermissionsReport?){ //object containing information about the permissions.

                if (report!!.areAllPermissionsGranted()){//if permission granted
                    val GalleryIntent=Intent(MediaStore.ACTION_IMAGE_CAPTURE) //clicking
                    startActivityForResult(GalleryIntent, CAMERA)//parent activity
                }
            }
            override fun onPermissionRationaleShouldBeShown(
                permissions: MutableList<PermissionRequest>,
                token: PermissionToken )
            {
                showRationalForPermission()
            }
        })
            .onSameThread().check()// asking for permission and displaying the rationale at the same time

    }



    private fun saveImageToInternalStorage(bitmap: Bitmap):Uri{
        val wrapper=ContextWrapper(applicationContext)
    //ContextWrapper in this context is a way to access the directory specific to your application within the internal storage of the device.
    // This directory is a private area where your application can store files, including images.
   var file=wrapper.getDir(IMAGE_DIRECTORY, Context.MODE_PRIVATE)
        //trying to access application and creating directory of tht particular application where images are stored "IMAGE_DIRECTORY"
        file= File(file,"${UUID.randomUUID()}.jpg")

        try{
                val stream:OutputStream=FileOutputStream(file)//this prepares the stream for writing data to the file.
            bitmap.compress(Bitmap.CompressFormat.JPEG,100,stream)//so basically we r compressing the bitmap image into the stream (writing image data)


            //So, the purpose of this code is to persistently save the compressed image to a file on the device, even though you may have already saved the image in a directory.
        // It's a step in the process of storing the image data on the device for long-term use,

            stream.flush()//makes the buffering data to easily reach the actual file
            stream.close()//releasing any system resources associated with it.
        }catch(e:IOException){
            e.printStackTrace()
        }
        return Uri.parse(file.absolutePath)//passing the file's location as "URI"
    }
    companion object{
        private const val GALLERY=1
        private const val CAMERA=2
        private const val IMAGE_DIRECTORY="HappyPlacesImages"
        private const val PLACE_AUTOCOMPLETE_REQUEST_CODE=3
    }

}