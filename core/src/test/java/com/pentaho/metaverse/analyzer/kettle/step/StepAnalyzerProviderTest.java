package com.pentaho.metaverse.analyzer.kettle.step;

import com.google.common.collect.Lists;
import org.pentaho.metaverse.api.analyzer.kettle.step.IStepAnalyzer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.internal.util.collections.Sets;
import org.mockito.runners.MockitoJUnitRunner;
import org.pentaho.di.trans.step.BaseStepMeta;
import org.pentaho.di.trans.steps.tableoutput.TableOutputMeta;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith( MockitoJUnitRunner.class )
public class StepAnalyzerProviderTest {

  StepAnalyzerProvider provider;

  @Mock
  IStepAnalyzer mockStepAnalyzer;

  @Before
  public void setUp() throws Exception {
    provider = new StepAnalyzerProvider();
  }

  @After
  public void tearDown() throws Exception {

  }

  @Test
  public void testGetAnalyzersEmpty() throws Exception {
    assertNotNull( provider.getAnalyzers() );
    assertTrue( provider.getAnalyzers().isEmpty() );
  }

  @Test
  public void testGetAnalyzersNonEmpty() throws Exception {
    assertTrue( provider.getAnalyzers().isEmpty() );
    List<IStepAnalyzer> analyzerSet = new ArrayList<IStepAnalyzer>() {{
      add( mockStepAnalyzer );
    }};
    provider.stepAnalyzers = analyzerSet;
    assertFalse( provider.getAnalyzers().isEmpty() );
  }

  @Test
  public void testGetAnalyzersForClass() throws Exception {
    Set<IStepAnalyzer> baseStepAnalyzerSet = Sets.newSet( mock( IStepAnalyzer.class ) );
    Set<IStepAnalyzer> tableOutputStepAnalyzerSet = Sets.newSet( mock( IStepAnalyzer.class ) );
    // Return the baseStepAnalyzer set if BaseStepMeta analyzers are requested
    provider.analyzerTypeMap.put( BaseStepMeta.class, baseStepAnalyzerSet );
    // Return the tableOutputStepAnalyzerSet set if TableOutputMeta analyzers are requested
    provider.analyzerTypeMap.put( TableOutputMeta.class, tableOutputStepAnalyzerSet );

    List<IStepAnalyzer> analyzers = provider.getAnalyzers( new ArrayList() {{
      add( BaseStepMeta.class );
      add( TableOutputMeta.class );
    }} );
    assertEquals( analyzers.size(), 2 );

    analyzers = provider.getAnalyzers( new HashSet() {{
      add( TableOutputMeta.class );
    }} );
    assertEquals( analyzers.size(), 1 );
  }

  @Test
  public void testSetStepAnalyzersNull() throws Exception {
    assertNotNull( provider.stepAnalyzers );
    assertTrue( provider.stepAnalyzers.isEmpty() );
    provider.setStepAnalyzers( null );
    assertNull( provider.stepAnalyzers );
  }

  @Test
  public void testSetStepAnalyzers() throws Exception {
    assertNotNull( provider.stepAnalyzers );
    assertTrue( provider.stepAnalyzers.isEmpty() );
    List<IStepAnalyzer> analyzerSet = new ArrayList<IStepAnalyzer>() {{
      add( mockStepAnalyzer );
    }};
    provider.setStepAnalyzers( analyzerSet );
    assertFalse( provider.stepAnalyzers.isEmpty() );
  }

  @Test
  public void testLoadAnalyzerTypeMap() throws Exception {
    IStepAnalyzer baseStepAnalyzer = mock( IStepAnalyzer.class );
    when( baseStepAnalyzer.getSupportedSteps() ).thenReturn( Sets.newSet( BaseStepMeta.class ) );

    IStepAnalyzer tableOutputStepAnalyzer = mock( IStepAnalyzer.class );
    when( tableOutputStepAnalyzer.getSupportedSteps() ).thenReturn( Sets.newSet( TableOutputMeta.class ) );

    IStepAnalyzer tableOutputStepAnalyzer2 = mock( IStepAnalyzer.class );
    when( tableOutputStepAnalyzer2.getSupportedSteps() ).thenReturn( Sets.newSet( TableOutputMeta.class ) );

    provider.stepAnalyzers = Lists.newArrayList( baseStepAnalyzer, tableOutputStepAnalyzer, tableOutputStepAnalyzer2 );

    // Method under test
    provider.loadAnalyzerTypeMap();

    Set<IStepAnalyzer> baseStepAnalyzers = provider.analyzerTypeMap.get( BaseStepMeta.class );
    assertNotNull( baseStepAnalyzers );
    assertEquals( baseStepAnalyzers.size(), 1 );

    Set<IStepAnalyzer> tableOutputStepAnalyzers = provider.analyzerTypeMap.get( TableOutputMeta.class );
    assertNotNull( tableOutputStepAnalyzers );
    assertEquals( tableOutputStepAnalyzers.size(), 2 );
  }

  @Test
  public void testRemoveAnalyzer() throws Exception {
    IStepAnalyzer baseStepAnalyzer = mock( IStepAnalyzer.class );
    when( baseStepAnalyzer.getSupportedSteps() ).thenReturn( Sets.newSet( BaseStepMeta.class ) );

    IStepAnalyzer tableOutputStepAnalyzer = mock( IStepAnalyzer.class );
    when( tableOutputStepAnalyzer.getSupportedSteps() ).thenReturn( Sets.newSet( TableOutputMeta.class ) );

    provider.setStepAnalyzers(
        Lists.newArrayList( baseStepAnalyzer, tableOutputStepAnalyzer ) );

    Set<IStepAnalyzer> tableOutputStepAnalyzers = provider.analyzerTypeMap.get( TableOutputMeta.class );
    assertNotNull( tableOutputStepAnalyzers );
    assertEquals( tableOutputStepAnalyzers.size(), 1 );

    provider.removeAnalyzer( tableOutputStepAnalyzer );
    tableOutputStepAnalyzers = provider.analyzerTypeMap.get( TableOutputMeta.class );
    assertNull( tableOutputStepAnalyzers );

  }

  @Test
  public void testRemoveAnalyzer_multipleWithTheSameType() throws Exception {
    IStepAnalyzer baseStepAnalyzer = mock( IStepAnalyzer.class );
    when( baseStepAnalyzer.getSupportedSteps() ).thenReturn( Sets.newSet( BaseStepMeta.class ) );

    IStepAnalyzer tableOutputStepAnalyzer = mock( IStepAnalyzer.class );
    when( tableOutputStepAnalyzer.getSupportedSteps() ).thenReturn( Sets.newSet( TableOutputMeta.class ) );

    IStepAnalyzer tableOutputStepAnalyzer2 = mock( IStepAnalyzer.class );
    when( tableOutputStepAnalyzer2.getSupportedSteps() ).thenReturn( Sets.newSet( TableOutputMeta.class ) );

    provider.setStepAnalyzers(
        Lists.newArrayList( baseStepAnalyzer, tableOutputStepAnalyzer, tableOutputStepAnalyzer2 ) );

    Set<IStepAnalyzer> tableOutputStepAnalyzers = provider.analyzerTypeMap.get( TableOutputMeta.class );
    assertNotNull( tableOutputStepAnalyzers );
    assertEquals( tableOutputStepAnalyzers.size(), 2 );

    provider.removeAnalyzer( tableOutputStepAnalyzer2 );
    tableOutputStepAnalyzers = provider.analyzerTypeMap.get( TableOutputMeta.class );
    assertNotNull( tableOutputStepAnalyzers );
    assertEquals( tableOutputStepAnalyzers.size(), 1 );
    assertEquals( tableOutputStepAnalyzer, tableOutputStepAnalyzers.iterator().next() );

  }
}
