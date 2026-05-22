# âš¡ FastJava Repository Setup Guide

This guide details the standard, automated procedure for initializing and pushing new FastJava repositories, including programmatic setup of metadata, topics, and automated CI pipelines.

---

## ðŸ› ï¸ Step-by-Step Repository Lifecycle

### 1. Initialize Local Git
Inside your newly created project directory:
```bash
git init
git branch -M main
git add -A
git commit -m "feat: initial commit for FastSpider v0.1.0"
```

### 2. Connect Remote Repository
Once the repository is created on GitHub under `andrestubbe/FastSpider`:
```bash
git remote add origin https://github.com/andrestubbe/FastSpider.git
git push -u origin main
```

---

## ðŸ·ï¸ Programmatic About-Section & Tags (GitHub CLI)

To maintain a consistent, search-optimized presentation across the FastJava suite, always apply the repository description and tags programmatically using the official **GitHub CLI (`gh`)**:

### Set Description & Topics
```bash
gh repo edit andrestubbe/FastSpider \
  --description "High-Performance, Zero-Allocation native Windows XXX API for Java" \
  --add-topic "fastjava,java,zero-allocation,high-performance,windows,tui,terminal"
```

---

## ðŸ¤– Continuous Integration Setup (GitHub Actions)

Every FastJava repository includes automatic testing on push to guarantee API stability.

Create a workflow file in `.github/workflows/maven.yml`:
```yaml
name: Java CI with Maven

on:
  push:
    branches: [ "main" ]
  pull_request:
    branches: [ "main" ]

jobs:
  build:
    runs-on: ubuntu-latest

    steps:
    - uses: actions/checkout@v3
    - name: Set up JDK 17
      uses: actions/setup-java@v3
      with:
        java-version: '17'
        distribution: 'temurin'
        cache: maven
    - name: Build with Maven
      run: mvn -B package --file pom.xml
```

Once pushed, your Build Status badge in `README.md` will dynamically reflect the compilation status:
```markdown
[![Build](https://img.shields.io/github/actions/workflow/status/andrestubbe/FastSpider/maven.yml?branch=main)](https://github.com/andrestubbe/FastSpider/actions)
```

