import cv2
import torch
from PIL import Image
from torchvision import transforms

import matplotlib.pyplot as plt

from deeplearning.object.modules import faster_rcnn
from deeplearning.object import setting
from deeplearning.object import predict as od

from deeplearning.semantic import predict as ss

SHOW_IMAGE = True

def object_detection(img_path):
    model = faster_rcnn.Faster_RCNN(setting.num_classes)
    model.load_state_dict(torch.load(setting.saved_weight))
    device = torch.device('cpu')
    model.to(device)

    image = cv2.imread(img_path)
    image = cv2.cvtColor(image, cv2.COLOR_BGR2RGB)

    transform = transforms.Compose([transforms.ToTensor()])

    x = transform(Image.fromarray(image))
    x = x.unsqueeze(0)

    with torch.no_grad():
        x.to(device)
        preds =od.make_prediction(model, x, 0.5)
        print("prediction: ", preds)

    print("Object Detection Done.")

    if SHOW_IMAGE:
        x = x.squeeze(0)
        od.plot_image(x, preds[0])

    return preds


def semantic_segmentation(img_path):
    model_wrapper=ss.ModelWrapper()
    image=cv2.imread(img_path,cv2.IMREAD_COLOR)
    width, height, _ = image.shape
    image=cv2.cvtColor(image,cv2.COLOR_BGR2RGB)
    segmap=model_wrapper.predict(image)

    segmap=cv2.resize(segmap,(height,width),interpolation=cv2.INTER_NEAREST)

    print(segmap)
    print(segmap.shape)

    print('Semantic Segmentation Done.')

    if SHOW_IMAGE:
        plt.imshow(segmap)
        plt.show()

    return segmap

