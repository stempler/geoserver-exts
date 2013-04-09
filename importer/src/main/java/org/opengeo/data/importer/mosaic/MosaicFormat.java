package org.opengeo.data.importer.mosaic;

import java.io.IOException;
import java.util.List;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.catalog.DimensionInfo;
import org.geoserver.catalog.DimensionPresentation;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.impl.DimensionInfoImpl;
import org.geotools.data.DataUtilities;
import org.geotools.gce.imagemosaic.ImageMosaicFormat;
import org.opengeo.data.importer.GridFormat;
import org.opengeo.data.importer.ImportData;
import org.opengeo.data.importer.ImportItem;
import org.opengeo.data.importer.job.ProgressMonitor;

public class MosaicFormat extends GridFormat {

    public MosaicFormat() {
        super(ImageMosaicFormat.class);
    }

    @Override
    public CoverageStoreInfo createStore(ImportData data, WorkspaceInfo workspace, Catalog catalog) throws IOException {
        MosaicIndex index = new MosaicIndex((Mosaic) data);
        index.write();

        CoverageStoreInfo store = super.createStore(data, workspace, catalog);
        store.setURL(DataUtilities.fileToURL(index.getFile()).toString());
        return store;
    }

    @Override
    public List<ImportItem> list(ImportData data, Catalog catalog, ProgressMonitor monitor) throws IOException {
        
        List<ImportItem> items = super.list(data, catalog, monitor);

        Mosaic m = (Mosaic) data;
        if (m.getTimeMode() != TimeMode.NONE) {
            //set up the time dimension object
            for (ImportItem item : items) {
                DimensionInfo dim = new DimensionInfoImpl();
                dim.setEnabled(true);
                dim.setAttribute("time");
                dim.setPresentation(DimensionPresentation.LIST);
                dim.setUnits("ISO8601"); //TODO: is there an enumeration for this?

                ResourceInfo r = item.getLayer().getResource();
                r.getMetadata().put(ResourceInfo.TIME, dim);
            }
        }
        
        return items;
    }
}
