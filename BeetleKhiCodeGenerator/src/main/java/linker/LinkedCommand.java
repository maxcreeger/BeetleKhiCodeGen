package linker;

import exceptions.InvalidCommandAttributeException;
import exceptions.UnavailableCommandException;
import test.beetlekhi.command.Attribute;
import test.beetlekhi.command.Command;
import test.beetlekhi.process.ExecuteCommand;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class LinkedCommand {

    protected final LinkedOperation linkedOperation;
    private final LinkedNode target;
    private final ExecuteCommand exec;
    private final Command command;
    private final List<LinkedAttribute> attributes = new ArrayList<>();

    public LinkedCommand(linker.LinkedOperation linkedOperation, ExecuteCommand exec, Command command, linker.LinkedNode target) throws InvalidCommandAttributeException, UnavailableCommandException {
        super();
        this.linkedOperation = linkedOperation;
        this.exec = exec;
        this.command = command;
        this.linkedOperation.getLinkedCommands().add(this);
        this.target = target;
        int execAttributeCount = exec.getAttributes() == null || exec.getAttributes().getAttribute() == null ? 0 : exec.getAttributes().getAttribute().size();
        int moduleAttributeCount = command.getAttributes() == null || command.getAttributes().getAttribute() == null ? 0 : command.getAttributes().getAttribute().size();
        if (execAttributeCount != moduleAttributeCount) {
            throw new UnavailableCommandException("Command signature doesn't match: different number of arguments");
        }
        if (exec.getAttributes() != null) {
            nextExecAttribute:
            for (Attribute execAttribute : getExecuteCommand().getAttributes().getAttribute()) {
                if (getModuleCommand().getAttributes() != null) {
                    for (Attribute moduleAttribute : getModuleCommand().getAttributes()
                            .getAttribute()) {
                        if (moduleAttribute.getName().equals(execAttribute.getName())) {
                            LinkedAttribute linkedAttribute = new LinkedAttribute(execAttribute, moduleAttribute);
                            attributes.add(linkedAttribute);
                            System.out.println("  |  |  + Attribute '" + execAttribute.getName() + "' of type '" + execAttribute.getType() + "' : '" + execAttribute.getValue() + "'");
                            continue nextExecAttribute;
                        }
                    }
                    throw new InvalidCommandAttributeException("Operation '" + linkedOperation.operation.getName() + "' requests command '"
                            + getModuleCommand().getName() + "' on module '" + exec.getNode() + "' but the command does not accept an attribute named '"
                            + execAttribute.getName() + "'");
                }
            }
        }

    }

    public String getName() {
        return exec.getName();
    }

    public String getKeyword() {
        return command.getKeyword();
    }

    public List<LinkedAttribute> getAttributes() {
        return attributes;
    }

    public LinkedNode getTarget() {
        return target;
    }

    public Command getModuleCommand() {
        return command;
    }

    public ExecuteCommand getExecuteCommand() {
        return exec;
    }

    public StringBuilder constructMasterCommand() {
        StringBuilder program = new StringBuilder();
        StringBuilder parameters = new StringBuilder("int nodeAddress");
        for (LinkedAttribute attribute : attributes) {
            parameters.append(", ").append(attribute.constructDeclaration());
        }
        program.append("\nvoid execute_").append(getName()).append("(").append(parameters).append(") {");
        program.append("\n  Wire.beginTransmission(nodeAddress);");
        program.append("\n  Wire.write(\"").append(getKeyword()).append("\");");
        program.append(constructMasterWrites());
        program.append("\n  Wire.endTransmission();");
        program.append("\n}\n");
        return program;
    }

    public StringBuilder constructMasterWrites() {
        StringBuilder builder = new StringBuilder();
        if (exec.getAttributes() != null) {
            for (LinkedAttribute attr : getAttributes()) {
                builder.append(attr.constructMasterWrite());
            }
        }
        return builder;
    }

    public String constructMethodCall() {
        return attributes.stream().map(LinkedAttribute::getValue).collect(Collectors.joining(", "));
    }

    public static class LinkedAttribute {

        private final Attribute execAttribute; // same Name
        private final Attribute moduleAttribute; // same Name

        public LinkedAttribute(Attribute execAttribute, Attribute moduleAttribute) throws InvalidCommandAttributeException {
            if (!execAttribute.getName().equals(moduleAttribute.getName())) {
                throw new InvalidCommandAttributeException("Trying to pair two attributes with different names");
            }
            this.execAttribute = execAttribute;
            this.moduleAttribute = moduleAttribute;
        }

        public String getName() {
            return moduleAttribute.getName();
        }

        public String getType() {
            return moduleAttribute.getType();
        }

        public String getValue() {
            return execAttribute.getValue();
        }

        public String constructMasterWrite() {
            return "\n  Wire.write(" + getName() + ");";
        }

        public String constructDeclaration() {
            return getType() + " " + getName();
        }
    }
}