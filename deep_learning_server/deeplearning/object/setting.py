import albumentations.pytorch

train_path='G:/dataset/train/'
test_path='G:/dataset/test/'

batch_size=2
test_batch_size=2

epoch=10
learning_rate=0.001
num_classes=3  #include background class

saved_weight='C:/users/mike5/desktop/capstone/deep_learning_server/deeplearning/object/Faster R-CNN_30_0.001.pt'

albumentations_transform=albumentations.Compose([
    #albumentations.RandomSizedBBoxSafeCrop(width=960,height=540,erosion_rate=0.1),
    #bbox_params=albumentations.BboxParams(format='pascal_voc',label_fields=['labels']),
    albumentations.pytorch.transforms.ToTensorV2()])

