import re
from typing import List
from sklearn.feature_extraction.text import TfidfVectorizer
import numpy as np

def split_sentences(text: str) -> List[str]:
    """
    Простое разбиение на предложения по знакам препинания.
    Для MVP достаточно. В проде лучше использовать nltk/spacy.
    """
    # Разбиваем по . ! ? с последующим пробелом или концом строки
    # (lookbehind не используем, чтобы не усложнять, просто re.split)
    sentences = re.split(r'(?<=[.!?])\s+', text)
    return [s.strip() for s in sentences if s.strip()]

def summarize_text(text: str, num_sentences: int = 5) -> dict:
    """
    Создает extractive summary на основе TF-IDF.
    Возвращает словарь с summary и списком предложений.
    """
    sentences = split_sentences(text)
    
    # Если предложений меньше, чем просим — возвращаем всё
    if len(sentences) <= num_sentences:
        return {
            "summary": text,
            "bullets": sentences
        }

    # Считаем TF-IDF матрицу
    # stop_words='english' уберет английские стоп-слова. 
    # Для русского scikit-learn не имеет встроенных, но для MVP TF-IDF сработает и так (частые слова и так будут иметь низкий вес)
    vectorizer = TfidfVectorizer(min_df=1, stop_words=None) 
    
    try:
        tfidf_matrix = vectorizer.fit_transform(sentences)
    except ValueError:
        # Если словарь пустой (например, текст из одних стоп-слов или знаков)
        return {"summary": text, "bullets": sentences}

    # Считаем сумму весов для каждого предложения (важность предложения)
    sentence_scores = np.array(tfidf_matrix.sum(axis=1)).flatten()

    # Сортируем индексы предложений по убыванию веса
    ranked_indices = sentence_scores.argsort()[::-1]
    
    # Берем top-k лучших
    top_indices = ranked_indices[:num_sentences]
    
    # Сортируем их обратно по порядку появления в тексте, чтобы сохранить логику повествования
    top_indices = sorted(top_indices)

    selected_sentences = [sentences[i] for i in top_indices]
    
    summary_text = " ".join(selected_sentences)

    return {
        "summary": summary_text,
        "bullets": selected_sentences
    }
