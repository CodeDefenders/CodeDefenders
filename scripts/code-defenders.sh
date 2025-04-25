#!/bin/bash

# TODO add a function to join a running game (maybe leaving it also?) given the id
# TODO Check the "LAST" role query: why that returns the wrong results ?

# General Setting

if [ ! -f credentials.cfg ]; then echo "Missing credentials.cfg file!"; exit 1; fi

. credentials.cfg

: ${DB_USER:?Missing}
: ${DB_PWD:?Missing}
: ${DB_HOST:?Missing}
: ${DB_NAME:?Missing}
#
: ${CODE_DEFENDER_URL:?Missing}

shopt -s expand_aliases

# MAC some utilities have different names and options
if uname -a | grep -c Darwin > /dev/null; then
    alias shuf="gshuf"
    alias split="gsplit"
    DICTIONARY="/usr/share/dict/words"
    date_timestamp='date +"%Y-%m-%d %H:%M:%S"'
    date_timestamp_start='date -v-1d +"%Y-%m-%d %H:%M:%S"'
    date_timestamp_end='date -v+1d +"%Y-%m-%d %H:%M:%S"'
    alias remove_empty_lines="sed -i \".original\" '/^[[:space:]]*$/d'"
else
    DICTIONARY="/usr/share/dict/american-english"
    date_timestamp='date +"%Y-%m-%d %H:%M:%S"'
    date_timestamp_start='date +"%Y-%m-%d %H:%M:%S" -d "-1days"'
    date_timestamp_end='date +"%Y-%m-%d %H:%M:%S" -d "+1days"'
    alias remove_empty_lines="sed -i'.original' '/^[[:space:]]*$/d'"
fi

function __private_generate_password(){
	PWD=""
 	while read -r word; do
 		# Remove Invalid Chars, like '
 		word=$(echo $word | sed "s|'||g")
 		PWD=${PWD}$(__private_capitalize ${word})
	done < <(shuf -n2 ${DICTIONARY})
  	# Truncate Passwords that are too long
  	PWD="${PWD::10}${RANDOM:0:2}"
  	echo ${PWD}
}

function __private_capitalize(){
	local s=$1
	echo $(tr '[:lower:]' '[:upper:]' <<< ${s:0:1})${s:1}
}

# Use the password if any
function register_student(){
	if [ $# -lt 1 ]; then echo "Missing student StupIP id"; return 1; fi

    username=$1
    email=${username}@gw.uni-passau.de
    password=${2:-$(__private_generate_password)}
    command="curl -s --data \"formType=create&username=${username}&email=${email}&password=${password}&confirm=${password}\" ${CODE_DEFENDER_URL}/user && echo \"SUCCESS!\" || echo \"FAILED TO REGISTER ${username} !\""

    echo "Creating ${username}"
    # echo ${command}
    eval ${command}
}

#
# Create a mass registration script to register the users provided in the CSV file
# This also generate passwords
#
function create_registration_script_from_studip_csv(){
	if [ $# -lt 1 ]; then echo "Missing Stud.IP csv file"; return 1; fi

	if [ -f registration.sh ]; then mv -v registration.sh registration.sh.$(date +%s); fi

	while read -r line
	do
		# Extract username from CSV
		username=$(echo ${line}| sed 's|.* \([a-zA-Z][a-zA-Z]*[0-9][0-9][0-9]*\) .*|\1|')
		# Extract email address
		# email=$(echo ${line} |sed 's|.* \([a-zA-Z][a-zA-Z]*[0-9][0-9][0-9]*@[^ ]*\) .*|\1|')
		# Generate Email Address
		email=${username}@gw.uni-passau.de
		# Generate password
		password=$(__private_generate_password)
		# Output the String to register the user. We keep this to remeber the password for them
		echo "echo \"Registering ${username}\"">> registration.sh
		echo "curl -s --data \"formType=create&username=${username}&email=${email}&password=${password}&confirm=${password}\" ${CODE_DEFENDER_URL}/login && echo \"SUCCESS!\" || echo \"FAILED TO REGISTER ${username} !\"" >> registration.sh
		echo "echo \"\"" >> registration.sh
	done < $1
}

function __private_query_db(){
	query=$1
	## TODO Sanitize the query
	. credentials.cfg
	mysql -u${DB_USER} -p${DB_PWD} -h${DB_HOST} -ss -N -e "SET SESSION sql_mode = 'STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_AUTO_CREATE_USER,NO_ENGINE_SUBSTITUTION'; ${query}" ${DB_NAME} \
	  2>/dev/null
}
## What's the difference ?
function __private_execute_sql(){
	if [ $# -lt 1 ]; then echo "Missing SQL script"; return 1; fi
	sql=$1
	if [ -z ${sql} ]; then echo "Missing SQL script"; return 1; fi
	## TODO Sanitize the query
	. credentials.cfg
	mysql -u${DB_USER} -p${DB_PWD} -h${DB_HOST} ${DB_NAME} < ${sql}
}

## Use alias instead of class name
function get_cut_for(){
  echo $(__private_query_db "select Class_ID from classes where Alias='$1';")
}

# Can we make the game creation parametric on the "Last ID of the game"?
#
# Create a set of game scripts based on user ranking and last role
# chose students, create a game by querying the DB directly while creating the game and create the script to finish the game !
function create_balanced_game_scripts(){
 	if [ $# -lt 1 ]; then echo "Provide a list of code-defenders usernames"; return 1; fi
    local participants=$1
    echo "Got $(cat ${participants} | wc -l)"

    # remove empty lines and backup the original version to
    # This create a ${participants}.original file
    remove_empty_lines ${participants}

    mv ${participants} ${participants}.sanitized
    mv ${participants}.original ${participants} # Switch back the original version of the file !

    # use the sanitized version now on
    participants=${participants}.sanitized
    echo "Got $(cat ${participants} | wc -l)"
    # Check that the file is valid
    if [ $(awk '{print NF}' ${participants} | sort -nu | tail -n 1) -gt 1 ]; then
        echo "File ${participants} is invalid. The file must have only one column, which contains user ID only!"
        return 1;
    fi

	if [ $# -lt 2 ]; then echo "Provide Size of Game. 4 for a 2 vs 2, 6 for a 3 vs 3"; return 1; fi
	    local gameSize=$2
	if [ $# -lt 3 ]; then echo "Provide CUT ALIAS Name for the Games. eg. Lift"; return 1; fi
	    local cutName=$3
	if [ $# -gt 3 ]; then
		LEVEL=$4
	else
		LEVEL="HARD"
	fi
	# TODO: Sanitize input?
	#Optional parameter LEVEL. EASY/HARD
	#LEVEL=${$4:-HARD}

	# Get bot ID or register bot user
	if [ $(__private_query_db "select count(*) from users where Username='bot';") -eq 0 ]; then
		 $(register_student "bot" "iRobot") 2>&1 > /dev/null
    fi

	# Creator ID
	CREATOR_ID=$(__private_query_db "select User_ID from users where Username='bot';")

	## TODO Check that exists or exit with erro
	CUT_ID=$(get_cut_for ${cutName})

	TIMESTAMP=$(eval ${date_timestamp})
	START_TIMESTAMP=$(eval ${date_timestamp_start})
	END_TIMESTAMP=$(eval ${date_timestamp_end})

# SELECT ALL THE GAMES FOR AN USER, i.e., "settik01"
# SELECT Username, U.User_ID, GP.Role, GP.Start_Time from users U left join (select P.User_ID as User_ID, G.Start_Time as Start_Time, P.Role as Role from games G left join players P on G.ID=P.Game_ID ) GP on U.User_ID=GP.User_ID WHERE U.Username='settik01';

# Last Role Played

# Order ALL students by Defender score only
__private_query_db "SELECT U.username AS username, IFNULL(DScore,0) AS Score FROM users U LEFT JOIN ( SELECT PA.user_id, sum(M.Points) as AScore  FROM players PA LEFT JOIN mutants M on PA.id = M.Player_ID GROUP BY PA.user_id ) AS Attacker ON U.user_id = Attacker.user_id LEFT JOIN ( SELECT PD.user_id, sum(T.Points) as DScore FROM players PD LEFT JOIN tests T on PD.id = T.Player_ID GROUP BY PD.user_id ) AS Defender ON U.user_id = Defender.user_id WHERE U.user_id > 2 ORDER BY Score DESC;" > sorted-all-participants

# Filter on students in class
grep -f ${participants} sorted-all-participants > sorted-participants

# List ALL students with their last played role, if any
__private_query_db "SELECT Username, Role FROM users INNER JOIN players ON users.User_ID = players.User_ID INNER JOIN games ON players.Game_ID = games.ID INNER JOIN (SELECT players.User_ID, max(players.Game_ID) AS latestGame FROM players GROUP BY players.User_ID) AS lg ON lg.User_ID = players.User_ID AND lg.latestGame = games.ID;" > last-role-all-participants

#__private_query_db "SELECT Username, GP.Role from users U left join (select P.User_ID as User_ID, MAX(G.Start_Time) as Start_Time, P.Role as Role from games G left join players P on G.ID=P.Game_ID GROUP BY P.User_ID) GP on U.User_ID=GP.User_ID WHERE U.User_ID>8;" > last-role-all-participants
# Filter on students in class
grep -f ${participants} last-role-all-participants > last-role-participants

wc -l last-role-participants

# Switch ROLE bases on previous role
cat last-role-participants | grep "DEFENDER" | awk '{print $1}' > next_attackers
cat last-role-participants | grep "ATTACKER" | awk '{print $1}'> next_defenders

# Add the score information
grep -f next_attackers sorted-participants > sorted_attackers
# Add the score information
grep -f next_defenders sorted-participants > sorted_defenders

# If students never played they go by default in the last class (0 score means at the very bottom), split in half at random
shuf last-role-participants | grep "NULL" | awk '{print $1, 0}'> next_random

awk 'NR%2' next_random >> sorted_defenders
awk 'NR%2==0' next_random >> sorted_attackers

# Add the default score to random information



# Add Role information to files
awk 'NR==1 {v="ATTACKER"}{print $0,v}' sorted_attackers > tmp && mv tmp sorted_attackers
# Add Role information
awk 'NR==1 {v="DEFENDER"}{print $0,v}' sorted_defenders > tmp && mv tmp sorted_defenders

echo "ATTACKERS = $(cat sorted_attackers | wc -l)"
echo "DEFENDERS = $(cat sorted_defenders | wc -l)"

# Re-SORT files before shuffling
sort -nk 2,2 -r sorted_attackers -o sorted_attackers
sort -nk 2,2 -r sorted_defenders -o sorted_defenders

# We split in 4 categories, shuffle and merge them back again. This avoids (to some extend) creating the same games over and over.
# High N_GROUPS -> high chance to recreate the game with same students over and over
# Low N_GROUPS -> more diverse teams, less likely to form teams/games that are balanced
N_GROUPS=6

# Split the students in N_GROUPS (or N_GROUPS+1 if rounded) - Each category contains the students with "similar" rank
## This creates category.01 category.02 category.03 ...
# Guarantee at least 1 split. Avoids divide by zero
SPLIT=$((`wc -l < sorted_attackers`/${N_GROUPS}))
if [ ${SPLIT} -lt 1 ]; then SPLIT=1; fi
split -l${SPLIT} sorted_attackers attack -da 2
# Guarantee at least 1 split. Avoids divide by zero
SPLIT=$((`wc -l < sorted_defenders`/${N_GROUPS}))
if [ ${SPLIT} -lt 1 ]; then SPLIT=1; fi
split -l${SPLIT} sorted_defenders defend -da 2

# Shuffle per category and append to file
set +e
mv sorted_attackers sorted_attackers.debug
mv sorted_defenders sorted_defenders.debug
set -e

find . -iname "attack*" -exec basename {} \; | sort | \
	while read -r FILE; do
		shuf ${FILE} >> sorted_attackers
	done

find . -iname "defend*" -exec basename {} \; | sort | \
	while read -r FILE; do
		shuf ${FILE} >> sorted_defenders
	done

# Merge the files back together line by line
paste -d"\n" sorted_attackers sorted_defenders | sed '/^$/d' > participants.tmp

# FINALLY CREATE THE FILES FOR GAME SETUP

# GET THE LAST ID
# Define game ID. We start from what's already in the DB
ID=$(__private_query_db 'select Max(ID) from games;')
if [ "${ID}" == "NULL" ]; then ID=0; fi

# echo "DEBUG: Last GAME ID is: ${ID}"

lineNumber=0

CREATE_FILE=""
# We read the role from the file. The last game(s) will have more attackers than defenders or vice-versa
while read -r username score role; do
	Game_ID=$((${lineNumber}/${gameSize}))
    Game_ID=$((${Game_ID}+$((${ID}+1))))


    if [ -z "${score}" ]; then score=0; fi

    # Every X times setup a new game
	if [ "$((${lineNumber}%${gameSize}))" == "0" ]; then

		if [ "${CREATE_FILE}" != "" ]; then
 			echo "UNLOCK TABLES;" >> ${CREATE_FILE}
			# DEBUG echo "Closing ${CREATE_FILE}"
			echo ""
		fi

    	CREATE_FILE="create_game_${Game_ID}.sql"
    	END_FILE="finish_game_${Game_ID}.sql"

    	echo "Creating Game ${Game_ID}"

 # $((${gameSize}/2)),$((${gameSize}/2)) --> 0, 0
    	cat > ${CREATE_FILE} << EOL
USE ${DB_NAME};
LOCK TABLES games WRITE;
INSERT INTO games (ID, Class_ID, Level, Timestamp, Creator_ID, Prize, Defender_Value, Attacker_Value, Coverage_Goal, Mutant_Goal, Attackers_Needed, Defenders_Needed, Start_Time, Finish_Time, Attackers_Limit, Defenders_Limit, State, CurrentRound, FinalRound, ActiveRole, Mode, RequiresValidation, IsAIDummyGame) \
VALUES (${Game_ID},${CUT_ID},'${LEVEL}','${TIMESTAMP}',${CREATOR_ID},1,100,100,1.1,1.1,0,0,'${START_TIMESTAMP}','${END_TIMESTAMP}',0,0,'ACTIVE',1,5,'ATTACKER','PARTY',0,0);
UNLOCK TABLES;

LOCK TABLES players WRITE;
EOL

		cat > ${END_FILE} << EOL
USE ${DB_NAME};
UPDATE games set State='FINISHED' WHERE ID=${Game_ID};

EOL

	fi

    User_ID=$(__private_query_db "select User_ID from users where Username='${username}';")
    echo "-- ${username} - ID:${User_ID} with score: ${score} plays as ${role} in game ${Game_ID}" | tee -a ${CREATE_FILE}

    # We omit ID of player relation. Mysql will provide a new one
	echo "INSERT INTO players (User_ID, Game_ID, Points, Role, Active) VALUES (${User_ID},${Game_ID},0,'${role}',1);" >> ${CREATE_FILE}

    # Increment
  	lineNumber=$((lineNumber+1))
done< <( cat participants.tmp ) # | sed 's|.* \([a-zA-Z][a-zA-Z]*[0-9][0-9][0-9]*\) .*|\1|')

### Close the last file
echo "UNLOCK TABLES;" >> ${CREATE_FILE}

if [ "$(cat sorted_attackers | wc -l)" -ne "$(cat sorted_defenders | wc -l)" ]; then
	echo ""
	echo "WARN: Check the last game(s) !!"
	echo ""
	echo "ATTACKERS = $(cat sorted_attackers | wc -l)"
	echo "DEFENDERS = $(cat sorted_defenders | wc -l)"

fi

# Clean up TMP Files
set +e
rm participants.tmp
rm sorted-all-participants
rm sorted-participants
rm sorted_attackers
rm sorted_defenders
rm last-role-all-participants
rm last-role-participants
rm next_attackers
rm next_defenders
rm next_random
rm attack*
rm defend*
rm sorted_attackers.debug
rm sorted_defenders.debug
set -e
}


# TODO: THis does not really work if students are allowed to
# Create all the scripts to create the games given a list of participants.
# The class under test be updated manually
#
function create_game_and_revenge_game_scripts(){

	if [ $# -lt 1 ]; then echo "Provide List of Users"; return 1; fi
	local participants=$1
	if [ $# -lt 2 ]; then echo "Provide Size of Game. 4 for a 2 vs 2"; return 1; fi
	local gameSize=$2
	if [ $# -lt 3 ]; then echo "Provide CUT Name for the Games."; return 1; fi
	local cutName=$3

	# Creator ID
	CREATOR_ID=$(__private_query_db "select User_ID from users where Username='bot';")
	# TODO Check this is there !
	# CREATOR_ID=1; # Use default CREATOR_ID from AI_ATTACKER_INACCESSIBLE
	CUT_ID=$(get_cut_for ${cutName})

	TIMESTAMP=$(eval ${date_timestamp})
	START_TIMESTAMP=$(eval ${date_timestamp_start})
	END_TIMESTAMP=$(eval ${date_timestamp_end})

	# We create size users mod size users number of GAMES
	nGames=$(( $(cat ${participants} | wc -l) / ${gameSize} ))
	outsiders=$(( $(cat ${participants} | wc -l) % ${gameSize} ))

	if [ ${outsiders} -gt 0 ]; then
		echo "Add last game for the outsiders"
		nGames=$((${nGames}+1))
	fi

	# Define game ID. We start from what's already in the DB
	ID=$(__private_query_db 'select Max(ID) from games;')
	if [ "${ID}" == "NULL" ]; then ID=0; fi

	# Ideally we need twice the games to account for reverse roles
	for i in $(seq $((1+${ID})) $((${nGames}+${ID})))
	do
		echo "Creating Scripts for Game $i";

		# Create and Start Game - We give the ID
		CREATE_FILE="create_game_${i}.sql"
		END_FILE="finish_game_${i}.sql"

cat > ${CREATE_FILE} << EOL
USE ${DB_NAME};
LOCK TABLES games WRITE;
INSERT INTO games (ID, Class_ID, Level, Timestamp, Creator_ID, Prize, Defender_Value, Attacker_Value, Coverage_Goal, Mutant_Goal, Attackers_Needed, Defenders_Needed, Start_Time, Finish_Time, Attackers_Limit, Defenders_Limit, State, CurrentRound, FinalRound, ActiveRole, Mode, RequiresValidation, IsAIDummyGame) \
VALUES (${i},${CUT_ID},'HARD','${TIMESTAMP}',${CREATOR_ID},1,100,100,1.1,1.1,0,0,'${START_TIMESTAMP}','${END_TIMESTAMP}',$((${gameSize}/2)),$((${gameSize}/2)),'ACTIVE',1,5,'ATTACKER','PARTY',0,0);
UNLOCK TABLES;
EOL

# Close Game - the ID before
cat > ${END_FILE} << EOL
USE ${DB_NAME};
UPDATE games set State='FINISHED' WHERE ID=${i};
EOL

        ####################################
        # Create also the Revenge Game
        ####################################
        revengeID=$(($(($i+${nGames}))+1))
        echo "Creating Scripts for (Revenge) Game ${revengeID}";

		# Create and Start Game - We give the ID
		CREATE_FILE="create_game_${revengeID}.sql"
		END_FILE="finish_game_${revengeID}.sql"

cat > ${CREATE_FILE} << EOL
USE ${DB_NAME};
LOCK TABLES games WRITE;
INSERT INTO games (ID, Class_ID, Level, Timestamp, Creator_ID, Prize, Defender_Value, Attacker_Value, Coverage_Goal, Mutant_Goal, Attackers_Needed, Defenders_Needed, Start_Time, Finish_Time, Attackers_Limit, Defenders_Limit, State, CurrentRound, FinalRound, ActiveRole, Mode, RequiresValidation, IsAIDummyGame) \
VALUES (${revengeID},${CUT_ID},'HARD','${TIMESTAMP}',${CREATOR_ID},1,100,100,1.1,1.1,0,0,'${START_TIMESTAMP}','${END_TIMESTAMP}',$((${gameSize}/2)),$((${gameSize}/2)),'ACTIVE',1,5,'ATTACKER','PARTY',0,0);
UNLOCK TABLES;
EOL

# Close Game - the ID before
cat > ${END_FILE} << EOL
USE ${DB_NAME};
UPDATE games set State='FINISHED' WHERE ID=${revengeID};
EOL

	done


shuffledParticipants=${participants}.shuffled
echo "Shuffling to ${shuffledParticipants}"
shuf ${participants} > ${shuffledParticipants}

# Associate Players to Games
# https://stackoverflow.com/questions/21309020/remove-odd-or-even-lines-from-a-text-file
lineNumber=0;
CREATE_FILE=""
while read -r username
do
	Game_ID=$((${lineNumber}/${gameSize}))
    Game_ID=$((${Game_ID}+$((${ID}+1))))

	if [ "$((${lineNumber}%${gameSize}))" == "0" ]; then
		if [ "${CREATE_FILE}" != "" ]; then
			echo "Closing ${CREATE_FILE}"
 			echo "UNLOCK TABLES;" >> ${CREATE_FILE}
		fi
    	CREATE_FILE="create_game_${Game_ID}.sql"
    	echo "LOCK TABLES players WRITE;" >> ${CREATE_FILE}
	fi

	if [ "$((${lineNumber} % 2))" == "0" ]; then
		role="ATTACKER"
	else
		role="DEFENDER"
    fi
    User_ID=$(__private_query_db "select User_ID from users where Username='${username}';")
    echo "DEBUG ${lineNumber} - ${username} - ${User_ID} - ${role} - ${Game_ID}"
    # We omit ID of player relation in the hope mysql provides a new one
	echo "INSERT INTO players (User_ID, Game_ID, Points, Role, Active) VALUES (${User_ID},${Game_ID},0,'${role}',1);" >> ${CREATE_FILE}
    # Increment
  	lineNumber=$((lineNumber+1))
done< <( cat ${shuffledParticipants} | \
	sed 's|.* \([a-zA-Z][a-zA-Z]*[0-9][0-9][0-9]*\) .*|\1|')

# Associate Players to (Revenge) Games
	lineNumber=0;
CREATE_FILE=""
while read -r username
do
	Game_ID=$((${lineNumber}/${gameSize}))
    Game_ID=$((${Game_ID}+$((${ID}+1))))
    ### NOTE THIS FOR REVENGE GAMES
    Game_ID=$(($((${Game_ID}+${nGames}))+1))

	if [ "$((${lineNumber}%${gameSize}))" == "0" ]; then
		if [ "${CREATE_FILE}" != "" ]; then
			echo "Closing (Revenge) Game ${CREATE_FILE}"
 			echo "UNLOCK TABLES;" >> ${CREATE_FILE}
		fi
    	CREATE_FILE="create_game_${Game_ID}.sql"
    	echo "LOCK TABLES players WRITE;" >> ${CREATE_FILE}
	fi

	if [ "$((${lineNumber} % 2))" == "0" ]; then
		role="DEFENDER"
	else
		role="ATTACKER"
    fi
    User_ID=$(__private_query_db "select User_ID from users where Username='${username}';")
    echo "DEBUG ${lineNumber} - ${username} - ${User_ID} - ${role} - ${Game_ID}"
    # We omit ID of player relation in the hope mysql provides a new one
	echo "INSERT INTO players (User_ID, Game_ID, Points, Role, Active) VALUES (${User_ID},${Game_ID},0,'${role}',1);" >> ${CREATE_FILE}
    # Increment
  	lineNumber=$((lineNumber+1))
done< <(cat ${shuffledParticipants} | \
	sed 's|.* \([a-zA-Z][a-zA-Z]*[0-9][0-9][0-9]*\) .*|\1|')
}

function start_game(){
	if [ "$#" -ne "1" ]; then echo "Missing Game ID"; return 1; fi
	ID=$1
	echo "Starting game ${ID}"
	__private_execute_sql $(find . -iname create_game_${ID}.sql -exec basename {} \;)
}

function start_games(){
	if [ "$#" -ne "2" ]; then echo "Missing Games ID-ID"; return 1; fi
	for ID in $(seq $1 $2)
	do
	 start_game ${ID}
   done
}

function end_game(){
	if [ "$#" -ne "1" ]; then echo "Missing Game ID"; return 1; fi
	ID=$1
	echo "Ending  game ${ID}"
	__private_execute_sql $(find . -iname "finish_game_${ID}.sql" -exec basename {} \;)
}

function end_games(){
	if [ "$#" -ne "2" ]; then echo "Missing Game ID"; return 1; fi
	for ID in $(seq $1 $2)
	do
	  end_game ${ID}
    done
}

# Delete all the sql files
function clean_up(){
  rm -v finish_game_*.sql
  rm -v create_game_*.sql
}

# TODO There's a better way to list the functions here than manually list them
function help(){
	echo "COMMANDS"
	cat code-defenders.sh | grep function | grep -v "__private" | grep -v "\#" | sed -e '/^ /d' -e 's|function \(.*\)(){|\1|g'
}


# Invoke functions by name
if declare -f "$1" > /dev/null
then
    # call arguments verbatim
    "$@"
else
    # Show a helpful error
    echo "'$1' is not a known function name" >&2
    help
    #  exit 1
fi
