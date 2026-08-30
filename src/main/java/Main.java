import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.event.*;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.UserMessage;
import io.agentscope.core.model.Model;
import io.agentscope.core.model.ModelCreationContext;
import io.agentscope.core.model.ModelRegistry;
import io.agentscope.core.state.JsonFileAgentStateStore;
import io.agentscope.harness.agent.HarnessAgent;
import io.agentscope.harness.agent.memory.compaction.CompactionConfig;
import util.SafeObjectMapper;

import java.nio.file.Path;
import java.util.*;


public class Main {

    private static final java.io.PrintStream out = System.out;
    private final SafeObjectMapper som = new SafeObjectMapper();
    private final Scanner input = new Scanner(System.in);

    private final CompactionConfig compactionConfig = CompactionConfig.builder()
            .triggerMessages(30)
            .keepMessages(10).build();

    private final Model deepseek_v4_flash = ModelRegistry.resolve(
            "deepseek:deepseek-v4-flash",
            ModelCreationContext.builder()
                    .enableThinking(false)
                    .baseUrl("https://api.deepseek.com")
                    .apiKey(System.getenv("DEEPSEEK_API_KEY"))
                    .stream(true)
                    .build()
    );

    private final HarnessAgent agent = HarnessAgent.builder()
            .name("deepseek-agent")
            .sysPrompt("你是智能助手，需要言简意赅地回应用户。")
            .model(deepseek_v4_flash)
            .workspace(Path.of(".agentscope/workspace"))
            .stateStore(new JsonFileAgentStateStore(Path.of("./agent-state")))
            .compaction(compactionConfig).build();

    private final Map<String, RuntimeContext> user_ctx = new HashMap<>();
    {
        user_ctx.put("xiaowei", RuntimeContext.builder()
                .sessionId("xiaowei-session-0")
                .userId("xiaowei")
                .build()
        );
    }


    public static void main(String[] args) {

        Main app = new Main();
        while (true) {
            app.run();
        }
    }

    private void run() {


//        agent.call("你好，我叫小魏，你是谁？", xiaowei_ctx).block();
//        System.out.println(agent.call("那你可以做什么事情？", xiaowei_ctx).block().getTextContent());

//        out.println(agent.getToolkit().getToolNames());

        RuntimeContext ctx = user_ctx.getOrDefault("xiaowei", RuntimeContext.empty());
        out.print("USER: ");
        agent.streamEvents(input.nextLine(), ctx).doOnNext(event -> {


            if (event instanceof RequireUserConfirmEvent confirmEvent) {
                List<ConfirmResult> confirmResults = new ArrayList<>(confirmEvent.getToolCalls().size());
                confirmEvent.getToolCalls().forEach(tc -> {
                    out.printf("access: %s \n", som.writeValueAsString(tc));
                    ConfirmResult confirmResult = new ConfirmResult(true, tc);
                    confirmResults.add(confirmResult);
                });

                UserMessage confirmMsg = UserMessage.builder()
                        .metadata(Map.of(Msg.METADATA_CONFIRM_RESULTS, confirmResults))
                        .build();
                agent.call(confirmMsg, ctx).block();
            }


            switch (event.getType()) {

                case AgentEventType.TEXT_BLOCK_START -> out.print("AGENT: ");
                case AgentEventType.TEXT_BLOCK_DELTA -> out.print(
                        ((TextBlockDeltaEvent) event).getDelta()
                );
                case AgentEventType.TEXT_BLOCK_END -> out.print('\n');

                case AgentEventType.TOOL_CALL_START -> out.printf("[tool]: %s \n", som.writeValueAsString(event));
                case AgentEventType.TOOL_CALL_DELTA -> out.print(((ToolCallDeltaEvent) event).getDelta());
                case AgentEventType.TOOL_CALL_END -> out.println('\n');
            }
        }).blockLast();
    }
}
