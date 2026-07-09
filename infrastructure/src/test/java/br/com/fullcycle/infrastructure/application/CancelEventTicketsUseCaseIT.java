package br.com.fullcycle.infrastructure.application;

import br.com.fullcycle.IntegrationTest;
import br.com.fullcycle.application.ticket.CreateTicketForCustomerUseCase;
import br.com.fullcycle.domain.customer.CustomerRepository;
import br.com.fullcycle.domain.event.EventId;
import br.com.fullcycle.domain.event.EventRepository;
import br.com.fullcycle.domain.partner.PartnerRepository;
import br.com.fullcycle.domain.event.Event;
import br.com.fullcycle.domain.customer.Customer;
import br.com.fullcycle.domain.partner.Partner;
import br.com.fullcycle.domain.event.ticket.TicketRepository;
import br.com.fullcycle.infrastructure.gateways.ConsumerQueueGateway;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;

import java.util.Collection;

@ActiveProfiles("test")
class CancelEventTicketsUseCaseIT extends IntegrationTest {

    @Autowired
    private PartnerRepository partnerRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private CreateTicketForCustomerUseCase createTicketForCustomerUseCase;

    @Autowired
    private ConsumerQueueGateway consumerQueueGateway;

    @Autowired
    private ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        ticketRepository.deleteAll();
        eventRepository.deleteAll();
        customerRepository.deleteAll();
        partnerRepository.deleteAll();
    }

    @Test
    @DisplayName("Fluxo assíncrono ponta a ponta: evento cancelado cancela tickets")
    public void testEventCancelledFlowCancelsTickets() throws Exception {
        // given
        final var partner = partnerRepository.create(Partner.newPartner("Disney", "45.123.123/0001-12", "disney@gmail.com"));
        final var event = eventRepository.create(Event.newEvent("Disney on Ice", "2021-01-01", 10, partner));
        final var customer = customerRepository.create(Customer.newCustomer("John Doe", "123.456.789-00", "john@gmail.com"));

        final var eventTicketId = br.com.fullcycle.domain.event.EventTicketId.unique().value();
        createTicketForCustomerUseCase.execute(new CreateTicketForCustomerUseCase.Input(eventTicketId, event.eventId().value(), customer.customerId().value()));

        final Collection<br.com.fullcycle.domain.event.ticket.Ticket> ticketsBefore = ticketRepository.ticketsByEventId(event.eventId());
        Assertions.assertEquals(1, ticketsBefore.size());
        Assertions.assertEquals(br.com.fullcycle.domain.event.ticket.TicketStatus.PENDING, ticketsBefore.iterator().next().status());

        // when
        final var domainEvent = new br.com.fullcycle.domain.event.EventCancelled(EventId.with(event.eventId().value()));
        consumerQueueGateway.publish(mapper.writeValueAsString(domainEvent));

        Thread.sleep(1000);

        // then
        final Collection<br.com.fullcycle.domain.event.ticket.Ticket> ticketsAfter = ticketRepository.ticketsByEventId(event.eventId());
        Assertions.assertEquals(1, ticketsAfter.size());
        Assertions.assertEquals(br.com.fullcycle.domain.event.ticket.TicketStatus.CANCELLED, ticketsAfter.iterator().next().status());
    }
}
