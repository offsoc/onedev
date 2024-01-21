package io.onedev.server.plugin.pack.npm;

import io.onedev.commons.loader.AbstractPluginModule;
import io.onedev.server.pack.PackService;
import io.onedev.server.pack.PackSupport;

/**
 * NOTE: Do not forget to rename moduleClass property defined in the pom if you've renamed this class.
 *
 */
public class NpmModule extends AbstractPluginModule {

	@Override
	protected void configure() {
		super.configure();

		bind(NpmPackService.class);
		contribute(PackService.class, NpmPackService.class);
		contribute(PackSupport.class, new NpmPackSupport());
	}

}
