import os
os.umask(0)
os.environ["CUDA_DEVICE_ORDER"]="PCI_BUS_ID"
os.environ["CUDA_VISIBLE_DEVICES"]="0"
import sys
sys.path.append('/mnt/data/thanhdd/Change_Detection/ChangeFormer')
from run_3m import predict_single
from kafka import KafkaProducer,KafkaConsumer
import json
import time
import cv2
import numpy as np

from bs4 import BeautifulSoup
from math import degrees


def predict(img1_path,img2_path,output_folder):
    os.makedirs(output_folder,exist_ok=True)
    with open(os.path.join(os.path.dirname(img1_path),'INSPIRE.xml'),'r') as f:
        data = f.read()
    data = BeautifulSoup(data,'xml')
    latlon = data.find_all('gmd:EX_GeographicBoundingBox')
    lonmax = float(data.find('gmd:eastBoundLongitude').find('gco:Decimal').get_text())
    lonmin = float(data.find('gmd:westBoundLongitude').find('gco:Decimal').get_text())
    latmax = float(data.find('gmd:northBoundLatitude').find('gco:Decimal').get_text())
    latmin = float(data.find('gmd:southBoundLatitude').find('gco:Decimal').get_text())
    res = 10
    def find_latlon(x,y):
        r = 6371 * 10**3 #m
        lat = latmax - degrees(y*res/r)
        lon = lonmin + degrees(x*res/r)
        return lat,lon
    
    img1 = cv2.imread(img1_path)
    img2 = cv2.imread(img2_path)
    result = predict_single(img1,img2,img_size=256,ratio=0.5)
    print('Change Detection result ',result.shape)
    cv2.imwrite('temp.jpg',result)
    ret, thresh = cv2.threshold(cv2.cvtColor(result,cv2.COLOR_BGR2GRAY),0,255,cv2.THRESH_BINARY)
    thresh = cv2.morphologyEx(thresh,cv2.MORPH_OPEN,np.ones((11,11),dtype=np.uint8))
    img1 = cv2.bitwise_and(img1,img1,mask=thresh)
    img2 = cv2.bitwise_and(img2,img2,mask=thresh)
    contours, hierarchy = cv2.findContours(thresh, cv2.RETR_TREE, cv2.CHAIN_APPROX_SIMPLE)
    for idx,contour in enumerate(contours):
        if cv2.contourArea(contour) < 100: continue
        hull = cv2.convexHull(contour)
        x,y,w,h = cv2.boundingRect(hull)
        print(os.path.join(output_folder,f"{idx}_compare.jpg"))
        cv2.imwrite(os.path.join(output_folder,f"{idx}_origin.jpg"),img1[y:y+h,x:x+w])
        cv2.imwrite(os.path.join(output_folder,f"{idx}_compare.jpg"),img2[y:y+h,x:x+w])
        latori,lonori = find_latlon(x,y+h)
        latcor,loncor = find_latlon(x+w,y)
        with open(os.path.join(output_folder,f"{idx}_infor.txt"),'w') as f:
            f.write(f"Origin Longitude = {lonori}\n")
            f.write(f"Origin Latitude = {latori}\n")
            f.write(f"Corner Longitude = {loncor}\n")
            f.write(f"Corner Latitude = {latcor}\n")
            f.write(f"Width = {w*res}\n")
            f.write(f"Height = {h*res}")
            
test_msg = {
	"uuidKey": "c6678237-b195-476a-83b2-02685674888t",
	"tileNumber": "T51QTV",
	"rootDataFolderPathOrigin": "/ttttbien2/metacen/satellite-images/20221117/05/S2A_MSIL2A_20220129T025951_N0400_R032_T49QFU_20220129T052010",
	"rootDataFolderPathCompare": "/ttttbien2/metacen/satellite-images/20221117/00/S2B_MSIL2A_20220603T025549_N0400_R032_T49QFU_20220603T064057",
	"resultFolder": "/ttttbien2/metacen/satellite-images/20221117/00/test",
	"retryNum": 0
}


servers= "192.168.51.18:29092,192.168.51.18:29093,192.168.51.18:29094"
consumer = KafkaConsumer(
        "SATELLITE_IMAGE_CHANGES_TO_PROCESS",
        bootstrap_servers=servers,
        auto_offset_reset='latest')

producer = KafkaProducer(
        bootstrap_servers=servers,)

# folder = '/ttttbien2/metacen/satellite-images/bienDong_infor'
print('Change Detection Ready')
while True:
    msg_pack = consumer.poll(timeout_ms=500)
    for tp, messages in msg_pack.items():
        for message in messages:
            start = time.time()
            value = json.loads(message.value.decode('utf-8'))
            img1_path = os.path.join(value['rootDataFolderPathOrigin'], 'bgr.jpg')
            img2_path = os.path.join(value['rootDataFolderPathCompare'], 'bgr.jpg')
            output_folder = value['resultFolder']
            print(img1_path)
            print(img2_path)
            processStatus = True
            try:
                predict(img1_path,img2_path,output_folder)
            except Exception as e:
                print(f"Process False with {e}")
                processStatus = False
            
            print(processStatus, output_folder)
            value['processStatus']= processStatus
            value['totalProcessedTime'] = int((time.time()-start)*10**3)
            producer.send(topic='SATELLITE_IMAGE_CHANGES_PROCESSED',
                            value = json.dumps(value,indent=4).encode('utf-8'),
                            key=message.key)
            
