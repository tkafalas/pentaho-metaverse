/*! ******************************************************************************
 *
 * Pentaho Data Integration
 *
 * Copyright (C) 2002-2015 by Pentaho : http://www.pentaho.com
 *
 *******************************************************************************
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 ******************************************************************************/

package org.pentaho.metaverse.analyzer.kettle;

import org.pentaho.di.core.Const;
import org.pentaho.di.core.exception.KettleXMLException;
import org.pentaho.di.core.parameters.UnknownParamException;
import org.pentaho.di.job.Job;
import org.pentaho.di.job.JobHopMeta;
import org.pentaho.di.job.JobMeta;
import org.pentaho.di.job.entry.JobEntryCopy;
import org.pentaho.di.job.entry.JobEntryInterface;
import org.pentaho.dictionary.DictionaryConst;
import org.pentaho.metaverse.analyzer.kettle.jobentry.GenericJobEntryMetaAnalyzer;
import org.pentaho.metaverse.api.IComponentDescriptor;
import org.pentaho.metaverse.api.IDocument;
import org.pentaho.metaverse.api.IMetaverseNode;
import org.pentaho.metaverse.api.INamespace;
import org.pentaho.metaverse.api.MetaverseAnalyzerException;
import org.pentaho.metaverse.api.MetaverseComponentDescriptor;
import org.pentaho.metaverse.api.Namespace;
import org.pentaho.metaverse.api.PropertiesHolder;
import org.pentaho.metaverse.api.analyzer.kettle.jobentry.IJobEntryAnalyzer;
import org.pentaho.metaverse.api.analyzer.kettle.jobentry.IJobEntryAnalyzerProvider;
import org.pentaho.metaverse.messages.Messages;
import org.pentaho.platform.engine.core.system.PentahoSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

/**
 * The JobAnalyzer class is responsible for gathering job metadata, creating links
 * to form relationships between the job and its child collaborators (ie, entries, dbMetas), and
 * calling the analyzers responsible for providing the metadata for the child collaborators.
 */
public class JobAnalyzer extends BaseDocumentAnalyzer {

  /**
   * A set of types supported by this analyzer
   */
  protected static final Set<String> defaultSupportedTypes = new HashSet<String>() {
    {
      add( "kjb" );
    }
  };

  /**
   * A reference to the job entry analyzer provider
   */
  private IJobEntryAnalyzerProvider jobEntryAnalyzerProvider;

  private static final Logger log = LoggerFactory.getLogger( JobAnalyzer.class );

  @Override
  public synchronized IMetaverseNode analyze( IComponentDescriptor descriptor, IDocument document )
    throws MetaverseAnalyzerException {

    validateState( document );

    Object repoObject = document.getContent();

    JobMeta jobMeta = null;
    if ( repoObject instanceof String ) {

      // hydrate the job
      try {
        String content = (String) repoObject;
        ByteArrayInputStream xmlStream = new ByteArrayInputStream( content.getBytes() );
        jobMeta = new JobMeta( xmlStream, null, null );
      } catch ( KettleXMLException e ) {
        throw new MetaverseAnalyzerException( e );
      }

    } else if ( repoObject instanceof JobMeta ) {
      jobMeta = (JobMeta) repoObject;
    }

    // construct a dummy job based on our JobMeta so we get out VariableSpace set properly
    jobMeta.setFilename( document.getStringID() );
    Job j = new Job( null, jobMeta );
    j.setInternalKettleVariables( jobMeta );

    IComponentDescriptor documentDescriptor = new MetaverseComponentDescriptor( document.getStringID(),
      DictionaryConst.NODE_TYPE_JOB, new Namespace( descriptor.getLogicalId() ), descriptor.getContext() );

    // Create a metaverse node and start filling in details
    IMetaverseNode node = metaverseObjectFactory.createNodeObject(
      document.getNamespace(),
      jobMeta.getName(),
      DictionaryConst.NODE_TYPE_JOB );
    node.setLogicalIdGenerator( DictionaryConst.LOGICAL_ID_GENERATOR_DOCUMENT );

    // pull out the standard fields
    String description = jobMeta.getDescription();
    if ( description != null ) {
      node.setProperty( DictionaryConst.PROPERTY_DESCRIPTION, description );
    }

    String extendedDescription = jobMeta.getExtendedDescription();
    if ( extendedDescription != null ) {
      node.setProperty( "extendedDescription", extendedDescription );
    }

    Date createdDate = jobMeta.getCreatedDate();
    if ( createdDate != null ) {
      node.setProperty( DictionaryConst.PROPERTY_CREATED, Long.toString( createdDate.getTime() ) );
    }

    String createdUser = jobMeta.getCreatedUser();
    if ( createdUser != null ) {
      node.setProperty( DictionaryConst.PROPERTY_CREATED_BY, createdUser );
    }

    Date lastModifiedDate = jobMeta.getModifiedDate();
    if ( lastModifiedDate != null ) {
      node.setProperty( DictionaryConst.PROPERTY_LAST_MODIFIED, Long.toString( lastModifiedDate.getTime() ) );
    }

    String lastModifiedUser = jobMeta.getModifiedUser();
    if ( lastModifiedUser != null ) {
      node.setProperty( DictionaryConst.PROPERTY_LAST_MODIFIED_BY, lastModifiedUser );
    }

    String version = jobMeta.getJobversion();
    if ( version != null ) {
      node.setProperty( DictionaryConst.PROPERTY_ARTIFACT_VERSION, version );
    }

    String status = Messages.getString( "INFO.JobOrTrans.Status_" + Integer.toString( jobMeta.getJobstatus() ) );
    if ( status != null && !status.startsWith( "!" ) ) {
      node.setProperty( DictionaryConst.PROPERTY_STATUS, status );
    }

    node.setProperty( DictionaryConst.PROPERTY_PATH, document.getProperty( DictionaryConst.PROPERTY_PATH ) );

    // Process job parameters
    String[] parameters = jobMeta.listParameters();
    if ( parameters != null ) {
      for ( String parameter : parameters ) {
        try {
          // Determine parameter properties and add them to a map, then the map to the list
          String defaultParameterValue = jobMeta.getParameterDefault( parameter );
          String parameterValue = jobMeta.getParameterValue( parameter );
          String parameterDescription = jobMeta.getParameterDescription( parameter );
          PropertiesHolder paramProperties = new PropertiesHolder();
          paramProperties.setProperty( "defaultValue", defaultParameterValue );
          paramProperties.setProperty( "value", parameterValue );
          paramProperties.setProperty( "description", parameterDescription );
          node.setProperty( "parameter_" + parameter, paramProperties.toString() );
        } catch ( UnknownParamException upe ) {
          // This shouldn't happen as we're using the list provided by the meta
          throw new MetaverseAnalyzerException( upe );
        }
      }
    }
    // handle the entries
    for ( int i = 0; i < jobMeta.nrJobEntries(); i++ ) {
      JobEntryCopy entry = jobMeta.getJobEntry( i );
      try {
        if ( entry != null ) {
          entry.getEntry().setParentJob( j );
          IMetaverseNode jobEntryNode = null;
          JobEntryInterface jobEntryInterface = entry.getEntry();

          IComponentDescriptor entryDescriptor = new MetaverseComponentDescriptor( entry.getName(),
            DictionaryConst.NODE_TYPE_JOB_ENTRY, node, descriptor.getContext() );

          Set<IJobEntryAnalyzer> jobEntryAnalyzers = getJobEntryAnalyzers( jobEntryInterface );
          if ( jobEntryAnalyzers != null && !jobEntryAnalyzers.isEmpty() ) {
            for ( IJobEntryAnalyzer jobEntryAnalyzer : jobEntryAnalyzers ) {
              jobEntryAnalyzer.setMetaverseBuilder( metaverseBuilder );
              jobEntryNode = (IMetaverseNode) jobEntryAnalyzer.analyze( entryDescriptor, entry.getEntry() );
            }
          } else {
            GenericJobEntryMetaAnalyzer defaultJobEntryAnalyzer = new GenericJobEntryMetaAnalyzer();
            defaultJobEntryAnalyzer.setMetaverseBuilder( metaverseBuilder );
            jobEntryNode = defaultJobEntryAnalyzer.analyze( entryDescriptor, jobEntryInterface );
          }
          if ( jobEntryNode != null ) {
            metaverseBuilder.addLink( node, DictionaryConst.LINK_CONTAINS, jobEntryNode );
          }
        }
      } catch ( Throwable mae ) {
        //Don't throw an exception, just log and carry on
        log.warn( Messages.getString( "ERROR.ErrorDuringAnalysis", entry.getName(),
          Const.NVL( mae.getLocalizedMessage(), "Unspecified" ) ) );
        log.debug( Messages.getString( "ERROR.ErrorDuringAnalysisStackTrace" ), mae );
      }
    }

    // Model the hops between steps
    int numHops = jobMeta.nrJobHops();
    for ( int i = 0; i < numHops; i++ ) {
      JobHopMeta hop = jobMeta.getJobHop( i );
      JobEntryCopy fromEntry = hop.getFromEntry();
      JobEntryCopy toEntry = hop.getToEntry();
      INamespace childNs = new Namespace( node.getLogicalId() );

      // process legitimate hops
      if ( fromEntry != null && toEntry != null ) {
        IMetaverseNode fromEntryNode = metaverseObjectFactory.createNodeObject(
          childNs,
          fromEntry.getName(),
          DictionaryConst.NODE_TYPE_JOB_ENTRY );

        IMetaverseNode toEntryNode = metaverseObjectFactory.createNodeObject(
          childNs,
          toEntry.getName(),
          DictionaryConst.NODE_TYPE_JOB_ENTRY );

        metaverseBuilder.addLink( fromEntryNode, DictionaryConst.LINK_HOPSTO, toEntryNode );
      }
    }

    metaverseBuilder.addNode( node );
    addParentLink( documentDescriptor, node );
    return node;
  }

  /**
   * Returns a set of strings corresponding to which types of content are supported by this analyzer
   *
   * @return the supported types (as a set of Strings)
   * @see IDocumentAnalyzer#getSupportedTypes()
   */
  @Override
  public Set<String> getSupportedTypes() {
    return defaultSupportedTypes;
  }

  public Set<IJobEntryAnalyzer> getJobEntryAnalyzers( final JobEntryInterface jobEntryInterface ) {

    Set<IJobEntryAnalyzer> jobEntryAnalyzers = new HashSet<>();

    // Attempt to discover a BaseStepMeta from the given StepMeta
    jobEntryAnalyzerProvider = getJobEntryAnalyzerProvider();
    if ( jobEntryAnalyzerProvider != null ) {
      if ( jobEntryInterface == null ) {
        jobEntryAnalyzers.addAll( jobEntryAnalyzerProvider.getAnalyzers() );
      } else {
        Set<Class<?>> analyzerClassSet = new HashSet<>( 1 );
        analyzerClassSet.add( jobEntryInterface.getClass() );
        jobEntryAnalyzers.addAll( jobEntryAnalyzerProvider.getAnalyzers( analyzerClassSet ) );
      }
    } else {
      jobEntryAnalyzers.add( new GenericJobEntryMetaAnalyzer() );
    }

    return jobEntryAnalyzers;
  }

  public void setJobEntryAnalyzerProvider( IJobEntryAnalyzerProvider jobEntryAnalyzerProvider ) {
    this.jobEntryAnalyzerProvider = jobEntryAnalyzerProvider;
  }

  /**
   * Retrieves the step analyzer provider. This is used to find step-specific analyzers
   *
   * @return the IKettleStepAnalyzer provider instance that provides step-specific analyzers
   */
  public IJobEntryAnalyzerProvider getJobEntryAnalyzerProvider() {
    if ( jobEntryAnalyzerProvider != null ) {
      return jobEntryAnalyzerProvider;
    }
    jobEntryAnalyzerProvider = PentahoSystem.get( IJobEntryAnalyzerProvider.class );
    return jobEntryAnalyzerProvider;
  }
}
