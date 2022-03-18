import albumentations.pytorch.transforms
import cv2
import os
import torch
from torch.utils.data import Dataset
from bs4 import BeautifulSoup


def generate_box(obj):
    xtl = float(obj['xtl'])
    ytl = float(obj['ytl'])
    xbr = float(obj['xbr'])
    ybr = float(obj['ybr'])

    return [xtl, ytl, xbr, ybr]


adjust_label = 0
def generate_label(obj):
    if obj['label'] == "bicycle":
        return 1 + adjust_label
    elif obj['label'] == "scooter":
        return 2 + adjust_label


def generate_target(path):
    f = open(path, 'rt', encoding='UTF-8')
    soup = BeautifulSoup(f, "lxml")

    tag = soup.find_all("box")
    boxes = []
    labels = []

    for i in tag:
        boxes.append(generate_box(i))
        labels.append(generate_label(i))

    boxes = torch.as_tensor(boxes, dtype=torch.float32)
    labels = torch.as_tensor(labels, dtype=torch.int64)

    target = {}
    target["boxes"] = boxes
    target["labels"] = labels

    f.close()

    return target

class AlbumentationDataset(Dataset):
    def __init__(self, path, transform=None):
        self.path = path
        self.transform = transform

        imgs = [img for img in os.listdir(self.path) if img.endswith('.jpg') or img.endswith('.png')]
        self.imgs = imgs

    def __len__(self):
        return len(self.imgs)

    def __getitem__(self, idx):
        img_path = self.path + self.imgs[idx]
        annotation_path = self.path + 'annotation/' + (self.imgs[idx])[:-4] + '.xml'

        image = cv2.imread(img_path)
        image = cv2.cvtColor(image, cv2.COLOR_BGR2RGB)

        target = generate_target(annotation_path)

        albumentations.pytorch.transforms.ToTensorV2()

        # albumentation
        if self.transform:
            transformed = self.transform(image=image, bboxes=target['boxes'], labels=target['labels'])
            image = transformed['image']
            albumentations.pytorch.transforms.ToTensorV2()
            target = {'boxes': transformed['bboxes'], 'labels': transformed['labels']}

        target['boxes'] = torch.as_tensor(target['boxes'], dtype=torch.float32)
        target['labels'] = torch.as_tensor(target['labels'], dtype=torch.int64)

        # normalize
        image = image.float()
        image = image / 255
        return image, target


def collate_fn(batch):
    return tuple(zip(*batch))