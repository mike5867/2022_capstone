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
#     display_name: Python 3 (ipykernel)
#     language: python
#     name: python3
# ---

# +
import shutil
import glob
import random
import os

from bs4 import BeautifulSoup

default_path='G:/full_dataset/'


# -

def xml_list(start,end): #start~end 폴더 번호
    s=r'G:/walking_video/bounding_box/Bbox_'
    xml_list=[]
    for i in range(start,end+1):
        path=s+str(i).zfill(4)+"/*.xml"
        file=glob.glob(path)
        xml_list.append(file[0]) #경로당 xml파일 1개이기때문
    
    return xml_list


def box_size(obj):
    xtl=float(obj['xtl'])
    ytl=float(obj['ytl'])
    xbr=float(obj['xbr'])
    ybr=float(obj['ybr'])
    
    width=xbr-xtl
    height=ybr-ytl    
    
    if width<200 or height<200:
        return False
    else:
        return True


def data_setting(xml_list):
    
    imgs=[]
    for xml in xml_list:
        f=open(xml,'rt',encoding='UTF-8')
        soup=BeautifulSoup(f,"lxml")
        bicycle_list=soup.find_all("box",{"label":"bicycle","occluded":"0"})
        scooter_list=soup.find_all("box",{"label":"scooter","occluded":"0"})
        object_list=bicycle_list+scooter_list
        
        for obj in object_list:
            #if box_size(obj):
                img_name=(obj.parent["name"])[:-4] #확장자 제거
                path=xml[:40]+(obj.parent["name"])
                
                annot_file=open(default_path+'train/annotation/'+img_name+'.xml','a')
                annot_file.write(str(obj))
                annot_file.write('\n')
                annot_file.close()
                
                imgs.append(path)
        
    f.close()
    imgs=list(set(imgs))

    for img in imgs:
        shutil.copy2(img,default_path'train')
        


# +
def divide_random(N):
    img_list=[img for img in os.listdir(default_path+'train') if img.endswith(".jpg")]
    idx=random.sample(range(len(img_list)),N)
    
    for i in idx:
        img_path=default_path+'train/'+img_list[i]
        img_annotation_path=default_path+'train/annotation/'+(img_list[i])[:-4]+'.xml'
        shutil.move(img_path,default_path+'test')
        shutil.move(img_annotation_path,default_path+'test/annotation')
        
    
# -

xmls=xml_list(1,690)

data_setting(xmls)

divide_random(200)


