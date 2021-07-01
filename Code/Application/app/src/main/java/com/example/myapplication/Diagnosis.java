package com.example.myapplication;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

public class Diagnosis extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_diagnosis);
        Intent intent = getIntent();
        String prediction = intent.getStringExtra("prediction");
        String url = intent.getStringExtra("url");
        //Bitmap bitmap = (Bitmap) intent.getParcelableExtra("bitmap");
        System.out.println(prediction);
        TextView melanoma = (TextView)findViewById(R.id.melanoma);
        Button lime_button = (Button) findViewById(R.id.button3);
        //TextView url_lime = (TextView)findViewById(R.id.url);
        //ImageView view = (ImageView)findViewById(R.id.imageView);
        //view.setImageBitmap(bitmap);
        melanoma.setText(prediction);
        //url_lime.setText(url);
        lime_button.setEnabled(true);
        lime_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Uri uri = Uri.parse(url);
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                startActivity(intent);
            }
        });
    }
}