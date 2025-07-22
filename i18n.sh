# step 1: create keys.pot file by extracting all strings that should be translated
if [ "$1" = "extract" ]; then
  node ./node_modules/xgettext-regex/bin/xgettext-regex.js src -f i18n.tr -o po/keys.pot
fi

# step 3: create the Messages_language.class resource bundle files using the .po files
if [ "$1" = "compile" ]; then
  for po_file in po/*.po; do
    lang=$(basename "$po_file" .po)
    msgfmt --java2 -d target/classes -r org.codedefenders.i18n.Messages -l "$lang" "$po_file"
  done
fi
