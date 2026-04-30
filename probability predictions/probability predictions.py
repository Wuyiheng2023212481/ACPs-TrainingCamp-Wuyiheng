import joblib
import pandas as pd

# 定义模型文件路径
model_files = {
    "Logistic Regression": r"C:\Users\26782\Desktop\比赛数据集\结直肠癌\saved_models\Logistic_Regression.pkl",
    "Random Forest": r"C:\Users\26782\Desktop\比赛数据集\结直肠癌\saved_models\Random_Forest.pkl",
    "XGBoost": r"C:\Users\26782\Desktop\比赛数据集\结直肠癌\saved_models\XGBoost.pkl"
}

# 加载模型
models = {name: joblib.load(path) for name, path in model_files.items()}

# 已知模型的输入特征
feature_columns = [
    'Country',
    'Age',
    'Gender',
    'Cancer_Stage',
    'Tumor_Size_mm',
    'Family_History',
    'Smoking_History',
    'Alcohol_Consumption',
    'Obesity_BMI',
    'Diet_Risk',
    'Physical_Activity',
    'Diabetes',
    'Inflammatory_Bowel_Disease',
    'Genetic_Mutation',
    'Screening_History',
    'Early_Detection',
    'Treatment_Type',
    'Healthcare_Costs',
    'Incidence_Rate_per_100K',
    'Mortality_Rate_per_100K',
    'Urban_or_Rural',
    'Economic_Classification',
    'Healthcare_Access',
    'Insurance_Status'
]

# 创建一个示例输入数据（需根据实际模型特征预处理）
example_data = pd.DataFrame([[  # 示例输入数据
    'USA',
    77,
    'M',
    "Localized",
    12,
    'Yes',
    'Yes',
    'Yes',
    'Overweight',
    'Low',
    'Low',
    'No',
    'No',
    'No',
    'Never',
    'Yes',
    'Combination',
    10000,
    50,
    5,
    'Urban',
    'Developed',
    'Low',
    'Insured'
]], columns=feature_columns)

# 对所有模型进行概率预测
probability_predictions = {}
for model_name, model in models.items():
    probability_predictions[model_name] = model.predict_proba(example_data)[:, 1]  # 获取患病（类别 1）的概率

# 定义权重
weights = {
    "Logistic Regression": 0.1,
    "Random Forest": 0.1,
    "XGBoost": 0.8
}

# 计算加权平均概率
final_probability = sum(probability_predictions[model_name][0] * weight for model_name, weight in weights.items())

# 输出最终的加权预测概率
print(f"基于模型加权后预测的存活概率: {final_probability:.4f}")


