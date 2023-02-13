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

len_downloaded = 10000
downloaded = []

while True:
    now = datetime.now()
    print ("________________________%s/%s/%s %s:%s:%s______________________" % (now.month,now.day,now.year,now.hour,now.minute,now.second),end="", flush=True)
    print("\r", end="", flush=True)
    time.sleep(1)
    products = OrderedDict()
    try:
        pp = api.query(footprint,
                    date=('NOW-3DAYS', 'NOW'),
                    # date=('20221023','20221024'),
                    platformname='Sentinel-2',    
                    cloudcoverpercentage=(0, 100),
                    )
    except: 
        continue
    products.update(pp)
    for key in products.keys():  
        if products[key]['title'].split('_')[1] != 'MSIL2A': continue
        if products[key]['title'] in downloaded: continue
        else:
            downloaded.append(products[key]['title'])
            if len(downloaded) > len_downloaded: del downloaded[0]
        start = time.time()
        file_path = os.path.join(folder,f"{products[key]['title']}.zip")
        try:
            api.download(key,directory_path=folder,)
        except:
            continue
        with zipfile.ZipFile(file_path, 'r') as zip_ref:
            zip_ref.extractall("/".join(file_path.split('/')[:-1]))
        os.remove(file_path)
        
        folder_path = f"{file_path[:-3]}SAFE"
        band_paths = []
        for (root,dirs,files) in os.walk(folder_path, topdown=True):
            for file in files:
                file_path = os.path.join(root,file)
                if np.any([y in file_path for y in ['B02','B03','B04']]) \
                    and np.all([y not in file_path for y in ['QI_DATA','R20','R60']]):
                    band_paths.append(os.path.join(root,file))
        
        if len(band_paths) != 3: continue
        print("Downloaded to: ",folder_path)

        band_paths = sorted(band_paths)
        bands = [rasterio.open(path) for path in band_paths] #2,3,4
        b,g,r = [band.read(1) for band in bands]
        
        b=brighten(b)
        g=brighten(g)
        r=brighten(r)

        try:
            b = normalize(b)
            g = normalize(g)
            r = normalize(r)
        except:
            pass
        
        bgr = np.dstack((b, g, r))
        bgr = bgr/np.max(bgr)*255
        bgr = bgr.astype(np.uint8)
        h,w = bgr.shape[:2]
        #______________________________________________Ship Detection_____________________________________________________
        # predict(
        #     model_type='yolov5',
        #     model_path='/mnt/data/ngocpt/thermal_detection/tph-yolov5/runs/train/exp2/weights/best.pt',
        #     model_device='cuda:0',
        #     model_confidence_threshold=0.5,
        #     source=output_path, #path to the image
        #     slice_height=512,
        #     slice_width=512,
        #     overlap_height_ratio=0.2,
        #     overlap_width_ratio=0.2,
        #     project=os.path.dirname(output_path),
        #     export_pickle=False,
        #     visual_bbox_thickness= 1,
        #     visual_text_thickness=1,
        # )
        
        #_______________________________________________GeoTIFF__________________________________________________
        output_folder_path = os.path.join(folder+'_infor',f"{products[key]['title']}.SAFE")
        os.makedirs(os.path.join(output_folder_path),exist_ok=True)  
        # os.makedirs(os.path.join(output_folder_path,'R10m'),exist_ok=True)  
        # os.chmod(output_folder_path, file_permission)

        shutil.copy2(os.path.join(folder_path,'INSPIRE.xml'),os.path.join(output_folder_path,'INSPIRE.xml'))
        # for img_path in glob.glob(os.path.join(folder_path,'GRANULE','*','IMG_DATA','R10m','*.jp2')):
        #     shutil.copy2(img_path,os.path.join(folder+'_infor',f"{products[key]['title']}.SAFE",'R10m'))
            
        output_filename = 'infor'
        cv2.imwrite(os.path.join(output_folder_path,f'{output_filename}.jpg'),cv2.resize(bgr,(2000,2000)))
        cv2.imwrite(os.path.join(output_folder_path,"bgr.jpg") ,bgr)
        
        tif_path = os.path.join(output_folder_path,f'{output_filename}.tiff')
        with open(tif_path,'w',opener=opener) as _:
            temp_tif_path = os.path.join('/tmp',f'{output_filename}.tiff')
            dst_ds = gdal.GetDriverByName('GTiff').Create(temp_tif_path, h, w, 3, gdal.GDT_Byte)
            dst_ds.SetProjection(gdal.Open(band_paths[0]).GetProjection())   #specify coordinate system 
            dst_ds.SetGeoTransform(gdal.Info(band_paths[0],format='json')['geoTransform'])    # specify coords
            dst_ds.GetRasterBand(1).WriteArray(bgr[...,2])   # write r-band to the raster
            dst_ds.GetRasterBand(2).WriteArray(bgr[...,1])   # write g-band to the raster
            dst_ds.GetRasterBand(3).WriteArray(bgr[...,0])   # write b-band to the raster
            dst_ds.FlushCache()                              # write to disk
            dst_ds = None
            warp = gdal.Warp(temp_tif_path,temp_tif_path, options = gdal.WarpOptions(xRes=40, yRes=-40, dstSRS=f'EPSG:3395'))
            warp = gdal.Warp(temp_tif_path,temp_tif_path, options = gdal.WarpOptions(dstSRS=f'EPSG:4326'))
            shutil.copy2(temp_tif_path,tif_path)
        
        with open(os.path.join(output_folder_path,f'{output_filename}.prj'),'w',opener=opener) as f:
            info = gdal.Info(tif_path, format='json')
            f.write(info['coordinateSystem']['wkt'])
        
        with open(os.path.join(output_folder_path,f'{output_filename}.meta'),'w',opener=opener) as f:
            lonmin,latmin = info['cornerCoordinates']['lowerLeft']
            lonmax,latmax = info['cornerCoordinates']['upperRight']
            f.write(f"Layer Name = Map Capture\nOrigin Longitude = {lonmin}\nOrigin Latitude = {latmin}\nCorner Longitude = {lonmax}\nCorner Latitude = {latmax}")

        # for (root,dirs,files) in os.walk(output_folder_path, topdown=True):
        #     for file in files:
        #         if os.path.isfile(os.path.join(root,file)):
        #             os.chmod(os.path.join(root,file), file_permission)
        
        os.rename(output_folder_path,output_folder_path[:-5]+'_FN')
        print("Saved to: ",output_folder_path[:-5]+'_FN')
        shutil.rmtree(folder_path,ignore_errors=True)
        shutil.rmtree(folder_path,ignore_errors=True)
        print("TOTAL TIME: ",time.time()-start)
        