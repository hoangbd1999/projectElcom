package com.elcom.metacen.raw.data.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigInteger;
import java.util.List;

@Data
@Accessors(chain = true)
@JsonIgnoreProperties
public class MappingVsatAisResponse {
    private BigInteger mmsi;
    private String mappingId;
}
