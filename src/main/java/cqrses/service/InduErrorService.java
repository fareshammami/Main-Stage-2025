package cqrses.service;

import com.eventstore.dbclient.*;
import cqrses.command.*;
import cqrses.entity.CompensationStatus;
import cqrses.entity.InduErrorStatus;
import cqrses.event.CompensationCreatedEvent;
import cqrses.event.InduErrorCreatedEvent;
import lombok.RequiredArgsConstructor;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.serialization.SerializedObject;
import org.axonframework.serialization.Serializer;
import org.axonframework.serialization.SimpleSerializedObject;
import org.axonframework.serialization.SimpleSerializedType;
import org.springframework.stereotype.Service;

import java.util.concurrent.ExecutionException;

@Service
@RequiredArgsConstructor
public class InduErrorService {

    private final CommandGateway commandGateway;
    private final EventStoreDBClient eventStoreDBClient;
    private final Serializer serializer;

    public void initializeGroup(String groupId) {
        commandGateway.send(new CreateInduErrorCommand(groupId));
    }

    public void addInduError(String groupId, Double amount) {
        commandGateway.send(new AddInduErrorCommand(groupId, amount));
    }

    public void addCompensation(String groupId, Double amount) {
        commandGateway.send(new AddCompensationCommand(groupId, amount));
    }

    public double processErrors(String groupId) throws ExecutionException, InterruptedException {
        String streamId = "EventAggregate-" + groupId;
        double induTotal = 0.0;
        double compensationTotal = 0.0;

        System.out.println("📤 Reading stream: " + streamId);

        ReadResult result = eventStoreDBClient.readStream(streamId, ReadStreamOptions.get().forwards().fromStart()).get();

        for (ResolvedEvent resolved : result.getEvents()) {
            RecordedEvent event = resolved.getEvent();

            switch (event.getEventType()) {
                case "InduErrorCreatedEvent" -> {
                    SerializedObject<byte[]> serialized = new SimpleSerializedObject<>(
                            event.getEventData(),
                            byte[].class,
                            new SimpleSerializedType(InduErrorCreatedEvent.class.getName(), null)
                    );
                    InduErrorCreatedEvent indu = (InduErrorCreatedEvent) serializer.deserialize(serialized);
                    if (indu.getStatus() == InduErrorStatus.NOT_TRAITED) {
                        induTotal += indu.getAmount();
                        commandGateway.send(new HandleInduErrorCommand(
                                groupId,
                                indu.getInduErrorId(),
                                indu.getAmount()
                        ));
                        System.out.println("🔁 InduError traité pour: " + indu.getInduErrorId());
                    }
                }

                case "CompensationCreatedEvent" -> {
                    SerializedObject<byte[]> serialized = new SimpleSerializedObject<>(
                            event.getEventData(),
                            byte[].class,
                            new SimpleSerializedType(CompensationCreatedEvent.class.getName(), null)
                    );
                    CompensationCreatedEvent comp = (CompensationCreatedEvent) serializer.deserialize(serialized);
                    if (comp.getStatus() == CompensationStatus.NOT_TRAITED) {
                        compensationTotal += comp.getAmount();
                        commandGateway.send(new HandleCompensationCommand(
                                groupId,
                                comp.getCompensationId(),
                                comp.getAmount()
                        ));
                        System.out.println("🔁 Compensation traitée pour: " + comp.getCompensationId());
                    }
                }
            }
        }

        double finalTotal = induTotal - compensationTotal;
        System.out.println("✅ Indus: " + induTotal + " | Compensations: -" + compensationTotal + " | ➡ Total: " + finalTotal);

        // Envoyer l'état actuel
        commandGateway.send(new ProcessInduErrorsCommand(groupId, finalTotal));
        return finalTotal;
    }
}
