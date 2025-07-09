# download and install the latest gettext version from https://launchpad.net/ubuntu/+source/gettext
# tested with 0.23.1 (from 2025-05-29)

# step 1: create keys.pot file by extracting all strings that should be translated
xgettext-regex src -f i18n.tr -o po/keys.pot
# step 2: use PoEdit to create .po files containing the translations for all languages
# step 3: create the Messages_language.class resource bundle files using the .po files
msgfmt --java2 -d target/classes -r org.codedefenders.i18n.Messages -l de po/de.po

# more information: https://xnap-commons.sourceforge.net/gettext-commons/tutorial.html#Creating_Resource_Bundles
