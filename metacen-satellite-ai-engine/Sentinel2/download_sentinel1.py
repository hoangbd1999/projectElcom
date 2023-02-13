import os
from collections import OrderedDict
from sentinelsat import SentinelAPI, read_geojson, geojson_to_wkt, make_path_filter


# https://geojson.io/#map=7/24.472/119.487
folder = '/mnt/data/thanhdd/remote_sensing/Sentinel2/DaNang'
os.makedirs(folder,exist_ok=True)
api = SentinelAPI('thanhdd', 'doduythanh021199')
footprint = geojson_to_wkt(read_geojson('bienDong_expanded.geojson'))
print(footprint)
products = api.query(footprint,
                    date=('NOW-5DAYS', 'NOW'),
                    # date=('20221023','20221024'),
                    platformname='Sentinel-1',    
                    )
for key in list(products.keys())[:5]:  
    api.download(key,directory_path=folder,)
