from flask import Flask, request, make_response, jsonify

import pymysql
import time

app = Flask(__name__)

lockerdb = pymysql.connect(host='localhost', port=3306, user='root', db='penaltykickdb', password='root',
                           charset='utf8')
cursor = lockerdb.cursor()


@app.route('/')
def hello():
    return '<h1> hello </h1>'


@app.route('/login', methods=['GET'])
def login():
    userid = request.args.get('id')
    userpw = request.args.get('pw')

    sql = "select * from users where id=\'" + userid + "\'"
    cursor.execute(sql)
    result = cursor.fetchone()

    if result == None:
        return make_response(jsonify({"result": "not exist"}))
    else:
        return make_response(jsonify({"result": "exist", "locker id": str(result[2])}))


@app.route('/state', methods=['GET'])
def update_state():
    locker_id = request.args.get('id')
    lock_state = request.args.get('state')
    latitude = request.args.get('lat')
    longitude = request.args.get('long')

    # 위도, 경도가 null이 아닌경우에만 db update
    if latitude == "null" or longitude == "null":
        pass
    else:
        sql = "update locker set latitude=" + latitude + ", longitude=" + longitude + " where id=\'" + locker_id + "\'"
        cursor.execute(sql)
        lockerdb.commit()


    sql = "update locker set checkflag=1 where id=\'" + locker_id + "\'"
    cursor.execute(sql)
    lockerdb.commit()

    sql = "select lockflag from locker where id=\'" + locker_id + "\'"
    cursor.execute(sql)
    result = cursor.fetchone()

    if result[0] != int(lock_state):
        return make_response("changed\n")
    else:
        return make_response("unchanged\n")


@app.route('/lock', methods=['GET'])
def lock_request():
    locker_id = request.args.get('id')

    for i in range(0, 3):
        sql = "update locker set lockflag=1, checkflag=0 where id=\'" + locker_id + "\'"
        cursor.execute(sql)
        lockerdb.commit()
        time.sleep(5)

        sql = "select checkflag from locker where id=\'" + locker_id + "\'"
        cursor.execute(sql)
        result = cursor.fetchone()
        if result[0] == 1:  # 잠금장치가 확인한 경우
            return make_response(jsonify({"result": "success"}))

    sql = "update locker set lockflag=0 where id=\'" + locker_id + "\'"
    cursor.execute(sql)
    lockerdb.commit()

    return make_response(jsonify({"result": "fail"}))


@app.route('/unlock', methods=['GET'])
def unlock_request():
    locker_id = request.args.get('id')
    sql = "select lockflag from locker where id=\'" + locker_id + "\'"
    cursor.execute(sql)
    result = cursor.fetchone()

    if result[0] == 0:  # 잠겨있지 않은 경우
        return make_response(jsonify({"result": "already unlock"}))

    for i in range(0, 3):
        sql = "update locker set lockflag=0, checkflag=0 where id=\'" + locker_id + "\'"
        cursor.execute(sql)
        lockerdb.commit()
        time.sleep(5)

        sql = "select checkflag from locker where id=\'" + locker_id + "\'"
        cursor.execute(sql)
        result = cursor.fetchone()

        if result[0] == 1:  # 잠금장치가 확인한 경우
            return make_response(jsonify({"result": "success"}))

    sql = "update locker set lockflag=1 where id=\'" + locker_id + "\'"
    cursor.execute(sql)
    lockerdb.commit()

    return make_response(jsonify({"result": "fail"}))


@app.route('/location', methods=['GET'])
def location_request():
    currentLatitude=request.args.get('lat')
    currentLongitude=request.args.get('long')

    sql = "select id, latitude, longitude, (6371*acos(cos(radians("+currentLatitude+"))" \
        "*cos(radians(latitude))*cos(radians(longitude)-radians("+currentLongitude+"))" \
        "+sin(radians("+currentLatitude+"))*sin(radians(latitude))))as distance from locker having distance <=3"
    cursor.execute(sql)
    result = cursor.fetchall()

    lockers = []
    for row in result:
        id = row[0]
        latitude = row[1]
        longitude = row[2]

        locker = {"id": id, "latitude": latitude, "longitude": longitude}
        lockers.append(locker)

    return make_response(jsonify(lockers))

@app.route('/idcheck',methods=['GET'])
def idcheck_request():
    id=request.args.get('id')

    sql="select count(*) from users where id=\'"+id+"\'"
    cursor.execute(sql)
    result=cursor.fetchone()

    if result[0]>=1: #중복 되는 경우
        return make_response(jsonify({"result":"fail"}))
    else:
        return make_response(jsonify({"result":"pass"}))


@app.route('/adduser',methods=['POST'])
def adduser_request():
    if(request.is_json):
        params=request.get_json()
        id=params['id']
        password=params['password']
        email=params['email']

        data=(id,password,email)
        sql="insert into users(id,pw,email) values(%s,%s,%s)"
        cursor.execute(sql,data)
        lockerdb.commit()

        return make_response(jsonify({"result":"pass"}))

    return make_response(jsonify({"result":"fail"}))



if __name__ == "__main__":
    app.run(host='0.0.0.0', port=2259, debug=True, threaded=True)
