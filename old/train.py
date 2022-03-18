# ---
# jupyter:
#   jupytext:
#     formats: ipynb,py:light
#     text_representation:
#       extension: .py
#       format_name: light
#       format_version: '1.5'
#       jupytext_version: 1.13.7
#   kernelspec:
#     display_name: capstone
#     language: python
#     name: capstone
# ---

# +
# !pip3 install torch==1.10.1+cu113 torchvision==0.11.2+cu113 torchaudio===0.10.1+cu113 -f https://download.pytorch.org/whl/cu113/torch_stable.html

# !pip install matplotlib

# !pip install beautifulsoup4

# !pip install lxml

# !pip install opencv-python

# !pip3 install albumentations

# !pip install --upgrade albumentations

# +
import shutil
import glob
import matplotlib.pyplot as plt
import matplotlib.patches as patches
from bs4 import BeautifulSoup
import os

from PIL import Image
import cv2
import numpy as np
import torch
import torchvision
from torch.utils.data import Dataset
from torchvision import transforms,datasets,models
from torchvision.models.detection.faster_rcnn import FastRCNNPredictor
import albumentations
import albumentations.pytorch

import time
from tqdm import tqdm

from collections import Counter
import utils_ObjectDetection as utils
# -

os.environ['CUDA_LAUNCH_BLOCKING'] = "1"


def generate_box(obj):
    xtl=float(obj['xtl'])
    ytl=float(obj['ytl'])
    xbr=float(obj['xbr'])
    ybr=float(obj['ybr'])
    
    return [xtl,ytl,xbr,ybr]


# +
adjust_label=0

def generate_label(obj):
    if obj['label']=="bicycle":
        return 1 + adjust_label
    elif obj['label']=="scooter":
        return 2 + adjust_label
    return 0 + adjust_label


# -

def generate_target(path):
    f=open(path,'rt',encoding='UTF-8')    
    soup=BeautifulSoup(f,"lxml")
    
    tag=soup.find_all("box")
    boxes=[]
    labels=[]
    
    for i in tag:
        boxes.append(generate_box(i))
        labels.append(generate_label(i))
        
    boxes=torch.as_tensor(boxes,dtype=torch.float32)
    labels=torch.as_tensor(labels,dtype=torch.int64)
    
    target={}
    target["boxes"]=boxes
    target["labels"]=labels
    
    f.close()
    
    return target


def plot_image(img,annotation):
    img=img.permute(1,2,0)
    
    fig,ax=plt.subplots(1)
    
    ax.imshow(img)
    for idx in range(len(annotation["boxes"])):
        xtl,ytl,xbr,ybr=annotation["boxes"][idx]
        
        if annotation["labels"][idx]==1: #bicycle
            rect=patches.Rectangle((xtl,ytl),(xbr-xtl),(ybr-ytl),linewidth=1,edgecolor='g',facecolor='none')
        elif annotation["labels"][idx]==2: #scooter
            rect=patches.Rectangle((xtl,ytl),(xbr-xtl),(ybr-ytl),linewidth=1,edgecolor='b',facecolor='none')
        else:
            rect=patches.Rectangle((xtl,ytl),(xbr-xtl),(ybr-ytl),linewidth=1,edgecolor='r',facecolor='none')
        ax.add_patch(rect)
    
    plt.show()         


# +
class AlbumentationDataset(Dataset):
    def __init__(self,path,transform=None):
        self.path=path
        self.transform=transform
        
        imgs=[img for img in os.listdir(self.path) if img.endswith('.jpg')]
        self.imgs=imgs
        
    def __len__(self):
        return len(self.imgs)
    
    def __getitem__(self,idx):
        img_path=self.path+self.imgs[idx];
        annotation_path=self.path+'annotation/'+(self.imgs[idx])[:-4]+'.xml';
        
        image=cv2.imread(img_path)
        image=cv2.cvtColor(image,cv2.COLOR_BGR2RGB)
        
        target=generate_target2(annotation_path)
        
        #albumentation
        if self.transform:
            transformed=self.transform(image=image,bboxes=target['boxes'],labels=target['labels'])
            image=transformed['image']
            target={'boxes':transformed['bboxes'],'labels':transformed['labels']}
            
        target['boxes']=torch.as_tensor(target['boxes'],dtype=torch.float32)
        target['labels']=torch.as_tensor(target['labels'],dtype=torch.int64)
        
        

        #normalize
        image=image.float()
        image=image/255
        return image,target
    
def collate_fn(batch):
    return tuple(zip(*batch))


# -

albumentations_transform=albumentations.Compose([
    albumentations.RandomSizedBBoxSafeCrop(width=1280,height=720,erosion_rate=0.1),
    albumentations.pytorch.transforms.ToTensorV2()],
    bbox_params=albumentations.BboxParams(format='pascal_voc',label_fields=['labels']),
)


def get_num_objects_for_each_class(dataset):
    
    total_labels=[]
    for img,annot in tqdm(dataset,position=0,leave=True):
        total_labels+=[int(i) for i in annot['labels']]
        
    return Counter(total_labels)


def Faster_RCNN(num_classes):
    model=torchvision.models.detection.fasterrcnn_resnet50_fpn(pretrained=True)
    in_features=model.roi_heads.box_predictor.cls_score.in_features
    model.roi_heads.boxx_predictor=FastRCNNPredictor(in_features,num_classes)
    
    return model


def make_prediction(model,img,threshold):
    model.eval()
    preds=model(img)
    for id in range(len(preds)):
        idx_list=[]
        
        for idx,score in enumerate(preds[id]['scores']):
            if score>threshold:
                idx_list.append(idx)
                
        preds[id]['boxes']=preds[id]['boxes'][idx_list]
        preds[id]['labels']=preds[id]['labels'][idx_list]
        preds[id]['scores']=preds[id]['scores'][idx_list]
        
        
    return preds


# +
path='G:/dataset/train'
dataset=AlbumentationDataset(path=path,transform=albumentations_transform)
data_loader=torch.utils.data.DataLoader(dataset,batch_size=2, collate_fn=collate_fn)

test_path='G:/dataset/test/'
test_dataset=AlbumentationDataset(path=test_path,transform=albumentations_transform)
test_data_loader=torch.utils.data.DataLoader(test_dataset,batch_size=2, collate_fn=collate_fn)
# -

print(get_num_objects_for_each_class(dataset))
print(get_num_objects_for_each_class(test_dataset))

# +
model=Faster_RCNN(3)

device=torch.device('cuda') if torch.cuda.is_available() else torch.device('cpu')
model.to(device)

# +
torch.cuda.empty_cache()
num_epochs=50
learning_rate=0.0005

params=[p for p in model.parameters() if p.requires_grad]
optimizer=torch.optim.SGD(params, lr=learning_rate, momentum=0.9, weight_decay=0.0005)

len_dataloader=len(data_loader)


for epoch in range(num_epochs):
    start=time.time()
    model.train()
    
    zero=0
    i=0
    epoch_loss=0
    for images, targets in tqdm(data_loader):
        images=list(image.to(device) for image in images)
        targets=[{k:v.to(device) for k,v in t.items()} for t in targets]

        loss_dict=model(images,targets)
        
        losses=sum(loss for loss in loss_dict.values())
        
        i+=1
        
        optimizer.zero_grad()
        losses.backward()
        optimizer.step()
        
        epoch_loss+=losses
        
    print(epoch,epoch_loss,f'time: {time.time()-start}')
        

# +
#save
torch.save(model.state_dict(),f'Faster R-CNN_{num_epochs}_{learning_rate}.pt')

#load
model.load_state_dict(torch.load(f'retina_50_0.001.pt'))
device=torch.device('cuda') if torch.cuda.is_available() else torch.device('cpu')
model.to(device)

# +
labels=[]
preds_adj_all=[]
annot_all=[]

for im,annot in tqdm(test_data_loader,position=0,leave=True):
    im=list(img.to(device) for img in im)
    
    for t in annot:
        labels+=t['labels']
        
    with torch.no_grad():
        preds_adj=make_prediction(model,im,0.5)
        preds_adj=[{k:v.to(torch.device('cpu')) for k,v in t.items()} for t in preds_adj]
        preds_adj_all.append(preds_adj)
        annot_all.append(annot)

# +
import random

total=range(len(test_data_loader))
select=random.sample(total,10);

for i in select:
    test_annot=annot_all[i][0]
    img,annot=test_dataset[i]

    plot_image(img,annot)
    plot_image(img,test_annot)

# +
sample_metrics=[]
for batch_i in range(len(preds_adj_all)):
    sample_metrics+=utils.get_batch_statistics(preds_adj_all[batch_i],annot_all[batch_i],iou_threshold=0.5)
    
true_positives,pred_scores,pred_labels=[torch.cat(x,0) for x in list(zip(*sample_metrics))]
precision,recall,AP,f1,ap_class=utils.ap_per_class(true_positives,pred_scores,pred_labels,torch.tensor(labels))
mAP=torch.mean(AP)
print(f'mAP : {mAP}')
print(f'AP : {AP}')
# -


