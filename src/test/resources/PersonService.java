
public class PersonService {

	interface PersonDao {
		// Person name or null
		public String fetchPerson(Integer personID);
		// Update if exists
		public void update(Integer personID, String person);
		// Insert if does not exist yet. Otherwise return -1
		public int insert(String person);
	}

	private final PersonDao personDao;

	public PersonService(PersonDao personDao) {
		this.personDao = personDao;
	}

	public boolean update(Integer personId, String updatedPerson) {
		String person = personDao.fetchPerson(personId);
		if (person != null) {
			personDao.update(personId, updatedPerson);
			return true;
		} else {
			return false;
		}
	}
	
	
	public int createPerson(String personName){
		return personDao.insert(personName);
	}
}
