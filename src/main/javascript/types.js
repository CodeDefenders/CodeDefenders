/* Type documentation for IDEs. This file should never be included anywhere. */

/**
 * @typedef {object} SimpleUser
 * @property {number} id
 * @property {string} name
 */

/**
 * @typedef {object} MutantDTO
 * @property {number} id
 * @property {SimpleUser} creator
 * @property {string} state
 * @property {number} points
 * @property {string} lineString
 * @property {SimpleUser} killedBy
 * @property {boolean} canMarkEquivalent
 * @property {boolean} canView
 * @property {number} killedByTestId
 * @property {string} killMessage
 * @property {string} description
 * @property {boolean} covered
 * @property {number[]} lines
 */

/**
 * @typedef {object} TestDTO
 * @property {number} id
 * @property {SimpleUser} creator
 * @property {number} points
 * @property {boolean} canView
 * @property {number[]} coveredMutantIds
 * @property {number[]} killedMutantIds
 * @property {string[]} smells
 */

/**
 * @typedef {object} GHMutantDTO
 * @property {number} id
 * @property {number} score
 * @property {string} lines
 * @property {string} creatorName
 * @property {string} status
 * @property {boolean} canClaim
 */

/**
 * @typedef {object} GHTestDTO
 * @property {number} id
 */

/**
 * @typedef {object} MutantAccordionCategory
 * @property {string} description
 * @property {number[]} mutantIds
 * @property {string} id
 */

/**
 * @typedef {object} TestAccordionCategory
 * @property {string} id
 * @property {string} description
 * @property {number[]} testIds
 */

/**
 * @typedef {object} PushSocketMessage
 * @property {string} type
 * @property {any} data
 */

/**
 * @typedef {object} GameChatMessage
 * @property {boolean} system
 * @property {string} message
 * @property {boolean|undefined} isAllChat
 * @property {string|undefined} role
 * @property {number|undefined} senderId
 * @property {string|undefined} senderName
 * @property {HTMLDivElement|undefined} _cache
 */

/**
 * @typedef {object} ServerSystemChatEvent
 * @property {string} message
 */

/**
 * @typedef {object} ServerGameChatEvent
 * @property {string} message
 * @property {string} role
 * @property {number} senderId
 * @property {string} senderName
 * @property {boolean} isAllChat
 */
