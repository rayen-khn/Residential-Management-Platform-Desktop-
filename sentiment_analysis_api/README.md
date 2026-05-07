# Sentiment Analysis with Langchain & Groq

A modern, elegant web application for sentiment analysis and emotion detection powered by Langchain and Groq's advanced language models. Features a stunning **Obsidian Black theme with Red accents** using Liquid Glass glassmorphism design.

## ✨ Features

- **AI-Powered Sentiment Analysis**: Analyze text sentiment with detailed explanations
- **Emotion Detection**: Identify and categorize emotions in text with confidence levels
- **Full Analysis**: Comprehensive side-by-side sentiment and emotion analysis
- **Beautiful UI**: Obsidian black background with red accents, ultra-smooth glassmorphism effects
- **Rich Examples**: 32 diverse, complex examples that regenerate each time you interact
- **Real-time Processing**: Instant analysis powered by Groq (ultra-fast inference)
- **Privacy Focused**: Text is processed securely via Groq's API
- **Free to Use**: Uses Groq's free tier (no credit card required)
- **Responsive Design**: Optimized for desktop, tablet, and mobile devices
- **Keyboard Shortcuts**: Quick access with Alt+S/E/F/C commands

## Technology Stack

- **Backend**: Flask 3.1.3 (Python web framework)
- **AI/ML**: Langchain 0.3.27 + Groq API (Llama 3.3 70B model)
- **Frontend**: HTML5, CSS3 (Obsidian black + red theme with glassmorphism), JavaScript
- **Python Version**: 3.13+
- **API**: RESTful API + Groq API integration

## Prerequisites

Before running this application, you need:

1. **Python 3.13+** - Download from [python.org](https://www.python.org)
2. **Groq API Key** - Free from [console.groq.com](https://console.groq.com)
3. **Windows PowerShell** (or use `.py` scripts directly)

## Installation

### Step 1: Get Groq API Key

1. Visit [console.groq.com](https://console.groq.com)
2. Sign up for a free account
3. Go to API Keys section
4. Create a new API key
5. Copy and save your API key securely

### Step 2: Configure Environment

1. Open the `.env` file in the project directory
2. Replace `your_groq_api_key_here` with your actual Groq API key:

```
GROQ_API_KEY=gsk_your_actual_key_here
MODEL_NAME=llama-3.3-70b-versatile
FLASK_ENV=development
FLASK_DEBUG=True
```

Available Groq Models:
- **llama-3.3-70b-versatile** (Recommended - Currently used)
- **llama-2-70b-4096**
- **mixtral-8x7b-32768** (Decommissioned)

### Step 3: Install Dependencies (Windows)

**Option A: Automated PowerShell Script (Recommended)**
```bash
.\start_server.ps1
```
This script automatically:
- Creates virtual environment if needed
- Installs all dependencies from requirements.txt
- Starts the Flask server
- Keeps window open for monitoring

**Option B: Manual batch file**
```bash
install_requirements.bat
```

**Option C: Direct Python installation (no venv)**
```bash
install_direct.bat
```

## Running the Application

### Quick Start (Windows - Recommended)

1. Run the PowerShell script:
```bash
.\start_server.ps1
```

2. The script will:
   - Create a virtual environment
   - Install all dependencies
   - Start the Flask server
   - Keep the window open (won't close when server stops)

3. Open your browser to `http://localhost:5000`

### Manual Start

```bash
# Create virtual environment
python -m venv venv

# Activate it
venv\Scripts\activate

# Install dependencies
pip install -r requirements.txt

# Run the Flask app
python app.py
```

You should see:
```
 * Running on http://127.0.0.1:5000
 * Debug mode: on
```

## Usage

### Sentiment Analysis
1. Enter or paste text in the input area (up to 5000 characters)
2. Click "Sentiment" button
3. View the sentiment classification with detailed explanation

### Emotion Detection
1. Enter text in the input area
2. Click "Emotions" button
3. View identified emotions with their analysis

### Full Analysis
1. Enter text in the input area
2. Click "Full Analysis" button
3. View **side-by-side comparison** of:
   - Sentiment Analysis (left panel)
   - Emotion Detection (right panel)
4. Both analyses appear with enhanced styling and icons

### Rich Examples
- Browse 32 diverse, complex examples in the "Try an Example" section
- Click any example to load it into the text area
- Examples automatically **regenerate after each interaction**
- Examples cover multiple categories:
  - Positive, Negative, Neutral sentiments
  - Enthusiastic, Anxious, Grateful emotions
  - Mixed feelings, factual statements
  - Professional and technical contexts

### Keyboard Shortcuts
- **Alt+S**: Analyze Sentiment
- **Alt+E**: Detect Emotions
- **Alt+F**: Perform Full Analysis
- **Alt+C**: Clear Text

## API Endpoints

### POST `/api/analyze`
Analyze text sentiment

**Request:**
```json
{
  "text": "I love this product!"
}
```

**Response:**
```json
{
  "text": "I love this product!",
  "analysis": "Sentiment: Positive\nConfidence: 95\nExplanation: ...",
  "success": true
}
```

### POST `/api/emotions`
Detect emotions in text

**Request:**
```json
{
  "text": "I'm so excited about this!"
}
```

**Response:**
```json
{
  "text": "I'm so excited about this!",
  "emotions": "Emotions: Joy: 90%, Excitement: 85%\nPrimary Emotion: Joy",
  "success": true
}
```

### POST `/api/full-analysis`
Get complete sentiment and emotion analysis

**Request:**
```json
{
  "text": "Sample text here"
}
```

**Response:**
```json
{
  "text": "Sample text here",
  "sentiment": "...",
  "emotions": "...",
  "success": true
}
```

### GET `/api/examples`
Get example texts for demonstration

## Groq API Information

### Why Groq?
- **Ultra-Fast Inference**: 10-100x faster than other LLMs
- **Free Tier**: Generous free API credits
- **No Setup Required**: No local installation needed
- **Cloud-Based**: Works anywhere with internet connection
- **Best Model**: Mixtral 8x7B - powerful and efficient

### Free Tier Limits
- **Rate Limit**: 30 requests per minute
- **Tokens**: Up to 8,000 tokens per request
- **No Credit Card**: Free signup required
- **Unlimited Usage**: Within rate limits

Get your free API key at: [console.groq.com](https://console.groq.com)

### Available Groq Models

| Model | Speed | Quality | Status |
|-------|-------|---------|--------|
| **llama-3.3-70b-versatile** | ⚡⚡⚡ Fast | ⭐⭐⭐⭐⭐ Excellent | ✅ Currently Used |
| **llama-2-70b-4096** | ⚡⚡ Moderate | ⭐⭐⭐⭐⭐ Excellent | Available |
| **mixtral-8x7b-32768** | ⚡⚡⚡⚡ Very Fast | ⭐⭐⭐⭐⭐ Excellent | ❌ Decommissioned |

## Project Structure

```
sentiment-analysis-w-langchain/
├── app.py                        # Flask application & API endpoints
├── config.py                     # Configuration settings
├── requirements.txt              # Python dependencies
├── .env                          # Environment variables (API keys)
├── .gitignore                    # Git ignore rules
├── start_server.ps1             # PowerShell auto-installer & server launcher
├── install_requirements.bat      # Manual requirements installer
├── install_direct.bat           # Direct Python installation (no venv)
├── README.md                     # This file
├── templates/
│   └── index.html               # Main HTML template
└── static/
    ├── css/
    │   └── style.css            # Obsidian black + red theme styling
    └── js/
        └── script.js            # Frontend logic & examples (32 examples)
```

## Customization

### Change Language Model
Edit the `MODEL_NAME` in `.env`:

```
MODEL_NAME=llama-2-70b-4096    # Switch to Llama 2
```

Or modify `app.py`:
```python
llm = ChatGroq(
    model="llama-2-70b-4096",
    temperature=0.7,
    max_tokens=1024
)
```

Available models:
- `llama-3.3-70b-versatile` - Current default
- `llama-2-70b-4096` - Alternative option
- `mixtral-8x7b-32768` - Decommissioned (no longer available)

### Adjust Model Temperature
Edit `app.py` to change creativity/randomness:
```python
temperature=0.3  # More consistent (0.0-0.3)
temperature=0.7  # Balanced (0.5-0.7)
temperature=1.0  # More creative (0.8-1.0)
```

### Modify Prompts
Edit the prompt templates in `app.py` to customize analysis behavior:

```python
sentiment_prompt = PromptTemplate(
    input_variables=["text"],
    template="Your custom prompt here..."
)
```

### Customize Theme
Edit `static/css/style.css` to modify colors, spacing, or animations.

## Troubleshooting

### "Invalid API Key" or "Unauthorized" Error
1. Check your `.env` file has the correct API key
2. Verify you copied the entire API key from [console.groq.com](https://console.groq.com)
3. No extra spaces or quotes around the key
4. Ensure the key hasn't expired or been revoked

### "Module not found" error
- Make sure you ran `start_server.ps1` or `install_requirements.bat`
- Or manually run: `pip install -r requirements.txt`
- Verify Python 3.13+ is installed: `python --version`

### "Model decommissioned" Error (Mixtral)
- **Solution**: Model has been decommissioned by Groq
- The application now uses `llama-3.3-70b-versatile`
- If you manually changed the model, update `.env` or `app.py` back to the current model

### PowerShell Script Won't Run
- Check execution policy: `Get-ExecutionPolicy`
- If "Restricted", run: `Set-ExecutionPolicy -ExecutionPolicy RemoteSigned -Scope CurrentUser`
- Or use `install_requirements.bat` instead

### "Rate limit exceeded" Error
- You've hit Groq's free tier rate limit (30 requests/minute)
- Wait a minute and try again
- Consider upgrading to a paid plan for higher limits

### "Connection refused" Error
- Check your internet connection
- Verify Groq API is accessible
- Try visiting [console.groq.com](https://console.groq.com) to confirm access

### Port 5000 Already in Use
- Change Flask port in `app.py`: `app.run(port=5001)`

### Slow Responses
- First response may take 5-10 seconds (model loading)
- Subsequent requests are much faster
- Check internet connection speed
- Llama 3.3 is already optimized for speed

## Performance Tips

1. **Llama 3.3 70B** - Excellent balance of speed and quality
2. **Monitor API Usage** - Check [console.groq.com](https://console.groq.com) for analytics
3. **Keep Text Concise** - Shorter text = faster responses and fewer tokens
4. **Avoid Rapid Requests** - Stay below 30 requests/minute (free tier)
5. **Browser Caching** - Results are cached client-side for repeated inputs

## UI Design

### Obsidian Black Theme with Red Accents
- **Primary Color**: Obsidian Red (#ff1744) for highlights and CTAs
- **Background**: Pure black (#000000) for minimal eye strain
- **Glass Effect**: Glassmorphism design with red-tinted transparency
- **Typography**: Bold, high-contrast text for readability
- **Spacing**: Increased padding and gaps for breathing room

### Key UI Features
- **Side-by-side Analysis**: Full analysis displays sentiment and emotions in parallel grid
- **Hover Effects**: Smooth color transitions and lift animations
- **Responsive Grid**: Adapts from 2 columns (desktop) to 1 column (mobile)
- **Example Auto-Regeneration**: Fresh examples appear after each interaction
- **Loading Indicators**: Real-time feedback with animated spinner

## Future Enhancements

- [ ] Multi-language support
- [ ] Sentiment visualization (charts/graphs)
- [ ] History/saved analyses
- [ ] Batch analysis
- [ ] Custom model training
- [ ] API authentication
- [ ] Database integration for results storage

## License

MIT License - Feel free to use and modify

## Support

For issues or questions:
1. Check the **Troubleshooting** section first
2. Verify your Groq API key is valid and not expired
3. Ensure all dependencies are installed: `pip install -r requirements.txt`
4. Check terminal output for detailed error messages
5. Verify Python 3.13+ is installed: `python --version`
6. Test internet connection to Groq API servers

## Credits

Built with:
- **Flask** - Web framework
- **Langchain** - LLM orchestration framework
- **Groq** - Ultra-fast inference API
- **Llama 3.3 70B** - Language model
- **HTML5/CSS3/JavaScript** - Modern web technologies

## Version History

- **v2.0** - Obsidian black + red theme, Llama 3.3 70B, enhanced UI, 32 rich examples
- **v1.0** - Initial release with Mixtral 8x7B
- Ollama for local AI models
- Modern CSS for Liquid Glass Obsidian design

---

**Enjoy analyzing sentiment and emotions with AI!** 🚀✨
