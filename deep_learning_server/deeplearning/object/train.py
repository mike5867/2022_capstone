import os
import torch
import time
from torch.utils.data import Dataset
from tqdm import tqdm

from deeplearning.object.modules import setting
from deeplearning.object.modules import dataloader
from deeplearning.object.modules import faster_rcnn

os.environ['CUDA_LAUNCH_BLOCKING'] = "1"

dataset = dataloader.AlbumentationDataset(path=setting.train_path)
data_loader = torch.utils.data.DataLoader(dataset, batch_size=setting.batch_size, collate_fn=dataloader.collate_fn)

model = faster_rcnn.Faster_RCNN(setting.num_classes)
device = torch.device('cuda') if torch.cuda.is_available() else torch.device('cpu')
model.to(device)

num_epochs= setting.epoch
learning_rate= setting.learning_rate


def train():
    #torch.cuda.empty_cache()

    params = [p for p in model.parameters() if p.requires_grad]
    optimizer = torch.optim.SGD(params, lr=learning_rate, momentum=0.9, weight_decay=0.0005)
    for epoch in range(num_epochs):
        i = 0
        start = time.time()
        model.train()

        epoch_loss = 0
        tbar=tqdm(data_loader)
        for images, targets in tbar:
            images = list(image.to(device) for image in images)
            targets = [{k: v.to(device) for k, v in t.items()} for t in targets]

            loss_dict = model(images, targets)

            losses = sum(loss for loss in loss_dict.values())

            optimizer.zero_grad()
            losses.backward()
            optimizer.step()

            epoch_loss += losses
            tbar.set_description('Train loss: %.5f' %(epoch_loss/(i+1)))

            i+=1

        print(epoch, epoch_loss, f'time: {time.time() - start}')

    torch.save(model.state_dict(), f'Faster R-CNN_{num_epochs}_{learning_rate}.pt')


