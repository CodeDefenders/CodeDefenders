import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import org.junit.Test;

public class PersonServiceTest {
	@Test
    public void shouldUpdatePersonName()
    {
		PersonService.PersonDao personDAO = mock(PersonService.PersonDao.class);
        String person = "Phillip";
        
        PersonService personService = new PersonService( personDAO);
        
        when( personDAO.fetchPerson( 1 ) ).thenReturn( person );
        
        boolean updated = personService.update( 1, "David" );
        assertTrue( updated );
        
//        verify( personDAO ).fetchPerson( 1 );
//        
//        ArgumentCaptor<Person> personCaptor = ArgumentCaptor.forClass( Person.class );
//        verify( personDAO ).update( personCaptor.capture() );
//        Person updatedPerson = personCaptor.getValue();
//        assertEquals( "David", updatedPerson.getPersonName() );
//        // asserts that during the test, there are no other calls to the mock object.
//        verifyNoMoreInteractions( personDAO );
    }
}
