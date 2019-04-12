package edu.mcw.rgd;

import edu.mcw.rgd.dao.impl.AnnotationDAO;
import edu.mcw.rgd.dao.impl.AssociationDAO;
import edu.mcw.rgd.dao.impl.RGDManagementDAO;
import edu.mcw.rgd.dao.impl.ReferenceDAO;
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
    ReferenceDAO refDao = new ReferenceDAO();
    AssociationDAO assDao = new AssociationDAO();

    public String getConnectionInfo() {
        return refDao.getConnectionInfo();
    }

    //get all active rgdids that are more than one rgdid for given pubmed Id.
    public List<XdbId> getPubmedIdsWithMultipleReferenceRgdIds(int xdbKey) throws Exception{

        String query = "SELECT acc.*,i.*\n" +
                "FROM rgd_acc_xdb acc, references ref, rgd_ids i\n" +
                "WHERE acc.rgd_id=ref.rgd_id\n" +
                "and acc.rgd_id=i.rgd_id\n" +
                "and i.OBJECT_STATUS='ACTIVE'\n" +
                "and acc.XDB_KEY=? and \n" +
                "acc.ACC_ID in\n" +
                "(\n" +
                "select x.ACC_ID\n" +
                "FROM rgd_acc_xdb x, references r, rgd_ids rgd\n" +
                "where x.rgd_id=r.rgd_id\n" +
                "and x.rgd_id=rgd.rgd_id\n" +
                "and rgd.OBJECT_STATUS='ACTIVE'\n" +
                "and x.XDB_KEY=?\n" +
                "group by x.ACC_ID\n" +
                "having count(x.ACC_ID) > 1\n" +
                ")\n" +
                "order by acc.ACC_ID";

        XdbQuery xq = new XdbQuery(refDao.getDataSource(), query);
        return refDao.execute(xq, xdbKey, xdbKey);
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
