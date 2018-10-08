// ---------------------------------------------------------------------------
// Proxy Setup for H2
// ---------------------------------------------------------------------------


// Optional step
// Create a specific user and specific schema for DbClassload
// Any schema and user can be used:
// As long as the schema can contain a table or view that complies to the given below  
CREATE USER IF NOT EXISTS PROXY PASSWORD 'Az3ts';
CREATE SCHEMA IF NOT EXISTS PROXY AUTHORIZATION SA;

drop table PROXY.SERVLET;

// This tbale contains the servlets (name, and URL pattern) 
CREATE CACHED TABLE IF NOT EXISTS  PROXY.SERVLET(SERVLET_NAME VARCHAR(128) NOT NULL,
		                                 PROXY_TYPE VARCHAR(16) DEFAULT 'Async',
		                                 URL_PATTERN VARCHAR(512) NOT NULL, 
	                                     INIT_PARAMETERS VARCHAR(1024),  
	                                     LOAD_ON_STARTUP INTEGER DEFAULt -1,

                                         EMPTY_ROLE_SEMANTIC VARCHAR(16) DEFAULT 'PERMIT',
                                         TRANSPORT_GUARANTEE VARCHAR(16) DEFAULT 'NONE',
                                         ROLE_NAMES VARCHAR(128),
	                                  
	                                  PRIMARY KEY (SERVLET_NAME));

GRANT ALL ON PROXY.SERVLET TO PROXY;
			


drop table PROXY.PROXY;

CREATE CACHED TABLE IF NOT EXISTS  PROXY.PROXY(SERVLET_NAME VARCHAR(128) NOT NULL,
		                                        LOCATION VARCHAR(512) NOT NULL,
		                                        SOCKET_TIMEOUT INTEGER DEFAULT 12000,
		                                        CONNECT_TIMEOUT INTEGER DEFAULT 3000,
		                                        EXPIRES DATE DEFAULT NULL,
		                                        REDIRECT_SC INTEGER DEFAULT 307,
		                                        
	                                            PRIMARY KEY (SERVLET_NAME, LOCATION));

GRANT ALL ON PROXY.PROXY TO PROXY;
 
 