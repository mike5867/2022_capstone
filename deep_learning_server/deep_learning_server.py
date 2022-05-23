from flask import Flask,request,make_response,jsonify
from werkzeug.utils import secure_filename

import checking
import result_code

app = Flask(__name__)


@app.route('/')
def hello_pybo():
    return '<h1> hello </h1>'

@app.route('/photo', methods=['POST'])
def received_photo():
    f=request.files['uploaded_file']
    type=request.form['lockerType']
    file_path='./received_photo/'+secure_filename(f.filename)
    f.save(file_path)

    result=checking.judge(file_path,type)

    if result==result_code.Result.PASS:
        return make_response(jsonify({"result":"pass"}))
    elif result==result_code.Result.FAIL:
        return make_response(jsonify({"result":"fail"}))
    elif result==result_code.Result.DIFF:
        return make_response(jsonify({"result":"different"}))
    else: #DETECTED FAIL
        return make_response(jsonify({"result":"detect fail"}))


if __name__ == '__main__':
    app.run(host='0.0.0.0', port=2258)


