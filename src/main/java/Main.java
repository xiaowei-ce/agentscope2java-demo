import io.agentscope.core.agent.RuntimeContext;
import util.AgentRunner;

import java.util.*;


public class Main {

    private static final Scanner input = new Scanner(System.in);

    public static final Map<String, RuntimeContext> user_ctx = new HashMap<>();
    {
        user_ctx.put("xiaowei-s0", RuntimeContext.builder()
                .userId("xiaowei")
                .sessionId("session-0")
                .build()
        );
    }

    public static void main(String[] args) {

        AgentRunner runner = AgentRunner.getRunner();
        while (runner.getRunFlag()) {
            System.out.print("USER:");
            runner.run(
                    input.nextLine(),
                    user_ctx.getOrDefault("xiaowei-s0", RuntimeContext.empty())
            );
        }
    }
}




// just test
//        DataBlock dataBlock = DataBlock.builder()
//                .source(Base64Source.builder()
//                        .mediaType("image/png")
//                        .data(FileBase64.base64("./580b57fcd9996e24bc43c180.png"))
//                        .build()
//                ).build();
//        TextBlock textBlock = TextBlock.builder()
//                .text("描述一下这张图片")
//                .build();
//        UserMessage userMessage = new UserMessage(dataBlock, textBlock);
//        List<DataBlock> contentBlocks = userMessage.getContentBlocks(DataBlock.class);
//        out.print(contentBlocks);
//        System.out.println(agent.call(userMessage).block().getTextContent());