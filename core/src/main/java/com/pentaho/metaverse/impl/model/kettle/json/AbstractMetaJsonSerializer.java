/*
 * PENTAHO CORPORATION PROPRIETARY AND CONFIDENTIAL
 *
 * Copyright 2002 - 2014 Pentaho Corporation (Pentaho). All rights reserved.
 *
 * NOTICE: All information including source code contained herein is, and
 * remains the sole property of Pentaho and its licensors. The intellectual
 * and technical concepts contained herein are proprietary and confidential
 * to, and are trade secrets of Pentaho and may be covered by U.S. and foreign
 * patents, or patents in process, and are protected by trade secret and
 * copyright laws. The receipt or possession of this source code and/or related
 * information does not convey or imply any rights to reproduce, disclose or
 * distribute its contents, or to manufacture, use, or sell anything that it
 * may describe, in whole or in part. Any reproduction, modification, distribution,
 * or public display of this information without the express written authorization
 * from Pentaho is strictly prohibited and in violation of applicable laws and
 * international treaties. Access to the source code contained herein is strictly
 * prohibited to anyone except those individuals and entities who have executed
 * confidentiality and non-disclosure agreements or other agreements with Pentaho,
 * explicitly covering such access.
 */

package com.pentaho.metaverse.impl.model.kettle.json;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import org.pentaho.dictionary.DictionaryConst;
import org.pentaho.metaverse.api.model.IInfo;
import org.pentaho.metaverse.api.model.BaseResourceInfo;
import org.pentaho.metaverse.api.model.ExternalResourceInfoFactory;
import com.pentaho.metaverse.impl.model.ParamInfo;
import com.pentaho.metaverse.impl.model.kettle.LineageRepository;
import com.pentaho.metaverse.messages.Messages;
import org.pentaho.di.base.AbstractMeta;
import org.pentaho.di.core.database.DatabaseMeta;
import org.pentaho.di.core.parameters.UnknownParamException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

/**
 * User: RFellows Date: 12/15/14
 */
public abstract class AbstractMetaJsonSerializer<T extends AbstractMeta> extends StdSerializer<T> {
  private static final Logger LOGGER = LoggerFactory.getLogger( AbstractMetaJsonSerializer.class );

  public static final String JSON_PROPERTY_PARAMETERS = "parameters";
  public static final String JSON_PROPERTY_STEPS = "steps";
  public static final String JSON_PROPERTY_CONNECTIONS = "connections";
  public static final String JSON_PROPERTY_HOPS = "hops";
  public static final String JSON_PROPERTY_VARIABLES = "variables";
  public static final String JSON_PROPERTY_CREATED_DATE = DictionaryConst.PROPERTY_CREATED;
  public static final String JSON_PROPERTY_LAST_MODIFIED_DATE = DictionaryConst.PROPERTY_LAST_MODIFIED;
  public static final String JSON_PROPERTY_CREATED_BY = DictionaryConst.PROPERTY_CREATED_BY;
  public static final String JSON_PROPERTY_LAST_MODIFIED_BY = DictionaryConst.PROPERTY_LAST_MODIFIED_BY;
  public static final String JSON_PROPERTY_PATH = DictionaryConst.PROPERTY_PATH;
  public static final String JSON_PROPERTY_REPOSITORY = "repository";

  private LineageRepository lineageRepository;

  protected AbstractMetaJsonSerializer( Class<T> aClass ) {
    super( aClass );
  }

  protected AbstractMetaJsonSerializer( JavaType javaType ) {
    super( javaType );
  }

  protected AbstractMetaJsonSerializer( Class<?> aClass, boolean b ) {
    super( aClass, b );
  }


  public LineageRepository getLineageRepository() {
    return lineageRepository;
  }

  public void setLineageRepository( LineageRepository repo ) {
    this.lineageRepository = repo;
  }

  @Override
  public void serialize( T meta, JsonGenerator json, SerializerProvider serializerProvider )
    throws IOException, JsonGenerationException {

    json.writeStartObject();
    json.writeStringField( IInfo.JSON_PROPERTY_CLASS, meta.getClass().getName() );
    json.writeStringField( IInfo.JSON_PROPERTY_NAME, meta.getName() );
    json.writeStringField( IInfo.JSON_PROPERTY_DESCRIPTION, meta.getDescription() );
    json.writeObjectField( JSON_PROPERTY_CREATED_DATE, meta.getCreatedDate() );
    json.writeObjectField( JSON_PROPERTY_LAST_MODIFIED_DATE, meta.getModifiedDate() );
    json.writeStringField( JSON_PROPERTY_CREATED_BY, meta.getCreatedUser() );
    json.writeStringField( JSON_PROPERTY_LAST_MODIFIED_BY, meta.getModifiedUser() );
    json.writeStringField( JSON_PROPERTY_PATH, meta.getFilename() );
    if ( meta.getRepository() != null ) {
      json.writeStringField( JSON_PROPERTY_REPOSITORY, meta.getRepository().getName() );
    }

    serializeParameters( meta, json );
    serializeVariables( meta, json );
    serializeSteps( meta, json );
    serializeConnections( meta, json );
    serializeHops( meta, json );

    json.writeEndObject();
  }

  protected abstract void serializeHops( T meta, JsonGenerator json ) throws IOException;

  protected void serializeConnections( T meta, JsonGenerator json ) throws IOException {
    // connections
    json.writeArrayFieldStart( JSON_PROPERTY_CONNECTIONS );
    for ( DatabaseMeta dbmeta : meta.getDatabases() ) {
      BaseResourceInfo resourceInfo = (BaseResourceInfo) ExternalResourceInfoFactory.createDatabaseResource( dbmeta );
      resourceInfo.setInput( true );
      json.writeObject( resourceInfo );
    }
    json.writeEndArray();
  }

  protected abstract void serializeSteps( T meta, JsonGenerator json ) throws IOException;

  protected abstract List<String> getUsedVariables( T meta );

  protected void serializeVariables( T meta, JsonGenerator json ) throws IOException {
    json.writeArrayFieldStart( JSON_PROPERTY_VARIABLES );
    List<String> variables = getUsedVariables( meta );
    if ( variables != null ) {
      for ( String param : variables ) {
        ParamInfo paramInfo = new ParamInfo( param, meta.getVariable( param ) );
        json.writeObject( paramInfo );
      }
    }
    json.writeEndArray();
  }

  protected void serializeParameters( T meta, JsonGenerator json ) throws IOException {
    json.writeArrayFieldStart( JSON_PROPERTY_PARAMETERS );
    String[] parameters = meta.listParameters();
    if ( parameters != null ) {
      for ( String param : parameters ) {
        try {
          ParamInfo paramInfo = new ParamInfo( param, null, meta.getParameterDefault( param ),
              meta.getParameterDescription( param ) );
          json.writeObject( paramInfo );
        } catch ( UnknownParamException e ) {
          LOGGER.warn( Messages.getString( "WARNING.Serialization.Trans.Param", param ), e );
        }
      }
    }
    json.writeEndArray();
  }
}
