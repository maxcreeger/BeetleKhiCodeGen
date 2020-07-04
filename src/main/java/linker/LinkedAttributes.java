package linker;

import exceptions.UnavailableCommandException;
import test.beetlekhi.command.Attribute;
import test.beetlekhi.command.Command;
import test.beetlekhi.process.ExecuteCommand;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class LinkedAttributes {

    protected final LinkedCommand linkedCommand;
    public Command moduleCommand;
    public ExecuteCommand exec;
    public Map<String, String> assignments = new HashMap<>();

    public LinkedAttributes(LinkedCommand runtimeCommand, Command moduleCommand, ExecuteCommand exec) throws UnavailableCommandException {
        this.linkedCommand = runtimeCommand;
        this.moduleCommand = moduleCommand;
        this.exec = exec;
        if (exec.getAttributes() != null) {
            exec:
            for (Attribute execAttribute : exec.getAttributes()
                    .getAttribute()) {
                if (moduleCommand.getAttributes() != null) {
                    for (test.beetlekhi.command.Attribute availAttribute : moduleCommand.getAttributes()
                            .getAttribute()) {
                        if (availAttribute.getName()
                                .equals(execAttribute.getName())) {
                            assignments.put(execAttribute.getName(), execAttribute.getValue());
                            System.out.println("  |  |  + Attribute '" + execAttribute.getName() + "' of type '" + execAttribute.getType() + "' : '"
                                    + execAttribute.getValue() + "'");
                            continue exec;
                        }
                    }
                    throw new UnavailableCommandException("Operation '" + this.linkedCommand.linkedOperation.operation.getName() + "' requests command '"
                            + moduleCommand.getName() + "' on module '" + exec.getNode() + "' but the command does not accept an attribute named '"
                            + execAttribute.getName() + "'");
                }
            }
        }
        runtimeCommand.commandAttributes = this;
    }

    public String constructMethodCall() {
        return String.join(", ", assignments.values());
    }

    public String constructMethodDeclaration() {
        if(moduleCommand.getAttributes()!=null) {
            List<String> attributeDeclarations = moduleCommand.getAttributes()
                    .getAttribute()
                    .stream()
                    .map(attr -> attr.getType() + " " + attr.getName())
                    .collect(Collectors.toList());
            return String.join(", ", attributeDeclarations);
        } else {
            return "";
        }
    }

    public StringBuilder constructMasterWrite() {
        StringBuilder builder = new StringBuilder();
        if(exec.getAttributes() != null) {
            for (Attribute attr : exec.getAttributes().getAttribute()) {
                builder.append("\n  Wire.write(").append(attr.getName()).append(");");
            }
        }
        return builder;
    }
}