package myproject.mcpclient;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Iterator;
import java.util.Map;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.ServerParameters;
import io.modelcontextprotocol.client.transport.StdioClientTransport;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.ClientCapabilities;
import io.modelcontextprotocol.spec.McpSchema.TextContent;

@Component(immediate=true)
public class McpClientComponent {

	private static Logger logger = LoggerFactory.getLogger(McpClientComponent.class);
	
	private static Path SERVER_APP_JAR = Paths.get("bndtools.demo.mcpserver","generated","bndtools.demo.mcpserver.bndrun.jar");

	private static String BNDTOOLS_WORKSPACETEMPLATES_ADDING_TOOLS = "org.bndtools.mcp.tools.workspacetemplate.WorkspaceTemplateURLAddingTools.";
	
	// Client created in activate()
	private McpSyncClient client;
	
	@Activate
	void activate() throws Exception {
		// The following ServerParameters setup launches as a java application an OSGi-implemented MCP Server in project
		// bndtools.demo.mcpserver/generated/bndtools.demo.mcpserver.bndrun.jar
		
		// The command is ${java.home}/bin/java
		ServerParameters.Builder spBuilder = ServerParameters.builder(Path.of(System.getProperty("java.home"), "bin", "java").toAbsolutePath().toString());
		// this is jar in ../bndtools.demo.mcpserver/generated/bndtools.demo.mcpserver.bndrun.jar
		String classpath_jar = Paths.get("").toAbsolutePath().getParent().resolve(SERVER_APP_JAR).toAbsolutePath().toString();
		// Add the classpath_jar to the classpath for the server launch.  Also add EmbeddedLauncher class as main
		spBuilder.args("-classpath", "\"" + classpath_jar + "\"" ,"aQute.launcher.pre.EmbeddedLauncher");
		
		// Create StdioClient transport from ServerParameters Builder
		StdioClientTransport transport = new StdioClientTransport(spBuilder
			    .build());
		
		// Create client with stdio transport and some reasonable default timeouts for stdio
		client = McpClient.sync(transport)
		    .requestTimeout(Duration.ofSeconds(1000))
		    .capabilities(ClientCapabilities.builder()
		        .build())
	        .build();
		
		// start and connect to stdio server. This will launch the server defined in ServerParameters and then use the Stdio
		// transport to communicate with it/initialize the connection.
		client.initialize();
		// Completion means that client successfully started the server, connected to MCP server via stdio transport
		logger.debug("MCPCLIENT initialized");
		
		// TEST list tools from server
		// This will show the tools and metadata for each that the MCP Server has via the imported remote service
		client.listTools().tools().forEach(t -> logger.debug("MCPCLIENT seeing tool=" + t.toString()));
		
		// TEST of the usage of the two tools exposed by getExistingWorkspaceTemplateURLs service
		String tool_name1 = BNDTOOLS_WORKSPACETEMPLATES_ADDING_TOOLS + "getExistingWorkspaceTemplateURLs";
		logger.debug("MCPCLIENT: Calling tool:  "+ tool_name1 + "(" + null + ")");
		CallToolResult getResult = client.callTool(
			    new CallToolRequest(tool_name1, 
			        (Map<String, Object>) null)
			);
		String jsonResult = ((TextContent) getResult.content().get(0)).text();
		logger.debug("MCPCLIENT: call tool=" + tool_name1 + " json result=" + jsonResult);
		try {
			JsonNode uris = new ObjectMapper().readTree(jsonResult).findValue("uris");
			if (uris.isArray()) {
				for (Iterator<JsonNode> i = uris.iterator(); i.hasNext(); ) {
					JsonNode item = i.next();
					logger.debug("MCPCLIENT: WorkspaceTemplateURI=" + item.asText());
				}
			}
		} catch (JsonProcessingException e) {
			logger.error("MCPCLIENT: exception causing json processing exception", e);
		}
		// Then add a workspace template url to the bndtools/Eclipse preferences...where it will
		// then appear for the user when they subsequently go to create a new workspace
		String tool_name2 = BNDTOOLS_WORKSPACETEMPLATES_ADDING_TOOLS + "addWorkspaceTemplateURL";
		String uri = "https://github.com/ECF/bndtools.workspace";
		Map<String, Object> args = Map.of("workspaceTemplateURL", uri, "name", "", "branch", "");
		logger.debug("MCPCLIENT: Calling tool:  "+ tool_name2 + "(" + args + ")");
		CallToolResult result = client.callTool(
			    new CallToolRequest(tool_name2, 
			        args)
			);
		String resultString = ((TextContent) result.content().get(0)).text();
		if (Boolean.parseBoolean(resultString)) {
			logger.debug("MCPCLIENT: Added URI: " + uri + " to running bndtools");
		} else {
			logger.debug("MCPCLIENT: URI: " + uri + " NOT added to running bndtools");
		}
	}
	
	@Deactivate
	void deactivate() {
		if (this.client != null) {
			this.client.closeGracefully();
			this.client = null;
		}
	}
	
}
