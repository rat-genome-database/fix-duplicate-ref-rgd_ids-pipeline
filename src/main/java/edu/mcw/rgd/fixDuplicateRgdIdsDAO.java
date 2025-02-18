package edu.mcw.rgd;

import edu.mcw.rgd.dao.impl.AnnotationDAO;
import edu.mcw.rgd.dao.impl.AssociationDAO;
import edu.mcw.rgd.dao.impl.RGDManagementDAO;
import edu.mcw.rgd.dao.spring.XdbQuery;
import edu.mcw.rgd.datamodel.RgdId;
import edu.mcw.rgd.datamodel.XdbId;
import edu.mcw.rgd.datamodel.ontology.Annotation;

import java.util.List;

/**
 * @author pjayaraman
 * @since 11/19/12
 */
public class fixDuplicateRgdIdsDAO {
    AnnotationDAO annDao = new AnnotationDAO();
    RGDManagementDAO rgdDao = new RGDManagementDAO();
    AssociationDAO assDao = new AssociationDAO();

    public String getConnectionInfo() {
        return annDao.getConnectionInfo();
    }

    //get all active rgdids that are more than one rgdid for given pubmed Id.
    public List<XdbId> getPubmedIdsWithMultipleReferenceRgdIds() throws Exception{

        String query = """
            SELECT a.*  FROM rgd_acc_xdb a, references r, rgd_ids i
            WHERE a.rgd_id=r.rgd_id  AND  a.rgd_id=i.rgd_id
             AND i.object_status='ACTIVE'  AND   a.xdb_key=2
             AND a.ACC_ID IN(
              SELECT acc_id FROM rgd_acc_xdb x, references f, rgd_ids d
              WHERE x.rgd_id=f.rgd_id  AND  x.rgd_id=d.rgd_id
                AND d.object_status='ACTIVE'  AND  x.xdb_key=2
              GROUP BY x.acc_id HAVING COUNT(x.acc_id) > 1
            )
            ORDER BY a.acc_id";
            """;

        XdbQuery q = new XdbQuery(annDao.getDataSource(), query);
        return annDao.execute(q);
    }

    public List<Annotation> getListAnnotsByRefRgdId(int refRgdid) throws Exception {
        return annDao.getAnnotationsByReference(refRgdid);
    }

    public int updateAnnotationObject(Annotation modifiedAnnotation) throws Exception {
        return annDao.updateAnnotation(modifiedAnnotation);
    }

    public void removeOldReferenceAssociation(int oldRefRgdId, int objectRgdId) throws Exception{
        assDao.removeReferenceAssociation(objectRgdId, oldRefRgdId);
    }

    public void insertReferenceeAssociation(int rgdId, int refRgdId) throws Exception{
        assDao.insertReferenceeAssociation(rgdId, refRgdId);
    }

    public List<Integer> getAnnotatedRgdidsGivenRef(int refRgdId) throws Exception{

        return assDao.getObjectsAssociatedWithReference(refRgdId);
    }

    public RgdId getActiveRgdObject(int rgdId) throws Exception{

        RgdId rgdObj =  rgdDao.getRgdId2(rgdId);
        if(rgdObj.getObjectStatus().equals("ACTIVE")){
            return rgdObj;
        }else {
            return null;
        }
    }

    public void retireOldrefRgdId(int rgdNumber) throws Exception{
        rgdDao.retire(getActiveRgdObject(rgdNumber));
    }

    public void recordRetiredRefRgdidHistory(int retiredRefRgdid, int replacementRgdid) throws Exception{
        rgdDao.recordIdHistory(retiredRefRgdid, replacementRgdid);
    }
}
