import pyrebase

config = {
    "apiKey": "AIzaSyBXNC1f6fZzDxGERqXrf7oz0BpOJcr203c",
    "authDomain": "skindoc-8e127.firebaseapp.com",
    "databaseURL": "https://skindoc-8e127.firebaseapp.com",
    "projectId": "skindoc-8e127",
    "storageBucket": "skindoc-8e127.appspot.com",
    "messagingSenderId": "677904606327",
    "appId": "1:677904606327:web:2d63b6e7366a8fc60c9272",
    "measurementId": "G-F8ZEJR2YZY",
    "servioeAccount": "serviceAccountKey.json"
}

def upload_img(img_file):
    firebase_storage = pyrebase.initialize_app(config)
    storage = firebase_storage.storage()

    storage.child(img_file).put(img_file)
