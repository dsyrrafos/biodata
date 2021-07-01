from flask import Flask, request, jsonify
import werkzeug
import matplotlib.pyplot as plt 
import numpy as np
import single_image_inference as single_image
from io import BytesIO
from PIL import Image
import time
import firebase_python




app = Flask(__name__)


@app.route('/', methods=['GET', 'POST'])
def handle_request():

    return "Successful Connection"

@app.route('/login', methods=['GET', 'POST'])
def handle_login():
    content = request.json
    print(content['username'])
    print(content['password'])
    return "Login"
    
    

@app.route('/x', methods = ['GET', 'POST'])
def handle_image_request():
    imagefile = request.files['image']
    filename = werkzeug.utils.secure_filename(imagefile.filename)
    print("\nReceived image File name : " + imagefile.filename)
    imagefile.save(filename)
    result, fname = single_image.single_img_infer("androidFlask.jpg")
    result = round(result*100, 3)
    firebase_python.upload_img(fname)
    return str(result) + ' %'

app.run(host="0.0.0.0", port=5000, debug=True)
