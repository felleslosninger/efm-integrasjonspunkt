export GNUPGHOME=$(mktemp -d)
chmod 700 "$GNUPGHOME"

gpg --batch --import kodesignering-eformidling-private.key

gpg --batch --yes \
    --pinentry-mode loopback \
    --passphrase "$GPG_PASSPHRASE" \
    --quick-set-expire AEF27AA6948A3856932AF98ECA5643393753ECE3 5y

gpg --armor --export AEF27AA6948A3856932AF98ECA5643393753ECE3 > $1

rm -rf "$GNUPGHOME"
unset GNUPGHOME
