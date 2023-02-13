package com.elcom.metacen.raw.data.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.experimental.Accessors;
import org.springframework.data.mongodb.core.mapping.Field;

import javax.validation.constraints.Size;
import java.math.BigInteger;

@Data
@Accessors(chain = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class MarineVesselInfoDTO {
    private String uuid;
    private BigInteger mmsi;
    private String id;
    private String name;
    private String imo;
    private Long countryId;
    private String typeId;
    private Double dimA;
    private Double dimC;
    private String payroll;
    private String description;
    private String equipment;
    private Long draught;
    private Double grossTonnage;
    private Double speedMax;
    private int isDeleted;
    private String sideId;
}
