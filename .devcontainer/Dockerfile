FROM mcr.microsoft.com/devcontainers/java:1-17-bookworm
USER root
RUN apt-get update && apt-get install -y --no-install-recommends unzip curl ca-certificates && rm -rf /var/lib/apt/lists/*
ENV ANDROID_SDK_ROOT=/opt/android-sdk ANDROID_HOME=/opt/android-sdk
ENV PATH="/opt/android-sdk/cmdline-tools/latest/bin:/opt/android-sdk/platform-tools:/opt/gradle-8.11.1/bin:${PATH}"
RUN mkdir -p /opt/android-sdk/cmdline-tools /tmp/sdk && \
    curl -fsSL https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip -o /tmp/sdk.zip && \
    unzip -q /tmp/sdk.zip -d /tmp/sdk && mv /tmp/sdk/cmdline-tools /opt/android-sdk/cmdline-tools/latest && rm -rf /tmp/sdk.zip /tmp/sdk && \
    yes | sdkmanager --licenses >/dev/null && sdkmanager 'platforms;android-35' 'build-tools;35.0.0' 'platform-tools'
RUN curl -fsSL https://services.gradle.org/distributions/gradle-8.11.1-bin.zip -o /tmp/g.zip && unzip -q /tmp/g.zip -d /opt && rm /tmp/g.zip
USER vscode
