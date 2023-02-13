import os
import zipfile
import glob
for file_path in list(glob.glob('Taiwan/*')):
    print(file_path)
    if file_path[-3:] != 'zip': continue
    print(f"{file_path[:-3]}SAFE")
    if os.path.isdir(f"{file_path[:-3]}SAFE"): continue
    with zipfile.ZipFile(file_path, 'r') as zip_ref:
        zip_ref.extractall("/".join(file_path.split('/')[:-1]))