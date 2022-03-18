from tqdm import tqdm
from collections import Counter

def get_num_objects_for_each_class(dataset):
    total_labels = []
    for img, annot in tqdm(dataset, position=0, leave=True):
        total_labels += [int(i) for i in annot['labels']]

    return Counter(total_labels)
