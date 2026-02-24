export GNUPGHOME=$(mktemp -d)
chmod 700 "$GNUPGHOME"

gpg --batch --import kodesignering-eformidling-private.key

gpg --batch --yes \
    --pinentry-mode loopback \
    --passphrase "$GPG_PASSPHRASE" \
    --detach-sign --armor \
    "$1"

rm -rf "$GNUPGHOME"
unset GNUPGHOME
