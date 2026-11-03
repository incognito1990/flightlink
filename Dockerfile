# syntax=docker/dockerfile:1.7
#
# Android build toolchain for FlightLink.
#
# The image is built for the host architecture. On arm64 the Android SDK's
# native aapt2 (x86_64) is executed through qemu-user 7.2: newer qemu releases
# break aapt2's argument parsing. No binfmt_misc registration is required.
#
FROM eclipse-temurin:17-jdk-jammy

ENV ANDROID_SDK_ROOT=/opt/android-sdk \
    ANDROID_HOME=/opt/android-sdk \
    DEBIAN_FRONTEND=noninteractive

# Keep version knobs as ARG, not ENV: qemu-user treats a QEMU_VERSION
# environment variable as a request to print its version and exit.
ARG GRADLE_VERSION=8.7
ARG AAPT2_VERSION=8.5.2-11315950
ARG QEMU_VERSION=7.2+dfsg-7+deb12u18+b3
ARG CMDLINE_TOOLS_VERSION=11076708

RUN apt-get update \
    && apt-get install -y --no-install-recommends ca-certificates curl unzip git \
    && rm -rf /var/lib/apt/lists/*

# arm64 only: x86_64 runtime libraries for the Android aapt2 binary.
RUN if [ "$(dpkg --print-architecture)" = "arm64" ]; then \
        rm -f /etc/apt/sources.list.d/*; \
        printf '%s\n' \
          'deb [arch=arm64] http://ports.ubuntu.com/ubuntu-ports jammy main restricted universe multiverse' \
          'deb [arch=arm64] http://ports.ubuntu.com/ubuntu-ports jammy-updates main restricted universe multiverse' \
          'deb [arch=arm64] http://ports.ubuntu.com/ubuntu-ports jammy-security main restricted universe multiverse' \
          'deb [arch=amd64] http://archive.ubuntu.com/ubuntu jammy main restricted universe multiverse' \
          'deb [arch=amd64] http://archive.ubuntu.com/ubuntu jammy-updates main restricted universe multiverse' \
          'deb [arch=amd64] http://archive.ubuntu.com/ubuntu jammy-security main restricted universe multiverse' \
          > /etc/apt/sources.list \
        && dpkg --add-architecture amd64 \
        && apt-get update \
        && apt-get install -y --no-install-recommends \
             libc6:amd64 libstdc++6:amd64 zlib1g:amd64 libgcc-s1:amd64 \
        && ln -sf /lib/x86_64-linux-gnu/ld-linux-x86-64.so.2 /lib64/ld-linux-x86-64.so.2 \
        && rm -rf /var/lib/apt/lists/*; \
    fi

# arm64 only: pinned qemu-user 7.2 (newer releases break aapt2's argument
# parsing). HTTP/1.1 avoids flaky HTTP/2 streams on some networks.
RUN if [ "$(dpkg --print-architecture)" = "arm64" ]; then \
        curl -fSL --http1.1 --retry 5 --retry-delay 3 --connect-timeout 30 \
             -o /tmp/qemu.deb \
             "https://deb.debian.org/debian/pool/main/q/qemu/qemu-user-static_${QEMU_VERSION}_arm64.deb" \
        && dpkg-deb -x /tmp/qemu.deb /tmp/qemu \
        && install -m 0755 /tmp/qemu/usr/bin/qemu-x86_64-static /usr/local/bin/qemu-x86_64-static \
        && rm -rf /tmp/qemu.deb /tmp/qemu; \
    fi

# The exact aapt2 build used by AGP 8.5.2. On arm64 it is wrapped with qemu.
RUN mkdir -p /opt/aapt2 \
    && curl -fSL --http1.1 --retry 5 --retry-delay 3 --connect-timeout 30 -o /tmp/aapt2.jar \
         "https://dl.google.com/dl/android/maven2/com/android/tools/build/aapt2/${AAPT2_VERSION}/aapt2-${AAPT2_VERSION}-linux.jar" \
    && unzip -q /tmp/aapt2.jar -d /opt/aapt2 \
    && chmod +x /opt/aapt2/aapt2 \
    && rm /tmp/aapt2.jar \
    && if [ "$(dpkg --print-architecture)" = "arm64" ]; then \
         mv /opt/aapt2/aapt2 /opt/aapt2/aapt2.bin \
         && printf '#!/bin/sh\nexec /usr/local/bin/qemu-x86_64-static -L / /opt/aapt2/aapt2.bin "$@"\n' > /opt/aapt2/aapt2 \
         && chmod +x /opt/aapt2/aapt2; \
       fi

RUN mkdir -p "$ANDROID_SDK_ROOT/cmdline-tools" \
    && curl -fSL --http1.1 --retry 5 --retry-delay 3 --connect-timeout 30 -o /tmp/cmdline-tools.zip \
         "https://dl.google.com/android/repository/commandlinetools-linux-${CMDLINE_TOOLS_VERSION}_latest.zip" \
    && unzip -q /tmp/cmdline-tools.zip -d "$ANDROID_SDK_ROOT/cmdline-tools" \
    && mv "$ANDROID_SDK_ROOT/cmdline-tools/cmdline-tools" "$ANDROID_SDK_ROOT/cmdline-tools/latest" \
    && rm /tmp/cmdline-tools.zip

ENV PATH="$ANDROID_SDK_ROOT/cmdline-tools/latest/bin:$ANDROID_SDK_ROOT/platform-tools:$PATH"

RUN yes | sdkmanager --licenses >/dev/null || true \
    && sdkmanager --install "platforms;android-34" "build-tools;34.0.0" >/dev/null

# If the build ever invokes the SDK's zipalign, run it through qemu too.
RUN if [ "$(dpkg --print-architecture)" = "arm64" ] \
        && [ -x /opt/android-sdk/build-tools/34.0.0/zipalign ]; then \
         mv /opt/android-sdk/build-tools/34.0.0/zipalign /opt/android-sdk/build-tools/34.0.0/zipalign.bin \
         && printf '#!/bin/sh\nexec /usr/local/bin/qemu-x86_64-static -L / /opt/android-sdk/build-tools/34.0.0/zipalign.bin "$@"\n' > /opt/android-sdk/build-tools/34.0.0/zipalign \
         && chmod +x /opt/android-sdk/build-tools/34.0.0/zipalign; \
       fi

RUN curl -fSL --http1.1 --retry 5 --retry-delay 3 --connect-timeout 30 -o /tmp/gradle.zip \
         "https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip" \
    && unzip -q /tmp/gradle.zip -d /opt \
    && ln -s "/opt/gradle-${GRADLE_VERSION}/bin/gradle" /usr/local/bin/gradle \
    && rm /tmp/gradle.zip

WORKDIR /workspace

CMD ["bash"]
