from flask import Flask, render_template, request, jsonify
from langchain_groq import ChatGroq
from langchain.prompts import PromptTemplate
from langchain.chains import LLMChain
import os
from dotenv import load_dotenv

load_dotenv()

app = Flask(__name__)
import logging
log = logging.getLogger('werkzeug')
log.setLevel(logging.ERROR)

# Initialize Groq LLM with Llama 3.3 70B (best free model)
llm = ChatGroq(
    model="llama-3.3-70b-versatile",
    api_key=os.getenv("GROQ_API_KEY"),
    temperature=0.7,
    max_tokens=1024
)

# Create sentiment analysis prompt template
sentiment_prompt = PromptTemplate(
    input_variables=["text"],
    template="""Analyze the sentiment of the following text and provide:
1. The sentiment (Positive, Negative, or Neutral)
2. Confidence score (0-100)
3. Brief explanation of why

Text: {text}

Response format:
Sentiment: [sentiment]
Confidence: [score]
Explanation: [explanation]"""
)

# Create the sentiment analysis chain (modern pipe syntax)
sentiment_chain = sentiment_prompt | llm

# Create emotion detection prompt
emotion_prompt = PromptTemplate(
    input_variables=["text"],
    template="""Identify the primary emotions in the following text. List up to 3 emotions with confidence levels.

Text: {text}

Response format:
Emotions: [emotion1: score%, emotion2: score%, emotion3: score%]
Primary Emotion: [main emotion]"""
)

# Create the emotion detection chain (modern pipe syntax)
emotion_chain = emotion_prompt | llm


@app.route('/')
def index():
    """Render the main page"""
    return render_template('index.html')


@app.route('/api/analyze', methods=['POST'])
def analyze_sentiment():
    """Analyze sentiment of provided text"""
    try:
        data = request.get_json()
        text = data.get('text', '').strip()

        if not text:
            return jsonify({'error': 'Text cannot be empty'}), 400

        if len(text) > 5000:
            return jsonify({'error': 'Text is too long (max 5000 characters)'}), 400

        # Run sentiment analysis (modern invoke syntax)
        sentiment_result = sentiment_chain.invoke({"text": text}).content

        return jsonify({
            'text': text,
            'analysis': sentiment_result,
            'success': True
        })

    except Exception as e:
        return jsonify({'error': str(e), 'success': False}), 500


@app.route('/api/emotions', methods=['POST'])
def detect_emotions():
    """Detect emotions in provided text"""
    try:
        data = request.get_json()
        text = data.get('text', '').strip()

        if not text:
            return jsonify({'error': 'Text cannot be empty'}), 400

        if len(text) > 5000:
            return jsonify({'error': 'Text is too long (max 5000 characters)'}), 400

        # Run emotion detection (modern invoke syntax)
        emotion_result = emotion_chain.invoke({"text": text}).content

        return jsonify({
            'text': text,
            'emotions': emotion_result,
            'success': True
        })

    except Exception as e:
        return jsonify({'error': str(e), 'success': False}), 500


@app.route('/api/full-analysis', methods=['POST'])
def full_analysis():
    """Perform complete sentiment and emotion analysis"""
    try:
        data = request.get_json()
        text = data.get('text', '').strip()

        if not text:
            return jsonify({'error': 'Text cannot be empty'}), 400

        if len(text) > 5000:
            return jsonify({'error': 'Text is too long (max 5000 characters)'}), 400

        # Run both analyses (modern invoke syntax)
        sentiment_result = sentiment_chain.invoke({"text": text}).content
        emotion_result = emotion_chain.invoke({"text": text}).content

        return jsonify({
            'text': text,
            'sentiment': sentiment_result,
            'emotions': emotion_result,
            'success': True
        })

    except Exception as e:
        return jsonify({'error': str(e), 'success': False}), 500


@app.route('/api/examples', methods=['GET'])
def get_examples():
    """Get example texts for demonstration"""
    examples = [
        {
            'text': "I absolutely love this product! It's amazing and works perfectly.",
            'category': 'Positive'
        },
        {
            'text': "This is terrible. I'm very disappointed with the quality and service.",
            'category': 'Negative'
        },
        {
            'text': "The weather today is partly cloudy with temperatures around 75 degrees.",
            'category': 'Neutral'
        }
    ]
    return jsonify(examples)


if __name__ == '__main__':
    app.run(debug=False, host='0.0.0.0', port=5000)
