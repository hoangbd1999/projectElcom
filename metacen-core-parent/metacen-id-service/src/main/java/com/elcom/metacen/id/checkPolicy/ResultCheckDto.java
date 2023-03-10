package com.elcom.metacen.id.checkPolicy;

import java.util.Map;

public class ResultCheckDto {
    private Boolean status;
    private Boolean admin;
    private Object description;
    private String type;

    public ResultCheckDto(Map<String, Object> map){
        if(map != null && map.size() > 0){
            if(map.containsKey("status")) this.status = (Boolean) map.get("status");
            if(map.containsKey("admin")) this.admin = (Boolean) map.get("admin");
            if(map.containsKey("description")) this.description =  map.get("description");
        }
    }

    public ResultCheckDto( ) {
    }

    public ResultCheckDto(Boolean status) {
        this.status = status;
    }

    public Boolean getStatus() {
        return status;
    }

    public void setStatus(Boolean status) {
        this.status = status;
    }

    public Boolean getAdmin() {
        return admin;
    }

    public void setAdmin(Boolean admin) {
        this.admin = admin;
    }

    public Object getDescription() {
        return description;
    }

    public void setDescription(Object description) {
        this.description = description;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
