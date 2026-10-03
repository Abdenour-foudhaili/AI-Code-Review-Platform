import os
import subprocess
import pickle
import yaml
from flask import Flask, request, render_template_string, send_file

app = Flask(__name__)


@app.route("/ping")
def ping():
    host = request.args.get("host")
    # Command injection
    output = subprocess.check_output("ping -c 1 " + host, shell=True)
    return output


@app.route("/run")
def run():
    cmd = request.args.get("cmd")
    return os.popen(cmd).read()


@app.route("/download")
def download():
    filename = request.args.get("file")
    # Path traversal
    return send_file("/var/www/files/" + filename)


@app.route("/hello")
def hello():
    name = request.args.get("name", "")
    # XSS / Server-Side Template Injection
    return render_template_string("<h1>Hello " + name + "</h1>")


@app.route("/load", methods=["POST"])
def load():
    data = request.get_data()
    # Désérialisation non sécurisée
    obj = pickle.loads(data)
    return str(obj)


@app.route("/config", methods=["POST"])
def config():
    # yaml.load sans SafeLoader
    cfg = yaml.load(request.get_data())
    return str(cfg)


@app.route("/eval")
def evaluate():
    expr = request.args.get("expr")
    return str(eval(expr))


if __name__ == "__main__":
    app.run(host="0.0.0.0", debug=True)
