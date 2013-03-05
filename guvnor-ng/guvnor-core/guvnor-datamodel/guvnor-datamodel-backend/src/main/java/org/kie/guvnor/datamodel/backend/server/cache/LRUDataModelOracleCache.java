package org.kie.guvnor.datamodel.backend.server.cache;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.inject.Named;

import org.kie.commons.io.IOService;
import org.kie.commons.validation.PortablePreconditions;
import org.kie.guvnor.commons.service.builder.model.BuildMessage;
import org.kie.guvnor.commons.service.builder.model.BuildResults;
import org.kie.guvnor.datamodel.backend.server.builder.packages.PackageDataModelOracleBuilder;
import org.kie.guvnor.datamodel.events.InvalidateDMOPackageCacheEvent;
import org.kie.guvnor.datamodel.events.InvalidateDMOProjectCacheEvent;
import org.kie.guvnor.datamodel.oracle.DataModelOracle;
import org.kie.guvnor.datamodel.oracle.ProjectDefinition;
import org.kie.guvnor.project.service.ProjectService;
import org.kie.guvnor.services.backend.file.FileExtensionFilter;
import org.kie.guvnor.services.cache.LRUCache;
import org.kie.guvnor.services.file.FileDiscoveryService;
import org.kie.guvnor.services.file.Filter;
import org.uberfire.backend.server.util.Paths;
import org.uberfire.backend.vfs.Path;

/**
 * A simple LRU cache for Package DataModelOracles
 */
@ApplicationScoped
@Named("PackageDataModelOracleCache")
public class LRUDataModelOracleCache extends LRUCache<Path, DataModelOracle> {

    private static final Filter FILTER_ENUMERATIONS = new FileExtensionFilter( ".enumeration" );

    private static final Filter FILTER_DSLS = new FileExtensionFilter( ".dsl" );

    private static final Filter FILTER_GLOBALS = new FileExtensionFilter( ".global.drl" );

    @Inject
    private Paths paths;

    @Inject
    @Named("ioStrategy")
    private IOService ioService;

    @Inject
    private FileDiscoveryService fileDiscoveryService;

    @Inject
    @Named("ProjectDataModelOracleCache")
    private LRUProjectDataModelOracleCache cacheProjects;

    @Inject
    private ProjectService projectService;

    public void invalidatePackageCache( @Observes final InvalidateDMOPackageCacheEvent event ) {
        PortablePreconditions.checkNotNull( "event",
                                            event );
        final Path resourcePath = event.getResourcePath();
        final Path packagePath = projectService.resolvePackage( resourcePath );

        //If resource was not within a Package there's nothing to invalidate
        if ( packagePath != null ) {
            invalidateCache( packagePath );
        }
    }

    public void invalidateProjectPackagesCache( @Observes final InvalidateDMOProjectCacheEvent event ) {
        PortablePreconditions.checkNotNull( "event",
                                            event );
        final Path resourcePath = event.getResourcePath();
        final Path projectPath = projectService.resolveProject( resourcePath );

        //If resource was not within a Project there's nothing to invalidate
        if ( projectPath == null ) {
            return;
        }

        final String projectUri = projectPath.toURI();
        final List<Path> cacheEntriesToInvalidate = new ArrayList<Path>();
        for ( final Path packagePath : getKeys() ) {
            final String packageUri = packagePath.toURI();
            if ( packageUri.startsWith( projectUri ) ) {
                cacheEntriesToInvalidate.add( packagePath );
            }
        }
        for ( final Path packagePath : cacheEntriesToInvalidate ) {
            invalidateCache( packagePath );
        }
    }

    //Check the DataModelOracle for the Package has been created, otherwise create one!
    public DataModelOracle assertPackageDataModelOracle( final Path projectPath,
                                                         final Path packagePath ) throws BuildException {
        DataModelOracle oracle = getEntry( packagePath );
        if ( oracle == null ) {
            oracle = makePackageDataModelOracle( projectPath,
                                                 packagePath );
            setEntry( packagePath,
                      oracle );
        }
        return oracle;
    }

    private DataModelOracle makePackageDataModelOracle( final Path projectPath,
                                                        final Path packagePath ) throws BuildException {
        final String packageName = projectService.resolvePackageName( packagePath );
        final PackageDataModelOracleBuilder dmoBuilder = PackageDataModelOracleBuilder.newDataModelBuilder( packageName );
        final ProjectDefinition projectDefinition = cacheProjects.assertProjectDataModelOracle( projectPath );
        dmoBuilder.setProjectDefinition( projectDefinition );

        //Add Guvnor enumerations
        loadEnumsForPackage( dmoBuilder,
                             packagePath );

        //Add DSLs
        loadDslsForPackage( dmoBuilder,
                            packagePath );

        //Add Globals
        loadGlobalsForPackage( dmoBuilder,
                               packagePath );

        //Report any errors
        final BuildResults results = new BuildResults();
        final List<String> errors = dmoBuilder.getErrors();
        for ( final String error : errors ) {
            results.getMessages().add( makeMessage( error ) );
        }
        if ( !results.getMessages().isEmpty() ) {
            throw new BuildException( results );
        }

        return dmoBuilder.build();
    }

    private BuildMessage makeMessage( final String msg ) {
        final BuildMessage buildMessage = new BuildMessage();
        buildMessage.setLevel( BuildMessage.Level.ERROR );
        buildMessage.setText( msg );
        return buildMessage;
    }

    private void loadEnumsForPackage( final PackageDataModelOracleBuilder dmoBuilder,
                                      final Path packagePath ) {
        final org.kie.commons.java.nio.file.Path nioPackagePath = paths.convert( packagePath );
        final Collection<org.kie.commons.java.nio.file.Path> enumFiles = fileDiscoveryService.discoverFiles( nioPackagePath,
                                                                                                             FILTER_ENUMERATIONS );
        for ( final org.kie.commons.java.nio.file.Path path : enumFiles ) {
            final String enumDefinition = ioService.readAllString( path );
            dmoBuilder.addEnum( enumDefinition );
        }
    }

    private void loadDslsForPackage( final PackageDataModelOracleBuilder dmoBuilder,
                                     final Path packagePath ) {
        final org.kie.commons.java.nio.file.Path nioPackagePath = paths.convert( packagePath );
        final Collection<org.kie.commons.java.nio.file.Path> dslFiles = fileDiscoveryService.discoverFiles( nioPackagePath,
                                                                                                            FILTER_DSLS );
        for ( final org.kie.commons.java.nio.file.Path path : dslFiles ) {
            final String dslDefinition = ioService.readAllString( path );
            dmoBuilder.addDsl( dslDefinition );
        }
    }

    private void loadGlobalsForPackage( final PackageDataModelOracleBuilder dmoBuilder,
                                        final Path packagePath ) {
        final org.kie.commons.java.nio.file.Path nioPackagePath = paths.convert( packagePath );
        final Collection<org.kie.commons.java.nio.file.Path> globalFiles = fileDiscoveryService.discoverFiles( nioPackagePath,
                                                                                                               FILTER_GLOBALS );
        for ( final org.kie.commons.java.nio.file.Path path : globalFiles ) {
            final String definition = ioService.readAllString( path );
            dmoBuilder.addGlobals( definition );
        }
    }

}