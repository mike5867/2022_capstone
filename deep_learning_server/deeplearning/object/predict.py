import torch
import torch.utils.data
from tqdm import tqdm

from deeplearning.object.modules import utils_ObjectDetection as utils
from deeplearning.object.modules import faster_rcnn
from deeplearning.object.modules import dataloader
from deeplearning.object import setting

import random
import matplotlib.pyplot as plt
import matplotlib.patches as patches


def plot_image(img, annotation):
    img = img.cpu().permute(1, 2, 0)

    fig, ax = plt.subplots(1)

    ax.imshow(img)
    for idx in range(len(annotation["boxes"])):
        xtl, ytl, xbr, ybr = annotation["boxes"][idx]

        if annotation["labels"][idx] == 1:  # bicycle
            rect = patches.Rectangle((xtl, ytl), (xbr - xtl), (ybr - ytl), linewidth=3, edgecolor='g', facecolor='none')
        elif annotation["labels"][idx] == 2:  # scooter
            rect = patches.Rectangle((xtl, ytl), (xbr - xtl), (ybr - ytl), linewidth=3, edgecolor='b', facecolor='none')
        # else:
        #    rect=patches.Rectangle((xtl,ytl),(xbr-xtl),(ybr-ytl),linewidth=1,edgecolor='r',facecolor='none')
        ax.add_patch(rect)

    plt.show()


def calc_performance():
    sample_metrics = []
    for batch_i in range(len(preds_adj_all)):
        sample_metrics += utils.get_batch_statistics(preds_adj_all[batch_i], annot_all[batch_i], iou_threshold=0.5)

    true_positives, pred_scores, pred_labels = [torch.cat(x, 0) for x in list(zip(*sample_metrics))]
    precision, recall, AP, f1, ap_class = utils.ap_per_class(true_positives, pred_scores, pred_labels, torch.tensor(labels))
    mAP = torch.mean(AP)
    print(f'mAP : {mAP}')
    print(f'AP : {AP}')


def make_prediction(model, img, threshold):
    model.eval()
    preds = model(img)
    for id in range(len(preds)):
        idx_list = []

        for idx, score in enumerate(preds[id]['scores']):
            if score > threshold:
                idx_list.append(idx)

        preds[id]['boxes'] = preds[id]['boxes'][idx_list]
        preds[id]['labels'] = preds[id]['labels'][idx_list]
        preds[id]['scores'] = preds[id]['scores'][idx_list]

    return preds




def predict_main():
    global labels

    tbar=tqdm(test_data_loader, position=0, leave=True)
    for im,annot in tbar:
        im = list(img.to(device) for img in im)

        for t in annot:
            labels += t['labels']

        with torch.no_grad():
            preds_adj = make_prediction(model, im, 0.5)
            preds_adj = [{k: v.to(torch.device('cpu')) for k, v in t.items()} for t in preds_adj]
            preds_adj_all.append(preds_adj)
            annot_all.append(annot)

    calc_performance()

if __name__ =="__main__":
    model = faster_rcnn.Faster_RCNN(setting.num_classes)
    model.load_state_dict(torch.load(setting.saved_weight))
    device = torch.device('cuda') if torch.cuda.is_available() else torch.device('cpu')
    model.to(device)

    labels = []
    preds_adj_all = []
    annot_all = []
    sample_metrics = []

    test_dataset = dataloader.AlbumentationDataset(path=setting.test_path, transform=setting.albumentations_transform)
    test_data_loader = torch.utils.data.DataLoader(test_dataset, batch_size=setting.test_batch_size,
                                                   collate_fn=dataloader.collate_fn)
    predict_main()


    total = range(len(test_data_loader))
    select = random.sample(total, 4)

    for i in select:
        test_annot = preds_adj_all[i][0]
        img, annot = test_dataset[i * 2]  # batch size가 2이기 때문에 2를 곱해야함

        plot_image(img, annot)
        plot_image(img, test_annot)







