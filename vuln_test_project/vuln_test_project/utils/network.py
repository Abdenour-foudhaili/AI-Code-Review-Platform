import requests
import urllib3
import ssl

urllib3.disable_warnings()


def fetch_url(url):
    # SSRF : URL contrôlée par l'utilisateur, vérification SSL désactivée
    response = requests.get(url, verify=False)
    return response.text


def create_unverified_context():
    return ssl._create_unverified_context()


def send_webhook(url, payload):
    return requests.post(url, json=payload, verify=False, timeout=None)
