package bndtools.demo.mcpserver;

import org.bndtools.mcp.tools.workspacetemplate.WorkspaceTemplateURLAddingTools;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.transport.StdioServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema.ServerCapabilities;

@Component(immediate=true)
public class BndtoolsDemoMCPServer {

	@Reference
	private WorkspaceTemplateURLAddingTools tools;
	private ServiceToolGroup toolGroup;
	private McpSyncServer server;

	@Activate
	void activate() throws Exception {
		// Create server
		this.server = McpServer.sync(new StdioServerTransportProvider(new ObjectMapper(), System.in, System.out))
				.serverInfo("WorkspaceTemplateURLAddingTools-server", "1.0.0")
				.capabilities(ServerCapabilities.builder().logging().tools(true).build())
				.build();
		// create tool group for WorkspaceTemplateURLAddingTools
		this.toolGroup =  new ServiceToolGroup(tools, WorkspaceTemplateURLAddingTools.class);
		// Add arithmetic sync tools from remote service to server
		this.toolGroup.addSyncTools(this.server);
	}

	@Deactivate
	void deactivate() throws Exception {
		if (this.toolGroup != null) {
			this.toolGroup.dispose();
			this.toolGroup = null;
		}
		if (this.server != null) {
			this.server.closeGracefully();
			this.server = null;
		}
	}
}
