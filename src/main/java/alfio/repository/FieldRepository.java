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
package alfio.repository;

import alfio.model.TicketFieldConfiguration;
import ch.digitalfondue.npjt.*;

import java.util.List;
import java.util.Set;

@QueryRepository
interface FieldRepository {

    @Query("insert into ticket_field_configuration(event_id_fk, field_name, field_order, field_type, field_restricted_values, field_maxlength, field_minlength, field_required, context, additional_service_id, ticket_category_ids) " +
        " values (:eventId, :name, :order, :type, :restrictedValues, :maxLength, :minLength, :required, :context, :additionalServiceId, :categoryIds)")
    @AutoGeneratedKey("id")
    AffectedRowCountAndKey<Integer> insertConfiguration(@Bind("eventId") int eventId, @Bind("name") String name, @Bind("order") int order, @Bind("type") String type, @Bind("restrictedValues") String restrictedValues,
                                                        @Bind("maxLength") Integer maxLength, @Bind("minLength") Integer minLength, @Bind("required") boolean required, @Bind("context") TicketFieldConfiguration.Context context,
                                                        @Bind("additionalServiceId") Integer additionalServiceId, @Bind("categoryIds") String categoryIdsJson);

    @Query("update ticket_field_configuration set field_required = :required, field_restricted_values = :restrictedValues, field_disabled_values = :disabledValues, ticket_category_ids = :categoryIds where id = :id")
    int updateRequiredAndRestrictedValues(@Bind("id") int id, @Bind("required") boolean required, @Bind("restrictedValues") String restrictedValues, @Bind("disabledValues") String disabledValues, @Bind("categoryIds") String linkedCategoryIds);

    @Query("insert into ticket_field_description(ticket_field_configuration_id_fk, field_locale, description) values (:ticketConfigurationId, :locale, :description)")
    int insertDescription(@Bind("ticketConfigurationId") int ticketConfigurationId, @Bind("locale") String locale, @Bind("description") String description);

    @Query("update ticket_field_description set description = :description where ticket_field_configuration_id_fk = :ticketConfigurationId and field_locale = :locale")
    int updateDescription(@Bind("ticketConfigurationId") int ticketConfigurationId, @Bind("locale") String locale, @Bind("description") String description);

    @Query("select distinct lower(field_name) from ticket_field_configuration where lower(field_name) in (:fieldNames) and event_id_fk = :eventId")
    List<String> getExistingFields(@Bind("eventId") int eventId, @Bind("fieldNames") Set<String> fieldName);

}
