package elcom.com.neo4j.config;

//import org.apache.spark.SparkConf;
//import org.apache.spark.api.java.JavaSparkContext;
//import org.apache.spark.sql.SparkSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Created by Prakrish
 */
//@Configuration
//@PropertySource("classpath:config/application-common.properties")
public class ApplicationConfig {
//
//    @Value("${app.name:spark-boot2}")
//    private String appName;
//
//    @Value("${spark.home}")
//    private String sparkHome;
//
//    @Value("${master.uri:local}")
//    private String masterUri;
//
//    @Bean
//    public SparkConf sparkConf() {
//        SparkConf sparkConf = new SparkConf()
//                .setAppName(appName)
//                .setSparkHome(sparkHome)
//                .setMaster(masterUri);
//
//        return sparkConf;
//    }
//
//    @Bean
//    public JavaSparkContext javaSparkContext() {
//        return new JavaSparkContext(sparkConf());
//    }
//
//    @Bean
//    public SparkSession sparkSession() {
//        return SparkSession
//                .builder()
//                .sparkContext(javaSparkContext().sc())
//                .appName("Java Spark SQL basic example")
//                .getOrCreate();
//    }
//
//    @Bean
//    public static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
//        return new PropertySourcesPlaceholderConfigurer();
//    }
//    @Bean("ReProcessing")
//    public TaskExecutor getAsyncExecutor() {
//        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
//        executor.setCorePoolSize(2);
//        executor.setMaxPoolSize(100000);
//        executor.setWaitForTasksToCompleteOnShutdown(true);
//        executor.setThreadNamePrefix("Async-");
//        return executor;
//    }
//    @Bean("saveNode")
//    public TaskExecutor getSaveNodeExecutor() {
//        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
//        executor.setCorePoolSize(2);
//        executor.setMaxPoolSize(100000);
//        executor.setWaitForTasksToCompleteOnShutdown(true);
//        executor.setThreadNamePrefix("Async saveNode-");
//        return executor;
//    }

}