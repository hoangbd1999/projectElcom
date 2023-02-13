/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.elcom.metacen.raw.data.messaging.rabbitmq;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * @author Admin
 */
@Component
public class RabbitMQProperties {

    // User service
    @Value("${user.rpc.exchange}")
    public static String USER_RPC_EXCHANGE;

    @Value("${user.rpc.queue}")
    public static String USER_RPC_QUEUE;

    @Value("${user.rpc.key}")
    public static String USER_RPC_KEY;

    @Value("${user.rpc.authen.url}")
    public static String USER_RPC_AUTHEN_URL;

    @Value("${user.rpc.uuidLst.url}")
    public static String USER_RPC_UUIDLIST_URL;

    @Value("${user.rpc.internal.list.url}")
    public static String USER_RPC_INTERNAL_LIST_URL;

    // Menu Management Service
    @Value("${menu-management.role.menu}")
    public static String MANAGEMENT_ROLE_MENU;

    @Value("${menu-management.rpc.queue}")
    public static String MENU_MANAGEMENT_RPC_QUEUE;

    @Value("${menu-management.rpc.exchange}")
    public static String MENU_MANAGEMENT_RPC_EXCHANGE;

    @Value("${menu-management.rpc.key}")
    public static String MENU_MANAGEMENT_RPC_KEY;

    // ABAC
    @Value("${abac.rpc.exchange}")
    public static String ABAC_RPC_EXCHANGE;

    @Value("${abac.rpc.queue}")
    public static String ABAC_RPC_QUEUE;

    @Value("${abac.rpc.key}")
    public static String ABAC_RPC_KEY;

    @Value("${abac.rpc.role.url}")
    public static String ABAC_RPC_ROLE_URL;

    @Value("${abac.rpc.author.url}")
    public static String ABAC_RPC_AUTHOR_URL;

    @Value("${abac.rpc.attribute.url}")
    public static String ABAC_RPC_ATTRIBUTE_URL;

    @Value("${contact.marine-vessel.ships-info.url}")
    public static String SHIP_INFO_METACEN_URL;

    @Value("${contact.rpc.exchange}")
    public static String CONTACT_RPC_EXCHANGE;

    @Value("${contact.rpc.queue}")
    public static String CONTACT_RPC_QUEUE;

    @Value("${contact.rpc.key}")
    public static String CONTACT_RPC_KEY;

    @Value("${mapping-data.rpc.queue}")
    public static String MAPPING_RPC_QUEUE;

    @Value("${mapping-data.rpc.exchange}")
    public static String MAPPING_RPC_EXCHANGE;

    @Value("${mapping-data.rpc.key}")
    public static String MAPPING_RPC_KEY;

    @Value("${mapping-data.vsat-ais.url}")
    public static String MAPPING_RPC_GET_VSAT_AIS_METACEN_IDS_URL;


    @Autowired
    public RabbitMQProperties(@Value("${user.rpc.exchange}") String userRpcExchange,
                              @Value("${user.rpc.queue}") String userRpcQueue,
                              @Value("${user.rpc.key}") String userRpcKey,
                              @Value("${user.rpc.authen.url}") String userRpcAuthenUrl,
                              @Value("${user.rpc.uuidLst.url}") String userRpcUuidLstUrl,
                              @Value("${user.rpc.internal.list.url}") String userRpcInternalLstUrl,
                              @Value("${menu-management.role.menu}") String managementRoleMenu,
                              @Value("${menu-management.rpc.queue}") String menumanagementRpcQueue,
                              @Value("${menu-management.rpc.exchange}") String menumanagementRpcExchange,
                              @Value("${menu-management.rpc.key}") String menumanagementRpcKey,
                              @Value("${abac.rpc.exchange}") String abacRpcExchange,
                              @Value("${abac.rpc.queue}") String abacRpcQueue,
                              @Value("${abac.rpc.key}") String abacRpcKey,
                              @Value("${abac.rpc.role.url}") String abacRpcRoleUrl,
                              @Value("${abac.rpc.author.url}") String abacRpcAuthorUrl,
                              @Value("${abac.rpc.attribute.url}") String abacRpcAttribute,
                              @Value("${contact.marine-vessel.ships-info.url}") String shipInfoMetacen,
                              @Value("${contact.rpc.exchange}") String contactRpcExchange,
                              @Value("${contact.rpc.queue}") String contactRpcQueue,
                              @Value("${contact.rpc.key}") String contactRpcKey,
                              @Value("${mapping-data.rpc.queue}") String mappingRpcQueue,
                              @Value("${mapping-data.rpc.exchange}") String mappingRpcExchange,
                              @Value("${mapping-data.rpc.key}") String mappingRpcKey,
                              @Value("${mapping-data.vsat-ais.url}") String mappingRpcVsatAisUrl
    ) {
        //USER
        USER_RPC_EXCHANGE = userRpcExchange;
        USER_RPC_QUEUE = userRpcQueue;
        USER_RPC_KEY = userRpcKey;
        USER_RPC_AUTHEN_URL = userRpcAuthenUrl;
        USER_RPC_UUIDLIST_URL = userRpcUuidLstUrl;
        USER_RPC_INTERNAL_LIST_URL = userRpcInternalLstUrl;

        //MENU MANAGEMENT
        MANAGEMENT_ROLE_MENU = managementRoleMenu;
        MENU_MANAGEMENT_RPC_QUEUE = menumanagementRpcQueue;
        MENU_MANAGEMENT_RPC_EXCHANGE = menumanagementRpcExchange;
        MENU_MANAGEMENT_RPC_KEY = menumanagementRpcKey;

        //ABAC
        ABAC_RPC_EXCHANGE = abacRpcExchange;
        ABAC_RPC_QUEUE = abacRpcQueue;
        ABAC_RPC_KEY = abacRpcKey;
        ABAC_RPC_ROLE_URL = abacRpcRoleUrl;
        ABAC_RPC_AUTHOR_URL = abacRpcAuthorUrl;
        ABAC_RPC_ATTRIBUTE_URL = abacRpcAttribute;

        //CONTACT
        SHIP_INFO_METACEN_URL = shipInfoMetacen;
        CONTACT_RPC_EXCHANGE = contactRpcExchange;
        CONTACT_RPC_QUEUE = contactRpcQueue;
        CONTACT_RPC_KEY = contactRpcKey;

        //MAPPING
        MAPPING_RPC_QUEUE = mappingRpcQueue;
        MAPPING_RPC_EXCHANGE = mappingRpcExchange;
        MAPPING_RPC_KEY = mappingRpcKey;
        MAPPING_RPC_GET_VSAT_AIS_METACEN_IDS_URL = mappingRpcVsatAisUrl;
    }
}
