# -*- coding: utf-8 -*-
"""
AI 智能风控 - 异常检测模型训练脚本
生成模拟的正常/异常行为数据，训练 Isolation Forest，保存为 model.pkl。
运行方式：在本目录下执行  python train.py
"""

import numpy as np
from sklearn.ensemble import IsolationForest
import joblib
import os

# 特征顺序与 Java / app.py 一致：count_verify_1m, count_query_1m, count_verify_5m, count_query_5m
FEATURE_NAMES = ['count_verify_1m', 'count_query_1m', 'count_verify_5m', 'count_query_5m']


def generate_normal_data(n=500):
    """正常行为：1 分钟内次数少，5 分钟内略多但仍在合理范围"""
    np.random.seed(42)
    v1 = np.random.randint(0, 4, size=n)   # 1 分钟验证 0~3 次
    q1 = np.random.randint(0, 5, size=n)   # 1 分钟查询 0~4 次
    v5 = np.random.randint(0, 10, size=n)   # 5 分钟验证 0~9 次
    q5 = np.random.randint(0, 12, size=n)   # 5 分钟查询 0~11 次
    return np.column_stack([v1, q1, v5, q5])


def generate_anomaly_data(n=150):
    """异常行为：短时密集，1 分钟内验证/查询次数明显偏高。
    下界放低到 4，使「5、6 次」这类行为明确落在异常侧，便于模型与弹窗触发。"""
    np.random.seed(123)
    v1 = np.random.randint(4, 20, size=n)   # 1 分钟验证 4~19 次（含 5、6 次）
    q1 = np.random.randint(3, 15, size=n)   # 1 分钟查询 3~14 次
    v5 = np.random.randint(5, 35, size=n)   # 5 分钟验证 5~34 次（含 5、6 次）
    q5 = np.random.randint(5, 30, size=n)   # 5 分钟查询 5~29 次
    return np.column_stack([v1, q1, v5, q5])


def main():
    base_dir = os.path.dirname(os.path.abspath(__file__))
    model_path = os.path.join(base_dir, 'model.pkl')

    X_normal = generate_normal_data(500)
    X_anomaly = generate_anomaly_data(150)
    X = np.vstack([X_normal, X_anomaly])
    # IsolationForest 训练时：-1 表示异常，1 表示正常；我们只做无监督，不传 y
    model = IsolationForest(n_estimators=100, contamination=0.2, random_state=42)
    model.fit(X)

    joblib.dump(model, model_path)
    print('模型已保存至:', model_path)
    print('特征顺序:', FEATURE_NAMES)


if __name__ == '__main__':
    main()
