/**
 * This file is part of alf.io.
 *
 * alf.io is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * alf.io is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with alf.io.  If not, see <http://www.gnu.org/licenses/>.
 */
package alfio.manager;

import alfio.TestConfiguration;
import alfio.config.DataSourceConfiguration;
import alfio.config.Initializer;
import alfio.config.RepositoryConfiguration;
import alfio.manager.user.UserManager;
import alfio.model.Event;
import alfio.model.Ticket;
import alfio.model.TicketCategory;
import alfio.model.modification.*;
import alfio.model.whitelist.Whitelist;
import alfio.model.whitelist.WhitelistConfiguration;
import alfio.repository.EventRepository;
import alfio.repository.TicketRepository;
import alfio.repository.WhitelistRepository;
import alfio.repository.system.ConfigurationRepository;
import alfio.repository.user.AuthorityRepository;
import alfio.repository.user.OrganizationRepository;
import alfio.repository.user.UserRepository;
import alfio.test.util.IntegrationTestUtil;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

import static alfio.test.util.IntegrationTestUtil.*;
import static org.junit.Assert.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {RepositoryConfiguration.class, DataSourceConfiguration.class, TestConfiguration.class})
@ActiveProfiles({Initializer.PROFILE_DEV, Initializer.PROFILE_DISABLE_JOBS})
@Transactional
public class WhitelistManagerIntegrationTest {

    @Autowired
    private EventManager eventManager;
    @Autowired
    private OrganizationRepository organizationRepository;
    @Autowired
    private UserManager userManager;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private AuthorityRepository authorityRepository;
    @Autowired
    private TicketRepository ticketRepository;
    @Autowired
    private ConfigurationRepository configurationRepository;
    @Autowired
    private EventRepository eventRepository;
    @Autowired
    private WhitelistRepository whitelistRepository;
    @Autowired
    private WhitelistManager whitelistManager;
    @Autowired
    private TicketReservationManager ticketReservationManager;

    @BeforeClass
    public static void initEnv() {
        initSystemProperties();
    }

    @Before
    public void setup() {
        IntegrationTestUtil.ensureMinimalConfiguration(configurationRepository);
        initAdminUser(userRepository, authorityRepository);
    }

    @Test
    public void testLinkToEvent() {

        List<TicketCategoryModification> categories = Collections.singletonList(
            new TicketCategoryModification(null, "default", 10,
                new DateTimeModification(LocalDate.now().plusDays(1), LocalTime.now()),
                new DateTimeModification(LocalDate.now().plusDays(2), LocalTime.now()),
                DESCRIPTION, BigDecimal.TEN, false, "", false, null, null, null, null, null));
        Pair<Event, String> pair = initEvent(categories, organizationRepository, userManager, eventManager, eventRepository);
        Event event = pair.getKey();
        Whitelist whitelist = whitelistManager.createNew("test", "This is a test", event.getOrganizationId());
        assertNotNull(whitelist);
        WhitelistConfiguration configuration = whitelistManager.createConfiguration(whitelist.getId(), event.getId(), null, WhitelistConfiguration.Type.ONCE_PER_VALUE, WhitelistConfiguration.MatchType.FULL);
        assertNotNull(configuration);
        List<TicketCategory> ticketCategories = eventManager.loadTicketCategories(event);
        int categoryId = ticketCategories.get(0).getId();
        assertTrue(whitelistManager.isWhitelistConfiguredFor(event.getId(), categoryId));
        List<WhitelistConfiguration> activeConfigurations = whitelistRepository.findActiveConfigurationsFor(event.getId(), categoryId);
        assertFalse(activeConfigurations.isEmpty());
        assertEquals(1, activeConfigurations.size());
        assertEquals(configuration.getId(), activeConfigurations.get(0).getId());
        assertFalse("Whitelist is empty, therefore no value is allowed", whitelistManager.isAllowed("test@test.ch", event.getId(), categoryId));
        int items = whitelistManager.insertItems(whitelist.getId(), Collections.singletonList(new WhitelistItemModification("test@test.ch", "description")));
        assertEquals(1, items);
        assertTrue("Value should be allowed", whitelistManager.isAllowed("test@test.ch", event.getId(), categoryId));

        TicketReservationModification ticketReservation = new TicketReservationModification();
        ticketReservation.setAmount(1);
        ticketReservation.setTicketCategoryId(categoryId);

        String reservationId = ticketReservationManager.createTicketReservation(event, Collections.singletonList(new TicketReservationWithOptionalCodeModification(ticketReservation, Optional.empty())),
            Collections.emptyList(), DateUtils.addDays(new Date(), 1), Optional.empty(), Optional.empty(), Locale.ENGLISH, false);

        Ticket ticket = ticketRepository.findFirstTicketInReservation(reservationId).orElseThrow(NullPointerException::new);
        assertTrue("cannot confirm ticket", whitelistManager.confirmTicket("test@test.ch", event.getId(), categoryId, ticket.getId()));

        reservationId = ticketReservationManager.createTicketReservation(event, Collections.singletonList(new TicketReservationWithOptionalCodeModification(ticketReservation, Optional.empty())),
            Collections.emptyList(), DateUtils.addDays(new Date(), 1), Optional.empty(), Optional.empty(), Locale.ENGLISH, false);

        ticket = ticketRepository.findFirstTicketInReservation(reservationId).orElseThrow(NullPointerException::new);
        assertFalse("shouldn't be allowed", whitelistManager.confirmTicket("test@test.ch", event.getId(), categoryId, ticket.getId()));

    }
}