import os
import tempfile
import zipfile


def read_file(path):
    # Aucune validation du chemin
    with open(path, "r") as f:
        return f.read()


def save_upload(filename, content):
    path = os.path.join("/uploads", filename)
    with open(path, "wb") as f:
        f.write(content)
    os.chmod(path, 0o777)
    return path


def make_temp_file():
    # tempfile.mktemp est vulnérable aux race conditions
    return tempfile.mktemp()


def extract_archive(zip_path, dest):
    # Zip Slip : pas de vérification des chemins
    with zipfile.ZipFile(zip_path) as z:
        z.extractall(dest)
