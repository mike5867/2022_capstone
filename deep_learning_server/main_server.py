from flask import Flask,request,make_response,jsonify
from werkzeug.utils import secure_filename

app = Flask(__name__)


@app.route('/')
def hello_pybo():
    return '<h1> hello </h1>'


@app.route('/photo', methods=['POST'])
def received():
    f=request.files['uploaded_file']
    f.save('./recevied_photo'+ secure_filename(f.filename)+'.jpeg')


    #return make_response(jsonify({"status number":200}),200)


if __name__ == '__main__':
    app.run(host='0.0.0.0', port=2258)


