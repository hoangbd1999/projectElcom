import matplotlib.pyplot as plt
import numpy as np
import rasterio
import os
import glob
import cv2
os.environ['PARALLEL_READS']='False'
def brighten(band):
    alpha=0.06
    beta=0
    return np.clip(alpha*band+beta, 0,255)

def normalize(band):
    value = list(set(band.flatten()))
    if 0.0 in value:
        value.remove(0.0)
    band_min, band_max = (min(value), max(value))
    return ((band-band_min)/((band_max - band_min)))


# for folder_path in glob.glob('Taiwan/*'):
#     if not os.path.isdir(folder_path): continue
#     band_paths = glob.glob(os.path.join(folder_path,'*','*','IMG_DATA','*'))
#     band_paths = [x for x in band_paths 
#                   if np.any([y in x for y in ['B02','B03','B04']])]
#     if len(band_paths) != 3: continue
#     print(folder_path)
#     band_paths = sorted(band_paths)
#     bands = [rasterio.open(path) for path in band_paths] #2,3,4
#     b,g,r = [band.read(1) for band in bands]
    
#     r=brighten(r)
#     b=brighten(b)
#     g=brighten(g)

#     red_n = normalize(r)
#     green_n = normalize(g)
#     blue_n = normalize(b)

#     bgr = np.dstack((blue_n, green_n, red_n))
#     bgr = bgr/np.max(bgr)*255
#     bgr = bgr.astype(np.uint8)
#     output_path = os.path.join(folder_path,'merged.jpg')
#     cv2.imwrite(output_path,bgr)
#     print(output_path,bgr.shape)

for folder_path in glob.glob('Taiwan/*'):
    if not os.path.isdir(folder_path): continue
    band_paths = []
    for (root,dirs,files) in os.walk(folder_path, topdown=True):
        for file in files:
            file_path = os.path.join(root,file)
            if np.any([y in file_path for y in ['B02','B03','B04']]) \
                and np.all([y not in file_path for y in ['QI_DATA','R20','R60']]):
                print(root,file)
            
            

# folder_path = '/mnt/data/thanhdd/remote_sensing/Sentinel2/L2A_T50SQE_A037166_20220804T025314_2022-08-04_con'
# band_paths = glob.glob(f'{folder_path}/*.tif')

# band_paths = [x for x in band_paths 
#                 if np.any([y in x for y in ['B02','B03','B04']])]
# band_paths = sorted(band_paths)
# print(band_paths)

# bands = [rasterio.open(path) for path in band_paths] #2,3,4
# b,g,r = [band.read(1) for band in bands]

# r=brighten(r)
# b=brighten(b)
# g=brighten(g)

# red_n = normalize(r)
# green_n = normalize(g)
# blue_n = normalize(b)

# bgr = np.dstack((blue_n, green_n, red_n))
# bgr = bgr/np.max(bgr)*255
# bgr = bgr.astype(np.uint8)
# output_path = os.path.join(folder_path,'merged.jpg')
# print(output_path,bgr.shape)
# cv2.imwrite(output_path,bgr)