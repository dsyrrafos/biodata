DEBUG = True
import os
import sys
import time
import numpy as np
import pandas as pd
import cv2
import PIL.Image
import matplotlib.pyplot as plt
import seaborn as sns
from tqdm.notebook import tqdm
from sklearn.metrics import roc_auc_score
from skimage.segmentation import mark_boundaries
import json
from lime import lime_image
from PIL import Image


import torch
from torch.utils.data import TensorDataset, DataLoader, Dataset
import torch.nn as nn
import torch.nn.functional as F
import albumentations as A
import geffnet

torch.cuda.empty_cache()

TORCH_MAJOR = int(torch.__version__.split('.')[0])
TORCH_MINOR = int(torch.__version__.split('.')[1])

if TORCH_MAJOR == 1 and TORCH_MINOR < 8:
    from torch._six import container_abcs
else:
    import collections.abc as container_abcs

#Μήνυμα στο #general

device = torch.device('cuda')

# MODEL INFO
kernel_type = '9c_b7ns_1e_640_ext_15ep'
image_size = 640
use_amp = False
data_dir = 'jpeg-melanoma-768x768'
data_dir2 = 'jpeg-isic2019-768x768'
model_dir = 'melanoma-winning-models'
enet_type = 'efficientnet-b7'
batch_size = 16
num_workers = 4
out_dim = 9

use_meta = False
use_external = '_ext' in kernel_type

# READ CSV
df_test = pd.read_csv(os.path.join(data_dir, 'test.csv'))
df_test['filepath'] = df_test['image_name'].apply(lambda x: os.path.join(data_dir, 'test', f'{x}.jpg'))
df_train = pd.read_csv(os.path.join(data_dir, 'train.csv'))
df_train = df_train[df_train['tfrecord'] != -1].reset_index(drop=True)
df_train['fold'] = df_train['tfrecord'] % 5
tfrecord2fold = {
    2:0, 4:0, 5:0,
    1:1, 10:1, 13:1,
    0:2, 9:2, 12:2,
    3:3, 8:3, 11:3,
    6:4, 7:4, 14:4,
}
df_train['fold'] = df_train['tfrecord'].map(tfrecord2fold)
df_train['is_ext'] = 0
df_train['filepath'] = df_train['image_name'].apply(lambda x: os.path.join(data_dir, 'train', f'{x}.jpg'))

df_train['diagnosis'] = df_train['diagnosis'].apply(lambda x: x.replace('seborrheic keratosis', 'BKL'))
df_train['diagnosis'] = df_train['diagnosis'].apply(lambda x: x.replace('lichenoid keratosis', 'BKL'))
df_train['diagnosis'] = df_train['diagnosis'].apply(lambda x: x.replace('solar lentigo', 'BKL'))
df_train['diagnosis'] = df_train['diagnosis'].apply(lambda x: x.replace('lentigo NOS', 'BKL'))
df_train['diagnosis'] = df_train['diagnosis'].apply(lambda x: x.replace('cafe-au-lait macule', 'unknown'))
df_train['diagnosis'] = df_train['diagnosis'].apply(lambda x: x.replace('atypical melanocytic proliferation', 'unknown'))

#df_train['diagnosis'].value_counts()

if use_external:
    df_train2 = pd.read_csv(os.path.join(data_dir2, 'train.csv'))
    df_train2 = df_train2[df_train2['tfrecord'] >= 0].reset_index(drop=True)
    df_train2['fold'] = df_train2['tfrecord'] % 5
    df_train2['is_ext'] = 1
    df_train2['filepath'] = df_train2['image_name'].apply(lambda x: os.path.join(data_dir2, 'train', f'{x}.jpg'))

    df_train2['diagnosis'] = df_train2['diagnosis'].apply(lambda x: x.replace('NV', 'nevus'))
    df_train2['diagnosis'] = df_train2['diagnosis'].apply(lambda x: x.replace('MEL', 'melanoma'))
    df_train = pd.concat([df_train, df_train2]).reset_index(drop=True)

diagnosis2idx = {d: idx for idx, d in enumerate(sorted(df_train.diagnosis.unique()))}
df_train['target'] = df_train['diagnosis'].map(diagnosis2idx)
mel_idx = diagnosis2idx['melanoma']

# diagnosis2idx

# DATASET

class SIIMISICDataset(Dataset):
    def __init__(self, csv, split, mode, transform=None):

        self.csv = csv.reset_index(drop=True)
        self.split = split
        self.mode = mode
        self.transform = transform

    def __len__(self):
        return self.csv.shape[0]

    def __getitem__(self, index):
        row = self.csv.iloc[index]
        
        image = cv2.imread(row.filepath)
        image = image[:, :, ::-1]

        if self.transform is not None:
            res = self.transform(image=image)
            image = res['image'].astype(np.float32)
        else:
            image = image.astype(np.float32)

        image = image.transpose(2, 0, 1)

        if self.mode == 'test':
            return torch.tensor(image).float()
        else:
            return torch.tensor(image).float(), torch.tensor(self.csv.iloc[index].target).long()

transforms_val = A.Compose([
    A.Resize(image_size, image_size),
    A.Normalize()
])

# MODEL

class enetv2(nn.Module):
    def __init__(self, backbone, out_dim, n_meta_features=0, load_pretrained=False):

        super(enetv2, self).__init__()
        self.n_meta_features = n_meta_features
        self.enet = geffnet.create_model(enet_type.replace('-', '_'), pretrained=load_pretrained)
        self.dropout = nn.Dropout(0.5)

        in_ch = self.enet.classifier.in_features
        self.myfc = nn.Linear(in_ch, out_dim)
        self.enet.classifier = nn.Identity()

    def extract(self, x):
        x = self.enet(x)
        return x

    def forward(self, x, x_meta=None):
        x = self.extract(x).squeeze(-1).squeeze(-1)
        x = self.myfc(self.dropout(x))
        return x

# FOLDS

def get_trans(img, I):
    if I >= 4:
        img = img.transpose(2,3)
    if I % 4 == 0:
        return img
    elif I % 4 == 1:
        return img.flip(2)
    elif I % 4 == 2:
        return img.flip(3)
    elif I % 4 == 3:
        return img.flip(2).flip(3)

# PREDICT
n_test = 8
models_list = []
for i_fold in range(5):
    model = enetv2(enet_type, n_meta_features=0, out_dim=out_dim)
    model = model.to(device)
    model_file = os.path.join(model_dir, f'{kernel_type}_best_fold{i_fold}.pth')
#    state_dict = torch.load(model_file,map_location=torch.device('cpu'))
    state_dict = torch.load(model_file)
    state_dict = {k.replace('module.', ''): state_dict[k] for k in state_dict.keys()}
    model.load_state_dict(state_dict, strict=True)
    model.eval()
    models_list.append(model)
# len(models_list)


# SINGLE IMAGE INFERENCE
import csv

def single_img_infer(f1):
    #patient test images processing
    # patient 1 image name
    in1 = 'hello'
    # patient 1 patient_id
    pi1 = 'IP23232'
    # patient 1 sex
    s1 = 'male'
    # patient 1 age
    a1 = 70.0
    # patient 1 anatom_site_general_challenge. where its located
    asgc1 = 'torso'
    # patient 1 image filepath
    #f1 = '../input/jpeg-melanoma-768x768/test/ISIC_0052060.jpg'
    with open('datasettesting.csv', 'w', newline='') as file:
        writer = csv.writer(file)
        writer.writerow(["image_name", "patient_id", "sex", "age_approx", "anatom_site_general_challenge", "width", "height", "filepath"])
        writer.writerow([in1,pi1, s1, a1, asgc1, 6000, 4000, f1])

    df_single_image = pd.read_csv('datasettesting.csv')
    # df_single_image

    # as we only read a single image, so we don't need a dataloader
    dataset_test = SIIMISICDataset(df_single_image, 'test', 'test', transform=transforms_val)
    image = dataset_test[0]  
    image = image.to(device).unsqueeze(0)  # a single image need to be added a new axis to act like batch_size = 1

    with torch.no_grad():
        probs = torch.zeros((image.shape[0], out_dim)).to(device)
        for model in models_list:
            for I in range(n_test):
                l = model(get_trans(image, I))
                probs += l.softmax(1)

    prediction = probs[:, mel_idx].item()

    fname = lime_explain(f1, image)
    return prediction, fname

def lime_infer_image(image):
    #print('image shape',image.shape)
    image = image.reshape((1,3,640,640))
    image = torch.tensor(image).to(device, dtype=torch.float)
    with torch.no_grad():
        probs = torch.zeros((image.shape[0], out_dim)).to(device)
        for model in models_list:
            for I in range(n_test):
                l = model(get_trans(image, I))
                probs += l.softmax(1)
    print('provs',probs)
    prediction = probs[:, mel_idx].item()
    #print(type(prediction))
    return probs.cpu().numpy()

def lime_explain(f1, image):
    explainer = lime_image.LimeImageExplainer()
    image = image.cpu().numpy().astype('double')
    image = image[0].transpose((1,2,0))
    explanation = explainer.explain_instance(image, lime_infer_image, batch_size=1, top_labels=9, hide_color=0, num_samples=10) # number of images that will be sent to classification function

    temp, mask = explanation.get_image_and_mask(explanation.top_labels[0], positive_only=True, negative_only=False, num_features=10, hide_rest=False)
    img_boundry1 = mark_boundaries(temp, mask, color=(1, 1, 1), mode='thick', outline_color=(1,1,1))

    fname = 'lime_'+f1
    im = Image.fromarray((img_boundry1 * 255).astype(np.uint8))
    im = im.save(fname)

    return fname
