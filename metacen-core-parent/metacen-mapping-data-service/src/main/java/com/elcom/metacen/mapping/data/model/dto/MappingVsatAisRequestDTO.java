package com.elcom.metacen.mapping.data.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.math.BigInteger;
import java.util.List;

@Data
@Accessors(chain = true)
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class MappingVsatAisRequestDTO {
    private List<IpMmsiRequestDTO> ipMmsiList;

    @Data
    public static class IpMmsiRequestDTO {
        private String ip;
        private BigInteger mmsi;
    }
}
