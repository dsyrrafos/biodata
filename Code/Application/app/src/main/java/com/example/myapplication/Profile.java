package com.example.myapplication;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import android.os.Environment;
import android.os.StrictMode;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.content.Intent;
import android.view.Window;
import android.widget.Button;
import android.widget.Toast;


//import com.androidnetworking.AndroidNetworking;
//import com.androidnetworking.common.Priority;
//import com.androidnetworking.error.ANError;
//import com.androidnetworking.interfaces.JSONObjectRequestListener;
//import com.androidnetworking.interfaces.UploadProgressListener;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;


public class Profile extends AppCompatActivity implements View.OnClickListener {
    public static Uri photoURI;
    public String currentPhotoPath;
    public Bitmap bitmap;
    public String url = "https://192.168.1.12:5000/x";
    public Bitmap bitmap_lime;
    public String url_lime = null;
    public ProgressDialog progressDialog;
//    TextView melanoma = (TextView)findViewById(R.id.melanoma);
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        //AndroidNetworking.initialize(getApplicationContext());
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
        setContentView(R.layout.activity_profile);

        Button profileButton = findViewById(R.id.profileButton);
        Button cameraButton = findViewById(R.id.cameraButton);

        profileButton.setOnClickListener(this);
        cameraButton.setOnClickListener(this);

        if (ContextCompat.checkSelfPermission( Profile.this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(Profile.this, new String[]{
                    Manifest.permission.CAMERA
            }, 100);
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.profileButton:

                openProfile();
                break;
            case R.id.cameraButton:
                imagePicker();
                break;
        }

    }
    public void imagePicker(){
        final Dialog MyDialog = new Dialog(Profile.this);
        MyDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        MyDialog.setContentView(R.layout.imagepicker);

        Button camera = (Button)MyDialog.findViewById(R.id.camera);
        Button gallery = (Button)MyDialog.findViewById(R.id.gallery);
        Button cancel = (Button)MyDialog.findViewById(R.id.cancel);



        camera.setEnabled(true);
        gallery.setEnabled(true);
        cancel.setEnabled(true);

        camera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openCamera();
//                Toast.makeText(getApplicationContext(), "Hello, I'm Custom Alert Dialog", Toast.LENGTH_LONG).show();
                MyDialog.cancel();
            }
        });
        gallery.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openGallery();
//                MyDialog.cancel();
                MyDialog.cancel();
            }
        });
        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MyDialog.cancel();
            }
        });

        MyDialog.show();
    }

    public void openProfile() {
        Intent intent = new Intent(this, Profile.class);
        startActivity(intent);
    }

    public void openGallery() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, false);
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), 30);
    }

    @SuppressLint("QueryPermissionsNeeded")
    public void openCamera() {
//            ImagePicker.with(this)
//                    .start(20);
        Intent takePictureIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE); // Intent για λειτουργία φωτογραφίας
//            System.out.println(takePictureIntent.resolveActivity(getPackageManager()));
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = createImageFile(); // Δημιουργία αρχείου
            } catch (IOException ex) {
                Log.d("PHOTOTAG", "OOPS SOmEthing Happened");
            }
            if (photoFile != null) {
                photoURI = FileProvider.getUriForFile(this,
                        "com.example.android.fileprovider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI); // Πέρασμα uri φωτογραφίας μέσω του intent
                startActivityForResult(takePictureIntent, 20); // Εκκίνηση του Activity result
            }
        }
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        @SuppressLint("SimpleDateFormat") String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date()); // Δημιουργία timestamp
        String imageFileName = "JPEG_" + timeStamp + "_"; // Όνομα αρχείου βάσει του timestamp
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES); // Εύρεση εξωτερικής τοποθεσίας
        File image = File.createTempFile(   // Δημιουργία προσωρινού αρχείου
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );
        // Save a file: path for use with ACTION_VIEW intents
        currentPhotoPath = image.getAbsolutePath();
        return image; // Επιστροφή αρχείου εικόνας
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        String postUrl= "http://"+"192.168.1.12"+":"+5000+"/x";
        System.out.println("aaaa!");
        if (requestCode == 20) {
            System.out.println("camera!");
            try {
                System.out.println("camera!");
                // Δημιουργία δύο αρχείων
                //File fileStorage = new File(currentPhotoPath); // Αρχείο για μεταφόρτωση στο Firebase Storage
                //File fileFireStore = new File(currentPhotoPath+1); // Αρχείο για μεταφόρτωση στο Server Ταξινόμησης
                File finalFile = new File(currentPhotoPath);
                try {
                    //Bitmap photo = (Bitmap) data.getExtras().get("data");
                    //imageView.setImageBitmap(photo);
                    System.out.println("inside camera!");
                    // CALL THIS METHOD TO GET THE URI FROM THE BITMAP
                    Bitmap bitmap = getBitmapFormUri(Profile.this, photoURI, 1024,1024); // Κλιμάκωση αρχείου Firebase Storage και επιστροφή σε bitmap

                    // CALL THIS METHOD TO GET THE ACTUAL PATH
//                    Bitmap bitmapStorage = getBitmapFormUri(GalleryGrid.this, photoURI, 1000,500); // Κλιμάκωση αρχείου Firebase Storage και επιστροφή σε bitmap
//                    Bitmap bitmapFireStore = getBitmapFormUri(GalleryGrid.this, photoURI, 331,331); // Κλιμάκωση αρχείου Server Ταξινόμησης και επιστροφή σε bitmap
                    ByteArrayOutputStream stream = new ByteArrayOutputStream();
                    //FileOutputStream out = new FileOutputStream(finalFile); // Ροή εξόδου του Bitmap στο αρχείο του Firebase Storage
                    //FileOutputStream out2 = new FileOutputStream(fileFireStore); // Ροη εξόδου του Bitmao στο αρχείο του Server Ταξινόμησης
                    bitmap = fixImage(bitmap); // Διόρθωση σε τυχόν θέμα γωνίας αποθήκευσης φωτογραφίας
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream); // Αποθήκευση αλλαγών στο αρχείο του Firebase Storage
                    byte[] byteArray = stream.toByteArray();
                    RequestBody postBodyImage = new MultipartBody.Builder()
                            .setType(MultipartBody.FORM)
                            .addFormDataPart("image", "androidFlask.jpg", RequestBody.create(MediaType.parse("image/*jpg"), byteArray))
                            .build();


                    postRequest(postUrl, postBodyImage);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } catch (Exception e) {
                e.printStackTrace();
               // startActivity(new Intent(this, GalleryGrid.class)); // Σε περίπτωση προβλήματος επιστροφή στη βασική οθόνη της εφαρμογής (πλέγμα εικόνων)
            }
        }
        if (requestCode == 30 && data != null){
            Uri loadURI = data.getData(); // Τοποθεσία εικόνας στη συσκευή
            try {
                File f = createImageFile(); // Δημιουργία αρχείου
                try{
                    ByteArrayOutputStream stream = new ByteArrayOutputStream();
                    Bitmap bitmap = getBitmapFormUri(Profile.this, loadURI, 1024,1024); // Κλιμάκωση εικόνας και δημιουργία bitmap
                    bitmap = fixImage(bitmap); // Διόρθωση γωνίας αποθήκευσης εικόνας
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream); // Συμπίεση εικόνας που επιλέχθηκε για ταξινόμηση
                    byte[] byteArray = stream.toByteArray();
                    RequestBody postBodyImage = new MultipartBody.Builder()
                            .setType(MultipartBody.FORM)
                            .addFormDataPart("image", "androidFlask.jpg", RequestBody.create(MediaType.parse("image/*jpg"), byteArray))
                            .build();

                    postRequest(postUrl, postBodyImage);


                }
                catch(Exception e){
                    e.printStackTrace();
                    startActivity(new Intent(this, Profile.class)); // Επιστροφή στην κύρια οθόνη σε περίπτωση λάθους
                }
            }
            catch (Exception e) {
                e.printStackTrace();
                startActivity(new Intent(this, Profile.class)); // Επιστροφή στην κύρια οθόνη σε περίπτωση λάθους
            }


        }
    }

    void postRequest(String postUrl, RequestBody postBody) {

        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .build();

        progressDialog = new ProgressDialog(Profile.this);
        progressDialog.show();
        progressDialog.setContentView(R.layout.progress_dialog);
        progressDialog.getWindow().setBackgroundDrawableResource(
                android.R.color.transparent
        );

        Request request = new Request.Builder()
                .url(postUrl)
                .post(postBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                // Cancel the post on failure.
                call.cancel();

                // In order to access the TextView inside the UI thread, the code is executed inside runOnUiThread()
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(Profile.this, "Something went wrong:" + " " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        progressDialog.cancel();
                    }
                });
            }




            @Override
            public void onResponse(Call call, final Response response) throws IOException {
                // In order to access the TextView inside the UI thread, the code is executed inside runOnUiThread()
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
//                        ObjectMapper objectMapper = new ObjectMapper();
//                        ResponseBody responseBody = client.newCall(request).execute().body();
//                        SimpleEntity entity = objectMapper.readValue(responseBody.string(), SimpleEntity.class);
//                        try {
//                            String json = EntityUtils.toString(response.getEntity());
//                            JSONParser jsonParser=new JSONParser();
//                            JSONObject json=(JSONObject) jsonParser.parse(response.toString().replace("HttpResponseProxy",""));
//                            System.out.println(json.toString());
//                            BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent(), "UTF-8"));
//                            @Nullable ResponseBody resBody = response.body();
                            System.out.println("OnResponse");
//                            InputStream inputStream = response.body().byteStream();
//                            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);





                        try {
                            String prediction = response.body().string();
////                            melanoma.setText(prediction);
                            download(prediction);
                            //System.out.println(url_lime);
                            //prediction(prediction, url_lime);
                            //Toast.makeText(Profile.this, prediction, Toast.LENGTH_LONG).show();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }





//                            JSONObject Jobject = new JSONObject(jsonData);

//                            String json = reader.readLine();
//                            JSONTokener tokener = new JSONTokener(json);
//                            JSONArray finalResult = new JSONArray(tokener);

//

//                            Toast.makeText(Profile.this, "ssss", Toast.LENGTH_LONG).show();
//                        }
//                        catch (IOException e) {
//                            Toast.makeText(Profile.this,"Exception", Toast.LENGTH_LONG).show();
//                            e.printStackTrace();
//                        }
                    }
                });
            }
        });
    }

    public void download(String prediction) {
        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference imageRef = storage.getReference().child("lime_androidFlask.jpg");

//        imageRef.getBytes(4096*4096)
//                .addOnSuccessListener(new OnSuccessListener<byte[]>() {
//                    @Override
//                    public void onSuccess(byte[] bytes) {
//                        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
//                        prediction(prediction, bitmap);
//                    }
//                }).addOnFailureListener(new OnFailureListener() {
//            @Override
//            public void onFailure(@NonNull Exception e) {
//                Toast.makeText(Profile.this, e.getMessage(), Toast.LENGTH_LONG).show();
//            }
//        });
        
        imageRef.getDownloadUrl()
                .addOnSuccessListener(new OnSuccessListener<Uri>() {
                    @Override
                    public void onSuccess(Uri uri) {
                        url_lime = uri.toString();
                        System.out.println(url_lime);
                        prediction(prediction, url_lime);
                    }
                });
        System.out.println(url_lime);
    }

    public void prediction(String prediction, String url_lime) {
        Intent intent = new Intent(this, Diagnosis.class);
        intent.putExtra("prediction", prediction);
        intent.putExtra("url", url_lime);
        progressDialog.cancel();
        startActivity(intent);


    }

    public String generateLabel(String dummyValue, int count){
        return dummyValue+count; // TimeStamp + αριθμός
    }

    public String getRealPathFromURI(Uri uri) {
        String path = "";
        if (getContentResolver() != null) {
            Cursor cursor = getContentResolver().query(uri, null, null, null, null);
            if (cursor != null) {
                cursor.moveToFirst();
                int idx = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
                path = cursor.getString(idx);
                cursor.close();
            }
        }
        return path;
    }

    public Uri getImageUri(Context inContext, Bitmap inImage) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        inImage.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
        String path = MediaStore.Images.Media.insertImage(inContext.getContentResolver(), inImage, "Title", null);
        return Uri.parse(path);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private int getCameraAngle(){ // Γωνία αποθήκευσης εικόνας απο τη συσκευή
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        int orientation = 0;
        try{
            String cameraID = manager.getCameraIdList()[1];
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraID);
            orientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
            System.out.println(orientation);
            return orientation;
        }
        catch(Exception e){
            return orientation;
        }
    }

    public static Bitmap getBitmapFormUri(Activity ac, Uri uri, float hh, float ww) throws FileNotFoundException, IOException { // Κλιμάκωση εικόνων
        InputStream input = ac.getContentResolver().openInputStream(uri);
        BitmapFactory.Options onlyBoundsOptions = new BitmapFactory.Options();
        onlyBoundsOptions.inJustDecodeBounds = true;
        onlyBoundsOptions.inDither = true;//optional
        onlyBoundsOptions.inPreferredConfig = Bitmap.Config.ARGB_8888;//optional
        BitmapFactory.decodeStream(input, null, onlyBoundsOptions);
        input.close();
        int originalWidth = onlyBoundsOptions.outWidth;
        int originalHeight = onlyBoundsOptions.outHeight;
        if ((originalWidth == -1) || (originalHeight == -1))
            return null;
        //Image resolution is based on 480x800
        //float hh = 800f;//The height is set as 800f here
        //float ww = 480f;//Set the width here to 480f

        //float hh = 1000f;//The height is set as 800f here
        //float ww = 500f;//Set the width here to 480f
        //Zoom ratio. Because it is a fixed scale, only one data of height or width is used for calculation
        int be = 1;//be=1 means no scaling

        if (originalWidth > originalHeight && originalWidth > ww) {//If the width is large, scale according to the fixed size of the width
            be = (int) (originalWidth / ww);
        } else if (originalWidth < originalHeight && originalHeight > hh) {//If the height is high, scale according to the fixed size of the width
            be = (int) (originalHeight / hh);
        }
        if (be <= 0)
            be = 1;
        System.out.println(be);
        //Proportional compression
        BitmapFactory.Options bitmapOptions = new BitmapFactory.Options();
        bitmapOptions.inSampleSize = be;//Set scaling
        bitmapOptions.inDither = true;//optional
        bitmapOptions.inPreferredConfig = Bitmap.Config.ARGB_8888;//optional
        input = ac.getContentResolver().openInputStream(uri);
        Bitmap bitmap = BitmapFactory.decodeStream(input, null, bitmapOptions);
        input.close();

        return compressImage(bitmap);//Mass compression again
    }
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public Bitmap fixImage(Bitmap bitmap){
        int cameraAngle = getCameraAngle(); // Εύρεση γωνίας εικόνας
        String manufacturer = Build.MANUFACTURER;
        if(manufacturer.equals("XIAOMI")||manufacturer.equals("Xiaomi")||manufacturer.equals("Samsung")||manufacturer.equals("SAMSUNG")) { // Διαφορετικές γωνίες κάποιων κατασκευαστών
            if(cameraAngle == 180){bitmap = rotateBitmap(bitmap,180);} // Αντίστοιχη περιστροφή
            else if(cameraAngle == 270){bitmap = rotateBitmap(bitmap,90);}

        }return bitmap;
    }

    public Bitmap rotateBitmap(Bitmap original, float degrees) { // Περιστροφή Bitmap
        Matrix matrix = new Matrix();
        matrix.preRotate(degrees);
        Bitmap rotatedBitmap = Bitmap.createBitmap(original, 0, 0, original.getWidth(), original.getHeight(), matrix, true);
        original.recycle();
        return rotatedBitmap;
    }

    /**
     * Mass compression method
     *
     * @param image
     * @return
     */
    public static Bitmap compressImage(Bitmap image) { // Περαιτέρω συμπίεση εικόνων
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        image.compress(Bitmap.CompressFormat.JPEG, 100, baos);//Quality compression method, here 100 means no compression, store the compressed data in the BIOS
        int options = 100;
        while (baos.toByteArray().length / 1024 > 100) {  //Cycle to determine if the compressed image is greater than 100kb, greater than continue compression
            baos.reset();//Reset the BIOS to clear it
            //First parameter: picture format, second parameter: picture quality, 100 is the highest, 0 is the worst, third parameter: save the compressed data stream
            image.compress(Bitmap.CompressFormat.JPEG, options, baos);//Here, the compression options are used to store the compressed data in the BIOS
            options -= 10;//10 less each time
        }
        ByteArrayInputStream isBm = new ByteArrayInputStream(baos.toByteArray());//Store the compressed data in ByteArrayInputStream
        Bitmap bitmap = BitmapFactory.decodeStream(isBm, null, null);//Generate image from ByteArrayInputStream data
        return bitmap;
    }
}






