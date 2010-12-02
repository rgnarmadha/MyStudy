package org.sakaiproject.nakamura.persondirectory.providers;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sakaiproject.nakamura.api.persondirectory.PersonProvider;
import org.sakaiproject.nakamura.api.persondirectory.PersonProviderException;
import org.sakaiproject.nakamura.api.profile.ProviderSettings;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import javax.jcr.Node;

@RunWith(MockitoJUnitRunner.class)
public class PersonProfileProviderAdapterTest {

  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  ProviderSettings ps1;

  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  PersonProvider personProvider;

  @SuppressWarnings("unchecked")
  @Test
  public void testGetProvidedMap() throws InterruptedException, ExecutionException,
      PersonProviderException {
    PersonProfileProviderAdapter pppa = new PersonProfileProviderAdapter();
    pppa.personProvider = this.personProvider;
    ArrayList<ProviderSettings> list = new ArrayList<ProviderSettings>();
    list.add(ps1);

    Map<String, Object> profileSection = new HashMap<String, Object>();
    profileSection.put("foo", "bar");

    Node node = mock(Node.class);
    when(ps1.getNode()).thenReturn(node);
    when(personProvider.getProfileSection(node)).thenReturn(profileSection);

    Map<Node, Future<Map<String, Object>>> result = (Map<Node, Future<Map<String, Object>>>) pppa.getProvidedMap(list);
    Future<Map<String, Object>> fut = result.get(node);
    assertEquals(profileSection, fut.get());
  }

  @Test
  public void testGetProvidedMapHandlesException() throws PersonProviderException,
      InterruptedException, ExecutionException {
    PersonProfileProviderAdapter pppa = new PersonProfileProviderAdapter();
    pppa.personProvider = this.personProvider;
    ArrayList<ProviderSettings> list = new ArrayList<ProviderSettings>();
    list.add(ps1);

    Node node = mock(Node.class);
    when(ps1.getNode()).thenReturn(node);
    String errorMessage = "Mocked error is a mock";
    when(this.personProvider.getProfileSection(org.mockito.Mockito.any(Node.class)))
        .thenThrow(
new PersonProviderException(errorMessage));

    @SuppressWarnings("unchecked")
    Map<Node, Future<Map<String,Object>>> result = (Map<Node, Future<Map<String,Object>>>) pppa.getProvidedMap(list);
    Future<Map<String, Object>> fut = result.get(node);
    assertEquals(errorMessage, fut.get().get("error"));
  }
}
