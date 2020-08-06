package linker;

import beetlkhi.utils.xsd.ElementFilter;
import exceptions.InvalidKhiProcessException;
import parsing.ProcessLinker;
import test.beetlekhi.module.Communication;
import test.beetlekhi.module.Event;
import test.beetlekhi.module.Events;
import test.beetlekhi.module.Khimodule;
import test.beetlekhi.process.EventListener;
import test.beetlekhi.process.Timer;
import test.beetlekhi.process.*;

import java.util.*;

public class LinkedTrigger {

    public final LinkedOperation nextLinkedOperation;
    final String operationName;
    final AndTrigger andTrigger;

    public LinkedTrigger(Trigger trigger,
                         String operationName,
                         ProcessLinker.OperationLinker operationLinker,
                         ProcessLinker.NodeLinker nodeLinker,
                         String processName) throws InvalidKhiProcessException {
        this.operationName = operationName;
        // Validate next Operation
        String nextOperationName = trigger.getNextOperation().getValue();
        Operation nextOperation = operationLinker.lookupOperation(nextOperationName);
        if (nextOperation == null) {
            throw new InvalidKhiProcessException("Process '" + processName + "', operation '" + operationName
                    + "' defines Trigger '" + trigger.getName() + " which triggers the next operation '" + nextOperationName
                    + "', but no such operation is defined in the process. Available operations: " + operationLinker.getOperationLookup().keySet());
        }
        nextLinkedOperation = operationLinker.lookupLinkedOperation(nextOperationName);
        System.out.println("  |  + Trigger " + (trigger.getName() == null ? "(unnamed)" : trigger.getName()) + " triggers Operation '" + nextLinkedOperation.getName() + "'");

        if (trigger.getEventListenersLogicalAnd() != null) {
            this.andTrigger = new AndTrigger(operationName, trigger.getEventListenersLogicalAnd(), nextLinkedOperation, operationLinker, nodeLinker, processName, "  |  | ");
        } else {
            andTrigger = null;
        }
    }

    public LinkedOperation getNextLinkedOperation() {
        return nextLinkedOperation;
    }

    public AndTrigger getAndTrigger() {
        return andTrigger;
    }

    public Set<LinkedEventListener> getAllInternalEvents() {
        return andTrigger.getAllInternalEvents();
    }

    public StringBuilder constructTransitionCondition() {
        return andTrigger.constructTransitionCondition();
    }

    public interface IEventListener {
        Set<LinkedEventListener> getAllInternalEvents();

        StringBuilder constructTransitionCondition();
    }

    class OrTrigger implements IEventListener {

        List<LinkedEventListener> events = new ArrayList<>();
        List<AndTrigger> andEvents = new ArrayList<>();
        List<Timer> timers = new ArrayList<>();

        OrTrigger(String operationName, EventListenersLogicalOr logicalOr, LinkedOperation nextLinkedOperation, ProcessLinker.OperationLinker operationLinker, ProcessLinker.NodeLinker nodeLinker, String processName, String printPrefix) throws InvalidKhiProcessException {
            System.out.println(printPrefix + " + OR");
            for (EventListener eventListener : logicalOr.getEventListener()) {
                events.add(new LinkedEventListener(eventListener, nextLinkedOperation, operationLinker, nodeLinker, operationName, processName, printPrefix + " | "));
            }
            for (Timer timer : logicalOr.getTimer()) {
                timers.add(timer);
                // Print
                System.out.println(printPrefix + " + Timer '" + timer.getName() + "' waiting for " + timer.getValue() + "ms");
            }
            for (EventListenersLogicalAnd logicalAnd : logicalOr.getEventListenersLogicalAnd()) {
                andEvents.add(new AndTrigger(operationName, logicalAnd, nextLinkedOperation, operationLinker, nodeLinker, processName, printPrefix + " | "));
            }
        }

        @Override
        public Set<LinkedEventListener> getAllInternalEvents() {
            Set<LinkedEventListener> result = new HashSet<>(events);
            andEvents.stream()
                    .map(IEventListener::getAllInternalEvents)
                    .forEach(result::addAll);
            return result;
        }

        @Override
        public StringBuilder constructTransitionCondition() {
            String separator = "";
            StringBuilder content = new StringBuilder("(");
            for (LinkedTrigger.LinkedEventListener linkedEventListener : events) {
                String eventBool = linkedEventListener.getEmitterNode() + "_" + linkedEventListener.getEvent().getName();
                content.append(separator).append(eventBool);
                separator = " || ";
            }
            for (LinkedTrigger.AndTrigger andTrigger : andEvents) {
                StringBuilder eventBool = andTrigger.constructTransitionCondition();
                content.append(separator).append(eventBool);
                separator = " || ";
            }
            return content.append(")");
        }
    }

    class AndTrigger implements IEventListener {

        List<LinkedEventListener> events = new ArrayList<>();
        List<OrTrigger> orEvents = new ArrayList<>();
        List<Timer> timers = new ArrayList<>();

        AndTrigger(String operationName, EventListenersLogicalAnd logicalAnd, LinkedOperation nextLinkedOperation, ProcessLinker.OperationLinker operationLinker, ProcessLinker.NodeLinker nodeLinker, String processName, String printPrefix) throws InvalidKhiProcessException {
            System.out.println(printPrefix + " + AND");
            for (EventListener eventListener : logicalAnd.getEventListener()) {
                events.add(new LinkedEventListener(eventListener, nextLinkedOperation, operationLinker, nodeLinker, operationName, processName, printPrefix + " | "));
            }
            for (Timer timer : logicalAnd.getTimer()) {
                timers.add(timer);
                // Print
                System.out.println(printPrefix + " |  + Timer '" + timer.getName() + "' waiting for " + timer.getValue() + "ms");
            }
            for (EventListenersLogicalOr logicalOr : logicalAnd.getEventListenersLogicalOr()) {
                orEvents.add(new OrTrigger(operationName, logicalOr, nextLinkedOperation, operationLinker, nodeLinker, processName, printPrefix + " | "));
            }
        }

        @Override
        public Set<LinkedEventListener> getAllInternalEvents() {
            Set<LinkedEventListener> result = new HashSet<>(events);
            orEvents.stream()
                    .map(IEventListener::getAllInternalEvents)
                    .forEach(result::addAll);
            return result;
        }

        @Override
        public StringBuilder constructTransitionCondition() {
            String separator = "";
            StringBuilder content = new StringBuilder("(");
            for (LinkedTrigger.LinkedEventListener linkedEventListener : events) {
                String eventBool = linkedEventListener.getEmitterNode() + "_" + linkedEventListener.getEvent().getName();
                content.append(separator).append(eventBool);
                separator = " && ";
            }
            for (LinkedTrigger.OrTrigger orTrigger : orEvents) {
                StringBuilder eventBool = orTrigger.constructTransitionCondition();
                content.append(separator).append(eventBool);
                separator = " && ";
            }
            for (Timer timer : timers) {
                StringBuilder eventBool = new StringBuilder("millis() > stateStartTime[step_").append(operationName).append("] + ").append(timer.getValue());
                content.append(separator).append(eventBool);
                separator = " && ";
            }
            return content.append(")");
        }
    }

    public static class LinkedEventListener {

        public EventListener eventListener;
        public Event event;

        LinkedEventListener(EventListener eventListener, LinkedOperation nextLinkedOperation, ProcessLinker.OperationLinker operationLinker, ProcessLinker.NodeLinker nodeLinker, String operationName, String processName, String printPrefix) throws InvalidKhiProcessException {
            this.eventListener = eventListener;
            // Validate attribute 'node'
            LinkedNode linkedNode = nodeLinker.lookupLinkedNode(eventListener.getNode());
            if (linkedNode == null) {
                throw new InvalidKhiProcessException("Process '" + processName +
                        "', operation '" + operationName
                        + "' defines listener '" + eventListener.getName() +
                        " on node '" + eventListener.getNode()
                        + "' but no such node is defined in the process. Available nodes: " + nodeLinker.getLinkedNodeLookup().keySet());
            }
            Khimodule module = linkedNode.getKhiModule();

            // Validate attribute 'event'
            this.event = findEvent(eventListener, module, operationName, processName);

            // Print
            System.out.println(printPrefix + " + Event '" + eventListener.getName() + "' listening to signal '" + eventListener.getEvent() + "' sent by node '" + eventListener.getNode() + "'");
        }

        public String getEmitterNode() {
            return eventListener.getNode();
        }

        public EventListener getEventListener() {
            return eventListener;
        }

        public Event getEvent() {
            return event;
        }


        public Event findEvent(EventListener eventListener, Khimodule module, String operationName, String processName) throws InvalidKhiProcessException {
            String eventName = eventListener.getEvent();
            Event event = null;
            Optional<Communication> communication = ElementFilter.getClass(module.getCodeOrCommunicationOrHardware(), Communication.class);
            if (!communication.isPresent()) {
                throw new InvalidKhiProcessException("Process '" + processName + "', operation '" + operationName
                        + "' defines listener '" + eventListener.getName() + " for event '" + eventName + "' on node '" + eventListener.getNode()
                        + "' implemented by module '" + module.getName() + "' but this module does not define any Communication at all");
            }
            Optional<Events> events = ElementFilter.getClass(communication.get().getCommandsOrSensorsOrEvents(), Events.class);
            if (!events.isPresent()) {
                throw new InvalidKhiProcessException("Process '" + processName + "', operation '" + operationName
                        + "' defines listener '" + eventListener.getName() + " for event '" + eventName + "' on node '" + eventListener.getNode()
                        + "' implemented by module '" + module.getName() + "' but this module does not define any Event at all");
            }
            for (Event eventOfModule : events.get().getEvent()) {
                if (eventOfModule.getName()
                        .equals(eventName)) {
                    event = eventOfModule;
                    break;
                }
            }
            if (event == null) {
                throw new InvalidKhiProcessException("Process '" + processName + "', operation '" + operationName
                        + "' defines listener '" + eventListener.getName() + " for event '" + eventName + "' on node '" + eventListener.getNode()
                        + "' implemented by module '" + module.getName() + "' but this module does not define an event by that name");
            }
            return event;
        }
    }
}