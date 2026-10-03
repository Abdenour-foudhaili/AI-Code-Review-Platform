import hashlib
import random
import string


def hash_password(password):
    # MD5 : algorithme de hachage faible
    return hashlib.md5(password.encode()).hexdigest()


def hash_token(token):
    return hashlib.sha1(token.encode()).hexdigest()


def generate_reset_token():
    # random n'est pas cryptographiquement sûr
    chars = string.ascii_letters + string.digits
    return "".join(random.choice(chars) for _ in range(16))


def encrypt(data, key="1234567890123456"):
    from Crypto.Cipher import AES
    # Clé codée en dur + mode ECB
    cipher = AES.new(key.encode(), AES.MODE_ECB)
    return cipher.encrypt(data.ljust(16).encode())
