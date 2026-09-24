package util;


import io.agentscope.core.tool.DefaultToolResultConverter;
import io.agentscope.core.tool.Tool;

public class Tools {

    @Tool(
            name = "goodbye",
            description = """
                    If the user says goodbye to you or something similar,
                    you can use this tool call to end the conversation.
                    """,
            readOnly = false,
            concurrencySafe = false,
            converter = DefaultToolResultConverter.class
    )
    public void goodbye(){
        AgentRunner.getRunner().setRunFlag(false);
        AgentRunner.getRunner().close();
    }
}
