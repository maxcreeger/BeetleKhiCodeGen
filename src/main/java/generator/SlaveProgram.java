package generator;

import beetlkhi.utils.xsd.ElementFilter;
import linker.LinkedNode;
import test.beetlekhi.command.Attribute;
import test.beetlekhi.command.Command;
import test.beetlekhi.module.*;

import javax.xml.bind.JAXBElement;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

public class SlaveProgram {

    final LinkedNode linkedNode;

    /*
     * Data Type    Size                  Range                  Common name
     * int         2 bytes         -32,768 to 32,767             integer
     * int8_t      1 byte             -128 to 128                signed char
     * uint8_t     1 byte                0 to 255                unsigned char
     * int16_t     2 bytes         -32,768 to 32,767             integer
     * uint16_t    2 bytes               0 to 65,535             unsigned integer
     * int32_t     4 bytes  -2,147,483,648 to 2,147,483,647      long
     * uint32_t    4 bytes               0 to 4,294,967,295      unsigned long
     *
     */
    public static enum Number {
        INT(2, -32_768, 32_767, "integer"), INT8(1, -128, 32_767, "signed char"), UINT8(1, 0, 32_767, "unsigned char"), INT16(2, -32_768, 32_767, "integer"), UINT16(
                2, 0, 32_767, "unsigned integer"), INT32(4, -2_147_483_648, 2_147_483_647, "long"), UINT32(4, 0, 4_294_967_295L, "unsigned long");

        int byteSize;
        long min;
        long max;
        String commonName;

        Number(int byteSize, long min, long max, String commonName) {
            this.byteSize = byteSize;
            this.min = min;
            this.max = max;
            this.commonName = commonName;
        }
    }

    public SlaveProgram(LinkedNode linkedNode) {
        this.linkedNode = linkedNode;
    }

    public String constructProgramCode() {
        StringBuilder program = new StringBuilder("//" + linkedNode.getKhiModule()
                .getName());
        program.append(constructIncludeStatements());
        program.append(constructHelpers());
        program.append(constructEventVariablesDeclaration());
        program.append(constructSensorVariablesDeclaration());
        program.append(constructMessagingVariables());
        program.append(constructCommandVariables());
        program.append(constructSetupMethod());
        program.append(constructLoopMethod());
        program.append(constructReceiveI2cMessageMethod());
        program.append(constructRespondI2cRequestMethod());
        program.append(constructMethods());

        // End of file
        program.append("\n");
        return program.toString();
    }

    public void saveToFile(File file) throws IOException {
        if (file.getParentFile().exists() || file.getParentFile().mkdirs()) {
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(this.toString().getBytes());
        }
    }

    private StringBuilder constructIncludeStatements() {
        StringBuilder includes = new StringBuilder();
        includes.append("\n\n// Includes");
        includes.append("\n#include <Wire.h>\n#include <stdio.h>");
        List<Object> codeOrCommunicationOrHardware = linkedNode.getKhiModule().getCodeOrCommunicationOrHardware();
        Optional<Code> code = ElementFilter.getClass(codeOrCommunicationOrHardware, Code.class);
        if (code.isPresent()) {
            Optional<Libraries> libraries = code.get().getLibrariesOrStateVariablesOrSetup()
                    .stream()
                    .filter(cch -> cch.getClass() == Libraries.class)
                    .map(Libraries.class::cast)
                    .findAny();
            if (libraries.isPresent() && libraries.get().getLibrary() != null) {
                for (Library lib : libraries.get().getLibrary()) {
                    includes.append("\n#include <")
                            .append(lib.getValue())
                            .append(">");
                }
            }
        }
        return includes;
    }

    private StringBuilder constructHelpers() {
        StringBuilder helpers = new StringBuilder("\n\n// Helper methods");
        // simple checksum method:
        // Send a pointer to the data, and the size of the data.
        // The result of the method is the checksum.
        // Interestingly, if the expected checksum is passed at the end of the data, then the result *should* be zero.
        helpers.append("\nunsigned char checksum (unsigned char *ptr, size_t sz) {")
                .append("\n  unsigned char chk = 0;")
                .append("\n  while (sz-- != 0)")
                .append("\n    chk -= *ptr++;")
                .append("\n    return chk;")
                .append("\n  }")
                .append("\n}");

        // The Wire API uses uint_t datatype (char, 1byte, 0 <-> 255) so for more complex numbers we need to encode them:
        helpers.append("\nvoid putInt(unsigned char *ptr, int16_t number) {")
                .append("\n  ptr[        0         ] = (number >>  8) & 0xFF;")
                .append("\n  ptr[  sizeof( int16_t)] =  number        & 0xFF;")
                .append("\n}");
        helpers.append("\nvoid putUnsignedInt(unsigned char *ptr, uint16_t number) {")
                .append("\n  ptr[        0         ] = (number >>  8) & 0xFF;")
                .append("\n  ptr[  sizeof(uint16_t)] =  number        & 0xFF;")
                .append("\n}");
        helpers.append("\nvoid putLong(unsigned char *ptr, int32_t number) {")
                .append("\n  ptr[        0         ] = (number >> 24) & 0xFF;")
                .append("\n  ptr[  sizeof( int32_t)] = (number >> 16) & 0xFF;")
                .append("\n  ptr[2*sizeof( int32_t)] = (number >>  8) & 0xFF;")
                .append("\n  ptr[3*sizeof( int32_t)] =  number        & 0xFF;")
                .append("\n}");
        helpers.append("\nvoid putUnsignedLong(unsigned char *ptr, uint32_t number) {")
                .append("\n  ptr[        0         ] = (number >> 24) & 0xFF;")
                .append("\n  ptr[  sizeof(uint32_t)] = (number >> 16) & 0xFF;")
                .append("\n  ptr[2*sizeof(uint32_t)] = (number >>  8) & 0xFF;")
                .append("\n  ptr[3*sizeof(uint32_t)] =  number        & 0xFF;")
                .append("\n}");
        // Read
        helpers.append("\nint16_t readInt(unsigned char *ptr) {")
                .append("\n  int16_t number;")
                .append("\n  number = ptr[0];")
                .append("\n  number = number << 8 | ptr[  sizeof(int16_t)];")
                .append("\n  return number;")
                .append("\n}");
        helpers.append("\nuint16_t readUnsignedInt(unsigned char *ptr) {")
                .append("\n  uint16_t number;")
                .append("\n  number = ptr[0];")
                .append("\n  number = number << 8 | ptr[  sizeof(int16_t)];")
                .append("\n  return number;")
                .append("\n}");
        helpers.append("\nint32_t readLong(unsigned char *ptr) {")
                .append("\n  int32_t number;")
                .append("\n  number = ptr[0];")
                .append("\n  number = number << 8 | ptr[  sizeof(int32_t)];")
                .append("\n  number = number << 8 | ptr[2*sizeof(int32_t)];")
                .append("\n  number = number << 8 | ptr[3*sizeof(int32_t)];")
                .append("\n  return number;")
                .append("\n}");
        helpers.append("\nuint32_t readUnsignedLong(unsigned char *ptr) {")
                .append("\n  uint32_t number;")
                .append("\n  number = ptr[0];")
                .append("\n  number = number << 8 | ptr[  sizeof(uint32_t)];")
                .append("\n  number = number << 8 | ptr[2*sizeof(uint32_t)];")
                .append("\n  number = number << 8 | ptr[3*sizeof(uint32_t)];")
                .append("\n  return number;")
                .append("\n}");
        return helpers;
    }

    private StringBuilder constructEventVariablesDeclaration() {
        StringBuilder program = new StringBuilder();
        program.append("\n\n// Event Variables");
        Optional<Communication> communication = ElementFilter.getClass(linkedNode.getKhiModule().getCodeOrCommunicationOrHardware(), Communication.class);
        if (communication.isPresent()) {
            Optional<Events> events = ElementFilter.getClass(communication.get().getCommandsOrSensorsOrEvents(), Events.class);
            if (events.isPresent() &&
                    events.get().getEvent() != null) {
                for (Event event : events.get().getEvent()) {
                    program.append("\n")
                            .append("boolean ")
                            .append(linkedNode.getKhiModule()
                                    .getName())
                            .append("_")
                            .append(event.getName())
                            .append(" = false;");

                }

            }
        }
        return program;
    }

    private StringBuilder constructSensorVariablesDeclaration() {
        StringBuilder program = new StringBuilder();
        program.append("\n\n// State Variables (Sensor values)");
        Optional<Code> code = ElementFilter.getClass(linkedNode.getKhiModule().getCodeOrCommunicationOrHardware(), Code.class);
        if (code.isPresent()) {
            Optional<StateVariables> stateVariables = ElementFilter.getClass(code.get().getLibrariesOrStateVariablesOrSetup(), StateVariables.class);
            if (stateVariables.isPresent() && stateVariables.get()
                    .getStateVariable() != null) {
                for (StateVariable var : stateVariables.get().getStateVariable()) {
                    program.append("\n")
                            .append(var.getType())
                            .append(" ")
                            .append(var.getName())
                            .append(" = ")
                            .append(ProcessOverview.defaultValueForType(var.getType()))
                            .append(";");
                }
            }
        }
        return program;
    }

    private StringBuilder constructMessagingVariables() {
        StringBuilder program = new StringBuilder();
        program.append("\n\n// Messaging");
        program.append("\nchar receivedMessage[32] = \"\";");
        program.append("\nboolean bMessageReceived = false;");
        return program;
    }

    private StringBuilder constructCommandVariables() {
        StringBuilder program = new StringBuilder();
        program.append("\n\n// Received Commands");
        Optional<Communication> communication = ElementFilter.getClass(linkedNode.getKhiModule().getCodeOrCommunicationOrHardware(), Communication.class);
        if (communication.isPresent()) {
            Optional<Commands> commands = ElementFilter.getClass(communication.get().getCommandsOrSensorsOrEvents(), Commands.class);
            if (commands.isPresent()) {
                for (Command cmd : commands.get().getCommand()) {
                    if (cmd.getAttributes() != null && cmd.getAttributes()
                            .getAttribute() != null) {
                        for (Attribute attr : cmd.getAttributes()
                                .getAttribute()) {
                            program.append('\n')
                                    .append(attr.getType())
                                    .append(" ")
                                    .append(attr.getValue())
                                    .append(" = ")
                                    .append(ProcessOverview.defaultValueForType(attr.getType()))
                                    .append(";");
                        }
                    }
                }
            }
        }
        return program;
    }

    private StringBuilder constructSetupMethod() {
        Optional<Code> code = ElementFilter.getClass(linkedNode.getKhiModule().getCodeOrCommunicationOrHardware(), Code.class);
        StringBuilder program = new StringBuilder();
        if (code.isPresent()) {
            for (Object obj : code.get().getLibrariesOrStateVariablesOrSetup()) {
                if (obj instanceof JAXBElement) {
                    JAXBElement<?> element = ((JAXBElement<?>) obj);
                    if (String.class == element.getDeclaredType()) {
                        JAXBElement<String> stringElement = (JAXBElement<String>) element;
                        if ("setup".equals(stringElement.getName().getLocalPart())) {
                            String value = stringElement.getValue();
                            program.append("\n\n// Setup");
                            program.append("\nvoid setup() {");
                            program.append("\n  Wire.begin(")
                                    .append(linkedNode.node.getI2Caddress())
                                    .append("); // Join i2c bus");
                            program.append("\n  Wire.onRequest(requestedI2cMessage); // Register 'Master requests a message' event");
                            program.append("\n  Wire.onReceive(receiveI2cMessage);   // Register 'Master sends a message' event");
                            program.append("\n  Serial.begin(9600);                   // start serial for output");
                            program.append(value);
                            program.append("\n}");
                        }
                    }
                }
            }
        }
        if (program.length() == 0) {
            return new StringBuilder("\n\n// No 'Setup' code\n");
        } else {
            return program;
        }
    }

    private StringBuilder constructLoopMethod() {
        Optional<Code> code = ElementFilter.getClass(linkedNode.getKhiModule().getCodeOrCommunicationOrHardware(), Code.class);
        StringBuilder program = new StringBuilder();
        program.append("\n\nvoid loop() {");
        program.append("\n  delay(100);");
        if (code.isPresent()) {
            for (Object obj : code.get().getLibrariesOrStateVariablesOrSetup()) {
                if (obj instanceof JAXBElement) {
                    JAXBElement<?> element = ((JAXBElement<?>) obj);
                    if (String.class == element.getDeclaredType()) {
                        JAXBElement<String> stringElement = (JAXBElement<String>) element;
                        if ("setup".equals(stringElement.getName().getLocalPart())) {
                            String value = stringElement.getValue();
                            program.append(value);
                        }
                    }
                }
            }
        }
        program.append("\n}\n");
        return program;
    }

    /*
     * +-----------+-------------+-----------+----------+-----------+----------+---------+----------+
     * | MSG BEGIN | ADDR SOURCE | ADDR DEST | MSG SIZE | MSG CKSUM | MSG TYPE | MSG UID | MSG BODY |
     * +-----------+-------------+-----------+----------+-----------+----------+---------+----------+
     *
     * <--32bits--> <--32 bits--> <-32 bits-> <-32bits-> <-32 bits-> <-16bits-> <-32bits> <-N bits->
     *
     * MSG BEGIN: valeur 32 bits indicant le début d'un message. Typiquement on utilise un code hexadecimal du type 0xDEADBEEF. Cela permet de se synchroniser avec un message entrant. La probabilité de détecter un faux début de message est de 1 sur 4 milliards.
     * ADDR SOURCE: valeur indicant l'adresse de l'émetteur du message.
     * ADDR DEST: valeur indicant l'adresse du destinataire du message. Si l'adresse ne correspond pas à l'adresse du module le message est rejeté sauf si cette adresse est le complément de zéro (i.e. 0xfffff... en hexadécimal) ce qui indiquera un message de type "broadcast" (pour tous). Un message broadcast permet, par ex, de faire la liste de tous les modules présents sur un réseau.
     * MSG SIZE: taille du message (y compris l'en-tête) en bytes. Pour des raisons pratique, nous devrons limiter cette taille à qq centaines de bytes maximum pour ne pas saturer les capacités de RAM des modules. Le message n'est traité que lorsque tous les bytes sont disponibles dans la mémoire tampon.
     * MSG CKSUM: somme de contrôle du message. Une fois que tous les bytes sont parvenus dans la mémoire tampon, la somme de contrôle est vérifiée. Cela permet de rejeter les messages foireux (par ex 1 bit mal transmis).
     * MSG TYPE: un identifiant 16 bits de la nature du corps du message (voir plus bas).
     * MSG UID: un identifiant unique à 32 bits du message pour le transfert sécurisé (voir plus bas).
     * MSG BODY: une série de bytes codant pour le corps du message.
     */
    private StringBuilder constructReceiveI2cMessageMethod() {
        Optional<Communication> communication = ElementFilter.getClass(linkedNode.getKhiModule().getCodeOrCommunicationOrHardware(), Communication.class);
        if (!communication.isPresent()) {
            return noCommunication();
        }
        Optional<Commands> commands = ElementFilter.getClass(communication.get().getCommandsOrSensorsOrEvents(), Commands.class);
        if (!commands.isPresent()) {
            return noCommunication();
        }
        StringBuilder program = new StringBuilder();
        // Check all commands
        program.append("\n\n// Receive communication from master Node");
        program.append("\nvoid receiveI2cMessage(int size) {");
        program.append("\n  char receivedMessage[32];");
        program.append("\n  int i = 0;");
        program.append("\n  while (1 < Wire.available() && i < 32) {        // loop through all but the last");
        program.append("\n    receivedMessage[i] = Wire.read();   // receive byte as a character");
        program.append("\n    i++;");
        program.append("\n  }");
        for (Command cmd : commands.get().getCommand()) {
            String keyword = cmd.getKeyword();
            program.append("\n  if(");
            String separator = "";
            int nbcar = 0;
            for (char car : keyword.toCharArray()) {
                program.append(separator)
                        .append("receivedMessage[")
                        .append(nbcar)
                        .append("] == '")
                        .append(car)
                        .append("'");
                separator = " &&\n     ";
                nbcar++;
            }
            nbcar++;
            program.append(") {");
            program.append("\n    // Parse attribute values");
            if (cmd.getAttributes() != null && cmd.getAttributes().getAttribute() != null) {
                for (Attribute attr : cmd.getAttributes()
                        .getAttribute()) {
                    String attrVarName = attr.getValue();
                    program.append("\n    // Parse ").append(attr.getName());
                    program.append("\n    ").append(attrVarName).append(" = ").append(ProcessOverview.defaultValueForType(attr.getType())).append(";");
                    switch (attr.getType()) {
                        case "int":
                        case "long":
                            // Parse int
                            program.append("\n    for(int i = 0; i < ").append(attr.getLength()).append("; i++) {");
                            program.append("\n      ").append(attrVarName).append(" = ").append(attrVarName).append(" * 10 + ").append("receivedMessage[").append(nbcar).append(" + i] - '0';");
                            program.append("\n    }");
                            nbcar += Integer.parseInt(attr.getLength()) + 2;
                            break;
                        case "float":
                        case "double":
                            // Parse int
                            program.append("\n    for(int i = 0; i < ").append(attr.getLength()).append("; i++) {");
                            program.append("\n      ").append(attrVarName).append(" = ").append(attrVarName).append(" * 10 + ").append("receivedMessage[").append(nbcar).append(" + i] - '0';");
                            program.append("\n    }");
                            nbcar += Double.parseDouble(attr.getLength()) + 2;
                            break;
                        case "boolean":
                            program.append("\n    ").append(attrVarName).append(" = receivedMessage[").append(nbcar + 1).append("] == 't';");
                            nbcar += 6; // 'true' or 'false'+ space
                            break;
                        default:
                            program.append("\n// unknown type: ").append(attr.getType()).append(", cannot parse!");
                            break;
                    }
                }
            }
            program.append("\n    return;");
            program.append("\n  }");
        }
        program.append("\n  bMessageReceived = true;");
        program.append("\n}");
        return program;
    }

    private StringBuilder noCommunication() {
        StringBuilder program = new StringBuilder();
        program.append("\n\n// No communication");
        program.append("\nvoid receiveI2cMessage(int size) {");
        program.append("\n  // No communication defined");
        program.append("\n}");
        return program;
    }

    private StringBuilder constructRespondI2cRequestMethod() {
        StringBuilder program = new StringBuilder();
        program.append("\n\nvoid requestedI2cMessage() {");
        program.append("\n  int16_t num = 1234;  // number to send");
        program.append("\n  byte myArray[2];");
        program.append("\n  putInt(myArray, num);");
        program.append("\n  Wire.write(myArray); // Send message");
        program.append("\n}");
        return program;
    }

    private String constructMethods() {
        StringBuilder methodsDeclaration = new StringBuilder("\n\n// Methods dedicated to the module:\n");
        Optional<Code> code = ElementFilter.getClass(linkedNode.getKhiModule().getCodeOrCommunicationOrHardware(), Code.class);
        if (code.isPresent()) {
            Optional<Methods> methods = ElementFilter.getClass(code.get().getLibrariesOrStateVariablesOrSetup(), Methods.class);
            if (methods.isPresent()) {
                for (Method method : methods.get().getMethod()) {
                    methodsDeclaration.append("\nvoid ")
                            .append(method.getName())
                            .append("() {")
                            .append(method.getValue())
                            .append("\n}");
                }
            }
        }
        return methodsDeclaration.toString();
    }

    @Override
    public String toString() {
        return constructProgramCode();
    }
}