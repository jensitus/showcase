package at.cibseven.cibdemo.config;

import org.springframework.context.annotation.Configuration;

@Configuration
public class FilterConfiguration {

//    @Bean
//    public FilterRegistrationBean<ProcessEngineAuthenticationFilter> AuthenticationFilter(JerseyApplicationPath applicationPath) {
//        FilterRegistrationBean<ProcessEngineAuthenticationFilter> registrationBean = new FilterRegistrationBean<>();
//        registrationBean.setName("cibseven-composite-auth");
//        registrationBean.setFilter(new ProcessEngineAuthenticationFilter());
//        registrationBean.setOrder(10);// Order of execution if multiple filters
//
//        String restApiPathPattern = applicationPath.getPath();
//
//        // Apply to all URLs under engine-rest except /engine-rest/identity/verify
//        String[] urlPatterns = Arrays.asList(
//                "/process-definition/*",
//                "/process-instance/*",
//                "/history/*",
//                "/execution/*",
//                "/batch/*",
//                "/decision-definition/*",
//                "/deployment/*",
//                "/filter/*",
//                "/incident/*",
//                "/job-definition/*",
//                "/job/*",
//                "/telemetry/*",
//                "/metrics/*",
//                "/authorization/*",
//                "/group/*",
//                "/user/*",
//                "/message/*",
//                "/event-subscription/*",
//                "/variable-instance/*",
//                "/task/*",
//                "/engine/*",
//                "/identity/groups"
//        ).stream().map(pattern -> addUrl(restApiPathPattern, pattern)).toArray(String[]::new);
//
//        // Enable async support
//        registrationBean.setAsyncSupported(true);
//        // Init parameters
//        registrationBean.addInitParameter(
//                "authentication-provider",
//                org.cibseven.bpm.engine.rest.security.auth.impl.CompositeAuthenticationProvider.class.getName()
//        );
//
//        registrationBean.addUrlPatterns(urlPatterns);
//        return registrationBean;
//    }

    private String addUrl(String base, String extend) {
        return (base + extend).replaceFirst("^(\\/+|([^/]))", "/$2");
    }

}
