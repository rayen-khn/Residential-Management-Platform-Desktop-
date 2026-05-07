# Changelog

All notable changes to this project will be documented in this file.

## [2.0.0] - 2026-03-04

### Major Updates

#### 🎨 Complete UI Redesign
- **Theme**: Migrated from cyan/purple to Obsidian Black with Red Accents (#ff1744)
- **Glass Effect**: Enhanced glassmorphism with red-tinted transparency (rgba(255,23,68,0.15))
- **Background**: Pure black (#000000) for reduced eye strain
- **Typography**: Increased font sizes and weights for better hierarchy
  - H1: 4rem → 5rem
  - H2: 2rem → 2.2rem
  - Nav Brand: 1.5rem → 1.8rem
- **Spacing**: Significantly increased padding and gaps (15-30% improvement)
  - Container max-width: 1600px → 1800px
  - Glass cards padding: 3rem → 4rem
  - Input min-height: 240px → 280px
  - Button padding: 1.2rem → 1.4rem
  - All gaps increased by 1.5-2x

#### 🔴 Color System Overhaul
- Primary Color: #00d4ff (Cyan) → #ff1744 (Obsidian Red)
- Secondary Color: #0099cc → #dc004e (Dark Red)
- Accent Color: #9d4edd (Purple) → #ff4569 (Light Red)
- All borders, shadows, and hover effects now use red tints
- Red gradient backgrounds on all titles and CTAs

#### 📊 Full Analysis UI Enhancement
- **New Layout**: Side-by-side comparison grid (2 columns on desktop, 1 on mobile)
- **Sentiment Section**: Left panel with red top border
- **Emotion Section**: Right panel with light red top border
- **Enhanced Styling**:
  - Semi-transparent red background (rgba(255,23,68,0.06))
  - Red border indicators for visual separation
  - Hover animations with lift effect (translateY -5px) and color shift
  - Icon + label headers with underline separators
  - Better padding and readability (2.5rem sections)

#### 🧪 Examples Pool Expansion
- **Size**: 20 → 32 diverse, complex examples
- **Auto-Regeneration**: Examples refresh after each user interaction
- **Categories Enhanced**:
  - Positive: 3 variations (product love, excellent service, best purchase)
  - Negative: 3 variations (service issues, product quality, frustration)
  - Neutral: 3 variations (factual statements, basic operations)
  - Enthusiastic: 3 variations (movie reviews, tech innovation, solutions)
  - Anxious: 3 variations (worries, stress, uncertainty)
  - Other emotions: Grateful, Disappointed, Proud, Confused, Delighted, Mixed, Hopeful, Hurt, Sad, Outraged, Thankful
- **Complexity**: Multi-sentence examples reflecting real-world scenarios
- **Professional Context**: Technical issues, workplace situations, business scenarios

#### 🚀 AI Model Update
- **Previous Model**: mixtral-8x7b-32768 (decommissioned by Groq)
- **Current Model**: llama-3.3-70b-versatile
- **Reason**: Mixtral decommissioned; Llama 3.3 provides excellent speed/quality balance
- **Error Handling**: Added proper error message for decommissioned models

#### 🛠️ Project Cleanup
- **Removed**: 20+ documentation and setup guide files
  - FIX_MODULE_NOT_FOUND.txt
  - GROQ_SETUP_GUIDE.txt
  - INSTALL_FIX_README.txt
  - NUMPY_WINDOWS_FIX.txt
  - PYTHON_3.13_FIXED.txt
  - QUICK_FIX_COMMAND.txt
  - STEP_BY_STEP_RECOVERY.txt
  - VERSION_FIX_GUIDE.txt
  - MODEL_UPDATE.txt
  - And 11+ others
- **Removed**: Duplicate scripts and batch files
  - Multiple install variants
  - Alternative startup scripts
  - Duplicate requirements files (requirements_dev.txt, requirements_prod.txt)
- **Kept Essential**: start_server.ps1, install_requirements.bat, install_direct.bat

#### 📝 Documentation Updates
- **README.md**: Complete rewrite reflecting v2.0
  - New feature descriptions
  - Updated quick start instructions
  - Current model documentation
  - Enhanced troubleshooting section
  - Added "Obsidian Black Theme with Red Accents" UI design guide
  - Python 3.13+ requirement documentation
  - Version history section

### Technical Details

#### Package Versions
- **Flask**: 3.1.3 (Python web framework)
- **Langchain**: 0.3.27 (LLM framework)
- **langchain-community**: 0.3.28
- **langchain-groq**: 0.3.8 (Groq integration)
- **Groq SDK**: 0.30.0
- **Python**: 3.13+ (latest compatibility)

#### CSS Changes
- Updated 100+ style declarations
- New `.full-analysis-wrapper` grid system
- New `.full-analysis-section` styling with hover effects
- Updated color variables in :root
- Enhanced navbar, container, card styling
- Improved responsive breakpoints for mobile

#### JavaScript Changes
- Expanded examplesPool from 20 to 32 items
- Enhanced displayFullAnalysis() with new HTML structure
- Improved formatting for side-by-side display
- Better example regeneration logic

### Bug Fixes
- Fixed "Model decommissioned" error for Mixtral
- Resolved Unicode encoding issues in batch files
- Improved PowerShell script reliability
- Better error handling for invalid API keys

### Performance Improvements
- Optimized CSS for faster rendering
- Improved example loading with client-side generation
- Faster model response times (Llama 3.3 vs Mixtral)

### Removed Features
- Ollama support (switched to cloud-based Groq)
- Old tutorial/setup documentation
- Feature cards section (simplified to tech stack only)
- About section (streamlined UI)

### Breaking Changes
- **Model Change**: Applications using Mixtral 8x7b-32768 must update to llama-3.3-70b-versatile
- **Theme Color Change**: CSS expecting cyan (#00d4ff) colors need updates
- **File Structure**: Removed many support files; ensure you're only using start_server.ps1

### Migration Guide

If upgrading from v1.0:

1. **Update .env file**:
   ```
   # Replace old model
   MODEL_NAME=mixtral-8x7b-32768
   
   # With new model
   MODEL_NAME=llama-3.3-70b-versatile
   ```

2. **Clear browser cache** to see new UI theme (red vs cyan)

3. **Remove old files** if they exist:
   - Delete old startup scripts (keep only start_server.ps1)
   - Delete old documentation files
   - Keep only: app.py, config.py, requirements.txt, .env, templates/, static/

4. **Install fresh**:
   ```bash
   .\start_server.ps1
   ```

### Known Issues
- None identified in v2.0

### Contributors
- UI Design: Complete redesign to obsidian black + red theme
- Model Migration: Mixtral → Llama 3.3 70B
- Examples: Expanded and diversified pool
- Documentation: Comprehensive README update

---

## [1.0.0] - Initial Release

### Features
- Sentiment analysis with Mixtral 8x7B model
- Emotion detection
- Full analysis combining both
- Liquid Glass Obsidian UI with cyan/purple theme
- Flask backend with Groq API integration
- Responsive design
- Keyboard shortcuts (Alt+S/E/F/C)
- Character count display
- 20 example sentences
- Windows PowerShell auto-installer

### Tech Stack
- Flask 3.1.3
- Langchain 0.3.27
- Groq API (Mixtral 8x7b-32768)
- HTML5/CSS3/JavaScript
- Python 3.13+
