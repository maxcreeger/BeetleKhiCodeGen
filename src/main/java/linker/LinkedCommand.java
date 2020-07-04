package linker;

import test.beetlekhi.command.Command;
import test.beetlekhi.process.ExecuteCommand;

public class LinkedCommand {

    protected final LinkedOperation linkedOperation;
    LinkedNode target;
    ExecuteCommand exec;
    Command command;
    LinkedAttributes commandAttributes;

    public LinkedCommand(linker.LinkedOperation linkedOperation, ExecuteCommand exec, Command command, linker.LinkedNode target) {
        super();
        this.linkedOperation = linkedOperation;
        this.exec = exec;
        this.command = command;
        this.linkedOperation.getLinkedCommands().add(this);
        this.target = target;
    }

    public String getName() {
        return exec.getName();
    }

    public String getKeyword() {
        return command.getKeyword();
    }

    public LinkedAttributes getAttributes() {
        return commandAttributes;
    }

    public LinkedNode getTarget() {
        return target;
    }

    public StringBuilder constructMasterCommand(){
        StringBuilder program = new StringBuilder();
        String parameters = commandAttributes.constructMethodDeclaration();
        parameters = "int nodeAddress" + (parameters.isEmpty() ? "" : ", " + parameters);
        program.append("\nvoid execute_").append(getName()).append("(").append(parameters).append(") {");
        program.append("\n  Wire.beginTransmission(nodeAddress);");
        program.append("\n  Wire.write(\"").append(getKeyword()).append("\");");
        program.append(commandAttributes.constructMasterWrite());
        program.append("\n  Wire.endTransmission();");
        program.append("\n}");
        return program;
    }
}