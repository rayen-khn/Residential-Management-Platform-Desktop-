"""
Configuration file for Sentiment Analysis application
"""

import os
from dotenv import load_dotenv

load_dotenv()

class Config:
    """Base configuration"""
    DEBUG = False
    TESTING = False
    SECRET_KEY = os.getenv('SECRET_KEY', 'dev-secret-key-change-in-production')
    
    # Ollama Configuration
    OLLAMA_API_BASE = os.getenv('OLLAMA_API_BASE', 'http://localhost:11434')
    MODEL_NAME = os.getenv('MODEL_NAME', 'mistral')
    
    # Application Configuration
    MAX_TEXT_LENGTH = 5000
    TIMEOUT = 300  # Request timeout in seconds


class DevelopmentConfig(Config):
    """Development configuration"""
    DEBUG = True
    ENV = 'development'


class ProductionConfig(Config):
    """Production configuration"""
    DEBUG = False
    ENV = 'production'


class TestingConfig(Config):
    """Testing configuration"""
    DEBUG = True
    TESTING = True
    ENV = 'testing'


# Select configuration based on environment
config = {
    'development': DevelopmentConfig,
    'production': ProductionConfig,
    'testing': TestingConfig,
    'default': DevelopmentConfig
}

def get_config():
    """Get configuration based on FLASK_ENV"""
    env = os.getenv('FLASK_ENV', 'development')
    return config.get(env, config['default'])
