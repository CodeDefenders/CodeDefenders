# step 1: create keys.pot file by extracting all strings that should be translated
find src -iname "*.java" -o -iname "*.jsp" | xargs xgettext -k -ktrc -ktr -kmarktr -ktrn:1,2 -kfmt\:message:1 -o po/keys.pot --from-code=utf-8
# step 2: use PoEdit to create .po files containing the translations for all languages
# step 3: create the Messages_language.class resource bundle files using the .po files
msgfmt --java2 -d target/classes -r org.codedefenders.i18n.Messages -l de po/de.po
