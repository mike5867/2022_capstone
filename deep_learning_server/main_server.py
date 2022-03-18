from flask import Flask

app = Flask(__name__)


@app.route('/')
def hello_pybo():
    return '<h1> hello </h1>'


if __name__ == '__main__':
    app.run(host='0.0.0.0', port=2258)
