import os
import shutil
from tqdm import tqdm
folder = 'bienDong'

for subfolder in tqdm(os.listdir(folder)):
    # print(os.path.join(folder,subfolder))
    for (root,dirs,files) in os.walk(os.path.join(folder,subfolder), topdown=True):
        if root == os.path.join(folder,subfolder): continue
        # if len(os.listdir(os.path.join(root))) == 0:
        #     shutil.rmtree(os.path.join(root))
        for file in files:
            file_path = os.path.join(root,file)
            if not ('B02' in file_path and '10m' in file_path) and 'crop' not in file_path:
                os.remove(file_path)