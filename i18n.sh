if [ "$1" = "extract" ]; then
  # create keys.pot file by extracting all strings that should be translated
  node ./node_modules/@tim-greller/xgettext-regex/bin/xgettext-regex.js src -r "(?<=(?:i18n.trn\\(\\s*(?:(?:\"(?:[^\"]|\\\\.)*\"|'(?:[^']|\\\\.)*')\\s*,\\s*)*)|(?:(?:i18n.tr|I18n.marktr|I18nService.marktrf)\\(\\s*))(([\"'])(?:(?=(\\\\?))\\3.)*?\\2)(?=(?:\\s*,\\s*(?:\"(?:[^\"]|\\\\.)*\"|'(?:[^']|\\\\.)*'|[^\"')]+))*\\s*\\))" -i 1 -o po/keys.pot
elif [ "$1" = "compile" ]; then
  # create the Messages_language.class resource bundle files for all of the .po files
  for po_file in po/*.po; do
    lang=$(basename "$po_file" .po)
    msgfmt --java2 -d target/classes -r org.codedefenders.i18n.Messages -l "$lang" "$po_file"
  done
else
  echo "Unknown parameter: '$1'"
  echo "Usage: $0 [extract|compile]"
  exit 1
fi
