import prediction
import result_code

def judge(img_path):
    pred = prediction.object_detection(img_path)
    pred = pred[0]
    segmap = prediction.semantic_segmentation(img_path)

    if len(pred["boxes"]) == 0:
        return result_code.Result.DETECTED_FAIL
    elif len(pred["boxes"]) > 1:  # 예측한 박스가 두 개 이상인 경우
        biggest_size = 0
        biggest_idx = 0
        for idx, box in enumerate(pred["boxes"]):
            box_size = (box[2] - box[0]) * (box[3] - box[1])
            if biggest_size < box_size:
                biggest_size = box_size
                biggest_idx = idx

        box = pred["boxes"][biggest_idx]
        labels = pred["labels"][biggest_idx]
    else:
        box = pred["boxes"][0]
        labels = pred["labels"][0]

    print("target box: ", box)
    print("target label: ", labels)

    xtl = int(box[0])
    ytl = int(box[1])
    xbr = int(box[2])
    ybr = int(box[3])

    height = ybr - ytl
    target_height = int(height * 0.3)  # 높이의 30프로만 확인

    # segmap slicing
    target_segmap = segmap[ybr - target_height:ybr + 1, xtl:xbr + 1, :]
    print("target segmap: \n", target_segmap)

    # judge logic
    for h in range(len(target_segmap)):
        for w in range(len(target_segmap[h])):
            if target_segmap[h][w][0]==255 and target_segmap[h][w][1]==0 and target_segmap[h][w][2]==255: #횡단 보도
                return result_code.Result.FAIL
            if target_segmap[h][w][0]==255 and target_segmap[h][w][1]==255 and target_segmap[h][w][2]==0: #점자 블록
                return result_code.Result.FAIL

    return result_code.Result.PASS
