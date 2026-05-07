// Sentiment Analysis App - JavaScript

// Example texts pool for random generation - Complex & Diverse Examples
const examplesPool = [
    // Positive Examples
    { text: "I absolutely love this product! It exceeded all my expectations and transformed my daily routine.", category: "Positive" },
    { text: "The customer service team was incredibly helpful and patient. They went above and beyond!", category: "Positive" },
    { text: "This is hands down the best purchase I've made this year. Highly recommended!", category: "Positive" },
    
    // Negative Examples
    { text: "This service is terrible and a complete waste of money. Extremely disappointed with the results.", category: "Negative" },
    { text: "The product broke after just two weeks. Poor quality and terrible customer support.", category: "Negative" },
    { text: "I'm deeply frustrated. The documentation was misleading and the implementation was flawed.", category: "Negative" },
    
    // Neutral Examples
    { text: "The weather today is 72 degrees Fahrenheit with 30% humidity and light winds.", category: "Neutral" },
    { text: "It's an okay product. Does what it's supposed to do without any special features.", category: "Neutral" },
    { text: "The meeting was scheduled for 3 PM and lasted approximately one hour.", category: "Neutral" },
    
    // Enthusiastic Examples
    { text: "I can't believe how amazing this movie was! Absolutely groundbreaking cinematography and storytelling!", category: "Enthusiastic" },
    { text: "This technology is revolutionary! It completely changed how I approach my work. Simply incredible!", category: "Enthusiastic" },
    { text: "The innovation here is phenomenal! This solution elegantly addresses problems I've struggled with for years!", category: "Enthusiastic" },
    
    // Anxious/Worried Examples
    { text: "I'm genuinely worried about the implications of this decision. There are so many variables I can't control.", category: "Anxious" },
    { text: "This uncertainty is making me nervous. I hope everything works out, but I have serious concerns.", category: "Anxious" },
    { text: "The deadline is approaching and I'm feeling increasingly stressed about meeting all the requirements.", category: "Anxious" },
    
    // Grateful Examples
    { text: "What a wonderful surprise! I'm truly grateful for your help and kindness during this difficult time.", category: "Grateful" },
    { text: "I can't thank you enough for everything you've done. Your support means the world to me.", category: "Grateful" },
    
    // Disappointed Examples
    { text: "The customer support was unhelpful, dismissive, and rude. I won't be returning to this company again.", category: "Disappointed" },
    { text: "I had high hopes for this event, but it was poorly organized and disappointing from start to finish.", category: "Disappointed" },
    
    // Proud Examples
    { text: "Finally got the promotion I've been working towards for three years! So proud of my accomplishments!", category: "Proud" },
    { text: "I'm incredibly proud of our team's hard work and dedication. We delivered an exceptional product!", category: "Proud" },
    
    // Confused Examples
    { text: "I'm confused by the instructions provided. The documentation contradicts itself in several places.", category: "Confused" },
    { text: "The interface is confusing and unintuitive. I spent hours trying to figure out basic functionality.", category: "Confused" },
    
    // Delighted Examples
    { text: "This restaurant has the most incredible food I've ever tasted! Every dish was absolutely delightful!", category: "Delighted" },
    { text: "I was delighted by the unexpected upgrade! The attention to detail and quality is remarkable.", category: "Delighted" },
    
    // Mixed Feelings
    { text: "I appreciate the effort and innovation, but execution could be significantly better. Great concept, rough implementation.", category: "Mixed" },
    { text: "The product has some excellent features, but performance issues are preventing me from fully recommending it.", category: "Mixed" },
    
    // Hopeful Examples
    { text: "I'm cautiously optimistic about these new changes. They show promise, and I'm hopeful for future improvements.", category: "Hopeful" },
    { text: "Despite the current challenges, I believe in the potential of this project. The vision is inspiring!", category: "Hopeful" },
    
    // Hurt/Betrayed Examples
    { text: "I feel deeply betrayed by your broken promises and lack of accountability. This is deeply disappointing.", category: "Hurt" },
    { text: "After everything I've done, this treatment is hurtful and disrespectful. I'm feeling let down.", category: "Hurt" },
    
    // Sad Examples
    { text: "The quality has declined significantly over the years. It's truly unfortunate to see something once great become mediocre.", category: "Sad" },
    { text: "I'm saddened by the closure of this wonderful community. It brought joy to so many people throughout the years.", category: "Sad" },
    
    // Outraged Examples
    { text: "This is the worst experience I've ever had with any company. The negligence is absolutely infuriating!", category: "Outraged" },
    { text: "I'm absolutely outraged by this unethical behavior and deceptive marketing practices!", category: "Outraged" },
    
    // Thankful Examples
    { text: "Feeling blessed and thankful to have such amazing people in my life supporting me unconditionally!", category: "Thankful" },
    { text: "I'm deeply thankful for this opportunity and everyone who believed in me along the way.", category: "Thankful" }
];

document.addEventListener('DOMContentLoaded', function() {
    // DOM Elements
    const inputText = document.getElementById('inputText');
    const charCount = document.getElementById('charCount');
    const sentimentBtn = document.getElementById('sentimentBtn');
    const emotionBtn = document.getElementById('emotionBtn');
    const fullAnalysisBtn = document.getElementById('fullAnalysisBtn');
    const clearBtn = document.getElementById('clearBtn');
    const loadingSpinner = document.getElementById('loadingSpinner');
    const resultsContainer = document.getElementById('resultsContainer');
    const closeResults = document.getElementById('closeResults');
    const examplesList = document.getElementById('examplesList');

    // Initialize
    loadRandomExamples();
    setupEventListeners();

    // Character count update
    inputText.addEventListener('input', function() {
        charCount.textContent = this.value.length;
    });

    // Setup Event Listeners
    function setupEventListeners() {
        sentimentBtn.addEventListener('click', analyzeSentiment);
        emotionBtn.addEventListener('click', detectEmotions);
        fullAnalysisBtn.addEventListener('click', performFullAnalysis);
        clearBtn.addEventListener('click', clearAnalysis);
        closeResults.addEventListener('click', closeResultsPanel);

        // Example items click handlers (delegated)
        examplesList.addEventListener('click', function(e) {
            const exampleItem = e.target.closest('.example-item');
            if (exampleItem) {
                const text = exampleItem.querySelector('.example-text').textContent;
                inputText.value = text;
                charCount.textContent = text.length;
                // Regenerate examples after selection
                loadRandomExamples();
            }
        });
    }

    // Load random example texts
    function loadRandomExamples() {
        // Shuffle and select 3 random examples
        const shuffled = [...examplesPool].sort(() => 0.5 - Math.random());
        const selected = shuffled.slice(0, 3);
        
        examplesList.innerHTML = '';
        selected.forEach((example) => {
            const div = document.createElement('div');
            div.className = 'example-item';
            div.innerHTML = `
                <div class="example-text">${escapeHtml(example.text)}</div>
                <span class="example-label">${example.category}</span>
            `;
            examplesList.appendChild(div);
        });
    }

    // Analyze sentiment
    async function analyzeSentiment() {
        const text = inputText.value.trim();
        if (!validateInput(text)) return;

        showLoading();
        try {
            const response = await fetch('/api/analyze', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({ text: text })
            });

            const data = await response.json();
            hideLoading();

            if (data.success) {
                displaySentimentResults(data.analysis);
            } else {
                displayError(data.error);
            }
        } catch (error) {
            hideLoading();
            displayError('Failed to analyze sentiment: ' + error.message);
        }
    }

    // Detect emotions
    async function detectEmotions() {
        const text = inputText.value.trim();
        if (!validateInput(text)) return;

        showLoading();
        try {
            const response = await fetch('/api/emotions', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({ text: text })
            });

            const data = await response.json();
            hideLoading();

            if (data.success) {
                displayEmotionResults(data.emotions);
            } else {
                displayError(data.error);
            }
        } catch (error) {
            hideLoading();
            displayError('Failed to detect emotions: ' + error.message);
        }
    }

    // Perform full analysis
    async function performFullAnalysis() {
        const text = inputText.value.trim();
        if (!validateInput(text)) return;

        showLoading();
        try {
            const response = await fetch('/api/full-analysis', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({ text: text })
            });

            const data = await response.json();
            hideLoading();

            if (data.success) {
                displayFullAnalysis(data.sentiment, data.emotions);
            } else {
                displayError(data.error);
            }
        } catch (error) {
            hideLoading();
            displayError('Failed to perform analysis: ' + error.message);
        }
    }

    // Validate input
    function validateInput(text) {
        if (!text) {
            displayError('Please enter some text to analyze');
            return false;
        }
        if (text.length > 5000) {
            displayError('Text is too long (maximum 5000 characters)');
            return false;
        }
        return true;
    }

    // Display sentiment results
    function displaySentimentResults(analysis) {
        hideAllResults();
        const sentimentResults = document.getElementById('sentimentResults');
        const sentimentContent = document.getElementById('sentimentContent');
        
        sentimentContent.textContent = analysis;
        sentimentResults.classList.remove('hidden');
        resultsContainer.classList.remove('hidden');
        resultsContainer.scrollIntoView({ behavior: 'smooth' });
    }

    // Display emotion results
    function displayEmotionResults(emotions) {
        hideAllResults();
        const emotionResults = document.getElementById('emotionResults');
        const emotionContent = document.getElementById('emotionContent');
        
        emotionContent.textContent = emotions;
        emotionResults.classList.remove('hidden');
        resultsContainer.classList.remove('hidden');
        resultsContainer.scrollIntoView({ behavior: 'smooth' });
    }

    // Display full analysis
    function displayFullAnalysis(sentiment, emotions) {
        hideAllResults();
        const fullResults = document.getElementById('fullResults');
        const fullContent = document.getElementById('fullContent');
        
        fullContent.innerHTML = `
            <div class="full-analysis-wrapper">
                <div class="full-analysis-section sentiment-section">
                    <div class="full-analysis-header">
                        <i class="fas fa-chart-line"></i>
                        <h4>Sentiment Analysis</h4>
                    </div>
                    <div class="full-analysis-content">${escapeHtml(sentiment)}</div>
                </div>
                
                <div class="full-analysis-section emotion-section">
                    <div class="full-analysis-header">
                        <i class="fas fa-heart"></i>
                        <h4>Emotion Detection</h4>
                    </div>
                    <div class="full-analysis-content">${escapeHtml(emotions)}</div>
                </div>
            </div>
        `;
        
        fullResults.classList.remove('hidden');
        resultsContainer.classList.remove('hidden');
        resultsContainer.scrollIntoView({ behavior: 'smooth' });
    }

    // Display error
    function displayError(message) {
        hideAllResults();
        const errorContainer = document.getElementById('errorContainer');
        const errorText = document.getElementById('errorText');
        
        errorText.textContent = message;
        errorContainer.classList.remove('hidden');
        resultsContainer.classList.remove('hidden');
        resultsContainer.scrollIntoView({ behavior: 'smooth' });
    }

    // Hide all results
    function hideAllResults() {
        document.getElementById('sentimentResults').classList.add('hidden');
        document.getElementById('emotionResults').classList.add('hidden');
        document.getElementById('fullResults').classList.add('hidden');
        document.getElementById('errorContainer').classList.add('hidden');
    }

    // Show loading spinner
    function showLoading() {
        loadingSpinner.classList.remove('hidden');
        resultsContainer.classList.add('hidden');
    }

    // Hide loading spinner
    function hideLoading() {
        loadingSpinner.classList.add('hidden');
    }

    // Clear analysis
    function clearAnalysis() {
        inputText.value = '';
        charCount.textContent = '0';
        resultsContainer.classList.add('hidden');
        inputText.focus();
    }

    // Close results panel
    function closeResultsPanel() {
        resultsContainer.classList.add('hidden');
    }

    // Escape HTML to prevent XSS
    function escapeHtml(text) {
        const map = {
            '&': '&amp;',
            '<': '&lt;',
            '>': '&gt;',
            '"': '&quot;',
            "'": '&#039;'
        };
        return text.replace(/[&<>"']/g, m => map[m]);
    }

    // Smooth scrolling for navigation links
    document.querySelectorAll('a[href^="#"]').forEach(anchor => {
        anchor.addEventListener('click', function (e) {
            e.preventDefault();
            const target = document.querySelector(this.getAttribute('href'));
            if (target) {
                target.scrollIntoView({
                    behavior: 'smooth',
                    block: 'start'
                });
            }
        });
    });

    // Add keyboard shortcuts
    document.addEventListener('keydown', function(e) {
        // Alt+S for Sentiment Analysis
        if (e.altKey && e.key === 's') {
            e.preventDefault();
            analyzeSentiment();
        }
        // Alt+E for Emotion Detection
        if (e.altKey && e.key === 'e') {
            e.preventDefault();
            detectEmotions();
        }
        // Alt+F for Full Analysis
        if (e.altKey && e.key === 'f') {
            e.preventDefault();
            performFullAnalysis();
        }
        // Alt+C for Clear
        if (e.altKey && e.key === 'c') {
            e.preventDefault();
            clearAnalysis();
        }
    });
});
