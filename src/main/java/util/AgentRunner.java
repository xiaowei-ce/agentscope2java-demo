package util;

import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.event.*;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.UserMessage;
import io.agentscope.core.model.Model;
import io.agentscope.core.model.ModelCreationContext;
import io.agentscope.core.model.ModelRegistry;
import io.agentscope.core.state.JsonFileAgentStateStore;
import io.agentscope.core.tool.Toolkit;
import io.agentscope.core.tool.builtin.TodoTools;
import io.agentscope.harness.agent.HarnessAgent;
import io.agentscope.harness.agent.memory.compaction.CompactionConfig;
import lombok.Getter;
import lombok.Setter;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


public class AgentRunner {

    @Getter @Setter
    private volatile Boolean runFlag = true;

    @Getter
    private final static AgentRunner runner = new AgentRunner();

    private final java.io.PrintStream out = System.out;
    private final SafeObjectMapper som = new SafeObjectMapper();
    
    private final CompactionConfig compactionConfig = CompactionConfig.builder()
            .triggerMessages(30)
            .keepMessages(10).build();

    private final Model deepseek_flash = ModelRegistry.resolve(
            "deepseek:deepseek-flash",
            ModelCreationContext.builder()
                    .enableThinking(false)
                    .baseUrl("https://api.deepseek.com")
                    .apiKey(System.getenv("DEEPSEEK_API_KEY"))
                    .stream(true)
                    .build()
    );

    private final Toolkit toolkit = new Toolkit();
    {
        toolkit.registerTool(new Tools());
        toolkit.registerTool(new TodoTools());
    }

    private final HarnessAgent agent = HarnessAgent.builder()
            .name("deepseek-agent")
            .sysPrompt("You are an intelligent assistant, and you need to answer the user's questions concisely.")
            .model(deepseek_flash)
            .workspace(Path.of(".agent/workspace"))
            .stateStore(new JsonFileAgentStateStore(Path.of(".agent/agentstate")))
            .toolkit(toolkit)
            .compaction(compactionConfig).build();


    public void run(String msg, RuntimeContext context){
        agent.streamEvents(msg, context).doOnNext(agentEvent -> {

            switch (agentEvent){
                case AgentStartEvent event -> out.print("AGENT: ");
                case AgentEndEvent event -> out.println();

                case ThinkingBlockStartEvent event -> out.print("[think: ");
                case ThinkingBlockDeltaEvent event -> out.print(event.getDelta());
                case ThinkingBlockEndEvent event -> out.println("]");

                case TextBlockStartEvent event -> {}
                case TextBlockDeltaEvent event -> out.print(event.getDelta());
                case TextBlockEndEvent event -> out.println();

                case ToolCallStartEvent event -> out.printf("[tool]: %s \n", som.writeValueAsString(agentEvent));
                case ToolCallDeltaEvent event -> out.print(event.getDelta());
                case ToolCallEndEvent event -> out.println();

                case RequireUserConfirmEvent event -> {
                    List<ConfirmResult> confirmResults = new ArrayList<>(event.getToolCalls().size());
                    event.getToolCalls().forEach(tc -> {
                        out.printf("allowed: %s\n", tc);
                        confirmResults.add(new ConfirmResult(true, tc));
                    });
                    UserMessage confirmMsg = UserMessage.builder()
                            .metadata(Map.of(Msg.METADATA_CONFIRM_RESULTS, confirmResults))
                            .build();
                    agent.call(confirmMsg, context).block();
                }
                case UserConfirmResultEvent event -> out.printf("confirm: %s\n", event.getConfirmResults());

                default -> {}
            }
        }).blockLast();
    }

    public void close(){
        agent.close();
    }


}
