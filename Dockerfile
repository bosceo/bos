FROM bosprotocol/bos-gradle

RUN set -o errexit -o nounset \
    && echo "git clone" \
    && git clone https://github.com/bosprotocol/java-bos.git \
    && cd java-bos \
    && gradle build

WORKDIR /java-bos

EXPOSE 18888