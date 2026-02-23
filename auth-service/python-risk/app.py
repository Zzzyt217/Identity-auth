# -*- coding: utf-8 -*-
"""
AI 智能风控 - 异常检测预测服务
加载 model.pkl，提供 POST /predict 接口，供 Java AiRiskEngine 调用。
运行方式：在本目录下执行  python app.py
启动后请在浏览器或 Java 中访问  http://localhost:5000/predict
"""

import os
import numpy as np
import joblib
from flask import Flask, request, jsonify

app = Flask(__name__)

FEATURE_NAMES = ['count_verify_1m', 'count_query_1m', 'count_verify_5m', 'count_query_5m']
MODEL_PATH = os.path.join(os.path.dirname(os.path.abspath(__file__)), 'model.pkl')
model = None


def load_model():
    global model
    if not os.path.exists(MODEL_PATH):
        raise FileNotFoundError('未找到 model.pkl，请先在本目录执行: python train.py')
    model = joblib.load(MODEL_PATH)


def anomaly_score_to_level(score):
    """将 0~1 异常分映射为 高/中/低（对应规则阈值 60：中，80：高，即 0.6/0.8）"""
    if score >= 0.8:
        return '高'
    if score >= 0.6:
        return '中'
    return '低'


@app.route('/predict', methods=['POST'])
def predict():
    """接收 JSON：{ count_verify_1m, count_query_1m, count_verify_5m, count_query_5m }，返回 anomaly_score 与 risk_level"""
    if model is None:
        return jsonify({'error': 'model not loaded'}), 500
    data = request.get_json()
    if not data:
        return jsonify({'error': 'need JSON body'}), 400
    try:
        x = np.array([[int(data.get(k, 0)) for k in FEATURE_NAMES]], dtype=np.float64)
    except (TypeError, ValueError) as e:
        return jsonify({'error': 'invalid feature values', 'detail': str(e)}), 400
    raw = model.decision_function(x)[0]
    # decision_function：越负越异常。映射到 [0,1]，1 表示最异常
    anomaly_score = float(1.0 / (1.0 + np.exp(raw)))
    risk_level = anomaly_score_to_level(anomaly_score)
    return jsonify({
        'anomaly_score': round(anomaly_score, 4),
        'risk_level': risk_level
    })


@app.route('/health', methods=['GET'])
def health():
    return jsonify({'status': 'ok', 'model_loaded': model is not None})


if __name__ == '__main__':
    load_model()
    print('模型已加载，服务启动在 http://127.0.0.1:5000')
    print('预测接口: POST http://127.0.0.1:5000/predict')
    app.run(host='0.0.0.0', port=5000, debug=False)
