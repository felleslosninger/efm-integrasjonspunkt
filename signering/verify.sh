#!/bin/bash
export GNUPGHOME=$(mktemp -d)
chmod 700 "$GNUPGHOME"

gpg --import kodesignering-eformidling-public.key
gpg --verify "$1.asc" "$1"

rm -rf "$GNUPGHOME"
unset GNUPGHOME
