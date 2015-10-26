package embl.ebi.variation.commons.models.metadata.database;

import embl.ebi.variation.commons.models.metadata.DatabaseTestConfiguration;
import embl.ebi.variation.commons.models.metadata.Publication;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.jpa.JpaSystemException;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

/**
 * Created by parce on 20/10/15.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@Transactional
@ContextConfiguration(classes = DatabaseTestConfiguration.class)
public class PublicationDatabaseTest {

    @Autowired
    PublicationRepository repository;

    Publication publication1, publication2, publication3;

    @Before
    public void setUp() {
        String author1 = "Author 1";
        String author2 = "Author 2";
        List<String> publication1Authors = new ArrayList<>();
        publication1Authors.add(author1);
        publication1Authors.add(author2);
        List<String> publication2Authors = new ArrayList<>();
        publication2Authors.add(author2);
        publication2Authors.add(author1);

        publication1 = new Publication("Publication 1", "Journal 1", "111", publication1Authors);
        publication2 = new Publication("Publication 2", "Journal 2", "111", publication2Authors);
        publication3 = new Publication("Publication 3", "Journal 3", "3", publication1Authors, "database", "dbid");
    }

    /**
     * After saving a publication in the database, when only the mandatory fields are
     * set, their values must be the same before and after, and optional fields
     * must be empty.
     */
    @Test
    public void testSaveMandatoryFields() {
        Publication savedPublication = repository.save(publication1);

        assertNotNull(savedPublication.getId());
        assertEquals(publication1.getTitle(), savedPublication.getTitle());
        assertEquals(publication1.getJournal(), savedPublication.getJournal());
        assertEquals(publication1.getVolume(), savedPublication.getVolume());
        assertThat(publication1.getAuthors(), contains(savedPublication.getAuthors().toArray()));
        assertEquals(0, savedPublication.getStartPage());
        assertEquals(0, savedPublication.getEndPage());
        assertNull(savedPublication.getDatabase());
        assertNull(savedPublication.getDbId());
        assertNull(savedPublication.getDoi());
        assertNull(savedPublication.getIsbn());
        assertNull(savedPublication.getPublicationDate());
        assertThat(savedPublication.getStudies(), empty());
    }

    /**
     * After saving a publication in the database, when only the mandatory fields are
     * set, their values must be the same before and after, and relationship optional
     * fields must be empty.
     */
    @Test
    public void testSaveNonMandatoryFields() {
        // add optional fields that are not in the constructor
        publication3.setDoi("doi");
        publication3.setStartPage(100);
        publication3.setEndPage(120);
        Calendar calendar = Calendar.getInstance();
        publication3.setPublicationDate(calendar);
        Publication savedPublication = repository.save(publication3);

        assertNotNull(savedPublication.getId());
        assertEquals(publication3.getTitle(), savedPublication.getTitle());
        assertEquals(publication3.getJournal(), savedPublication.getJournal());
        assertEquals(publication3.getVolume(), savedPublication.getVolume());
        assertThat(publication3.getAuthors(), contains(savedPublication.getAuthors().toArray()));
        assertEquals(publication3.getDatabase(), savedPublication.getDatabase());
        assertEquals(publication3.getDbId(), savedPublication.getDbId());
        assertEquals(publication3.getDoi(), savedPublication.getDoi());
        assertEquals(publication3.getPublicationDate(), savedPublication.getPublicationDate());
        assertThat(savedPublication.getStudies(), empty());
    }



    /**
     * Two different publications are both inserted into the database.
     */
    @Test
    public void testNonDuplicated() {
        repository.save(publication1);
        repository.save(publication2);
        assertEquals(2, repository.count());
    }

    /**
     * Inserting the same publication twice creates only one entry in the database.
     */
    @Test
    public void testDuplicated() {
        Publication savedPublication1 = repository.save(publication1);
        Publication savedPublication2 = repository.save(publication1);
        assertEquals(1, repository.count());
        assertEquals(savedPublication1, savedPublication2);
    }

    /**
     * A file before and after insertion must be considered equal. Two different
     * files in the database can't be equal.
     */
    @Test
    public void testEquals() {
        // Compare saved and unsaved entities
        Publication savedPublication1 = repository.save(publication1);
        Publication detachedPublication1 = new Publication("Publication 1", "Journal 1", "111", Arrays.asList("Author 1", "Author 2"));
        assertEquals(detachedPublication1, savedPublication1);

        Publication savedPublication2 = repository.save(publication2);
        Publication detachedPublication2 = new Publication("Publication 2", "Journal 2", "111", Arrays.asList("Author 1", "Author 2"));
        // Compare two saved entities
        assertEquals(detachedPublication2, savedPublication2);
        assertNotEquals(publication1, savedPublication2);
        assertNotEquals(savedPublication1, savedPublication2);
    }

    /**
     * The hash code before and after insertion must be the same.
     */
    @Test
    public void testHashCode() {
        Publication savedPublication1 = repository.save(publication1);
        assertEquals(publication1.hashCode(), savedPublication1.hashCode());
    }

    /**
     * Deleting one publication leaves the other still in the database.
     */
    @Test
    public void testDelete() {
        repository.save(publication1);
        repository.save(publication2);
        assertEquals(2, repository.count());
        repository.delete(publication1);
        assertEquals(1, repository.count());
        assertEquals(publication2, repository.findAll().iterator().next());
    }

    /**
     * Updating a publication assigning only part of the unique key from other must not fail when serialising
     */
    @Test
    public void testUpdateNonDuplicate() {
        Publication savedPublication1 = repository.save(publication1);
        Publication savedPublication2 = repository.save(publication2);

        savedPublication1.setTitle(publication2.getTitle());
        repository.save(savedPublication1);

        Iterator<Publication> iterator = repository.findAll().iterator();

        Publication retrievedPublication1 = iterator.next();
        assertEquals(publication2.getTitle(), retrievedPublication1.getTitle());
        assertEquals(publication1.getJournal(), retrievedPublication1.getJournal());
        assertEquals(publication1.getVolume(), retrievedPublication1.getVolume());
        assertThat(publication1.getAuthors(), contains(retrievedPublication1.getAuthors().toArray()));

        Publication retrievedPublication2 = iterator.next();
        assertEquals(publication2.getTitle(), retrievedPublication2.getTitle());
        assertEquals(publication2.getJournal(), retrievedPublication2.getJournal());
        assertEquals(publication2.getVolume(), retrievedPublication2.getVolume());
        assertThat(publication2.getAuthors(), contains(retrievedPublication2.getAuthors().toArray()));
    }

    /**
     * Updating a publication assigning the unique key from other must fail when serialising
     * @todo How to report this kind of errors?
     */
    @Test(expected = JpaSystemException.class)
    public void testUpdateDuplicate() {
        Publication savedPublication1 = repository.save(publication1);
        Publication savedPublication3 = repository.save(publication3);

        savedPublication1.setTitle(publication3.getTitle());
        savedPublication1.setJournal(publication3.getJournal());
        savedPublication1.setVolume(publication3.getVolume());
        // is not necessary update the authors because publication 1 has the same author list than publication 3

        repository.save(savedPublication1);
        repository.findAll();
    }
}
