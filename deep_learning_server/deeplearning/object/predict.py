import torch
from tqdm import tqdm

from deeplearning.object.modules import utils_ObjectDetection as utils
from deeplearning.object.modules import faster_rcnn
from deeplearning.object.modules import dataloader
from deeplearning.object.modules import setting


model= faster_rcnn.Faster_RCNN(setting.num_classes)
model.load_state_dict(torch.load(setting.saved_weight))
device = torch.device('cuda') if torch.cuda.is_available() else torch.device('cpu')
model.to(device)

labels = []
preds_adj_all = []
annot_all = []
sample_metrics = []

test_dataset = dataloader.AlbumentationDataset(path=setting.test_path)
test_data_loader = torch.utils.data.DataLoader(test_dataset, batch_size=setting.test_batch_size, collate_fn=dataloader.collate_fn)

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

def calc_performance():
    sample_metrics = []
    for batch_i in range(len(preds_adj_all)):
        sample_metrics += utils.get_batch_statistics(preds_adj_all[batch_i], annot_all[batch_i], iou_threshold=0.5)

    true_positives, pred_scores, pred_labels = [torch.cat(x, 0) for x in list(zip(*sample_metrics))]
    precision, recall, AP, f1, ap_class = utils.ap_per_class(true_positives, pred_scores, pred_labels, torch.tensor(labels))
    mAP = torch.mean(AP)
    print(f'mAP : {mAP}')
    print(f'AP : {AP}')



def predict_main():
    global labels

    tbar=tqdm(test_data_loader, position=0, leave=True)
    for im, annot in tbar:
        im = list(img.to(device) for img in im)

        for t in annot:
            labels += t['labels']

        with torch.no_grad():
            preds_adj = make_prediction(model, im, 0.5)
            preds_adj = [{k: v.to(torch.device('cpu')) for k, v in t.items()} for t in preds_adj]
            preds_adj_all.append(preds_adj)
            annot_all.append(annot)

    calc_performance()


