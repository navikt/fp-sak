package no.nav.foreldrepenger.web.server.jetty;

import java.io.File;
import java.io.IOException;
import java.security.Security;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.security.auth.message.config.AuthConfigFactory;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.geronimo.components.jaspi.AuthConfigFactoryImpl;
import org.eclipse.jetty.annotations.AnnotationConfiguration;
import org.eclipse.jetty.jaas.JAASLoginService;
import org.eclipse.jetty.plus.webapp.EnvConfiguration;
import org.eclipse.jetty.plus.webapp.PlusConfiguration;
import org.eclipse.jetty.security.ConstraintSecurityHandler;
import org.eclipse.jetty.security.DefaultIdentityService;
import org.eclipse.jetty.security.SecurityHandler;
import org.eclipse.jetty.security.jaspi.JaspiAuthenticatorFactory;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.ForwardedRequestCustomizer;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.util.resource.ResourceCollection;
import org.eclipse.jetty.webapp.Configuration;
import org.eclipse.jetty.webapp.MetaData;
import org.eclipse.jetty.webapp.WebAppConfiguration;
import org.eclipse.jetty.webapp.WebAppContext;
import org.eclipse.jetty.webapp.WebInfConfiguration;
import org.eclipse.jetty.webapp.WebXmlConfiguration;
import org.slf4j.MDC;

abstract class AbstractJettyServer {
    private static final String SERVER_HOST = "0.0.0.0";

    /**
     * Legges først slik at alltid resetter context før prosesserer nye requests.
     * Kjøres først så ikke risikerer andre har satt Request#setHandled(true).
     */
    static final class ResetLogContextHandler extends AbstractHandler {
        @Override
        public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) {
            MDC.clear();
        }
    }

    /**
     * nedstrippet sett med Jetty configurations for raskere startup.
     */
    protected static final Configuration[] CONFIGURATIONS = new Configuration[] {
            new WebAppConfiguration(),
            new WebInfConfiguration(),
            new WebXmlConfiguration(),
            new AnnotationConfiguration(),
            new EnvConfiguration(),
            new PlusConfiguration(),
    };
    private final JettyWebKonfigurasjon webKonfigurasjon;

    public AbstractJettyServer(JettyWebKonfigurasjon webKonfigurasjon) {
        this.webKonfigurasjon = webKonfigurasjon;
    }

    protected void bootStrap() throws Exception {
        konfigurer();
        migrerDatabaser();
        start(webKonfigurasjon);
    }

    protected void konfigurer() throws Exception {
        konfigurerMiljø();
        konfigurerSikkerhet();
        konfigurerJndi();
        konfigurerJms();
    }

    private void konfigurerJms() throws Exception {
        JmsKonfig.settOppJndiConnectionfactory("jms/ConnectionFactory", "mqGateway02");
        JmsKonfig.settOppJndiMessageQueue("jms/QueueFpsakOkonomiOppdragSend", "fpsak.okonomi.oppdrag.send");
        JmsKonfig.settOppJndiMessageQueue("jms/QueueFpsakOkonomiOppdragMotta", "fpsak.okonomi.oppdrag.mottak");
        JmsKonfig.settOppJndiMessageQueue("jms/QueueFpsakGrensesnittavstemmingSend", "ray.avstem.data", true);
    }

    protected abstract void konfigurerMiljø() throws Exception;

    protected void konfigurerSikkerhet() {
        Security.setProperty(AuthConfigFactory.DEFAULT_FACTORY_SECURITY_PROPERTY, AuthConfigFactoryImpl.class.getCanonicalName());

        var jaspiConf = new File(System.getProperty("conf", "./conf") + "/jaspi-conf.xml");
        if (!jaspiConf.exists()) {
            throw new IllegalStateException("Missing required file: " + jaspiConf.getAbsolutePath());
        }
        System.setProperty("org.apache.geronimo.jaspic.configurationFile", jaspiConf.getAbsolutePath());
    }

    protected abstract void konfigurerJndi() throws Exception;

    protected abstract void migrerDatabaser();

    protected void start(JettyWebKonfigurasjon jettyWebKonfigurasjon) throws Exception {
        var server = new Server(jettyWebKonfigurasjon.getServerPort());
        server.setConnectors(createConnectors(jettyWebKonfigurasjon, server).toArray(new Connector[] {}));
        var handlers = new HandlerList(new ResetLogContextHandler(), createContext(jettyWebKonfigurasjon));
        server.setHandler(handlers);
        server.start();
        server.join();
    }

    protected List<Connector> createConnectors(JettyWebKonfigurasjon jettyWebKonfigurasjon, Server server) {
        List<Connector> connectors = new ArrayList<>();
        var httpConnector = new ServerConnector(server, new HttpConnectionFactory(createHttpConfiguration()));
        httpConnector.setPort(jettyWebKonfigurasjon.getServerPort());
        httpConnector.setHost(SERVER_HOST);
        connectors.add(httpConnector);

        return connectors;
    }

    protected WebAppContext createContext(JettyWebKonfigurasjon webKonfigurasjon) throws IOException {
        var webAppContext = new WebAppContext();
        webAppContext.setParentLoaderPriority(true);

        // må hoppe litt bukk for å hente web.xml fra classpath i stedet for fra
        // filsystem.
        String descriptor;
        try (var resource = Resource.newClassPathResource("/WEB-INF/web.xml")) {
            descriptor = resource.getURI().toURL().toExternalForm();
        }
        webAppContext.setDescriptor(descriptor);
        webAppContext.setBaseResource(createResourceCollection());
        webAppContext.setContextPath(webKonfigurasjon.getContextPath());
        webAppContext.setConfigurations(CONFIGURATIONS);
        webAppContext.setAttribute("org.eclipse.jetty.server.webapp.WebInfIncludeJarPattern", "^.*resteasy-.*.jar$|^.*felles-.*.jar$");
        webAppContext.setSecurityHandler(createSecurityHandler());

        webAppContext.setParentLoaderPriority(true);
        updateMetaData(webAppContext.getMetaData());
        return webAppContext;
    }

    private ResourceCollection createResourceCollection() {
        return new ResourceCollection(
            Resource.newClassPathResource("META-INF/resources/webjars/"),
            Resource.newClassPathResource("/web"));
    }

    private void updateMetaData(MetaData metaData) {
        // Find path to class-files while starting jetty from development environment.
        var resources = getWebInfClasses().stream()
            .map(c -> Resource.newResource(c.getProtectionDomain().getCodeSource().getLocation()))
            .distinct()
            .collect(Collectors.toList());

        metaData.setWebInfClassesResources(resources);
    }

    protected abstract List<Class<?>> getWebInfClasses();

    protected HttpConfiguration createHttpConfiguration() {
        // Create HTTP Config
        var httpConfig = new HttpConfiguration();

        // Add support for X-Forwarded headers
        httpConfig.addCustomizer(new ForwardedRequestCustomizer());

        return httpConfig;
    }

    private static SecurityHandler createSecurityHandler() {
        var securityHandler = new ConstraintSecurityHandler();
        securityHandler.setAuthenticatorFactory(new JaspiAuthenticatorFactory());

        var loginService = new JAASLoginService();
        loginService.setName("jetty-login");
        loginService.setLoginModuleName("jetty-login");
        loginService.setIdentityService(new DefaultIdentityService());
        securityHandler.setLoginService(loginService);

        return securityHandler;
    }
}
