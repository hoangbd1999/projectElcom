# import os
# import cv2
# import glob
# folder = 'bienDong'

# for subfolder in os.listdir(folder):
#     img_path = os.path.join(folder,subfolder,'merged.jpg')
#     if not os.path.exists(img_path): continue
#     # print(f"{subfolder[:-5]}.jpg")
#     os.rename(img_path, os.path.join(folder,subfolder,f"{subfolder[:-5]}.jpg"))
#     os.rename(os.path.join(folder,subfolder,'merged_result.jpg'),os.path.join(folder,subfolder,f"{subfolder[:-5]}_result.jpg"))



# import glob

# file_paths = glob.glob('bienDong/*/S*[!result].jpg')
# print(file_paths)

# import cv2
# import numpy as np
# img = cv2.imread('temp.jpg')
# ret, thresh = cv2.threshold(cv2.cvtColor(img,cv2.COLOR_BGR2GRAY),0,255,cv2.THRESH_BINARY)
# thresh = cv2.morphologyEx(thresh,cv2.MORPH_OPEN,np.ones((11,11),dtype=np.uint8))
# contours, hierarchy = cv2.findContours(thresh, cv2.RETR_TREE, cv2.CHAIN_APPROX_SIMPLE)
# for contour in contours:
#     if cv2.contourArea(contour) < 100: continue
#     hull = cv2.convexHull(hull)
#     bbox = cv2.boundingRect(contour)
#     print(bbox)

import cv2
import numpy as np
import sys
import os
import shutil
import pandas as pd
sys.path.append('./sahi')
from sahi.predict import predict
from math import degrees
from osgeo import gdal

output_folder = 'temp'
if os.path.exists(output_folder):
    shutil.rmtree(output_folder)
os.mkdir(output_folder)
os.mkdir(os.path.join(output_folder,'objects'))
img_path = '/mnt/data/thanhdd/remote_sensing/Sentinel2/Anh duoi 1m/20211202_022208_ssc14_u0001'
img = cv2.imread(img_path+'.png')
x,y,w,h = 300,6900,10000,10000
latmax = 22.682905698918674
lonmin = 120.26241529966856
res = 0.5
def find_latlon(x,y):
    r = 6371 * 10**3 #m
    lat = latmax - degrees(y*res/r)
    lon = lonmin + degrees(x*res/r)
    return lat,lon

img = img[y:y+h,x:x+w]
draw = img.copy()
cv2.imwrite(os.path.join(output_folder,'bgr.jpg'),img)
# predict(
#     model_type='yolov5',
#     model_path='sahi/best.pt',
#     model_device='cuda:0',
#     model_confidence_threshold=0.5,
#     source=os.path.join(output_folder,'bgr.jpg'), #path to the image
#     slice_height=1024,
#     slice_width=1024,
#     overlap_height_ratio=0.2,
#     overlap_width_ratio=0.2,
#     project=output_folder,
#     novisual=True,
#     export_pickle=True,
#     export_crop=False,
#     return_dict=True
#     )
# pickle = pd.read_pickle(os.path.join(output_folder,'pickles',output_folder,'bgr_result.pickle'))
# for i,obj in enumerate(pickle):
#     x1,y1,x2,y2 = obj.bbox.to_voc_bbox()
#     cv2.rectangle(draw,(x1,y1),(x2,y2),(0,0,255),5)
#     obj_img = img[y1:y2,x1:x2]
#     cv2.imwrite(os.path.join(output_folder,'objects',f"object_{i}.jpg"),obj_img)
#     lat,lon = find_latlon(x+(x1+x2)/2,y+(y1+y2)/2)
#     with open(os.path.join(output_folder,'objects',f"object_{i}.txt"),'w') as f:
#             f.write(f"h: {(y2-y1)*res}\nw: {(x2-x1)*res}\nlat: {lat}\nlon: {lon}")
            
# cv2.imwrite(os.path.join(output_folder,'result.jpg'),draw)


tiff_path = os.path.join(output_folder,'infor.tiff')
with open(tiff_path,'w') as _:
    dst_ds = gdal.GetDriverByName('GTiff').Create(tiff_path, h, w, 3, gdal.GDT_Byte)
    coor = gdal.Info(img_path+'.tiff',format='json')['geoTransform']
    lat,lon = find_latlon(x,y)
    coor[0] = lon
    coor[3] = lat
    dst_ds.SetProjection(gdal.Open(img_path+'.tiff').GetProjection())   #specify coordinate system 
    dst_ds.SetGeoTransform(coor)    # specify coords
    dst_ds.GetRasterBand(1).WriteArray(img[...,2])   # write r-band to the raster
    dst_ds.GetRasterBand(2).WriteArray(img[...,1])   # write g-band to the raster
    dst_ds.GetRasterBand(3).WriteArray(img[...,0])   # write b-band to the raster
    dst_ds.FlushCache()                              # write to disk
    dst_ds = None
    warp = gdal.Warp(tiff_path,tiff_path, options = gdal.WarpOptions(xRes=0.00002,
                                                                     yRes=-0.00002,
                                                                     dstSRS=f'EPSG:4326'))
