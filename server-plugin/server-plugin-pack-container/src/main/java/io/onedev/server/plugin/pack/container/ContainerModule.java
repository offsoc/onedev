package io.onedev.server.plugin.pack.container;

import java.util.Collection;

import com.google.common.collect.Sets;

import io.onedev.commons.loader.AbstractPluginModule;
import io.onedev.commons.loader.ImplementationProvider;
import io.onedev.server.OneDev;
import io.onedev.server.buildspec.step.PublishReportStep;
import io.onedev.server.jetty.ServletConfigurator;
import io.onedev.server.pack.PackSupport;
import io.onedev.server.security.FilterChainConfigurator;
import org.eclipse.jetty.servlet.ServletHolder;

/**
 * NOTE: Do not forget to rename moduleClass property defined in the pom if you've renamed this class.
 *
 */
public class ContainerModule extends AbstractPluginModule {

	@Override
	protected void configure() {
		super.configure();

		contribute(PackSupport.class, new ContainerPackSupport());
		bind(ContainerServlet.class);

		contribute(ServletConfigurator.class, context -> {
			context.addServlet(
					new ServletHolder(OneDev.getInstance(ContainerServlet.class)),
					ContainerServlet.PATH + "/*");
		});

		bind(ContainerAuthenticationFilter.class);
		contribute(FilterChainConfigurator.class, filterChainManager -> {
			filterChainManager.addFilter("containerAuthc",
					OneDev.getInstance(ContainerAuthenticationFilter.class));
			filterChainManager.createChain(
					ContainerServlet.PATH + "/**",
					"noSessionCreation, containerAuthc");
		});		
	}

}
