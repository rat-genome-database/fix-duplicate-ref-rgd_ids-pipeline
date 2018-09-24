package edu.mcw.rgd;

import edu.mcw.rgd.datamodel.RgdId;
import edu.mcw.rgd.datamodel.XdbId;
import edu.mcw.rgd.datamodel.ontology.Annotation;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.core.io.FileSystemResource;

import java.util.*;

/**
 * @author pjayaraman
 * @since 11/19/12
 */
public class fixDuplicateRefRgdIds {

    fixDuplicateRgdIdsDAO fixDupDao = new fixDuplicateRgdIdsDAO();
    private int xdbKey = XdbId.XDB_KEY_PUBMED;
    private static final Logger logStatus = Logger.getLogger("log_status");
    private static final Logger logUpdates = Logger.getLogger("log_updates");
    private String version;

    public static void main(String args[]) throws Exception{
        DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
        new XmlBeanDefinitionReader(bf).loadBeanDefinitions(new FileSystemResource("properties/AppConfigure.xml"));
        fixDuplicateRefRgdIds manager = (fixDuplicateRefRgdIds) (bf.getBean("duplicateRgdIds"));
        manager.run();
    }


    public void run() throws Exception {
        logStatus.info(getVersion());

        // map of duplicate PMIDs:  PMID to set of REF_RGD_IDs
        Map<String, Set<Integer>> pmidToList = new HashMap<>();

        //get list of xdbid objects that contain more than one ACTIVE rgdId for given pubmed Id.
        List<XdbId> xdbIdsToBeFixed = fixDupDao.getPubmedIdsWithMultipleReferenceRgdIds(xdbKey);
        logStatus.info("Count of duplicate references: "+xdbIdsToBeFixed.size());

        for( XdbId xdbId: xdbIdsToBeFixed ) {
            Set<Integer> refRgdIds = pmidToList.get(xdbId.getAccId());
            if( refRgdIds==null ) {
                refRgdIds = new HashSet<>();
                pmidToList.put(xdbId.getAccId(), refRgdIds);
            }
            refRgdIds.add(xdbId.getRgdId());
        }

        Collection<String> pmidAccKeys = pmidToList.keySet();

        //for each pmid acc, get the list of rGDIdsused.
        //for each of the rgdIds
        //1. Move the older annotations from the RgdId to the new rgdId in full annot table under Ref RGDID for those
        // that dont have the same annotations with new refRgdId.
        //2. remove old ref_key in rgd_ref_rgdId for the .
        //3. Also make the older rgdId to "withdrawn"
        //4. also enter in RGD_ID_History

        for (String pubmedAcc : pmidAccKeys) {
            Set<Integer> listOfRgdIds = pmidToList.get(pubmedAcc);
            List<RgdId> rgdObjectsList = new ArrayList<>();

            //get RGDId Object that is newest. replace the other annotations with the "replacementRgdId"
            for (int rgdIdNum : listOfRgdIds) {
                RgdId rgdObj = fixDupDao.getActiveRgdObject(rgdIdNum);
                if (rgdObj != null) {
                    rgdObjectsList.add(rgdObj);
                }
            }

            int replacementRGdId = getMostRecentRgdId(rgdObjectsList);
            logMsg("Duplicate reference for PMID:"+pubmedAcc+"; replacement RgdId = RGD:" + replacementRGdId);

            //for those rgdids that arent replacementRgdIds,
            for (int rgdNumber : listOfRgdIds) {
                if (rgdNumber == replacementRGdId) {
                    continue;
                }

                //update annotation in full Annot table
                List<Annotation> annList = fixDupDao.getListAnnotsByRefRgdId(rgdNumber);
                List<Annotation> newAnnObjList = fixDupDao.getListAnnotsByRefRgdId(replacementRGdId);

                for (Annotation annObjTobeModified : annList) {

                    int sameannotYesFlag = 0;
                    for (Annotation newAnnObj : newAnnObjList) {
                        if ((newAnnObj.getAnnotatedObjectRgdId().equals(annObjTobeModified.getAnnotatedObjectRgdId()))
                                && (newAnnObj.getRefRgdId() == replacementRGdId)
                                && (annObjTobeModified.getRefRgdId() == rgdNumber)
                                && (newAnnObj.getTermAcc().equalsIgnoreCase(annObjTobeModified.getTermAcc()))
                                && (newAnnObj.getEvidence().equalsIgnoreCase(annObjTobeModified.getEvidence()))
                                && (newAnnObj.getTerm().equalsIgnoreCase(annObjTobeModified.getTerm()))) {

                            //System.out.println("Maybe same annotation.");
                            sameannotYesFlag = 1;

                        }
                    }

                    if (sameannotYesFlag == 0) {
                        logMsg("Updating RefRgdId for Annotation: RGDID:" + annObjTobeModified.getAnnotatedObjectRgdId()
                                +" PMID:"+ pubmedAcc+" Old RefRgdId : " + rgdNumber + " NewRefRgdId: " + replacementRGdId);

                        annObjTobeModified.setRefRgdId(replacementRGdId);
                        annObjTobeModified.setLastModifiedDate(new Date());
                        fixDupDao.updateAnnotationObject(annObjTobeModified);

                        logUpdates.debug("Updated annotation: "+annObjTobeModified.dump("|"));
                    }
                }


                //get list of associated object rgdIds with the reference to be changed.
                List<Integer> rgdidsAssociatedWithRef = fixDupDao.getAnnotatedRgdidsGivenRef(rgdNumber);

                //update all those rows with the new reference
                for (Integer rgdIdOfObjAssociatedWithRef : rgdidsAssociatedWithRef) {
                    logMsg("Removing association of Old RefRgdId:" + rgdNumber + " with RgdId: " + rgdIdOfObjAssociatedWithRef);
                    fixDupDao.removeOldReferenceAssociation(rgdNumber, rgdIdOfObjAssociatedWithRef);

                    logMsg("Inserting association of New RefRgdId:" + replacementRGdId + " with RgdId: " + rgdIdOfObjAssociatedWithRef);
                    fixDupDao.insertReferenceeAssociation(rgdIdOfObjAssociatedWithRef, replacementRGdId);
                }


                //make older Rgdid "withdrawn"
                logMsg("Withdrawing Old RefRgdId: " + rgdNumber);
                fixDupDao.retireOldrefRgdId(rgdNumber);

                //record in rgdid History
                logMsg("Recording in RGD_ID_HISTORY: OldRgdId: " + rgdNumber + " replacementRgdId:" + replacementRGdId);
                fixDupDao.recordRetiredRefRgdidHistory(rgdNumber, replacementRGdId);

                logMsg("======");
            }
        }

        logStatus.info("OK!");
    }

    void logMsg(String msg) {
        logStatus.info(msg);
        logUpdates.info(msg);
    }

    private int getMostRecentRgdId(List<RgdId> rgdObjectsList) {

        int mostRecentRgdId=rgdObjectsList.get(0).getRgdId();
        Date mostRecentDate = rgdObjectsList.get(0).getCreatedDate();

        for(RgdId rgdObj : rgdObjectsList){
            if(rgdObj.getCreatedDate().compareTo(mostRecentDate)>0){
                //System.out.println("Date newer than oldest date");
                mostRecentRgdId=rgdObj.getRgdId();
                mostRecentDate = rgdObj.getCreatedDate();
            }else if(rgdObj.getCreatedDate().compareTo(mostRecentDate)==0){
                //System.out.println("date is the same");
                if(rgdObj.getRgdId()>mostRecentRgdId){
                    mostRecentRgdId=rgdObj.getRgdId();
                }
            }
        }

        return mostRecentRgdId;
    }


    public void setXdbKey(int xdbKey) {
        this.xdbKey = xdbKey;
    }

    public int getXdbKey() {
        return xdbKey;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getVersion() {
        return version;
    }
}
