package br.com.fullcycle.application.ticket;

import br.com.fullcycle.application.repository.InMemoryTicketRepository;
import br.com.fullcycle.domain.customer.Customer;
import br.com.fullcycle.domain.event.Event;
import br.com.fullcycle.domain.event.EventTicketId;
import br.com.fullcycle.domain.event.EventId;
import br.com.fullcycle.domain.event.ticket.Ticket;
import br.com.fullcycle.domain.event.ticket.TicketStatus;
import br.com.fullcycle.domain.event.ticket.TicketRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

class CancelEventTicketsUseCaseTest {

    @Test
    @DisplayName("Deve cancelar todos os tickets de um evento")
    public void testCancelEventTickets() throws Exception {
        // given
        final var aPartner = br.com.fullcycle.domain.partner.Partner.newPartner("John Doe", "41.536.538/0001-00", "john.doe@gmail.com");
        final var anEvent = Event.newEvent("Disney on Ice", "2021-01-01", 10, aPartner);

        final var aCustomer = Customer.newCustomer("John Doe", "123.456.789-01", "john.doe@gmail.com");

        final var ticketRepository = new InMemoryTicketRepository();

        final var ticket = Ticket.newTicket(EventTicketId.unique(), aCustomer.customerId(), anEvent.eventId());
        ticketRepository.create(ticket);

        final var useCase = new CancelEventTicketsUseCase(ticketRepository);

        // when
        useCase.execute(new CancelEventTicketsUseCase.Input(anEvent.eventId().value()));

        // then
        final var tickets = ticketRepository.ticketsByEventId(anEvent.eventId());
        Assertions.assertEquals(1, tickets.size());
        Assertions.assertEquals(TicketStatus.CANCELLED, tickets.iterator().next().status());
    }

    @Test
    @DisplayName("Deve executar sem erro quando não há tickets")
    public void testCancelEventTicketsWhenNoTickets() throws Exception {
        // given
        final var ticketRepository = new InMemoryTicketRepository();
        final var useCase = new CancelEventTicketsUseCase(ticketRepository);

        // when / then - should not throw
        Assertions.assertDoesNotThrow(() -> useCase.execute(new CancelEventTicketsUseCase.Input(UUID.randomUUID().toString())));
    }
}
