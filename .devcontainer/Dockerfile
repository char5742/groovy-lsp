FROM eclipse-temurin:21-jdk

# Install basic utilities
RUN apt-get update && apt-get install -y \
    curl \
    zip \
    unzip \
    git \
    && rm -rf /var/lib/apt/lists/*

# Install SDKMAN and Groovy
RUN curl -s "https://get.sdkman.io" | bash
RUN bash -c "source $HOME/.sdkman/bin/sdkman-init.sh && sdk install groovy 4.0.27"

# Install Gradle
RUN curl -L https://services.gradle.org/distributions/gradle-8.11.1-bin.zip -o gradle.zip \
    && unzip gradle.zip -d /opt \
    && rm gradle.zip \
    && ln -s /opt/gradle-8.11.1/bin/gradle /usr/local/bin/gradle

# Set environment variables
ENV JAVA_HOME=/opt/java/openjdk
ENV GROOVY_HOME=/root/.sdkman/candidates/groovy/current
ENV PATH=$PATH:$GROOVY_HOME/bin:/opt/gradle-8.11.1/bin

WORKDIR /workspace