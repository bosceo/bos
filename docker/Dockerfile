FROM bosprotocol/bos-gradle

RUN set -o errexit -o nounset \
#  Download and build java-bos
    && echo "git clone" \
    && git clone https://github.com/bosprotocol/java-bos.git \
    && cd java-bos \
    && gradle build

# Change work directory
WORKDIR /java-bos

# open port 18888
EXPOSE 18888

