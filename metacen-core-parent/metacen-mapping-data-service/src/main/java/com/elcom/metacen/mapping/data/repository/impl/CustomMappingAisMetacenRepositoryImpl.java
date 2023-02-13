package com.elcom.metacen.mapping.data.repository.impl;

import com.elcom.metacen.mapping.data.model.MappingAisMetacen;
import com.elcom.metacen.mapping.data.model.MappingVsatMetacen;
import com.elcom.metacen.mapping.data.model.dto.MappingAisFilterDTO;
import com.elcom.metacen.mapping.data.model.dto.MappingAisResponseDTO;
import com.elcom.metacen.mapping.data.model.dto.MappingVsatResponseDTO;
import com.elcom.metacen.mapping.data.repository.CustomMappingAisMetacenRepository;
import com.elcom.metacen.utils.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.aggregation.*;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.TextCriteria;
import org.springframework.data.mongodb.core.query.TextQuery;
import org.springframework.stereotype.Component;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import static com.elcom.metacen.enums.ObjectType.VEHICLE;
import static com.elcom.metacen.mapping.data.constant.MappingFieldConstant.*;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.*;

/**
 * @author Admin
 */
@Component
public class CustomMappingAisMetacenRepositoryImpl extends BaseCustomRepositoryImpl<MappingAisMetacen> implements CustomMappingAisMetacenRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(CustomMappingAisMetacenRepositoryImpl.class);

    @Override
    public Page<MappingAisResponseDTO> search(MappingAisFilterDTO mappingAisFilterDTO, Pageable pageable) {
        Criteria criteria;
        criteria = Criteria.where("id").ne(null);

        List<Criteria> andCriterias = new ArrayList<>();
        if (!StringUtil.isNullOrEmpty(mappingAisFilterDTO.getTerm())) {
            String term = ".*" + mappingAisFilterDTO.getTerm().trim() + ".*";
            Criteria termCriteria = new Criteria();
            termCriteria.orOperator(
                    Criteria.where("aisMmsi").regex(term, "i"),
                    Criteria.where("aisShipName").regex(term, "i"),
                    Criteria.where("objectType").regex(term, "i"),
                    Criteria.where("objectId").regex(term, "i"),
                    Criteria.where("objectName").regex(term, "i")
            );
            andCriterias.add(termCriteria);
        }
        if (mappingAisFilterDTO.getAisMmsi() != null) {
            andCriterias.add(Criteria.where("aisMmsi").in(mappingAisFilterDTO.getAisMmsi()));
        }
        if (mappingAisFilterDTO.getObjectTypes() != null && !mappingAisFilterDTO.getObjectTypes().isEmpty()) {
            andCriterias.add(Criteria.where("objectType").in(mappingAisFilterDTO.getObjectTypes()));
        }
        if (!StringUtil.isNullOrEmpty(mappingAisFilterDTO.getObjectId())) {
            andCriterias.add(Criteria.where("objectId").regex(".*" + mappingAisFilterDTO.getObjectId().trim() + ".*", "i"));
        }

        Criteria allCriteria = criteria;
        if (andCriterias.size() > 0) {
            allCriteria = allCriteria.andOperator(andCriterias.stream().toArray(Criteria[]::new));
        }

        // matchStage
        MatchOperation matchStage = Aggregation.match(allCriteria);

        // sortStage
        SortOperation sortStage = sort(Sort.Direction.DESC, "created_date");
        if (!StringUtil.isNullOrEmpty(mappingAisFilterDTO.getSort())) {
            String sortItem = mappingAisFilterDTO.getSort();
            if (sortItem.substring(0, 1).equals("-")) {
                sortStage = sort(Sort.Direction.DESC, sortItem.substring(1));
            } else {
                sortStage = sort(Sort.Direction.ASC, sortItem);
            }
        }
        Aggregation aggregation = Aggregation.newAggregation(
                matchStage,
                sortStage,
                project("uuid", "aisMmsi", "aisShipName", "objectType", "objectId", "objectUuid", "objectName",
                        "createdBy", "createdDate", "modifiedBy", "modifiedDate"),
                Aggregation.skip((long) pageable.getPageNumber() * pageable.getPageSize()),
                Aggregation.limit(pageable.getPageSize())
        );
        AggregationResults<MappingAisResponseDTO> output = mongoOps.aggregate(aggregation, MappingAisMetacen.class, MappingAisResponseDTO.class);
        List<MappingAisResponseDTO> results = output.getMappedResults();

        // total
        long total = mongoOps.count(Query.query(allCriteria).limit(-1).skip(-1), domain);

        return new PageImpl<>(results, pageable, total);
    }

    @Override
    public List<MappingAisResponseDTO> getMappingAisByMmsiLst(List<BigInteger> mmsiLst) {
        Criteria criteria = new Criteria(AIS_MMSI).in(mmsiLst)
                .and(OBJECT_TYPE).is(VESSEL);
        MatchOperation match = new MatchOperation(criteria);
        GroupOperation group = group(AIS_MMSI).first(OBJECT_ID).as(OBJECT_ID);
//        GroupOperation group = new GroupOperation(Fields.fields(VSAT_IP_ADDRESS))
        ProjectionOperation project = Aggregation.project()
                .andExpression("_id").as(AIS_MMSI)
                .andExpression(OBJECT_ID).as( "objectId");
        Aggregation aggregation = Aggregation.newAggregation(match, group, project);
        return (List<MappingAisResponseDTO>) mongoOps.aggregate(aggregation, MappingAisMetacen.class, MappingAisResponseDTO.class).getMappedResults();
    }
}
