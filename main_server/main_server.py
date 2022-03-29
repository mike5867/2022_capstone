from flask import Flask,request,make_response,jsonify

import pymysql
import time

app = Flask(__name__)

@app.route('/')
def hello():
    return '<h1> hello </h1>'

lockerdb=pymysql.connect(host='localhost',port=3306,user='root',db='penaltykickdb',password='root',charset='utf8')
cursor=lockerdb.cursor()


@app.route('/state',methods=['GET'])
def update_state():
    locker_id=request.args.get('id')
    lock_state=request.args.get('state')

    sql="""update locker set checkflag=1 where id="""+locker_id
    cursor.execute(sql)
    lockerdb.commit()

    sql="""select lockflag from locker where id="""+locker_id
    cursor.execute(sql)
    result=cursor.fetchone()

    if result[0]!=int(lock_state):
        return make_response("changed\n")
    else:
        return make_response("unchanged\n")

@app.route('/lock',methods=['GET'])
def lock_request():
    locker_id=request.args.get('id')

    for i in range(0,3):
        sql="""update locker set lockflag=1, checkflag=0 where id="""+locker_id
        cursor.execute(sql)
        lockerdb.commit()
        time.sleep(5)

        sql="""select checkflag from locker where id="""+locker_id
        cursor.execute(sql)
        result=cursor.fetchone()

        if result[0]==1: # 잠금장치가 확인한 경우
            return make_response(jsonify({"result":"success"}))


    sql="""update locker set lockflag=0 where id="""+locker_id
    cursor.execute(sql)
    lockerdb.commit()

    return make_response(jsonify({"result":"fail"}))


@app.route('/unlock',methods=['GET'])
def unlock_request():
    locker_id=request.args.get('id')
    sql="""select lockflag from locker where id="""+locker_id
    cursor.execute(sql)
    result=cursor.fetchone()

    if result[0]==0: # 잠겨있지 않은 경우
        return make_response(jsonify({"result":"already lock"}))

    for i in range(0,3):
        sql="""update locker set lockflag=0, checkflag=0 where id="""+locker_id
        cursor.execute(sql)
        lockerdb.commit()
        time.sleep(5)

        sql="""select checkflag from locker where id="""+locker_id
        cursor.execute(sql)
        result=cursor.fetchone()

        if result[0]==1: # 잠금장치가 확인한 경우
            return make_response(jsonify({"result":"success"}))

    sql = """update locker set lockflag=1 where id=""" + locker_id
    cursor.execute(sql)
    lockerdb.commit()

    return make_response(jsonify({"result":"fail"}))


if __name__=="__main__":
    app.run(host='0.0.0.0',port=2259)