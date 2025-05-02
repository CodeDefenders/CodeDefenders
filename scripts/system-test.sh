#!/bin/bash

HERE=$(pwd)

if [ ! -f credentials.cfg-system-tests ]; then echo "Missing credentials.cfg-system-tests file!"; exit 1; fi

. credentials.cfg-system-tests

# Check all the data are there
: ${CD_HOME:?Missing} # "/scratch/defender"
: ${SYSTEM_TESTS_HOME:?Missing} #="/scratch/defender/system-tests"
#
: ${DB_USER:?Missing}
: ${DB_PWD:?Missing}
: ${DB_HOST:?Missing}
: ${DB_NAME?Missing}
#
: ${SOURCE_DB_USER:?Missing}
: ${SOURCE_DB_PWD:?Missing}
: ${SOURCE_DB_HOST:?Missing}
#
: ${CODE_DEFENDER_URL:?Missing}

# users.list contains usernames and password of the syntethic clients
if [ ! -f users.list ]; then echo "Missing users.list file !;" exit 1; fi

shopt -s expand_aliases

# Under MAC some utilities have different name. This requires that utils is installed on Mac. Use brew.
if uname -a | grep -c Darwin > /dev/null; then
 alias shuf="gshuf"
 alias split="gsplit"
 DICTIONARY="/usr/share/dict/words"
 date_timestamp='date +"%Y-%m-%d %H:%M:%S"'
 date_timestamp_start='date -v-1d +"%Y-%m-%d %H:%M:%S"'
 date_timestamp_end='date -v+1d +"%Y-%m-%d %H:%M:%S"'
else
 DICTIONARY="/usr/share/dict/american-english"
 date_timestamp='date +"%Y-%m-%d %H:%M:%S"'
 date_timestamp_start='date +"%Y-%m-%d %H:%M:%S" -d "-1days"'
 date_timestamp_end='date +"%Y-%m-%d %H:%M:%S" -d "+1days"'
fi

# Execute a query
function __private_query_source_db(){
	#(>&2 echo "__private_query_source_db EXECUTE")
	query=$1
	## TODO Sanitize the query
	. credentials.cfg-system-tests
	mysql -u${SOURCE_DB_USER} -p${SOURCE_DB_PWD} -h${SOURCE_DB_HOST} \
		-ss -N \
		-e "SET SESSION sql_mode = 'STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_AUTO_CREATE_USER,NO_ENGINE_SUBSTITUTION'; ${query}" \
		${SOURCE_DB_NAME} \
	 	2>/dev/null
}
# Execute a SQL script
function __private_execute_sql(){
	if [ $# -lt 1 ]; then echo "Missing SQL script"; return 1; fi
	sql=$1
	if [ -z ${sql} ]; then echo "Missing SQL script"; return 1; fi
	## TODO Sanitize the query
	. credentials.cfg-system-tests
	mysql -u${DB_USER} -p${DB_PWD} -h${DB_HOST} ${DB_NAME} < ${sql} 2>/dev/null
}

function __private_query_db(){
#	(>&2 echo "__private_query_db")
	query=$1
	## TODO Sanitize the query
	. credentials.cfg-system-tests
	mysql -u${DB_USER} -p${DB_PWD} -h${DB_HOST} \
		-ss -N \
		-e "SET SESSION sql_mode = 'STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_AUTO_CREATE_USER,NO_ENGINE_SUBSTITUTION'; ${query}" \
		${DB_NAME} \
	 	2>/dev/null
}


### TODO: MOVE CLIENT FUNCTIONALITIES IN a separate client.sh file ?
function __private_do_login(){
 local username=$1
 local password=$2
 local cookie=$3

#echo "Login as ${username}"
curl --data "formType=login&username=${username}&password=${password}" \
--cookie "${cookie}" --cookie-jar "${cookie}" \
-o /dev/null \
-s ${CODE_DEFENDER_URL}/login
}

function __private_do_join_game(){
	local gameId=$1
	local cookie=$2

#echo "Join game ${gameId}"

curl -X GET \
--cookie "${cookie}" --cookie-jar "${cookie}" \
-o /dev/null \
-s ${CODE_DEFENDER_URL}/multiplayergame?gameId=${gameId}

}

function __private_do_attack() {
	local gameId=$1
	local mutant=$2
	local cookie=$3

#echo "Game ${gameId}: Attack with ${mutant}"

  # This return the total time to complete the request
curl -X POST \
--data "formType=createMutant&gameId=${gameId}" \
--data-urlencode mutant@${mutant} \
--cookie "${cookie}" --cookie-jar "${cookie}" \
-w @curl-format.txt \
-s ${CODE_DEFENDER_URL}/multiplayergame


}

function __private_do_defend() {
	local gameId=$1
	local test=$2
	local cookie=$3

#echo "Game ${gameId}: Defend with "${test}
# This return the total time to complete the request
curl -X POST \
--data "formType=createTest&gameId=${gameId}" \
--data-urlencode test@${test} \
--cookie "${cookie}" --cookie-jar "${cookie}" \
-w @curl-format.txt \
-s ${CODE_DEFENDER_URL}/multiplayergame
}

# This runs in background and produce a log which says for each request how long it was the total response time
function __private_run_user_on_game(){
	local userId=$1
	local gameId=$2
	local folder="${SYSTEM_TESTS_HOME}/${gameId}/${userId}"
	local logFile="${SYSTEM_TESTS_HOME}/${gameId}/${userId}/requests.log"

	local role=$(head -1 ${folder}/role)

	# Do login
	__private_do_login \
		$(cat ${folder}/credentials | awk '{print $1}') \
		$(cat ${folder}/credentials | awk '{print $2}') \
		${folder}/cookie.txt

	__private_do_join_game \
		${gameId} \
		${folder}/cookie.txt

# Do invocations on game X. Notw that n events and n mutants/tests must be the same
local i=0;
local tot=$(cat ${folder}/timeline | wc -l)

while read -r timestamp <&3 && read -r codeFile <&4; do
	((i++))
	# Start with the sleep so we do not have a hit of requests at the very beginning
	echo "At " $(date) " -- In game ${gameId} ${userId} thinks for ${timestamp} sec"
	sleep ${timestamp}

	if [ ${role} == "ATTACKER" ]; then
		echo "At " $(date) " -- In game ${gameId} ${userId} attacks using ${codeFile}" | tee -a ${logFile}
		local totTime=$(__private_do_attack \
			${gameId} \
			${codeFile} \
			${folder}/cookie.txt)

		echo "At " $(date) " -- In game ${gameId} ${userId} attack ${i}/${tot} using ${codeFile} ends. It took ${totTime} secs" | tee -a ${logFile}

	else
		echo "At " $(date) " -- In game ${gameId} ${userId} defends using ${codeFile}" | tee -a ${logFile}

		local totTime=$(__private_do_defend \
			${gameId} \
			${codeFile} \
			${folder}/cookie.txt)

		echo "At " $(date) " -- In game ${gameId} ${userId} defense ${i}/${tot} using ${codeFile} ends.It took ${totTime} secs" | tee -a ${logFile}
	fi

done 3<${folder}/timeline 4< <(find ${folder} -iname "*.java" | sort)
}


function __private_create_timeline(){
	local nUserEvents=$1
	local userFolder=$2
	local timeline=${userFolder}/timeline

	# Clear timeline if any
	> ${timeline}
	for i in $(seq 1 ${nUserEvents}); do
		shuf -i 1-5 -n 1 >> ${timeline}
	done
}

function __private_create_mutants(){
	local nUserEvents=$1
	local userFolder=$2
	local classAlias=$3

	local i=0
	while read -r codeFile; do
		printf -v mutantId '%03d' ${i}
#		echo "Mutant: "
		cp ${codeFile} ${userFolder}/mutant_${mutantId}.java
		i=$((${i}+1))
	done< <(__private_query_source_db "\
	SELECT M.JavaFile FROM \
		mutants M LEFT JOIN games G ON M.Game_ID=G.ID \
		WHERE G.ID IN (\
			SELECT G1.ID \
			FROM games as G1 LEFT JOIN classes C ON G1.Class_ID=C.Class_ID \
			  WHERE C.alias='${classAlias}'\
		) \
		ORDER BY RAND() \
		LIMIT ${nUserEvents};")
}

function __private_create_tests(){
	local nUserEvents=$1
	local userFolder=$2
	local classAlias=$3

	local i=0
	while read -r codeFile; do
		printf -v testId '%03d' ${i}
		# echo "Test: "
		cp  ${codeFile} ${userFolder}/test_${testId}.java
		i=$((${i}+1))
	done< <(__private_query_source_db "\
	SELECT T.JavaFile FROM \
		tests T LEFT JOIN games G ON T.Game_ID=G.ID \
		WHERE G.ID IN (\
			SELECT G1.ID \
			FROM games as G1 LEFT JOIN classes C ON G1.Class_ID=C.Class_ID \
			  WHERE C.alias='${classAlias}'\
		) \
		ORDER BY RAND() \
		LIMIT ${nUserEvents};")
}


# TODO: Implement the replay of a real  game given it's ID. Note that this might require also to speed-up a bit the users

# In any case, we do not replicate equivalence challenge and  checking, so the final score using systems
# 	test might not be the same as the original game

# RANDOM GAME
function create_random_game(){
	local gameSize=$1
	local classAlias=$2
	local nUserEvents=${3:-10}

	# Define game ID. We start from what's already in the DB
	local gameId=$(__private_query_db 'select Max(ID) from games;')
	if [ "${gameId}" == "NULL" ]; then gameId=0; fi
	gameId=$((${gameId}+1))

	# Create Game Folder
	local gameFolder="${SYSTEM_TESTS_HOME}/${gameId}"
	#
	while [ -e ${gameFolder} ]; do

		(>&2 echo "${gameFolder} exists. Try next one")

		gameId=$((${gameId}+1));
		gameFolder="${SYSTEM_TESTS_HOME}/${gameId}";
	done
	mkdir ${gameFolder}

	local cutId=$(__private_query_db "SELECT Class_ID FROM classes WHERE Alias='${classAlias}';")
	local TIMESTAMP=$(eval ${date_timestamp})
	local START_TIMESTAMP=$(eval ${date_timestamp_start})
	local END_TIMESTAMP=$(eval ${date_timestamp_end})
	local CREATOR_ID=$(__private_query_db "SELECT User_ID FROM users WHERE Username='bot';")

	cat > ${gameFolder}/create_and_start_game.sql << EOL
USE ${DB_NAME};
LOCK TABLES games WRITE;
INSERT INTO games (ID, Class_ID, Level, Timestamp, Creator_ID, Prize, Defender_Value, Attacker_Value, Coverage_Goal, Mutant_Goal, Attackers_Needed, Defenders_Needed, Start_Time, Finish_Time, Attackers_Limit, Defenders_Limit, State, CurrentRound, FinalRound, ActiveRole, Mode, RequiresValidation, IsAIDummyGame) \
VALUES (${gameId},${cutId},'HARD','${TIMESTAMP}',${CREATOR_ID},1,100,100,1.1,1.1,0,0,'${START_TIMESTAMP}','${END_TIMESTAMP}',0,0,'ACTIVE',1,5,'ATTACKER','PARTY',0,0);
UNLOCK TABLES;

LOCK TABLES players WRITE;

EOL

cat > ${gameFolder}/close_game.sql << EOL
USE ${DB_NAME};
UPDATE games set State='FINISHED' WHERE ID=${gameId};
EOL

	# Create Users Folders: WHERE TO PICK THE CREDENTIALS FROM ?!
	local role="ATTACKER";
	while read -r line; do
		##
#		echo "Processing ${line}"
		username=$(echo ${line} | awk '{print $1}')
	 	password=$(echo ${line} | awk '{print $2}')

		local userFolder="${gameFolder}/${username}"
		# Create user folder
		mkdir ${userFolder}

		echo ${role} >> ${userFolder}/role

		echo ${line} >> ${userFolder}/credentials

		# Create a timeline of nUserEvents events
		__private_create_timeline \
			${nUserEvents} \
			${userFolder}

		local userId=$(__private_query_db "SELECT User_ID FROM users WHERE Username='${username}';")
			# Add to game file
			echo "-- ${username} with id ${userId} plays as ${role} in game ${gameId}" >> ${gameFolder}/create_and_start_game.sql

    		# We omit ID of player relation. Mysql will provide a new one
			echo "INSERT INTO players (User_ID, Game_ID, Points, Role, Active) VALUES (${userId},${gameId},0,'${role}',1);" >> ${gameFolder}/create_and_start_game.sql


		# Create files and Alternate roles
		if [ ${role} == "ATTACKER" ]; then

			__private_create_mutants \
				${nUserEvents} \
				${userFolder} \
				${classAlias}

			role="DEFENDER";
		else
			__private_create_tests \
				${nUserEvents} \
				${userFolder} \
				${classAlias}

			role="ATTACKER";
		fi

	done< <(shuf -n ${gameSize} users.list)

	echo "UNLOCK TABLES;" >> ${gameFolder}/create_and_start_game.sql

	echo "${gameId}"
}


function replay_game(){
	# Open the game - TODO probably we should check if the game is started already ?
	local gameId=$1
	local gameFolder="${SYSTEM_TESTS_HOME}/${gameId}"
	local logFile="${SYSTEM_TESTS_HOME}/${gameId}/game.log"
  (  # opening the subshell

	echo "At "$(date)" -- Starting the game ${gameId}"
	__private_execute_sql ${gameFolder}/create_and_start_game.sql

	# Get the users and run the in background
	while read -r username; do
	 echo "Starting user ${username}"
    	__private_run_user_on_game ${username} ${gameId} &
	done< <(find ${gameFolder} -maxdepth 1 -type d -not -ipath ${gameFolder} -exec basename {} \;)
#	done< <(find ${gameFolder} -type d -depth 1 -exec basename {} \;)

	wait

	echo "At "$(date)" -- Closing the game ${gameId}"
	__private_execute_sql ${gameFolder}/close_game.sql
   # closing and redirecting the subshell
  ) | tee -a ${logFile}
}

function __private_setup(){
	# TODO: Remove all the Tests/Mutants from the test bed which by default is?

	mkdir -p ${SYSTEM_TESTS_HOME}
	mkdir -p ${SYSTEM_TESTS_HOME}/tests
	mkdir -p ${SYSTEM_TESTS_HOME}/mutants
	mkdir -p ${SYSTEM_TESTS_HOME}/ai

	cp -r ${CD_HOME}/sources ${SYSTEM_TESTS_HOME}/sources

	cp ${CD_HOME}/build.xml ${SYSTEM_TESTS_HOME}
	cp ${CD_HOME}/security.policy ${SYSTEM_TESTS_HOME}

	# Restore Test-DB: recreate the tables and fill in users and classes.
	__private_execute_sql sqlScripts/system-tests.sql

}

function test(){
	local gameSize=$1
	local classAlias=$2
	local nUserEvents=$3
	local configurationFiles=${@:4:$#}

	for cFile in ${configurationFiles}; do
		echo "> ${cFile}"
	done
}

function benchmark(){
	local gameSize=$1
	local classAlias=$2
	local nUserEvents=$3
	local configurationFiles=${@:4:$#}

	# Create a random game with given size and class, use 10 as default number of events per client
	local gameId=$(create_random_game ${gameSize} ${classAlias} ${nUserEvents})
	echo "> Playing GameID = ${gameId}"

	# TODO Show details about the game ?!

	for cFile in ${configurationFiles}; do
		# FIXME Clean up and restore. On defender this might require to ssh defender@defender ?
		echo "> Clean up"
		__private_setup

		echo "> Redeploy CD using ${cFile}"
		cd ..
		set -e
			mvn clean compile package install war:war tomcat7:redeploy -Dconfig.properties=${cFile} -DskipTests
		set +e
		cd ${HERE}

		# Run the game
		replay_game ${gameId}

		# Collect logs - TODO
		mv -v "${SYSTEM_TESTS_HOME}/${gameId}/game.log" "${SYSTEM_TESTS_HOME}/${gameId}/game-$(basename ${cFile}).log"

		# Output some Stats
		echo "----- Stats ----- "
		echo "Total "`grep took ${SYSTEM_TESTS_HOME}/${gameId}/game-$(basename ${cFile}).log | awk '{ num = num + 1; total = total + $17 } END { print num, "m", total/num, "s"}'`
		echo "  Attack "`grep took ${SYSTEM_TESTS_HOME}/${gameId}/game-$(basename ${cFile}).log | grep attacks | awk '{ num = num + 1; total = total + $17 } END { print num, "m", total/num, "s"}'`
		echo "  Defense "`grep took ${SYSTEM_TESTS_HOME}/${gameId}/game-$(basename ${cFile}).log | grep defends | awk '{ num = num + 1; total = total + $17 } END { print num, "m", total/num, "s"}'`

	done
}


function multi_benchmark(){
	local nGames=$1
	local gameSize=$2
	local classAlias=$3
	local nUserEvents=$4
	local configurationFiles=${@:5:$#}

	# Create ${nGames} random games
	local gameIds=""
	for i in $(seq 1 ${nGames}); do
		gameIds+=" $(create_random_game ${gameSize} ${classAlias} ${nUserEvents})"
	done

	for gameId in "${gameIds}"; do
		echo "${gameId}"
	done

	for cFile in ${configurationFiles}; do

		# Automatically make ${configurationFiles} an absolute path
		if [ "${cFile}" == "$(basename ${cFile})" ];
		then
			cFile=$(pwd)/${cFile}
		fi


		# FIXME Clean up and restore. On defender this might require to ssh defender@defender ?
		echo "> Clean up"
		__private_setup

		echo "> Redeploy CD using ${cFile}"
		cd ..
		# Stop on error
		set -e
			mvn clean compile package install war:war tomcat7:redeploy -Dconfig.properties=${cFile} -DskipTests
		set +e
		cd ${HERE}

		for gameId in ${gameIds}; do
			# Run the game in background
			replay_game ${gameId} &
		done

		wait

		# Collect logs
		for gameId in ${gameIds}; do
			mv -v "${SYSTEM_TESTS_HOME}/${gameId}/game.log" "${SYSTEM_TESTS_HOME}/${gameId}/game-$(basename ${cFile}).log"
			# Output some Stats for each game, not overall
			__private_stats_on_game ${gameId} ${cFile}
		done

	done
}

function __private_stats_on_game(){
	local gameId=$1
	local cFile=$2
	echo "----- Stats ----- "
        echo "Total "`grep took ${SYSTEM_TESTS_HOME}/${gameId}/game-$(basename ${cFile}).log | awk '{ num = num + 1; total = total + $19 } END { print num, "m", total/num, "s"}'`
        echo "  Attack "`grep took ${SYSTEM_TESTS_HOME}/${gameId}/game-$(basename ${cFile}).log | grep "\battack\b" | awk '{ num = num + 1; total = total + $20 } END { print num, "m", total/num, "s"}'`
        echo "  Defense "`grep took ${SYSTEM_TESTS_HOME}/${gameId}/game-$(basename ${cFile}).log | grep defense | awk '{ num = num + 1; total = total + $19 } END { print num, "m", total/num, "s"}'`
}

function help(){
	echo "COMMANDS"
# TODO Maybe there's a better way to list the functions defined here
	cat system-test.sh | grep function | grep -v "__private" | grep -v "\#" | sed -e '/^ /d' -e 's|function \(.*\)(){|\1|g'
}


# Invoke functions by name
if declare -f "$1" > /dev/null
then
  # call arguments verbatim
  "$@"
else
  # Show a helpful error
  echo "'$1' is not a known function name" >&2
  echo "CD_HOME is ${CD_HOME}" >&2
  echo "SYSTEM_TESTS_HOME is ${SYSTEM_TESTS_HOME}" >&2
  help
fi
