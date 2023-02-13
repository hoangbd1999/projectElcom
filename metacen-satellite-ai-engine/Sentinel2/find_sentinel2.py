import os
import sys
import stat
os.umask(0)
os.environ["CUDA_DEVICE_ORDER"]="PCI_BUS_ID"
os.environ["CUDA_VISIBLE_DEVICES"]="1"
os.environ['PROJ_LIB'] = 'thanhdd/share/proj'
os.environ['GDAL_DATA'] = 'thanhdd/share'
# import sys
# sys.path.append('/mnt/data/ngocpt/thermal_detection/sahi')
# from sahi.predict import predict
from collections import OrderedDict
from sentinelsat import SentinelAPI, read_geojson, geojson_to_wkt, make_path_filter
import numpy as np
import glob
import zipfile
import rasterio
import cv2
from datetime import datetime
import time
from osgeo import gdal
import shutil

def brighten(band):
    alpha=0.06
    beta=0
    return np.clip(alpha*band+beta, 0,255)

def normalize(band):
    value = list(set(band.flatten()))
    value = [x for x in value if x != 0.0]
    band_min, band_max = min(value), max(value)
    if band_min == band_max: band_min = band_max - 1
    return ((band-band_min)/((band_max - band_min)))

file_permission = 0o777
def opener(path, flags):
    return os.open(path, flags, file_permission)

folder = '/ttttbien2/metacen/satellite-images/bienDong'
os.makedirs(folder,exist_ok=True)
# os.chmod(folder, file_permission)
api = SentinelAPI('thanhdd', 'doduythanh021199')
footprint = geojson_to_wkt(read_geojson('geojson/bienDong_expanded.geojson')) # https://geojson.io/
print(footprint)


# sudo ./thanhdd/bin/python3.8 download_sentinel2.py

products = OrderedDict()
pp = api.query(footprint,
            date=('NOW-10DAYS', 'NOW'),
            # date=('20221023','20221024'),
            platformname='Sentinel-2',    
            cloudcoverpercentage=(0, 100),
            )

products.update(pp)
for key,value in products.items():  
    if '49PHN' in value['title']: print(value['title'])

        