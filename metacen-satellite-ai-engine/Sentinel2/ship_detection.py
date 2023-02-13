import os
os.umask(0)
os.environ["CUDA_DEVICE_ORDER"]="PCI_BUS_ID"
os.environ["CUDA_VISIBLE_DEVICES"]="1"
import sys
sys.path.append('./sahi')
from sahi.predict import predict
from kafka import KafkaProducer,KafkaConsumer
import json
import time

servers= "192.168.51.18:29092,192.168.51.18:29093,192.168.51.18:29094"
consumer = KafkaConsumer(
        "SATELLITE_IMAGE_TO_PROCESS",
        bootstrap_servers=servers,
        auto_offset_reset='latest')

producer = KafkaProducer(
        bootstrap_servers=servers,)

# folder = '/ttttbien2/metacen/satellite-images/bienDong_infor'
print('Ship Detection ready')
while True:
    msg_pack = consumer.poll(timeout_ms=500)
    for tp, messages in msg_pack.items():
        for message in messages:
            start = time.time()
            value = json.loads(message.value.decode('utf-8'))
            subfolder_path = value['rootDataFolderPath']
            img_path = os.path.join(subfolder_path,"bgr.jpg")
            print(img_path)
            processStatus = True
            try:
                predict(
                    model_type='yolov5',
                    model_path='sahi/best.pt',
                    model_device='cuda:0',
                    model_confidence_threshold=0.5,
                    source=img_path, #path to the image
                    slice_height=512,
                    slice_width=512,
                    overlap_height_ratio=0.2,
                    overlap_width_ratio=0.2,
                    project=subfolder_path,
                    novisual=True,
                    export_pickle=False,
                    export_crop=True,
                    visual_bbox_thickness= 1,
                    visual_text_thickness=1,
                    visual_export_format='jpg',
                )
            except Exception as e:
                print(f"Process False with {e}")
                processStatus = False
            
            print(processStatus)
            value['processStatus']= processStatus
            value['totalProcessedTime'] = int((time.time()-start)*10**3)
            producer.send(topic='SATELLITE_IMAGE_PROCESSED',
                          value = json.dumps(value,indent=4).encode('utf-8'),
                          key=message.key)