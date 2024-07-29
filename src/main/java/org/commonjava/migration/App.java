package org.commonjava.migration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class App
{

    public static void main(String[] args) {

        final Logger logger = LoggerFactory.getLogger("Main App");

        logger.info("Hello, Migration!");

        CassandraMigrationExcutor excutor = new CassandraMigrationExcutor();
        try
        {
            excutor.export();
        }
        catch (Exception e)
        {
            logger.error("Migration failure.", e);
        }
    }

}
