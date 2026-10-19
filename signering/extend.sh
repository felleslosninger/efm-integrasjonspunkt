#!/bin/bash
export GNUPGHOME=$(mktemp -d)
chmod 700 "$GNUPGHOME"

gpg --batch --import kodesignering-eformidling-private.key

gpg --batch --yes \
    --pinentry-mode loopback \
    --passphrase "$GPG_PASSPHRASE" \
    --quick-set-expire AEF27AA6948A3856932AF98ECA5643393753ECE3 5y

# export the new public key
gpg --armor --export AEF27AA6948A3856932AF98ECA5643393753ECE3 > "$1.public.key"

# export the new private key with extended expiry
gpg --batch --yes \
    --pinentry-mode loopback \
    --passphrase "$GPG_PASSPHRASE" \
    --armor --export-secret-keys AEF27AA6948A3856932AF98ECA5643393753ECE3 > "$1.private.key"

rm -rf "$GNUPGHOME"
unset GNUPGHOME
