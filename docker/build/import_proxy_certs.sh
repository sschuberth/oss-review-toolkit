#/!bin/sh

[ -z "$https_proxy" ] && exit

FILE="proxy.crt"
FILE_PREFIX="proxy-"

REGEX_BEGIN="/^-----BEGIN CERTIFICATE-----$/"
REGEX_END="/^-----END CERTIFICATE-----$"

# Pick a server to connect to that is used during the Gradle build, and which reports the proxy's certificate instead of
# its own.
openssl s_client -showcerts -proxy ${https_proxy#*//} -connect jcenter.bintray.com:443 | \
    sed -n "$REGEX_BEGIN,$REGEX_END/p" > $FILE

# Split the potentially multiple certifcates into multiple files to avoid only the first certificate being imported.
csplit -f $FILE_PREFIX -b "%02d.crt" -z $FILE "$REGEX_BEGIN" "{*}"

KEYTOOL=$(realpath $(command -v keytool))
KEYSTORE=$(realpath $(dirname $KEYTOOL)/../lib/security/cacerts)

for CRT_FILE in $FILE_PREFIX*; do
    echo "Adding the following proxy certificate from '$CRT_FILE' to the JRE's certificate store at '$KEYSTORE':"
    cat $CRT_FILE

    $KEYTOOL -importcert -noprompt -trustcacerts -alias $CRT_FILE -file $CRT_FILE -keystore $KEYSTORE -storepass changeit
done

# Also add the proxy certificates to the system certificates, e.g. for curl to work.
echo "Adding proxy certificates to the system certificates..."
cp $FILE_PREFIX* /usr/local/share/ca-certificates/
update-ca-certificates
